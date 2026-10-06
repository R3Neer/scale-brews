param([string]$OutputDirectory,[string[]]$Mutants=@('current-pose','renderer-root','initial-channel','parent-identity','current-flags','shared-state','cube-bounds','fingerprint'),[switch]$SkipBaseline)
$ErrorActionPreference='Stop'
$repo=Split-Path $PSScriptRoot -Parent
if(-not $OutputDirectory){$OutputDirectory=Join-Path (Split-Path $repo -Parent) 's55-adversarial-mutations'}
if(-not [IO.Path]::IsPathRooted($OutputDirectory)){$OutputDirectory=Join-Path $repo $OutputDirectory}
New-Item -ItemType Directory -Force -Path $OutputDirectory | Out-Null
$out=(Resolve-Path -LiteralPath $OutputDirectory).Path
$fixture=Join-Path $repo 'src/gametest/java/io/github/r3neer/scalebrews/client/collision/preparation/S55AdversarialGeometryProof.java'
$edits=@{
 'current-pose'=@('ModelPartStructure','var pose = original.getInitialPose();','var pose = new net.minecraft.client.model.geom.PartPose(original.x,original.y,original.z,original.xRot,original.yRot,original.zRot,original.xScale,original.yScale,original.zScale);')
 'renderer-root'=@('ModelPartStructure','.withModelTransform(new Matrix4f())','')
 'initial-channel'=@('OriginalResetGeometryPreparation',' || !row.initial().equals(part.sourcePose())','')
 'parent-identity'=@('OriginalResetGeometryPreparation',' || !java.util.Objects.equals(row.parent(), part.parent())','')
 'current-flags'=@('ModelPartStructure','result.visible = true;','result.visible = original.visible;')
 'shared-state'=@('OriginalResetGeometryPreparation','var certificate = OriginalResetPosePreparation.inspect(model);','if(model!=null) model.root().xScale += .01f; var certificate = OriginalResetPosePreparation.inspect(model);')
 'cube-bounds'=@('ModelPartStructure','var fingerprint = new Fingerprint(', 'geometry = new ModelGeometry(2,geometry.source(),geometry.version(),geometry.parts(),geometry.pieces().stream().map(p -> new ModelGeometry.Piece(p.id(),p.part(),java.util.List.of(p.min().get(0)+.01,p.min().get(1),p.min().get(2)),java.util.List.of(p.max().get(0)+.01,p.max().get(1),p.max().get(2)),p.excluded())).toList(),geometry.modelTransform()); var fingerprint = new Fingerprint(')
 'fingerprint'=@('OriginalResetGeometryPreparation','if (!recipe.initialStructure().equals(snapshot.fingerprint()))','if (false)')
}
foreach($name in $Mutants){if(-not $edits.ContainsKey($name)){throw "Unknown mutant $name"}}
function Write-Shadow([string]$Dir,[string]$Owner,[string]$Text){
 $java=Join-Path $Dir ('io/github/r3neer/scalebrews/client/collision/preparation/'+$Owner+'.java')
 New-Item -ItemType Directory -Force -Path (Split-Path $java -Parent) | Out-Null
 [IO.File]::WriteAllText($java,$Text,[Text.UTF8Encoding]::new($false));return $java
}
function Invoke-Lane([string]$Name,[string]$Shadow,[bool]$Instrumented){
 $log=Join-Path $out ($Name+'.log')
 $argv=@('-I',(Join-Path $repo 'tools/s55_adversarial.init.gradle'),('-PscalebrewsBuildDir='+ (Join-Path $out 'build')),'--project-cache-dir',(Join-Path $out 'cache'),('-Ps55RunDir='+ (Join-Path $out 'run')),'runClientGameTest')
 if($Shadow){$argv+=('-Ps55Shadow='+$Shadow)}
 if($Instrumented){$argv+='-Ps55Instrumented=true'}
 & (Join-Path $repo 'gradlew.bat') @argv *> $log
 $code=$LASTEXITCODE;$text=[IO.File]::ReadAllText($log)
 [pscustomobject]@{exit=$code;log=$log;assertion=$text.Contains('java.lang.AssertionError: S55 ADV');pass=($code -eq 0 -and $text.Contains('S55_INDEPENDENT_GEOMETRY_PASS'))}
}
$before=(Get-Location).Path
try{
 Set-Location -LiteralPath $repo
 $baseline=$null
 if(-not $SkipBaseline){$baseline=Invoke-Lane 'baseline' '' $false;if(-not $baseline.pass){throw "Baseline failed: $($baseline.log)"}}
 $instrumentedBaseline=$null;$results=@()
 foreach($name in $Mutants){
  $edit=$edits[$name];$owner=$edit[0]
  $source=Join-Path $repo ('src/client/java/io/github/r3neer/scalebrews/client/collision/preparation/'+$owner+'.java')
  $text=[IO.File]::ReadAllText($source);if(-not $text.Contains($edit[1])){throw "Anchor absent $name"}
  $shadow=Join-Path $out ('shadows/'+$name)
  if($name -eq 'fingerprint'){
   $structure=[IO.File]::ReadAllText((Join-Path $repo 'src/client/java/io/github/r3neer/scalebrews/client/collision/preparation/ModelPartStructure.java'))
   $instrument=$structure.Replace('return new Snapshot(fingerprint, geometry);','if(Boolean.getBoolean("scalebrews.s55InjectDrift") && source.startsWith("original_reset/")) fingerprint = new Fingerprint("fault",fingerprint.parts(),fingerprint.cubes(),fingerprint.solidPieces()); return new Snapshot(fingerprint, geometry);')
   $baseShadow=Join-Path $out 'shadows/instrumented-baseline';Write-Shadow $baseShadow 'ModelPartStructure' $instrument | Out-Null
   $instrumentedBaseline=Invoke-Lane 'instrumented-baseline' $baseShadow $true
   if(-not $instrumentedBaseline.pass){throw 'Instrumented baseline failed'}
   Write-Shadow $shadow 'ModelPartStructure' $instrument | Out-Null
  }
  $java=Write-Shadow $shadow $owner ($text.Replace($edit[1],$edit[2]))
  $result=Invoke-Lane $name $shadow ($name -eq 'fingerprint')
  $kill=$result.exit -ne 0 -and $result.assertion
  $results+=[pscustomobject]@{name=$name;owner=$owner;killed=$kill;exit=$result.exit;log=$result.log;sourceSha256=(Get-FileHash -LiteralPath $source).Hash.ToLowerInvariant();shadowSha256=(Get-FileHash -LiteralPath $java).Hash.ToLowerInvariant()}
  Write-Host "$name killed=$kill exit=$($result.exit)"
 }
 $report=[ordered]@{fixtureSha256=(Get-FileHash -LiteralPath $fixture).Hash.ToLowerInvariant();baseline=$baseline;instrumentedBaseline=$instrumentedBaseline;results=$results}
 [IO.File]::WriteAllText((Join-Path $out 'mutation-results.json'),(ConvertTo-Json $report -Depth 12),[Text.UTF8Encoding]::new($false))
 if($results.Where({-not $_.killed}).Count){throw 'Surviving mutation or infrastructure failure; inspect logs'}
}finally{Set-Location -LiteralPath $before}
exit 0
