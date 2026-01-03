# GeoPing - Aplicativo Android

App de coleta de dados Wi-Fi para treinamento do modelo de localização indoor.

## Requisitos

- Android Studio Flamingo ou superior
- Android SDK 23+
- JDK 17+
- Dispositivo Android 6.0+ ou emulador

## Instalação

### 1. Configurar SDK Location

Edite `android/local.properties` e ajuste o caminho do SDK:

```properties
sdk.dir=C\:\\Users\\SEU_USUARIO\\AppData\\Local\\Android\\Sdk
```

Para descobrir o caminho correto:
- Android Studio → File → Settings → System Settings → Android SDK
- Copie o caminho "Android SDK Location"

### 2. Abrir no Android Studio

1. Android Studio → File → Open
2. Selecione a pasta `android/`
3. Aguarde o Gradle sincronizar (pode demorar na primeira vez)

### 3. Sincronizar Gradle

Se não sincronizar automaticamente:
- File → Sync Project with Gradle Files
- Ou clique no ícone do elefante na barra de ferramentas

### 4. Instalar no Dispositivo

1. Conecte um Android via USB (com depuração USB ativada)
   - Ou inicie um emulador
2. Clique em **Run** (play verde)
3. Selecione o dispositivo

## Uso do App

1. **Nome da Sala**: Digite o identificador da sala (ex: `LAB_LESERC`)
2. **URL do Servidor**: Digite o endereço do backend (ex: `http://192.168.1.100:3000`)
3. **Intervalo**: Tempo entre scans em segundos (padrão: 3)
4. Clique em **INICIAR COLETA**
5. Caminhe pela sala por 5-10 minutos
6. Clique em **PARAR COLETA**

## Permissões Necessárias

O app solicita permissões de:
- Localização (necessário para escanear Wi-Fi no Android 10+)
- Acesso ao estado do Wi-Fi
- Internet (para enviar dados ao servidor)

## Estrutura do Código

```
app/src/main/
├── AndroidManifest.xml                           # Configurações e permissões
├── java/com/geoping/datacollection/
│   └── DataCollectionActivity.java              # Activity principal
└── res/
    ├── layout/
    │   └── activity_data_collection.xml         # Interface visual
    └── values/
        ├── strings.xml                          # Textos
        ├── colors.xml                           # Cores
        └── themes.xml                           # Tema
```

## Arquitetura

```
[DataCollectionActivity]
    │
    ├─► WifiManager.startScan()
    │       └─► BroadcastReceiver (resultados)
    │
    ├─► Coleta: BSSID, SSID, RSSI
    │
    └─► OkHttp POST → Backend (http://server:3000/api/collect)
```

## Troubleshooting

### Gradle não sincroniza

Consulte: [SYNC_ANDROID_STUDIO.md](SYNC_ANDROID_STUDIO.md)

### Permissões negadas

1. Configurações → Apps → GeoPing → Permissões
2. Habilite "Localização"

### Erro ao conectar ao servidor

- Verifique se o backend está rodando
- Use o IP da máquina, não 127.0.0.1
- PC e celular devem estar na mesma rede Wi-Fi

### Wi-Fi scan throttling

Android 9+ limita scans a 4 por 2 minutos em foreground. Use intervalos maiores (5-10 segundos).

## Dependências

- AndroidX AppCompat 1.6.1
- Material Design 1.9.0
- CardView 1.0.0
- OkHttp 4.11.0

## Build via Linha de Comando

```bash
# Windows
cd android
gradlew.bat assembleDebug
adb install app\build\outputs\apk\debug\app-debug.apk
```

## Licença

MIT





