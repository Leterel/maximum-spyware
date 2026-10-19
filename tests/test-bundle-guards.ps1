param(
    [Parameter(Mandatory=$true)][string]$BundlePath,
    [Parameter(Mandatory=$true)][string]$BundleToolJar,
    [Parameter(Mandatory=$true)][string]$JavaHome,
    [Parameter(Mandatory=$true)][string]$DebugKeyStorePath,
    [Parameter(Mandatory=$true)][string]$PackageId,
    [Parameter(Mandatory=$true)][string]$OutputDirectory
)
$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest
$OutputDirectory = [IO.Path]::GetFullPath($OutputDirectory)
if (Test-Path -LiteralPath $OutputDirectory) { throw 'Choose a new test directory.' }
New-Item -ItemType Directory -Path $OutputDirectory | Out-Null
$runner = Join-Path $PSScriptRoot 'test-app-bundle.ps1'
$arguments = @{
    BundlePath = $BundlePath; BundleToolJar = $BundleToolJar; JavaHome = $JavaHome
    TestKeyStorePath = $DebugKeyStorePath; PackageId = $PackageId
}
$checks = [Collections.Generic.List[string]]::new()
function Reject([scriptblock]$Action, [string]$Pattern, [string]$Name) {
    $failure = $null
    try { & $Action | Out-Null } catch { $failure = $_.Exception.Message }
    if (!$failure -or $failure -notmatch $Pattern) { throw "$Name was not rejected as expected: $failure" }
    $checks.Add($Name)
}
$invalid = $arguments.Clone()
$invalid.PackageId = 'bad/package'
Reject { & $runner @invalid -OutputDirectory (Join-Path $OutputDirectory 'invalid-package') } 'PackageId|pattern' 'invalid package rejected'
if (Test-Path (Join-Path $OutputDirectory 'invalid-package')) { throw 'Invalid package created output.' }
$fixtureKey = Join-Path $OutputDirectory 'non-debug-fixture.jks'
$keytool = Join-Path $JavaHome 'bin/keytool.exe'
& $keytool -genkeypair -keystore $fixtureKey -storepass android -keypass android -alias androiddebugkey -dname 'CN=Release Fixture' -keyalg RSA -keysize 2048 -validity 1 *> (Join-Path $OutputDirectory 'fixture-keytool.txt')
if ($LASTEXITCODE -ne 0) { throw 'Cannot create non-production fixture key.' }
$nonDebug = $arguments.Clone()
$nonDebug.TestKeyStorePath = $fixtureKey
Reject { & $runner @nonDebug -OutputDirectory (Join-Path $OutputDirectory 'wrong-key') } 'Only the pinned development' 'non-debug identity rejected'
if (Test-Path (Join-Path $OutputDirectory 'wrong-key')) { throw 'Wrong key created output.' }
Reject { & $runner @arguments -Serial 'physical-device' -OutputDirectory (Join-Path $OutputDirectory 'physical') } 'Serial|pattern' 'physical serial rejected'
$existing = Join-Path $OutputDirectory 'preserved'
New-Item -ItemType Directory -Path $existing | Out-Null
Reject { & $runner @arguments -OutputDirectory $existing } 'new output directory' 'existing evidence preserved'
# No Serial: this verifies the debug-signing path without touching any device.
& $runner @arguments -OutputDirectory (Join-Path $OutputDirectory 'debug-success') | Out-Null
$checks.Add('pinned development identity accepted and APK set built')
$result = @{ passed = $checks.Count; checks = @($checks); deviceActions = $false }
$result | ConvertTo-Json -Depth 3 | Set-Content -LiteralPath (Join-Path $OutputDirectory 'guards-result.json')
$result | ConvertTo-Json -Depth 3
