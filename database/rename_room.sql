-- Script para renomear sala no banco de dados
-- GeoPing - Sistema de Localização Indoor

-- Ver quantos registros existem com o nome atual
SELECT 
    room_label,
    COUNT(*) as total_registros
FROM wifi_training_data
WHERE room_label LIKE '%Fabr%'
GROUP BY room_label;

-- Renomear de 'Casa Fabrício' para 'casa fabricio'
-- ATENÇÃO: Execute esta query apenas UMA vez!
-- Descomente a linha abaixo para executar:

-- UPDATE wifi_training_data SET room_label = 'casa fabricio' WHERE room_label = 'Casa Fabrício';

-- Verificar se funcionou
-- SELECT room_label, COUNT(*) FROM wifi_training_data GROUP BY room_label;





