const express = require('express');
const bcrypt = require('bcrypt');
const jwt = require('jsonwebtoken');
const router = express.Router();

// JWT Secret (em producao, colocar no .env)
const JWT_SECRET = process.env.JWT_SECRET || 'geoping_secret_key_2024';

/**
 * POST /api/auth/register
 * Registrar novo usuario
 */
router.post('/register', async (req, res) => {
    const { username, email, password } = req.body;

    // Validacao basica
    if (!username || !email || !password) {
        return res.status(400).json({
            success: false,
            error: 'Campos obrigatorios: username, email, password'
        });
    }

    if (password.length < 6) {
        return res.status(400).json({
            success: false,
            error: 'Senha deve ter pelo menos 6 caracteres'
        });
    }

    try {
        const pool = req.app.get('pool');

        // Verificar se usuario ja existe
        const checkUser = await pool.query(
            'SELECT id FROM users WHERE username = $1 OR email = $2',
            [username, email]
        );

        if (checkUser.rows.length > 0) {
            return res.status(409).json({
                success: false,
                error: 'Usuario ou email ja cadastrado'
            });
        }

        // Hash da senha
        const saltRounds = 10;
        const password_hash = await bcrypt.hash(password, saltRounds);

        // Inserir usuario
        const result = await pool.query(
            `INSERT INTO users (username, email, password_hash)
             VALUES ($1, $2, $3)
             RETURNING id, username, email, created_at`,
            [username, email, password_hash]
        );

        const user = result.rows[0];

        // Gerar token JWT
        const token = jwt.sign(
            { userId: user.id, username: user.username },
            JWT_SECRET,
            { expiresIn: '7d' }
        );

        res.status(201).json({
            success: true,
            message: 'Usuario registrado com sucesso',
            user: {
                id: user.id,
                username: user.username,
                email: user.email,
                created_at: user.created_at
            },
            token
        });

    } catch (error) {
        console.error('Erro ao registrar usuario:', error);
        res.status(500).json({
            success: false,
            error: 'Erro interno do servidor',
            message: error.message
        });
    }
});

/**
 * POST /api/auth/login
 * Login de usuario
 */
router.post('/login', async (req, res) => {
    const { username, password } = req.body;

    if (!username || !password) {
        return res.status(400).json({
            success: false,
            error: 'Campos obrigatorios: username, password'
        });
    }

    try {
        const pool = req.app.get('pool');

        // Buscar usuario
        const result = await pool.query(
            'SELECT id, username, email, password_hash, created_at FROM users WHERE username = $1',
            [username]
        );

        if (result.rows.length === 0) {
            return res.status(401).json({
                success: false,
                error: 'Usuario ou senha incorretos'
            });
        }

        const user = result.rows[0];

        // Verificar senha
        const passwordMatch = await bcrypt.compare(password, user.password_hash);

        if (!passwordMatch) {
            return res.status(401).json({
                success: false,
                error: 'Usuario ou senha incorretos'
            });
        }

        // Gerar token JWT
        const token = jwt.sign(
            { userId: user.id, username: user.username },
            JWT_SECRET,
            { expiresIn: '7d' }
        );

        res.json({
            success: true,
            message: 'Login realizado com sucesso',
            user: {
                id: user.id,
                username: user.username,
                email: user.email,
                created_at: user.created_at
            },
            token
        });

    } catch (error) {
        console.error('Erro ao fazer login:', error);
        res.status(500).json({
            success: false,
            error: 'Erro interno do servidor',
            message: error.message
        });
    }
});

/**
 * GET /api/auth/me
 * Obter dados do usuario autenticado
 */
router.get('/me', async (req, res) => {
    const token = req.headers.authorization?.split(' ')[1];

    if (!token) {
        return res.status(401).json({
            success: false,
            error: 'Token nao fornecido'
        });
    }

    try {
        const decoded = jwt.verify(token, JWT_SECRET);
        const pool = req.app.get('pool');

        const result = await pool.query(
            `SELECT id, username, email, created_at, rooms_created_count, subscriptions_count
             FROM users WHERE id = $1`,
            [decoded.userId]
        );

        if (result.rows.length === 0) {
            return res.status(404).json({
                success: false,
                error: 'Usuario nao encontrado'
            });
        }

        res.json({
            success: true,
            user: result.rows[0]
        });

    } catch (error) {
        if (error.name === 'JsonWebTokenError') {
            return res.status(401).json({
                success: false,
                error: 'Token invalido'
            });
        }

        console.error('Erro ao obter dados do usuario:', error);
        res.status(500).json({
            success: false,
            error: 'Erro interno do servidor'
        });
    }
});

module.exports = router;

