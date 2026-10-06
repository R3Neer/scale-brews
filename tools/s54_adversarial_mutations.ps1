param([string]$OutputDirectory,[string[]]$Mutants=@('loop-target','field-identity','cache-membership','override','transform-swap','path-frame'),[switch]$SkipBaseline)
$ErrorActionPreference='Stop'
$repo=Split-Path $PSScriptRoot -Parent
if(-not $OutputDirectory){$OutputDirectory=Join-Path (Split-Path $repo -Parent) 's54-adversarial-mutations'}
if(-not [IO.Path]::IsPathRooted($OutputDirectory)){$OutputDirectory=Join-Path $repo $OutputDirectory}
New-Item -ItemType Directory -Force -Path $OutputDirectory | Out-Null
$out=(Resolve-Path -LiteralPath $OutputDirectory).Path
$fixture=Join-Path $repo 'src/gametest/java/io/github/r3neer/scalebrews/client/collision/preparation/S54AdversarialResetProof.java'
$edits=@{
 'loop-target'=@('OriginalResetPosePreparation','if (reset.branch(Opcode.GOTO) != loop || done != reset.next)','if ((reset.branch(Opcode.GOTO) != loop || done != reset.next) && false)')
 'field-identity'=@('OriginalResetPosePreparation','if (!field.owner().asInternalName().equals(owner) || !field.name().stringValue().equals(name) || !field.type().stringValue().equals(type))','if (false)')
 'cache-membership'=@('OriginalResetPosePreparation',' || !seen.contains(part) || !cacheSeen.add(part)',' || false')
 'override'=@('OriginalResetPosePreparation','if (programs.resolveMethod(owner, "setupAnim", "(Ljava/lang/Object;)V").getDeclaringClass() != Model.class)','if (programs.resolveMethod(owner, "setupAnim", "(Ljava/lang/Object;)V").getDeclaringClass() != Model.class && false)')
 'transform-swap'=@('OriginalResetPosePreparation','new Transform(p.x(), p.y(), p.z(),','new Transform(p.y(), p.x(), p.z(),')
 'path-frame'=@('OriginalResetPosePreparation','path + "/" + key.length() + ":" + key','path + "/" + ((key.length()==1 || key.contains("/")) ? key : key.length()+":"+key)')
}
foreach($name in $Mutants){if(-not $edits.ContainsKey($name)){throw "Unknown mutant $name"}}
function Invoke-Lane([string]$Name,[string]$Shadow,[string]$Owner){
 $log=Join-Path $out ($Name+'.log')
 $argv=@('-I',(Join-Path $repo 'tools/s54_adversarial.init.gradle'),('-PscalebrewsBuildDir='+ (Join-Path $out 'build')),'--project-cache-dir',(Join-Path $out 'cache'),('-Ps54RunDir='+ (Join-Path $out 'run')),'runClientGameTest')
 if($Shadow){$argv+=('-Ps54Shadow='+$Shadow)}
 & (Join-Path $repo 'gradlew.bat') @argv *> $log
 $code=$LASTEXITCODE
 $text=[IO.File]::ReadAllText($log)
 [pscustomobject]@{exit=$code;log=$log;assertion=$text.Contains('java.lang.AssertionError: S54 ADV');pass=($code -eq 0 -and $text.Contains('S54_INDEPENDENT_RESET_PASS'))}
}
$before=(Get-Location).Path
try{
 Set-Location -LiteralPath $repo
 $baseline=$null
 if(-not $SkipBaseline){$baseline=Invoke-Lane 'baseline' '' '';if(-not $baseline.pass){throw "Baseline failed: $($baseline.log)"}}
 $results=@()
 foreach($name in $Mutants){
  $edit=$edits[$name];$owner=$edit[0]
  $source=Join-Path $repo ('src/client/java/io/github/r3neer/scalebrews/client/collision/preparation/'+$owner+'.java')
  $text=[IO.File]::ReadAllText($source);if(-not $text.Contains($edit[1])){throw "Anchor absent $name"}
  $shadow=Join-Path $out ('shadows/'+$name)
  $java=Join-Path $shadow ('io/github/r3neer/scalebrews/client/collision/preparation/'+$owner+'.java')
  New-Item -ItemType Directory -Force -Path (Split-Path $java -Parent) | Out-Null
  [IO.File]::WriteAllText($java,$text.Replace($edit[1],$edit[2]),[Text.UTF8Encoding]::new($false))
  $result=Invoke-Lane $name $shadow $owner
  $kill=$result.exit -ne 0 -and $result.assertion
  $results+=[pscustomobject]@{name=$name;owner=$owner;killed=$kill;exit=$result.exit;log=$result.log;sourceSha256=(Get-FileHash -LiteralPath $source).Hash.ToLowerInvariant();shadowSha256=(Get-FileHash -LiteralPath $java).Hash.ToLowerInvariant()}
  Write-Host "$name killed=$kill exit=$($result.exit)"
 }
 $report=[ordered]@{fixtureSha256=(Get-FileHash -LiteralPath $fixture).Hash.ToLowerInvariant();baseline=$baseline;results=$results}
 [IO.File]::WriteAllText((Join-Path $out 'mutation-results.json'),(ConvertTo-Json $report -Depth 12),[Text.UTF8Encoding]::new($false))
 if($results.Where({-not $_.killed}).Count){throw 'Surviving mutation or infrastructure failure; inspect logs'}
}finally{Set-Location -LiteralPath $before}
exit 0
