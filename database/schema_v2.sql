-- =====================================================
-- GeoPing v2.0 - Schema de Producao
-- Sistema de Chat baseado em Presenca Indoor
-- =====================================================

-- Tabela de Usuarios
CREATE TABLE IF NOT EXISTS users (
    id SERIAL PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    password_hash TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT NOW(),
    rooms_created_count INT DEFAULT 0 CHECK (rooms_created_count <= 3),
    subscriptions_count INT DEFAULT 0 CHECK (subscriptions_count <= 10)
);

-- Tabela de Salas
CREATE TABLE IF NOT EXISTS rooms (
    id SERIAL PRIMARY KEY,
    room_id VARCHAR(50) UNIQUE NOT NULL, -- ID gerado automaticamente
    room_name VARCHAR(100) NOT NULL, -- Nome visivel pelo usuario
    wifi_ssid VARCHAR(100) NOT NULL UNIQUE, -- Identificador logico (SSID)
    access_code VARCHAR(20) NOT NULL, -- Codigo de acesso
    creator_id INT REFERENCES users(id) ON DELETE CASCADE,
    model_trained BOOLEAN DEFAULT FALSE,
    model_version INT DEFAULT 1,
    last_trained_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT NOW()
);

-- Tabela de Assinaturas/Subscricoes
CREATE TABLE IF NOT EXISTS subscriptions (
    id SERIAL PRIMARY KEY,
    user_id INT REFERENCES users(id) ON DELETE CASCADE,
    room_id INT REFERENCES rooms(id) ON DELETE CASCADE,
    status VARCHAR(20) DEFAULT 'pending', -- pending, approved, rejected
    is_blocked BOOLEAN DEFAULT FALSE, -- Flag de banimento
    subscribed_at TIMESTAMP DEFAULT NOW(),
    approved_at TIMESTAMP,
    UNIQUE(user_id, room_id)
);

-- Tabela de Conversas (Threads)
CREATE TABLE IF NOT EXISTS conversations (
    id SERIAL PRIMARY KEY,
    conversation_id UUID DEFAULT gen_random_uuid(),
    room_id INT REFERENCES rooms(id) ON DELETE CASCADE,
    creator_id INT REFERENCES users(id) ON DELETE SET NULL,
    title VARCHAR(200), -- Opcional: titulo da conversa
    created_at TIMESTAMP DEFAULT NOW()
);

-- Tabela de Mensagens
CREATE TABLE IF NOT EXISTS messages (
    id SERIAL PRIMARY KEY,
    conversation_id UUID REFERENCES conversations(conversation_id) ON DELETE CASCADE,
    sender_id INT REFERENCES users(id) ON DELETE SET NULL,
    content TEXT NOT NULL,
    sent_at TIMESTAMP DEFAULT NOW()
);

-- Tabela de Confirmacoes de Recebimento
CREATE TABLE IF NOT EXISTS message_receipts (
    id SERIAL PRIMARY KEY,
    message_id INT REFERENCES messages(id) ON DELETE CASCADE,
    user_id INT REFERENCES users(id) ON DELETE CASCADE,
    received_at TIMESTAMP DEFAULT NOW(),
    read_at TIMESTAMP,
    UNIQUE(message_id, user_id)
);

-- Tabela de Presenca em Tempo Real
CREATE TABLE IF NOT EXISTS presence (
    id SERIAL PRIMARY KEY,
    user_id INT REFERENCES users(id) ON DELETE CASCADE,
    room_id INT REFERENCES rooms(id) ON DELETE CASCADE,
    status VARCHAR(10) NOT NULL, -- INSIDE, OUTSIDE
    last_fingerprint JSONB,
    reconstruction_error FLOAT,
    last_updated_at TIMESTAMP DEFAULT NOW(),
    UNIQUE(user_id, room_id)
);

-- Indices para Performance
CREATE INDEX IF NOT EXISTS idx_subscriptions_user_room ON subscriptions(user_id, room_id);
CREATE INDEX IF NOT EXISTS idx_subscriptions_status ON subscriptions(status);
CREATE INDEX IF NOT EXISTS idx_messages_conversation ON messages(conversation_id);
CREATE INDEX IF NOT EXISTS idx_messages_sent_at ON messages(sent_at DESC);
CREATE INDEX IF NOT EXISTS idx_presence_user_room ON presence(user_id, room_id);
CREATE INDEX IF NOT EXISTS idx_presence_status ON presence(status, last_updated_at);
CREATE INDEX IF NOT EXISTS idx_conversations_room ON conversations(room_id);
CREATE INDEX IF NOT EXISTS idx_rooms_ssid ON rooms(wifi_ssid);

-- Comentarios
COMMENT ON TABLE users IS 'Usuarios do sistema (max 3 salas criadas, max 10 assinaturas)';
COMMENT ON TABLE rooms IS 'Salas fisicas com modelo ML treinado';
COMMENT ON TABLE subscriptions IS 'Assinaturas de usuarios em salas (pendente/aprovado/bloqueado)';
COMMENT ON TABLE conversations IS 'Threads de conversa dentro de uma sala';
COMMENT ON TABLE messages IS 'Mensagens enviadas em conversas';
COMMENT ON TABLE message_receipts IS 'Confirmacoes de recebimento/leitura de mensagens';
COMMENT ON TABLE presence IS 'Status de presenca em tempo real (INSIDE/OUTSIDE)';

-- View: Usuarios online por sala
CREATE OR REPLACE VIEW users_online_in_room AS
SELECT 
    r.room_id,
    r.room_name,
    u.id as user_id,
    u.username,
    p.status,
    p.last_updated_at
FROM presence p
JOIN rooms r ON p.room_id = r.id
JOIN users u ON p.user_id = u.id
WHERE p.status = 'INSIDE'
  AND p.last_updated_at > NOW() - INTERVAL '30 seconds';

-- View: Estatisticas de salas
CREATE OR REPLACE VIEW room_statistics AS
SELECT 
    r.room_id,
    r.room_name,
    r.wifi_ssid,
    u.username as creator_username,
    COUNT(DISTINCT s.user_id) as total_subscribers,
    COUNT(DISTINCT CASE WHEN s.status = 'approved' THEN s.user_id END) as approved_subscribers,
    COUNT(DISTINCT p.user_id) FILTER (WHERE p.status = 'INSIDE') as users_online,
    r.model_trained,
    r.last_trained_at,
    r.created_at
FROM rooms r
LEFT JOIN users u ON r.creator_id = u.id
LEFT JOIN subscriptions s ON r.id = s.room_id
LEFT JOIN presence p ON r.id = p.room_id AND p.last_updated_at > NOW() - INTERVAL '30 seconds'
GROUP BY r.id, r.room_id, r.room_name, r.wifi_ssid, u.username, r.model_trained, r.last_trained_at, r.created_at;

