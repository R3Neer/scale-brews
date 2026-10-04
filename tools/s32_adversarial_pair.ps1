param([ValidateSet('', 'host', 'peer')][string]$Role='', [string]$Rendezvous='')
$ErrorActionPreference='Stop'
$repo=Split-Path -Parent $PSScriptRoot
$work=Split-Path -Parent $repo
if(!$Role) {
    $Rendezvous=Join-Path $work ('s32-adversary-pair-'+(Get-Date -Format 'yyyyMMdd-HHmmss-fff'))
    New-Item -ItemType Directory -Path $Rendezvous | Out-Null
    $jobs=@()
    foreach($lane in @('host','peer')) {
        $quotedScript='"'+$PSCommandPath+'"';$quotedRendezvous='"'+$Rendezvous+'"'
        $jobs+=Start-Process -FilePath (Join-Path $PSHOME 'pwsh.exe') -WindowStyle Hidden -PassThru `
            -ArgumentList @('-NoProfile','-File',$quotedScript,'-Role',$lane,'-Rendezvous',$quotedRendezvous)
    }
    foreach($job in $jobs){$job.WaitForExit()}
    foreach($job in $jobs){if($job.ExitCode -ne 0){throw "Native pair failed. Inspect $Rendezvous host.log and peer.log"}}
    Write-Output "S32 native pair GREEN: $Rendezvous"
    exit 0
}
if ($env:JAVA_HOME) {
    if (!(Test-Path -LiteralPath (Join-Path $env:JAVA_HOME 'bin/java.exe'))) {
        throw 'JAVA_HOME must point to an installed JDK compatible with this project.'
    }
    $env:Path="$(Join-Path $env:JAVA_HOME 'bin');$env:Path"
} elseif (!(Get-Command java -ErrorAction SilentlyContinue)) {
    throw 'Configure JAVA_HOME or put a compatible JDK on PATH before running this proof.'
}
Set-Location -LiteralPath $repo
$gradleArgs=@('runClientGameTest',
    '-Ps29AdversarialProof=io.github.r3neer.scalebrews.test.S32AdversarialNativeBundlePairProof',
    "-PscalebrewsBuildDir=$work/s32-adversary-$Role-build",
    "-Ps29AdversarialRunDir=$work/s32-adversary-$Role-run",
    '--project-cache-dir',"$work/s32-adversary-$Role-cache",
    "-Ps32NativePairRole=$Role","-Ps32NativePairDir=$Rendezvous",
    '-I','tools/s29_adversarial.init.gradle','--console=plain')
& .\gradlew.bat @gradleArgs *> (Join-Path $Rendezvous "$Role.log")
exit $LASTEXITCODE
