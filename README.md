# GeoPing - Sistema de Chat Indoor com Validação de Presença Geográfica

## Visão Geral

O **GeoPing** é uma plataforma móvel de comunicação em tempo real (Chat) que utiliza inteligência artificial para garantir a segurança e a privacidade das conversas. Diferente de chats baseados em GPS (que não funcionam bem em ambientes fechados), o GeoPing utiliza **One-Class Classification (Autoencoder)** baseada em assinaturas de Wi-Fi (Fingerprinting) para determinar se um usuário está fisicamente dentro de uma sala.

O sistema permite que qualquer usuário crie uma sala virtual associada a um espaço físico, treine um modelo neural personalizado para aquele ambiente e gerencie o acesso de outros usuários.

## Principais Funcionalidades

### 1. Sistema de Autenticação e Gestão
*   Login e Registro de usuários.
*   Criação de salas virtuais vinculadas a redes Wi-Fi (SSID).
*   Gestão de assinaturas: Usuários solicitam entrada e o criador aprova/rejeita.
*   **Privilégio de Criador:** O dono da sala tem acesso irrestrito (God Mode) e ferramentas de gerenciamento.

### 2. Inteligência Artificial (Core)
*   **Coleta de Dados:** Interface dedicada para escanear o ambiente e coletar amostras de sinal Wi-Fi (RSSI + BSSID).
*   **Treinamento On-Device/Server:** O usuário inicia o treinamento do modelo diretamente pelo App. O backend processa os dados usando TensorFlow/Keras.
*   **Validação de Presença:**
    *   O sistema utiliza um **Autoencoder** treinado apenas com dados "normais" (dentro da sala).
    *   Durante o chat, o App envia scans Wi-Fi periódicos para o servidor.
    *   O modelo calcula o erro de reconstrução; se for baixo, o usuário está **DENTRO**; se alto, está **FORA**.

### 3. Chat em Tempo Real
*   Comunicação via Socket.io.
*   **Bloqueio Geográfico:** Usuários (exceto o criador) só podem enviar mensagens ou criar tópicos se o sistema validar que estão fisicamente presentes na sala.
*   Feedback visual de presença ("Você está dentro" / "Você está fora") com nível de confiança.
*   Contador de usuários online e presentes na sala.

### 4. Flexibilidade de Desenvolvimento
*   **Configuração Dinâmica de IP:** Permite alterar o endereço do servidor backend diretamente na tela de login, facilitando testes em diferentes redes (Casa, Universidade, Laboratório) sem recompilar o App.

---

## Tecnologias Utilizadas

### Mobile (Android)
*   **Linguagem:** Java (Android Nativo)
*   **Comunicação:** Retrofit (REST API) e Socket.io Client (Real-time)
*   **UI:** Material Design, RecyclerViews, MPAndroidChart (Gráficos de treino)

### Backend
*   **Runtime:** Node.js
*   **Framework:** Express.js
*   **Real-time:** Socket.io
*   **Banco de Dados:** PostgreSQL (com suporte a JSONB para armazenar fingerprints)

### Machine Learning
*   **Linguagem:** Python 3.11
*   **Libraries:** TensorFlow/Keras, Scikit-learn, Pandas, NumPy
*   **Modelo:** Autoencoder (Rede Neural para detecção de anomalias/One-Class Classification)

---

## Guia de Instalação e Execução

### Pré-requisitos
*   Node.js 16+
*   Python 3.11+
*   PostgreSQL 14+
*   Android Studio
*   Dispositivo Android Físico (Recomendado para acesso ao Wi-Fi)

### 1. Banco de Dados
```bash
# Criar banco e rodar o script de inicialização completo
psql -U postgres -c "CREATE DATABASE geoping_db;"
psql -U postgres -d geoping_db -f database/init_complete.sql
```

### 2. Backend
```bash
cd backend
npm install

# Crie um arquivo .env na pasta backend com as configurações do banco:
# DB_USER=postgres
# DB_PASSWORD=sua_senha
# DB_HOST=localhost
# DB_PORT=5432
# DB_DATABASE=geoping_db
# JWT_SECRET=sua_chave_secreta

npm start
# O servidor rodará na porta 3000
```

### 3. Módulo de ML (Python)
```bash
cd ml
python -m venv venv
# Ativar venv:
# Windows: venv\Scripts\activate
# Linux/Mac: source venv/bin/activate

pip install -r requirements.txt
```

### 4. App Android
1.  Abra a pasta `android` no Android Studio.
2.  Conecte seu dispositivo Android na mesma rede Wi-Fi do computador.
3.  Compile e instale o App (`Shift + F10`).
4.  **Configuração Inicial:**
    *   Na tela de Login, identifique o campo "URL do Servidor".
    *   Insira o IP da sua máquina: `http://192.168.X.X:3000`
    *   Clique em ENTRAR.

---

## Fluxo de Utilização (Roteiro de Teste)

1.  **Criar Sala:**
    *   Faça login.
    *   Vá na aba "Minhas Salas" -> Botão "+".
    *   Dê um nome e insira o SSID exato da rede Wi-Fi do local.

2.  **Coletar Dados (Fingerprinting):**
    *   Entre na sala criada -> Clique em "GERENCIAR" -> "Coletar Dados".
    *   Caminhe pelo ambiente enquanto o App coleta as amostras.
    *   Recomenda-se pelo menos 50 a 100 amostras para um bom resultado.

3.  **Treinar Modelo:**
    *   Após a coleta, clique em "TREINAR MODELO".
    *   O servidor processará os dados e gerará o arquivo `.h5` do modelo.
    *   Ao finalizar, o status da sala mudará para "Modelo: Treinado".

4.  **Testar o Chat:**
    *   Volte e clique em "ENTRAR" na sala.
    *   O App começará a validar sua presença.
    *   Se estiver no local (e o modelo reconhecer), o status ficará Verde ("Você está dentro") e o chat será liberado.
    *   Se sair do local, o status ficará Vermelho e o chat será bloqueado.

---

## Estrutura do Projeto

```
geoping/
│
├── android/                 # Código fonte do App Android
│   └── app/src/main/java/com/geoping/app/
│       ├── activities/      # Telas (Login, Chat, Coleta, etc.)
│       ├── adapters/        # Listas (Salas, Mensagens)
│       └── utils/           # Clientes HTTP, Socket e Auth
│
├── backend/                 # Servidor Node.js
│   ├── server.js            # Entry point
│   ├── routes/              # Rotas da API (auth, rooms, presence)
│   └── middleware/          # Autenticação JWT
│
├── database/                # Scripts SQL
│   └── init_complete.sql    # Schema do banco
│
├── ml/                      # Scripts Python
│   ├── train_autoencoder.py # Script de treinamento
│   ├── predict_realtime.py  # Script de inferência (usado pelo backend)
│   └── models/              # Arquivos de modelos salvos (.h5, .json)
│
└── doc/                     # Documentação do projeto
```

## Licença
Projeto desenvolvido para fins acadêmicos - UFMA.
