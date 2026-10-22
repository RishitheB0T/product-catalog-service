# BUILD FLOW 08.09 — scripts/measure-queries.ps1
# First introduced: Phase 08
# Purpose: Measures every query in benchmarks/queries/search-queries.sql against catalog_bench: one warm-up run, then
#          EXPLAIN ANALYZE several times (the median counts), plus a fingerprint of the complete result. Writes
#          benchmarks/query-plans/<label>.txt. With -Baseline it also compares every result with an earlier file.
#          Usage: .\scripts\measure-queries.ps1 -Label before-v2
#                 .\scripts\measure-queries.ps1 -Label after-v2 -Baseline before-v2
#                 .\scripts\measure-queries.ps1 -Label index-alternatives -QueryFile benchmarks\queries\index-alternatives.sql
# Depends on: Docker, infra/compose.dev.yaml, infra/.env, and a filled catalog_bench (scripts/bench-db.ps1 plus one
#             start of the backend with the bench profile).
# Used by: The developer; docs/QUERY_OPTIMIZATION.md explains the method and the results.
# Runtime role: Developer tooling only. It changes no data: it refreshes the table's statistics, then runs SELECT and
#               EXPLAIN ANALYZE as the application's user.

# BUILD FLOW 08.09.01 — Parameters and paths
param(
    [Parameter(Mandatory)] [ValidatePattern('^[a-z0-9-]+$')] [string]$Label,
    [ValidatePattern('^[a-z0-9-]+$')] [string]$Baseline,
    [ValidateRange(1, 9)] [int]$Runs = 3,
    # Relative to the repository root.
    [string]$QueryFile = 'benchmarks\queries\search-queries.sql'
)
$ErrorActionPreference = 'Stop'
$OutputEncoding = New-Object System.Text.UTF8Encoding($false)
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$invariant = [System.Globalization.CultureInfo]::InvariantCulture

$repoRoot    = Split-Path -Parent $PSScriptRoot
$composeFile = Join-Path $repoRoot 'infra\compose.dev.yaml'
$envFile     = Join-Path $repoRoot 'infra\.env'
# PowerShell variable names ignore case, so this must not be called $queryFile: that would overwrite $QueryFile.
$queryPath   = Join-Path $repoRoot $QueryFile
$planDir     = Join-Path $repoRoot 'benchmarks\query-plans'
$outFile     = Join-Path $planDir "$Label.txt"

# BUILD FLOW 08.09.02 — Running SQL as the application's user
# Same approach as bench-db.ps1 (08.02): SQL on standard input, the password taken inside the container. --raw keeps
# the line breaks of EXPLAIN ANALYZE's tree; --skip-column-names leaves only the values.
function Invoke-BenchSql([string]$Sql) {
    $previous = $ErrorActionPreference
    $ErrorActionPreference = 'Continue'
    try {
        $output = $Sql | & docker compose --env-file $envFile -f $composeFile exec -T mysql sh -c 'MYSQL_PWD=$MYSQL_PASSWORD mysql -u $MYSQL_USER catalog_bench --batch --raw --skip-column-names'
    } finally { $ErrorActionPreference = $previous }
    if ($LASTEXITCODE -ne 0) { throw "MySQL command failed with exit code $LASTEXITCODE" }
    return (@($output) -join "`n")
}

function Get-Sha256([string]$Text) {
    $sha = [System.Security.Cryptography.SHA256]::Create()
    try { -join ($sha.ComputeHash([System.Text.Encoding]::UTF8.GetBytes($Text)) | ForEach-Object { $_.ToString('x2') }) }
    finally { $sha.Dispose() }
}

# BUILD FLOW 08.09.03 — Reading the query file
# Every query starts with "-- query: <name>" and "-- request: <the API request that produced it>"; the SQL follows and
# ends with ";". Other comment lines are skipped.
$queries = New-Object System.Collections.Generic.List[object]
$current = $null
foreach ($line in Get-Content -LiteralPath $queryPath -Encoding UTF8) {
    if ($line -match '^-- query:\s*(\S+)') { $current = [ordered]@{ Name = $Matches[1]; Request = ''; Sql = '' }; $queries.Add($current); continue }
    if ($null -eq $current) { continue }
    if ($line -match '^-- request:\s*(.+)$') { $current.Request = $Matches[1].Trim(); continue }
    if ($line -match '^\s*--' -or $line.Trim() -eq '') { continue }
    $current.Sql = ($current.Sql + ' ' + $line.Trim()).Trim()
}
if ($queries.Count -eq 0) { throw "No queries found in $QueryFile" }

# BUILD FLOW 08.09.04 — The context of this measurement
# Results only mean something together with the data and the schema they were measured on. ANALYZE TABLE first: it
# refreshes the statistics the optimizer chooses plans with, and the sizes reported below, which MySQL otherwise caches.
$null = Invoke-BenchSql 'ANALYZE TABLE product;'
$context = @((Invoke-BenchSql @"
SELECT VERSION();
SELECT COUNT(*) FROM product;
SELECT COALESCE(MAX(CAST(version AS UNSIGNED)), 0) FROM flyway_schema_history WHERE success = 1;
SELECT ROUND(@@innodb_buffer_pool_size / 1048576);
SELECT CONCAT(ROUND(data_length / 1048576, 1), ' MB table, ', ROUND(index_length / 1048576, 1), ' MB other indexes') FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'product';
SELECT CONCAT(index_name, ' (', GROUP_CONCAT(column_name ORDER BY seq_in_index SEPARATOR ', '), ')') FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'product' GROUP BY index_name ORDER BY index_name;
"@) -split "`n")
$indexes = ($context[5..($context.Count - 1)]) -join '; '

# BUILD FLOW 08.09.05 — Measuring each query
# The warm-up run loads the pages the query needs into MySQL's memory (the buffer pool), so every timed run measures
# the same thing: work in memory, not first-time disk reads. EXPLAIN ANALYZE really executes the query and reports
# each step's actual time; the root step's second number is the total in milliseconds. Single runs vary, so the
# median of several runs counts. The fingerprint is a SHA-256 hash of the complete result (every column of every
# row, in order): identical fingerprints mean identical results.
$measured = New-Object System.Collections.Generic.List[object]
foreach ($query in $queries) {
    $sql = $query.Sql.TrimEnd(';').Trim()
    Write-Host "Measuring $($query.Name) ..."
    $null = Invoke-BenchSql "$sql;"
    $runsDone = @()
    for ($i = 1; $i -le $Runs; $i++) {
        $plan = Invoke-BenchSql "EXPLAIN ANALYZE $sql;"
        $firstLine = ($plan -split "`n")[0]
        if ($firstLine -notmatch 'actual time=[\d.]+\.\.([\d.]+)') { throw "Unexpected EXPLAIN ANALYZE output for $($query.Name): $firstLine" }
        $runsDone += [pscustomobject]@{ Ms = [double]::Parse($Matches[1], $invariant); Plan = $plan }
    }
    $median = @($runsDone | Sort-Object Ms)[[int][Math]::Floor($Runs / 2)]
    $result = Invoke-BenchSql "$sql;"
    $rows = if ($result) { @($result -split "`n").Count } else { 0 }
    $measured.Add([pscustomobject]@{
        Name = $query.Name; Request = $query.Request; Sql = $sql
        Times = ($runsDone | ForEach-Object { $_.Ms.ToString('0.000', $invariant) }) -join ' '
        Median = $median.Ms; Plan = $median.Plan; Rows = $rows; Sha = Get-Sha256 $result
    })
}

# The first column is as wide as the longest query name, so the tables below stay aligned.
$width = [Math]::Max(28, ($measured | ForEach-Object { $_.Name.Length } | Measure-Object -Maximum).Maximum + 2)
$summaryRow = "{0,-$width} {1,10} {2,8}  {3}"
$comparisonRow = "{0,-$width} {1,10} {2,10} {3,10}  {4}"

# BUILD FLOW 08.09.06 — Comparing with a baseline
# Reads the baseline file's "Median ms" and "Result" lines. Different fingerprints mean the change altered results:
# that is reported, and the script ends with an error so it cannot go unnoticed.
$comparison = @()
$different = 0
if ($Baseline) {
    $baselineFile = Join-Path $planDir "$Baseline.txt"
    $before = @{}
    foreach ($section in ((Get-Content -LiteralPath $baselineFile -Raw -Encoding UTF8) -split '(?m)^## ')) {
        if ($section -match '^(\S+)\s*\n' -and $section -match '(?m)^Median ms: ([\d.]+)' ) {
            $name = ($section -split "`n")[0].Trim()
            $ms = [double]::Parse(($section | Select-String '(?m)^Median ms: ([\d.]+)').Matches[0].Groups[1].Value, $invariant)
            $sha = ($section | Select-String '(?m)^Result: \d+ rows?, sha256 ([0-9a-f]{64})').Matches[0].Groups[1].Value
            $before[$name] = [pscustomobject]@{ Ms = $ms; Sha = $sha }
        }
    }
    foreach ($m in $measured) {
        $b = $before[$m.Name]
        if ($null -eq $b) { $comparison += $comparisonRow -f $m.Name, '-', $m.Median.ToString('0.000', $invariant), '-', 'new query'; continue }
        $same = $b.Sha -eq $m.Sha
        if (-not $same) { $different++ }
        $factor = if ($m.Median -gt 0) { ($b.Ms / $m.Median).ToString('0.0', $invariant) + 'x' } else { '-' }
        $comparison += $comparisonRow -f $m.Name, $b.Ms.ToString('0.000', $invariant), $m.Median.ToString('0.000', $invariant), $factor, $(if ($same) { 'identical' } else { 'DIFFERENT' })
    }
}

# BUILD FLOW 08.09.07 — Writing the file
$lines = @(
    "# Query measurements: $Label",
    "# Generated by scripts/measure-queries.ps1 on $((Get-Date).ToString('yyyy-MM-ddTHH:mm:sszzz', $invariant)). Do not edit by hand.",
    "# Queries: $($QueryFile -replace '\\', '/')",
    "# MySQL $($context[0]); database catalog_bench; $($context[1]) products; schema version $($context[2]); buffer pool $($context[3]) MB.",
    "# Indexes: $indexes",
    "# Size: $($context[4])",
    "# Method: one warm-up run, then EXPLAIN ANALYZE $Runs times; 'Median ms' is the median of the root step's total time.",
    "#         The fingerprint is a SHA-256 hash of the complete result (every column of every row, in order).",
    '',
    '## Summary',
    ($summaryRow -f 'query', 'median ms', 'rows', 'fingerprint (first 12)')
)
foreach ($m in $measured) { $lines += $summaryRow -f $m.Name, $m.Median.ToString('0.000', $invariant), $m.Rows, $m.Sha.Substring(0, 12) }
if ($Baseline) {
    $lines += @('', "## Comparison with $Baseline", ($comparisonRow -f 'query', 'before ms', 'after ms', 'faster', 'results'))
    $lines += $comparison
}
foreach ($m in $measured) {
    $lines += @('', "## $($m.Name)", "Request: $($m.Request)", "SQL: $($m.Sql)", "Times ms: $($m.Times)",
        "Median ms: $($m.Median.ToString('0.000', $invariant))", "Result: $($m.Rows) rows, sha256 $($m.Sha)", 'Plan of the median run:')
    $lines += ($m.Plan -split "`n")
}
New-Item -ItemType Directory -Force -Path $planDir | Out-Null
[System.IO.File]::WriteAllText($outFile, (($lines -join "`n") + "`n"), (New-Object System.Text.UTF8Encoding($false)))

Write-Host ''
Write-Host "Wrote $outFile"
$summaryStart = [array]::IndexOf($lines, '## Summary') + 1
$lines[$summaryStart..($summaryStart + $measured.Count)] | ForEach-Object { Write-Host $_ }
if ($Baseline) {
    $comparison | ForEach-Object { Write-Host $_ }
    if ($different -gt 0) { Write-Host "RESULTS DIFFER for $different quer(y/ies)."; exit 1 }
    Write-Host 'All results are identical to the baseline.'
}
