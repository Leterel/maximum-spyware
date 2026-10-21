param([Parameter(Mandatory=$true)][string]$JavaHome)
$ErrorActionPreference = 'Stop'
$project = Split-Path $PSScriptRoot
$output = Join-Path $project 'build/consent-gate-tests'
New-Item -ItemType Directory -Force -Path $output | Out-Null
& (Join-Path $JavaHome 'bin/javac.exe') -encoding UTF-8 --release 8 -d $output (Join-Path $project 'app/src/main/java/de/leterel/ads/ConsentGate.java') (Join-Path $PSScriptRoot 'ConsentGateTest.java')
if ($LASTEXITCODE -ne 0) { throw 'ConsentGate test compilation failed.' }
& (Join-Path $JavaHome 'bin/java.exe') -cp $output de.leterel.ads.ConsentGateTest
if ($LASTEXITCODE -ne 0) { throw 'ConsentGate checks failed.' }
