$APP_NAME = "demo-tts"

Write-Host "--- [1/2] Ensuring Docker Network & Compose Build.. ---" -ForegroundColor Cyan
docker network inspect demo-network > $null 2>&1
if ($LASTEXITCODE -ne 0) {
    Write-Host "Creating demo-network..." -ForegroundColor Yellow
    docker network create demo-network
}
docker-compose up -d --build

Write-Host "--- [2/2] Application is Starting.. ---" -ForegroundColor Green
docker logs -f "$APP_NAME"
