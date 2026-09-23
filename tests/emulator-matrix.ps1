param(
    [Parameter(Mandatory=$true)][ValidatePattern('^emulator-[0-9]+$')][string]$Serial,
    [Parameter(Mandatory=$true)][string]$AdbPath,
    [Parameter(Mandatory=$true)][string]$ApkPath,
    [Parameter(Mandatory=$true)][ValidateSet('handy','spyware')][string]$App,
    [Parameter(Mandatory=$true)][string]$OutputDirectory,
    [switch]$SkipLifecycle
)
$ErrorActionPreference='Stop'
$AdbPath=(Resolve-Path -LiteralPath $AdbPath).Path
$ApkPath=(Resolve-Path -LiteralPath $ApkPath).Path
New-Item -ItemType Directory -Force -Path $OutputDirectory | Out-Null
$OutputDirectory=(Resolve-Path -LiteralPath $OutputDirectory).Path
$script:checks=0
$script:evidence=@()
function Adb([Parameter(ValueFromRemainingArguments=$true)][string[]]$Arguments) {
    # No fallback to a default/physical device, even when only one is connected.
    $qemu=& $AdbPath -s $Serial shell getprop ro.kernel.qemu
    if ($LASTEXITCODE -ne 0 -or "$qemu".Trim() -ne '1') { throw "Refusing action: $Serial is not a verified emulator" }
    $result=& $AdbPath -s $Serial @Arguments
    if ($LASTEXITCODE -ne 0) { throw "Emulator command failed: $Arguments" }
    return $result
}
function Check([bool]$Condition,[string]$Message) {
    if (!$Condition) { throw "FAIL: $Message" }
    $script:checks++; Write-Output "PASS: $Message"
}
function Dump([string]$Name) {
    Adb @('shell','rm','-f','/sdcard/codex-matrix.xml') | Out-Null
    Adb @('shell','uiautomator','dump','/sdcard/codex-matrix.xml') | Out-Null
    $path=Join-Path $OutputDirectory "$Name.xml"
    Adb @('pull','/sdcard/codex-matrix.xml',$path) | Out-Null
    [xml]$xml=Get-Content -LiteralPath $path -Raw
    return $xml
}
function Capture([string]$Name) {
    Adb @('shell','screencap','-p','/sdcard/codex-matrix.png') | Out-Null
    Adb @('pull','/sdcard/codex-matrix.png',(Join-Path $OutputDirectory "$Name.png")) | Out-Null
    $script:evidence+=$Name
}
function Has([xml]$Xml,[string]$Text) { return @($Xml.SelectNodes('//node') | Where-Object { $_.text -eq $Text }).Count -gt 0 }
function ScrollTop([string]$Name) {
    $xml=Dump "$Name-before-top"
    $root=$xml.SelectNodes('//node') | Select-Object -First 1
    if($root.bounds -notmatch '\[\d+,\d+\]\[(\d+),(\d+)\]') { throw 'No viewport bounds' }
    $w=[int]$Matches[1]; $h=[int]$Matches[2]
    for($i=0;$i -lt 8;$i++) {
        Adb @('shell','input','swipe',"$([int]($w/2))","$([int]($h*.25))","$([int]($w/2))","$([int]($h*.8))",'150') | Out-Null
    }
}
function ClickText([string]$Text,[string]$Name) {
    for($i=0;$i -lt 10;$i++) {
        $xml=Dump "$Name-find-$i"
        $root=$xml.SelectNodes('//node') | Select-Object -First 1
        if($root.bounds -notmatch '\[\d+,\d+\]\[(\d+),(\d+)\]') { throw 'No viewport bounds' }
        $w=[int]$Matches[1]; $h=[int]$Matches[2]
        $node=$xml.SelectNodes('//node') | Where-Object { $_.text -eq $Text -and $_.enabled -eq 'true' } | Select-Object -First 1
        if($node -and $node.bounds -match '\[(\d+),(\d+)\]\[(\d+),(\d+)\]') {
            $x=[int](([int]$Matches[1]+[int]$Matches[3])/2)
            $y=[int](([int]$Matches[2]+[int]$Matches[4])/2)
            # A partially visible view may have accessibility bounds below the
            # navigation bar. Scroll it into the middle before tapping it.
            if($y -gt $h*.1 -and $y -lt $h*.9) {
                Adb @('shell','input','tap',"$x","$y") | Out-Null
                return
            }
        }
        # wm dimensions follow the actual rotated viewport; use its hierarchy bounds.
        $root=$xml.SelectNodes('//node') | Select-Object -First 1
        if($root.bounds -notmatch '\[\d+,\d+\]\[(\d+),(\d+)\]') { throw 'No viewport bounds' }
        $w=[int]$Matches[1]; $h=[int]$Matches[2]
        Adb @('shell','input','swipe',"$([int]($w/2))","$([int]($h*.8))","$([int]($w/2))","$([int]($h*.2))",'300') | Out-Null
    }
    throw "Button unreachable: $Text"
}
function Launch([bool]$Cold=$true) {
    if($Cold) { Adb @('shell','am','force-stop',$package) | Out-Null }
    Adb @('shell','am','start','-W','-n',"$package/.MainActivity") | Out-Null
}
function Layout([string]$Size,[int]$Density,[double]$Font,[int]$Rotation) {
    Adb @('shell','wm','size',$Size) | Out-Null
    Adb @('shell','wm','density',"$Density") | Out-Null
    Adb @('shell','settings','put','system','font_scale',($Font.ToString([cultureinfo]::InvariantCulture))) | Out-Null
    Adb @('shell','settings','put','system','accelerometer_rotation','0') | Out-Null
    Adb @('shell','wm','fixed-to-user-rotation','enabled') | Out-Null
    Adb @('shell','wm','user-rotation','lock',"$Rotation") | Out-Null
    Start-Sleep -Milliseconds 800
}
$package=if($App -eq 'handy'){'de.leterel.isthandy'}else{'com.maximumspyware.parody'}
$originalUserRotation=(Adb @('shell','wm','user-rotation')).Trim()
$originalFixedRotation=(Adb @('shell','wm','fixed-to-user-rotation')).Trim()
$originalSize=Adb @('shell','wm','size')
$originalDensity=Adb @('shell','wm','density')
$originalFont=(Adb @('shell','settings','get','system','font_scale')).Trim()
$originalRotation=(Adb @('shell','settings','get','system','user_rotation')).Trim()
$originalAuto=(Adb @('shell','settings','get','system','accelerometer_rotation')).Trim()
try {
    Check ((Adb @('shell','getprop','sys.boot_completed')).Trim() -eq '1') 'Emulator boot completed'
    Adb @('install','-r',$ApkPath) | Out-Null
    $layouts=@(
        @{Name='phone';Size='1080x1920';Density=420;Font=1.0;Rotation=0;Title='Ja, is Handy, Mois.'},
        @{Name='small-font200';Size='720x1280';Density=360;Font=2.0;Rotation=0;Title='Ja, is Handy, Mois.'},
        @{Name='landscape';Size='1080x1920';Density=420;Font=1.0;Rotation=1;Title='Ja, is Handy, Mois.'},
        @{Name='tablet';Size='1600x2560';Density=320;Font=1.0;Rotation=0;Title='Nee, is Tablet.'}
    )
    foreach($layout in $layouts) {
        Layout $layout.Size $layout.Density $layout.Font $layout.Rotation
        Launch
        # The emulator launcher can force portrait while cold-starting a task.
        # Apply the requested rotation after the activity owns the display.
        Adb @('shell','wm','user-rotation','lock',"$($layout.Rotation)") | Out-Null
        Start-Sleep -Seconds 1
        $name="$App-$($layout.Name)"
        if($App -eq 'handy') {
            Start-Sleep -Seconds 7
            $xml=Dump $name
            Check (Has $xml $layout.Title) "$name correct visible verdict"
            Check ([int]$xml.hierarchy.rotation -eq $layout.Rotation) "$name actual display rotation"
            Capture $name
        } else {
            ClickText 'OKAY, SPIONIER MICH FAKE AUS →' "$name-enter"
            Start-Sleep -Seconds 23
            ClickText 'MEINEN FAKE-BERICHT ANSEHEN →' "$name-report"
            ScrollTop "$name-report"
            $reportXml=Dump "$name-report-top"
            Check (Has $reportXml "WIR HABEN`nNICHTS GEGEN DICH.") "$name completed report visible"
            Capture "$name-report"
            ClickText 'NEUER SCAN' "$name-restart"
            # UIAutomator requires an idle window; an actively ticking scan is
            # deliberately not idle. Wait for completion before dumping it.
            Start-Sleep -Seconds 23
            ScrollTop "$name-restarted"
            $xml=Dump "$name-restarted"
            Check (Has $xml 'LOKALES THEATER / SITZUNG 2') "$name report and restart reachable"
            Check ([int]$xml.hierarchy.rotation -eq $layout.Rotation) "$name actual display rotation"
            Capture "$name-restarted"
        }
    }
    if ($App -eq 'handy' -and !$SkipLifecycle) {
    Layout '1080x1920' 420 1.0 0
    Launch
        Start-Sleep -Seconds 7
        Adb @('shell','input','keyevent','KEYCODE_HOME') | Out-Null
        Start-Sleep -Milliseconds 600
        Launch $false
        Start-Sleep -Milliseconds 700
        Capture 'handy-resume'
        Start-Sleep -Seconds 6
        $xml=Dump 'handy-resume-complete'
        Check (Has $xml 'Ja, is Handy, Mois.') 'Handy completes scan after returning; inspect immediate screenshot for restarted animation'
    }
    [ordered]@{testedAt=(Get-Date).ToString('o');serial=$Serial;api=(Adb @('shell','getprop','ro.build.version.sdk')).Trim();app=$App;apk=(Split-Path $ApkPath -Leaf);sha256=(Get-FileHash $ApkPath -Algorithm SHA256).Hash;checks=$script:checks;evidence=$script:evidence;lifecycleIncluded=($App -eq 'handy' -and !$SkipLifecycle);scope='Android emulator only; display configurations share one API-34 system image'} | ConvertTo-Json -Depth 4 | Set-Content (Join-Path $OutputDirectory "$App-results.json") -Encoding utf8
} finally {
    # Restore only this explicitly verified emulator, never use adb without -s.
    Adb @('shell','wm','fixed-to-user-rotation',$originalFixedRotation) | Out-Null
    if ($originalUserRotation -match '^lock (\d)$') { Adb @('shell','wm','user-rotation','lock',$Matches[1]) | Out-Null } else { Adb @('shell','wm','user-rotation','free') | Out-Null }
    $size=if("$originalSize" -match 'Override size: (\d+x\d+)'){$Matches[1]}else{'reset'}
    $density=if("$originalDensity" -match 'Override density: (\d+)'){$Matches[1]}else{'reset'}
    Adb @('shell','wm','size',$size) | Out-Null
    Adb @('shell','wm','density',$density) | Out-Null
    foreach($setting in @(@('font_scale',$originalFont),@('user_rotation',$originalRotation),@('accelerometer_rotation',$originalAuto))) {
        if($setting[1] -eq 'null') { Adb @('shell','settings','delete','system',$setting[0]) | Out-Null }
        else { Adb @('shell','settings','put','system',$setting[0],$setting[1]) | Out-Null }
    }
}

