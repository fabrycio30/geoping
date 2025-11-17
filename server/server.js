/**
 * Servidor Socket.IO de exemplo para o GeoPing
 * 
 * Este é um servidor Node.js simples que implementa a lógica necessária
 * para gerenciar salas de chat e mensagens em tempo real.
 * 
 * Como usar:
 * 1. Instale Node.js (https://nodejs.org)
 * 2. No terminal, navegue até esta pasta
 * 3. Execute: npm install
 * 4. Execute: node server.js
 * 5. O servidor estará rodando em http://SEU_IP:3000
 */

const express = require('express');
const app = express();
const http = require('http').createServer(app);
const io = require('socket.io')(http);

// Porta do servidor
const PORT = 3000;

// Armazena informações sobre as salas ativas
const salas = {};

// Configuração do Express para servir arquivos estáticos (opcional)
app.use(express.static('public'));

// Rota principal (opcional - para teste no navegador)
app.get('/', (req, res) => {
    res.send(`
        <h1>Servidor GeoPing Socket.IO</h1>
        <p>Servidor rodando com sucesso!</p>
        <p>Salas ativas: ${Object.keys(salas).length}</p>
        <pre>${JSON.stringify(salas, null, 2)}</pre>
    `);
});

// Lógica do Socket.IO
io.on('connection', (socket) => {
    console.log('================================');
    console.log('Novo cliente conectado!');
    console.log('ID do Socket:', socket.id);
    console.log('Data/Hora:', new Date().toLocaleString());
    console.log('================================');
    
    // Evento: Cliente entra em uma sala
    socket.on('join_room', (data) => {
        const roomId = data.room;
        socket.join(roomId);
        
        // Registra o cliente na sala
        if (!salas[roomId]) {
            salas[roomId] = {
                usuarios: [],
                mensagens: 0,
                criada_em: new Date()
            };
        }
        
        salas[roomId].usuarios.push(socket.id);
        
        console.log(`\n[JOIN] Cliente ${socket.id} entrou na sala: ${roomId}`);
        console.log(`[INFO] Sala ${roomId} agora tem ${salas[roomId].usuarios.length} usuário(s)`);
        
        // Notifica outros usuários da sala
        socket.to(roomId).emit('usuario_entrou', {
            socketId: socket.id,
            room: roomId,
            timestamp: Date.now()
        });
        
        // Envia mensagem de boas-vindas ao usuário que entrou
        socket.emit('bem_vindo', {
            room: roomId,
            mensagem: `Bem-vindo à sala ${roomId}!`,
            usuarios_na_sala: salas[roomId].usuarios.length
        });
    });
    
    // Evento: Cliente sai de uma sala
    socket.on('leave_room', (data) => {
        const roomId = data.room;
        socket.leave(roomId);
        
        // Remove o cliente da sala
        if (salas[roomId]) {
            salas[roomId].usuarios = salas[roomId].usuarios.filter(id => id !== socket.id);
            
            console.log(`\n[LEAVE] Cliente ${socket.id} saiu da sala: ${roomId}`);
            console.log(`[INFO] Sala ${roomId} agora tem ${salas[roomId].usuarios.length} usuário(s)`);
            
            // Se a sala ficou vazia, remove ela
            if (salas[roomId].usuarios.length === 0) {
                delete salas[roomId];
                console.log(`[INFO] Sala ${roomId} foi removida (sem usuários)`);
            } else {
                // Notifica outros usuários da sala
                socket.to(roomId).emit('usuario_saiu', {
                    socketId: socket.id,
                    room: roomId,
                    timestamp: Date.now()
                });
            }
        }
    });
    
    // Evento: Cliente envia mensagem
    socket.on('mensagem', (data) => {
        const { message, room, username, timestamp } = data;
        
        console.log(`\n[MSG] Mensagem na sala ${room}`);
        console.log(`[FROM] ${username} (${socket.id})`);
        console.log(`[TEXT] ${message}`);
        
        // Atualiza contador de mensagens da sala
        if (salas[room]) {
            salas[room].mensagens++;
        }
        
        // Envia a mensagem para todos na sala (incluindo o remetente)
        io.to(room).emit('nova_mensagem', {
            message: message,
            room: room,
            username: username,
            timestamp: timestamp || Date.now(),
            socketId: socket.id
        });
    });
    
    // Evento: Cliente desconecta
    socket.on('disconnect', () => {
        console.log('\n================================');
        console.log('Cliente desconectado!');
        console.log('ID do Socket:', socket.id);
        console.log('Data/Hora:', new Date().toLocaleString());
        
        // Remove o cliente de todas as salas
        for (const roomId in salas) {
            if (salas[roomId].usuarios.includes(socket.id)) {
                salas[roomId].usuarios = salas[roomId].usuarios.filter(id => id !== socket.id);
                
                console.log(`[INFO] Cliente removido da sala ${roomId}`);
                
                // Notifica outros usuários
                socket.to(roomId).emit('usuario_saiu', {
                    socketId: socket.id,
                    room: roomId,
                    timestamp: Date.now()
                });
                
                // Remove sala se vazia
                if (salas[roomId].usuarios.length === 0) {
                    delete salas[roomId];
                    console.log(`[INFO] Sala ${roomId} foi removida`);
                }
            }
        }
        console.log('================================');
    });
    
    // Evento de erro
    socket.on('error', (error) => {
        console.error(`\n[ERROR] Erro no socket ${socket.id}:`, error);
    });
});

// Inicia o servidor
http.listen(PORT, '0.0.0.0', () => {
    console.log('\n╔════════════════════════════════════════════╗');
    console.log('║   SERVIDOR GEOPING SOCKET.IO INICIADO     ║');
    console.log('╚════════════════════════════════════════════╝');
    console.log(`\nServidor rodando em:`);
    console.log(`- Local: http://localhost:${PORT}`);
    console.log(`- Rede: http://SEU_IP:${PORT}`);
    console.log(`\nPara descobrir seu IP local:`);
    console.log(`- Windows: ipconfig`);
    console.log(`- Linux/Mac: ifconfig`);
    console.log(`\nPressione Ctrl+C para parar o servidor\n`);
});

// Tratamento de erros não capturados
process.on('uncaughtException', (error) => {
    console.error('\n[ERRO CRÍTICO] Exceção não capturada:', error);
});

process.on('unhandledRejection', (reason, promise) => {
    console.error('\n[ERRO CRÍTICO] Promise rejeitada não tratada:', reason);
});

