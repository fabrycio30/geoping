-- =====================================================
-- GeoPing v2.0 - Schema Completo
-- Executa: init.sql + schema_v2.sql
-- =====================================================

-- Schema original (tabela de treinamento)
CREATE TABLE IF NOT EXISTS wifi_training_data (
    id SERIAL PRIMARY KEY,
    room_label VARCHAR(100) NOT NULL,
    scan_timestamp TIMESTAMP DEFAULT NOW(),
    device_id VARCHAR(100),
    wifi_fingerprint JSONB NOT NULL,
    heuristics JSONB
);

CREATE INDEX IF NOT EXISTS idx_room_label ON wifi_training_data(room_label);
CREATE INDEX IF NOT EXISTS idx_scan_timestamp ON wifi_training_data(scan_timestamp);

-- Schema v2 (usuarios, salas, chat)
CREATE TABLE IF NOT EXISTS users (
    id SERIAL PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    password_hash TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT NOW(),
    rooms_created_count INT DEFAULT 0 CHECK (rooms_created_count <= 3),
    subscriptions_count INT DEFAULT 0 CHECK (subscriptions_count <= 10)
);

CREATE TABLE IF NOT EXISTS rooms (
    id SERIAL PRIMARY KEY,
    room_id VARCHAR(50) UNIQUE NOT NULL,
    room_name VARCHAR(100) NOT NULL,
    wifi_ssid VARCHAR(100) NOT NULL UNIQUE,
    access_code VARCHAR(20) NOT NULL,
    creator_id INT REFERENCES users(id) ON DELETE CASCADE,
    model_trained BOOLEAN DEFAULT FALSE,
    model_version INT DEFAULT 1,
    last_trained_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS subscriptions (
    id SERIAL PRIMARY KEY,
    user_id INT REFERENCES users(id) ON DELETE CASCADE,
    room_id INT REFERENCES rooms(id) ON DELETE CASCADE,
    status VARCHAR(20) DEFAULT 'pending',
    is_blocked BOOLEAN DEFAULT FALSE,
    subscribed_at TIMESTAMP DEFAULT NOW(),
    approved_at TIMESTAMP,
    UNIQUE(user_id, room_id)
);

CREATE TABLE IF NOT EXISTS conversations (
    id SERIAL PRIMARY KEY,
    conversation_id UUID DEFAULT gen_random_uuid(),
    room_id INT REFERENCES rooms(id) ON DELETE CASCADE,
    creator_id INT REFERENCES users(id) ON DELETE SET NULL,
    title VARCHAR(200),
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS messages (
    id SERIAL PRIMARY KEY,
    conversation_id UUID REFERENCES conversations(conversation_id) ON DELETE CASCADE,
    sender_id INT REFERENCES users(id) ON DELETE SET NULL,
    content TEXT NOT NULL,
    sent_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS message_receipts (
    id SERIAL PRIMARY KEY,
    message_id INT REFERENCES messages(id) ON DELETE CASCADE,
    user_id INT REFERENCES users(id) ON DELETE CASCADE,
    received_at TIMESTAMP DEFAULT NOW(),
    read_at TIMESTAMP,
    UNIQUE(message_id, user_id)
);

CREATE TABLE IF NOT EXISTS presence (
    user_id INT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    room_id INT NOT NULL REFERENCES rooms(id) ON DELETE CASCADE,
    is_present BOOLEAN NOT NULL DEFAULT FALSE,
    confidence FLOAT NOT NULL DEFAULT 0.0,
    last_seen_at TIMESTAMP NOT NULL DEFAULT NOW(),
    PRIMARY KEY (user_id, room_id)
);

-- Indices
CREATE INDEX IF NOT EXISTS idx_subscriptions_user_room ON subscriptions(user_id, room_id);
CREATE INDEX IF NOT EXISTS idx_subscriptions_status ON subscriptions(status);
CREATE INDEX IF NOT EXISTS idx_messages_conversation ON messages(conversation_id);
CREATE INDEX IF NOT EXISTS idx_messages_sent_at ON messages(sent_at DESC);
CREATE INDEX IF NOT EXISTS idx_presence_user ON presence(user_id);
CREATE INDEX IF NOT EXISTS idx_presence_room ON presence(room_id);
CREATE INDEX IF NOT EXISTS idx_presence_last_seen ON presence(last_seen_at);
CREATE INDEX IF NOT EXISTS idx_presence_is_present ON presence(is_present);
CREATE INDEX IF NOT EXISTS idx_conversations_room ON conversations(room_id);
CREATE INDEX IF NOT EXISTS idx_rooms_ssid ON rooms(wifi_ssid);

