# Como Compartilhar o APK do GeoPing

## Guia Completo para Enviar o App para Outra Pessoa

---

## Método 1: Via Android Studio (Recomendado)

### Passo 1: Gerar o APK

**No Android Studio:**

1. Clique no menu: `Build` → `Build Bundle(s) / APK(s)` → `Build APK(s)`
2. Aguarde a compilação (aparece progresso no canto inferior)
3. Quando terminar, aparecerá uma notificação
4. Clique em `locate` na notificação

**Atalho do Teclado:**
- Pressione `Ctrl + Shift + A`
- Digite: `Build APK`
- Pressione `Enter`

### Passo 2: Localizar o APK

O APK estará em:
```
[seu_projeto]\app\build\outputs\apk\debug\app-debug.apk
```

Caminho completo:
```
C:\Users\Willdemarques\Documents\dev\UFMA\semestre_3\geoping\app\build\outputs\apk\debug\app-debug.apk
```

### Passo 3: Copiar para Área de Trabalho (Opcional)

**Opção A: Manual**
1. Navegue até a pasta acima
2. Copie `app-debug.apk`
3. Cole na sua área de trabalho ou pasta de fácil acesso

**Opção B: Script Automático**
1. Execute no PowerShell:
   ```powershell
   .\copiar-apk.ps1
   ```
2. O APK será copiado automaticamente para sua área de trabalho
3. O Explorer abrirá mostrando o arquivo

---

## Método 2: Script Automático (Mais Fácil)

Criamos um script que faz tudo automaticamente:

### Como Usar:

```powershell
.\copiar-apk.ps1
```

**O que o script faz:**
1. ✅ Procura o APK gerado
2. ✅ Copia para área de trabalho com nome amigável
3. ✅ Renomeia para `GeoPing-v1.0-debug.apk`
4. ✅ Abre a pasta no Explorer
5. ✅ Mostra instruções de compartilhamento

---

## Como Compartilhar o APK

### Opções de Envio:

#### 1. WhatsApp
- Abra conversa com seu colega
- Clique no 📎 (anexar)
- Selecione "Documento"
- Escolha o arquivo `app-debug.apk` ou `GeoPing-v1.0-debug.apk`
- Envie

#### 2. Telegram
- Abra conversa com seu colega
- Clique no 📎 (anexar)
- Selecione o arquivo APK
- Envie

#### 3. Google Drive
- Acesse drive.google.com
- Faça upload do APK
- Clique com botão direito → "Obter link"
- Compartilhe o link com seu colega

#### 4. Email
- Anexe o arquivo APK no email
- Envie para seu colega

#### 5. Transferência Direta (AirDroid, ShareIt, etc.)
- Use apps de transferência local
- Mais rápido para arquivos grandes

---

## Como Seu Colega Deve Instalar

### No Celular Android:

1. **Baixar o APK**
   - Baixe o arquivo que você enviou
   - Pode estar em Downloads ou no app usado (WhatsApp, Telegram, etc.)

2. **Abrir o Arquivo**
   - Toque no arquivo `app-debug.apk` ou `GeoPing-v1.0-debug.apk`
   - Você pode usar um gerenciador de arquivos

3. **Permitir Instalação**
   - Android vai pedir: "Permitir instalação de apps desconhecidos?"
   - Toque em "Configurações"
   - Ative "Permitir desta fonte"
   - Volte e toque em "Instalar"

4. **Instalar**
   - Aguarde a instalação
   - Toque em "Abrir" ou "Concluir"

5. **Configurar Permissões**
   - O app vai pedir permissões de localização
   - Aceite as permissões necessárias

6. **Testar**
   - Configure o servidor Socket.IO
   - Teste o envio/recebimento de mensagens

---

## Configurações Importantes

### Antes de Compartilhar:

#### 1. Configure o IP do Servidor

**Edite:** `app/src/main/java/com/geoping/services/SocketManager.java`

```java
private static final String SERVER_URL = "http://SEU_IP:3000";
```

**Troque para:**
```java
private static final String SERVER_URL = "http://192.168.18.12:3000";
```

> **⚠️ IMPORTANTE:** Use o IP da sua rede local ou um servidor público!

#### 2. Configure o SSID da Rede

**Edite:** `app/src/main/java/com/geoping/services/WifiProximityService.java`

```java
private static final String TARGET_SSID = "ALMEIDA 2.4G";
```

**Troque para o Wi-Fi que vocês vão testar!**

#### 3. Recompile o APK

Após fazer as alterações:
1. `Build` → `Rebuild Project`
2. `Build` → `Build APK(s)`
3. Compartilhe o novo APK

---

## Diferença: APK Debug vs Release

### APK Debug (app-debug.apk)

**Vantagens:**
- ✅ Rápido de gerar
- ✅ Não precisa de assinatura
- ✅ Ideal para testes

**Desvantagens:**
- ❌ Maior tamanho (não otimizado)
- ❌ Não pode ser publicado na Play Store
- ❌ Contém código de debug

**Quando Usar:**
- Para testes com colegas
- Desenvolvimento
- Demonstrações

### APK Release (app-release.apk)

**Vantagens:**
- ✅ Otimizado (menor tamanho)
- ✅ Sem código de debug
- ✅ Pode ser publicado na Play Store

**Desvantagens:**
- ❌ Precisa de keystore (assinatura)
- ❌ Mais complexo de gerar

**Quando Usar:**
- Versão final
- Publicação
- Distribuição ampla

---

## Gerando APK Release (Opcional)

Para gerar um APK otimizado e assinado:

### Passo 1: Criar Keystore

No Android Studio:
1. `Build` → `Generate Signed Bundle / APK`
2. Selecione `APK`
3. Clique em `Create new...`
4. Preencha os dados:
   - Key store path: escolha local e nome
   - Password: crie uma senha
   - Alias: ex: "geoping"
   - Validity: 25 anos
   - First and Last Name: seu nome
5. Clique `OK`

### Passo 2: Gerar APK Release

1. `Build` → `Generate Signed Bundle / APK`
2. Selecione `APK`
3. Escolha o keystore criado
4. Digite a senha
5. Selecione `release`
6. Marque `V1` e `V2`
7. Clique `Finish`

O APK estará em:
```
app\build\outputs\apk\release\app-release.apk
```

---

## Troubleshooting

### "APK não encontrado após Build"

**Solução:**
1. Certifique-se que o build terminou com sucesso
2. Verifique o painel "Build" no Android Studio
3. Navegue manualmente para: `app\build\outputs\apk\debug\`

### "Não consigo enviar APK pelo WhatsApp"

**Solução:**
- WhatsApp tem limite de 100MB
- Se o APK for maior, use Google Drive ou Telegram

### "Instalação Bloqueada" no celular do colega

**Solução:**
1. Configurações → Segurança
2. Ative "Fontes desconhecidas" ou
3. Permita instalação para o app específico (Chrome, WhatsApp, etc.)

### "App não funciona no celular do colega"

**Verificar:**
- Ambos estão na mesma rede (ou servidor é público)
- IP do servidor está correto
- SSID configurado existe na rede
- Permissões foram concedidas

---

## Checklist Antes de Compartilhar

- [ ] Configurei o IP do servidor correto
- [ ] Configurei o SSID correto (ou deixei genérico)
- [ ] Testei o APK no meu celular
- [ ] Gerei o APK atualizado
- [ ] Copiei o APK para local acessível
- [ ] Escolhi método de compartilhamento
- [ ] Enviei instruções de instalação para colega

---

## Resumo Rápido

### Para Você (Desenvolvedor):

```bash
# No Android Studio
Build → Build APK(s)

# Ou execute
.\copiar-apk.ps1

# Envie o APK via WhatsApp/Telegram/Drive
```

### Para Seu Colega (Usuário):

```bash
1. Baixe o APK
2. Abra o arquivo
3. Permita "Apps desconhecidos"
4. Instale
5. Abra e use!
```

---

## Links Úteis

- [Documentação Android - Instalar APKs](https://developer.android.com/studio/command-line/adb#move)
- [Como Habilitar Fontes Desconhecidas](https://www.androidauthority.com/how-to-install-apks-31494/)

---

**Pronto! Agora você pode compartilhar o GeoPing facilmente!** 🚀


