# GeoPing - Cercas Digitais via Wi-Fi

## Descrição do Projeto

GeoPing é um aplicativo Android nativo que detecta a proximidade do usuário a "Cercas Digitais" (Geofences) através de escaneamento passivo de Wi-Fi. Quando o dispositivo detecta que está dentro de uma cerca digital (baseado no SSID e força do sinal), o aplicativo automaticamente se inscreve em uma sala de chat em tempo real usando Socket.IO.

## Características Principais

- **Detecção Automática de Proximidade**: Usa escaneamento Wi-Fi passivo para detectar cercas digitais
- **Histerese de Sinal**: Implementa limiares diferentes para entrada (-75 dBm) e saída (-85 dBm) para evitar oscilações
- **Chat em Tempo Real**: Comunicação via Socket.IO quando dentro de uma cerca digital
- **Arquitetura MVVM**: Código organizado e separação de responsabilidades
- **Serviço em Background**: Monitoramento contínuo mesmo com o app em background
- **UI Moderna**: Interface intuitiva com Material Design

## Arquitetura

O projeto segue o padrão **MVVM (Model-View-ViewModel)**:

```
com.geoping/
├── model/              # Modelos de dados
│   └── ChatMessage.java
├── viewmodel/          # ViewModels (lógica de negócio da UI)
│   └── ChatViewModel.java
├── services/           # Serviços de background
│   ├── SocketManager.java
│   └── WifiProximityService.java
└── ui/                 # Activities e Adapters
    ├── MainActivity.java
    └── ChatAdapter.java
```

## Componentes Principais

### 1. SocketManager (Singleton)
Gerencia a conexão Socket.IO com o servidor:
- Conexão/desconexão automática
- Entrada/saída de salas
- Envio e recebimento de mensagens
- Status de conexão em tempo real

### 2. WifiProximityService
Serviço em foreground que escaneia redes Wi-Fi:
- Escaneamento periódico (5 segundos)
- Lógica de histerese para entrada/saída de salas
- Detecção da cerca digital "GP_Lab"
- Notificação persistente de status

### 3. ChatViewModel
Gerencia o estado da interface de chat:
- Lista de mensagens observável
- Sala atual observável
- Status de conexão observável
- Coordenação entre UI e serviços

### 4. MainActivity
Interface principal do usuário:
- RecyclerView com mensagens
- Campo de entrada e botão de envio
- Indicadores de status (conexão, sala atual)
- Gerenciamento de permissões

## Tecnologias Utilizadas

- **Linguagem**: Java
- **API Mínima**: Android 8.0 (API 26)
- **API Alvo**: Android 14 (API 34)
- **Bibliotecas**:
  - AndroidX (AppCompat, RecyclerView, ConstraintLayout)
  - Lifecycle (ViewModel, LiveData)
  - Socket.IO Client 2.1.0
  - Material Components

## Configuração do Servidor

### Pré-requisitos
- Servidor Socket.IO rodando (Node.js)
- O servidor deve estar acessível na rede local

### Configuração do IP do Servidor

Antes de compilar o aplicativo, você precisa configurar o IP do seu servidor Socket.IO:

1. Abra o arquivo `app/src/main/java/com/geoping/services/SocketManager.java`
2. Localize a linha:
```java
private static final String SERVER_URL = "http://192.168.1.100:3000";
```
3. Substitua `192.168.1.100` pelo IP do seu servidor
4. Se necessário, altere a porta `3000` para a porta que seu servidor utiliza

### Exemplo de Servidor Node.js

```javascript
const express = require('express');
const app = express();
const http = require('http').createServer(app);
const io = require('socket.io')(http);

io.on('connection', (socket) => {
    console.log('Cliente conectado:', socket.id);
    
    socket.on('join_room', (data) => {
        socket.join(data.room);
        console.log(`Cliente ${socket.id} entrou na sala ${data.room}`);
    });
    
    socket.on('leave_room', (data) => {
        socket.leave(data.room);
        console.log(`Cliente ${socket.id} saiu da sala ${data.room}`);
    });
    
    socket.on('mensagem', (data) => {
        io.to(data.room).emit('nova_mensagem', data);
        console.log(`Mensagem na sala ${data.room}: ${data.message}`);
    });
    
    socket.on('disconnect', () => {
        console.log('Cliente desconectado:', socket.id);
    });
});

http.listen(3000, () => {
    console.log('Servidor rodando na porta 3000');
});
```

## Configuração da Cerca Digital

Para alterar a cerca digital alvo, edite o arquivo `WifiProximityService.java`:

```java
private static final String TARGET_SSID = "GP_Lab";  // Nome da rede Wi-Fi
private static final int THRESHOLD_ENTER = -75;      // Limiar de entrada (dBm)
private static final int THRESHOLD_EXIT = -85;       // Limiar de saída (dBm)
```

## Como Usar o Emulador Android no Cursor

### Opção 1: Usar o Emulador do Android Studio

1. **Abra o Android Studio** que você já tem instalado
2. **Configure um AVD (Android Virtual Device)**:
   - Tools → Device Manager
   - Crie um novo dispositivo virtual ou use um existente
   - Recomendado: Pixel 5 com API 34
3. **Inicie o emulador** no Android Studio
4. **No Cursor**, compile e instale o app:
   ```bash
   cd C:\Users\Willdemarques\Documents\dev\UFMA\semestre_3\geoping
   .\gradlew assembleDebug
   adb install app/build/outputs/apk/debug/app-debug.apk
   ```

### Opção 2: Usar ADB diretamente

Se o emulador já estiver rodando, você pode verificar com:
```bash
adb devices
```

### Limitações do Emulador

**IMPORTANTE**: O emulador do Android não suporta escaneamento Wi-Fi real. Para testar a funcionalidade completa de detecção de cercas digitais, você precisa usar um **dispositivo físico**.

#### Testando no Dispositivo Físico

1. **Habilite o modo desenvolvedor** no seu dispositivo Android
2. **Ative a depuração USB**
3. **Conecte o dispositivo via USB**
4. **Verifique a conexão**:
   ```bash
   adb devices
   ```
5. **Instale o app**:
   ```bash
   .\gradlew installDebug
   ```

## Compilação e Instalação

### Usando Gradle Wrapper (Recomendado)

```bash
# Windows PowerShell
cd C:\Users\Willdemarques\Documents\dev\UFMA\semestre_3\geoping

# Compilar o projeto
.\gradlew assembleDebug

# Instalar no dispositivo/emulador conectado
.\gradlew installDebug

# Ou fazer tudo de uma vez
.\gradlew build installDebug
```

### Usando Android Studio

1. Abra o Android Studio
2. File → Open → Selecione a pasta do projeto GeoPing
3. Aguarde o Gradle sincronizar
4. Clique em Run (Shift+F10)

## Estrutura de Arquivos

```
geoping/
├── app/
│   ├── src/
│   │   └── main/
│   │       ├── java/com/geoping/
│   │       │   ├── model/
│   │       │   ├── viewmodel/
│   │       │   ├── services/
│   │       │   └── ui/
│   │       ├── res/
│   │       │   ├── layout/
│   │       │   ├── values/
│   │       │   └── drawable/
│   │       └── AndroidManifest.xml
│   ├── build.gradle
│   └── proguard-rules.pro
├── gradle/
│   └── wrapper/
├── build.gradle
├── settings.gradle
├── gradle.properties
└── README.md
```

## Permissões Necessárias

O aplicativo solicita as seguintes permissões:

- `INTERNET`: Para comunicação Socket.IO
- `ACCESS_WIFI_STATE`: Para ler informações de Wi-Fi
- `CHANGE_WIFI_STATE`: Para iniciar escaneamento Wi-Fi
- `ACCESS_FINE_LOCATION`: Necessária para escaneamento Wi-Fi no Android 8.0+
- `ACCESS_COARSE_LOCATION`: Backup de localização
- `FOREGROUND_SERVICE`: Para manter o serviço em execução

## Fluxo de Funcionamento

1. **Inicialização**:
   - App solicita permissões necessárias
   - Conecta ao servidor Socket.IO
   - Inicia o WifiProximityService

2. **Detecção de Cerca Digital**:
   - Serviço escaneia Wi-Fi a cada 5 segundos
   - Quando detecta "GP_Lab" com sinal > -75 dBm, entra na sala automaticamente
   - Quando o sinal cai abaixo de -85 dBm, sai da sala

3. **Chat em Tempo Real**:
   - Usuário envia mensagens através da UI
   - Mensagens são enviadas via Socket.IO para a sala atual
   - Novas mensagens são recebidas e exibidas em tempo real

## Troubleshooting

### App não se conecta ao servidor
- Verifique se o servidor Socket.IO está rodando
- Confirme se o IP está correto no `SocketManager.java`
- Verifique se o dispositivo está na mesma rede que o servidor
- No AndroidManifest.xml, confirme que `usesCleartextTraffic="true"` está presente

### Escaneamento Wi-Fi não funciona
- Use um dispositivo físico (emulador não suporta escaneamento Wi-Fi real)
- Verifique se as permissões de localização foram concedidas
- Confirme que o Wi-Fi está ligado no dispositivo
- Verifique se a rede "GP_Lab" está realmente disponível

### Serviço para de funcionar
- Verifique se o dispositivo não está em modo de economia de bateria extrema
- Alguns fabricantes (Xiaomi, Huawei) podem matar serviços em background
- Configure o app para não ser otimizado pela bateria

## Melhorias Futuras

- [ ] Suporte a múltiplas cercas digitais
- [ ] Configuração de SSID via interface
- [ ] Histórico de mensagens persistente
- [ ] Notificações para novas mensagens
- [ ] Personalização de nome de usuário
- [ ] Autenticação de usuário
- [ ] Criptografia de mensagens

## Licença

Este é um projeto acadêmico desenvolvido para a UFMA.

## Autor

Desenvolvido como protótipo para demonstração de conceito de Cercas Digitais via Wi-Fi.

## Suporte

Para questões e suporte, consulte a documentação do código ou entre em contato com o desenvolvedor.

