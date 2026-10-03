param([string[]]$Mutations = @('inference','binding','tracking','epoch','dimension','nearest','material-barrier','bound','cross-support'))
$ErrorActionPreference = 'Stop'
$repo = Split-Path $PSScriptRoot -Parent
$scratch = Split-Path $repo -Parent
$build = Join-Path $scratch 's29-adversary-build'
$run = Join-Path $scratch 's29-adversary-run'
$cache = Join-Path $scratch 's29-adversary-cache'
$expected = @{
    inference='Consecutive endpoint serials inferred an interval|Missing serial silently selected another interval'
    binding='Mismatched certificate axis 5 became authoritative'
    tracking='Mismatched certificate axis 6 became authoritative'
    epoch='Mismatched certificate axis 0 became authoritative'
    dimension='Mismatched certificate axis 2 became authoritative'
    nearest='Missing serial silently selected another interval|Certified interval changed material endpoints or root trajectory'
    'material-barrier'='pre-UNAVAILABLE material interval revived'
    bound='Overflow inbox retained authoritative interval'
    'cross-support'="Another support's certified interval became authoritative for target"
}
Push-Location $repo
try {
    foreach ($mutation in $Mutations) {
        if (!$expected.ContainsKey($mutation)) { throw "Unknown mutation $mutation" }
        $destination = Join-Path $scratch "s29-front-$mutation"
        $props = @(python tools/s29_adversarial_frontend_mutation.py $mutation $destination)
        if ($LASTEXITCODE -ne 0) { throw "Mutation preparation failed: $mutation" }
        $proof = if ($mutation -eq 'material-barrier') {'S29AdversarialUnavailableReplayProof'} elseif ($mutation -eq 'bound') {'S29AdversarialWireBoundsProof'} elseif ($mutation -eq 'cross-support') {'S29AdversarialCrossSupportProof'} else {'S29AdversarialCertificateIdentityProof'}
        $log = Join-Path $scratch "s29-front-$mutation.log"
        $arguments = @('runClientGameTest', "-Ps29AdversarialProof=io.github.r3neer.scalebrews.test.$proof",
            "-PscalebrewsBuildDir=$build", "-Ps29AdversarialRunDir=$run", '--project-cache-dir', $cache,
            '-I', 'tools/s29_adversarial.init.gradle', '--console=plain') + $props
        if ($mutation -eq 'material-barrier') { $arguments += '-Ps29GravityRoundTrip=true' }
        Write-Output "START $mutation"
        # Windows PowerShell 5 surfaces normal javac stderr notes as ErrorRecords.
        # Judge the native exit and runtime assertion, not those diagnostic records.
        $ErrorActionPreference = 'Continue'
        & .\gradlew.bat @arguments *> $log
        $result = $LASTEXITCODE
        $ErrorActionPreference = 'Stop'
        $output = Get-Content -LiteralPath $log -Raw
        if ($result -eq 0 -or $output -notmatch $expected[$mutation] -or $output -match 'Compilation failed|Task .+ not found') {
            throw "No valid oracle kill for $mutation; inspect $log"
        }
        Write-Output "KILLED $mutation (compiled runtime assertion); $log"
    }
} finally { Pop-Location }
