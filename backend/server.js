// ================================================================================
// Backend GeoPing - Servidor Node.js + Express
// Sistema de Localização Indoor usando One-Class Classification (Autoencoder)
// ================================================================================

// Carregar variáveis de ambiente do arquivo .env
require('dotenv').config();

const express = require('express');
const http = require('http');
const cors = require('cors');
const { Pool } = require('pg');
const { spawn } = require('child_process');
const path = require('path');
const fs = require('fs');
const { Server } = require('socket.io');

// Configuração do servidor
const app = express();
const server = http.createServer(app);
const io = new Server(server, {
    cors: {
        origin: "*",
        methods: ["GET", "POST"]
    }
});
const PORT = process.env.PORT || 3000;

// Middleware
app.use(cors());
app.use(express.json({ limit: '10mb' }));
app.use(express.urlencoded({ extended: true }));

// Configuração do Pool de conexão com PostgreSQL
const pool = new Pool({
    user: process.env.DB_USER || 'postgres',
    host: process.env.DB_HOST || 'localhost',
    database: process.env.DB_NAME || 'geoping',
    password: process.env.DB_PASSWORD || 'postgres',
    port: process.env.DB_PORT || 5432,
});

// Disponibilizar pool e io para as rotas
app.set('pool', pool);
app.set('io', io);

// Teste de conexão com o banco
pool.connect((err, client, release) => {
    if (err) {
        console.error('Erro ao conectar ao banco de dados:', err.stack);
    } else {
        console.log('Conexão com PostgreSQL estabelecida com sucesso!');
        release();
    }
});

// ================================================================================
// ROTAS DE PRODUCAO (v2.0)
// ================================================================================

// Importar rotas modulares
const authRoutes = require('./routes/auth');
const roomRoutes = require('./routes/rooms');
const presenceRoutes = require('./routes/presence');
const messagesRoutes = require('./routes/messages');

// Importar middleware de autenticação
const authMiddleware = require('./middleware/auth');
const jwt = require('jsonwebtoken');

// ================================================================================
// GERENCIAMENTO SOCKET.IO
// ================================================================================

io.on('connection', (socket) => {
    console.log(`[Socket.io] Cliente conectado: ${socket.id}`);
    
    let authenticatedUserId = null;
    let authenticatedUsername = null;

    // Evento: Autenticação
    socket.on('authenticate', (token) => {
        try {
            if (!token || !token.startsWith('Bearer ')) {
                socket.emit('authentication_failed', 'Token inválido');
                return;
            }
            
            const actualToken = token.substring(7);
            const decoded = jwt.verify(actualToken, process.env.JWT_SECRET || 'geoping_secret_key');
            
            authenticatedUserId = decoded.userId;
            authenticatedUsername = decoded.username;
            
            socket.emit('authenticated', `Autenticado como ${authenticatedUsername}`);
            console.log(`[Socket.io] Usuário autenticado: ${authenticatedUsername} (${authenticatedUserId})`);
        } catch (error) {
            console.error('[Socket.io] Erro na autenticação:', error.message);
            socket.emit('authentication_failed', 'Token inválido ou expirado');
        }
    });

    // Evento: Entrar em uma sala
    socket.on('join_room', (roomId) => {
        if (!authenticatedUserId) {
            socket.emit('error', 'Você precisa estar autenticado');
            return;
        }
        
        socket.join(roomId);
        console.log(`[Socket.io] ${authenticatedUsername} entrou na sala: ${roomId}`);
        socket.emit('joined_room', roomId);
        
        // Notificar outros na sala
        socket.to(roomId).emit('user_joined', {
            userId: authenticatedUserId,
            username: authenticatedUsername
        });
    });

    // Evento: Sair de uma sala
    socket.on('leave_room', (roomId) => {
        socket.leave(roomId);
        console.log(`[Socket.io] ${authenticatedUsername} saiu da sala: ${roomId}`);
        
        // Notificar outros na sala
        socket.to(roomId).emit('user_left', {
            userId: authenticatedUserId,
            username: authenticatedUsername
        });
    });

    // Evento: Desconexão
    socket.on('disconnect', () => {
        console.log(`[Socket.io] Cliente desconectado: ${socket.id} (${authenticatedUsername || 'não autenticado'})`);
    });
});

// Registrar rotas públicas
app.use('/api/auth', authRoutes);

// Registrar rotas protegidas (requerem autenticação)
app.use('/api/rooms', roomRoutes);
app.use('/api/presence', presenceRoutes);
app.use('/api', messagesRoutes);

// ================================================================================
// ROTAS DA API
// ================================================================================

// Rota de teste
app.get('/', (req, res) => {
    res.json({
        message: 'GeoPing API - Sistema de Localização Indoor',
        version: '1.0.0',
        status: 'online'
    });
});

// Rota para coletar dados de treinamento do Wi-Fi
app.post('/api/collect', async (req, res) => {
    try {
        const { room_label, device_id, wifi_scan_results, heuristics } = req.body;

        // Validação dos dados obrigatórios
        if (!room_label || !device_id || !wifi_scan_results) {
            return res.status(400).json({
                error: 'Dados incompletos',
                message: 'Os campos room_label, device_id e wifi_scan_results são obrigatórios'
            });
        }

        // Validação do formato do wifi_scan_results
        if (!Array.isArray(wifi_scan_results)) {
            return res.status(400).json({
                error: 'Formato inválido',
                message: 'wifi_scan_results deve ser um array'
            });
        }

        // Validação de cada rede no scan
        for (const network of wifi_scan_results) {
            if (!network.bssid || network.rssi === undefined) {
                return res.status(400).json({
                    error: 'Formato inválido',
                    message: 'Cada rede deve conter pelo menos bssid e rssi'
                });
            }
        }

        // Inserção no banco de dados
        const query = `
            INSERT INTO wifi_training_data 
            (room_label, device_id, wifi_fingerprint, heuristics, scan_timestamp)
            VALUES ($1, $2, $3, $4, NOW())
            RETURNING id, scan_timestamp
        `;

        const values = [
            room_label,
            device_id,
            JSON.stringify(wifi_scan_results),
            heuristics ? JSON.stringify(heuristics) : null
        ];

        const result = await pool.query(query, values);

        console.log(`[COLETA] Sala: ${room_label} | Device: ${device_id} | Redes: ${wifi_scan_results.length} | ID: ${result.rows[0].id}`);

        res.status(200).json({
            success: true,
            message: 'Dados coletados com sucesso',
            data: {
                id: result.rows[0].id,
                timestamp: result.rows[0].scan_timestamp,
                networks_count: wifi_scan_results.length
            }
        });

    } catch (error) {
        console.error('Erro ao salvar dados de coleta:', error);
        res.status(500).json({
            error: 'Erro interno do servidor',
            message: error.message
        });
    }
});

// Rota para obter estatísticas de coleta por sala
app.get('/api/stats/:room_label', async (req, res) => {
    try {
        const { room_label } = req.params;

        const query = `
            SELECT 
                room_label,
                COUNT(*) as total_scans,
                COUNT(DISTINCT device_id) as unique_devices,
                MIN(scan_timestamp) as first_scan,
                MAX(scan_timestamp) as last_scan
            FROM wifi_training_data
            WHERE room_label = $1
            GROUP BY room_label
        `;

        const result = await pool.query(query, [room_label]);

        if (result.rows.length === 0) {
            return res.status(404).json({
                error: 'Sala não encontrada',
                message: `Nenhum dado encontrado para a sala ${room_label}`
            });
        }

        res.json({
            success: true,
            data: result.rows[0]
        });

    } catch (error) {
        console.error('Erro ao obter estatísticas:', error);
        res.status(500).json({
            error: 'Erro interno do servidor',
            message: error.message
        });
    }
});

// Rota para obter todos os dados de uma sala (para treinamento)
app.get('/api/training-data/:room_label', async (req, res) => {
    try {
        const { room_label } = req.params;

        const query = `
            SELECT 
                id,
                room_label,
                scan_timestamp,
                device_id,
                wifi_fingerprint,
                heuristics
            FROM wifi_training_data
            WHERE room_label = $1
            ORDER BY scan_timestamp ASC
        `;

        const result = await pool.query(query, [room_label]);

        res.json({
            success: true,
            room_label: room_label,
            total_samples: result.rows.length,
            data: result.rows
        });

    } catch (error) {
        console.error('Erro ao obter dados de treinamento:', error);
        res.status(500).json({
            error: 'Erro interno do servidor',
            message: error.message
        });
    }
});

// Rota para listar todas as salas com dados
app.get('/api/rooms', async (req, res) => {
    try {
        const query = `
            SELECT 
                room_label,
                COUNT(*) as sample_count,
                MIN(scan_timestamp) as first_scan,
                MAX(scan_timestamp) as last_scan
            FROM wifi_training_data
            GROUP BY room_label
            ORDER BY room_label
        `;

        const result = await pool.query(query);

        res.json({
            success: true,
            total_rooms: result.rows.length,
            rooms: result.rows
        });

    } catch (error) {
        console.error('Erro ao listar salas:', error);
        res.status(500).json({
            error: 'Erro interno do servidor',
            message: error.message
        });
    }
});

// ================================================================================
// ROTAS DE TREINAMENTO DO MODELO
// ================================================================================

// Configuração de quantidade mínima de amostras
const MIN_SAMPLES_FOR_TRAINING = 30;

// Rota para verificar quantidade de amostras disponíveis
app.get('/api/check-samples/:room_label', async (req, res) => {
    try {
        const { room_label } = req.params;

        const query = `
            SELECT COUNT(*) as sample_count
            FROM wifi_training_data
            WHERE room_label = $1
        `;

        const result = await pool.query(query, [room_label]);
        const sampleCount = parseInt(result.rows[0].sample_count);

        const canTrain = sampleCount >= MIN_SAMPLES_FOR_TRAINING;

        res.json({
            success: true,
            room_label: room_label,
            sample_count: sampleCount,
            min_required: MIN_SAMPLES_FOR_TRAINING,
            can_train: canTrain,
            message: canTrain 
                ? `Quantidade suficiente para treinar (${sampleCount}/${MIN_SAMPLES_FOR_TRAINING})`
                : `Insuficiente. Colete mais ${MIN_SAMPLES_FOR_TRAINING - sampleCount} amostras.`
        });

    } catch (error) {
        console.error('Erro ao verificar amostras:', error);
        res.status(500).json({
            error: 'Erro interno do servidor',
            message: error.message
        });
    }
});

// Rota para treinar o modelo
app.post('/api/train/:room_label', async (req, res) => {
    try {
        const { room_label } = req.params;

        // Verificar quantidade de amostras
        const checkQuery = `
            SELECT COUNT(*) as sample_count
            FROM wifi_training_data
            WHERE room_label = $1
        `;
        
        const checkResult = await pool.query(checkQuery, [room_label]);
        const sampleCount = parseInt(checkResult.rows[0].sample_count);

        if (sampleCount < MIN_SAMPLES_FOR_TRAINING) {
            return res.status(400).json({
                success: false,
                error: 'Amostras insuficientes',
                message: `Você tem ${sampleCount} amostras. Mínimo necessário: ${MIN_SAMPLES_FOR_TRAINING}`,
                sample_count: sampleCount,
                min_required: MIN_SAMPLES_FOR_TRAINING
            });
        }

        console.log(`[TREINAMENTO] Iniciando para sala '${room_label}' com ${sampleCount} amostras...`);

        // Caminho para o script Python
        const pythonScript = path.join(__dirname, '..', 'ml', 'train_autoencoder.py');
        const pythonExecutable = process.platform === 'win32' 
            ? path.join(__dirname, '..', 'ml', 'venv', 'Scripts', 'python.exe')
            : path.join(__dirname, '..', 'ml', 'venv', 'bin', 'python');

        // Verificar se o script existe
        if (!fs.existsSync(pythonScript)) {
            return res.status(500).json({
                success: false,
                error: 'Script de treinamento não encontrado',
                message: `Arquivo não existe: ${pythonScript}`
            });
        }

        // Definir headers para streaming
        res.setHeader('Content-Type', 'application/json');
        res.setHeader('Transfer-Encoding', 'chunked');

        // Executar o script Python no diretório correto
        const mlDirectory = path.join(__dirname, '..', 'ml');
        const pythonProcess = spawn(pythonExecutable, [pythonScript, room_label], {
            cwd: mlDirectory  // Executar no diretório ml/
        });

        let outputBuffer = '';
        let errorBuffer = '';

        // Capturar stdout
        pythonProcess.stdout.on('data', (data) => {
            const output = data.toString();
            outputBuffer += output;
            console.log(`[PYTHON] ${output.trim()}`);
            
            // Enviar progresso em tempo real
            res.write(JSON.stringify({ 
                type: 'log', 
                message: output.trim() 
            }) + '\n');
        });

        // Capturar stderr
        pythonProcess.stderr.on('data', (data) => {
            const error = data.toString();
            errorBuffer += error;
            console.error(`[PYTHON ERROR] ${error.trim()}`);
            
            res.write(JSON.stringify({ 
                type: 'error', 
                message: error.trim() 
            }) + '\n');
        });

        // Tratar conclusão do processo
        pythonProcess.on('close', async (code) => {
            if (code === 0) {
                console.log(`[TREINAMENTO] Concluído com sucesso para '${room_label}'`);
                
                // Atualizar status do modelo na tabela rooms
                try {
                    const updateQuery = `
                        UPDATE rooms 
                        SET model_trained = TRUE, 
                            last_trained_at = NOW() 
                        WHERE wifi_ssid = $1
                    `;
                    await pool.query(updateQuery, [room_label]);
                    console.log(`[TREINAMENTO] Status do modelo atualizado no banco de dados para '${room_label}'`);
                } catch (updateError) {
                    console.error(`[TREINAMENTO] Erro ao atualizar status do modelo:`, updateError);
                }
                
                // Verificar se os arquivos foram gerados
                const modelsDir = path.join(__dirname, '..', 'ml', 'models');
                const trainingHistoryImg = path.join(modelsDir, `${room_label}_training_history.png`);
                const reconstructionErrorsImg = path.join(modelsDir, `${room_label}_reconstruction_errors.png`);

                const filesGenerated = {
                    training_history: fs.existsSync(trainingHistoryImg),
                    reconstruction_errors: fs.existsSync(reconstructionErrorsImg)
                };

                // Ler metadados do treinamento
                const metadataPath = path.join(modelsDir, `${room_label}_metadata.json`);
                let trainingInfo = null;
                
                if (fs.existsSync(metadataPath)) {
                    try {
                        const metadataContent = fs.readFileSync(metadataPath, 'utf-8');
                        const metadata = JSON.parse(metadataContent);
                        
                        trainingInfo = {
                            samples_used: metadata.num_samples || sampleCount,
                            unique_bssids: metadata.num_bssids || 0,
                            epochs: metadata.model_config?.epochs || 0,
                            batch_size: metadata.model_config?.batch_size || 0,
                            latent_dim: metadata.model_config?.encoding_dim || 0,
                            hidden_layers: metadata.model_config?.hidden_layers || [],
                            validation_split: metadata.model_config?.validation_split || 0,
                            threshold: metadata.threshold || 0,
                            threshold_method: metadata.threshold_config?.method || '',
                            threshold_multiplier: metadata.threshold_config?.iqr_multiplier || 0,
                            activation: metadata.model_config?.activation || '',
                            optimizer: metadata.model_config?.optimizer || '',
                            loss_function: metadata.model_config?.loss || '',
                            training_date: metadata.training_date || ''
                        };
                    } catch (error) {
                        console.error('[TREINAMENTO] Erro ao ler metadados:', error);
                    }
                }

                res.write(JSON.stringify({
                    type: 'complete',
                    success: true,
                    message: 'Treinamento concluído com sucesso!',
                    room_label: room_label,
                    sample_count: sampleCount,
                    files_generated: filesGenerated,
                    training_info: trainingInfo,
                    results_urls: {
                        training_history: `/api/training-results/${room_label}/training_history.png`,
                        reconstruction_errors: `/api/training-results/${room_label}/reconstruction_errors.png`
                    }
                }) + '\n');
                res.end();
            } else {
                console.error(`[TREINAMENTO] Falhou para '${room_label}' com código ${code}`);
                res.write(JSON.stringify({
                    type: 'complete',
                    success: false,
                    error: 'Treinamento falhou',
                    message: errorBuffer || 'Erro desconhecido durante o treinamento',
                    exit_code: code
                }) + '\n');
                res.end();
            }
        });

        // Tratar erro no processo
        pythonProcess.on('error', (error) => {
            console.error(`[TREINAMENTO] Erro ao executar Python:`, error);
            res.write(JSON.stringify({
                type: 'complete',
                success: false,
                error: 'Erro ao executar Python',
                message: error.message
            }) + '\n');
            res.end();
        });

    } catch (error) {
        console.error('Erro ao iniciar treinamento:', error);
        res.status(500).json({
            success: false,
            error: 'Erro interno do servidor',
            message: error.message
        });
    }
});

// Rota para servir os gráficos gerados
app.get('/api/training-results/:room_label/:filename', (req, res) => {
    try {
        const { room_label, filename } = req.params;
        
        // Validar filename (apenas training_history.png ou reconstruction_errors.png)
        const allowedFiles = ['training_history.png', 'reconstruction_errors.png'];
        if (!allowedFiles.includes(filename)) {
            return res.status(400).json({
                error: 'Arquivo inválido',
                message: 'Apenas training_history.png ou reconstruction_errors.png são permitidos'
            });
        }

        // Construir caminho do arquivo
        const fileMap = {
            'training_history.png': `${room_label}_training_history.png`,
            'reconstruction_errors.png': `${room_label}_reconstruction_errors.png`
        };

        const filePath = path.join(__dirname, '..', 'ml', 'models', fileMap[filename]);

        // Verificar se o arquivo existe
        if (!fs.existsSync(filePath)) {
            return res.status(404).json({
                error: 'Arquivo não encontrado',
                message: `O gráfico ${filename} ainda não foi gerado para a sala '${room_label}'`
            });
        }

        // Enviar o arquivo
        res.sendFile(filePath);

    } catch (error) {
        console.error('Erro ao servir gráfico:', error);
        res.status(500).json({
            error: 'Erro interno do servidor',
            message: error.message
        });
    }
});

// ================================================================================
// INICIALIZAÇÃO DO SERVIDOR
// ================================================================================

server.listen(PORT, '0.0.0.0', () => {
    console.log('\n===========================================');
    console.log('  GeoPing Backend - Sistema Indoor RTLS v2.0');
    console.log('===========================================');
    console.log(`Servidor rodando em: http://localhost:${PORT}`);
    console.log(`Socket.IO ativo em: ws://localhost:${PORT}`);
    console.log(`Modo: ${process.env.NODE_ENV || 'development'}`);
    console.log('===========================================\n');
    console.log('Rotas disponíveis:');
    console.log(`  [AUTH]`);
    console.log(`  POST /api/auth/register                      - Registrar usuário`);
    console.log(`  POST /api/auth/login                         - Login`);
    console.log(`  [ROOMS]`);
    console.log(`  POST /api/rooms/create                       - Criar sala`);
    console.log(`  GET  /api/rooms/search                       - Buscar salas`);
    console.log(`  GET  /api/rooms/my-rooms                     - Minhas salas`);
    console.log(`  DELETE /api/rooms/:room_id                   - Deletar sala`);
    console.log(`  [PRESENCE]`);
    console.log(`  POST /api/presence/update                    - Atualizar presença`);
    console.log(`  GET  /api/presence/room/:room_id             - Usuários presentes`);
    console.log(`  [MESSAGES]`);
    console.log(`  POST /api/conversations/create               - Criar conversa`);
    console.log(`  POST /api/messages/send                      - Enviar mensagem`);
    console.log(`  GET  /api/messages/:conversation_id          - Buscar mensagens`);
    console.log(`  [ML/DATA]`);
    console.log(`  POST /api/collect                            - Coletar dados de Wi-Fi`);
    console.log(`  POST /api/train/:room_label                  - Treinar modelo da sala`);
    console.log(`  GET  /api/training-results/:room/:file       - Obter gráficos gerados`);
    console.log('\n');
});

// Tratamento de erros não capturados
process.on('unhandledRejection', (err) => {
    console.error('Erro não tratado:', err);
});

process.on('SIGINT', async () => {
    console.log('\nEncerrando servidor...');
    await pool.end();
    process.exit(0);
});

