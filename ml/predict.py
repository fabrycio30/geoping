#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
================================================================================
GeoPing - Script de Inferência para Localização Indoor
================================================================================

Script para realizar predições em tempo real usando o modelo Autoencoder treinado.
Determina se o usuário está dentro ou fora de uma sala específica.

Autor: GeoPing Team
Data: 2025
"""

import os
import sys
import json
import pickle
import numpy as np
from tensorflow import keras
import warnings

warnings.filterwarnings('ignore')

# ================================================================================
# CLASSE DE PREDIÇÃO
# ================================================================================

class IndoorLocationPredictor:
    """
    Classe para realizar predições de localização indoor usando Autoencoder.
    """
    
    def __init__(self, room_label, models_dir='models'):
        """
        Inicializa o preditor carregando modelo e metadados.
        
        Args:
            room_label (str): Nome da sala
            models_dir (str): Diretório onde os modelos estão salvos
        """
        self.room_label = room_label
        self.models_dir = models_dir
        
        # Carregar modelo
        model_path = os.path.join(models_dir, f'{room_label}_autoencoder.h5')
        if not os.path.exists(model_path):
            raise FileNotFoundError(f"Modelo não encontrado: {model_path}")
        self.model = keras.models.load_model(model_path)
        print(f"✓ Modelo carregado: {model_path}")
        
        # Carregar scaler
        scaler_path = os.path.join(models_dir, f'{room_label}_scaler.pkl')
        if not os.path.exists(scaler_path):
            raise FileNotFoundError(f"Scaler não encontrado: {scaler_path}")
        with open(scaler_path, 'rb') as f:
            self.scaler = pickle.load(f)
        print(f"✓ Scaler carregado: {scaler_path}")
        
        # Carregar metadados
        metadata_path = os.path.join(models_dir, f'{room_label}_metadata.json')
        if not os.path.exists(metadata_path):
            raise FileNotFoundError(f"Metadados não encontrados: {metadata_path}")
        with open(metadata_path, 'r') as f:
            self.metadata = json.load(f)
        print(f"✓ Metadados carregados: {metadata_path}")
        
        self.bssids = self.metadata['bssids']
        self.threshold = self.metadata['threshold']
        
        print(f"\n  Sala: {self.room_label}")
        print(f"  BSSIDs conhecidos: {len(self.bssids)}")
        print(f"  Limiar de decisão: {self.threshold:.6f}")
    
    
    def preprocess_wifi_scan(self, wifi_scan_results):
        """
        Pré-processa os resultados de um scan Wi-Fi.
        
        Args:
            wifi_scan_results (list): Lista de dicionários com {bssid, ssid, rssi}
        
        Returns:
            np.array: Vetor normalizado pronto para inferência
        """
        # Criar vetor de zeros
        rssi_vector = np.zeros(len(self.bssids))
        
        # Preencher com valores de RSSI
        for network in wifi_scan_results:
            bssid = network.get('bssid')
            rssi = network.get('rssi')
            
            if bssid in self.bssids:
                bssid_idx = self.bssids.index(bssid)
                # Converter RSSI de dBm (adicionar 100)
                rssi_vector[bssid_idx] = rssi + 100
        
        # Normalizar
        rssi_vector = rssi_vector.reshape(1, -1)
        rssi_vector_normalized = self.scaler.transform(rssi_vector)
        
        return rssi_vector_normalized
    
    
    def predict(self, wifi_scan_results, verbose=True):
        """
        Realiza predição se o usuário está dentro ou fora da sala.
        
        Args:
            wifi_scan_results (list): Lista de dicionários com {bssid, ssid, rssi}
            verbose (bool): Se True, imprime detalhes
        
        Returns:
            dict: Resultado da predição
        """
        # Pré-processar
        X = self.preprocess_wifi_scan(wifi_scan_results)
        
        # Obter reconstrução
        reconstruction = self.model.predict(X, verbose=0)
        
        # Calcular erro de reconstrução
        mse = np.mean(np.square(X - reconstruction))
        
        # Decisão
        is_inside = mse < self.threshold
        confidence = 1.0 - (mse / self.threshold) if is_inside else (mse / self.threshold) - 1.0
        confidence = max(0.0, min(1.0, confidence))  # Limitar entre 0 e 1
        
        result = {
            'room_label': self.room_label,
            'is_inside': bool(is_inside),
            'reconstruction_error': float(mse),
            'threshold': float(self.threshold),
            'confidence': float(confidence),
            'num_networks_detected': len(wifi_scan_results),
            'num_networks_matched': sum(1 for n in wifi_scan_results if n.get('bssid') in self.bssids)
        }
        
        if verbose:
            print("\n" + "=" * 60)
            print(f"  RESULTADO DA PREDIÇÃO - {self.room_label}")
            print("=" * 60)
            print(f"  Status: {'DENTRO' if is_inside else 'FORA'}")
            print(f"  Confiança: {confidence*100:.1f}%")
            print(f"  Erro de reconstrução: {mse:.6f}")
            print(f"  Limiar: {self.threshold:.6f}")
            print(f"  Redes detectadas: {result['num_networks_detected']}")
            print(f"  Redes conhecidas: {result['num_networks_matched']}")
            print("=" * 60 + "\n")
        
        return result


# ================================================================================
# FUNÇÃO PRINCIPAL PARA TESTE
# ================================================================================

def main():
    """
    Função principal para teste do preditor.
    """
    print("=" * 80)
    print("  GeoPing - Predição de Localização Indoor")
    print("=" * 80)
    
    # Solicitar nome da sala
    if len(sys.argv) > 1:
        room_label = sys.argv[1]
    else:
        room_label = input("\nDigite o nome da sala: ").strip()
    
    if not room_label:
        print("✗ Nome da sala não pode ser vazio")
        sys.exit(1)
    
    print(f"\nCarregando modelo para: {room_label}")
    print("-" * 80)
    
    # Inicializar preditor
    try:
        predictor = IndoorLocationPredictor(room_label)
    except FileNotFoundError as e:
        print(f"✗ Erro: {e}")
        sys.exit(1)
    
    # Exemplo de uso com dados simulados
    print("\nExemplo de predição com dados simulados:")
    print("-" * 80)
    
    # Simular scan Wi-Fi (substitua por dados reais em produção)
    example_scan = [
        {"bssid": "00:11:22:33:44:55", "ssid": "WiFi_Lab", "rssi": -65},
        {"bssid": "AA:BB:CC:DD:EE:FF", "ssid": "WiFi_Corredor", "rssi": -78},
        {"bssid": "11:22:33:44:55:66", "ssid": "WiFi_Outro", "rssi": -82}
    ]
    
    result = predictor.predict(example_scan)
    
    # Mostrar como usar em produção
    print("\nUso programático:")
    print("-" * 80)
    print("```python")
    print(f"predictor = IndoorLocationPredictor('{room_label}')")
    print("result = predictor.predict(wifi_scan_results, verbose=False)")
    print("if result['is_inside']:")
    print("    print('Usuário está dentro da sala')")
    print("else:")
    print("    print('Usuário está fora da sala')")
    print("```")


if __name__ == "__main__":
    main()

