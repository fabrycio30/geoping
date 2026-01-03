# Script para copiar APK para área de trabalho
# GeoPing - Compartilhar APK

Write-Host "`n╔════════════════════════════════════════════════════════╗" -ForegroundColor Cyan
Write-Host "║                                                        ║" -ForegroundColor Cyan
Write-Host "║      📦 COPIAR APK PARA COMPARTILHAR                 ║" -ForegroundColor Cyan
Write-Host "║                                                        ║" -ForegroundColor Cyan
Write-Host "╚════════════════════════════════════════════════════════╝`n" -ForegroundColor Cyan

$apkSource = "app\build\outputs\apk\debug\app-debug.apk"
$desktopPath = [Environment]::GetFolderPath("Desktop")
$apkDestination = "$desktopPath\GeoPing-v1.0-debug.apk"

Write-Host "Procurando APK...`n" -ForegroundColor Yellow

if (Test-Path $apkSource) {
    Write-Host "✅ APK encontrado!" -ForegroundColor Green
    
    # Copia para área de trabalho
    Copy-Item -Path $apkSource -Destination $apkDestination -Force
    
    $size = (Get-Item $apkDestination).Length / 1MB
    
    Write-Host "`n╔════════════════════════════════════════════════════════╗" -ForegroundColor Green
    Write-Host "║                                                        ║" -ForegroundColor Green
    Write-Host "║      ✅ APK COPIADO COM SUCESSO!                     ║" -ForegroundColor Green
    Write-Host "║                                                        ║" -ForegroundColor Green
    Write-Host "╚════════════════════════════════════════════════════════╝`n" -ForegroundColor Green
    
    Write-Host "Localização do APK:" -ForegroundColor Yellow
    Write-Host "══════════════════════════════════════════════════════════`n" -ForegroundColor DarkGray
    Write-Host $apkDestination -ForegroundColor Cyan
    Write-Host "`nTamanho: " -NoNewline -ForegroundColor Yellow
    Write-Host ("{0:N2} MB" -f $size) -ForegroundColor White
    
    Write-Host "`n══════════════════════════════════════════════════════════`n" -ForegroundColor DarkGray
    Write-Host "PRÓXIMOS PASSOS:" -ForegroundColor Yellow
    Write-Host "══════════════════════════════════════════════════════════`n" -ForegroundColor DarkGray
    Write-Host "1. Acesse sua área de trabalho" -ForegroundColor White
    Write-Host "2. Encontre o arquivo: GeoPing-v1.0-debug.apk" -ForegroundColor White
    Write-Host "3. Envie para seu colega via:" -ForegroundColor White
    Write-Host "   • WhatsApp" -ForegroundColor Gray
    Write-Host "   • Telegram" -ForegroundColor Gray
    Write-Host "   • Google Drive" -ForegroundColor Gray
    Write-Host "   • Email`n" -ForegroundColor Gray
    
    Write-Host "══════════════════════════════════════════════════════════`n" -ForegroundColor DarkGray
    Write-Host "INSTRUÇÕES PARA SEU COLEGA:" -ForegroundColor Yellow
    Write-Host "══════════════════════════════════════════════════════════`n" -ForegroundColor DarkGray
    Write-Host "1. Baixe o arquivo GeoPing-v1.0-debug.apk" -ForegroundColor White
    Write-Host "2. Abra o arquivo no celular" -ForegroundColor White
    Write-Host "3. Permita 'Instalar apps desconhecidos'" -ForegroundColor White
    Write-Host "4. Instale o app ✅`n" -ForegroundColor White
    
    Write-Host "══════════════════════════════════════════════════════════`n" -ForegroundColor DarkGray
    
    # Abre a pasta no Explorer
    Start-Process "explorer.exe" -ArgumentList "/select,`"$apkDestination`""
    
} else {
    Write-Host "❌ APK não encontrado!" -ForegroundColor Red
    Write-Host "`nPrimeiro você precisa gerar o APK:`n" -ForegroundColor Yellow
    Write-Host "No Android Studio:" -ForegroundColor Cyan
    Write-Host "  1. Build → Build Bundle(s) / APK(s) → Build APK(s)" -ForegroundColor White
    Write-Host "  2. Aguarde a compilação" -ForegroundColor White
    Write-Host "  3. Execute este script novamente`n" -ForegroundColor White
    Write-Host "Ou use o atalho:" -ForegroundColor Cyan
    Write-Host "  Ctrl + Shift + A → digite 'Build APK' → Enter`n" -ForegroundColor White
}

Write-Host "Pressione qualquer tecla para fechar..." -ForegroundColor Gray
$null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")


