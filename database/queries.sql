-- ================================================================================
-- Queries Úteis - GeoPing
-- ================================================================================

-- Ver todas as salas com dados coletados
SELECT 
    room_label,
    COUNT(*) as total_scans,
    COUNT(DISTINCT device_id) as unique_devices,
    MIN(scan_timestamp) as first_scan,
    MAX(scan_timestamp) as last_scan,
    AVG(jsonb_array_length(wifi_fingerprint)) as avg_networks_per_scan
FROM wifi_training_data
GROUP BY room_label
ORDER BY room_label;

-- Ver últimos 10 scans de uma sala específica
SELECT 
    id,
    room_label,
    scan_timestamp,
    device_id,
    jsonb_array_length(wifi_fingerprint) as num_networks
FROM wifi_training_data
WHERE room_label = 'LAB_LESERC'
ORDER BY scan_timestamp DESC
LIMIT 10;

-- Ver detalhes de um scan específico
SELECT 
    id,
    room_label,
    scan_timestamp,
    device_id,
    wifi_fingerprint,
    heuristics
FROM wifi_training_data
WHERE id = 1;

-- Contar quantos BSSIDs únicos foram detectados por sala
SELECT 
    room_label,
    COUNT(DISTINCT (network->>'bssid')) as unique_bssids
FROM wifi_training_data,
     jsonb_array_elements(wifi_fingerprint) as network
GROUP BY room_label;

-- Ver todos os SSIDs únicos detectados em uma sala
SELECT DISTINCT 
    network->>'ssid' as ssid,
    COUNT(*) as appearances,
    AVG((network->>'rssi')::int) as avg_rssi,
    MIN((network->>'rssi')::int) as min_rssi,
    MAX((network->>'rssi')::int) as max_rssi
FROM wifi_training_data,
     jsonb_array_elements(wifi_fingerprint) as network
WHERE room_label = 'LAB_LESERC'
GROUP BY network->>'ssid'
ORDER BY appearances DESC;

-- Ver distribuição de RSSI de um BSSID específico
SELECT 
    (network->>'rssi')::int as rssi,
    COUNT(*) as frequency
FROM wifi_training_data,
     jsonb_array_elements(wifi_fingerprint) as network
WHERE room_label = 'LAB_LESERC'
  AND network->>'bssid' = '00:11:22:33:44:55'
GROUP BY (network->>'rssi')::int
ORDER BY rssi DESC;

-- Deletar todos os dados de uma sala específica
-- DELETE FROM wifi_training_data WHERE room_label = 'SALA_TESTE';

-- Deletar scans mais antigos que uma data
-- DELETE FROM wifi_training_data WHERE scan_timestamp < '2025-01-01';

-- Ver tamanho da tabela
SELECT 
    pg_size_pretty(pg_total_relation_size('wifi_training_data')) as table_size;

-- Ver estatísticas gerais do banco
SELECT 
    COUNT(*) as total_records,
    COUNT(DISTINCT room_label) as total_rooms,
    COUNT(DISTINCT device_id) as total_devices,
    MIN(scan_timestamp) as oldest_scan,
    MAX(scan_timestamp) as newest_scan
FROM wifi_training_data;

-- Backup de dados de uma sala específica (exportar para JSON)
-- \copy (SELECT row_to_json(t) FROM (SELECT * FROM wifi_training_data WHERE room_label = 'LAB_LESERC') t) TO 'backup_lab_leserc.json'

-- Ver índices existentes
SELECT 
    indexname,
    indexdef
FROM pg_indexes
WHERE tablename = 'wifi_training_data';





