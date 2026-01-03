#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
================================================================================
Script de Teste do Sistema GeoPing
================================================================================

Este script verifica se todos os componentes do sistema estão funcionando.

Uso: python test_system.py

Autor: GeoPing Team
Data: 2025
"""

import os
import sys
import requests
import psycopg2

def test_database():
    """Testa conexão com o banco de dados"""
    print("\n[1/3] Testando conexão com PostgreSQL...")
    try:
        conn = psycopg2.connect(
            host='localhost',
            database='geo_ping',
            user='postgres',
            password='123456',
            port='5432'
        )
        cursor = conn.cursor()
        cursor.execute("SELECT COUNT(*) FROM wifi_training_data")
        count = cursor.fetchone()[0]
        cursor.close()
        conn.close()
        print(f"  ✓ PostgreSQL: OK")
        print(f"  ✓ Registros na tabela: {count}")
        return True
    except Exception as e:
        print(f"  ✗ PostgreSQL: FALHOU")
        print(f"  Erro: {e}")
        return False


def test_backend():
    """Testa se o backend está rodando"""
    print("\n[2/3] Testando Backend Node.js...")
    try:
        response = requests.get('http://localhost:3000', timeout=5)
        if response.status_code == 200:
            data = response.json()
            print(f"  ✓ Backend: OK")
            print(f"  ✓ Status: {data.get('status')}")
            print(f"  ✓ Versão: {data.get('version')}")
            return True
        else:
            print(f"  ✗ Backend: FALHOU (Status {response.status_code})")
            return False
    except requests.exceptions.ConnectionError:
        print(f"  ✗ Backend: NÃO ESTÁ RODANDO")
        print(f"  Inicie com: cd backend && npm start")
        return False
    except Exception as e:
        print(f"  ✗ Backend: FALHOU")
        print(f"  Erro: {e}")
        return False


def test_ml_dependencies():
    """Testa se as dependências de ML estão instaladas"""
    print("\n[3/3] Testando Dependências de Machine Learning...")
    
    required_packages = [
        ('numpy', 'NumPy'),
        ('pandas', 'Pandas'),
        ('sklearn', 'Scikit-Learn'),
        ('tensorflow', 'TensorFlow'),
        ('matplotlib', 'Matplotlib')
    ]
    
    all_ok = True
    for package, name in required_packages:
        try:
            __import__(package)
            print(f"  ✓ {name}: OK")
        except ImportError:
            print(f"  ✗ {name}: NÃO INSTALADO")
            all_ok = False
    
    if not all_ok:
        print(f"\n  Instale com: cd ml && pip install -r requirements.txt")
    
    return all_ok


def test_models_directory():
    """Verifica se o diretório de modelos existe"""
    print("\n[EXTRA] Verificando estrutura de diretórios...")
    
    models_dir = 'ml/models'
    if os.path.exists(models_dir):
        print(f"  ✓ Diretório de modelos existe")
        
        # Listar modelos existentes
        model_files = [f for f in os.listdir(models_dir) if f.endswith('.h5')]
        if model_files:
            print(f"  ✓ Modelos encontrados: {len(model_files)}")
            for model in model_files:
                print(f"    - {model}")
        else:
            print(f"  ⚠ Nenhum modelo treinado ainda")
    else:
        print(f"  ⚠ Diretório de modelos não existe (será criado no primeiro treinamento)")


def main():
    """Função principal"""
    print("="*70)
    print("  TESTE DO SISTEMA GEOPING")
    print("="*70)
    
    results = []
    
    # Executar testes
    results.append(("Banco de Dados", test_database()))
    results.append(("Backend", test_backend()))
    results.append(("ML Dependencies", test_ml_dependencies()))
    
    test_models_directory()
    
    # Resumo
    print("\n" + "="*70)
    print("  RESUMO DOS TESTES")
    print("="*70)
    
    all_passed = True
    for name, status in results:
        status_str = "✓ PASSOU" if status else "✗ FALHOU"
        print(f"  {name}: {status_str}")
        if not status:
            all_passed = False
    
    print("="*70)
    
    if all_passed:
        print("\n✓ Todos os testes passaram! O sistema está pronto para uso.")
        print("\nPróximos passos:")
        print("  1. Instale o app Android no smartphone")
        print("  2. Colete dados de treinamento")
        print("  3. Treine o modelo: python ml/train_autoencoder.py NOME_DA_SALA")
        return 0
    else:
        print("\n✗ Alguns testes falharam. Corrija os problemas acima.")
        return 1


if __name__ == "__main__":
    sys.exit(main())

