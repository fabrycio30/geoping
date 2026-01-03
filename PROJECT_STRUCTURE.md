# Estrutura do Projeto GeoPing

## Árvore de Arquivos

```
geoping_v2/
│
├── README.md                    # Documentação principal completa
├── QUICKSTART.md               # Guia de início rápido (15 min)
├── ARCHITECTURE.md             # Documentação técnica detalhada
├── PROJECT_STRUCTURE.md        # Este arquivo
├── .gitignore                  # Arquivos a ignorar no Git
├── test_system.py              # Script de teste do sistema
├── prompt.md                   # Especificação original do projeto
│
├── database/                   # Camada de Dados
│   ├── init.sql               # Script de criação do banco
│   └── queries.sql            # Queries úteis para análise
│
├── backend/                   # Camada de Aplicação (Node.js)
│   ├── server.js             # Servidor Express com API REST
│   ├── package.json          # Dependências Node.js
│   └── .env.example          # Template de configuração
│
├── android/                   # Camada de Apresentação (Mobile)
│   └── app/
│       ├── build.gradle                      # Configuração do Gradle
│       └── src/main/
│           ├── AndroidManifest.xml           # Configuração do app e permissões
│           ├── java/com/geoping/datacollection/
│           │   └── DataCollectionActivity.java  # Activity principal
│           └── res/
│               ├── layout/
│               │   └── activity_data_collection.xml  # Layout da UI
│               └── values/
│                   ├── strings.xml           # Strings do app
│                   ├── colors.xml            # Paleta de cores
│                   └── themes.xml            # Tema do app
│
└── ml/                        # Camada de Machine Learning (Python)
    ├── train_autoencoder.py  # Script de treinamento do modelo
    ├── predict.py            # Script de inferência/predição
    ├── utils.py              # Funções auxiliares e visualizações
    ├── requirements.txt      # Dependências Python
    ├── .env.example          # Template de configuração
    └── models/               # Diretório para modelos treinados (criado automaticamente)
        ├── SALA_*_autoencoder.h5           # Modelo Keras treinado
        ├── SALA_*_scaler.pkl               # Normalizador (pickle)
        ├── SALA_*_metadata.json            # Metadados do modelo
        ├── SALA_*_bssids.json              # Lista de BSSIDs conhecidos
        ├── SALA_*_training_history.png     # Gráfico de loss
        └── SALA_*_reconstruction_errors.png # Histograma de erros
```

## Descrição dos Componentes

### Documentação (5 arquivos)

| Arquivo | Propósito |
|---------|-----------|
| `README.md` | Documentação completa com instalação, uso e API |
| `QUICKSTART.md` | Guia rápido de 15 minutos para iniciar |
| `ARCHITECTURE.md` | Arquitetura técnica detalhada do sistema |
| `PROJECT_STRUCTURE.md` | Visão geral da estrutura (este arquivo) |
| `prompt.md` | Especificação original do projeto |

### Database (2 arquivos)

| Arquivo | Propósito |
|---------|-----------|
| `database/init.sql` | Cria tabela `wifi_training_data` com JSONB |
| `database/queries.sql` | Queries úteis para análise dos dados |

### Backend (3 arquivos)

| Arquivo | Propósito |
|---------|-----------|
| `backend/server.js` | API REST com 5 endpoints |
| `backend/package.json` | Dependências: express, pg, cors |
| `backend/.env.example` | Template de configuração do DB |

### Android (7 arquivos)

| Arquivo | Propósito |
|---------|-----------|
| `AndroidManifest.xml` | Permissões e configuração do app |
| `DataCollectionActivity.java` | Lógica de escaneamento Wi-Fi (650 linhas) |
| `activity_data_collection.xml` | Interface visual com Material Design |
| `build.gradle` | Dependências: OkHttp, CardView |
| `strings.xml` | Textos localizáveis |
| `colors.xml` | Paleta de cores do app |
| `themes.xml` | Tema visual |

### Machine Learning (4 arquivos)

| Arquivo | Propósito |
|---------|-----------|
| `ml/train_autoencoder.py` | Pipeline completo de treinamento (500 linhas) |
| `ml/predict.py` | Classe de inferência em tempo real |
| `ml/utils.py` | Visualizações e análises auxiliares |
| `ml/requirements.txt` | Dependências: tensorflow, pandas, sklearn |

### Utilitários (2 arquivos)

| Arquivo | Propósito |
|---------|-----------|
| `.gitignore` | Ignora node_modules, venv, modelos, etc |
| `test_system.py` | Testa DB, Backend e dependências ML |

## Tamanho dos Arquivos (Linhas de Código)

```
Componente                     | Linhas de Código
-------------------------------|------------------
Android (Java)                 | ~650 linhas
Backend (Node.js)              | ~200 linhas
ML - Treinamento (Python)      | ~500 linhas
ML - Inferência (Python)       | ~150 linhas
ML - Utils (Python)            | ~250 linhas
Database (SQL)                 | ~150 linhas
Documentação (Markdown)        | ~1000 linhas
-------------------------------|------------------
TOTAL                          | ~2900 linhas
```

## Fluxo de Trabalho

### 1. Configuração Inicial

```
1. database/init.sql           → Criar banco de dados
2. backend/package.json        → npm install
3. ml/requirements.txt         → pip install
4. android/build.gradle        → Sincronizar Gradle
```

### 2. Desenvolvimento

```
Backend:  backend/server.js         → Rodar servidor
Android:  DataCollectionActivity.java → Instalar app
ML:       train_autoencoder.py      → Treinar modelo
```

### 3. Testes

```
test_system.py                 → Verificar componentes
database/queries.sql           → Analisar dados coletados
ml/utils.py                    → Visualizar resultados
```

## Dependências Externas

### Backend (Node.js)

```json
{
  "express": "^4.18.2",      // Servidor HTTP
  "pg": "^8.11.3",           // Cliente PostgreSQL
  "cors": "^2.8.5",          // Cross-Origin Resource Sharing
  "dotenv": "^16.3.1"        // Variáveis de ambiente
}
```

### Android

```gradle
- androidx.appcompat:appcompat:1.6.1
- com.google.android.material:material:1.9.0
- androidx.cardview:cardview:1.0.0
- com.squareup.okhttp3:okhttp:4.11.0
```

### Machine Learning (Python)

```
numpy>=1.24.0              # Computação numérica
pandas>=2.0.0              # Manipulação de dados
tensorflow>=2.13.0         # Deep Learning
scikit-learn>=1.3.0        # Pré-processamento
psycopg2-binary>=2.9.0     # Cliente PostgreSQL
matplotlib>=3.7.0          # Visualização
```

## Artefatos Gerados

Após treinar um modelo para uma sala (ex: `LAB_LESERC`), os seguintes arquivos são criados em `ml/models/`:

1. `LAB_LESERC_autoencoder.h5` - Modelo treinado (Keras HDF5)
2. `LAB_LESERC_scaler.pkl` - Normalizador MinMaxScaler (Pickle)
3. `LAB_LESERC_metadata.json` - Metadados:
   ```json
   {
     "room_label": "LAB_LESERC",
     "bssids": [...],
     "threshold": 0.0234,
     "num_samples": 250,
     "num_bssids": 35
   }
   ```
4. `LAB_LESERC_bssids.json` - Lista ordenada de BSSIDs conhecidos
5. `LAB_LESERC_training_history.png` - Gráfico de loss durante treino
6. `LAB_LESERC_reconstruction_errors.png` - Histograma de erros + limiar

## Portas Utilizadas

| Serviço | Porta | Configurável |
|---------|-------|--------------|
| PostgreSQL | 5432 | Sim (DB_PORT) |
| Backend Node.js | 3000 | Sim (PORT) |

## Dados Armazenados

### PostgreSQL

- **Tabela:** `wifi_training_data`
- **Tamanho estimado:** ~5 KB por amostra (depende do número de redes)
- **Exemplo:** 250 amostras ≈ 1.25 MB

### Modelos ML

- **Modelo .h5:** ~50-200 KB (depende do número de BSSIDs)
- **Scaler .pkl:** ~5-10 KB
- **Metadados .json:** ~2-20 KB

## Checklist de Arquivos Criados

- [x] Documentação completa (README, QUICKSTART, ARCHITECTURE)
- [x] Scripts SQL (init, queries)
- [x] Backend Node.js completo
- [x] Android App completo (Java + XML)
- [x] Pipeline ML completo (train, predict, utils)
- [x] Arquivos de configuração (package.json, requirements.txt, build.gradle)
- [x] Utilitários (test_system, .gitignore)

## Próximos Passos Recomendados

1. **Testar o sistema:** `python test_system.py`
2. **Coletar dados:** Usar o app Android
3. **Treinar modelo:** `python ml/train_autoencoder.py SALA_TESTE`
4. **Validar resultados:** Verificar gráficos gerados em `ml/models/`
5. **Integração:** Conectar inferência com backend para uso em produção

---

**Status do Projeto:** Completo e pronto para uso  
**Última atualização:** Dezembro 2025





