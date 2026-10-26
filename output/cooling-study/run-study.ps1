$ErrorActionPreference = 'Stop'
$studyRoot = (Resolve-Path (Join-Path $PSScriptRoot '../..')).Path
Set-Location -LiteralPath $studyRoot
$studyDependencies = @((Get-Content -LiteralPath 'build/moddev/clientLegacyClasspath.txt'))
$studyDependencies += Join-Path $studyRoot 'build/classes/java/main'
$studyDependencies += Get-ChildItem -LiteralPath 'build/moddev/artifacts' -Filter '*.jar' | Where-Object { $_.Name -notmatch 'sources' } | Select-Object -ExpandProperty FullName
$studyDependencies += Get-ChildItem -LiteralPath (Join-Path $env:USERPROFILE '.gradle/caches/modules-2/files-2.1/org.jetbrains/annotations/24.1.0') -Recurse -Filter '*.jar' | Select-Object -ExpandProperty FullName
$studyClasspath = $studyDependencies -join ';'
$studyClasses = Join-Path $PSScriptRoot 'classes'
New-Item -ItemType Directory -Path $studyClasses -Force | Out-Null
$studySources = @(
  'src/main/java/biggerreactorsrework/Config.java',
  'src/main/java/biggerreactorsrework/core/util/HeatBody.java',
  'src/main/java/biggerreactorsrework/multiblocks/reactor/simulation/SimulationConfiguration.java',
  'src/main/java/biggerreactorsrework/multiblocks/reactor/simulation/SimulationDescription.java',
  'src/main/java/biggerreactorsrework/multiblocks/reactor/simulation/base/BaseReactorSimulation.java',
  'src/main/java/biggerreactorsrework/multiblocks/reactor/simulation/base/SimUtil.java',
  'src/main/java/biggerreactorsrework/multiblocks/reactor/simulation/base/CoolantTank.java',
  'src/main/java/biggerreactorsrework/multiblocks/reactor/simulation/base/FuelTank.java',
  'src/main/java/biggerreactorsrework/multiblocks/reactor/simulation/base/Battery.java',
  'src/main/java/biggerreactorsrework/multiblocks/reactor/simulation/cpu/TimeSlicedReactorSimulation.java',
  'src/main/java/biggerreactorsrework/util/FluidTransitionTank.java',
  'output/cooling-study/CoolingStudy.java'
)
& javac -proc:none -implicit:none -sourcepath $studyClasses -encoding UTF-8 -cp $studyClasspath -d $studyClasses @studySources
if ($LASTEXITCODE -ne 0) { throw 'Study compilation failed' }
& java -cp ($studyClasses + ';' + $studyClasspath) CoolingStudy
if ($LASTEXITCODE -ne 0) { throw 'Study execution failed' }
$studySources | ForEach-Object { Get-FileHash -LiteralPath $_ -Algorithm SHA256 } | Select-Object Path,Hash | ConvertTo-Json | Set-Content -LiteralPath 'output/cooling-study/source-hashes.json' -Encoding utf8
