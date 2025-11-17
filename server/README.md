# Servidor Socket.IO para GeoPing

Este é um servidor Node.js de exemplo para o aplicativo GeoPing.

## Instalação

1. Certifique-se de ter o Node.js instalado:
   - Download: https://nodejs.org
   - Versão recomendada: LTS (Long Term Support)

2. Navegue até esta pasta no terminal:
```bash
cd server-exemplo
```

3. Instale as dependências:
```bash
npm install
```

## Executando o Servidor

```bash
npm start
```

Ou, para desenvolvimento com auto-reload:
```bash
npm run dev
```

## Descobrindo seu IP Local

### Windows (PowerShell):
```powershell
ipconfig
```
Procure por "IPv4 Address" na interface de rede ativa (geralmente Wi-Fi ou Ethernet).

### Linux/Mac:
```bash
ifconfig
```
ou
```bash
ip addr show
```

## Configurando o App Android

Após descobrir seu IP (exemplo: 192.168.1.100), configure no app:

1. Abra `app/src/main/java/com/geoping/services/SocketManager.java`
2. Altere:
```java
private static final String SERVER_URL = "http://192.168.1.100:3000";
```

## Estrutura de Eventos

O servidor implementa os seguintes eventos Socket.IO:

### Cliente → Servidor
- `join_room`: Cliente entra em uma sala
- `leave_room`: Cliente sai de uma sala
- `mensagem`: Cliente envia mensagem

### Servidor → Cliente
- `nova_mensagem`: Nova mensagem recebida na sala
- `bem_vindo`: Mensagem de boas-vindas ao entrar na sala
- `usuario_entrou`: Notificação de novo usuário
- `usuario_saiu`: Notificação de usuário que saiu

## Testando o Servidor

Após iniciar o servidor, acesse no navegador:
```
http://localhost:3000
```

Você verá uma página com informações sobre as salas ativas.

## Logs

O servidor exibe logs detalhados no terminal:
- Conexões e desconexões
- Entrada e saída de salas
- Mensagens trocadas
- Erros

## Troubleshooting

**Erro: "Port 3000 is already in use"**
- Outra aplicação está usando a porta 3000
- Altere a porta no `server.js`: `const PORT = 3001;`
- Lembre-se de atualizar no app Android também

**App não conecta**
- Verifique se o servidor está rodando
- Confirme que o IP está correto no app
- Certifique-se de que o dispositivo Android e o servidor estão na mesma rede
- Desative firewalls temporariamente para testar

**Servidor para sozinho**
- Verifique os logs de erro
- Reinstale as dependências: `npm install`

