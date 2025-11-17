# Guia Rápido - GeoPing

## Passo a Passo para Executar o Projeto

### 1. Configuração Inicial

#### Configure o IP do Servidor Socket.IO
Abra `app/src/main/java/com/geoping/services/SocketManager.java` e altere:
```java
private static final String SERVER_URL = "http://SEU_IP_AQUI:3000";
```

#### Configure a Cerca Digital
Abra `app/src/main/java/com/geoping/services/WifiProximityService.java` e altere se necessário:
```java
private static final String TARGET_SSID = "GP_Lab";  // Nome da sua rede Wi-Fi
```

### 2. Compilar o Projeto no Cursor

Abra o terminal integrado no Cursor (Ctrl+`) e execute:

```powershell
# Navegue até a pasta do projeto (se não estiver nela)
cd C:\Users\Willdemarques\Documents\dev\UFMA\semestre_3\geoping

# Compile o projeto
.\gradlew assembleDebug
```

### 3. Executar no Emulador

#### Iniciando o Emulador do Android Studio:

1. Abra o Android Studio
2. Vá em **Tools → Device Manager**
3. Clique em "Play" no emulador desejado
4. Aguarde o emulador iniciar completamente

#### Instalando o App no Emulador:

No terminal do Cursor:
```powershell
# Verifique se o emulador está conectado
adb devices

# Instale o app
.\gradlew installDebug
```

### 4. Executar em Dispositivo Físico (Recomendado)

O escaneamento Wi-Fi NÃO funciona no emulador. Use um dispositivo real:

1. **Habilite o Modo Desenvolvedor** no Android:
   - Configurações → Sobre o telefone
   - Toque 7 vezes em "Número da compilação"

2. **Ative a Depuração USB**:
   - Configurações → Opções do desenvolvedor
   - Ative "Depuração USB"

3. **Conecte o dispositivo via USB**

4. **Instale o app**:
```powershell
adb devices
.\gradlew installDebug
```

### 5. Configurar Servidor Socket.IO

Crie um arquivo `server.js`:

```javascript
const express = require('express');
const app = express();
const http = require('http').createServer(app);
const io = require('socket.io')(http);

io.on('connection', (socket) => {
    console.log('Cliente conectado:', socket.id);
    
    socket.on('join_room', (data) => {
        socket.join(data.room);
        console.log(`Cliente entrou na sala ${data.room}`);
    });
    
    socket.on('leave_room', (data) => {
        socket.leave(data.room);
        console.log(`Cliente saiu da sala ${data.room}`);
    });
    
    socket.on('mensagem', (data) => {
        io.to(data.room).emit('nova_mensagem', data);
    });
    
    socket.on('disconnect', () => {
        console.log('Cliente desconectado');
    });
});

http.listen(3000, '0.0.0.0', () => {
    console.log('Servidor rodando na porta 3000');
});
```

Instale as dependências e execute:
```bash
npm install express socket.io
node server.js
```

### 6. Testando o Aplicativo

1. **Inicie o servidor Socket.IO** no seu computador
2. **Descubra seu IP local**:
   - Windows: `ipconfig` (procure por IPv4)
   - O IP será algo como `192.168.1.100`
3. **Configure o IP no SocketManager.java** e recompile
4. **Instale o app no dispositivo**
5. **Abra o app e conceda as permissões** solicitadas
6. **Aproxime-se da rede Wi-Fi** "GP_Lab"
7. **O app deve automaticamente entrar na sala** quando detectar o Wi-Fi

### Estrutura de Comandos Úteis

```powershell
# Compilar
.\gradlew assembleDebug

# Instalar
.\gradlew installDebug

# Compilar e instalar
.\gradlew build installDebug

# Limpar build
.\gradlew clean

# Ver logs em tempo real
adb logcat | Select-String "GeoPing"

# Ver logs do SocketManager
adb logcat | Select-String "SocketManager"

# Ver logs do WifiProximityService
adb logcat | Select-String "WifiProximityService"

# Desinstalar o app
adb uninstall com.geoping
```

### Troubleshooting Rápido

**Erro: "SDK location not found"**
- Crie um arquivo `local.properties` na raiz do projeto com:
```
sdk.dir=C\:\\Users\\SEU_USUARIO\\AppData\\Local\\Android\\Sdk
```

**Erro: "Permission denied"**
- No app, vá em Configurações do Android
- Apps → GeoPing → Permissões
- Conceda todas as permissões

**App não detecta Wi-Fi**
- Certifique-se de estar usando um dispositivo físico
- Verifique se o Wi-Fi está ligado
- Confirme que a rede "GP_Lab" está disponível

**Não conecta ao servidor**
- Verifique o IP no SocketManager.java
- Confirme que o servidor está rodando
- Dispositivo e servidor devem estar na mesma rede

## Usando o Cursor para Desenvolvimento Android

### Extensões Recomendadas

Embora o Cursor já tenha bom suporte para Java, você pode instalar:

1. **Extension Pack for Java** (Microsoft)
2. **Android iOS Emulator** (para facilitar visualização)
3. **Gradle Language Support**

Para instalar, pressione `Ctrl+Shift+X` e busque pelos nomes acima.

### Terminal Integrado

Use `Ctrl+` ` para abrir o terminal integrado do Cursor.

### Navegação Rápida

- `Ctrl+P`: Buscar arquivo
- `Ctrl+Shift+F`: Buscar em todos os arquivos
- `F12`: Ir para definição
- `Ctrl+Click`: Ir para definição

### Debug com Cursor

O Cursor não tem debug integrado para Android. Use:
- **Logs**: `Log.d(TAG, "mensagem")`
- **Android Studio**: Para debugging avançado
- **ADB Logcat**: Para ver logs em tempo real

## Próximos Passos

Após conseguir executar o projeto:

1. Teste o chat com múltiplos dispositivos
2. Experimente alterar os limiares de sinal
3. Adicione novas cercas digitais
4. Customize a interface
5. Implemente melhorias sugeridas no README.md

## Dicas de Desenvolvimento

- Sempre compile antes de instalar
- Use `gradlew clean` se encontrar erros estranhos
- Monitore os logs para debug
- Teste em dispositivo físico para Wi-Fi
- Mantenha o servidor Socket.IO rodando durante os testes

