const jwt = require('jsonwebtoken');

const JWT_SECRET = process.env.JWT_SECRET || 'geoping_secret_key_2024';

/**
 * Middleware para verificar autenticacao JWT
 */
function authenticateToken(req, res, next) {
    const authHeader = req.headers['authorization'];
    const token = authHeader && authHeader.split(' ')[1]; // Bearer TOKEN

    if (!token) {
        return res.status(401).json({
            success: false,
            error: 'Token de autenticacao nao fornecido'
        });
    }

    jwt.verify(token, JWT_SECRET, (err, user) => {
        if (err) {
            return res.status(403).json({
                success: false,
                error: 'Token invalido ou expirado'
            });
        }

        req.user = user; // { userId, username }
        next();
    });
}

module.exports = { authenticateToken };

