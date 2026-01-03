-- ================================================================================
-- Script de Inicialização do Banco de Dados - GeoPing
-- Sistema de Localização Indoor usando One-Class Classification (Autoencoder)
-- ================================================================================

-- Criação do banco de dados (descomente se necessário)
-- CREATE DATABASE geo_ping;
-- \c geo_ping;

-- Criação da tabela para armazenar dados brutos de treinamento
CREATE TABLE IF NOT EXISTS wifi_training_data (
    id SERIAL PRIMARY KEY,
    room_label VARCHAR(100) NOT NULL,
    scan_timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    device_id VARCHAR(100) NOT NULL,
    wifi_fingerprint JSONB NOT NULL,
    heuristics JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Índices para melhorar performance das consultas
CREATE INDEX IF NOT EXISTS idx_room_label ON wifi_training_data(room_label);
CREATE INDEX IF NOT EXISTS idx_scan_timestamp ON wifi_training_data(scan_timestamp);
CREATE INDEX IF NOT EXISTS idx_device_id ON wifi_training_data(device_id);

-- Índice GIN para consultas eficientes em JSONB
CREATE INDEX IF NOT EXISTS idx_wifi_fingerprint ON wifi_training_data USING GIN (wifi_fingerprint);
CREATE INDEX IF NOT EXISTS idx_heuristics ON wifi_training_data USING GIN (heuristics);

-- Comentários explicativos
COMMENT ON TABLE wifi_training_data IS 'Tabela de armazenamento de dados de treinamento do fingerprint Wi-Fi para localização indoor';
COMMENT ON COLUMN wifi_training_data.room_label IS 'Identificador da sala/ambiente (ex: LAB_LESERC)';
COMMENT ON COLUMN wifi_training_data.scan_timestamp IS 'Timestamp da coleta do scan Wi-Fi';
COMMENT ON COLUMN wifi_training_data.device_id IS 'Identificador único do dispositivo Android';
COMMENT ON COLUMN wifi_training_data.wifi_fingerprint IS 'Array JSON com objetos contendo {bssid, ssid, rssi} de todas as redes visíveis';
COMMENT ON COLUMN wifi_training_data.heuristics IS 'Dados heurísticos da sala (dimensões, obstáculos, etc.) para calibração do limiar';

-- Exemplo de estrutura esperada para wifi_fingerprint:
-- [
--   {"bssid": "00:11:22:33:44:55", "ssid": "WiFi_Lab", "rssi": -65},
--   {"bssid": "AA:BB:CC:DD:EE:FF", "ssid": "WiFi_Corredor", "rssi": -78}
-- ]

-- Exemplo de estrutura esperada para heuristics:
-- {
--   "room_area": 50.0,
--   "room_dimensions": {"width": 8.0, "length": 6.25, "height": 3.0},
--   "obstacle_density": "high",
--   "num_rooms": 1,
--   "wall_material": "concrete"
-- }

-- Query de exemplo para visualizar os dados
-- SELECT 
--     id,
--     room_label,
--     scan_timestamp,
--     device_id,
--     jsonb_array_length(wifi_fingerprint) as num_networks,
--     wifi_fingerprint,
--     heuristics
-- FROM wifi_training_data
-- ORDER BY scan_timestamp DESC
-- LIMIT 10;





