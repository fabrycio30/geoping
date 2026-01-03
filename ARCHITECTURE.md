# Arquitetura Técnica - GeoPing

## Visão Geral do Sistema

O GeoPing é um sistema de localização indoor que utiliza **Machine Learning** (Autoencoders) para detectar a presença de um usuário em uma sala específica através da análise de sinais Wi-Fi.

## Arquitetura de Alto Nível

```
┌──────────────────────────────────────────────────────────────┐
│                    CAMADA DE APRESENTAÇÃO                     │
│                                                               │
│  ┌─────────────────────────────────────────────────────┐    │
│  │         Android App (Java)                          │    │
│  │  - WifiManager: Escaneamento de redes              │    │
│  │  - UI: Coleta de dados e monitoramento             │    │
│  │  - HTTP Client: Envio de fingerprints              │    │
│  └─────────────────────────────────────────────────────┘    │
└──────────────────────────────────────────────────────────────┘
                              │
                              │ REST API (JSON/HTTP)
                              ▼
┌──────────────────────────────────────────────────────────────┐
│                    CAMADA DE APLICAÇÃO                        │
│                                                               │
│  ┌─────────────────────────────────────────────────────┐    │
│  │         Backend Node.js (Express)                   │    │
│  │  - API REST: Endpoints para coleta e consulta      │    │
│  │  - Validação: Validação de payloads JSON           │    │
│  │  - Logging: Monitoramento de operações             │    │
│  └─────────────────────────────────────────────────────┘    │
└──────────────────────────────────────────────────────────────┘
                              │
                              │ SQL
                              ▼
┌──────────────────────────────────────────────────────────────┐
│                      CAMADA DE DADOS                          │
│                                                               │
│  ┌─────────────────────────────────────────────────────┐    │
│  │         PostgreSQL                                  │    │
│  │  - Tabela: wifi_training_data                      │    │
│  │  - JSONB: Armazenamento flexível de fingerprints  │    │
│  │  - Índices: Otimização de queries                 │    │
│  └─────────────────────────────────────────────────────┘    │
└──────────────────────────────────────────────────────────────┘
                              │
                              │ Python/psycopg2
                              ▼
┌──────────────────────────────────────────────────────────────┐
│                   CAMADA DE MACHINE LEARNING                  │
│                                                               │
│  ┌─────────────────────────────────────────────────────┐    │
│  │         Python (TensorFlow/Keras)                   │    │
│  │  - Treinamento: Autoencoder para cada sala         │    │
│  │  - Inferência: Detecção de anomalias (dentro/fora) │    │
│  │  - Modelos: .h5 (Keras), .pkl (Scaler)            │    │
│  └─────────────────────────────────────────────────────┘    │
└──────────────────────────────────────────────────────────────┘
```

## Fluxo de Dados

### 1. Fase de Coleta (Training)

```
[Android] Scan Wi-Fi
    │
    ├─> Coletar: BSSID, SSID, RSSI de todas as redes
    │
    ├─> Montar JSON:
    │   {
    │     "room_label": "LAB_LESERC",
    │     "device_id": "android_xxx",
    │     "wifi_scan_results": [
    │       {"bssid": "...", "ssid": "...", "rssi": -65}
    │     ]
    │   }
    │
    └─> HTTP POST /api/collect
            │
            ▼
    [Backend] Validar e salvar
            │
            └─> INSERT INTO wifi_training_data
                    │
                    ▼
            [PostgreSQL] Armazenar em JSONB
```

### 2. Fase de Treinamento

```
[Python] train_autoencoder.py
    │
    ├─> SELECT * FROM wifi_training_data WHERE room_label = 'LAB_LESERC'
    │
    ├─> Pré-processamento:
    │   - Extrair todos os BSSIDs únicos (colunas)
    │   - Criar matriz esparsa: [amostras × BSSIDs]
    │   - Normalizar RSSI: [-100, -30] → [0, 1]
    │
    ├─> Construir Autoencoder:
    │   Input (N features)
    │     → Encoder: [64, 32, 16]
    │     → Latent Space: 16
    │     → Decoder: [16, 32, 64]
    │   Output (N features)
    │
    ├─> Treinar:
    │   - Loss: MSE (Mean Squared Error)
    │   - Input = Output (reconstrução)
    │   - Epochs: 100
    │
    ├─> Calcular Limiar (δ):
    │   - Erro de reconstrução para cada amostra
    │   - δ = Q3 + 1.5 × IQR
    │
    └─> Salvar:
        - modelo.h5
        - scaler.pkl
        - metadata.json
        - bssids.json
```

### 3. Fase de Inferência

```
[Python] predict.py
    │
    ├─> Carregar:
    │   - Modelo treinado (.h5)
    │   - Scaler (.pkl)
    │   - Lista de BSSIDs
    │   - Limiar (δ)
    │
    ├─> Receber scan Wi-Fi atual
    │
    ├─> Pré-processar:
    │   - Criar vetor com mesmos BSSIDs do treino
    │   - Normalizar com mesmo scaler
    │
    ├─> Reconstruir:
    │   X_new → Autoencoder → X_reconstructed
    │
    ├─> Calcular erro:
    │   MSE = mean((X_new - X_reconstructed)²)
    │
    └─> Decisão:
        SE MSE < δ:
            DENTRO da sala (normal)
        SENÃO:
            FORA da sala (anomalia)
```

## Modelo de Dados

### Tabela: wifi_training_data

```sql
CREATE TABLE wifi_training_data (
    id SERIAL PRIMARY KEY,
    room_label VARCHAR(100) NOT NULL,
    scan_timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    device_id VARCHAR(100) NOT NULL,
    wifi_fingerprint JSONB NOT NULL,
    heuristics JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

**Exemplo de wifi_fingerprint:**
```json
[
  {
    "bssid": "00:11:22:33:44:55",
    "ssid": "WiFi_Lab",
    "rssi": -65
  },
  {
    "bssid": "AA:BB:CC:DD:EE:FF",
    "ssid": "WiFi_Corredor",
    "rssi": -78
  }
]
```

## Componentes Detalhados

### 1. Android App

**Tecnologias:**
- Java (Android SDK)
- WifiManager (escaneamento)
- OkHttp (networking)

**Responsabilidades:**
- Solicitar permissões de localização
- Escanear redes Wi-Fi periodicamente
- Formatar dados em JSON
- Enviar para backend via HTTP POST
- Exibir logs e estatísticas

**Desafios:**
- Android 10+ requer permissões de localização para Wi-Fi
- Throttling de scans (máx 4 a cada 2 minutos)
- Lidar com Wi-Fi desabilitado

### 2. Backend Node.js

**Tecnologias:**
- Express.js (servidor HTTP)
- pg (cliente PostgreSQL)
- CORS (permitir requisições do Android)

**Responsabilidades:**
- Receber payloads JSON
- Validar estrutura dos dados
- Inserir no banco de dados
- Fornecer APIs de consulta
- Logging de operações

**Endpoints:**
- `POST /api/collect` - Coletar dados
- `GET /api/stats/:room` - Estatísticas
- `GET /api/training-data/:room` - Dados de treino
- `GET /api/rooms` - Listar salas

### 3. PostgreSQL

**Tecnologias:**
- PostgreSQL 12+
- JSONB (dados semi-estruturados)
- Índices GIN (consultas em JSONB)

**Responsabilidades:**
- Persistir dados de treinamento
- Consultas eficientes por sala
- Suportar múltiplos dispositivos
- Histórico temporal

### 4. Machine Learning (Python)

**Tecnologias:**
- TensorFlow/Keras (Autoencoder)
- Scikit-Learn (pré-processamento)
- Pandas (manipulação de dados)
- Matplotlib (visualização)

**Responsabilidades:**
- Carregar dados do PostgreSQL
- Pré-processar fingerprints
- Treinar Autoencoder
- Calcular limiar de decisão
- Realizar inferências

## Fundamentos Teóricos

### Por que Autoencoder?

**Problema:** Distinguir "dentro" vs "fora" da sala

**Solução Tradicional (Falha):**
- Usar limiar de RSSI de um único AP
- Problema: RSSI instável, multicaminho, andares adjacentes

**Nossa Solução:**
- **One-Class Classification**: Treinar apenas com dados positivos ("dentro")
- **Autoencoder**: Aprende a comprimir e reconstruir o padrão normal
- **Detecção de Anomalia**: Se o padrão muda (fora), erro de reconstrução aumenta

### Funcionamento do Autoencoder

1. **Treinamento:**
   - Aprende a "assinatura radioelétrica" da sala
   - Comprime informação no latent space (16 dimensões)
   - Reconstrução próxima do original para dados normais

2. **Inferência:**
   - Dados dentro da sala → baixo erro de reconstrução
   - Dados fora da sala → alto erro de reconstrução
   - Comparar com limiar (δ) para decidir

### Cálculo do Limiar

**Método IQR (Intervalo Interquartil):**

```
Erro = [e1, e2, ..., en] (erros de reconstrução no treino)

Q1 = percentil 25 (Erro)
Q3 = percentil 75 (Erro)
IQR = Q3 - Q1

δ = Q3 + 1.5 × IQR
```

Este método é robusto a outliers e adapta-se à distribuição dos dados.

### Calibração Heurística

**Ideia:** Ajustar δ baseado em características físicas da sala

- Salas grandes → mais variabilidade → δ maior
- Alta densidade de obstáculos → mais atenuação → δ maior
- Múltiplos cômodos → padrões variados → δ maior

**Implementação futura:**
```python
δ_adjusted = δ × calibration_factor(heuristics)
```

## Escalabilidade

### Múltiplas Salas

- Cada sala tem seu próprio modelo (.h5)
- Modelos independentes, treinados separadamente
- Inferência paralela possível

### Múltiplos Usuários

- Coleta de dados pode ser feita por vários dispositivos
- Treino usa todos os dados da sala (agregado)
- Modelo único por sala, funciona para todos

### Produção

Para uso em produção, considere:

1. **Backend:**
   - Adicionar autenticação (JWT)
   - Rate limiting
   - Load balancing
   - Cache (Redis)

2. **ML:**
   - Servir modelo via TensorFlow Serving
   - API REST para inferência
   - Re-treinamento periódico

3. **Mobile:**
   - Implementar Foreground Service
   - Sincronização offline
   - Batching de requests

## Limitações e Considerações

### Limitações

1. **Mudanças no Ambiente:**
   - Roteadores movidos/desligados afetam o modelo
   - Necessário re-treinar periodicamente

2. **Escala do Ambiente:**
   - Funciona melhor em ambientes indoor
   - Densidade de APs é importante

3. **Throttling Android:**
   - Limitação de 4 scans por 2 minutos
   - Impacta tempo real

### Considerações de Privacidade

- BSSIDs são considerados dados sensíveis
- GDPR/LGPD podem aplicar
- Anonimizar dados quando possível
- Consentimento do usuário necessário

## Melhorias Futuras

1. **Multi-Room Classification:**
   - Em vez de One-Class, usar Multi-Class
   - Um modelo reconhece todas as salas

2. **Deep Learning Avançado:**
   - LSTM para sequências temporais
   - CNN para padrões espaciais

3. **Fusão de Sensores:**
   - Combinar Wi-Fi com Bluetooth
   - Adicionar dados de acelerômetro/giroscópio

4. **Transferência de Aprendizado:**
   - Modelo pré-treinado como base
   - Fine-tuning para nova sala

5. **Edge Computing:**
   - Inferência no próprio smartphone
   - TensorFlow Lite para Android

## Referências Técnicas

- **ZeroTouch:** Nikola et al. (2025)
- **Autoencoders:** Goodfellow et al., Deep Learning
- **One-Class SVM:** Schölkopf et al.
- **Wi-Fi Fingerprinting:** Bahl & Padmanabhan, RADAR (2000)

---

**Documento mantido por:** GeoPing Team  
**Última atualização:** Dezembro 2025





