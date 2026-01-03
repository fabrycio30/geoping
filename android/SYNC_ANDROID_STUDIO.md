# Como Sincronizar o Projeto no Android Studio

## Problema: Gradle não sincroniza

Se o Gradle não sincronizar automaticamente, siga estes passos:

## Solução Passo a Passo:

### 1. Verificar o caminho do Android SDK

Abra o arquivo `android/local.properties` e verifique se o caminho do SDK está correto:

```properties
sdk.dir=C\:\\Users\\Willdemarques\\AppData\\Local\\Android\\Sdk
```

**Para descobrir o caminho correto do seu SDK:**

- No Android Studio: File → Settings → Appearance & Behavior → System Settings → Android SDK
- Copie o caminho "Android SDK Location"
- Cole no arquivo `local.properties` (substitua `\` por `\\`)

### 2. Fechar e Reabrir o Projeto

1. **Feche** o Android Studio completamente
2. **Reabra** o Android Studio
3. **File → Open**
4. Selecione a pasta: `C:\Users\Willdemarques\Documents\dev\UFMA\semestre_3\geoping_v2\android`
5. Clique em **OK**

### 3. Forçar Sincronização do Gradle

No Android Studio:

1. **File → Sync Project with Gradle Files**
2. Ou clique no ícone do elefante (Gradle sync) na barra de ferramentas
3. Aguarde o download das dependências (primeira vez pode demorar)

### 4. Se ainda não funcionar: Limpar Cache

```
File → Invalidate Caches / Restart → Invalidate and Restart
```

### 5. Alternativa: Sincronizar via Terminal

Abra o terminal no Android Studio e execute:

```bash
# Windows
cd android
gradlew.bat clean
gradlew.bat build
```

### 6. Verificar versão do Gradle

Se aparecer erro de versão do Gradle:

1. Abra `android/gradle/wrapper/gradle-wrapper.properties`
2. Verifique a linha `distributionUrl`
3. Deve ser algo como: `gradle-8.0-bin.zip` ou superior

### 7. Verificar JDK

O projeto precisa do **JDK 17** ou superior:

1. File → Project Structure → SDK Location
2. Verifique se o JDK está configurado
3. Se não estiver, baixe de: https://adoptium.net/

## Erros Comuns e Soluções

### Erro: "SDK location not found"

**Solução:** Ajuste o caminho em `local.properties` conforme passo 1.

### Erro: "Unsupported Gradle version"

**Solução:** 
- Atualize o Android Studio para a versão mais recente
- Ou ajuste a versão do Gradle em `gradle-wrapper.properties`

### Erro: "Failed to resolve dependencies"

**Solução:**
- Verifique sua conexão com internet
- File → Settings → Appearance & Behavior → System Settings → HTTP Proxy
- Tente usar "No proxy"

### Erro: "Namespace not specified"

**Solução:** Já corrigido no `app/build.gradle` com a linha `namespace 'com.geoping.datacollection'`

## Estrutura Final Esperada

Após sincronizar, você deve ver:

```
android/
├── .gradle/                  (criado automaticamente)
├── .idea/                    (criado automaticamente)
├── app/
│   ├── build/               (criado após build)
│   ├── src/
│   ├── build.gradle         ✓
│   └── proguard-rules.pro   ✓
├── gradle/
│   └── wrapper/
│       └── gradle-wrapper.properties ✓
├── build.gradle             ✓
├── gradle.properties        ✓
├── gradlew                  (será criado)
├── gradlew.bat              (será criado)
├── local.properties         ✓
└── settings.gradle          ✓
```

## Após Sincronização Bem-Sucedida

1. Conecte um dispositivo Android via USB ou inicie um emulador
2. Clique no botão **Run** (play verde)
3. O app será instalado no dispositivo

## Suporte

Se continuar com problemas:
1. Verifique se todas as dependências do Android Studio foram instaladas
2. Tente criar um novo projeto "Empty Activity" para testar se o Android Studio funciona
3. Compare com este projeto GeoPing





