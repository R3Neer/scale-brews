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
$oldJavaHome = $env:JAVA_HOME
try {
    $env:JAVA_HOME = $JavaHome
    $baseArgs = @('-p',$checkout,'--console=plain')
    if ($GradleUserHome) { $baseArgs += @('-g',$GradleUserHome) }
    if ($Offline) { $baseArgs += '--offline' }
    & (Join-Path $checkout 'gradlew.bat') @baseArgs "-PscalebrewsCompatMods=$mods" '-PscalebrewsAnatomyProof=true' runClientGameTest
    if ($LASTEXITCODE -ne 0) { throw 'Client reference proof failed. No catalog has been published.' }
    $export = Join-Path $checkout 'build/run/clientGameTest/anatomy-export'
    & (Join-Path $checkout 'gradlew.bat') @baseArgs "-PscalebrewsAnatomyCatalog=$export" build runGameTest
    if ($LASTEXITCODE -ne 0) { throw 'Dedicated proof failed. No catalog has been published.' }
    Copy-Item -LiteralPath $export -Destination (Join-Path $outputRoot 'verified-proof-catalog') -Recurse
    Copy-Item -LiteralPath (Join-Path $checkout 'build/run/clientGameTest/anatomy-report') -Destination (Join-Path $outputRoot 'filter-report') -Recurse
    Write-Host "Proof passed. Results: $outputRoot. This is not yet an installable gameplay datapack."
} finally { $env:JAVA_HOME = $oldJavaHome }
