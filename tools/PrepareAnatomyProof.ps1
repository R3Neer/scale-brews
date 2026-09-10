[CmdletBinding()]
param(
    [Parameter(Mandatory=$true)][string]$PackMods,
    [Parameter(Mandatory=$true)][string]$OutputDirectory,
    [string]$JavaHome = $env:JAVA_HOME,
    [string]$GradleUserHome,
    [switch]$Offline
)
$ErrorActionPreference = 'Stop'
# Proof catalog only. This deliberately does not install datapacks, alter worlds, or copy pack settings.
$sourceRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$outputRoot = [IO.Path]::GetFullPath($OutputDirectory)
if (Test-Path -LiteralPath $outputRoot) { throw 'OutputDirectory must be a new directory; existing results are never overwritten.' }
if ([string]::IsNullOrWhiteSpace($JavaHome) -or !(Test-Path -LiteralPath (Join-Path $JavaHome 'bin/java.exe'))) { throw 'Provide a Java 25 installation with -JavaHome.' }
Add-Type -AssemblyName System.IO.Compression.FileSystem
$required = @('alexsmobs','codxlib','cloth-config')
$selected = @{}
foreach ($jar in Get-ChildItem -LiteralPath $PackMods -Filter '*.jar' -File) {
    $archive = [IO.Compression.ZipFile]::OpenRead($jar.FullName)
    try {
        $entry = $archive.GetEntry('fabric.mod.json')
        if (!$entry) { continue }
        $reader = [IO.StreamReader]::new($entry.Open())
        try { $metadata = $reader.ReadToEnd() | ConvertFrom-Json } finally { $reader.Dispose() }
        if ($required -notcontains $metadata.id) { continue }
        if ($selected.ContainsKey($metadata.id)) { throw "Duplicate mod: $($metadata.id)" }
        $selected[$metadata.id] = @{ path=$jar.FullName; version=$metadata.version; sha256=(Get-FileHash -LiteralPath $jar.FullName -Algorithm SHA256).Hash }
    } finally { $archive.Dispose() }
}
foreach ($id in $required) { if (!$selected.ContainsKey($id)) { throw "Missing proof dependency: $id" } }
if ($selected['alexsmobs'].version -ne '2.1.9') { throw 'The grizzly pose proof currently supports Alexs Mobs Continued 2.1.9 only.' }
New-Item -ItemType Directory -Path $outputRoot | Out-Null
$checkout = Join-Path $outputRoot 'checkout'
$mods = Join-Path $outputRoot 'proof-mods'
New-Item -ItemType Directory -Path $checkout,$mods | Out-Null
foreach ($name in @('src','gradle','gradlew','gradlew.bat','build.gradle','settings.gradle','gradle.properties','LICENSE')) {
    Copy-Item -LiteralPath (Join-Path $sourceRoot $name) -Destination $checkout -Recurse
}
foreach ($id in $required) { Copy-Item -LiteralPath $selected[$id].path -Destination (Join-Path $mods "$id.jar") }
# Only these audited model dependencies enter the proof. No EMF, NEA, First Person, resource packs or user configs.
$selected | ConvertTo-Json -Depth 8 | Set-Content -LiteralPath (Join-Path $outputRoot 'inputs.json') -Encoding UTF8
$sourceHashes = Get-ChildItem -LiteralPath (Join-Path $checkout 'src') -Recurse -File | ForEach-Object {
    @{ path=$_.FullName.Substring($checkout.Length+1); sha256=(Get-FileHash -LiteralPath $_.FullName -Algorithm SHA256).Hash }
}
$sourceHashes | ConvertTo-Json -Depth 4 | Set-Content -LiteralPath (Join-Path $outputRoot 'source-hashes.json') -Encoding UTF8
function Save-GameTestEvidence {
    param([Parameter(Mandatory=$true)][string]$Label,[Parameter(Mandatory=$true)][string]$Report)
    $logs = Join-Path $checkout 'build/run/gameTest/logs'
    if (!(Test-Path -LiteralPath $logs)) { throw "Missing GameTest logs for $Label." }
    Copy-Item -LiteralPath $logs -Destination (Join-Path $outputRoot "$Label-logs") -Recurse
    $manifest = Join-Path $checkout 'build/resources/gametest/fabric.mod.json'
    if (!(Test-Path -LiteralPath $manifest)) { throw "Missing effective GameTest manifest for $Label." }
    Copy-Item -LiteralPath $manifest -Destination (Join-Path $outputRoot "$Label-manifest.json")
    $debug = Join-Path $logs 'debug.log'
    $registered = @(Select-String -LiteralPath $debug -Pattern 'Registering test method:' | ForEach-Object { $_.Line.Trim() })
    if ($registered.Count -eq 0) { throw "No Fabric GameTest methods were registered for $Label." }
    $registered | Set-Content -LiteralPath (Join-Path $outputRoot "$Label-registered-tests.txt") -Encoding UTF8
    if (!(Test-Path -LiteralPath $Report)) { throw "Fabric did not emit the requested JUnit report for $Label." }
    [xml]$junit = Get-Content -LiteralPath $Report -Raw
    $executed = @($junit.SelectNodes('//testcase') | ForEach-Object { $_.GetAttribute('name') })
    if ($executed.Count -eq 0) { throw "JUnit report contains no executed GameTest cases for $Label." }
    $executed | Set-Content -LiteralPath (Join-Path $outputRoot "$Label-executed-tests.txt") -Encoding UTF8
}
$oldJavaHome = $env:JAVA_HOME
try {
    $env:JAVA_HOME = $JavaHome
    $baseArgs = @('-p',$checkout,'--console=plain')
    if ($GradleUserHome) { $baseArgs += @('-g',$GradleUserHome) }
    if ($Offline) { $baseArgs += '--offline' }
    & (Join-Path $checkout 'gradlew.bat') @baseArgs "-PscalebrewsCompatMods=$mods" '-PscalebrewsAnatomyProof=true' runClientGameTest
    if ($LASTEXITCODE -ne 0) { throw 'Client reference proof failed. No catalog has been published.' }
    $export = Join-Path $checkout 'build/run/clientGameTest/anatomy-export'
    # A: ordinary dedicated regressions. They intentionally exclude the nine
    # prepared-session cases, but remain mandatory for the rest of the mod.
    $ordinaryReport = Join-Path $outputRoot 'ordinary-dedicated-junit.xml'
    & (Join-Path $checkout 'gradlew.bat') @baseArgs "-PscalebrewsAnatomyCatalog=$export" "-PscalebrewsGameTestReport=$ordinaryReport" build runGameTest
    if ($LASTEXITCODE -ne 0) { throw 'Ordinary dedicated regression suite failed. No catalog has been published.' }
    Save-GameTestEvidence -Label 'ordinary-dedicated' -Report $ordinaryReport
    # B: one isolated prepared runtime lifetime over the same verified export.
    # It is a separate required result, never inferred from the ordinary suite.
    # Force the property-conditioned test manifest to be regenerated: a stale
    # ordinary manifest would make this command silently omit suite B.
    & (Join-Path $checkout 'gradlew.bat') @baseArgs '--rerun-tasks' "-PscalebrewsAnatomyCatalog=$export" '-PscalebrewsAnatomyPreparedSuite=true' processGametestResources
    if ($LASTEXITCODE -ne 0) { throw 'Prepared dedicated manifest generation failed. No catalog has been published.' }
    $preparedManifest = Join-Path $checkout 'build/resources/gametest/fabric.mod.json'
    $preparedEntrypoints = @((Get-Content -LiteralPath $preparedManifest -Raw | ConvertFrom-Json).entrypoints.'fabric-gametest')
    if ($preparedEntrypoints.Count -ne 1 -or $preparedEntrypoints[0] -ne 'io.github.r3neer.scalebrews.test.AnatomyPreparedIntegrationProof') {
        throw 'Prepared dedicated manifest did not select exactly the isolated AnatomyPreparedIntegrationProof suite.'
    }
    $preparedReport = Join-Path $outputRoot 'prepared-dedicated-junit.xml'
    & (Join-Path $checkout 'gradlew.bat') @baseArgs "-PscalebrewsAnatomyCatalog=$export" '-PscalebrewsAnatomyPreparedSuite=true' "-PscalebrewsGameTestReport=$preparedReport" runGameTest
    if ($LASTEXITCODE -ne 0) { throw 'Prepared dedicated anatomy proof failed. No catalog has been published.' }
    Save-GameTestEvidence -Label 'prepared-dedicated' -Report $preparedReport
    # C: delayed tracker lifecycle proof. It alone owns an activated anatomy level
    # across callbacks, and cannot be hidden by A's concurrent raw-core tests or B.
    & (Join-Path $checkout 'gradlew.bat') @baseArgs '--rerun-tasks' '-PscalebrewsAnatomyTrackerLifecycleSuite=true' processGametestResources
    if ($LASTEXITCODE -ne 0) { throw 'Tracker lifecycle manifest generation failed. No catalog has been published.' }
    $trackerManifest = Join-Path $checkout 'build/resources/gametest/fabric.mod.json'
    $trackerEntrypoints = @((Get-Content -LiteralPath $trackerManifest -Raw | ConvertFrom-Json).entrypoints.'fabric-gametest')
    if ($trackerEntrypoints.Count -ne 1 -or $trackerEntrypoints[0] -ne 'io.github.r3neer.scalebrews.test.AnatomyTrackerLifecycleProof') {
        throw 'Tracker lifecycle manifest did not select exactly the isolated AnatomyTrackerLifecycleProof suite.'
    }
    $trackerReport = Join-Path $outputRoot 'tracker-lifecycle-dedicated-junit.xml'
    & (Join-Path $checkout 'gradlew.bat') @baseArgs '-PscalebrewsAnatomyTrackerLifecycleSuite=true' "-PscalebrewsGameTestReport=$trackerReport" runGameTest
    if ($LASTEXITCODE -ne 0) { throw 'Tracker lifecycle proof failed. No catalog has been published.' }
    Save-GameTestEvidence -Label 'tracker-lifecycle-dedicated' -Report $trackerReport
    Copy-Item -LiteralPath $export -Destination (Join-Path $outputRoot 'verified-proof-catalog') -Recurse
    Copy-Item -LiteralPath (Join-Path $checkout 'build/run/clientGameTest/anatomy-report') -Destination (Join-Path $outputRoot 'filter-report') -Recurse
    Write-Host "Proof passed. Results: $outputRoot. This is not yet an installable gameplay datapack."
} finally { $env:JAVA_HOME = $oldJavaHome }
