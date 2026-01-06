-- ================================================================================
-- MIGRAÇÃO: Corrigir Tabela presence
-- ================================================================================
-- Este script corrige a estrutura da tabela presence para corresponder
-- ao código do backend que foi atualizado.
--
-- Execute este script no PostgreSQL:
-- psql -U postgres -d geoping -f database/migration_fix_presence.sql
-- ================================================================================

-- Passo 1: Remover a tabela antiga
DROP TABLE IF EXISTS presence CASCADE;

-- Passo 2: Criar tabela com estrutura correta
CREATE TABLE presence (
    user_id INT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    room_id INT NOT NULL REFERENCES rooms(id) ON DELETE CASCADE,
    is_present BOOLEAN NOT NULL DEFAULT FALSE,
    confidence FLOAT NOT NULL DEFAULT 0.0,
    last_seen_at TIMESTAMP NOT NULL DEFAULT NOW(),
    PRIMARY KEY (user_id, room_id)
);

-- Passo 3: Criar índices para melhor performance
CREATE INDEX idx_presence_user ON presence(user_id);
CREATE INDEX idx_presence_room ON presence(room_id);
CREATE INDEX idx_presence_last_seen ON presence(last_seen_at);
CREATE INDEX idx_presence_is_present ON presence(is_present);

-- Comentários
COMMENT ON TABLE presence IS 'Registra a presença atual dos usuários nas salas';
COMMENT ON COLUMN presence.is_present IS 'TRUE se o usuário está dentro da sala, FALSE se está fora';
COMMENT ON COLUMN presence.confidence IS 'Nível de confiança da predição (0.0 a 1.0)';
COMMENT ON COLUMN presence.last_seen_at IS 'Última vez que a presença foi atualizada';

-- Verificação
SELECT 
    'Tabela presence criada com sucesso!' AS status,
    COUNT(*) AS total_colunas
FROM information_schema.columns 
WHERE table_name = 'presence';

SELECT column_name, data_type 
FROM information_schema.columns 
WHERE table_name = 'presence'
ORDER BY ordinal_position;

