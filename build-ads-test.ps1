param(
    [Parameter(Mandatory=$true)][string]$SdkPath,
    [Parameter(Mandatory=$true)][string]$JavaHome,
    [Parameter(Mandatory=$true)][string]$GradleCache,
    [Parameter(Mandatory=$true)][string]$AndroidUserHome,
    [Parameter(Mandatory=$true)][string]$OutputDirectory,
    [switch]$Online
)
# Always the isolated .adstest package with official sample IDs, debug-signed.
$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest
$project = $PSScriptRoot
$sampleIds = @('ca-app-pub-3940256099942544~3347511713','ca-app-pub-3940256099942544/9214589741')
$profileText = (Get-ChildItem -LiteralPath (Join-Path $project 'app/src/adsTest') -Recurse -File | Where-Object { $_.Extension -in @('.java','.xml') } | ForEach-Object { Get-Content -LiteralPath $_.FullName -Raw }) -join "`n"
$foundIds = @([regex]::Matches($profileText, 'ca-app-pub-\d+[~/]\d+') | ForEach-Object Value | Select-Object -Unique)
if ($foundIds.Count -ne 2 -or @($foundIds | Where-Object { $_ -notin $sampleIds }).Count) { throw 'adsTest may only use the two fixed official sample IDs.' }
$OutputDirectory = [IO.Path]::GetFullPath($OutputDirectory)
if (Test-Path -LiteralPath $OutputDirectory) { throw 'Use a new output directory to preserve prior evidence.' }
$aapt = Join-Path (Resolve-Path -LiteralPath $SdkPath).Path 'build-tools/36.0.0/aapt2.exe'
if (!(Test-Path -LiteralPath $aapt)) { throw 'Android build tools 36.0.0 are required.' }
New-Item -ItemType Directory -Path $OutputDirectory | Out-Null
$previous = @{}
foreach ($name in @('JAVA_HOME','ANDROID_HOME','ANDROID_SDK_ROOT','GRADLE_USER_HOME','ANDROID_USER_HOME')) {
    $previous[$name] = [Environment]::GetEnvironmentVariable($name, 'Process')
}
try {
    $env:JAVA_HOME = (Resolve-Path -LiteralPath $JavaHome).Path
    $env:ANDROID_HOME = (Resolve-Path -LiteralPath $SdkPath).Path
    $env:ANDROID_SDK_ROOT = $env:ANDROID_HOME
    $env:GRADLE_USER_HOME = [IO.Path]::GetFullPath($GradleCache)
    $env:ANDROID_USER_HOME = [IO.Path]::GetFullPath($AndroidUserHome)
    & (Join-Path $project 'tests/test-consent-gate.ps1') -JavaHome $JavaHome
    $arguments = @('--no-daemon','-PadsTest=true',"-Pandroid.aapt2FromMavenOverride=$aapt",'assembleDebug')
    if (!$Online) { $arguments = @('--offline') + $arguments }
    Push-Location $project
    try {
        & (Join-Path $project 'gradlew.bat') @arguments *> (Join-Path $OutputDirectory 'build.txt')
        if ($LASTEXITCODE -ne 0) { throw 'adsTest build failed. See build.txt.' }
    } finally { Pop-Location }
    $built = Join-Path $project 'app/build/outputs/apk/debug/app-debug.apk'
    $badging = & $aapt dump badging $built
    if ($LASTEXITCODE -ne 0) { throw 'Cannot inspect adsTest APK.' }
    $package = [regex]::Match(($badging -join "`n"), "package: name='([^']+)'").Groups[1].Value
    if (!$package.EndsWith('.adstest')) { throw 'Refusing to export a non-adstest APK.' }
    if (($badging -join "`n") -match 'READ_BASIC_PHONE_STATE|permission.AD_ID|ACCESS_ADSERVICES_') { throw 'Unnecessary identifier/phone permissions leaked into adsTest APK.' }
    $mergedManifest = Join-Path $project 'app/build/intermediates/merged_manifests/debug/processDebugManifest/AndroidManifest.xml'
    [xml]$manifest = Get-Content -LiteralPath $mergedManifest -Raw
    if ($manifest.SelectNodes('/manifest/application/provider').Count -ne 0) { throw 'An automatic content-provider initializer was merged into adsTest.' }
    Copy-Item -LiteralPath $mergedManifest -Destination (Join-Path $OutputDirectory 'manifest.xml')
    $badging | Set-Content -LiteralPath (Join-Path $OutputDirectory 'manifest-badging.txt') -Encoding utf8
    $destination = Join-Path $OutputDirectory 'google-test-ads-debug.apk'
    Copy-Item -LiteralPath $built -Destination $destination
    [ordered]@{ package = $package; profile = 'adsTest'; officialTestIdsOnly = $true; debugSigned = $true;
        sha256 = (Get-FileHash -LiteralPath $destination -Algorithm SHA256).Hash;
        productionAds = $false; revenue = $false; consentFormAvailabilityRequiresNetwork = $true
    } | ConvertTo-Json | Set-Content -LiteralPath (Join-Path $OutputDirectory 'result.json') -Encoding utf8
    Write-Output "Built isolated Google test-ad APK: $destination"
} finally {
    foreach ($name in $previous.Keys) { [Environment]::SetEnvironmentVariable($name, $previous[$name], 'Process') }
}
