# GeoPing - Novo Comportamento Híbrido

## O Que Mudou?

O GeoPing agora funciona com um sistema **híbrido de inscrição + cobertura Wi-Fi**.

---

## Como Funciona Agora

### Professor (Publisher)

```
✅ Cria sala de qualquer lugar
✅ Envia mensagens de qualquer lugar
✅ Não precisa estar na cobertura Wi-Fi
```

### Aluno (Subscriber)

```
1️⃣ Inscreve-se na sala (uma vez, clicando no botão 🔧)
2️⃣ Inscrição fica SALVA permanentemente
3️⃣ Só RECEBE mensagens se:
   ✅ Está INSCRITO na sala
   E
   ✅ Está na COBERTURA Wi-Fi associada
   
Se sair da cobertura:
   ❌ Para de receber (mas continua inscrito)
   
Se voltar à cobertura:
   ✅ Volta a receber automaticamente
```

---

## Exemplo Prático Completo

### Cenário: Professor e Alunos no Lab

```
┌─────────────────────────────────────────────────────────┐
│  PASSO 1: Professor Cria a Sala (8h, em casa)          │
├─────────────────────────────────────────────────────────┤
│  1. Abre o app                                          │
│  2. Clica no botão 🔧                                   │
│  3. Clica em "+ Criar Nova Sala"                       │
│  4. Digita:                                             │
│     Nome: "lab redes moveis"                           │
│     SSID: "ALMEIDA 2.4G"                               │
│  5. Sala criada! ✅                                     │
└─────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────┐
│  PASSO 2: Professor Envia Mensagem (8h, em casa)       │
├─────────────────────────────────────────────────────────┤
│  1. Seleciona sala "lab redes moveis"                  │
│  2. Digita: "Atividade hoje às 14h"                   │
│  3. Envia ✅                                            │
│                                                         │
│  🎯 Resultado:                                          │
│  - Mensagem enviada com sucesso                        │
│  - Servidor guarda na sala "lab_redes_moveis"         │
│  - Aguardando alunos conectarem                        │
└─────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────┐
│  PASSO 3: Aluno A Chega no Lab (13h)                   │
├─────────────────────────────────────────────────────────┤
│  1. Entra fisicamente no laboratório                   │
│  2. Celular detecta Wi-Fi "ALMEIDA 2.4G"              │
│  3. App mostra: "📍 Na cobertura de: ALMEIDA 2.4G"    │
│                                                         │
│  ❌ MAS AINDA NÃO RECEBE A MENSAGEM!                   │
│                                                         │
│  Por quê?                                               │
│  → Detectar Wi-Fi é APENAS informativo                 │
│  → Precisa se INSCREVER na sala primeiro               │
└─────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────┐
│  PASSO 4: Aluno A Se Inscreve (13h05)                  │
├─────────────────────────────────────────────────────────┤
│  1. Clica no botão 🔧 (Configurar)                     │
│  2. Vê lista de salas:                                  │
│     ◉ lab redes moveis (Wi-Fi: ALMEIDA 2.4G)         │
│     ○ Biblioteca                                        │
│     ○ Auditório                                         │
│  3. Clica em "lab redes moveis"                        │
│                                                         │
│  🎯 O que acontece:                                     │
│  ✅ Inscrição SALVA permanentemente                     │
│  ✅ Como está na cobertura, entra no Socket.IO         │
│  ✅ RECEBE a mensagem do professor!                     │
│     "Atividade hoje às 14h" (5h atrás)                │
└─────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────┐
│  PASSO 5: Aluno A Vai Almoçar (13h30)                  │
├─────────────────────────────────────────────────────────┤
│  1. Sai fisicamente do laboratório                     │
│  2. App detecta: Wi-Fi perdido                         │
│  3. App mostra: "Monitorando cercas digitais..."      │
│                                                         │
│  🎯 O que acontece AUTOMATICAMENTE:                     │
│  ❌ Saiu da sala Socket.IO                              │
│  ✅ MAS continua INSCRITO                               │
│                                                         │
│  Se professor enviar mensagem AGORA:                    │
│  ❌ Aluno A NÃO recebe (fora da cobertura)             │
└─────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────┐
│  PASSO 6: Aluno A Volta pro Lab (14h)                  │
├─────────────────────────────────────────────────────────┤
│  1. Entra fisicamente no laboratório                   │
│  2. Wi-Fi "ALMEIDA 2.4G" detectado                     │
│  3. App mostra: "📍 Na cobertura de: ALMEIDA 2.4G"    │
│                                                         │
│  🎯 O que acontece AUTOMATICAMENTE:                     │
│  ✅ Sistema verifica: está inscrito em "lab redes..."  │
│  ✅ Entra automaticamente no Socket.IO                 │
│  ✅ VOLTA a receber mensagens                           │
│  ✅ NÃO precisa clicar no 🔧 novamente!                │
└─────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────┐
│  PASSO 7: Aluno B Chega no Lab (14h05)                 │
├─────────────────────────────────────────────────────────┤
│  1. Entra fisicamente no laboratório                   │
│  2. Wi-Fi detectado: "📍 Na cobertura de..."           │
│  3. ❌ NÃO se inscreve na sala                         │
│  4. Fica apenas navegando no celular                   │
│                                                         │
│  🎯 Resultado:                                          │
│  ❌ Aluno B NÃO recebe nenhuma mensagem                │
│  ❌ Professor não sabe que Aluno B está lá             │
│                                                         │
│  Por quê?                                               │
│  → Estar na cobertura Wi-Fi NÃO é suficiente           │
│  → Precisa estar INSCRITO na sala                      │
└─────────────────────────────────────────────────────────┘
```

---

## Regras do Sistema

### Para ENVIAR Mensagens

| Condição | Professor | Aluno |
|----------|-----------|-------|
| Precisa estar inscrito | ❌ Não | ✅ Sim |
| Precisa estar na cobertura | ❌ Não | ❌ Não |
| Pode enviar de qualquer lugar | ✅ Sim | ✅ Sim |

### Para RECEBER Mensagens

| Condição | Professor | Aluno |
|----------|-----------|-------|
| Precisa estar inscrito | ✅ Sim | ✅ Sim |
| Precisa estar na cobertura | ✅ Sim | ✅ Sim |
| AMBAS as condições | ✅ Obrigatório | ✅ Obrigatório |

---

## Estados do Sistema

### Inscrição (Persistente)

```
INSCRITO    = Salvo no SharedPreferences
DESINSCRITO = Removido do SharedPreferences

Ações:
- Inscrever: Clica na sala no seletor
- Desinscrever: (Futuro) Botão "Sair da sala"
```

### Conexão Socket.IO (Automática)

```
CONECTADO   = Dentro da cobertura + Inscrito
DESCONECTADO = Fora da cobertura OU Não inscrito

Controlado por:
- WifiProximityService (automático)
- Baseado em inscrições + detecção Wi-Fi
```

---

## Arquitetura Técnica

### Componentes Modificados

```
┌───────────────────────────────────────────────┐
│  RoomManager                                  │
│  • subscribeToRoom(roomId)                   │
│  • unsubscribeFromRoom(roomId)               │
│  • isSubscribedTo(roomId)                    │
│  • getSubscribedRoomIds()                    │
└───────────────────────────────────────────────┘
               ↓ Salva no SharedPreferences

┌───────────────────────────────────────────────┐
│  WifiProximityService                         │
│  • Detecta Wi-Fi                             │
│  • Busca salas inscritas com SSID            │
│  • Entra/sai do Socket.IO AUTOMATICAMENTE    │
│  • enterSubscribedRoomsForSSID()             │
│  • leaveAllActiveRooms()                     │
└───────────────────────────────────────────────┘
               ↓ Controla Socket.IO

┌───────────────────────────────────────────────┐
│  ChatViewModel                                │
│  • selectRoomForSending()                    │
│  • sendMessage() - de qualquer lugar         │
│  • NÃO controla Socket.IO                    │
└───────────────────────────────────────────────┘
               ↓ Apenas envia mensagens

┌───────────────────────────────────────────────┐
│  MainActivity                                 │
│  • Mostra sala selecionada (para envio)     │
│  • Mostra Wi-Fi detectado (informativo)     │
│  • Botão 🔧 → RoomSelectorActivity           │
└───────────────────────────────────────────────┘
```

### Fluxo de Dados

```
Wi-Fi Detectado
    ↓
WifiProximityService.handleScanResults()
    ↓
enterSubscribedRoomsForSSID("ALMEIDA 2.4G")
    ↓
Busca salas inscritas (RoomManager)
    ↓
SocketManager.joinRoom(roomId) para cada sala
    ↓
Aluno RECEBE mensagens
```

---

## Comparação: Antes vs Agora

### Antes (Modo Manual Simples)

```
❌ Aluno precisava selecionar sala manualmente
❌ Ao sair da cobertura, continuava na sala Socket.IO
❌ Recebia mensagens mesmo fora do lab
❌ Professor também precisava estar na cobertura
```

### Agora (Modo Híbrido: Inscrição + Cobertura)

```
✅ Aluno se inscreve uma vez (persistente)
✅ Entrada/saída do Socket.IO é AUTOMÁTICA
✅ Só recebe se na cobertura + inscrito
✅ Professor pode enviar de qualquer lugar
```

---

## Logs para Depuração

### WifiProximityService

```
✅ Entrou automaticamente na sala: lab redes moveis (inscrito + na cobertura)
❌ Saiu automaticamente da sala: lab redes moveis (fora da cobertura, mas continua inscrito)
```

### ChatViewModel

```
📤 Mensagem enviada para sala: lab_redes_moveis (de qualquer lugar, mas só quem está na cobertura recebe)
```

### RoomManager

```
Inscrito na sala: lab_redes_moveis
Desinscrito da sala: lab_redes_moveis
```

---

## Interface do Usuário

### MainActivity

```
┌────────────────────────────────────────────┐
│  Status Servidor: ● Conectado             │
│                                            │
│  📤 Enviando para: lab redes moveis       │
│  📍 Na cobertura de: ALMEIDA 2.4G         │
│                                            │
│  [Chat messages]                           │
│                                            │
│  [Digite mensagem] [Enviar] [🔧]          │
└────────────────────────────────────────────┘
```

### Botão 🔧 (Configurar)

```
Abre RoomSelectorActivity:
- Lista de salas disponíveis
- Botão "+ Criar Nova Sala"
- Ao selecionar:
  1. Inscreve automaticamente
  2. Define como sala para envio
  3. Se na cobertura, entra no Socket.IO
```

---

## Teste Passo a Passo

### 1. Preparação

```bash
# Terminal 1: Servidor
cd server
npm start

# Terminal 2: Build
.\gradlew installDebug
```

### 2. Teste Professor (Publisher)

1. Abra o app
2. Clique no 🔧
3. Crie sala "lab teste" com SSID "ALMEIDA 2.4G"
4. Selecione a sala
5. Envie mensagem: "Teste 1"
6. ✅ Deve enviar com sucesso

### 3. Teste Aluno (Subscriber) - Fora da Cobertura

1. Instale em outro dispositivo (ou desinstale e reinstale)
2. **NÃO** esteja conectado ao Wi-Fi "ALMEIDA 2.4G"
3. Abra o app
4. Clique no 🔧
5. Selecione sala "lab teste"
6. ✅ Deve mostrar "Inscrito em: lab teste"
7. ❌ NÃO deve receber mensagens (fora da cobertura)

### 4. Teste Aluno (Subscriber) - Dentro da Cobertura

1. **Conecte** ao Wi-Fi "ALMEIDA 2.4G"
2. Aguarde 5-10 segundos
3. ✅ Deve mostrar: "📍 Na cobertura de: ALMEIDA 2.4G"
4. ✅ Deve receber a mensagem "Teste 1"

### 5. Teste Entrada/Saída Automática

1. Aluno: Desconecte do Wi-Fi
2. ❌ Deve parar de receber mensagens
3. Professor: Envie "Teste 2"
4. ❌ Aluno NÃO recebe
5. Aluno: Conecte ao Wi-Fi novamente
6. ✅ Deve receber "Teste 2" automaticamente

---

## Conclusão

O GeoPing agora implementa o **comportamento híbrido** solicitado:

✅ Professor pode criar salas e enviar de qualquer lugar
✅ Aluno se inscreve uma vez (persistente)
✅ Aluno só recebe se estiver na cobertura Wi-Fi
✅ Entrada/saída do Socket.IO é automática
✅ Inscrição permanece salva mesmo após sair da cobertura

Este é o comportamento ideal para o cenário:
**"Professor em casa envia mensagem, só alunos no lab recebem"**

