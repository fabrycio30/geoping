#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
================================================================================
GeoPing - Utilitários para Machine Learning
================================================================================

Funções auxiliares para visualização, análise e debugging do sistema.

Autor: GeoPing Team
Data: 2025
"""

import numpy as np
import pandas as pd
import matplotlib.pyplot as plt
import seaborn as sns
from sklearn.decomposition import PCA
from sklearn.manifold import TSNE


def plot_bssid_frequency(df, room_label):
    """
    Plota a frequência de aparição de cada BSSID nos dados.
    
    Args:
        df: DataFrame com os dados de treinamento
        room_label: Nome da sala
    """
    # Coletar todos os BSSIDs
    bssid_counts = {}
    
    for idx, row in df.iterrows():
        for network in row['wifi_fingerprint']:
            bssid = network['bssid']
            bssid_counts[bssid] = bssid_counts.get(bssid, 0) + 1
    
    # Ordenar por frequência
    sorted_bssids = sorted(bssid_counts.items(), key=lambda x: x[1], reverse=True)
    
    # Plotar top 20
    top_n = 20
    bssids = [x[0][:17] for x in sorted_bssids[:top_n]]  # Truncar BSSID
    counts = [x[1] for x in sorted_bssids[:top_n]]
    
    plt.figure(figsize=(12, 6))
    plt.bar(range(len(bssids)), counts)
    plt.xticks(range(len(bssids)), bssids, rotation=45, ha='right')
    plt.xlabel('BSSID')
    plt.ylabel('Frequência de Aparição')
    plt.title(f'Top {top_n} BSSIDs Mais Frequentes - {room_label}')
    plt.tight_layout()
    plt.savefig(f'models/{room_label}_bssid_frequency.png', dpi=150)
    print(f"Gráfico salvo: models/{room_label}_bssid_frequency.png")
    plt.close()


def plot_rssi_distribution(df, room_label):
    """
    Plota a distribuição de RSSI de todos os sinais.
    
    Args:
        df: DataFrame com os dados de treinamento
        room_label: Nome da sala
    """
    all_rssi = []
    
    for idx, row in df.iterrows():
        for network in row['wifi_fingerprint']:
            all_rssi.append(network['rssi'])
    
    plt.figure(figsize=(10, 6))
    plt.hist(all_rssi, bins=50, alpha=0.7, color='blue', edgecolor='black')
    plt.axvline(np.mean(all_rssi), color='red', linestyle='--', 
                label=f'Média: {np.mean(all_rssi):.1f} dBm')
    plt.xlabel('RSSI (dBm)')
    plt.ylabel('Frequência')
    plt.title(f'Distribuição de RSSI - {room_label}')
    plt.legend()
    plt.grid(True, alpha=0.3)
    plt.tight_layout()
    plt.savefig(f'models/{room_label}_rssi_distribution.png', dpi=150)
    print(f"Gráfico salvo: models/{room_label}_rssi_distribution.png")
    plt.close()


def plot_networks_per_scan(df, room_label):
    """
    Plota o número de redes detectadas por scan ao longo do tempo.
    
    Args:
        df: DataFrame com os dados de treinamento
        room_label: Nome da sala
    """
    networks_count = [len(row['wifi_fingerprint']) for idx, row in df.iterrows()]
    
    plt.figure(figsize=(12, 6))
    plt.plot(networks_count, marker='o', markersize=2, linewidth=0.5)
    plt.axhline(np.mean(networks_count), color='red', linestyle='--', 
                label=f'Média: {np.mean(networks_count):.1f} redes')
    plt.xlabel('Scan #')
    plt.ylabel('Número de Redes Detectadas')
    plt.title(f'Redes Detectadas por Scan - {room_label}')
    plt.legend()
    plt.grid(True, alpha=0.3)
    plt.tight_layout()
    plt.savefig(f'models/{room_label}_networks_per_scan.png', dpi=150)
    print(f"Gráfico salvo: models/{room_label}_networks_per_scan.png")
    plt.close()


def visualize_latent_space(model, X_train, room_label):
    """
    Visualiza o espaço latente (bottleneck) do Autoencoder usando PCA e t-SNE.
    
    Args:
        model: Modelo Autoencoder treinado
        X_train: Dados de treinamento
        room_label: Nome da sala
    """
    # Criar modelo encoder apenas (até o latent space)
    from tensorflow.keras.models import Model
    encoder = Model(inputs=model.input, 
                   outputs=model.get_layer('latent_space').output)
    
    # Obter representações latentes
    latent_representations = encoder.predict(X_train, verbose=0)
    
    # PCA para 2D
    if latent_representations.shape[1] > 2:
        pca = PCA(n_components=2)
        latent_2d_pca = pca.fit_transform(latent_representations)
        
        plt.figure(figsize=(10, 8))
        plt.scatter(latent_2d_pca[:, 0], latent_2d_pca[:, 1], alpha=0.5)
        plt.xlabel(f'PC1 ({pca.explained_variance_ratio_[0]*100:.1f}%)')
        plt.ylabel(f'PC2 ({pca.explained_variance_ratio_[1]*100:.1f}%)')
        plt.title(f'Espaço Latente (PCA) - {room_label}')
        plt.grid(True, alpha=0.3)
        plt.tight_layout()
        plt.savefig(f'models/{room_label}_latent_space_pca.png', dpi=150)
        print(f"Gráfico salvo: models/{room_label}_latent_space_pca.png")
        plt.close()
    
    # t-SNE para 2D (para datasets maiores)
    if len(X_train) > 50:
        tsne = TSNE(n_components=2, random_state=42, perplexity=min(30, len(X_train)-1))
        latent_2d_tsne = tsne.fit_transform(latent_representations)
        
        plt.figure(figsize=(10, 8))
        plt.scatter(latent_2d_tsne[:, 0], latent_2d_tsne[:, 1], alpha=0.5)
        plt.xlabel('t-SNE 1')
        plt.ylabel('t-SNE 2')
        plt.title(f'Espaço Latente (t-SNE) - {room_label}')
        plt.grid(True, alpha=0.3)
        plt.tight_layout()
        plt.savefig(f'models/{room_label}_latent_space_tsne.png', dpi=150)
        print(f"Gráfico salvo: models/{room_label}_latent_space_tsne.png")
        plt.close()


def analyze_reconstruction_quality(model, X_train, bssids, room_label, num_samples=5):
    """
    Analisa a qualidade da reconstrução para algumas amostras.
    
    Args:
        model: Modelo treinado
        X_train: Dados de treinamento
        bssids: Lista de BSSIDs
        room_label: Nome da sala
        num_samples: Número de amostras a analisar
    """
    # Selecionar amostras aleatórias
    indices = np.random.choice(len(X_train), num_samples, replace=False)
    
    fig, axes = plt.subplots(num_samples, 1, figsize=(15, 3*num_samples))
    
    for i, idx in enumerate(indices):
        original = X_train[idx:idx+1]
        reconstructed = model.predict(original, verbose=0)
        
        ax = axes[i] if num_samples > 1 else axes
        
        # Plotar apenas BSSIDs não-zero
        non_zero_mask = original[0] > 0
        x_pos = np.where(non_zero_mask)[0]
        
        if len(x_pos) > 0:
            ax.plot(x_pos, original[0][non_zero_mask], 'o-', 
                   label='Original', markersize=4)
            ax.plot(x_pos, reconstructed[0][non_zero_mask], 's-', 
                   label='Reconstruído', markersize=4, alpha=0.7)
            ax.set_xlabel('Índice do BSSID')
            ax.set_ylabel('RSSI Normalizado')
            ax.set_title(f'Amostra {idx} - MSE: {np.mean(np.square(original - reconstructed)):.6f}')
            ax.legend()
            ax.grid(True, alpha=0.3)
    
    plt.tight_layout()
    plt.savefig(f'models/{room_label}_reconstruction_quality.png', dpi=150)
    print(f"Gráfico salvo: models/{room_label}_reconstruction_quality.png")
    plt.close()


def print_dataset_summary(df):
    """
    Imprime um resumo estatístico do dataset.
    
    Args:
        df: DataFrame com os dados
    """
    print("\n" + "="*60)
    print("  RESUMO DO DATASET")
    print("="*60)
    
    print(f"\nTotal de amostras: {len(df)}")
    print(f"Período: {df['scan_timestamp'].min()} a {df['scan_timestamp'].max()}")
    print(f"Dispositivos únicos: {df['device_id'].nunique()}")
    
    # Estatísticas de redes
    networks_per_scan = [len(row['wifi_fingerprint']) for _, row in df.iterrows()]
    print(f"\nRedes por scan:")
    print(f"  Média: {np.mean(networks_per_scan):.1f}")
    print(f"  Mediana: {np.median(networks_per_scan):.0f}")
    print(f"  Min: {np.min(networks_per_scan)}")
    print(f"  Max: {np.max(networks_per_scan)}")
    
    # BSSIDs únicos
    all_bssids = set()
    all_rssi = []
    for _, row in df.iterrows():
        for network in row['wifi_fingerprint']:
            all_bssids.add(network['bssid'])
            all_rssi.append(network['rssi'])
    
    print(f"\nBSSIDs únicos: {len(all_bssids)}")
    print(f"\nRSSI:")
    print(f"  Média: {np.mean(all_rssi):.1f} dBm")
    print(f"  Mediana: {np.median(all_rssi):.1f} dBm")
    print(f"  Min: {np.min(all_rssi)} dBm")
    print(f"  Max: {np.max(all_rssi)} dBm")
    
    print("="*60 + "\n")


if __name__ == "__main__":
    print("Este é um módulo de utilitários. Importe-o em outros scripts.")
    print("Exemplo: from utils import plot_bssid_frequency")





