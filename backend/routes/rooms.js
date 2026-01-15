const express = require('express');
const { v4: uuidv4 } = require('uuid');
const { authenticateToken } = require('../middleware/auth');
const router = express.Router();

// Gerar codigo de acesso aleatorio
function generateAccessCode() {
    return Math.random().toString(36).substring(2, 10).toUpperCase();
}

/**
 * POST /api/rooms/create
 * Criar nova sala (requer autenticacao)
 */
router.post('/create', authenticateToken, async (req, res) => {
    const { room_name, wifi_ssid } = req.body;
    const creator_id = req.user.userId;

    if (!room_name || !wifi_ssid) {
        return res.status(400).json({
            success: false,
            error: 'Campos obrigatorios: room_name, wifi_ssid'
        });
    }

    try {
        const pool = req.app.get('pool');

        // Verificar limite de salas criadas
        const userCheck = await pool.query(
            'SELECT rooms_created_count FROM users WHERE id = $1',
            [creator_id]
        );

        if (userCheck.rows[0].rooms_created_count >= 3) {
            return res.status(403).json({
                success: false,
                error: 'Limite de 3 salas criadas atingido'
            });
        }

        // Verificar se SSID ja esta em uso
        const ssidCheck = await pool.query(
            'SELECT id FROM rooms WHERE wifi_ssid = $1',
            [wifi_ssid]
        );

        if (ssidCheck.rows.length > 0) {
            return res.status(409).json({
                success: false,
                error: 'Ja existe uma sala com esse SSID'
            });
        }

        // Gerar room_id e access_code
        const room_id = `room_${uuidv4().substring(0, 8)}`;
        const access_code = generateAccessCode();

        // Criar sala
        const result = await pool.query(
            `INSERT INTO rooms (room_id, room_name, wifi_ssid, access_code, creator_id)
             VALUES ($1, $2, $3, $4, $5)
             RETURNING id, room_id, room_name, wifi_ssid, access_code, created_at`,
            [room_id, room_name, wifi_ssid, access_code, creator_id]
        );

        // Incrementar contador de salas criadas
        await pool.query(
            'UPDATE users SET rooms_created_count = rooms_created_count + 1 WHERE id = $1',
            [creator_id]
        );

        // Auto-assinar o criador na sala
        await pool.query(
            `INSERT INTO subscriptions (user_id, room_id, status)
             VALUES ($1, $2, 'approved')`,
            [creator_id, result.rows[0].id]
        );

        res.status(201).json({
            success: true,
            message: 'Sala criada com sucesso',
            room: result.rows[0]
        });

    } catch (error) {
        console.error('Erro ao criar sala:', error);
        res.status(500).json({
            success: false,
            error: 'Erro interno do servidor',
            message: error.message
        });
    }
});

/**
 * GET /api/rooms/search?query=nome
 * Buscar salas por nome (requer autenticacao)
 */
router.get('/search', authenticateToken, async (req, res) => {
    const { query } = req.query;

    if (!query) {
        return res.status(400).json({
            success: false,
            error: 'Parametro query obrigatorio'
        });
    }

    try {
        const pool = req.app.get('pool');

        const result = await pool.query(
            `SELECT 
                r.id, r.room_id, r.room_name, r.wifi_ssid, 
                r.model_trained, r.created_at,
                u.username as creator_username,
                COUNT(DISTINCT s.user_id) FILTER (WHERE s.status = 'approved') as subscriber_count
             FROM rooms r
             LEFT JOIN users u ON r.creator_id = u.id
             LEFT JOIN subscriptions s ON r.id = s.room_id
             WHERE LOWER(r.room_name) LIKE LOWER($1) 
                OR LOWER(r.wifi_ssid) LIKE LOWER($1)
             GROUP BY r.id, r.room_id, r.room_name, r.wifi_ssid, r.model_trained, r.created_at, u.username
             ORDER BY r.created_at DESC
             LIMIT 20`,
            [`%${query}%`]
        );

        res.json({
            success: true,
            rooms: result.rows
        });

    } catch (error) {
        console.error('Erro ao buscar salas:', error);
        res.status(500).json({
            success: false,
            error: 'Erro interno do servidor'
        });
    }
});

/**
 * GET /api/rooms/my-rooms
 * Listar salas criadas pelo usuario (requer autenticacao)
 */
router.get('/my-rooms', authenticateToken, async (req, res) => {
    const creator_id = req.user.userId;

    try {
        const pool = req.app.get('pool');

        const result = await pool.query(
            `SELECT 
                r.id, r.room_id, r.room_name, r.wifi_ssid, r.access_code,
                r.model_trained, r.last_trained_at, r.created_at,
                COUNT(DISTINCT s.user_id) FILTER (WHERE s.status = 'approved') as subscriber_count,
                COUNT(DISTINCT s.user_id) FILTER (WHERE s.status = 'pending') as pending_count
             FROM rooms r
             LEFT JOIN subscriptions s ON r.id = s.room_id
             WHERE r.creator_id = $1
             GROUP BY r.id
             ORDER BY r.created_at DESC`,
            [creator_id]
        );

        res.json({
            success: true,
            rooms: result.rows
        });

    } catch (error) {
        console.error('Erro ao listar salas:', error);
        res.status(500).json({
            success: false,
            error: 'Erro interno do servidor'
        });
    }
});

/**
 * GET /api/rooms/my-subscriptions
 * Listar salas que o usuario esta inscrito (requer autenticacao)
 */
router.get('/my-subscriptions', authenticateToken, async (req, res) => {
    const user_id = req.user.userId;

    try {
        const pool = req.app.get('pool');

        const result = await pool.query(
            `SELECT 
                r.id, r.room_id, r.room_name, r.wifi_ssid,
                r.model_trained, r.created_at,
                u.username as creator_username,
                s.status as subscription_status,
                s.is_blocked,
                s.subscribed_at,
                (SELECT COUNT(*) FROM subscriptions s2 WHERE s2.room_id = r.id AND s2.status = 'approved') as subscriber_count
             FROM subscriptions s
             JOIN rooms r ON s.room_id = r.id
             LEFT JOIN users u ON r.creator_id = u.id
             WHERE s.user_id = $1
             ORDER BY s.subscribed_at DESC`,
            [user_id]
        );

        res.json({
            success: true,
            subscriptions: result.rows
        });

    } catch (error) {
        console.error('Erro ao listar subscricoes:', error);
        res.status(500).json({
            success: false,
            error: 'Erro interno do servidor'
        });
    }
});

/**
 * POST /api/rooms/subscribe
 * Solicitar assinatura em uma sala (requer autenticacao)
 */
router.post('/subscribe', authenticateToken, async (req, res) => {
    const { room_id } = req.body;
    const user_id = req.user.userId;

    if (!room_id) {
        return res.status(400).json({
            success: false,
            error: 'Campo room_id obrigatorio'
        });
    }

    try {
        const pool = req.app.get('pool');

        // Verificar se sala existe
        const roomCheck = await pool.query(
            'SELECT id, room_name, creator_id FROM rooms WHERE room_id = $1',
            [room_id]
        );

        if (roomCheck.rows.length === 0) {
            return res.status(404).json({
                success: false,
                error: 'Sala nao encontrada'
            });
        }

        const room = roomCheck.rows[0];

        // Verificar se usuario eh o criador
        if (room.creator_id === user_id) {
            return res.status(400).json({
                success: false,
                error: 'Voce ja eh o criador desta sala'
            });
        }

        // Verificar limite de assinaturas
        const userCheck = await pool.query(
            'SELECT subscriptions_count FROM users WHERE id = $1',
            [user_id]
        );

        if (userCheck.rows[0].subscriptions_count >= 10) {
            return res.status(403).json({
                success: false,
                error: 'Limite de 10 assinaturas atingido'
            });
        }

        // Verificar se ja existe assinatura
        const subCheck = await pool.query(
            'SELECT id, status FROM subscriptions WHERE user_id = $1 AND room_id = $2',
            [user_id, room.id]
        );

        if (subCheck.rows.length > 0) {
            const existingStatus = subCheck.rows[0].status;
            return res.status(409).json({
                success: false,
                error: `Ja existe uma solicitacao com status: ${existingStatus}`
            });
        }

        // Criar solicitacao de assinatura
        const result = await pool.query(
            `INSERT INTO subscriptions (user_id, room_id, status)
             VALUES ($1, $2, 'pending')
             RETURNING id, subscribed_at`,
            [user_id, room.id]
        );

        res.status(201).json({
            success: true,
            message: 'Solicitacao de assinatura enviada',
            subscription: {
                id: result.rows[0].id,
                room_id: room_id,
                room_name: room.room_name,
                status: 'pending',
                subscribed_at: result.rows[0].subscribed_at
            }
        });

    } catch (error) {
        console.error('Erro ao solicitar assinatura:', error);
        res.status(500).json({
            success: false,
            error: 'Erro interno do servidor'
        });
    }
});

/**
 * GET /api/rooms/:room_id/pending-subscriptions
 * Listar solicitacoes pendentes de uma sala (apenas criador)
 */
router.get('/:room_id/pending-subscriptions', authenticateToken, async (req, res) => {
    const { room_id } = req.params;
    const user_id = req.user.userId;

    try {
        const pool = req.app.get('pool');

        // Verificar se usuario eh o criador
        const roomCheck = await pool.query(
            'SELECT id, creator_id FROM rooms WHERE room_id = $1',
            [room_id]
        );

        if (roomCheck.rows.length === 0) {
            return res.status(404).json({
                success: false,
                error: 'Sala nao encontrada'
            });
        }

        if (roomCheck.rows[0].creator_id !== user_id) {
            return res.status(403).json({
                success: false,
                error: 'Apenas o criador pode ver solicitacoes pendentes'
            });
        }

        // Buscar solicitacoes pendentes
        const result = await pool.query(
            `SELECT 
                s.id, s.subscribed_at,
                u.id as user_id, u.username, u.email
             FROM subscriptions s
             JOIN users u ON s.user_id = u.id
             WHERE s.room_id = $1 AND s.status = 'pending'
             ORDER BY s.subscribed_at ASC`,
            [roomCheck.rows[0].id]
        );

        res.json({
            success: true,
            pending_subscriptions: result.rows
        });

    } catch (error) {
        console.error('Erro ao listar solicitacoes pendentes:', error);
        res.status(500).json({
            success: false,
            error: 'Erro interno do servidor'
        });
    }
});

/**
 * POST /api/rooms/approve-subscription
 * Aprovar ou rejeitar solicitacao de assinatura (apenas criador)
 */
router.post('/approve-subscription', authenticateToken, async (req, res) => {
    const { subscription_id, approve, access_code } = req.body; // approve: true/false
    const creator_id = req.user.userId;

    if (!subscription_id || approve === undefined) {
        return res.status(400).json({
            success: false,
            error: 'Campos obrigatorios: subscription_id, approve'
        });
    }

    try {
        const pool = req.app.get('pool');

        // Buscar assinatura e verificar se usuario eh criador
        const subCheck = await pool.query(
            `SELECT s.id, s.user_id, s.room_id, r.creator_id, r.access_code
             FROM subscriptions s
             JOIN rooms r ON s.room_id = r.id
             WHERE s.id = $1`,
            [subscription_id]
        );

        if (subCheck.rows.length === 0) {
            return res.status(404).json({
                success: false,
                error: 'Solicitacao nao encontrada'
            });
        }

        const subscription = subCheck.rows[0];

        if (subscription.creator_id !== creator_id) {
            return res.status(403).json({
                success: false,
                error: 'Apenas o criador pode aprovar/rejeitar solicitacoes'
            });
        }

        // Atualizar status
        const newStatus = approve ? 'approved' : 'rejected';
        await pool.query(
            `UPDATE subscriptions 
             SET status = $1, approved_at = NOW()
             WHERE id = $2`,
            [newStatus, subscription_id]
        );

        // Se aprovado, incrementar contador de assinaturas do usuario
        if (approve) {
            await pool.query(
                'UPDATE users SET subscriptions_count = subscriptions_count + 1 WHERE id = $1',
                [subscription.user_id]
            );
        }

        res.json({
            success: true,
            message: approve ? 'Assinatura aprovada' : 'Assinatura rejeitada',
            access_code: approve ? subscription.access_code : null
        });

    } catch (error) {
        console.error('Erro ao aprovar/rejeitar assinatura:', error);
        res.status(500).json({
            success: false,
            error: 'Erro interno do servidor'
        });
    }
});

/**
 * POST /api/rooms/:room_id/block-user
 * Bloquear/desbloquear usuario de uma sala (apenas criador)
 */
router.post('/:room_id/block-user', authenticateToken, async (req, res) => {
    const { room_id } = req.params;
    const { user_id, block } = req.body; // block: true/false
    const creator_id = req.user.userId;

    if (!user_id || block === undefined) {
        return res.status(400).json({
            success: false,
            error: 'Campos obrigatorios: user_id, block'
        });
    }

    try {
        const pool = req.app.get('pool');

        // Verificar se usuario autenticado eh o criador
        const roomCheck = await pool.query(
            'SELECT id, creator_id FROM rooms WHERE room_id = $1',
            [room_id]
        );

        if (roomCheck.rows.length === 0) {
            return res.status(404).json({
                success: false,
                error: 'Sala nao encontrada'
            });
        }

        if (roomCheck.rows[0].creator_id !== creator_id) {
            return res.status(403).json({
                success: false,
                error: 'Apenas o criador pode bloquear usuarios'
            });
        }

        // Atualizar flag de bloqueio
        const result = await pool.query(
            `UPDATE subscriptions
             SET is_blocked = $1
             WHERE room_id = $2 AND user_id = $3 AND status = 'approved'
             RETURNING id`,
            [block, roomCheck.rows[0].id, user_id]
        );

        if (result.rows.length === 0) {
            return res.status(404).json({
                success: false,
                error: 'Usuario nao esta inscrito nesta sala'
            });
        }

        res.json({
            success: true,
            message: block ? 'Usuario bloqueado' : 'Usuario desbloqueado'
        });

    } catch (error) {
        console.error('Erro ao bloquear/desbloquear usuario:', error);
        res.status(500).json({
            success: false,
            error: 'Erro interno do servidor'
        });
    }
});

/**
 * POST /api/rooms/:room_id/update-model
 * Marcar sala como treinada apos treino do modelo
 */
router.post('/:room_id/update-model', authenticateToken, async (req, res) => {
    const { room_id } = req.params;
    const creator_id = req.user.userId;

    try {
        const pool = req.app.get('pool');

        // Verificar se usuario eh criador
        const roomCheck = await pool.query(
            'SELECT id, creator_id FROM rooms WHERE room_id = $1',
            [room_id]
        );

        if (roomCheck.rows.length === 0) {
            return res.status(404).json({
                success: false,
                error: 'Sala nao encontrada'
            });
        }

        if (roomCheck.rows[0].creator_id !== creator_id) {
            return res.status(403).json({
                success: false,
                error: 'Apenas o criador pode retreinar o modelo'
            });
        }

        // Atualizar status do modelo
        await pool.query(
            `UPDATE rooms
             SET model_trained = true,
                 model_version = model_version + 1,
                 last_trained_at = NOW()
             WHERE id = $1`,
            [roomCheck.rows[0].id]
        );

        res.json({
            success: true,
            message: 'Modelo atualizado com sucesso'
        });

    } catch (error) {
        console.error('Erro ao atualizar modelo:', error);
        res.status(500).json({
            success: false,
            error: 'Erro interno do servidor'
        });
    }
});

/**
 * DELETE /api/rooms/:room_id
 * Deletar uma sala (apenas o criador pode deletar)
 */
router.delete('/:room_id', authenticateToken, async (req, res) => {
    const { room_id } = req.params;
    const user_id = req.user.userId;

    try {
        const pool = req.app.get('pool');

        // Verificar se a sala existe e se o usuario e o criador
        const roomResult = await pool.query(
            'SELECT id, creator_id, room_name FROM rooms WHERE room_id = $1',
            [room_id]
        );

        if (roomResult.rows.length === 0) {
            return res.status(404).json({
                success: false,
                error: 'Sala nao encontrada'
            });
        }

        const room = roomResult.rows[0];

        if (room.creator_id !== user_id) {
            return res.status(403).json({
                success: false,
                error: 'Apenas o criador pode deletar esta sala'
            });
        }

        // Deletar sala (CASCADE vai deletar subscriptions, conversations, messages automaticamente)
        await pool.query('DELETE FROM rooms WHERE room_id = $1', [room_id]);

        // Decrementar contador de salas criadas pelo usuario
        await pool.query(
            'UPDATE users SET rooms_created_count = GREATEST(0, rooms_created_count - 1) WHERE id = $1',
            [user_id]
        );

        console.log(`Sala deletada: ${room.room_name} (ID: ${room_id}) por usuario ${user_id}`);

        res.json({
            success: true,
            message: 'Sala deletada com sucesso'
        });

    } catch (error) {
        console.error('Erro ao deletar sala:', error);
        res.status(500).json({
            success: false,
            error: 'Erro interno do servidor'
        });
    }
});

// Rota para buscar detalhes completos da sala (Gerenciamento)
router.get('/:room_id/details', authenticateToken, async (req, res) => {
    const { room_id } = req.params;
    const user_id = req.user.userId;

    try {
        const pool = req.app.get('pool');

        // 1. Buscar dados básicos da sala
        const roomResult = await pool.query(
            'SELECT * FROM rooms WHERE room_id = $1',
            [room_id]
        );

        if (roomResult.rows.length === 0) {
            return res.status(404).json({ error: 'Sala não encontrada' });
        }

        const room = roomResult.rows[0];

        // Verificar permissão (apenas criador ou inscritos aprovados)
        const subResult = await pool.query(
            'SELECT status FROM subscriptions WHERE room_id = $1 AND user_id = $2',
            [room.id, user_id]
        );

        if (room.creator_id !== user_id && (subResult.rows.length === 0 || subResult.rows[0].status !== 'approved')) {
            return res.status(403).json({ error: 'Acesso negado' });
        }

        // 2. Buscar inscritos e status online
        // Online = is_present=TRUE e last_seen_at < 45 segundos atrás
        const subscribersQuery = `
            SELECT 
                u.id, 
                u.username, 
                s.status,
                p.is_present,
                p.confidence,
                p.last_seen_at,
                CASE 
                    WHEN p.is_present = TRUE AND p.last_seen_at > NOW() - INTERVAL '45 seconds' THEN TRUE 
                    ELSE FALSE 
                END as is_online
            FROM subscriptions s
            JOIN users u ON s.user_id = u.id
            LEFT JOIN presence p ON p.user_id = u.id AND p.room_id = s.room_id
            WHERE s.room_id = $1 AND s.status = 'approved'
            ORDER BY is_online DESC, u.username ASC
        `;
        
        const subscribersResult = await pool.query(subscribersQuery, [room.id]);

        // 3. Buscar metadados do modelo (se treinado)
        let modelInfo = null;
        if (room.model_trained) {
            try {
                const fs = require('fs');
                const path = require('path');
                const metadataPath = path.join(__dirname, '..', '..', 'ml', 'models', `${room.wifi_ssid}_metadata.json`);
                
                if (fs.existsSync(metadataPath)) {
                    const metadata = JSON.parse(fs.readFileSync(metadataPath, 'utf8'));
                    modelInfo = {
                        threshold: metadata.threshold,
                        training_date: metadata.training_date,
                        num_samples: metadata.num_samples,
                        accuracy: metadata.accuracy // se tiver
                    };
                }
            } catch (e) {
                console.error('Erro ao ler metadados do modelo:', e);
            }
        }

        res.json({
            room: {
                id: room.id,
                name: room.room_name,
                ssid: room.wifi_ssid,
                code: room.access_code,
                model_trained: room.model_trained,
                last_trained_at: room.last_trained_at,
                subscribers_count: subscribersResult.rows.length
            },
            subscribers: subscribersResult.rows,
            model_info: modelInfo
        });

    } catch (error) {
        console.error('Erro ao buscar detalhes da sala:', error);
        res.status(500).json({ error: 'Erro interno do servidor' });
    }
});

module.exports = router;

