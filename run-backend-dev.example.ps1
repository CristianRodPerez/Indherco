$ErrorActionPreference = "Stop"

$rootPath = $PSScriptRoot
$backendPath = Join-Path $rootPath "backend"

$env:SPRING_PROFILES_ACTIVE = "dev"
$env:DB_URL = "jdbc:postgresql://localhost:5432/indherco"
$env:DB_USERNAME = "postgres"
$env:DB_PASSWORD = "change_me"
$env:JWT_SECRET = "change_me_to_a_long_random_secret_with_at_least_32_chars"
$env:ADMIN_USERNAME = "admin"
$env:ADMIN_PASSWORD = "change_me"

Write-Host "Iniciando backend Indherco con perfil dev..."
Set-Location $backendPath

mvn clean spring-boot:run

if ($LASTEXITCODE -ne 0) {
    Write-Error "El backend no pudo iniciarse."
    exit $LASTEXITCODE
}
