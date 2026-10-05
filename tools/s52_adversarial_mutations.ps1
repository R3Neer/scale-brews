param([string]$OutputDirectory, [string[]]$Mutants=@('authority','slot-inversion','origin','constructor','bridge-flags','join-target'), [switch]$SkipBaseline)
$ErrorActionPreference='Stop'
$repo=Split-Path $PSScriptRoot -Parent
if(-not $OutputDirectory){$OutputDirectory=Join-Path (Split-Path $repo -Parent) 's52-adversarial-mutations'}
if(-not [IO.Path]::IsPathRooted($OutputDirectory)){$OutputDirectory=Join-Path $repo $OutputDirectory}
New-Item -ItemType Directory -Force -Path $OutputDirectory | Out-Null
$out=(Resolve-Path -LiteralPath $OutputDirectory).Path
$source=Join-Path $repo 'src/client/java/io/github/r3neer/scalebrews/client/collision/preparation/OriginalAgeStateModelPreparation.java'
$fixture=Join-Path $repo 'src/gametest/java/io/github/r3neer/scalebrews/client/collision/preparation/S52AdversarialStateProof.java'
$text=[IO.File]::ReadAllText($source)
$edits=@{
 'authority'=@('"UNPROVEN"','"PROVEN"')
 'slot-inversion'=@('Field field = baby ? slots.baby : slots.adult;','Field field = baby ? slots.adult : slots.baby;')
 'origin'=@('private static void originalOrigin(Class<?> candidate) {','private static void originalOrigin(Class<?> candidate) { if(candidate != null) return;')
 'constructor'=@('private static void constructor(OriginalModelPrograms programs, Slots slots) {','private static void constructor(OriginalModelPrograms programs, Slots slots) { if(programs != null) return;')
 'bridge-flags'=@('if (!method.flags().has(AccessFlag.BRIDGE) || !method.flags().has(AccessFlag.SYNTHETIC))','if (false)')
 'join-target'=@('if (join != code.next)','if (false)')
}
foreach($name in $Mutants){if(-not $edits.ContainsKey($name)){throw "Unknown mutant $name"}}
function Invoke-Lane([string]$Name,[string]$Shadow){
 $log=Join-Path $out ($Name+'.log')
 $arguments=@('-I',(Join-Path $repo 'tools/s52_adversarial.init.gradle'),('-PscalebrewsBuildDir='+ (Join-Path $out 'build')),'--project-cache-dir',(Join-Path $out 'cache'),('-Ps52AdversarialRunDir='+ (Join-Path $out 'run')),'runClientGameTest')
 if($Shadow){$arguments+=('-Ps52AdversarialShadow='+$Shadow)}
 & (Join-Path $repo 'gradlew.bat') @arguments *> $log
 $exit=$LASTEXITCODE
 $content=[IO.File]::ReadAllText($log)
 [pscustomobject]@{name=$Name;exit=$exit;log=$log;assertion=($content -match 'java.lang.AssertionError: S52 ADV');pass=($exit -eq 0 -and $content.Contains('S52_INDEPENDENT_STATE_PASS'))}
}
$before=(Get-Location).Path
try{
 Set-Location -LiteralPath $repo
 $baseline=$null
 if(-not $SkipBaseline){$baseline=Invoke-Lane 'baseline' '';if(-not $baseline.pass){throw "Baseline failed: $($baseline.log)"}}
 $results=@()
 foreach($name in $Mutants){
  $shadow=Join-Path $out ('shadows/'+$name)
  $java=Join-Path $shadow 'io/github/r3neer/scalebrews/client/collision/preparation/OriginalAgeStateModelPreparation.java'
  New-Item -ItemType Directory -Force -Path (Split-Path $java -Parent) | Out-Null
  $edit=$edits[$name];if(-not $text.Contains($edit[0])){throw "Mutation anchor absent $name"}
  [IO.File]::WriteAllText($java,$text.Replace($edit[0],$edit[1]),[Text.UTF8Encoding]::new($false))
  $result=Invoke-Lane $name $shadow
  $killed=$result.exit -ne 0 -and $result.assertion
  $results+=[pscustomobject]@{name=$name;killed=$killed;exit=$result.exit;log=$result.log;shadowSha256=(Get-FileHash -LiteralPath $java).Hash.ToLowerInvariant()}
  Write-Host "$name killed=$killed exit=$($result.exit)"
 }
 $report=[ordered]@{sourceSha256=(Get-FileHash -LiteralPath $source).Hash.ToLowerInvariant();fixtureSha256=(Get-FileHash -LiteralPath $fixture).Hash.ToLowerInvariant();baseline=$baseline;results=$results}
 [IO.File]::WriteAllText((Join-Path $out 'mutation-results.json'),(ConvertTo-Json -InputObject $report -Depth 12),[Text.UTF8Encoding]::new($false))
 if($results.Where({-not $_.killed}).Count){throw 'Surviving mutation or infrastructure failure; inspect logs'}
}finally{Set-Location -LiteralPath $before}