# Guia de Instalação - Windows

Este guia detalha a instalação do GeoPing no Windows passo a passo.

## Pré-requisitos

Antes de começar, certifique-se de ter instalado:

1. **PostgreSQL** - https://www.postgresql.org/download/windows/
2. **Node.js** - https://nodejs.org/ (versão LTS recomendada)
3. **Python** - https://www.python.org/downloads/ (3.8 ou superior)
4. **Android Studio** (opcional, para desenvolvimento) - https://developer.android.com/studio

## Passo 1: Configurar PostgreSQL

### 1.1 Instalar PostgreSQL

1. Baixe o instalador do PostgreSQL para Windows
2. Execute o instalador
3. Durante a instalação:
   - Defina a senha do usuário `postgres` (anote essa senha!)
   - Mantenha a porta padrão `5432`
   - Instale o pgAdmin 4 (ferramenta gráfica útil)

### 1.2 Criar o Banco de Dados

**Opção A - Via linha de comando:**

```powershell
# Abrir PowerShell como Administrador

# Navegar até a pasta do PostgreSQL (ajuste o caminho se necessário)
cd "C:\Program Files\PostgreSQL\15\bin"

# Conectar ao PostgreSQL
.\psql.exe -U postgres

# Dentro do psql, executar:
CREATE DATABASE geoping;
\c geoping
\i 'C:\Users\Willdemarques\Documents\dev\UFMA\semestre_3\geoping_v2\database\init.sql'
\q
```

**Opção B - Via pgAdmin 4:**

1. Abra pgAdmin 4
2. Conecte ao servidor local (senha definida na instalação)
3. Clique com botão direito em "Databases" → "Create" → "Database"
4. Nome: `geoping`
5. Clique em "Save"
6. Clique com botão direito em `geoping` → "Query Tool"
7. Abra o arquivo `database/init.sql` e execute

### 1.3 Verificar Instalação

```powershell
psql -U postgres -d geoping -c "SELECT COUNT(*) FROM wifi_training_data;"
```

Deve retornar `0` (tabela vazia, mas existente).

## Passo 2: Configurar Backend Node.js

### 2.1 Verificar Node.js

```powershell
node --version  # Deve mostrar v16.x ou superior
npm --version   # Deve mostrar 8.x ou superior
```

Se não estiver instalado, baixe de https://nodejs.org/

### 2.2 Instalar Dependências

```powershell
# Navegar até a pasta do backend
cd C:\Users\Willdemarques\Documents\dev\UFMA\semestre_3\geoping_v2\backend

# Instalar dependências
npm install
```

### 2.3 Configurar Variáveis de Ambiente (Opcional)

Crie um arquivo `.env` na pasta `backend/`:

```
DB_USER=postgres
DB_PASSWORD=sua_senha_aqui
DB_NAME=geoping
DB_HOST=localhost
DB_PORT=5432
PORT=3000
```

### 2.4 Iniciar o Servidor

```powershell
npm start
```

Você deve ver:

```
GeoPing Backend - Sistema Indoor RTLS
Servidor rodando em: http://localhost:3000
```

**Deixe este terminal aberto!** O servidor precisa estar rodando.

### 2.5 Testar o Backend

Abra outro PowerShell e execute:

```powershell
curl http://localhost:3000
```

Ou abra no navegador: http://localhost:3000

Deve retornar um JSON com status "online".

## Passo 3: Configurar Machine Learning (Python)

### 3.1 Verificar Python

```powershell
python --version  # Deve mostrar 3.8 ou superior
pip --version
```

Se não estiver instalado, baixe de https://www.python.org/downloads/

**IMPORTANTE:** Durante a instalação do Python, marque a opção "Add Python to PATH".

### 3.2 Criar Ambiente Virtual

```powershell
# Navegar até a pasta ml
cd C:\Users\Willdemarques\Documents\dev\UFMA\semestre_3\geoping_v2\ml

# Criar ambiente virtual
python -m venv venv

# Ativar ambiente virtual
.\venv\Scripts\Activate.ps1
```

**Nota:** Se aparecer erro de política de execução, execute:

```powershell
Set-ExecutionPolicy -ExecutionPolicy RemoteSigned -Scope CurrentUser
```

Depois tente ativar novamente.

### 3.3 Instalar Dependências Python

```powershell
# Com o ambiente virtual ativado (deve aparecer (venv) no prompt)
pip install -r requirements.txt
```

Isso pode demorar alguns minutos, especialmente o TensorFlow.

### 3.4 Configurar Variáveis de Ambiente (Opcional)

Crie um arquivo `.env` na pasta `ml/`:

```
DB_USER=postgres
DB_PASSWORD=sua_senha_aqui
DB_NAME=geoping
DB_HOST=localhost
DB_PORT=5432
```

## Passo 4: Configurar Aplicativo Android

### 4.1 Instalar Android Studio

1. Baixe de https://developer.android.com/studio
2. Execute o instalador
3. Siga o assistente de instalação
4. Instale os componentes recomendados (SDK, emulador, etc.)

### 4.2 Abrir o Projeto

1. Abra o Android Studio
2. File → Open
3. Navegue até `C:\Users\Willdemarques\Documents\dev\UFMA\semestre_3\geoping_v2\android`
4. Clique em "OK"
5. Aguarde o Gradle sincronizar (pode demorar na primeira vez)

### 4.3 Configurar Dispositivo

**Opção A - Dispositivo Real (Recomendado):**

1. Habilite "Opções do Desenvolvedor" no seu Android:
   - Configurações → Sobre o telefone
   - Toque 7 vezes em "Número da versão"
2. Habilite "Depuração USB":
   - Configurações → Sistema → Opções do desenvolvedor
   - Ative "Depuração USB"
3. Conecte o celular ao PC via USB
4. Autorize a depuração quando solicitado no celular

**Opção B - Emulador:**

1. No Android Studio: Tools → Device Manager
2. Clique em "Create Device"
3. Selecione um dispositivo (ex: Pixel 4)
4. Selecione uma imagem do sistema (API 30 ou superior)
5. Clique em "Finish"

### 4.4 Compilar e Instalar

1. No Android Studio, clique em "Run" (ícone de play verde)
2. Selecione o dispositivo (real ou emulador)
3. Aguarde a compilação e instalação

## Passo 5: Descobrir o IP do PC

O smartphone precisa se conectar ao backend rodando no seu PC. Para isso, você precisa do **IP local** do seu PC (não use localhost/127.0.0.1).

```powershell
ipconfig
```

Procure por "Adaptador de Rede sem Fio Wi-Fi" → "Endereço IPv4"

Exemplo: `192.168.1.100`

**Importante:** PC e smartphone devem estar na **mesma rede Wi-Fi**.

## Passo 6: Liberar Porta no Firewall do Windows

O firewall do Windows pode bloquear conexões externas na porta 3000.

### 6.1 Criar Regra no Firewall

```powershell
# Abrir PowerShell como Administrador

# Criar regra de entrada
New-NetFirewallRule -DisplayName "GeoPing Backend" -Direction Inbound -LocalPort 3000 -Protocol TCP -Action Allow
```

### 6.2 Verificar Conexão do Smartphone

No navegador do smartphone, acesse:

```
http://SEU_IP:3000
```

Substitua `SEU_IP` pelo IP descoberto no Passo 5 (ex: `http://192.168.1.100:3000`)

Deve aparecer a mensagem do GeoPing.

## Passo 7: Testar o Sistema

### 7.1 Executar Script de Teste

```powershell
# Na raiz do projeto
cd C:\Users\Willdemarques\Documents\dev\UFMA\semestre_3\geoping_v2

python test_system.py
```

Todos os testes devem passar (✓).

### 7.2 Coletar Dados de Teste

No app Android:

1. **Nome da Sala:** `TESTE_WINDOWS`
2. **URL do Servidor:** `http://SEU_IP:3000` (use o IP do Passo 5)
3. **Intervalo:** `3`
4. Clique em **INICIAR COLETA**
5. Deixe rodando por 3-5 minutos (mínimo 50 scans)
6. Clique em **PARAR COLETA**

### 7.3 Verificar Dados Coletados

```powershell
# Via API
curl http://localhost:3000/api/stats/TESTE_WINDOWS

# Via PostgreSQL
psql -U postgres -d geoping -c "SELECT COUNT(*) FROM wifi_training_data WHERE room_label = 'TESTE_WINDOWS';"
```

### 7.4 Treinar Modelo

```powershell
cd C:\Users\Willdemarques\Documents\dev\UFMA\semestre_3\geoping_v2\ml

# Ativar ambiente virtual
.\venv\Scripts\Activate.ps1

# Treinar
python train_autoencoder.py TESTE_WINDOWS
```

Deve exibir o progresso do treinamento e criar arquivos em `ml/models/`.

### 7.5 Testar Predição

```powershell
python predict.py TESTE_WINDOWS
```

## Troubleshooting Windows

### Erro: "psql não é reconhecido"

**Solução:** Adicione o PostgreSQL ao PATH:

1. Abra "Variáveis de Ambiente"
2. Em "Variáveis do Sistema", edite "Path"
3. Adicione: `C:\Program Files\PostgreSQL\15\bin`
4. Reinicie o PowerShell

### Erro: "node não é reconhecido"

**Solução:** Reinstale o Node.js marcando a opção "Add to PATH".

### Erro: "python não é reconhecido"

**Solução:** Reinstale o Python marcando "Add Python to PATH".

### Erro: "cannot be loaded because running scripts is disabled"

**Solução:**

```powershell
Set-ExecutionPolicy -ExecutionPolicy RemoteSigned -Scope CurrentUser
```

### Erro de conexão do Android com o backend

**Checklist:**
1. Backend está rodando? (`npm start`)
2. Firewall liberou porta 3000?
3. PC e celular na mesma rede Wi-Fi?
4. Usou o IP correto (não 127.0.0.1)?
5. Teste no navegador do celular primeiro

### Erro: "TensorFlow não encontrado"

**Solução:**

```powershell
# Ativar venv
cd ml
.\venv\Scripts\Activate.ps1

# Reinstalar TensorFlow
pip install --upgrade tensorflow
```

### Backend não inicia (erro de conexão com PostgreSQL)

**Solução:**
1. Verifique se o PostgreSQL está rodando:
   - Serviços do Windows → PostgreSQL
2. Verifique senha em `.env` ou use a padrão no código
3. Teste conexão: `psql -U postgres -d geoping`

## Scripts Úteis

### Iniciar Backend (Desenvolvimento)

Crie `start_backend.bat`:

```batch
@echo off
cd C:\Users\Willdemarques\Documents\dev\UFMA\semestre_3\geoping_v2\backend
npm start
pause
```

### Iniciar Treinamento ML

Crie `train_model.bat`:

```batch
@echo off
cd C:\Users\Willdemarques\Documents\dev\UFMA\semestre_3\geoping_v2\ml
call venv\Scripts\activate.bat
python train_autoencoder.py %1
pause
```

Uso: `train_model.bat LAB_LESERC`

## Próximos Passos

Após a instalação bem-sucedida:

1. Consulte o [QUICKSTART.md](QUICKSTART.md) para uso rápido
2. Leia o [README.md](README.md) para documentação completa
3. Explore [ARCHITECTURE.md](ARCHITECTURE.md) para entender a arquitetura

## Suporte

Se encontrar problemas:

1. Verifique se todas as dependências estão instaladas
2. Execute `python test_system.py`
3. Consulte a seção Troubleshooting acima
4. Verifique os logs do backend e do Android

---

**Boa sorte com seu projeto GeoPing!**





