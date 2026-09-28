param(
    [Parameter(Mandatory=$true)][ValidatePattern('^emulator-[0-9]+$')][string]$Serial,
    [Parameter(Mandatory=$true)][string]$AdbPath,
    [Parameter(Mandatory=$true)][string]$ApkPath,
    [ValidateSet('lifecycle','process-death')][string]$Scenario='lifecycle',
    [string]$OutputDirectory
)
$ErrorActionPreference='Stop'
Set-StrictMode -Version Latest
$repo=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..\..'))
$build=Join-Path $repo 'build'
if(!$OutputDirectory){$OutputDirectory=Join-Path $build ($Scenario+'-'+(Get-Date -Format 'yyyyMMdd-HHmmss'))}
$OutputDirectory=[IO.Path]::GetFullPath($OutputDirectory)
if(!$OutputDirectory.StartsWith($build+[IO.Path]::DirectorySeparatorChar)){throw 'Evidence must stay beneath this project build directory'}
New-Item -ItemType Directory -Force -Path $OutputDirectory | Out-Null
$AdbPath=(Resolve-Path -LiteralPath $AdbPath).Path
$ApkPath=(Resolve-Path -LiteralPath $ApkPath).Path
$probe=(Resolve-Path -LiteralPath (Join-Path $build 'lifecycle-probe\probe.apk')).Path
function Adb([Parameter(ValueFromRemainingArguments=$true)][string[]]$Arguments) {
    $qemu=& $AdbPath -s $Serial shell getprop ro.kernel.qemu
    if($LASTEXITCODE -ne 0 -or "$qemu".Trim() -ne '1'){throw "Refusing action: $Serial is not a verified emulator"}
    & $AdbPath -s $Serial @Arguments
    if($LASTEXITCODE -ne 0){throw "Emulator command failed: $Arguments"}
}
if((Adb @('shell','getprop','sys.boot_completed')).Trim() -ne '1'){throw 'Emulator is not booted'}
$oldSize=Adb @('shell','wm','size')
$oldDensity=Adb @('shell','wm','density')
$oldFont=(Adb @('shell','settings','get','system','font_scale')).Trim()
$oldAuto=(Adb @('shell','settings','get','system','accelerometer_rotation')).Trim()
$oldRotation=(Adb @('shell','wm','user-rotation')).Trim()
$oldFixed=(Adb @('shell','wm','fixed-to-user-rotation')).Trim()
$installed=$false
try {
    Adb @('shell','wm','size','1080x1920') | Out-Null
    Adb @('shell','wm','density','420') | Out-Null
    Adb @('shell','settings','put','system','font_scale','1.0') | Out-Null
    Adb @('shell','settings','put','system','accelerometer_rotation','0') | Out-Null
    Adb @('shell','wm','fixed-to-user-rotation','enabled') | Out-Null
    Adb @('shell','wm','user-rotation','lock','0') | Out-Null
    Adb @('install','-r',$ApkPath) | Out-Null
    Adb @('install','-r',$probe) | Out-Null
    $installed=$true
    # Clear only the separate test helper to prevent stale results after a failed run.
    Adb @('shell','pm','clear','de.leterel.prototypeqa') | Out-Null
    Adb @('shell','am','instrument','-w','-e','scenario',$Scenario,'de.leterel.prototypeqa/.UiProbe') |
        Tee-Object -FilePath (Join-Path $OutputDirectory 'instrumentation.log')
    Adb @('pull',('/sdcard/Android/data/de.leterel.prototypeqa/files/'+$Scenario),$OutputDirectory) | Out-Null
    $result=Get-Content -LiteralPath (Join-Path $OutputDirectory ($Scenario+'\'+$Scenario+'-results.json')) -Raw | ConvertFrom-Json
    [ordered]@{
        testedAt=(Get-Date).ToString('o');serial=$Serial;api=$result.api;
        apk=(Split-Path $ApkPath -Leaf);sha256=(Get-FileHash -LiteralPath $ApkPath -Algorithm SHA256).Hash;
        scenario=$Scenario;status=$result.status;checks=@($result.checks).Count;
        scope="Real UI on one API-$($result.api) Android emulator; no physical devices; original production APK";
    } | ConvertTo-Json | Set-Content -LiteralPath (Join-Path $OutputDirectory 'verification.json') -Encoding utf8
    if($result.status -ne 'passed'){throw "$Scenario test failed: $($result.error)"}
    Write-Output "PASS: $(@($result.checks).Count) $Scenario checks; evidence in $OutputDirectory"
} finally {
    Adb @('shell','wm','fixed-to-user-rotation',$oldFixed) | Out-Null
    if($oldRotation -match '^lock (\d)$'){Adb @('shell','wm','user-rotation','lock',$Matches[1])|Out-Null}
    else{Adb @('shell','wm','user-rotation','free')|Out-Null}
    $size=if("$oldSize" -match 'Override size: (\d+x\d+)'){$Matches[1]}else{'reset'}
    $density=if("$oldDensity" -match 'Override density: (\d+)'){$Matches[1]}else{'reset'}
    Adb @('shell','wm','size',$size)|Out-Null
    Adb @('shell','wm','density',$density)|Out-Null
    foreach($setting in @(@('font_scale',$oldFont),@('accelerometer_rotation',$oldAuto))){
        if($setting[1] -eq 'null'){Adb @('shell','settings','delete','system',$setting[0])|Out-Null}
        else{Adb @('shell','settings','put','system',$setting[0],$setting[1])|Out-Null}
    }
    if($installed){Adb @('uninstall','de.leterel.prototypeqa')|Out-Null}
}
