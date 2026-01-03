# Guia de Início Rápido - GeoPing

Este guia te ajudará a colocar o sistema GeoPing em funcionamento em 15 minutos.

## Pré-requisitos

- PostgreSQL instalado e rodando
- Node.js instalado
- Python 3.8+ instalado
- Smartphone Android
- Todos os dispositivos na mesma rede Wi-Fi

## Passo a Passo Rápido

### 1. Configurar o Banco de Dados (2 minutos)

```bash
# Windows (PowerShell como Admin)
psql -U postgres
CREATE DATABASE geoping;
\c geoping
\i C:\Users\Willdemarques\Documents\dev\UFMA\semestre_3\geoping_v2\database\init.sql
\q
```

### 2. Iniciar o Backend (2 minutos)

```bash
# Em um novo terminal
cd backend
npm install
npm start
```

Você verá:
```
GeoPing Backend - Sistema Indoor RTLS
Servidor rodando em: http://localhost:3000
```

### 3. Descobrir o IP do seu PC (1 minuto)

```bash
# Windows
ipconfig
# Procure por "IPv4 Address" (ex: 192.168.1.100)

# Linux/Mac
ifconfig
# Procure por "inet" (ex: 192.168.1.100)
```

Anote este IP, você vai precisar dele no app Android.

### 4. Instalar o App Android (3 minutos)

Opção A - Via Android Studio:
1. Abra Android Studio
2. File → Open → Selecione a pasta `android/`
3. Conecte seu celular via USB (com depuração USB ativada)
4. Clique em Run

Opção B - Via APK (mais rápido):
```bash
cd android
./gradlew assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk
```

### 5. Coletar Dados (5 minutos)

No app GeoPing:

1. **Nome da Sala:** Digite `SALA_TESTE`
2. **URL do Servidor:** Digite `http://SEU_IP:3000` (ex: `http://192.168.1.100:3000`)
3. **Intervalo de Scan:** Deixe `3`
4. Clique em **INICIAR COLETA**
5. Caminhe pela sala por 5 minutos
6. Clique em **PARAR COLETA**

Você deve ver algo como:
```
Scan #1: 15 redes detectadas
Scan #1 enviado com sucesso
Scan #2: 14 redes detectadas
...
```

### 6. Treinar o Modelo (2 minutos)

```bash
# Em um novo terminal
cd ml

# Instalar dependências (primeira vez apenas)
pip install -r requirements.txt

# Treinar
python train_autoencoder.py SALA_TESTE
```

Você verá o progresso do treinamento:
```
✓ 100 amostras carregadas para a sala 'SALA_TESTE'
→ 25 BSSIDs únicos encontrados
→ Matriz criada: 100 amostras × 25 features
...
Epoch 100/100
...
TREINAMENTO CONCLUÍDO COM SUCESSO
```

### 7. Testar o Modelo (1 minuto)

```bash
python predict.py SALA_TESTE
```

## Verificação Rápida

### Backend está rodando?
```bash
curl http://localhost:3000
```

Deve retornar:
```json
{
  "message": "GeoPing API - Sistema de Localização Indoor",
  "status": "online"
}
```

### Dados foram coletados?
```bash
curl http://localhost:3000/api/stats/SALA_TESTE
```

Deve retornar:
```json
{
  "success": true,
  "data": {
    "room_label": "SALA_TESTE",
    "total_scans": 100
  }
}
```

### Modelo foi criado?
```bash
# Windows
dir ml\models\SALA_TESTE_*

# Linux/Mac
ls -l ml/models/SALA_TESTE_*
```

Deve listar:
- `SALA_TESTE_autoencoder.h5`
- `SALA_TESTE_scaler.pkl`
- `SALA_TESTE_metadata.json`
- `SALA_TESTE_bssids.json`
- `SALA_TESTE_training_history.png`
- `SALA_TESTE_reconstruction_errors.png`

## Problemas Comuns

### "Permissões de localização negadas"
- Vá em Configurações → Apps → GeoPing → Permissões
- Habilite "Localização"

### "Erro ao conectar ao servidor"
- Verifique se o backend está rodando
- Verifique se você usou o IP correto (não use 127.0.0.1)
- Desative firewall temporariamente para testar

### "Nenhum dado encontrado"
- Verifique se a coleta foi bem-sucedida no app
- Verifique os logs do backend
- Teste a API: `curl http://localhost:3000/api/stats/SALA_TESTE`

### "Modelo com baixa acurácia"
- Colete mais dados (mínimo 100 amostras)
- Colete em diferentes posições da sala
- Aumente o intervalo de coleta

## Próximos Passos

1. Colete dados de outra sala e treine um segundo modelo
2. Integre o modelo com o backend para predições em tempo real
3. Ajuste os hiperparâmetros em `train_autoencoder.py`
4. Adicione heurísticas da sala para calibração do limiar

## Dicas

- Quanto mais dados, melhor o modelo
- Colete em diferentes horários do dia
- Colete com diferentes pessoas na sala
- Mantenha o smartphone na mesma posição/altura durante a coleta
- Evite mover o roteador Wi-Fi após treinar o modelo

---

Para mais detalhes, consulte o [README.md](README.md) completo.





