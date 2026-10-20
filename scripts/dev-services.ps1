# BUILD FLOW 03.03 — scripts/dev-services.ps1
# First introduced: Phase 03
# Purpose: One command to manage the local MySQL and Redis containers on Windows.
#          Usage (from any folder):  .\scripts\dev-services.ps1 up | status | logs | down | reset -Force
# Depends on: Docker Desktop (engine running), infra/compose.dev.yaml (03.01), infra/.env.example (03.02).
# Used by: The developer on this Windows machine. Never used on servers.
# Runtime role: Developer tooling only. It starts or stops containers; it does not run the backend.

# BUILD FLOW 03.03.01 — Parameters
# ValidateSet makes PowerShell reject any action that is not in the list, and enables Tab completion.
# A [switch] is an on/off flag: present means $true. reset deletes data, so it requires -Force.
param(
    [ValidateSet('up', 'status', 'logs', 'down', 'reset')]
    [string]$Action = 'status',
    [switch]$Force
)
$ErrorActionPreference = 'Stop'

# BUILD FLOW 03.03.02 — Paths
# $PSScriptRoot is the folder this script lives in (scripts/), so the paths are correct whatever the
# current folder is.
$repoRoot    = Split-Path -Parent $PSScriptRoot
$composeFile = Join-Path $repoRoot 'infra\compose.dev.yaml'
$envFile     = Join-Path $repoRoot 'infra\.env'
$envExample  = Join-Path $repoRoot 'infra\.env.example'

# BUILD FLOW 03.03.03 — Random passwords without bias
# RandomNumberGenerator is the cryptographic generator built into .NET (unlike Get-Random, it is designed
# for secrets). Each random byte (0-255) picks one alphabet character. Bytes at or above $limit are thrown
# away: 256 is not a multiple of the alphabet size, so keeping them would make some characters more likely
# than others ("modulo bias"). The alphabet leaves out look-alike characters such as l, 1, O, and 0.
function New-RandomSecret([int]$Length = 24) {
    $alphabet = 'abcdefghijkmnopqrstuvwxyzABCDEFGHJKLMNPQRSTUVWXYZ23456789'
    $limit = 256 - (256 % $alphabet.Length)
    $rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
    $buffer = New-Object byte[] 1
    $chars = New-Object System.Collections.Generic.List[char]
    try {
        while ($chars.Count -lt $Length) {
            $rng.GetBytes($buffer)
            if ($buffer[0] -lt $limit) { $chars.Add($alphabet[$buffer[0] % $alphabet.Length]) }
        }
    } finally {
        $rng.Dispose()
    }
    return -join $chars
}

# BUILD FLOW 03.03.04 — First run: create infra/.env from the template
# Only when infra/.env does not exist yet, so existing passwords (already baked into the MySQL volume) are
# never replaced. Comment lines are dropped; "generate-me" values become random passwords.
function Initialize-EnvFile {
    if (Test-Path -LiteralPath $envFile) { return }
    $lines = @('# Generated from infra/.env.example by scripts/dev-services.ps1. Private: git-ignored, do not share.')
    foreach ($line in Get-Content -LiteralPath $envExample) {
        if ($line -match '^\s*#' -or $line.Trim() -eq '') { continue }
        if ($line -match '^([A-Z_]+)=generate-me$') { $lines += "$($Matches[1])=$(New-RandomSecret)" } else { $lines += $line }
    }
    # UTF-8 without a byte-order mark and LF line endings: the plainest format for Docker Compose to read.
    [System.IO.File]::WriteAllText($envFile, (($lines -join "`n") + "`n"), (New-Object System.Text.UTF8Encoding($false)))
    Write-Host 'Created infra/.env with new random passwords (git-ignored; keep it private).'
}

# BUILD FLOW 03.03.05 — Running docker compose
# docker reports progress on stderr. Windows PowerShell 5.1 can turn stderr text into error records, which
# would stop this script under $ErrorActionPreference = 'Stop', so success is judged by the exit code instead.
# "@args" passes on whatever arguments this function received.
function Invoke-Compose {
    $composeArgs = @('compose', '--env-file', $envFile, '-f', $composeFile)
    $previous = $ErrorActionPreference
    $ErrorActionPreference = 'Continue'
    try { & docker @composeArgs @args } finally { $ErrorActionPreference = $previous }
    if ($LASTEXITCODE -ne 0) { throw "docker compose $($args -join ' ') failed with exit code $LASTEXITCODE" }
}

# BUILD FLOW 03.03.06 — The actions
if ($Action -ne 'up' -and -not (Test-Path -LiteralPath $envFile)) {
    Write-Host 'infra/.env does not exist yet. Run ".\scripts\dev-services.ps1 up" first.'
    exit 1
}
switch ($Action) {
    'up' {
        Initialize-EnvFile
        # --wait blocks until every service reports "healthy" (or fails after the timeout, in seconds).
        Invoke-Compose up -d --wait --wait-timeout 180
        Invoke-Compose ps
        Write-Host 'MySQL: 127.0.0.1:24306 (database "catalog") | Redis: 127.0.0.1:24379 | passwords: infra/.env'
    }
    'status' { Invoke-Compose ps }
    'logs'   { Invoke-Compose logs --tail 50 }
    'down'   { Invoke-Compose down }
    'reset' {
        if (-not $Force) {
            Write-Host 'reset deletes the MySQL data volume. Run again with -Force to confirm.'
            exit 1
        }
        # --volumes also removes the named volume, so the next "up" starts with an empty database.
        Invoke-Compose down --volumes
    }
}
