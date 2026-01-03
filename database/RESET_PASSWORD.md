# Como Resetar a Senha do PostgreSQL no Windows

## Problema

Erro de encoding ao conectar sugere que a senha ou caminho do PostgreSQL tem caracteres especiais.

## Solução: Resetar senha para algo simples

### Método 1: Via pgAdmin 4 (Mais fácil)

1. Abra o **pgAdmin 4**
2. Conecte ao servidor PostgreSQL (use a senha atual)
3. No painel esquerdo, expanda "Servers" → "PostgreSQL"
4. Clique com botão direito em "Login/Group Roles" → "postgres"
5. Selecione "Properties"
6. Vá na aba "Definition"
7. Digite nova senha: `postgres123` (sem acentos/caracteres especiais)
8. Clique em "Save"

### Método 2: Via psql (Terminal)

```powershell
# Abrir PowerShell como Administrador

# Navegar até pasta do PostgreSQL
cd "C:\Program Files\PostgreSQL\15\bin"

# Conectar como superusuário
.\psql.exe -U postgres

# Dentro do psql, executar:
ALTER USER postgres WITH PASSWORD 'postgres123';

# Sair
\q
```

### Método 3: Editar pg_hba.conf (Se não conseguir conectar)

1. Abra o arquivo (como Administrador):
   ```
   C:\Program Files\PostgreSQL\15\data\pg_hba.conf
   ```

2. Encontre as linhas com `md5` e troque por `trust`:
   ```
   # IPv4 local connections:
   host    all             all             127.0.0.1/32            trust
   # IPv6 local connections:
   host    all             all             ::1/128                 trust
   ```

3. Salve o arquivo

4. Reinicie o PostgreSQL:
   ```powershell
   # No PowerShell como Admin
   Restart-Service postgresql-x64-15
   ```

5. Conecte sem senha e redefina:
   ```powershell
   psql -U postgres
   ALTER USER postgres WITH PASSWORD 'postgres123';
   \q
   ```

6. Volte o `pg_hba.conf` para `md5` e reinicie novamente

## Depois de Resetar

### Atualizar arquivo .env do backend:

Edite `backend\.env`:
```
DB_PASSWORD=postgres123
```

### Atualizar arquivo .env do ml (se existir):

Edite `ml\.env`:
```
DB_PASSWORD=postgres123
```

### Ou usar diretamente no código:

No `ml/train_autoencoder.py`, linha ~20, ajuste:
```python
DB_CONFIG = {
    'host': os.getenv('DB_HOST', 'localhost'),
    'database': os.getenv('DB_NAME', 'geoping'),
    'user': os.getenv('DB_USER', 'postgres'),
    'password': os.getenv('DB_PASSWORD', 'postgres123'),  # <-- Nova senha aqui
    'port': os.getenv('DB_PORT', '5432')
}
```

## Testar

Depois de mudar a senha, teste:

```powershell
cd ml
python test_connection.py
```

Deve mostrar: "✓ Conexão bem-sucedida!"

## Se ainda não funcionar

O problema pode ser o caminho de instalação do PostgreSQL. Nesse caso, a solução é mais complexa e pode requerer reinstalação do PostgreSQL em um caminho sem acentos (ex: `C:\PostgreSQL\`).





