param(
    [string]$Database = "indherco",
    [string]$Username = "postgres"
)

$ErrorActionPreference = "Stop"
$securePassword = Read-Host "Password PostgreSQL" -AsSecureString
$passwordPointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($securePassword)
$plainPassword = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($passwordPointer)
[Runtime.InteropServices.Marshal]::ZeroFreeBSTR($passwordPointer)

$env:PGPASSWORD = $plainPassword
try {
    dropdb --if-exists --username=$Username $Database
    createdb --username=$Username $Database
    Write-Host "Base de datos $Database recreada correctamente."
} finally {
    Remove-Item Env:\PGPASSWORD -ErrorAction SilentlyContinue
}
