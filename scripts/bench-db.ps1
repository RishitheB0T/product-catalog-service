# BUILD FLOW 08.02 — scripts/bench-db.ps1
# First introduced: Phase 08
# Purpose: Manages the benchmark database catalog_bench inside the development MySQL container, and MySQL's general
#          query log, which records every statement exactly as the server receives it.
#          Usage: .\scripts\bench-db.ps1 create | status | reset -Force | drop -Force | log-on | log-show | log-off
# Depends on: Docker, infra/compose.dev.yaml (03.01), and infra/.env (created by ".\scripts\dev-services.ps1 up").
# Used by: The developer, before and during query measurements (docs/QUERY_OPTIMIZATION.md).
# Runtime role: Developer tooling only. Passwords never leave the container: every command runs inside it.

# BUILD FLOW 08.02.01 — Parameters
# reset (empty the table) and drop (delete the database) destroy data, so both require -Force.
param(
    [ValidateSet('create', 'status', 'reset', 'drop', 'log-on', 'log-show', 'log-off')]
    [string]$Action = 'status',
    [switch]$Force
)
$ErrorActionPreference = 'Stop'
# Text piped to docker is sent as UTF-8 (Windows PowerShell 5.1 would otherwise send ASCII).
$OutputEncoding = New-Object System.Text.UTF8Encoding($false)

$repoRoot    = Split-Path -Parent $PSScriptRoot
$composeFile = Join-Path $repoRoot 'infra\compose.dev.yaml'
$envFile     = Join-Path $repoRoot 'infra\.env'
$database    = 'catalog_bench'

if (-not (Test-Path -LiteralPath $envFile)) {
    Write-Host 'infra/.env does not exist yet. Run ".\scripts\dev-services.ps1 up" first.'
    exit 1
}

# BUILD FLOW 08.02.02 — Running SQL inside the MySQL container
# The SQL travels on standard input, which avoids Windows PowerShell's quoting problems (README "Quick start").
# Inside the container, MYSQL_PWD takes the password from the container's own environment, so it never appears in
# this script, in its output, or on the Windows command line. Root is needed only to create or drop the database,
# grant rights, and use the query log; everything else runs as the application's own user, on catalog_bench only.
# -Raw prints plain values without the table borders, for output that is read by a program or copied.
function Invoke-MySql([string]$Sql, [switch]$AsRoot, [switch]$Raw) {
    $login = if ($AsRoot) { 'MYSQL_PWD=$MYSQL_ROOT_PASSWORD mysql -u root' }
             else { "MYSQL_PWD=`$MYSQL_PASSWORD mysql -u `$MYSQL_USER $database" }
    $format = if ($Raw) { '--batch --raw --skip-column-names' } else { '--table' }
    $previous = $ErrorActionPreference
    $ErrorActionPreference = 'Continue'
    try { $Sql | & docker compose --env-file $envFile -f $composeFile exec -T mysql sh -c "$login $format" }
    finally { $ErrorActionPreference = $previous }
    if ($LASTEXITCODE -ne 0) { throw "MySQL command failed with exit code $LASTEXITCODE" }
}

# The application user's name is not secret; it is read from infra/.env and checked before it goes into SQL.
$user = ((Get-Content -LiteralPath $envFile | Where-Object { $_ -match '^MYSQL_USER=' }) -replace '^MYSQL_USER=', '').Trim()
if ($user -notmatch '^[A-Za-z0-9_]+$') { throw "Unexpected MYSQL_USER value in infra/.env" }

# BUILD FLOW 08.02.03 — The actions
switch ($Action) {
    'create' {
        # IF NOT EXISTS makes this safe to repeat. The application user gets every right on this one database, the
        # same rights it has on "catalog", so Flyway can create the tables when the bench profile first starts.
        Invoke-MySql -AsRoot -Sql @"
CREATE DATABASE IF NOT EXISTS $database CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
GRANT ALL PRIVILEGES ON $database.* TO '$user'@'%';
"@
        Write-Host "Database $database is ready for user $user. Next: start the backend with --spring.profiles.active=bench."
    }
    'status' {
        $exists = Invoke-MySql -AsRoot -Raw -Sql "SELECT COUNT(*) FROM information_schema.schemata WHERE schema_name = '$database';"
        if ("$exists".Trim() -eq '0') { Write-Host "Database $database does not exist. Run: .\scripts\bench-db.ps1 create"; break }
        $tables = Invoke-MySql -Raw -Sql "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'product';"
        if ("$tables".Trim() -eq '0') { Write-Host "No tables yet. Start the backend with the bench profile; Flyway creates them."; break }
        # Rows, applied migrations, and the indexes on product with their columns. ANALYZE TABLE first refreshes
        # the statistics that the sizes come from (MySQL caches them otherwise); it changes no data.
        $null = Invoke-MySql -Raw -Sql 'ANALYZE TABLE product;'
        Invoke-MySql -Sql @"
SELECT COUNT(*) AS products FROM product;
SELECT version, description, installed_on, execution_time AS ms FROM flyway_schema_history ORDER BY installed_rank;
SELECT index_name, GROUP_CONCAT(column_name ORDER BY seq_in_index) AS index_columns
  FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'product'
  GROUP BY index_name ORDER BY index_name;
"@
        # The size of each index, from InnoDB's own statistics (readable by root only): pages times the page size.
        # PRIMARY is the table itself, because InnoDB stores the rows inside the primary-key index.
        Invoke-MySql -AsRoot -Sql @"
SELECT index_name, ROUND(stat_value * @@innodb_page_size / 1048576, 2) AS size_mb
  FROM mysql.innodb_index_stats
 WHERE database_name = '$database' AND table_name = 'product' AND stat_name = 'size'
 ORDER BY index_name;
"@
    }
    'reset' {
        if (-not $Force) { Write-Host 'reset deletes every benchmark product. Run again with -Force to confirm.'; exit 1 }
        # TRUNCATE empties the table and restarts AUTO_INCREMENT at 1, so the next load gets the same ids again.
        Invoke-MySql -Sql 'TRUNCATE TABLE product;'
        Write-Host "Emptied $database.product. The next start with the bench profile loads the products again."
    }
    'drop' {
        if (-not $Force) { Write-Host "drop deletes the whole $database database. Run again with -Force to confirm."; exit 1 }
        Invoke-MySql -AsRoot -Sql "DROP DATABASE IF EXISTS $database;"
        Write-Host "Dropped $database."
    }
    'log-on' {
        # TABLE output writes the log into the table mysql.general_log, where SQL can read it. Logging every
        # statement costs time, so the log is switched on only while capturing.
        Invoke-MySql -AsRoot -Sql "TRUNCATE TABLE mysql.general_log; SET GLOBAL log_output = 'TABLE'; SET GLOBAL general_log = 'ON';"
        Write-Host 'General query log is ON. Send the requests, then run log-show, then log-off.'
    }
    'log-show' {
        # Only the application's product queries (Hibernate calls the table p1_0), not the container's health checks.
        Invoke-MySql -AsRoot -Raw -Sql "SELECT CONVERT(argument USING utf8mb4) FROM mysql.general_log WHERE command_type = 'Query' AND argument LIKE 'select%p1_0%' ORDER BY event_time;"
    }
    'log-off' {
        Invoke-MySql -AsRoot -Sql "SET GLOBAL general_log = 'OFF'; SET GLOBAL log_output = 'FILE'; TRUNCATE TABLE mysql.general_log;"
        Write-Host 'General query log is OFF and emptied.'
    }
}
