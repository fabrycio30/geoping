# Como Compilar e Instalar o GeoPing

## O Problema

O comando `.\gradlew installDebug` não funciona porque os arquivos do Gradle Wrapper não foram gerados ainda. Isso é normal em projetos novos.

---

## SOLUÇÃO RECOMENDADA: Use o Android Studio

Esta é a forma **mais fácil e rápida**:

### Passo a Passo:

#### 1. Abra o Android Studio

#### 2. Abra o Projeto
- Clique em **File → Open**
- Navegue até: `C:\Users\Willdemarques\Documents\dev\UFMA\semestre_3\geoping`
- Selecione a pasta `geoping` e clique em **OK**

#### 3. Aguarde a Sincronização do Gradle
- O Android Studio irá detectar automaticamente que é um projeto Android
- Vai aparecer uma barra de progresso: "Gradle Sync in progress..."
- **Aguarde terminar** (primeira vez pode demorar 2-5 minutos)
- O Android Studio irá:
  - ✓ Baixar todas as dependências
  - ✓ Criar os arquivos gradlew automaticamente
  - ✓ Configurar o projeto

#### 4. Conecte seu Dispositivo ou Inicie o Emulador

**Opção A - Dispositivo Físico (RECOMENDADO):**
- Conecte o celular via USB
- Ative a Depuração USB no celular
- O dispositivo aparecerá no topo do Android Studio

**Opção B - Emulador:**
- Clique em **Tools → Device Manager**
- Clique no ▶️ (Play) em um emulador
- Aguarde o emulador iniciar

#### 5. Compile e Instale
- Clique no botão verde **▶️ (Run)** no topo
- Ou pressione **Shift + F10**
- Ou vá em **Run → Run 'app'**

#### 6. Pronto!
- O app será compilado
- Instalado automaticamente
- E iniciado no dispositivo

---

## Após Abrir no Android Studio

Depois que o Android Studio sincronizar o Gradle pela primeira vez, você poderá usar os comandos no terminal:

```powershell
# No Windows, use gradlew.bat (com .bat)
.\gradlew.bat assembleDebug

# Ou para compilar e instalar
.\gradlew.bat installDebug

# Ver todas as tarefas disponíveis
.\gradlew.bat tasks
```

---

## Alternativa: Inicializar o Gradle Wrapper

Se preferir usar linha de comando desde o início, você pode usar o Gradle do Android Studio:

```powershell
# Defina o caminho do Gradle do Android Studio (ajuste a versão se necessário)
$env:PATH += ";C:\Program Files\Android\Android Studio\gradle\gradle-8.0\bin"

# Gere o wrapper
gradle wrapper

# Agora pode usar
.\gradlew.bat installDebug
```

---

## Verificando se Funcionou

Após abrir no Android Studio, verifique se foram criados:

- ✓ `gradlew.bat` (Windows)
- ✓ `gradlew` (Linux/Mac)
- ✓ `gradle/wrapper/gradle-wrapper.jar`

Se esses arquivos existirem, você pode usar linha de comando.

---

## Estrutura no Android Studio

Quando abrir o projeto, você verá:

```
GeoPing
├── app
│   ├── manifests
│   │   └── AndroidManifest.xml
│   ├── java
│   │   └── com.geoping
│   │       ├── model
│   │       ├── services
│   │       ├── ui
│   │       └── viewmodel
│   └── res
│       ├── drawable
│       ├── layout
│       └── values
└── Gradle Scripts
    ├── build.gradle (Project)
    └── build.gradle (Module: app)
```

---

## Atalhos Úteis no Android Studio

- **Shift + F10** - Compilar e executar
- **Ctrl + F9** - Compilar projeto
- **Alt + 1** - Abrir/fechar Project Explorer
- **Alt + 6** - Ver Logcat (logs do app)
- **Shift + F6** - Renomear
- **Ctrl + Alt + L** - Formatar código

---

## Troubleshooting

### "SDK location not found"
**Solução**: O arquivo `local.properties` já está configurado. Se der erro:
1. File → Project Structure
2. SDK Location
3. Verifique se aponta para: `C:\Users\Willdemarques\AppData\Local\Android\Sdk`

### "Gradle sync failed"
**Solução**:
1. File → Invalidate Caches / Restart
2. Aguarde reiniciar
3. Build → Clean Project
4. Build → Rebuild Project

### "Unable to resolve dependency"
**Solução**:
- Certifique-se de estar conectado à internet
- O Gradle precisa baixar as dependências

### Demora muito na primeira sincronização
**Isso é normal!** O Gradle está:
- Baixando o Gradle wrapper (~100MB)
- Baixando todas as dependências do Android
- Baixando Socket.IO client
- Indexando o projeto

**Aguarde pacientemente** (pode levar 5-10 minutos na primeira vez).

---

## Depois que Funcionar

Uma vez que o projeto esteja funcionando no Android Studio, você pode:

1. **Desenvolver no Android Studio** (recomendado para Android)
   - Melhor autocomplete
   - Debugging integrado
   - Visualização de layouts
   - Logcat integrado

2. **Editar código no Cursor e compilar no Android Studio**
   - Edite no Cursor (melhor para AI assistance)
   - Compile no Android Studio (melhor para Android)

3. **Usar linha de comando** (após Gradle estar configurado)
   ```powershell
   .\gradlew.bat installDebug
   ```

---

## Próximo Passo AGORA

**Abra o Android Studio e siga os passos acima.**

É a forma mais simples e confiável de compilar um projeto Android pela primeira vez.

Depois que estiver funcionando, você pode usar outros métodos.

---

## Dúvidas Comuns

**P: Preciso sempre usar o Android Studio?**
R: Não! Só na primeira vez para gerar os arquivos do Gradle. Depois pode usar linha de comando.

**P: Posso editar no Cursor?**
R: Sim! Edite onde preferir. O Android Studio é só para compilar/instalar.

**P: O emulador vai funcionar?**
R: Sim, mas lembre-se: escaneamento Wi-Fi NÃO funciona no emulador. Use dispositivo físico para testar a detecção de cercas.

**P: Quanto tempo demora?**
R: Primeira vez: 5-10 minutos. Depois: 30-60 segundos.

---

## Resumo Rápido

```
1. Abra Android Studio
2. File → Open → Selecione a pasta geoping
3. Aguarde Gradle Sync terminar
4. Conecte dispositivo ou inicie emulador
5. Clique em ▶️ Run (ou Shift+F10)
6. Pronto!
```

**É isso! Bem mais simples do que parece.**

