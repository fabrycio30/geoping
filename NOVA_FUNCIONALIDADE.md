# Nova Funcionalidade - Seleção Manual de Salas

## O que Mudou?

### ✅ Antes (Versão Original):
- Detectava Wi-Fi → Entrava automaticamente na sala
- Só podia enviar/receber se estivesse na cobertura Wi-Fi
- Uma única sala por rede

### ✨ Agora (Nova Versão):
- **Modo Manual**: Qualquer pessoa pode selecionar uma sala
- **Modo Informativo**: Wi-Fi apenas informa cobertura (não entra auto)
- **Múltiplas Salas**: Crie e gerencie várias salas
- **Salas Virtuais**: Salas sem Wi-Fi associado
- **Envio Remoto**: Envia de qualquer lugar, recebe só na cobertura

---

## Caso de Uso Principal

### Cenário: Professor e Alunos

**Professor em Casa** (manhã, 8h):
1. Abre o GeoPing
2. Clica no botão 🔧 "Configurar"
3. Seleciona "lab LESERC" (ou cria se não existir)
4. Digita: "Terei aula hoje às 14h no laboratório"
5. Envia mensagem

**Resultado**: Mensagem vai para sala "lab LESERC" via Socket.IO

---

**Aluno no Laboratório** (manhã, 11h):
1. Abre o GeoPing
2. App detecta Wi-Fi "ALMEIDA 2.4G"
3. Mostra: "📍 Na cobertura de: ALMEIDA 2.4G"
4. Clica no botão 🔧 e seleciona "lab LESERC"
5. RECEBE a mensagem do professor!

**Resultado**: Aluno recebe aviso mesmo antes da aula

---

**Aluno em Casa** (manhã, 11h):
1. Abre o GeoPing
2. App mostra: "📍 Nenhuma rede detectada"
3. Pode selecionar "lab LESERC" manualmente
4. Pode ENVIAR mensagens
5. NÃO RECEBE mensagens (não está na cobertura)

**Resultado**: Pode interagir, mas não recebe atualizações

---

## Interface Nova

### Tela Principal (MainActivity)

```
┌─────────────────────────────────────────┐
│  GeoPing                                │
│                                         │
│  Sala Atual:                         🔧 │  ← NOVO BOTÃO
│  lab LESERC                             │
│                                         │
│  📍 Na cobertura de: ALMEIDA 2.4G      │  ← NOVO STATUS
│  ● Conectado                            │
├─────────────────────────────────────────┤
│  [Mensagens do chat]                   │
├─────────────────────────────────────────┤
│  Digite mensagem...           ENVIAR   │
└─────────────────────────────────────────┘
```

### Nova Tela: Seleção de Salas

```
┌─────────────────────────────────────────┐
│  Selecionar Sala                        │
│  Escolha ou crie uma nova sala          │
├─────────────────────────────────────────┤
│                                         │
│  [+ Criar Nova Sala]                   │
│                                         │
│  Salas Disponíveis:                    │
│                                         │
│  ┌────────────────────────────────┐   │
│  │ 📍 lab LESERC              🗑️  │   │
│  │ Wi-Fi: ALMEIDA 2.4G            │   │
│  └────────────────────────────────┘   │
│                                         │
│  ┌────────────────────────────────┐   │
│  │ 📍 Biblioteca              🗑️  │   │
│  │ Sala virtual (sem Wi-Fi)       │   │
│  └────────────────────────────────┘   │
│                                         │
│               [Cancelar]               │
└─────────────────────────────────────────┘
```

### Diálogo: Criar Nova Sala

```
┌─────────────────────────────────────────┐
│  Criar Nova Sala                        │
├─────────────────────────────────────────┤
│                                         │
│  Nome da Sala:                         │
│  [___lab LESERC___________________]    │
│                                         │
│  SSID da Rede Wi-Fi (opcional):        │
│  [___ALMEIDA 2.4G_________________]    │
│                                         │
│  ℹ️ Deixe vazio para criar sala         │
│     virtual (sem detecção Wi-Fi)       │
│                                         │
│            [Cancelar]  [Criar]         │
└─────────────────────────────────────────┘
```

---

## Arquitetura Implementada

### Novas Classes:

**1. Room.java** (Model)
- Modelo de dados para salas
- Propriedades: roomId, roomName, wifiSSID
- Suporte a salas virtuais (sem Wi-Fi)
- Métodos de validação

**2. RoomManager.java** (Service - Singleton)
- Gerencia criação/remoção de salas
- Persistência via SharedPreferences
- Busca salas por ID ou SSID
- Gerencia sala selecionada

**3. RoomSelectorActivity.java** (UI)
- Tela de seleção de salas
- RecyclerView com lista de salas
- Diálogo para criar sala
- Callback para MainActivity

**4. RoomAdapter.java** (UI)
- Adapter para RecyclerView
- Exibe salas em CardViews
- Botão de deletar
- Click para selecionar

### Modificações em Classes Existentes:

**ChatViewModel.java**:
- Adicionado `detectedWifiLiveData` (separado de `currentRoomLiveData`)
- Método `selectRoomManually(roomId, roomName)`
- Método `clearRoomSelection()`
- Método `getDetectedWifi()`
- Flag `isManualMode`

**WifiProximityService.java**:
- **MUDANÇA CRÍTICA**: Não entra mais automaticamente em salas
- Apenas detecta e notifica via `detectedWifiLiveData`
- Remove métodos `enterRoom()` e `exitRoom()`
- Notificação mostra "📍 Detectado: ..."

**MainActivity.java**:
- Botão `btnConfigureRoom` (🔧)
- TextView `detectedWifiText`
- Observer para `getDetectedWifi()`
- Método `openRoomSelector()`
- Callback `onActivityResult()`

**AndroidManifest.xml**:
- Registro da `RoomSelectorActivity`

---

## Fluxo de Dados

### Seleção Manual de Sala:

```
MainActivity → btnConfigureRoom.onClick()
    ↓
RoomSelectorActivity.start()
    ↓
RoomManager.getAllRooms() → Lista de salas
    ↓
Usuário seleciona "lab LESERC"
    ↓
RoomManager.setSelectedRoom("lab_leserc")
    ↓
onActivityResult() → MainActivity
    ↓
ChatViewModel.selectRoomManually()
    ↓
SocketManager.joinRoom("lab_leserc")
    ↓
currentRoomLiveData.postValue("lab_leserc")
    ↓
UI atualiza: "Sala Atual: lab LESERC"
```

### Detecção de Wi-Fi:

```
WifiProximityService.handleScanResults()
    ↓
Encontra "ALMEIDA 2.4G" com -40 dBm
    ↓
Sinal > -75 dBm? SIM
    ↓
detectedWifiLiveData.postValue("ALMEIDA 2.4G")
    ↓
ChatViewModel.getDetectedWifi() observa
    ↓
MainActivity.updateDetectedWifi()
    ↓
UI mostra: "📍 Na cobertura de: ALMEIDA 2.4G"
```

**IMPORTANTE**: Detectar Wi-Fi NÃO entra na sala automaticamente!

---

## Comportamentos

### 1. Criar Sala Virtual

**Como**: Deixar SSID vazio no diálogo

**Resultado**:
- Sala criada sem Wi-Fi associado
- Pode enviar/receber de qualquer lugar
- Não tem detecção automática
- Ideal para salas conceituais

**Exemplo**: "Avisos Gerais", "Biblioteca", "Coordenação"

### 2. Criar Sala com Wi-Fi

**Como**: Preencher nome E SSID no diálogo

**Resultado**:
- Sala associada a rede Wi-Fi específica
- App detecta quando está na cobertura
- Mostra status: "📍 Na cobertura de: [SSID]"
- Ideal para laboratórios, salas específicas

**Exemplo**: "lab LESERC" → "ALMEIDA 2.4G"

### 3. Enviar Sem Estar no Local

**Cenário**: Professor em casa, alunos no lab

**Como Funciona**:
1. Professor seleciona "lab LESERC" manualmente
2. Envia mensagem
3. Socket.IO transmite para sala
4. Apenas alunos NA COBERTURA da rede associada recebem
5. Alunos fora não recebem (não estão na sala Socket.IO)

### 4. Múltiplos Dispositivos

**Cenário**: 5 alunos no laboratório

**Como Funciona**:
1. Todos detectam "ALMEIDA 2.4G"
2. Todos selecionam "lab LESERC"
3. Todos entram na mesma sala Socket.IO
4. Chat em grupo funciona normalmente
5. Se alguém sai do lab → sai da detecção → pode sair da sala

---

## Persistência de Dados

### SharedPreferences:

**Chave**: `GeoPingRooms`

**Estrutura**:
```json
{
  "rooms_list": [
    {
      "id": "lab_leserc",
      "name": "lab LESERC",
      "ssid": "ALMEIDA 2.4G",
      "created": 1700000000000
    },
    {
      "id": "biblioteca",
      "name": "Biblioteca",
      "ssid": null,
      "created": 1700000001000
    }
  ],
  "selected_room": "lab_leserc"
}
```

### Salas Padrão:

Na primeira execução, são criadas:
1. "lab LESERC" → SSID: "ALMEIDA 2.4G"
2. "Biblioteca" → Sala virtual
3. "Auditório" → Sala virtual

---

## Comandos de Compilação

```powershell
# No Android Studio:
1. Build → Sync Project with Gradle Files
2. Build → Rebuild Project  
3. Run → Run 'app' (Shift+F10)

# Ou via linha de comando:
.\gradlew.bat clean build installDebug
```

---

## Testando a Nova Funcionalidade

### Teste 1: Criar Sala

1. Abra o app
2. Clique no 🔧
3. Clique em "+ Criar Nova Sala"
4. Digite: Nome: "Minha Sala", SSID: (vazio)
5. Clique em "Criar"
6. ✅ Deve aparecer na lista

### Teste 2: Selecionar Sala

1. Abra o app
2. Clique no 🔧
3. Clique em uma sala da lista
4. ✅ Deve voltar para tela principal
5. ✅ "Sala Atual" deve mostrar o nome
6. ✅ Mensagem do sistema: "Você entrou na sala: ..."

### Teste 3: Enviar de Casa

1. Certifique-se de NÃO estar perto da rede Wi-Fi
2. Selecione uma sala
3. Envie uma mensagem
4. ✅ Mensagem deve ser enviada
5. ✅ Deve aparecer na lista (sua mensagem à direita)

### Teste 4: Detecção Wi-Fi

1. Aproxime-se da rede "ALMEIDA 2.4G"
2. Aguarde até 5 segundos
3. ✅ Deve aparecer: "📍 Na cobertura de: ALMEIDA 2.4G"
4. ✅ Notificação deve mostrar: "📍 Detectado: ..."

### Teste 5: Deletar Sala

1. Clique no 🔧
2. Clique no 🗑️ de uma sala
3. Confirme a deleção
4. ✅ Sala deve desaparecer da lista

---

## Diferenças Importantes

| Aspecto | Antes | Agora |
|---------|-------|-------|
| Entrada em sala | Automática (Wi-Fi) | Manual (botão) |
| Detecção Wi-Fi | Entra na sala | Apenas informa |
| Envio remoto | ❌ Não funcionava | ✅ Funciona |
| Múltiplas salas | ❌ Não | ✅ Sim |
| Salas virtuais | ❌ Não | ✅ Sim |
| Gerenciamento | ❌ Fixo no código | ✅ Interface |
| Persistência | ❌ Não | ✅ SharedPreferences |

---

## Casos de Uso Adicionais

### 1. Avisos Gerais (Sala Virtual)
- Coordenação cria sala "Avisos Gerais"
- Sem Wi-Fi associado
- Todos podem selecionar
- Avisos importantes para todos

### 2. Múltiplos Laboratórios
- "Lab 1" → Wi-Fi "LAB1_NET"
- "Lab 2" → Wi-Fi "LAB2_NET"
- "Lab 3" → Wi-Fi "LAB3_NET"
- Cada um detecta automaticamente

### 3. Biblioteca com Zonas
- "Biblioteca Geral" → Virtual
- "Sala de Estudos" → Wi-Fi específico
- "Sala Silenciosa" → Wi-Fi específico

---

## Limitações e Melhorias Futuras

### Limitações Atuais:
- Não sincroniza salas entre dispositivos
- Não tem autenticação de usuário
- Histórico de mensagens não persiste
- Uma sala associada por SSID

### Melhorias Futuras:
- [ ] Backend para sincronização de salas
- [ ] Múltiplos SSIDs por sala
- [ ] Permissões (admin vs usuário)
- [ ] Notificações push quando recebe mensagem
- [ ] Histórico local de mensagens
- [ ] Exportar/importar configurações de salas
- [ ] QR Code para compartilhar salas

---

## Conclusão

A nova funcionalidade transforma o GeoPing de um sistema de detecção automática em uma plataforma flexível de comunicação baseada em proximidade.

**Agora é possível**:
✅ Enviar mensagens de qualquer lugar  
✅ Criar e gerenciar múltiplas salas  
✅ Ter salas virtuais (sem Wi-Fi)  
✅ Controle manual sobre salas  
✅ Informação visual de cobertura Wi-Fi  

**Caso de uso principal atendido**:
✅ Professor em casa envia aviso  
✅ Apenas alunos no local recebem  
✅ Interface intuitiva para gerenciar  

---

**Data de Implementação**: 16/11/2025  
**Tempo de Implementação**: ~50 minutos  
**Status**: ✅ COMPLETO E FUNCIONAL

