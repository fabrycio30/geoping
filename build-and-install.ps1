# Script PowerShell para compilar e instalar o GeoPing
# Execute este script no PowerShell para facilitar o processo de build

Write-Host "╔════════════════════════════════════════════╗" -ForegroundColor Cyan
Write-Host "║        GEOPING - BUILD & INSTALL          ║" -ForegroundColor Cyan
Write-Host "╚════════════════════════════════════════════╝" -ForegroundColor Cyan
Write-Host ""

# Verifica se está na pasta correta
if (-Not (Test-Path "settings.gradle")) {
    Write-Host "[ERRO] Execute este script na pasta raiz do projeto!" -ForegroundColor Red
    exit 1
}

# Menu de opções
Write-Host "Escolha uma opção:" -ForegroundColor Yellow
Write-Host "1. Compilar (Debug)"
Write-Host "2. Compilar e Instalar (Debug)"
Write-Host "3. Limpar e Compilar"
Write-Host "4. Ver logs do app"
Write-Host "5. Ver dispositivos conectados"
Write-Host "6. Desinstalar app"
Write-Host "7. Sair"
Write-Host ""

$opcao = Read-Host "Digite o número da opção"

switch ($opcao) {
    "1" {
        Write-Host "`n[1/1] Compilando o projeto..." -ForegroundColor Green
        .\gradlew assembleDebug
        
        if ($LASTEXITCODE -eq 0) {
            Write-Host "`n✓ Compilação concluída com sucesso!" -ForegroundColor Green
            Write-Host "APK gerado em: app\build\outputs\apk\debug\app-debug.apk" -ForegroundColor Cyan
        } else {
            Write-Host "`n✗ Erro na compilação!" -ForegroundColor Red
        }
    }
    
    "2" {
        Write-Host "`n[1/2] Compilando o projeto..." -ForegroundColor Green
        .\gradlew assembleDebug
        
        if ($LASTEXITCODE -eq 0) {
            Write-Host "✓ Compilação concluída!" -ForegroundColor Green
            
            Write-Host "`n[2/2] Instalando no dispositivo..." -ForegroundColor Green
            .\gradlew installDebug
            
            if ($LASTEXITCODE -eq 0) {
                Write-Host "`n✓ App instalado com sucesso!" -ForegroundColor Green
            } else {
                Write-Host "`n✗ Erro na instalação!" -ForegroundColor Red
                Write-Host "Certifique-se de que um dispositivo/emulador está conectado." -ForegroundColor Yellow
            }
        } else {
            Write-Host "`n✗ Erro na compilação!" -ForegroundColor Red
        }
    }
    
    "3" {
        Write-Host "`n[1/2] Limpando build anterior..." -ForegroundColor Green
        .\gradlew clean
        
        Write-Host "`n[2/2] Compilando o projeto..." -ForegroundColor Green
        .\gradlew assembleDebug
        
        if ($LASTEXITCODE -eq 0) {
            Write-Host "`n✓ Build limpo e compilação concluída!" -ForegroundColor Green
        } else {
            Write-Host "`n✗ Erro na compilação!" -ForegroundColor Red
        }
    }
    
    "4" {
        Write-Host "`nMonitorando logs do GeoPing..." -ForegroundColor Green
        Write-Host "Pressione Ctrl+C para parar`n" -ForegroundColor Yellow
        adb logcat | Select-String "GeoPing|SocketManager|WifiProximity|ChatViewModel"
    }
    
    "5" {
        Write-Host "`nDispositivos conectados:" -ForegroundColor Green
        adb devices
        Write-Host ""
    }
    
    "6" {
        Write-Host "`nDesinstalando GeoPing..." -ForegroundColor Yellow
        adb uninstall com.geoping
        
        if ($LASTEXITCODE -eq 0) {
            Write-Host "✓ App desinstalado com sucesso!" -ForegroundColor Green
        } else {
            Write-Host "✗ Erro ao desinstalar ou app não estava instalado" -ForegroundColor Red
        }
    }
    
    "7" {
        Write-Host "`nSaindo..." -ForegroundColor Cyan
        exit 0
    }
    
    default {
        Write-Host "`nOpção inválida!" -ForegroundColor Red
    }
}

Write-Host "`nPressione qualquer tecla para sair..."
$null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")

