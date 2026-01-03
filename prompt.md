**Role:** Atue como um Engenheiro Full-Stack Sênior e Cientista de Dados especializado em IoT e Sistemas de Localização Indoor (RTLS).

**Contexto do Projeto (GeoPing):**
Estou desenvolvendo um protótipo para validar uma trabalho cientifico de Comptação móvel  sobre localização indoor. O objetivo é diferenciar se um usuário está dentro de uma sala específica, algo que métodos tradicionais baseados apenas em um RSSI falham. 
Vamos utilizar uma abordagem de **One-Class Classification** com  **Autoencoders** . O Android coletará o "fingerprint" completo de todas as redes Wi-Fi visíveis, enviará para o servidor, e um script Python treinará o modelo para reconhecer a "assinatura" daquela sala.

Detalhamento da aplicação: "Visando superar as limitações de precisão inerentes ao uso simples de limiares de RSSI, como a instabilidade do sinal causada por multicaminho, o sistema adota uma estratégia de detecção de anomalias baseada em Aprendizado Profundo (Deep Learning).

A metodologia adapta o conceito do ZeroTouch (que utiliza sensores fixos na sala para coletar dados e treinar um modelo autoencoder), apresentado por Nikola et al. (2025), para uma topologia INVERTIDA:

1. Vetor de Características Agregado: Em vez de depender apenas do sinal da rede alvo (SSID da cerca), o aplicativo coleta um vetor composto pelos níveis de sinal (RSSI) e identificadores físicos (BSSID) de todas as redes Wi-Fi visíveis no ambiente. A literatura demonstra que a agregação de múltiplos pontos de sinal cria uma "assinatura radioelétrica" robusta, difícil de ser falsificada ou replicada acidentalmente em andares adjacentes,.
2. Classificação de Classe Única (One-Class Classification): Utiliza-se um Autoencoder (Rede Neural) treinado exclusivamente com dados positivos ("usuário presente na sala"). O modelo aprende a comprimir e reconstruir o padrão de sinais típico do ambiente.
3. Inferência: Durante a operação, o sistema calcula o Erro de Reconstrução entre o vetor de sinais atual e a saída do Autoencoder. Se o usuário estiver fora da sala (ou no andar vizinho), a combinação de redes visíveis muda, elevando o erro de reconstrução. Se esse erro ultrapassar um limiar de decisão (δ), o sistema classifica o usuário como "Ausente".

Calibração Heurística do Limiar Para lidar com a variabilidade física dos ambientes, dados heurísticos fornecidos no cadastro da sala (como dimensões, quantidade de cômodos e densidade de obstáculos) não são utilizados como entrada da rede neural, mas sim para a calibração dinâmica do Limiar de Decisão (δ). Baseando-se em métodos estatísticos como o Intervalo Interquartil (IQR),, o sistema ajusta a sensibilidade do algoritmo: ambientes com alta complexidade arquitetônica (que geram maior atenuação e ruído no sinal) recebem automaticamente um limiar mais tolerante, reduzindo falsos negativos sem comprometer a segurança da cerca digital.

"

**Stack Tecnológica:**

1. **Mobile:** Android Nativo (Java) - *Conforme documentação do projeto.*
2. **Backend:** Node.js (Express) + Socket.io.
3. **Banco de Dados:** PostgreSQL (com JSONB para flexibilidade dos dados de sensores).
4. **Data Science:** Python (Pandas, Scikit-Learn, TensorFlow/Keras).

**Objetivo da Tarefa:**
Criar o código necessário para as três camadas (Banco, Backend, Mobile) para realizar a **Coleta de Dados** e o  **Treinamento do Modelo** .

---

#### 📌 Passo 1: Banco de Dados (PostgreSQL)

Crie o script SQL (`init.sql`) para criar a tabela de dados brutos de treinamento.

* Tabela `wifi_training_data`:
  * `id` (serial)
  * `room_label` (varchar) - Ex: "LAB_LESERC"
  * `scan_timestamp` (timestamp)
  * `device_id` (varchar)
  * `wifi_fingerprint` (JSONB) - Deve armazenar um array de objetos, onde cada objeto contém `{ "bssid":String, "ssid":String, "rssi":Int }`.
  * `heuristics` (JSONB) - Para armazenar dados da sala (dimensões, obstáculos) para calibração futura.

---

#### 📌 Passo 2: Backend (Node.js)

Crie um servidor simples em `server.js` com Express.

* Configure a conexão com PostgreSQL (`pg`).
* Crie uma rota `POST /api/collect`:
  * Recebe o JSON do Android contendo: `room_label`, `device_id`, `wifi_scan_results` (lista de redes).
  * Salva exatamente esse payload na tabela `wifi_training_data`.
  * Retorna 200 OK.
* *Nota:* Mantenha o código limpo e pronto para rodar localmente.

---

#### 📌 Passo 3: Cliente Android (Java)

Preciso de uma `Activity` chamada `DataCollectionActivity.java` e seu layout XML.

* **Permissões:** Inclua as permissões de localização necessárias no `AndroidManifest.xml` (`ACCESS_FINE_LOCATION`, `ACCESS_WIFI_STATE`, `CHANGE_WIFI_STATE`) para permitir escaneamento de Wi-Fi no Android 10+.
* **UI:**
  * Um `EditText` para o usuário digitar o "Nome da Sala" (Label).
  * Um `Button` "Iniciar Coleta" e "Parar Coleta".
  * Um `TextView` para mostrar logs (ex: "Scan #5 enviado...").
* **Lógica (`WifiManager`):**
  * Ao iniciar, crie um loop (Timer ou Handler) que executa a cada 3 segundos.
  * Dispare `wifiManager.startScan()`.
  * Registre um `BroadcastReceiver` para ouvir `SCAN_RESULTS_AVAILABLE_ACTION`.
  * No Receiver, pegue a lista `wifiManager.getScanResults()`.
  * Monte um objeto JSON contendo **todas** as redes encontradas (BSSID, SSID, RSSI).
  * Envie via HTTP POST (use `Retrofit` ou `OkHttp`) para `http://<IP_DO_SEU_PC>:3000/api/collect`.

---

#### 📌 Passo 4: Modelo de Machine Learning (Python)

Crie um script `train_autoencoder.py`.

1. **Carga de Dados:** Conecte no Postgres e baixe os dados da sala "LAB_LESERC" (ou a label que usarmos).
2. **Pré-processamento (Crucial):**
   * Converta o JSONB em uma matriz esparsa ou DataFrame.
   * **Colunas:** Devem ser os BSSIDs únicos encontrados em todo o dataset.
   * **Valores:** Normalize o RSSI. (Ex: converta -100dBm a -30dBm para uma escala 0 a 1). Se o BSSID não foi visto naquele scan, valor é 0.
3. **Arquitetura do Modelo (Autoencoder):**
   * Input Layer: Tamanho = número de BSSIDs únicos.
   * Encoder: Camadas densas reduzindo a dimensão (ex: 64 -> 32 -> 16).
   * Bottleneck (Latent Space).
   * Decoder: Camadas densas aumentando a dimensão (16 -> 32 -> 64).
   * Output Layer: Mesmo tamanho do Input.
4. **Treinamento:**
   * Treine o modelo usando os dados coletados (apenas dados da classe positiva, ou seja, "dentro da sala").
   * Use `MSE` (Mean Squared Error) como Loss function.
5. **Definição de Limiar (Thresholding):**
   * Após treinar, passe os dados de treino pelo modelo e calcule o erro de reconstrução (MSE) para cada amostra.
   * Calcule o **IQR (Intervalo Interquartil)** da distribuição de erros.
   * Defina o limiar de corte como `Q3 + 1.5 * IQR` (ou use um percentil, ex: 95%).
   * Salve o modelo (`.h5` ou `.keras`) e a lista de colunas (BSSIDs) em um arquivo JSON/Pickle para uso posterior.

**Gere o código completo, comentado e com instruções de como rodar cada parte.**
