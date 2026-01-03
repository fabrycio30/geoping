# Correção: Imagens do Treinamento Não Encontradas

## 🔴 Problema Identificado

Após treinamento bem-sucedido, o app Android exibia:
```
HTTP 404: Histórico de Treinamento não encontrado
HTTP 404: Erros de Reconstrução não encontrado
```

**Logs mostravam que salvou:**
```
[OK] Grafico salvo: models\F-5.8Ghz_training_history.png
[OK] Grafico salvo: models\F-5.8Ghz_reconstruction_errors.png
```

**Mas os arquivos não existiam em `ml/models/`**

---

## 🔍 Causa Raiz

O script Python estava sendo executado no diretório **`backend/`** ao invés de **`ml/`**.

### Comportamento Incorreto:

```
Diretório de execução: C:\...\geoping_v2\backend
Script Python tenta salvar em: models/F-5.8Ghz_training_history.png
Caminho real tentado: C:\...\geoping_v2\backend\models\F-5.8Ghz_training_history.png
Resultado: ❌ ERRO (pasta backend/models não existe)
```

### Por que isso acontecia?

No `spawn()`, quando não especificamos `cwd` (current working directory), o processo filho herda o diretório de trabalho do processo pai (Node.js), que está em `backend/`.

---

## ✅ Solução Aplicada

Adicionar `cwd` ao `spawn()` para executar o Python no diretório correto:

**Antes:**
```javascript
const pythonProcess = spawn(pythonExecutable, [pythonScript, room_label]);
```

**Depois:**
```javascript
const mlDirectory = path.join(__dirname, '..', 'ml');
const pythonProcess = spawn(pythonExecutable, [pythonScript, room_label], {
    cwd: mlDirectory  // Executar no diretório ml/
});
```

---

## 📁 Comportamento Correto Agora:

```
Diretório de execução: C:\...\geoping_v2\ml
Script Python salva em: models/F-5.8Ghz_training_history.png
Caminho real: C:\...\geoping_v2\ml\models\F-5.8Ghz_training_history.png
Resultado: ✅ SUCESSO
```

---

## 🧪 Como Testar

### 1. Reiniciar Backend
```bash
cd backend
# Ctrl+C para parar
npm start
```

### 2. Limpar Arquivos Antigos (Opcional)
Se quiser começar do zero:
```bash
cd ml/models
# Deletar arquivos de teste antigos se necessário
```

### 3. Treinar Novamente no App
1. Abra o app Android
2. Clique em "TREINAR MODELO DA SALA"
3. Aguarde conclusão

### 4. Verificar Arquivos Salvos
```bash
cd ml/models
dir  # Windows
ls   # Linux/Mac
```

**Deve mostrar:**
```
F-5.8Ghz_autoencoder.h5
F-5.8Ghz_scaler.pkl
F-5.8Ghz_metadata.json
F-5.8Ghz_bssids.json
F-5.8Ghz_training_history.png        ← ✓
F-5.8Ghz_reconstruction_errors.png   ← ✓
```

### 5. Ver Gráficos no App
- App deve abrir automaticamente `TrainingResultsActivity`
- Ambos os gráficos devem carregar corretamente
- **Sem erros HTTP 404**

---

## 📊 Fluxo Correto Completo

```
1. Android: POST /api/train/F-5.8Ghz
2. Backend: spawn Python em ml/
3. Python executa em: C:\...\geoping_v2\ml\
4. Python salva em: models/F-5.8Ghz_*.png
5. Arquivos criados: ml/models/F-5.8Ghz_*.png
6. Backend retorna URLs: /api/training-results/F-5.8Ghz/training_history.png
7. Android faz GET /api/training-results/F-5.8Ghz/training_history.png
8. Backend serve arquivo de: ml/models/F-5.8Ghz_training_history.png
9. Android exibe imagens ✓
```

---

## 🎯 Resultado Esperado

### Logs do Backend:
```
[TREINAMENTO] Iniciando para sala 'F-5.8Ghz' com 76 amostras...
[PYTHON] [OK] Conexao estabelecida...
[PYTHON] [1/5] Pre-processamento dos dados...
[PYTHON] [2/5] Construindo arquitetura...
[PYTHON] [3/5] Treinando o modelo...
[PYTHON] Epoch 100/100
[PYTHON] [4/5] Calculando limiar de decisão...
[PYTHON] [OK] Grafico salvo: models/F-5.8Ghz_training_history.png
[PYTHON] [OK] Grafico salvo: models/F-5.8Ghz_reconstruction_errors.png
[PYTHON] [5/5] Salvando modelo e metadados...
[PYTHON] [OK] Modelo salvo: models/F-5.8Ghz_autoencoder.h5
[PYTHON] TREINAMENTO CONCLUÍDO COM SUCESSO
[TREINAMENTO] Concluído com sucesso para 'F-5.8Ghz'
```

### App Android:
```
Resultados do Treinamento
Sala: F-5.8Ghz

[Gráfico 1: Training History carregado ✓]
[Gráfico 2: Reconstruction Errors carregado ✓]
```

---

## ✅ Arquivo Modificado

- `backend/server.js` (linha ~323)
  - Adicionado `cwd: mlDirectory` ao `spawn()`

---

**Problema resolvido! Agora os arquivos serão salvos no lugar correto.** 🚀

