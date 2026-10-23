param(
 [Parameter(Mandatory=$true)][ValidatePattern('^emulator-[0-9]+$')][string]$Serial,
 [Parameter(Mandatory=$true)][string]$AdbPath,
 [Parameter(Mandatory=$true)][string]$ApkPath,
 [Parameter(Mandatory=$true)][ValidateSet('handy','spyware')][string]$App,
 [Parameter(Mandatory=$true)][string]$ExpectedSha256,
 [Parameter(Mandatory=$true)][string]$OutputDirectory,
 [ValidateRange(26,100)][int]$ExpectedApi = 34
)
$ErrorActionPreference='Stop'; Set-StrictMode -Version Latest
$AdbPath=(Resolve-Path -LiteralPath $AdbPath).Path
$ApkPath=(Resolve-Path -LiteralPath $ApkPath).Path
$sha=(Get-FileHash -LiteralPath $ApkPath -Algorithm SHA256).Hash
if($sha -ne $ExpectedSha256){throw 'APK hash mismatch'}
New-Item -ItemType Directory -Path $OutputDirectory | Out-Null
$OutputDirectory=(Resolve-Path -LiteralPath $OutputDirectory).Path
$base=if($App -eq 'handy'){'de.leterel.isthandy'}else{'com.maximumspyware.parody'}
$package="$base.adstest"
$script:checks=@();$script:steps=@();$script:pids=@()
$label='WERBUNG · GOOGLE-TESTANZEIGE · KEINE EINNAHMEN'
$retry='Datenschutz / Testwerbung erneut prüfen'
function Adb([Parameter(ValueFromRemainingArguments=$true)][string[]]$Arguments){
 $q=& $AdbPath -s $Serial shell getprop ro.kernel.qemu
 if($LASTEXITCODE -ne 0 -or "$q".Trim() -ne '1'){throw 'Refusing non-QEMU or unavailable emulator'}
 $r=& $AdbPath -s $Serial @Arguments
 if($LASTEXITCODE -ne 0){throw "ADB command failed: $Arguments"};return $r
}
function Check([bool]$Condition,[string]$Message){$script:checks+=@{name=$Message;pass=$Condition};if(!$Condition){throw "FAIL: $Message"};Write-Host "PASS: $Message"}
function Dump([string]$Name){
 Adb @('shell','uiautomator','dump','/sdcard/codex-ads-failclosed.xml') | Out-Null
 $path=Join-Path $OutputDirectory "$Name.xml";Adb @('pull','/sdcard/codex-ads-failclosed.xml',$path) | Out-Null
 return [xml](Get-Content -LiteralPath $path -Raw)
}
function Capture([string]$Name){
 Adb @('shell','screencap','-p','/sdcard/codex-ads-failclosed.png') | Out-Null
 Adb @('pull','/sdcard/codex-ads-failclosed.png',(Join-Path $OutputDirectory "$Name.png")) | Out-Null
 $script:steps+=$Name
}
function Has([xml]$Xml,[string]$Text){return @($Xml.SelectNodes('//node')|Where-Object {$_.text -eq $Text}).Count -gt 0}
function Texts([xml]$Xml){return @($Xml.SelectNodes('//node')|ForEach-Object {$_.text}|Where-Object {$_})}
function Swipe([xml]$Xml,[bool]$Down=$true){
 $root=$Xml.SelectNodes('//node')|Select-Object -First 1
 if($root.bounds -notmatch '\[\d+,\d+\]\[(\d+),(\d+)\]'){throw 'Missing actual viewport bounds'}
 $w=[int]$Matches[1];$h=[int]$Matches[2];$from=if($Down){.78}else{.25};$to=if($Down){.25}else{.78}
 Adb @('shell','input','swipe',"$([int]($w*.5))","$([int]($h*$from))","$([int]($w*.5))","$([int]($h*$to))",'250') | Out-Null
}
function Find([string]$Text,[string]$Name,[bool]$Tap=$false){
 for($i=0;$i -lt 12;$i++){
  $xml=Dump "$Name-$i";$node=$xml.SelectNodes('//node')|Where-Object {$_.text -eq $Text -and (!$Tap -or $_.enabled -eq 'true')}|Select-Object -First 1
  if($node -and $node.bounds -match '\[(\d+),(\d+)\]\[(\d+),(\d+)\]'){
   $x=[int](([int]$Matches[1]+[int]$Matches[3])/2);$y=[int](([int]$Matches[2]+[int]$Matches[4])/2)
   if($Tap){Adb @('shell','input','tap',"$x","$y") | Out-Null};return $xml
  };Swipe $xml
 };throw "Text unreachable: $Text"
}
function Alive([string]$Name){
 $appPid=(Adb @('shell','pidof',$package)).Trim();Check ([bool]$appPid) "$Name app process alive"
 foreach($one in $appPid.Split(' ')){
  $script:pids+=$one
  $logs=Adb @('logcat','-d',"--pid=$one",'-v','threadtime')
  $logs | Set-Content -LiteralPath (Join-Path $OutputDirectory "$Name-pid-$one.log")
  Check (!(($logs -join "`n") -match 'FATAL EXCEPTION|AndroidRuntime: Process:')) "$Name no app-PID fatal exception"
 }
}
function FailClosed([string]$Name){
 $xml=Find $label "$Name-panel";Capture "$Name-panel"
 $status=(Texts $xml)|Where-Object {$_ -match 'Keine Anzeigenanfrage|Keine Testwerbung: Datenschutz|Datenschutzdienst nicht erreichbar'}
 if(!$status){Start-Sleep -Seconds 16;$xml=Find $label "$Name-settled";$status=(Texts $xml)|Where-Object {$_ -match 'Keine Anzeigenanfrage|Keine Testwerbung: Datenschutz|Datenschutzdienst nicht erreichbar'}}
 Check ([bool]$status) "$Name truthful fail-closed consent status"
 Check (!(Has $xml 'Google-Testanzeige. Keine produktive Werbung und keine Einnahmen.')) "$Name no delivered-banner success label"
 Capture "$Name-failclosed"
 # Landscape can expose the label at the lower edge while the status is still
 # below it. Scroll to the actual bottom for a readable visual proof as well.
 Swipe $xml;$null=Dump "$Name-readable";Capture "$Name-readable"
 return @($status)
}
$originalAuto=$null;$originalFixed=$null;$originalRotation=$null;$success=$false;$status=@();$actualApi=$null
try{
 Check ((Adb @('shell','getprop','sys.boot_completed')).Trim() -eq '1') 'Emulator boot complete'
 $actualApi=[int](Adb @('shell','getprop','ro.build.version.sdk')).Trim()
 Check ($actualApi -eq $ExpectedApi) "API $ExpectedApi verified"
 $originalAuto=(Adb @('shell','settings','get','system','accelerometer_rotation')).Trim()
 $originalFixed=(Adb @('shell','wm','fixed-to-user-rotation')).Trim()
 $originalRotation=(Adb @('shell','wm','user-rotation')).Trim()
 Adb @('install','-r',$ApkPath) | Out-Null
 Adb @('shell','pm','clear',$package) | Out-Null
 Adb @('shell','settings','put','system','accelerometer_rotation','0') | Out-Null
 Adb @('shell','wm','fixed-to-user-rotation','enabled') | Out-Null
 Adb @('shell','wm','user-rotation','lock','0') | Out-Null
 Adb @('shell','am','start','-W','-n',"$package/$base.MainActivity") | Out-Null
 if($App -eq 'spyware'){
  $xml=Dump '01-disclaimer';Check (!(Has $xml $label)) 'No ad panel on disclaimer';Capture '01-disclaimer'
  $null=Find 'OKAY, SPIONIER MICH FAKE AUS →' '02-enter' $true
 }
 Start-Sleep -Milliseconds 600;Capture '03-active-scan'
 if($App -eq 'spyware'){Swipe $xml;Capture '03-active-scan-bottom'}
 Start-Sleep -Seconds $(if($App -eq 'handy'){7}else{23})
 if($App -eq 'spyware'){
  $xml=Dump '04-complete-no-report';Check (!(Has $xml $label)) 'No ad panel before report opened';Capture '04-complete-no-report'
  $null=Find 'MEINEN FAKE-BERICHT ANSEHEN →' '05-report-open' $true
 }
 $status+=FailClosed '06-result'
 Alive '06-result'
 $null=Find $retry '07-retry' $true
 Start-Sleep -Seconds 16;$status+=FailClosed '07-retry-complete';Alive '07-retry-complete'
 Adb @('shell','input','keyevent','KEYCODE_HOME') | Out-Null;Start-Sleep -Seconds 2
 Adb @('shell','am','start','-W','-n',"$package/$base.MainActivity") | Out-Null
 Start-Sleep -Seconds $(if($App -eq 'handy'){8}else{2})
 $status+=FailClosed '08-resume';Alive '08-resume'
 Adb @('shell','wm','user-rotation','lock','1') | Out-Null
 Start-Sleep -Seconds $(if($App -eq 'handy'){8}else{3})
 $status+=FailClosed '09-landscape';$xml=Dump '09-actual-rotation';Check ([int]$xml.hierarchy.rotation -eq 1) 'Actual landscape rotation';Alive '09-landscape'
 Adb @('shell','wm','user-rotation','lock','0') | Out-Null;Start-Sleep -Seconds $(if($App -eq 'handy'){8}else{3})
 $status+=FailClosed '10-return-portrait';Alive '10-return-portrait';$success=$true
}finally{
 if($null -ne $originalRotation -and $originalRotation -match '^lock\s+([0-3])$'){Adb @('shell','wm','user-rotation','lock',$Matches[1])|Out-Null} elseif ($null -ne $originalRotation -and $originalRotation -match '^free'){Adb @('shell','wm','user-rotation','free')|Out-Null}
 if($null -ne $originalFixed){Adb @('shell','wm','fixed-to-user-rotation',$originalFixed)|Out-Null}
 if($null -ne $originalAuto){Adb @('shell','settings','put','system','accelerometer_rotation',$originalAuto)|Out-Null}
 @{success=$success;app=$App;package=$package;apk=$ApkPath;sha256=$sha;serial=$Serial;api=$actualApi;qemuRequiredEveryAction=$true;checks=$script:checks;captures=$script:steps;pids=@($script:pids|Select-Object -Unique);observedStatus=$status;consentInteraction='none';bannerDeliveryVerified=$false;networkTrafficMeasured=$false;scope='Synthetic app data only. App-PID logs only. No physical device. Scan screenshot requires separate visual review.'} | ConvertTo-Json -Depth 8 | Set-Content -LiteralPath (Join-Path $OutputDirectory 'evidence.json')
}
