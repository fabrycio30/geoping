# Debug de Problemas - GeoPing

## Problema Relatado:
"Quando estou conectado na sala não consigo enviar mensagem, o campo input fica bloqueado"

## Análise do Código:

### 1. Condições para Enviar Mensagem (MainActivity.java):

O app verifica 3 coisas antes de permitir envio:

```java
// 1. Mensagem não pode estar vazia
if (messageText.isEmpty()) {
    Toast: "A mensagem não pode estar vazia"
    return;
}

// 2. DEVE estar em uma sala
String currentRoom = chatViewModel.getCurrentRoom().getValue();
if (currentRoom == null) {
    Toast: "Você precisa estar em uma sala para enviar mensagens"
    return;  // ← BLOQUEIA AQUI!
}

// 3. DEVE estar conectado ao Socket.IO
Boolean isConnected = chatViewModel.getConnectionStatus().getValue();
if (isConnected == null || !isConnected) {
    Toast: "Sem conexão com o servidor"
    return;  // ← OU BLOQUEIA AQUI!
}
```

## Possíveis Causas:

### Causa 1: Não Está Realmente na Sala
**Sintoma**: Campo bloqueado, sem mensagens de sistema
**Motivo**: O WifiProximityService não detectou a rede
**Solução**: Verificar configurações

### Causa 2: Servidor Socket.IO Não Está Rodando
**Sintoma**: Mostra "Desconectado" (bolinha vermelha)
**Motivo**: Servidor em http://192.168.18.12:3000 está offline
**Solução**: Iniciar o servidor

### Causa 3: IP do Servidor Errado
**Sintoma**: Mostra "Desconectado" permanentemente
**Motivo**: IP configurado não é acessível
**Solução**: Corrigir IP no SocketManager.java

### Causa 4: Dispositivo e Servidor em Redes Diferentes
**Sintoma**: Mostra "Desconectado"
**Motivo**: Celular em uma rede, servidor em outra
**Solução**: Conectar ambos na mesma rede

---

## Checklist de Debug:

### Passo 1: Verificar Servidor
```bash
cd server
node server.js
```

**Deve aparecer**:
```
Servidor rodando em:
- Local: http://localhost:3000
- Rede: http://SEU_IP:3000
```

### Passo 2: Verificar IP do Computador
```powershell
ipconfig
```

Procure por "IPv4 Address" (exemplo: 192.168.18.12)

### Passo 3: Verificar IP no App
**Arquivo**: `app/src/main/java/com/geoping/services/SocketManager.java`
**Linha 32**:
```java
private static final String SERVER_URL = "http://192.168.18.12:3000";
```

**O IP deve ser o mesmo do Passo 2!**

### Passo 4: Verificar Rede Wi-Fi
**Tanto o CELULAR quanto o COMPUTADOR devem estar na mesma rede!**

**Verificar no celular**:
- Configurações → Wi-Fi
- Conectado em: ALMEIDA 2.4G (ou similar)

**Verificar no computador**:
- Deve estar na mesma rede que o celular

### Passo 5: Ver Logs do Android
```powershell
adb logcat | Select-String "GeoPing|SocketManager|WifiProximity|ChatViewModel"
```

**Logs esperados quando funciona**:
```
WifiProximityService: Rede ALMEIDA 2.4G encontrada com sinal: -40 dBm
WifiProximityService: ENTRANDO na sala: ALMEIDA 2.4G
SocketManager: Entrando na sala: ALMEIDA 2.4G
SocketManager: Socket conectado com sucesso
ChatViewModel: Mudança de sala detectada: ALMEIDA 2.4G
```

**Logs de problema**:
```
SocketManager: Erro de conexão: Error connecting to http://192.168.18.12:3000
```

---

## Configuração Atual (Baseado no seu código):

### WifiProximityService.java:
```java
TARGET_SSID = "ALMEIDA 2.4G"  // ✓ Correto (visto no WiFi Analyzer)
THRESHOLD_ENTER = -75 dBm     // ✓ Correto
THRESHOLD_EXIT = -85 dBm      // ✓ Correto
```

**Seu sinal**: -40 dBm (EXCELENTE!)
**-40 > -75?** SIM → Deve entrar na sala ✓

### SocketManager.java:
```java
SERVER_URL = "http://192.168.18.12:3000"  // ← VERIFICAR SE ESTÁ CORRETO!
```

---

## Testes Rápidos:

### Teste 1: Servidor está acessível?
No navegador do celular, acesse:
```
http://192.168.18.12:3000
```

**Resultado esperado**: Página do servidor Socket.IO

**Se não carregar**: Problema de rede ou servidor offline

### Teste 2: App detecta a rede?
Olhe na notificação persistente do app:
- "Monitorando cercas digitais..." = Não detectou
- "Conectado à sala: ALMEIDA 2.4G" = Detectou ✓

### Teste 3: Status no app
Olhe o header do app:
- **Sala Atual**: Deve mostrar "ALMEIDA 2.4G"
- **● Conectado** (verde): Socket.IO está conectado
- **● Desconectado** (vermelho): Problema no servidor

---

## Solução Mais Provável:

Baseado na sua captura de tela mostrando "Você saiu da sala", o problema pode ser:

1. **Servidor não está rodando** (mais provável)
   - Solução: Iniciar o servidor

2. **IP do servidor mudou**
   - Execute `ipconfig` e verifique o IP
   - Atualize no SocketManager.java se necessário
   - Recompile o app

3. **Firewall bloqueando**
   - Temporariamente desative o firewall para testar

---

## Comando para Ver Logs em Tempo Real:

```powershell
adb logcat -c  # Limpa logs antigos
adb logcat | Select-String "GeoPing|Socket|Wifi|Chat"
```

Deixe rodando e observe enquanto:
1. Abre o app
2. Aguarda 5 segundos
3. Tenta enviar mensagem

---

## Mensagens de Toast que Podem Aparecer:

Se tentar enviar mensagem e aparecer:

| Toast | Significado | Solução |
|-------|-------------|---------|
| "A mensagem não pode estar vazia" | Campo vazio | Digite algo |
| "Você precisa estar em uma sala para enviar mensagens" | Não detectou Wi-Fi | Verifique SSID e sinal |
| "Sem conexão com o servidor" | Socket.IO offline | Inicie o servidor |

---

## Próximo Passo Agora:

1. **Verifique se o servidor está rodando**:
   ```bash
   cd server
   node server.js
   ```

2. **Confirme o IP**:
   ```powershell
   ipconfig
   ```

3. **Se o IP mudou, atualize no código e recompile**

4. **Teste no navegador do celular**: http://192.168.18.12:3000

5. **Monitore os logs**: `adb logcat | Select-String "GeoPing"`

**Me mostre os logs ou diga qual Toast aparece quando tenta enviar mensagem!**

