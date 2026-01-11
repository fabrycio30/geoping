const express = require('express');
const router = express.Router();
const { authenticateToken } = require('../middleware/auth'); // Importar middleware

/**
 * POST /api/conversations/create
 * Criar uma nova conversa em uma sala
 */
router.post('/conversations/create', authenticateToken, async (req, res) => {
    const { room_id, title } = req.body;
    const creator_id = req.user.userId;

    if (!room_id) {
        return res.status(400).json({
            success: false,
            error: 'room_id é obrigatório'
        });
    }

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

        // [MODO TESTE] Verificação de presença desativada
        // const presenceResult = await pool.query(`
        //     SELECT is_present
        //     FROM presence
        //     WHERE user_id = $1 AND room_id = $2
        //       AND last_seen_at > NOW() - INTERVAL '30 seconds'
        // `, [creator_id, roomIdInternal]);

        // if (presenceResult.rows.length === 0 || !presenceResult.rows[0].is_present) {
        //     return res.status(403).json({
        //         success: false,
        //         error: 'Você precisa estar dentro da sala para criar uma conversa'
        //     });
        // }

        // Criar conversa
        const result = await pool.query(`
            INSERT INTO conversations (room_id, creator_id, title)
            VALUES ($1, $2, $3)
            RETURNING id, conversation_id, room_id, creator_id, title, created_at
        `, [roomIdInternal, creator_id, title || 'Nova Conversa']);

        const conversation = result.rows[0];

        // Buscar username do criador (para devolver no JSON e o Android não ficar 'null')
        const userResult = await pool.query(
            'SELECT username FROM users WHERE id = $1',
            [creator_id]
        );
        const creatorUsername = userResult.rows[0].username;

        // Emitir evento Socket.io
        const io = req.app.get('io');
        // Usar room_id do UUID (que já tem o prefixo room_ se for o padrão do sistema ou uuid puro)
        // O Android envia "room_ff..." no join_room. O banco guarda "room_ff...".
        // Então o canal deve ser EXATAMENTE room_id
        io.to(room_id).emit('new_conversation', {
            conversation_id: conversation.conversation_id,
            title: conversation.title,
            creator_id: conversation.creator_id,
            creatorUsername: creatorUsername, // Adicionado
            created_at: conversation.created_at
        });

        res.status(201).json({
            success: true,
            conversation: {
                ...conversation,
                creator_username: creatorUsername // Adicionado para o client HTTP
            }
        });

    } catch (error) {
        console.error('[CONVERSATIONS] Erro ao criar conversa:', error);
        res.status(500).json({
            success: false,
            error: 'Erro interno do servidor'
        });
    }
});

/**
 * POST /api/messages/send
 * Enviar mensagem em uma conversa
 */
router.post('/messages/send', authenticateToken, async (req, res) => {
    const { conversation_id, content } = req.body;
    const sender_id = req.user.userId;

    if (!conversation_id || !content) {
        return res.status(400).json({
            success: false,
            error: 'conversation_id e content são obrigatórios'
        });
    }

    try {
        const pool = req.app.get('pool');

        // Buscar conversa e sala
        const convResult = await pool.query(`
            SELECT c.id, c.room_id, r.room_id as room_uuid
            FROM conversations c
            JOIN rooms r ON c.room_id = r.id
            WHERE c.conversation_id = $1
        `, [conversation_id]);

        if (convResult.rows.length === 0) {
            return res.status(404).json({
                success: false,
                error: 'Conversa não encontrada'
            });
        }

        const conversation = convResult.rows[0];

        // [MODO TESTE] Verificação de presença desativada
        // const presenceResult = await pool.query(`
        //     SELECT is_present
        //     FROM presence
        //     WHERE user_id = $1 AND room_id = $2
        //       AND last_seen_at > NOW() - INTERVAL '30 seconds'
        // `, [sender_id, conversation.room_id]);

        // if (presenceResult.rows.length === 0 || !presenceResult.rows[0].is_present) {
        //     return res.status(403).json({
        //         success: false,
        //         error: 'Você precisa estar dentro da sala para enviar mensagens'
        //     });
        // }

        // Inserir mensagem
        const result = await pool.query(`
            INSERT INTO messages (conversation_id, sender_id, content)
            VALUES ($1, $2, $3)
            RETURNING id, conversation_id, sender_id, content, sent_at
        `, [conversation.id, sender_id, content]);

        const message = result.rows[0];

        // Buscar informações do remetente
        const userResult = await pool.query(
            'SELECT username FROM users WHERE id = $1',
            [sender_id]
        );

        const messageData = {
            ...message,
            sender_username: userResult.rows[0].username,
            conversation_id: conversation_id
        };

        // Emitir evento Socket.io para sala
        const io = req.app.get('io');
        // Usar room_uuid direto (o ID da sala que o socket entrou)
        io.to(conversation.room_uuid).emit('new_message', messageData);

        res.status(201).json({
            success: true,
            message: messageData
        });

    } catch (error) {
        console.error('[MESSAGES] Erro ao enviar mensagem:', error);
        res.status(500).json({
            success: false,
            error: 'Erro interno do servidor'
        });
    }
});

/**
 * GET /api/messages/:conversation_id
 * Buscar mensagens de uma conversa
 */
router.get('/messages/:conversation_id', authenticateToken, async (req, res) => {
    const { conversation_id } = req.params;
    const { limit = 50, offset = 0 } = req.query;

    try {
        const pool = req.app.get('pool');

        // Buscar conversa
        const convResult = await pool.query(
            'SELECT id FROM conversations WHERE conversation_id = $1',
            [conversation_id]
        );

        if (convResult.rows.length === 0) {
            return res.status(404).json({
                success: false,
                error: 'Conversa não encontrada'
            });
        }

        const conversationIdInternal = convResult.rows[0].id;

        // Buscar mensagens
        const result = await pool.query(`
            SELECT 
                m.id,
                m.sender_id,
                u.username as sender_username,
                m.content,
                m.sent_at
            FROM messages m
            JOIN users u ON m.sender_id = u.id
            WHERE m.conversation_id = $1
            ORDER BY m.sent_at DESC
            LIMIT $2 OFFSET $3
        `, [conversationIdInternal, parseInt(limit), parseInt(offset)]);

        res.json({
            success: true,
            conversation_id: conversation_id,
            message_count: result.rows.length,
            messages: result.rows.reverse() // Ordem cronológica
        });

    } catch (error) {
        console.error('[MESSAGES] Erro ao buscar mensagens:', error);
        res.status(500).json({
            success: false,
            error: 'Erro interno do servidor'
        });
    }
});

/**
 * GET /api/conversations/room/:room_id
 * Listar conversas de uma sala
 */
router.get('/conversations/room/:room_id', authenticateToken, async (req, res) => {
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

        // Buscar conversas
        const result = await pool.query(`
            SELECT 
                c.conversation_id,
                c.title,
                c.creator_id,
                u.username as creator_username,
                c.created_at,
                COUNT(m.id) as message_count
            FROM conversations c
            JOIN users u ON c.creator_id = u.id
            LEFT JOIN messages m ON c.id = m.conversation_id
            WHERE c.room_id = $1
            GROUP BY c.id, c.conversation_id, c.title, c.creator_id, u.username, c.created_at
            ORDER BY c.created_at DESC
        `, [roomIdInternal]);

        res.json({
            success: true,
            room_id: room_id,
            conversation_count: result.rows.length,
            conversations: result.rows
        });

    } catch (error) {
        console.error('[CONVERSATIONS] Erro ao listar conversas:', error);
        res.status(500).json({
            success: false,
            error: 'Erro interno do servidor'
        });
    }
});

module.exports = router;

