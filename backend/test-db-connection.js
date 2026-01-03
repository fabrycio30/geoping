// Script de teste de conexão com PostgreSQL
require('dotenv').config();
const { Pool } = require('pg');

async function testConnection() {
    console.log('\n==============================================');
    console.log('  Testando Conexão com PostgreSQL');
    console.log('==============================================\n');
    
    // Mostrar configurações (sem mostrar senha completa)
    console.log('Configurações carregadas do .env:');
    console.log(`  DB_USER: ${process.env.DB_USER || 'postgres'}`);
    console.log(`  DB_HOST: ${process.env.DB_HOST || 'localhost'}`);
    console.log(`  DB_NAME: ${process.env.DB_NAME || 'geoping'}`);
    console.log(`  DB_PORT: ${process.env.DB_PORT || '5432'}`);
    console.log(`  DB_PASSWORD: ${'*'.repeat((process.env.DB_PASSWORD || '').length)} (${(process.env.DB_PASSWORD || '').length} caracteres)`);
    console.log('\n');

    const pool = new Pool({
        user: process.env.DB_USER || 'postgres',
        host: process.env.DB_HOST || 'localhost',
        database: process.env.DB_NAME || 'geoping',
        password: process.env.DB_PASSWORD || 'postgres',
        port: process.env.DB_PORT || 5432,
    });

    try {
        console.log('Tentando conectar...');
        const client = await pool.connect();
        console.log('✓ Conexão bem-sucedida!\n');
        
        // Testar uma query simples
        const result = await client.query('SELECT COUNT(*) FROM wifi_training_data');
        console.log(`✓ Query executada com sucesso!`);
        console.log(`  Registros na tabela: ${result.rows[0].count}\n`);
        
        client.release();
        await pool.end();
        
        console.log('==============================================');
        console.log('  Teste concluído com SUCESSO!');
        console.log('==============================================\n');
        
        process.exit(0);
    } catch (error) {
        console.error('✗ Erro ao conectar ao banco de dados:\n');
        console.error(`  Tipo: ${error.name}`);
        console.error(`  Mensagem: ${error.message}`);
        console.error(`\n  Código do erro: ${error.code || 'N/A'}`);
        
        console.log('\n==============================================');
        console.log('  POSSÍVEIS SOLUÇÕES:');
        console.log('==============================================\n');
        
        if (error.message.includes('authentication') || error.message.includes('senha')) {
            console.log('❌ ERRO DE AUTENTICAÇÃO');
            console.log('\nA senha está incorreta. Para corrigir:\n');
            console.log('1. Abra o arquivo: backend\\.env');
            console.log('2. Altere a linha DB_PASSWORD para a senha correta');
            console.log('3. Salve o arquivo');
            console.log('4. Execute novamente: npm start\n');
            console.log('Se não lembra a senha do PostgreSQL:');
            console.log('- Windows: Abra pgAdmin 4 e redefina a senha');
            console.log('- Ou reinstale o PostgreSQL\n');
        } else if (error.code === 'ECONNREFUSED') {
            console.log('❌ SERVIDOR NÃO ESTÁ RODANDO');
            console.log('\nO PostgreSQL não está rodando. Para iniciar:');
            console.log('- Abra "Serviços" do Windows (services.msc)');
            console.log('- Procure por "postgresql"');
            console.log('- Clique com botão direito e selecione "Iniciar"\n');
        } else if (error.message.includes('database') && error.message.includes('does not exist')) {
            console.log('❌ BANCO DE DADOS NÃO EXISTE');
            console.log('\nO banco "geoping" não foi criado. Execute:');
            console.log('psql -U postgres');
            console.log('CREATE DATABASE geoping;');
            console.log('\\c geoping');
            console.log('\\i database/init.sql\n');
        } else {
            console.log('❌ ERRO DESCONHECIDO');
            console.log('\nVerifique se:');
            console.log('- PostgreSQL está instalado');
            console.log('- PostgreSQL está rodando');
            console.log('- As credenciais no .env estão corretas\n');
        }
        
        console.log('==============================================\n');
        await pool.end();
        process.exit(1);
    }
}

testConnection();





