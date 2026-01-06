#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
================================================================================
GeoPing - Script de Inferência em Tempo Real (stdin/stdout)
================================================================================

Script para realizar predições em tempo real via stdin/stdout.
Formato de entrada: JSON com dados do scan Wi-Fi
Formato de saída: JSON com resultado da predição

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

# Forçar UTF-8 no Windows
if sys.platform == 'win32':
    import io
    sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8', errors='replace')
    sys.stderr = io.TextIOWrapper(sys.stderr.buffer, encoding='utf-8', errors='replace')

class RealtimePredictor:
    """
    Preditor de localização indoor em tempo real.
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
        self.model = keras.models.load_model(model_path, compile=False)
        
        # Carregar scaler
        scaler_path = os.path.join(models_dir, f'{room_label}_scaler.pkl')
        if not os.path.exists(scaler_path):
            raise FileNotFoundError(f"Scaler não encontrado: {scaler_path}")
        with open(scaler_path, 'rb') as f:
            self.scaler = pickle.load(f)
        
        # Carregar metadados
        metadata_path = os.path.join(models_dir, f'{room_label}_metadata.json')
        if not os.path.exists(metadata_path):
            raise FileNotFoundError(f"Metadados não encontrados: {metadata_path}")
        with open(metadata_path, 'r', encoding='utf-8') as f:
            self.metadata = json.load(f)
        
        self.bssids = self.metadata['bssids']
        self.threshold = self.metadata['threshold']
    
    def preprocess_wifi_scan(self, wifi_scan_results):
        """
        Pré-processa os resultados de um scan Wi-Fi.
        
        Args:
            wifi_scan_results (list): Lista de dicts com 'bssid' e 'rssi'
            
        Returns:
            numpy.ndarray: Vetor de features normalizado
        """
        # Criar vetor de RSSI
        rssi_vector = np.zeros(len(self.bssids))
        
        for network in wifi_scan_results:
            bssid = network.get('bssid', '').upper()
            rssi = network.get('rssi', -100)
            
            if bssid in self.bssids:
                idx = self.bssids.index(bssid)
                rssi_vector[idx] = rssi
        
        # Normalizar
        rssi_vector_normalized = self.scaler.transform(rssi_vector.reshape(1, -1))
        
        return rssi_vector_normalized
    
    def predict(self, wifi_scan_results):
        """
        Realiza predição de presença indoor.
        
        Args:
            wifi_scan_results (list): Lista de redes Wi-Fi detectadas
            
        Returns:
            dict: Resultado da predição com 'inside', 'confidence', 'error'
        """
        try:
            # Pré-processar entrada
            X = self.preprocess_wifi_scan(wifi_scan_results)
            
            # Fazer predição (reconstrução)
            X_reconstructed = self.model.predict(X, verbose=0)
            
            # Calcular erro de reconstrução (MSE)
            mse = np.mean(np.square(X - X_reconstructed))
            
            # Decisão: inside se erro < threshold
            inside = bool(mse < self.threshold)
            
            # Calcular confidence (baseado na distância do threshold)
            if inside:
                # Se está dentro, confidence é maior quanto menor o erro
                confidence = float(1.0 - (mse / self.threshold))
            else:
                # Se está fora, confidence é baseado em quão longe está
                confidence = float(min(1.0, (mse - self.threshold) / self.threshold))
            
            confidence = max(0.0, min(1.0, confidence))  # Garantir [0, 1]
            
            return {
                'success': True,
                'inside': inside,
                'confidence': round(confidence, 4),
                'reconstruction_error': round(float(mse), 6),
                'threshold': round(float(self.threshold), 6),
                'room_label': self.room_label
            }
            
        except Exception as e:
            return {
                'success': False,
                'error': str(e),
                'inside': False,
                'confidence': 0.0
            }


def main():
    """
    Função principal para processar entrada via stdin.
    """
    try:
        # Ler JSON da entrada padrão
        input_data = sys.stdin.read()
        request = json.loads(input_data)
        
        # Validar entrada
        if 'room_label' not in request:
            raise ValueError("Campo 'room_label' é obrigatório")
        if 'wifi_scan_results' not in request:
            raise ValueError("Campo 'wifi_scan_results' é obrigatório")
        
        room_label = request['room_label']
        wifi_scan_results = request['wifi_scan_results']
        
        # Criar preditor
        predictor = RealtimePredictor(room_label)
        
        # Fazer predição
        result = predictor.predict(wifi_scan_results)
        
        # Retornar JSON via stdout
        print(json.dumps(result, ensure_ascii=False))
        sys.exit(0)
        
    except json.JSONDecodeError as e:
        error_result = {
            'success': False,
            'error': f'JSON inválido: {str(e)}',
            'inside': False,
            'confidence': 0.0
        }
        print(json.dumps(error_result, ensure_ascii=False))
        sys.exit(1)
        
    except FileNotFoundError as e:
        error_result = {
            'success': False,
            'error': f'Arquivo não encontrado: {str(e)}',
            'inside': False,
            'confidence': 0.0
        }
        print(json.dumps(error_result, ensure_ascii=False))
        sys.exit(1)
        
    except Exception as e:
        error_result = {
            'success': False,
            'error': str(e),
            'inside': False,
            'confidence': 0.0
        }
        print(json.dumps(error_result, ensure_ascii=False))
        sys.exit(1)


if __name__ == '__main__':
    main()

