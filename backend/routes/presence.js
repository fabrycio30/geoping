const express = require('express');
const { spawn } = require('child_process');
const path = require('path');
const router = express.Router();

/**
 * POST /api/presence/update
 * Atualizar status de presença do usuário
 * 
 * Body:
 * {
 *   "room_id": "uuid",
 *   "wifi_scan_results": [{"bssid": "xx:xx:xx:xx:xx:xx", "ssid": "Name", "rssi": -50}]
 * }
 */
router.post('/update', async (req, res) => {
    const { room_id, wifi_scan_results } = req.body;
    const user_id = req.userId; // Do middleware de autenticação

    if (!room_id || !wifi_scan_results) {
        return res.status(400).json({
            success: false,
            error: 'room_id e wifi_scan_results são obrigatórios'
        });
    }

    try {
        const pool = req.app.get('pool');

        // Buscar sala pelo room_id
        const roomResult = await pool.query(
            'SELECT id, wifi_ssid, model_trained FROM rooms WHERE room_id = $1',
            [room_id]
        );

        if (roomResult.rows.length === 0) {
            return res.status(404).json({
                success: false,
                error: 'Sala não encontrada'
            });
        }

        const room = roomResult.rows[0];

        if (!room.model_trained) {
            return res.status(400).json({
                success: false,
                error: 'Modelo não treinado para esta sala'
            });
        }

        // Chamar script Python para inferência
        const pythonScript = path.join(__dirname, '..', '..', 'ml', 'predict_realtime.py');
        const pythonExecutable = process.platform === 'win32'
            ? path.join(__dirname, '..', '..', 'ml', 'venv', 'Scripts', 'python.exe')
            : path.join(__dirname, '..', '..', 'ml', 'venv', 'bin', 'python');

        const mlDirectory = path.join(__dirname, '..', '..', 'ml');

        // Preparar entrada JSON para o Python
        const inputData = JSON.stringify({
            room_label: room.wifi_ssid,
            wifi_scan_results: wifi_scan_results
        });

        const pythonProcess = spawn(pythonExecutable, [pythonScript], {
            cwd: mlDirectory
        });

        let outputData = '';
        let errorData = '';

        // Enviar dados via stdin
        pythonProcess.stdin.write(inputData);
        pythonProcess.stdin.end();

        // Capturar stdout
        pythonProcess.stdout.on('data', (data) => {
            outputData += data.toString();
        });

        // Capturar stderr
        pythonProcess.stderr.on('data', (data) => {
            errorData += data.toString();
        });

        // Processar resultado
        pythonProcess.on('close', async (code) => {
            try {
                if (code !== 0) {
                    console.error('[PRESENCE] Erro no Python:', errorData);
                    return res.status(500).json({
                        success: false,
                        error: 'Erro na inferência do modelo',
                        details: errorData
                    });
                }

                // Parsear resultado JSON
                const prediction = JSON.parse(outputData.trim());

                if (!prediction.success) {
                    return res.status(500).json({
                        success: false,
                        error: prediction.error || 'Erro desconhecido na predição'
                    });
                }

                // Atualizar tabela de presença
                const isInside = prediction.inside;
                const confidence = prediction.confidence;

                await pool.query(`
                    INSERT INTO presence (user_id, room_id, is_present, confidence, last_seen_at)
                    VALUES ($1, $2, $3, $4, NOW())
                    ON CONFLICT (user_id, room_id)
                    DO UPDATE SET
                        is_present = $3,
                        confidence = $4,
                        last_seen_at = NOW()
                `, [user_id, room.id, isInside, confidence]);

                console.log(`[PRESENCE] User ${user_id} em sala ${room_id}: ${isInside ? 'INSIDE' : 'OUTSIDE'} (conf: ${confidence})`);

                // Retornar resultado
                res.json({
                    success: true,
                    inside: isInside,
                    confidence: confidence,
                    room_id: room_id,
                    reconstruction_error: prediction.reconstruction_error,
                    threshold: prediction.threshold
                });

            } catch (parseError) {
                console.error('[PRESENCE] Erro ao processar resultado:', parseError);
                res.status(500).json({
                    success: false,
                    error: 'Erro ao processar resultado da inferência',
                    details: outputData
                });
            }
        });

    } catch (error) {
        console.error('[PRESENCE] Erro ao atualizar presença:', error);
        res.status(500).json({
            success: false,
            error: 'Erro interno do servidor'
        });
    }
});

/**
 * GET /api/presence/room/:room_id
 * Listar usuários presentes em uma sala
 */
router.get('/room/:room_id', async (req, res) => {
    const { room_id } = req.params;

    try {
        const pool = req.app.get('pool');

        // Buscar sala
        const roomResult = await pool.query(
            'SELECT id FROM rooms WHERE room_id = $1',
            [room_id]
        );

        if (roomResult.rows.length === 0) {
            return res.status(404).json({
                success: false,
                error: 'Sala não encontrada'
            });
        }

        const roomIdInternal = roomResult.rows[0].id;

        // Buscar usuários presentes (atualizados nos últimos 30 segundos)
        const result = await pool.query(`
            SELECT 
                p.user_id,
                u.username,
                p.is_present,
                p.confidence,
                p.last_seen_at
            FROM presence p
            JOIN users u ON p.user_id = u.id
            WHERE p.room_id = $1
              AND p.is_present = TRUE
              AND p.last_seen_at > NOW() - INTERVAL '30 seconds'
            ORDER BY p.last_seen_at DESC
        `, [roomIdInternal]);

        res.json({
            success: true,
            room_id: room_id,
            user_count: result.rows.length,
            users: result.rows
        });

    } catch (error) {
        console.error('[PRESENCE] Erro ao listar presença:', error);
        res.status(500).json({
            success: false,
            error: 'Erro interno do servidor'
        });
    }
});

/**
 * GET /api/presence/user/:user_id
 * Obter salas onde o usuário está presente
 */
router.get('/user/:user_id', async (req, res) => {
    const { user_id } = req.params;

    try {
        const pool = req.app.get('pool');

        // Buscar salas onde o usuário está presente
        const result = await pool.query(`
            SELECT 
                r.room_id,
                r.room_name,
                r.wifi_ssid,
                p.is_present,
                p.confidence,
                p.last_seen_at
            FROM presence p
            JOIN rooms r ON p.room_id = r.id
            WHERE p.user_id = $1
              AND p.is_present = TRUE
              AND p.last_seen_at > NOW() - INTERVAL '30 seconds'
            ORDER BY p.last_seen_at DESC
        `, [user_id]);

        res.json({
            success: true,
            user_id: parseInt(user_id),
            room_count: result.rows.length,
            rooms: result.rows
        });

    } catch (error) {
        console.error('[PRESENCE] Erro ao buscar presença do usuário:', error);
        res.status(500).json({
            success: false,
            error: 'Erro interno do servidor'
        });
    }
});

module.exports = router;

