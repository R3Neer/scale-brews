param([string]$OutputDirectory,[string[]]$Mutants=@('body-slot','null-target','origin','map-collapse','family'),[switch]$SkipBaseline)
$ErrorActionPreference='Stop'
$repo=Split-Path $PSScriptRoot -Parent
if(-not $OutputDirectory){$OutputDirectory=Join-Path (Split-Path $repo -Parent) 's53-adversarial-mutations'}
if(-not [IO.Path]::IsPathRooted($OutputDirectory)){$OutputDirectory=Join-Path $repo $OutputDirectory}
New-Item -ItemType Directory -Force -Path $OutputDirectory | Out-Null
$out=(Resolve-Path -LiteralPath $OutputDirectory).Path
$fixture=Join-Path $repo 'src/gametest/java/io/github/r3neer/scalebrews/client/collision/preparation/S53AdversarialEnumProof.java'
$edits=@{
 'body-slot'=@('OriginalEnumSelectorProof','!bodySlot.getName().equals("BODY")','false')
 'null-target'=@('OriginalEnumSelectorProof','if (present != code.next)','if (false)')
 'origin'=@('OriginalEnumSelectorProof','static void originalOrigin(Class<?> candidate, Class<?> originalPlatform) {','static void originalOrigin(Class<?> candidate, Class<?> originalPlatform) { if(candidate != null) return;')
 'map-collapse'=@('EnumModelPreparation','map.get(constant)','map.get(constants[0])')
 'family'=@('EnumAgeModelPreparation','Family.ENUM_AGE','Family.ENUM_ONLY')
}
foreach($name in $Mutants){if(-not $edits.ContainsKey($name)){throw "Unknown mutant $name"}}
function Invoke-Lane([string]$Name,[string]$Shadow,[string]$Owner){
 $log=Join-Path $out ($Name+'.log')
 $argv=@('-I',(Join-Path $repo 'tools/s53_adversarial.init.gradle'),('-PscalebrewsBuildDir='+ (Join-Path $out 'build')),'--project-cache-dir',(Join-Path $out 'cache'),('-Ps53AdversarialRunDir='+ (Join-Path $out 'run')),'runClientGameTest')
 if($Shadow){$argv+=('-Ps53AdversarialShadow='+$Shadow);$argv+=('-Ps53AdversarialShadowOwner='+$Owner)}
 & (Join-Path $repo 'gradlew.bat') @argv *> $log
 $code=$LASTEXITCODE
 $text=[IO.File]::ReadAllText($log)
 [pscustomobject]@{exit=$code;log=$log;assertion=$text.Contains('java.lang.AssertionError: S53 ADV');pass=($code -eq 0 -and $text.Contains('S53_INDEPENDENT_ENUM_PASS') -and $text.Contains('S51_INDEPENDENT_NATIVE_PASS'))}
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
