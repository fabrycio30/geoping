#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
================================================================================
GeoPing - Treinamento do Modelo Autoencoder para Localização Indoor
================================================================================

Script para treinar um modelo Autoencoder (One-Class Classification) capaz de
reconhecer a "assinatura radioelétrica" de uma sala específica.

Metodologia baseada em Nikola et al. (2025) - ZeroTouch adaptado para topologia invertida.

Autor: GeoPing Team
Data: 2025
"""

import os
import sys
import json

# Configurar encoding UTF-8 para output no Windows
if sys.platform == 'win32':
    import codecs
    sys.stdout = codecs.getwriter('utf-8')(sys.stdout.buffer, 'strict')
    sys.stderr = codecs.getwriter('utf-8')(sys.stderr.buffer, 'strict')
import pickle
import numpy as np
import pandas as pd
import psycopg2
from psycopg2.extras import RealDictCursor
from datetime import datetime
import matplotlib.pyplot as plt
from sklearn.preprocessing import MinMaxScaler
from tensorflow import keras
from tensorflow.keras import layers
from tensorflow.keras.models import Model
import warnings

warnings.filterwarnings('ignore')

# ================================================================================
# CONFIGURAÇÕES
# ================================================================================

# Configuração do Banco de Dados
DB_CONFIG = {
    'host': os.getenv('DB_HOST', 'localhost'),
    'database': os.getenv('DB_NAME', 'geo_ping'),
    'user': os.getenv('DB_USER', 'postgres'),
    'password': os.getenv('DB_PASSWORD', '123456'),
    'port': os.getenv('DB_PORT', '5432')
}

# Configuração do Modelo
MODEL_CONFIG = {
    'encoding_dim': 16,         # Dimensão do bottleneck (latent space)
    'hidden_layers': [64, 32],  # Camadas do encoder
    'activation': 'relu',
    'output_activation': 'sigmoid',
    'loss': 'mse',
    'optimizer': 'adam',
    'epochs': 100,
    'batch_size': 32,
    'validation_split': 0.2
}

# Configuração do Limiar
THRESHOLD_CONFIG = {
    'method': 'iqr',           # 'iqr' ou 'percentile'
    'iqr_multiplier': 1.5,     # Q3 + 1.5 * IQR
    'percentile': 95           # Para método percentile
}

# Diretório para salvar modelos
OUTPUT_DIR = 'models'
os.makedirs(OUTPUT_DIR, exist_ok=True)

# ================================================================================
# FUNÇÕES DE CONEXÃO E CARGA DE DADOS
# ================================================================================

def connect_database():
    """
    Estabelece conexão com o banco de dados PostgreSQL.
    
    Returns:
        connection: Objeto de conexão do psycopg2
    """
    try:
        conn = psycopg2.connect(**DB_CONFIG)
        print(f"[OK] Conexao estabelecida com o banco de dados '{DB_CONFIG['database']}'")
        return conn
    except Exception as e:
        print(f"[ERRO] Erro ao conectar ao banco de dados: {e}")
        sys.exit(1)


def load_training_data(room_label):
    """
    Carrega os dados de treinamento de uma sala específica do banco de dados.
    
    Args:
        room_label (str): Nome/label da sala (ex: 'LAB_LESERC')
    
    Returns:
        pd.DataFrame: DataFrame com os dados de treinamento
    """
    conn = connect_database()
    
    query = """
        SELECT 
            id,
            room_label,
            scan_timestamp,
            device_id,
            wifi_fingerprint,
            heuristics
        FROM wifi_training_data
        WHERE room_label = %s
        ORDER BY scan_timestamp ASC
    """
    
    try:
        df = pd.read_sql_query(query, conn, params=(room_label,))
        conn.close()
        
        if len(df) == 0:
            print(f"[ERRO] Nenhum dado encontrado para a sala '{room_label}'")
            sys.exit(1)
        
        print(f" {len(df)} amostras carregadas para a sala '{room_label}'")
        return df
        
    except Exception as e:
        print(f"Erro ao carregar dados: {e}")
        conn.close()
        sys.exit(1)


# ================================================================================
# PRÉ-PROCESSAMENTO DOS DADOS
# ================================================================================

def extract_bssids_and_rssi(df):
    """
    Extrai todos os BSSIDs únicos e cria uma matriz de RSSI.
    
    Args:
        df (pd.DataFrame): DataFrame com os dados brutos
    
    Returns:
        tuple: (matriz_rssi, lista_bssids, scaler)
    """
    print("\n[1/5] Pré-processamento dos dados...")
    
    # Coletar todos os BSSIDs únicos do dataset
    all_bssids = set()
    for idx, row in df.iterrows():
        fingerprint = row['wifi_fingerprint']
        for network in fingerprint:
            all_bssids.add(network['bssid'])
    
    all_bssids = sorted(list(all_bssids))
    print(f"  → {len(all_bssids)} BSSIDs únicos encontrados")
    
    # Criar matriz esparsa: linhas = amostras, colunas = BSSIDs
    rssi_matrix = []
    
    for idx, row in df.iterrows():
        # Inicializar vetor com zeros
        rssi_vector = np.zeros(len(all_bssids))
        
        # Preencher com valores de RSSI onde disponível
        fingerprint = row['wifi_fingerprint']
        for network in fingerprint:
            bssid = network['bssid']
            rssi = network['rssi']
            
            if bssid in all_bssids:
                bssid_idx = all_bssids.index(bssid)
                # Converter RSSI de dBm para valor positivo (tipicamente -100 a -30)
                # Adicionar 100 para deixar positivo
                rssi_vector[bssid_idx] = rssi + 100
        
        rssi_matrix.append(rssi_vector)
    
    rssi_matrix = np.array(rssi_matrix)
    print(f"  → Matriz criada: {rssi_matrix.shape[0]} amostras × {rssi_matrix.shape[1]} features")
    
    # Normalizar valores entre 0 e 1
    scaler = MinMaxScaler()
    rssi_matrix_normalized = scaler.fit_transform(rssi_matrix)
    
    print(f"  → Dados normalizados (min=0, max=1)")
    
    return rssi_matrix_normalized, all_bssids, scaler


# ================================================================================
# CONSTRUÇÃO E TREINAMENTO DO AUTOENCODER
# ================================================================================

def build_autoencoder(input_dim):
    """
    Constrói a arquitetura do Autoencoder.
    
    Args:
        input_dim (int): Dimensão do vetor de entrada (número de BSSIDs)
    
    Returns:
        Model: Modelo Keras compilado
    """
    print("\n[2/5] Construindo arquitetura do Autoencoder...")
    
    # Input Layer
    input_layer = layers.Input(shape=(input_dim,))
    
    # Encoder
    encoded = input_layer
    for units in MODEL_CONFIG['hidden_layers']:
        encoded = layers.Dense(units, activation=MODEL_CONFIG['activation'])(encoded)
    
    # Bottleneck (Latent Space)
    latent = layers.Dense(MODEL_CONFIG['encoding_dim'], activation=MODEL_CONFIG['activation'], 
                         name='latent_space')(encoded)
    
    # Decoder (espelho do encoder)
    decoded = latent
    for units in reversed(MODEL_CONFIG['hidden_layers']):
        decoded = layers.Dense(units, activation=MODEL_CONFIG['activation'])(decoded)
    
    # Output Layer
    output_layer = layers.Dense(input_dim, activation=MODEL_CONFIG['output_activation'])(decoded)
    
    # Criar modelo
    autoencoder = Model(inputs=input_layer, outputs=output_layer, name='autoencoder')
    
    # Compilar
    autoencoder.compile(
        optimizer=MODEL_CONFIG['optimizer'],
        loss=MODEL_CONFIG['loss']
    )
    
    print(f"  → Arquitetura criada:")
    print(f"     Input: {input_dim} features")
    print(f"     Encoder: {' → '.join(map(str, MODEL_CONFIG['hidden_layers']))}")
    print(f"     Latent Space: {MODEL_CONFIG['encoding_dim']}")
    print(f"     Decoder: {' → '.join(map(str, reversed(MODEL_CONFIG['hidden_layers'])))}")
    print(f"     Output: {input_dim} features")
    
    return autoencoder


def train_autoencoder(model, X_train):
    """
    Treina o modelo Autoencoder.
    
    Args:
        model: Modelo Keras
        X_train: Dados de treinamento
    
    Returns:
        History: Histórico do treinamento
    """
    print("\n[3/5] Treinando o modelo...")
    print(f"  → Epochs: {MODEL_CONFIG['epochs']}")
    print(f"  → Batch size: {MODEL_CONFIG['batch_size']}")
    print(f"  → Validation split: {MODEL_CONFIG['validation_split']}")
    
    history = model.fit(
        X_train, X_train,  # Autoencoder: input = output
        epochs=MODEL_CONFIG['epochs'],
        batch_size=MODEL_CONFIG['batch_size'],
        validation_split=MODEL_CONFIG['validation_split'],
        shuffle=True,
        verbose=1
    )
    
    print("  [OK] Treinamento concluido")
    return history


# ================================================================================
# CÁLCULO DO LIMIAR DE DECISÃO
# ================================================================================

def calculate_threshold(model, X_train):
    """
    Calcula o limiar de decisão baseado no erro de reconstrução.
    
    Args:
        model: Modelo treinado
        X_train: Dados de treinamento
    
    Returns:
        float: Valor do limiar
    """
    print("\n[4/5] Calculando limiar de decisão...")
    
    # Obter reconstruções
    reconstructions = model.predict(X_train, verbose=0)
    
    # Calcular erro de reconstrução (MSE) para cada amostra
    mse_per_sample = np.mean(np.square(X_train - reconstructions), axis=1)
    
    print(f"  → Erro de reconstrução médio: {np.mean(mse_per_sample):.6f}")
    print(f"  → Desvio padrão: {np.std(mse_per_sample):.6f}")
    print(f"  → Min: {np.min(mse_per_sample):.6f}, Max: {np.max(mse_per_sample):.6f}")
    
    # Calcular limiar
    if THRESHOLD_CONFIG['method'] == 'iqr':
        # Método IQR (Intervalo Interquartil)
        Q1 = np.percentile(mse_per_sample, 25)
        Q3 = np.percentile(mse_per_sample, 75)
        IQR = Q3 - Q1
        threshold = Q3 + THRESHOLD_CONFIG['iqr_multiplier'] * IQR
        print(f"  → Método: IQR")
        print(f"     Q1={Q1:.6f}, Q3={Q3:.6f}, IQR={IQR:.6f}")
        print(f"     Limiar = Q3 + {THRESHOLD_CONFIG['iqr_multiplier']} × IQR = {threshold:.6f}")
    
    elif THRESHOLD_CONFIG['method'] == 'percentile':
        # Método Percentil
        threshold = np.percentile(mse_per_sample, THRESHOLD_CONFIG['percentile'])
        print(f"  → Método: Percentil {THRESHOLD_CONFIG['percentile']}")
        print(f"     Limiar = {threshold:.6f}")
    
    else:
        print(f"  [ERRO] Metodo desconhecido: {THRESHOLD_CONFIG['method']}")
        sys.exit(1)
    
    return threshold, mse_per_sample


# ================================================================================
# VISUALIZAÇÃO E SALVAMENTO
# ================================================================================

def plot_training_history(history, room_label):
    """
    Plota gráfico do histórico de treinamento.
    
    Args:
        history: Histórico do treinamento
        room_label: Nome da sala
    """
    plt.figure(figsize=(12, 4))
    
    # Loss
    plt.subplot(1, 2, 1)
    plt.plot(history.history['loss'], label='Training Loss')
    plt.plot(history.history['val_loss'], label='Validation Loss')
    plt.title(f'Histórico de Treinamento - {room_label}')
    plt.xlabel('Epoch')
    plt.ylabel('Loss (MSE)')
    plt.legend()
    plt.grid(True, alpha=0.3)
    
    # Loss em escala log
    plt.subplot(1, 2, 2)
    plt.plot(history.history['loss'], label='Training Loss')
    plt.plot(history.history['val_loss'], label='Validation Loss')
    plt.title(f'Histórico de Treinamento (Log Scale) - {room_label}')
    plt.xlabel('Epoch')
    plt.ylabel('Loss (MSE)')
    plt.yscale('log')
    plt.legend()
    plt.grid(True, alpha=0.3)
    
    plt.tight_layout()
    filename = os.path.join(OUTPUT_DIR, f'{room_label}_training_history.png')
    plt.savefig(filename, dpi=150)
    print(f"  [OK] Grafico salvo: {filename}")
    plt.close()


def plot_reconstruction_errors(mse_per_sample, threshold, room_label):
    """
    Plota histograma dos erros de reconstrução.
    
    Args:
        mse_per_sample: Array com os MSEs
        threshold: Valor do limiar
        room_label: Nome da sala
    """
    plt.figure(figsize=(10, 6))
    
    plt.hist(mse_per_sample, bins=50, alpha=0.7, color='blue', edgecolor='black')
    plt.axvline(threshold, color='red', linestyle='--', linewidth=2, 
                label=f'Limiar = {threshold:.6f}')
    plt.title(f'Distribuição dos Erros de Reconstrução - {room_label}')
    plt.xlabel('Erro de Reconstrução (MSE)')
    plt.ylabel('Frequência')
    plt.legend()
    plt.grid(True, alpha=0.3)
    
    filename = os.path.join(OUTPUT_DIR, f'{room_label}_reconstruction_errors.png')
    plt.savefig(filename, dpi=150)
    print(f"  [OK] Grafico salvo: {filename}")
    plt.close()


def save_model_and_metadata(model, bssids, scaler, threshold, room_label, df):
    """
    Salva o modelo treinado e metadados associados.
    
    Args:
        model: Modelo treinado
        bssids: Lista de BSSIDs
        scaler: Scaler utilizado
        threshold: Limiar calculado
        room_label: Nome da sala
        df: DataFrame original
    """
    print("\n[5/5] Salvando modelo e metadados...")
    
    # Salvar modelo Keras
    model_path = os.path.join(OUTPUT_DIR, f'{room_label}_autoencoder.h5')
    model.save(model_path)
    print(f"  [OK] Modelo salvo: {model_path}")
    
    # Salvar scaler
    scaler_path = os.path.join(OUTPUT_DIR, f'{room_label}_scaler.pkl')
    with open(scaler_path, 'wb') as f:
        pickle.dump(scaler, f)
    print(f"  [OK] Scaler salvo: {scaler_path}")
    
    # Salvar metadados
    metadata = {
        'room_label': room_label,
        'bssids': bssids,
        'threshold': float(threshold),
        'num_samples': len(df),
        'num_bssids': len(bssids),
        'model_config': MODEL_CONFIG,
        'threshold_config': THRESHOLD_CONFIG,
        'training_date': datetime.now().isoformat(),
        'db_config': {k: v for k, v in DB_CONFIG.items() if k != 'password'}
    }
    
    metadata_path = os.path.join(OUTPUT_DIR, f'{room_label}_metadata.json')
    with open(metadata_path, 'w') as f:
        json.dump(metadata, f, indent=2)
    print(f"  [OK] Metadados salvos: {metadata_path}")
    
    # Salvar lista de BSSIDs
    bssids_path = os.path.join(OUTPUT_DIR, f'{room_label}_bssids.json')
    with open(bssids_path, 'w') as f:
        json.dump(bssids, f, indent=2)
    print(f"  [OK] Lista de BSSIDs salva: {bssids_path}")


# ================================================================================
# FUNÇÃO PRINCIPAL
# ================================================================================

def main():
    """
    Função principal que executa todo o pipeline de treinamento.
    """
    print("=" * 80)
    print("  GeoPing - Treinamento do Modelo Autoencoder")
    print("  Sistema de Localização Indoor usando One-Class Classification")
    print("=" * 80)
    
    # Solicitar nome da sala
    if len(sys.argv) > 1:
        room_label = sys.argv[1]
    else:
        room_label = input("\nDigite o nome da sala para treinar (ex: LAB_LESERC): ").strip()
    
    if not room_label:
        print("[ERRO] Nome da sala nao pode ser vazio")
        sys.exit(1)
    
    print(f"\nSala selecionada: {room_label}")
    print("-" * 80)
    
    # 1. Carregar dados
    df = load_training_data(room_label)
    
    # 2. Pré-processar
    X_train, bssids, scaler = extract_bssids_and_rssi(df)
    
    # 3. Construir modelo
    model = build_autoencoder(input_dim=X_train.shape[1])
    
    # 4. Treinar modelo
    history = train_autoencoder(model, X_train)
    
    # 5. Calcular limiar
    threshold, mse_per_sample = calculate_threshold(model, X_train)
    
    # 6. Visualizar resultados
    plot_training_history(history, room_label)
    plot_reconstruction_errors(mse_per_sample, threshold, room_label)
    
    # 7. Salvar tudo
    save_model_and_metadata(model, bssids, scaler, threshold, room_label, df)
    
    # Resumo final
    print("\n" + "=" * 80)
    print("  TREINAMENTO CONCLUÍDO COM SUCESSO")
    print("=" * 80)
    print(f"\n  Sala: {room_label}")
    print(f"  Amostras utilizadas: {len(df)}")
    print(f"  BSSIDs únicos: {len(bssids)}")
    print(f"  Dimensão do latent space: {MODEL_CONFIG['encoding_dim']}")
    print(f"  Limiar de decisão: {threshold:.6f}")
    print(f"\n  Arquivos salvos em: {OUTPUT_DIR}/")
    print("=" * 80 + "\n")


if __name__ == "__main__":
    main()

