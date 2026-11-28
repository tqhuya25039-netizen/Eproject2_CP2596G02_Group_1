param(
    [switch]$Setup
)

$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$envFile = Join-Path $PSScriptRoot '.env'

function Read-Value {
    param(
        [string]$Prompt,
        [string]$DefaultValue
    )

    $value = Read-Host "$Prompt [$DefaultValue]"
    if ([string]::IsNullOrWhiteSpace($value)) {
        return $DefaultValue
    }
    return $value.Trim()
}

function Read-SecretValue {
    param([string]$Prompt)

    $secureValue = Read-Host $Prompt -AsSecureString
    $pointer = [IntPtr]::Zero
    try {
        $pointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secureValue)
        return [Runtime.InteropServices.Marshal]::PtrToStringBSTR($pointer)
    } finally {
        if ($pointer -ne [IntPtr]::Zero) {
            [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($pointer)
        }
    }
}

function New-EnvironmentFile {
    Write-Host 'Enter the MySQL connection settings for this computer.'
    Write-Host 'Example .env values for local XAMPP/MySQL:'
    Write-Host '  PHARMACY_DB_HOST=127.0.0.1'
    Write-Host '  PHARMACY_DB_PORT=3306'
    Write-Host '  PHARMACY_DB_NAME=nkp_db'
    Write-Host '  PHARMACY_DB_USER=root'
    Write-Host '  PHARMACY_DB_PASSWORD=(your MySQL password)'
    Write-Host 'Press Enter to use the suggested value shown in brackets.'

    $hostName = Read-Value 'MySQL host (example: 127.0.0.1)' '127.0.0.1'
    $port = Read-Value 'MySQL port (example: 3306)' '3306'
    $database = Read-Value 'Database name (example: nkp_db)' 'nkp_db'
    $user = Read-Value 'MySQL username (example: root)' 'root'

    $password = Read-SecretValue 'MySQL password (hidden; press Enter if no password is set)'
    $adminPassword = Read-SecretValue `
        'Initial admin password (hidden; press Enter if admin_demo is already initialized)'

    $portNumber = 0
    if (-not [int]::TryParse($port, [ref]$portNumber) -or $portNumber -lt 1 -or $portNumber -gt 65535) {
        throw 'MySQL port must be a number from 1 to 65535.'
    }
    if ($database -notmatch '^[A-Za-z0-9_]+$') {
        throw 'Database name may contain only letters, numbers, and underscores.'
    }
    if ([string]::IsNullOrWhiteSpace($hostName) -or [string]::IsNullOrWhiteSpace($user)) {
        throw 'MySQL host and username cannot be empty.'
    }

    $lines = @(
        "PHARMACY_DB_HOST=$hostName"
        "PHARMACY_DB_PORT=$port"
        "PHARMACY_DB_NAME=$database"
        "PHARMACY_DB_USER=$user"
        "PHARMACY_DB_PASSWORD=$password"
        "PHARMACY_SAMPLE_ADMIN_PASSWORD=$adminPassword"
    )
    $encoding = New-Object System.Text.UTF8Encoding($false)
    [System.IO.File]::WriteAllLines($envFile, $lines, $encoding)
    Write-Host "Saved settings to $envFile"
    Write-Warning 'The .env file contains passwords in plain text. Keep it private; it is excluded from Git.'
}

if ($Setup -or -not (Test-Path -LiteralPath $envFile)) {
    if ($Setup -and (Test-Path -LiteralPath $envFile)) {
        $confirmation = Read-Host 'Replace the existing .env file? [y/N]'
        if ($confirmation -notmatch '^(y|yes)$') {
            Write-Host 'Setup cancelled; the existing .env file was not changed.'
            exit 0
        }
    }
    New-EnvironmentFile
}

$allowedKeys = @(
    'PHARMACY_DB_HOST',
    'PHARMACY_DB_PORT',
    'PHARMACY_DB_NAME',
    'PHARMACY_DB_USER',
    'PHARMACY_DB_PASSWORD',
    'PHARMACY_SAMPLE_ADMIN_PASSWORD'
)
$settings = @{}
foreach ($line in [System.IO.File]::ReadAllLines($envFile)) {
    if ([string]::IsNullOrWhiteSpace($line) -or $line.TrimStart().StartsWith('#')) {
        continue
    }
    $separator = $line.IndexOf('=')
    if ($separator -lt 1) {
        throw 'Invalid .env line. Use KEY=VALUE.'
    }

    $key = $line.Substring(0, $separator).Trim()
    if ($allowedKeys -notcontains $key) {
        throw "Unsupported setting in .env: $key"
    }
    $settings[$key] = $line.Substring($separator + 1)
}

$requiredKeys = @(
    'PHARMACY_DB_HOST',
    'PHARMACY_DB_PORT',
    'PHARMACY_DB_NAME',
    'PHARMACY_DB_USER',
    'PHARMACY_DB_PASSWORD'
)
$missingKeys = @($requiredKeys | Where-Object { -not $settings.ContainsKey($_) })
if ($missingKeys.Count -gt 0) {
    throw "Missing settings in .env: $($missingKeys -join ', '). Run environment\run.bat /setup to recreate it."
}

foreach ($key in $settings.Keys) {
    [Environment]::SetEnvironmentVariable($key, $settings[$key], 'Process')
}

if (-not (Get-Command mvn -ErrorAction SilentlyContinue)) {
    throw 'Maven was not found. Install Maven 3.9+ and make sure mvn is on PATH.'
}

Write-Host 'Starting Ninh Kieu Pharma...'
Set-Location -LiteralPath $projectRoot
# Rebuild all classes after an incomplete or stale build.
& mvn clean javafx:run
exit $LASTEXITCODE
