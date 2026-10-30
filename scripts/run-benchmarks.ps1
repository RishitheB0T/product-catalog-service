# BUILD FLOW 11.03 — scripts/run-benchmarks.ps1
# First introduced: Phase 11
# Purpose: Runs the whole benchmark protocol and writes the evidence: search without and with the Phase 08 indexes,
#          then product detail with the cache off, cold, and warm. Each part starts the backend (bench profile) fresh,
#          warms it up, and runs several trials with k6 from the pinned Docker image. The computer is kept awake;
#          a trial disturbed outside the backend runs again (11.03.02); sleep or standby discards the run (11.03.03).
#          Usage: .\scripts\run-benchmarks.ps1                       (everything, about ten minutes)
#                 .\scripts\run-benchmarks.ps1 -SkipIndexComparison  (keeps catalog_bench; no "without indexes" runs)
#                 .\scripts\run-benchmarks.ps1 -AllowBattery         (measure a laptop on battery; slower numbers)
# Depends on: Docker, ".\scripts\dev-services.ps1 up", scripts/bench-db.ps1 (08.02), a built jar in backend\target,
#             benchmarks/k6/product-detail.js (11.01) and search.js (11.02).
# Used by: The developer; docs/PERFORMANCE.md explains the results.
# Runtime role: Developer tooling only. Without -SkipIndexComparison it rebuilds the local benchmark database
#               catalog_bench (never "catalog"). Everything it starts, it stops.

# BUILD FLOW 11.03.01 — Parameters and paths
param(
    [ValidateRange(1, 10)] [int]$Trials = 3,
    [ValidateRange(1, 100)] [int]$Vus = 10,
    [ValidatePattern('^\d+s$')] [string]$TrialDuration = '30s',
    [ValidatePattern('^\d+s$')] [string]$WarmupDuration = '20s',
    [switch]$SkipIndexComparison,
    [switch]$AllowBattery
)
$ErrorActionPreference = 'Stop'
$invariant = [System.Globalization.CultureInfo]::InvariantCulture

$repoRoot    = Split-Path -Parent $PSScriptRoot
$jar         = Join-Path $repoRoot 'backend\target\catalog-0.0.1-SNAPSHOT.jar'
$k6Dir       = Join-Path $repoRoot 'benchmarks\k6'
$resultsDir  = Join-Path $repoRoot 'benchmarks\results'
$benchDb     = Join-Path $PSScriptRoot 'bench-db.ps1'
$composeFile = Join-Path $repoRoot 'infra\compose.dev.yaml'
$envFile     = Join-Path $repoRoot 'infra\.env'
$k6Image     = 'grafana/k6:2.3.0'
$appUrl      = 'http://127.0.0.1:24083'
$k6Url       = 'http://host.docker.internal:24083'
# A scratch folder for k6's result files and the backend logs. The user's local application data folder comes in the
# long path form that Docker's volume mounts need; TEMP can be a short 8.3 path, with names like "ABCDEF~1".
$localData   = [Environment]::GetFolderPath('LocalApplicationData')
$work        = Join-Path $localData ('Temp\catalog-k6-' + (Get-Date -Format 'yyyyMMdd-HHmmss'))
New-Item -ItemType Directory -Force -Path $work | Out-Null
if (-not (Test-Path -LiteralPath $jar)) { throw 'No jar found. Build it first: cd backend; .\mvnw.cmd clean verify' }

# A laptop on battery slows its processor down, which lowers every number; Windows reports status 1 ("discharging").
$onBattery = @(Get-CimInstance Win32_Battery | Where-Object BatteryStatus -eq 1).Count -gt 0
if ($onBattery -and -not $AllowBattery) { throw 'The computer is running on battery, which slows the processor. Plug it in, or pass -AllowBattery to measure anyway.' }

# What counts as a disturbance outside the backend (Get-Disturbance, 11.03.02), and how often a disturbed trial is
# run again. A timed k6 run may end a little after its planned length, because it lets requests in flight finish.
$allowedOverrunSeconds = 5
$slowConnectSeconds = 5
$maxAttempts = 3

# BUILD FLOW 11.03.02 — Helpers
# Native programs (docker, curl.exe, java) report progress on stderr, which Windows PowerShell 5.1 would turn into
# errors under "Stop"; success is judged by their exit codes instead.
function Invoke-Native([scriptblock]$Command) {
    $previous = $ErrorActionPreference
    $ErrorActionPreference = 'Continue'
    try { & $Command } finally { $ErrorActionPreference = $previous }
}

# Keeps the computer and its display awake while the protocol runs. On a laptop with Modern Standby, the display
# switching off for being idle puts the whole system into standby, which pauses Docker and the backend mid-trial.
# The request belongs to this script's thread and ends with it, or when the normal state is set again.
if (-not ('CatalogBench.Power' -as [type])) {
    Add-Type -Namespace CatalogBench -Name Power -MemberDefinition '[DllImport("kernel32.dll")] public static extern uint SetThreadExecutionState(uint flags);'
}
$stayAwake   = [uint32]'0x80000003'  # ES_CONTINUOUS | ES_SYSTEM_REQUIRED | ES_DISPLAY_REQUIRED
$normalPower = [uint32]'0x80000000'  # ES_CONTINUOUS alone: the usual power settings apply again

# Runs one k6 script in the pinned container and returns the path of its summary file. The container reaches the
# backend through host.docker.internal, which Docker Desktop forwards to this computer's loopback address.
function Invoke-K6([string]$Script, [string]$Label, [hashtable]$Settings) {
    $envArgs = @('-e', "BASE_URL=$k6Url", '-e', "LABEL=$Label", '-e', "VUS=$Vus")
    foreach ($key in $Settings.Keys) { $envArgs += @('-e', "$key=$($Settings[$key])") }
    $output = Invoke-Native { docker run --rm -v "${k6Dir}:/scripts:ro" -v "${work}:/results" @envArgs $k6Image run --quiet "/scripts/$Script" 2>&1 }
    # k6's full output, including a warning for every failed request, is kept next to the summary.
    $output | ForEach-Object { "$_" } | Set-Content -LiteralPath (Join-Path $work "$Label.out") -Encoding UTF8
    if ($LASTEXITCODE -ne 0) { throw "k6 run $Label failed with exit code ${LASTEXITCODE}: $($output -join ' ')" }
    $output | Where-Object { "$_" -like "$Label*" } | ForEach-Object { Write-Host "    $_" }
    $file = Join-Path $work "$Label.json"
    if (-not (Test-Path -LiteralPath $file)) { throw "k6 wrote no summary for $Label" }
    return $file
}

# Decides whether something outside the backend disturbed a measured run, judged only by facts about the run itself,
# never by its latency results. Returns the reasons, or an empty string for a clean run. k6 logs a "Request Failed"
# warning for every request without a response; "dial" in it ("dial: i/o timeout", "dial tcp ...: connection
# refused") means no connection could be opened, so the backend never saw the request. Any other failure (an error
# status, a reset, a timeout) is the backend's own result: it stops the whole protocol instead of being repeated away.
function Get-Disturbance([string]$Label, [hashtable]$Settings) {
    $summary = Get-Content -LiteralPath (Join-Path $work "$Label.json") -Raw -Encoding UTF8 | ConvertFrom-Json
    $failed = [int]$summary.metrics.http_req_failed.values.passes
    $notConnected = @(Get-Content -LiteralPath (Join-Path $work "$Label.out") | Where-Object { $_ -match 'msg="Request Failed"' -and $_ -match '\bdial\b' }).Count
    if ($failed -gt $notConnected) {
        throw "$($failed - $notConnected) requests of $Label failed after reaching the backend. No results were written; see $Label.out and the backend log in $work"
    }
    $reasons = New-Object System.Collections.Generic.List[string]
    if ($notConnected -gt 0) { $reasons.Add("$notConnected requests could not open a connection") }
    $connectSeconds = [double]$summary.metrics.http_req_connecting.values.max / 1000
    if ($connectSeconds -gt $slowConnectSeconds) { $reasons.Add("opening a connection took $([math]::Round($connectSeconds, 1)) s") }
    if ($Settings.ContainsKey('DURATION')) {
        $planned = [int]$Settings.DURATION.TrimEnd('s')
        $actual = [double]$summary.state.testRunDurationMs / 1000
        if ($actual -gt $planned + $allowedOverrunSeconds) { $reasons.Add("it lasted $([math]::Round($actual, 1)) s instead of $planned s") }
    }
    return ($reasons -join '; ')
}

# Two settings for these runs only: the metrics endpoint (on 127.0.0.1) for the cache counters, and connections kept
# open for good. Tomcat closes a connection after 100 requests by default; at hundreds of requests per second the
# k6 container then opens several new connections a second through Docker Desktop's forwarding, where some stalled for
# 20-30 s (D-049). Every scenario runs with the same settings.
$benchSettings = @('--management.endpoints.web.exposure.include=health,metrics', '--server.tomcat.max-keep-alive-requests=-1')
$script:backend = $null
$script:startupNotes = New-Object System.Collections.Generic.List[string]

# Starts the backend with the bench profile and waits until the benchmark seeder has reported, which happens after the
# web server is already answering: measuring earlier could hit a half-loaded table.
function Start-Backend([string]$Name, [string[]]$Extra) {
    $log = Join-Path $work "$Name.log"
    $arguments = @('-jar', $jar, '--spring.profiles.active=bench') + $benchSettings + @("--logging.file.name=$log") + $Extra
    $script:backend = Start-Process -FilePath 'java' -ArgumentList $arguments -WorkingDirectory (Join-Path $repoRoot 'backend') -PassThru -WindowStyle Hidden
    $deadline = (Get-Date).AddSeconds(240)
    while ((Get-Date) -lt $deadline) {
        if ($script:backend.HasExited) { throw "The backend exited early (code $($script:backend.ExitCode)); see $log" }
        if ((Test-Path -LiteralPath $log) -and (Select-String -LiteralPath $log -Pattern 'Loaded \d+ benchmark products|Benchmark data not loaded' -Quiet)) { break }
        Start-Sleep -Milliseconds 500
    }
    $notes = Select-String -LiteralPath $log -Pattern 'Successfully applied \d+ migration|Loaded \d+ benchmark products|Benchmark data not loaded' |
        ForEach-Object { $_.Line -replace '^.*? : ', '' }
    foreach ($note in $notes) { Write-Host "    $note"; $script:startupNotes.Add("${Name}: $note") }
}

function Stop-Backend {
    if ($script:backend -and -not $script:backend.HasExited) {
        Stop-Process -Id $script:backend.Id -Force
        $script:backend.WaitForExit()
        Start-Sleep -Seconds 2
    }
    $script:backend = $null
}

# The cache counters of the running backend (only this local run exposes /actuator/metrics, on 127.0.0.1).
function Get-CacheCounts {
    $counts = [ordered]@{}
    foreach ($result in 'hit', 'miss', 'error') {
        $raw = Invoke-Native { curl.exe -s "$appUrl/actuator/metrics/catalog.product.cache?tag=result:$result" }
        $metric = $raw | ConvertFrom-Json
        $counts[$result] = if ($metric.measurements) { [double]$metric.measurements[0].value } else { 0.0 }
    }
    return $counts
}

# Removes exactly the 1,000 measured keys (never FLUSHALL), so that the next pass starts with a cold cache for them.
# The keys travel as arguments, 250 at a time, not through a pipe: on a console set to UTF-8, Windows PowerShell 5.1
# puts a byte-order mark in front of text piped into a program, and Redis then saw an unknown command instead of DEL.
# redis-cli exits with 0 even when Redis answers with an error, so the answer itself (a number) is checked.
function Clear-MeasuredKeys {
    $keys = @(1..1000 | ForEach-Object { "catalog:v1:product:$($_ * 100)" })
    for ($first = 0; $first -lt $keys.Count; $first += 250) {
        $batch = $keys[$first..($first + 249)]
        $reply = Invoke-Native { docker compose --env-file $envFile -f $composeFile exec -T redis redis-cli DEL @batch 2>&1 }
        if ($LASTEXITCODE -ne 0 -or "$reply".Trim() -notmatch '^\d+$') { throw "Deleting the measured keys from Redis failed: $reply" }
    }
}

$runs = New-Object System.Collections.Generic.List[object]
function Add-Run([string]$Scenario, [int]$Trial, [string]$File, $Before, $After) {
    $cache = $null
    if ($Before) { $cache = [ordered]@{ hit = $After.hit - $Before.hit; miss = $After.miss - $Before.miss; error = $After.error - $Before.error } }
    $runs.Add([pscustomobject]@{ Scenario = $Scenario; Trial = $Trial; File = $File; Cache = $cache })
}

# Runs one measured trial. A disturbed attempt is set aside (the report lists it with its reason) and the trial runs
# again, up to $maxAttempts times. -ColdCache deletes the measured keys before every attempt; -CountCache records the
# cache counters around it.
$setAside = New-Object System.Collections.Generic.List[object]
function Invoke-Trial([string]$Scenario, [int]$Trial, [string]$Script, [hashtable]$Settings, [switch]$ColdCache, [switch]$CountCache) {
    foreach ($attempt in 1..$maxAttempts) {
        $label = if ($attempt -eq 1) { "$Scenario-$Trial" } else { "$Scenario-$Trial-attempt-$attempt" }
        if ($ColdCache) { Clear-MeasuredKeys }
        $before = if ($CountCache) { Get-CacheCounts } else { $null }
        $file = Invoke-K6 $Script $label $Settings
        $after = if ($CountCache) { Get-CacheCounts } else { $null }
        # A cold pass must be exactly what its name says: 1,000 misses and no hit. Anything else is a fault in this
        # script, not a disturbance, so it stops the run.
        if ($ColdCache -and (($after.hit - $before.hit) -ne 0 -or ($after.miss - $before.miss) -ne 1000)) {
            throw "Pass $label was not cold: $($after.hit - $before.hit) hits and $($after.miss - $before.miss) misses instead of 0 and 1000. No results were written."
        }
        $reason = Get-Disturbance $label $Settings
        if (-not $reason) { Add-Run $Scenario $Trial $file $before $after; return }
        Write-Host "    $label set aside: $reason"
        $setAside.Add([pscustomobject]@{ attempt = $label; reason = $reason })
    }
    throw "Trial $Trial of $Scenario was disturbed $maxAttempts times in a row. No results were written; k6 output and backend logs: $work"
}

# BUILD FLOW 11.03.03 — The protocol
# Every part: a fresh backend, a warm-up whose results are discarded (it warms the JVM's compiled code and MySQL's
# buffer pool), then the measured trials. Only the warm-cache trials start with a warm Redis.
$started = Get-Date
$null = [CatalogBench.Power]::SetThreadExecutionState($stayAwake)
try {
    if (-not $SkipIndexComparison) {
        Write-Host '1/3  Search without the Phase 08 indexes: fresh catalog_bench at schema V1'
        Invoke-Native { & powershell -NoProfile -ExecutionPolicy Bypass -File $benchDb drop -Force | Out-Null }
        Invoke-Native { & powershell -NoProfile -ExecutionPolicy Bypass -File $benchDb create | Out-Null }
        Start-Backend 'search-without-indexes' @('--spring.flyway.target=1', '--catalog.cache.enabled=false')
        $null = Invoke-K6 'search.js' 'warmup-search-without-indexes' @{ DURATION = $WarmupDuration }
        foreach ($t in 1..$Trials) { Invoke-Trial 'search-without-indexes' $t 'search.js' @{ DURATION = $TrialDuration } }
        Stop-Backend
    }

    Write-Host '2/3  Search with the indexes, then product detail with the cache off'
    Start-Backend 'cache-off' @('--catalog.cache.enabled=false')
    $null = Invoke-K6 'search.js' 'warmup-search-with-indexes' @{ DURATION = $WarmupDuration }
    foreach ($t in 1..$Trials) { Invoke-Trial 'search-with-indexes' $t 'search.js' @{ DURATION = $TrialDuration } }
    $null = Invoke-K6 'product-detail.js' 'warmup-detail-cache-off' @{ DURATION = $WarmupDuration; ID_OFFSET = 100 }
    foreach ($t in 1..$Trials) {
        Invoke-Trial 'detail-cache-off' $t 'product-detail.js' @{ DURATION = $TrialDuration; ID_OFFSET = 100 } -CountCache
    }
    Stop-Backend

    Write-Host '3/3  Product detail with the cache on: cold passes, then warm trials'
    Start-Backend 'cache-on' @('--catalog.cache.enabled=true')
    # The warm-up uses other ids (ID_OFFSET 50), so the JVM is warm but Redis holds none of the measured keys.
    $null = Invoke-K6 'product-detail.js' 'warmup-detail-cache-on' @{ DURATION = $WarmupDuration; ID_OFFSET = 50 }
    foreach ($t in 1..$Trials) {
        Invoke-Trial 'detail-cache-cold' $t 'product-detail.js' @{ MODE = 'pass'; ID_OFFSET = 100 } -ColdCache -CountCache
    }
    foreach ($t in 1..$Trials) {
        Invoke-Trial 'detail-cache-warm' $t 'product-detail.js' @{ DURATION = $TrialDuration; ID_OFFSET = 100 } -CountCache
    }
    Stop-Backend
} finally {
    Stop-Backend
    $null = [CatalogBench.Power]::SetThreadExecutionState($normalPower)
}

# Staying awake cannot stop a closed lid or the power button, and the cold passes have no planned length to check.
# Windows logs every entry into sleep (Kernel-Power event 42) and into Modern Standby (event 506): one of them
# anywhere in the run discards the results.
$pauses = @(Get-WinEvent -FilterHashtable @{ LogName = 'System'; ProviderName = 'Microsoft-Windows-Kernel-Power'; Id = 42, 506; StartTime = $started } -ErrorAction SilentlyContinue)
if ($pauses.Count -gt 0) {
    throw "The computer entered sleep or standby during the run (first at $($pauses[-1].TimeCreated.ToString('HH:mm:ss'))). No results were written; run the script again."
}

# BUILD FLOW 11.03.04 — The setup, recorded with the results
$cpu = Get-CimInstance Win32_Processor | Select-Object -First 1
$os = Get-CimInstance Win32_OperatingSystem
$setup = [ordered]@{
    date = $started.ToString('yyyy-MM-ddTHH:mm:sszzz', $invariant)
    commit = (Invoke-Native { git -C $repoRoot rev-parse --short HEAD }).Trim()
    backendCodeChangedSinceCommit = [bool](Invoke-Native { git -C $repoRoot status --porcelain backend })
    machine = "$($cpu.Name.Trim()), $($cpu.NumberOfCores) cores / $($cpu.NumberOfLogicalProcessors) threads, $([math]::Round($os.TotalVisibleMemorySize / 1MB, 1)) GB RAM"
    os = "$($os.Caption) (build $($os.BuildNumber))"
    powerSource = if ($onBattery) { 'battery' } else { 'mains' }
    checks = "a trial counts only if every request got a response, every connection opened within $slowConnectSeconds s, and a timed run ended within $allowedOverrunSeconds s of its planned length; Windows logged no sleep or standby during the run"
    java = ((Invoke-Native { java -version 2>&1 }) | Select-Object -First 1).ToString().Trim()
    docker = (Invoke-Native { docker version --format '{{.Server.Version}}' }).Trim()
    loadGenerator = "$k6Image in Docker, on the same computer as everything else"
    networkPath = 'k6 container -> host.docker.internal -> Docker Desktop host forwarding -> backend on 127.0.0.1:24083'
    services = 'mysql:8.4.11 and redis:8.10.2-alpine (dev Compose); backend jar with the bench profile'
    backendSettings = $benchSettings -join ' '
    dataset = '100,000 generated products, seed 20261022 (dataset fingerprint 6b78f508...462c367f)'
    virtualUsers = $Vus
    trialDuration = $TrialDuration
    warmupDuration = $WarmupDuration
    trialsPerScenario = $Trials
    startupNotes = @($script:startupNotes)
}

# BUILD FLOW 11.03.05 — The raw results: every trial's complete k6 summary, merged into one file
# The k6 summaries are copied verbatim (as text), so nothing in them is rounded or reshaped.
New-Item -ItemType Directory -Force -Path $resultsDir | Out-Null
$parts = foreach ($run in $runs) {
    $cacheJson = if ($run.Cache) { $run.Cache | ConvertTo-Json -Compress } else { 'null' }
    '{"scenario":"' + $run.Scenario + '","trial":' + $run.Trial + ',"cacheCounters":' + $cacheJson + ',"k6":' +
        (Get-Content -LiteralPath $run.File -Raw -Encoding UTF8).Trim() + '}'
}
# The set-aside list is joined by hand: Windows PowerShell 5.1's ConvertTo-Json fails on an empty list.
$setAsideJson = '[' + (($setAside | ForEach-Object { $_ | ConvertTo-Json -Compress }) -join ',') + ']'
$merged = '{"setup":' + ($setup | ConvertTo-Json -Depth 3) + ',"setAside":' + $setAsideJson + ',"runs":[' + ($parts -join ",`n") + "]}`n"
[System.IO.File]::WriteAllText((Join-Path $resultsDir 'k6-summaries.json'), $merged.Replace("`r`n", "`n"), (New-Object System.Text.UTF8Encoding($false)))

# BUILD FLOW 11.03.06 — The report: tables computed from the raw results
function Get-Median([double[]]$Values) {
    $sorted = @($Values | Sort-Object)
    $n = $sorted.Count
    if ($n % 2 -eq 1) { return $sorted[[int][Math]::Floor($n / 2)] }
    return ($sorted[$n / 2 - 1] + $sorted[$n / 2]) / 2
}
function Format-Number([double]$Value, [int]$Decimals = 2) { [string]::Format($invariant, "{0:N$Decimals}", $Value) }
function Format-Range([double[]]$Values, [int]$Decimals = 2) {
    $median = Format-Number (Get-Median $Values) $Decimals
    if ($Values.Count -lt 2) { return $median }
    $min = Format-Number (($Values | Measure-Object -Minimum).Minimum) $Decimals
    $max = Format-Number (($Values | Measure-Object -Maximum).Maximum) $Decimals
    # Plain ASCII only inside strings: Windows PowerShell 5.1 reads a script file without a byte-order mark as
    # Windows-1252, where the bytes of a UTF-8 dash include a "smart quote" that ends the string.
    return "$median ($min to $max)"
}

$summaries = @{}
foreach ($run in $runs) { $summaries["$($run.Scenario)-$($run.Trial)"] = Get-Content -LiteralPath $run.File -Raw -Encoding UTF8 | ConvertFrom-Json }
function Get-Values([string]$Scenario, [string]$Metric, [string]$Stat) {
    $values = foreach ($run in ($runs | Where-Object Scenario -eq $Scenario)) {
        [double]($summaries["$Scenario-$($run.Trial)"].metrics.$Metric.values.$Stat)
    }
    return [double[]]@($values)
}

$lines = New-Object System.Collections.Generic.List[string]
$lines.Add('# Benchmark results')
$lines.Add('')
$lines.Add("Generated by ``scripts/run-benchmarks.ps1`` on $($setup.date). Do not edit by hand: every number below is")
$lines.Add('computed from `k6-summaries.json` in this folder. `docs/PERFORMANCE.md` explains the method and the results.')
$lines.Add('Each value is the median of the trials, with the lowest and highest trial in brackets.')
$lines.Add('')
$lines.Add('## Setup')
$lines.Add('')
$lines.Add('| Item | Value |')
$lines.Add('|---|---|')
foreach ($key in $setup.Keys) { if ($key -ne 'startupNotes') { $lines.Add("| $key | $($setup[$key]) |") } }
$lines.Add("| startupNotes | $(($setup.startupNotes) -join '; ') |")

$lines.Add('')
$lines.Add('## Product detail: `GET /api/v1/products/{id}`, 1,000 ids (every 100th product)')
$lines.Add('')
$lines.Add('| Scenario | Trials | Requests per trial | Throughput (req/s) | Mean (ms) | Median (ms) | p95 (ms) | Failed | Cache hits / misses / errors (all trials) |')
$lines.Add('|---|---:|---:|---:|---:|---:|---:|---:|---|')
foreach ($scenario in 'detail-cache-off', 'detail-cache-cold', 'detail-cache-warm') {
    $scenarioRuns = @($runs | Where-Object Scenario -eq $scenario)
    if ($scenarioRuns.Count -eq 0) { continue }
    $hits = ($scenarioRuns | ForEach-Object { $_.Cache.hit } | Measure-Object -Sum).Sum
    $misses = ($scenarioRuns | ForEach-Object { $_.Cache.miss } | Measure-Object -Sum).Sum
    $errors = ($scenarioRuns | ForEach-Object { $_.Cache.error } | Measure-Object -Sum).Sum
    $title = @{ 'detail-cache-off' = 'Cache off'; 'detail-cache-cold' = 'Cache on, cold (each id once; every request a miss)'; 'detail-cache-warm' = 'Cache on, warm' }[$scenario]
    $lines.Add("| $title | $($scenarioRuns.Count) | $(Format-Range (Get-Values $scenario 'http_reqs' 'count') 0) | $(Format-Range (Get-Values $scenario 'http_reqs' 'rate') 1) | $(Format-Range (Get-Values $scenario 'http_req_duration' 'avg')) | $(Format-Range (Get-Values $scenario 'http_req_duration' 'med')) | $(Format-Range (Get-Values $scenario 'http_req_duration' 'p(95)')) | $(Format-Range ((Get-Values $scenario 'http_req_failed' 'rate') | ForEach-Object { $_ * 100 }) 2)% | $hits / $misses / $errors |")
}

$searchNames = 'home-newest', 'category-newest', 'price-range-in-stock', 'name-contains-common'
$lines.Add('')
$lines.Add('## Search: four searches in rotation (see `benchmarks/k6/search.js`)')
$lines.Add('')
$lines.Add('| Search | Without indexes: median (ms) | Without indexes: p95 (ms) | With indexes: median (ms) | With indexes: p95 (ms) |')
$lines.Add('|---|---:|---:|---:|---:|')
foreach ($name in $searchNames) {
    $metric = "http_req_duration{name:$name}"
    $without = if (@($runs | Where-Object Scenario -eq 'search-without-indexes').Count) { @((Format-Range (Get-Values 'search-without-indexes' $metric 'med')), (Format-Range (Get-Values 'search-without-indexes' $metric 'p(95)'))) } else { @('not run', 'not run') }
    $with = @((Format-Range (Get-Values 'search-with-indexes' $metric 'med')), (Format-Range (Get-Values 'search-with-indexes' $metric 'p(95)')))
    $lines.Add("| $name | $($without[0]) | $($without[1]) | $($with[0]) | $($with[1]) |")
}
$lines.Add('')
$lines.Add('| Search run | Trials | Requests per trial | Throughput (req/s) | Failed |')
$lines.Add('|---|---:|---:|---:|---:|')
foreach ($scenario in 'search-without-indexes', 'search-with-indexes') {
    $scenarioRuns = @($runs | Where-Object Scenario -eq $scenario)
    if ($scenarioRuns.Count -eq 0) { continue }
    $lines.Add("| $scenario | $($scenarioRuns.Count) | $(Format-Range (Get-Values $scenario 'http_reqs' 'count') 0) | $(Format-Range (Get-Values $scenario 'http_reqs' 'rate') 1) | $(Format-Range ((Get-Values $scenario 'http_req_failed' 'rate') | ForEach-Object { $_ * 100 }) 2)% |")
}

$lines.Add('')
$lines.Add('## Attempts set aside')
$lines.Add('')
if ($setAside.Count -eq 0) {
    $lines.Add('None: every trial was clean on its first attempt.')
} else {
    $lines.Add('Disturbed outside the backend and run again (the rules are in `scripts/run-benchmarks.ps1`, 11.03.02). Their')
    $lines.Add('numbers are in no table above; their k6 output stayed in the scratch folder.')
    $lines.Add('')
    $lines.Add('| Attempt | Reason |')
    $lines.Add('|---|---|')
    foreach ($item in $setAside) { $lines.Add("| $($item.attempt) | $($item.reason) |") }
}
[System.IO.File]::WriteAllText((Join-Path $resultsDir 'k6-report.md'), (($lines -join "`n") + "`n"), (New-Object System.Text.UTF8Encoding($false)))

Write-Host ''
Write-Host "Done in $([math]::Round(((Get-Date) - $started).TotalMinutes, 1)) minutes. Results: benchmarks\results\k6-report.md and k6-summaries.json"
Write-Host "Scratch files (k6 summaries, backend logs): $work"
