# GeoPing - Plataforma Móvel para Comunicação Delimitada por Proximidade Geográfica


## Visão Geral

GeoPing é um sistema completo para detecção de presença indoor baseado em assinatura de redes Wi-Fi:

- **App Android**:
  - Coleta de dados Wi-Fi (RSSI + BSSID) e interface para treinamento do modelo padrão da sala. Utilização de arquitetura autoencoder para treinamento (One-Class Classification - Dentro ou fora da sala)
  - Aplicação Chat para usuários indoor
- **Backend Node.js**: API REST para persistência de dados e execução de treinamento,  inferencias e chat com usuários indoor.
- **ML Python**: Autoencoder para classificação One-Class (usuário DENTRO ou FORA da sala)
- **Database PostgreSQL**: Armazenamento de fingerprints e modelos treinados (ainda não implementado)

## Tecnologias

### Mobile

- Android Native (Java)

### Backend

- Node.js + Express
- PostgreSQL com JSONB
- Socket.io

### Machine Learning

- Python 3.11
- TensorFlow/Keras (Autoencoder)
- Scikit-Learn (pré-processamento)
- Pandas/NumPy (manipulação de dados)
- Matplotlib (visualização)

## Instalação e Especificações Técnicas

### 1. Database (PostgreSQL)

```bash
# Criar banco e rodar schema
psql -U postgres
CREATE DATABASE geoping_db;
\c geoping_db
\i database/init.sql
```

### 2. Backend

```bash
cd backend
npm install

# Configurar .env
echo "DB_USER=postgres" > .env
echo "DB_PASSWORD=sua_senha" >> .env
echo "DB_HOST=localhost" >> .env
echo "DB_PORT=5432" >> .env
echo "DB_DATABASE=geoping_db" >> .env

npm start
```

### 3. Machine Learning

```bash
cd ml
python -m venv venv
venv\Scripts\activate  # Windows
pip install -r requirements.txt
```

### 4. Android App

1. Abrir pasta `android/` no Android Studio
2. Build & Run
3. Configurar:
   - Nome da sala: `minha_sala`
   - URL do servidor: `http://<IP_DO_PC>:3000`
   - Intervalo: `5000` ms

## Funcionalidades

### Coleta de Dados

- Escaneamento automático de redes Wi-Fi (no mínimo 30 pontos)
- Envio das amostras coletadas para backend/banco de dados
- Log detalhado em tempo real da coleta
- Contador de amostras e redes

### Treinamento

- Botão integrado no app
- Validação de quantidade mínima de amostras (30)
- Logs de treinamento em tempo real
- Exibição de resultados gráficos

### Resultados do Treinamento

- Resumo: amostras, BSSIDs, limiar
- Interpretação: como funciona o modelo e tal
- Gráficos: histórico de loss e distribuição de erros
- Detalhes técnicos: arquitetura, hiperparâmetros, cálculo do limiar

## Arquitetura do Modelo

```
Input (N features) 
    ↓
Encoder: 64 → 32 neurônios
    ↓
Espaco Latente: 16 dimensões
    ↓
Decoder: 32 → 64 neurônios
    ↓
Output (N features)
```

### Hiperparâmetros Padrão

- Épocas: 100
- Batch size: 32
- Validation split: 20%
- Ativação: ReLU
- Otimizador: Adam
- Loss: MSE (Mean Squared Error)

### Threshold

- Método: IQR (Interquartile Range)
- Fórmula: `threshold = Q3 + 1.5 × IQR`
- Robusto contra outliers

## Estrutura do Projeto

```
geoping/
├── android/                # App Android nativo
│   └── app/
│       ├── src/main/
│       │   ├── java/com/geoping/datacollection/
│       │   │   ├── DataCollectionActivity.java
│       │   │   └── TrainingResultsActivity.java
│       │   └── res/layout/
│       │       ├── activity_data_collection.xml
│       │       └── activity_training_results.xml
│       └── build.gradle
│
├── backend/                # API REST
│   ├── server.js          # Express + rotas
│   ├── package.json
│   └── .env               # Configurações (não versionado)
│
├── ml/                    # Machine Learning
│   ├── train_autoencoder.py
│   ├── predict.py
│   ├── requirements.txt
│   └── models/            # Modelos salvos
│
├── database/
│   └── init.sql           # Schema PostgreSQL
│
└── docs/                  # Documentação
```

## Procedimentos para a utilização

### 1. Coleta de Dados

1. Abrir app Android
2. Preencher nome da sala e URL do servidor
3. Clicar em "INICIAR COLETA"
4. Aguardar 30+ coletas
5. Clicar em "PARAR COLETA"

### 2. Treinamento

1. Clicar em "TREINAR MODELO DA SALA"
2. Aguardar processo (exibe logs em tempo real)
3. Visualizar resultados gráficos
4. Salvar o modelo para inferência

### 3. Inferência (Futura)

- Usar `ml/predict.py` para classificar novos scans
- Retorna a decisão: DENTRO ou FORA da sala

## Requisitos

### Hardware

- Android 8.0+ (API 26+)
- Servidor com PostgreSQL (Estamos utilizando um servidr localmente para os testes)

### Software

- Node.js 16+
- Python 3.11
- PostgreSQL 14+
- Android Studio (utilizando para compilar app). Utilizando smartphones para os testes finais dada as limitações dos emuladores.

## Versões

### v2.0 (Atual)

- Sistema com modelos completo de coleta e treinamento
- Integração app-backend-ML
- Exibição de resultados com detalhes técnicos

### v1.0 (Antiga - branch `backup-versao-antiga`)

- Protótipo inicial para os milestones anteriores
- Chat com detecção de salas por Wi-Fi
- Interface básica

## Licença

ainda não definida
