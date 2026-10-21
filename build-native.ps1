param(
    [Parameter(Mandatory=$true)][string]$SdkPath,
    [string]$JavaHome = $env:JAVA_HOME,
    [string]$OutputDirectory,
    [string]$KeyStorePath
)
$ErrorActionPreference='Stop'; Set-StrictMode -Version Latest
$projectRoot = $PSScriptRoot
if (!$OutputDirectory) { $OutputDirectory = Join-Path $projectRoot 'build\outputs' }
if (!$KeyStorePath) { $KeyStorePath = Join-Path $projectRoot 'debug.keystore' }
if (!$JavaHome) { $JavaHome = Split-Path (Split-Path (Get-Command javac).Source) }
$OutputDirectory = [System.IO.Path]::GetFullPath($OutputDirectory)
$KeyStorePath = [System.IO.Path]::GetFullPath($KeyStorePath)
$sdk=(Resolve-Path -LiteralPath $SdkPath).Path; $javaBin=Join-Path $JavaHome 'bin'; $bt=Join-Path $sdk 'build-tools\36.0.0'; $androidJar=Join-Path $sdk 'platforms\android-36\android.jar'; $main=Join-Path $PSScriptRoot 'app\src\main'; $build=Join-Path $PSScriptRoot 'build'; $jar=Join-Path $build 'android.jar'
foreach($path in @($androidJar,(Join-Path $bt 'aapt2.exe'),(Join-Path $bt 'zipalign.exe'),(Join-Path $bt 'lib\d8.jar'),(Join-Path $bt 'lib\apksigner.jar'),(Join-Path $javaBin 'javac.exe'))){if(!(Test-Path -LiteralPath $path)){throw "Werkzeug fehlt: $path"}}
foreach ($part in @('classes','test-classes','generated','dex')) {
    $target = [System.IO.Path]::GetFullPath((Join-Path $build $part))
    if (!$target.StartsWith([System.IO.Path]::GetFullPath($build) + [System.IO.Path]::DirectorySeparatorChar)) { throw 'Invalid build cleanup path' }
    if (Test-Path -LiteralPath $target) { Remove-Item -LiteralPath $target -Recurse -Force }
    New-Item -ItemType Directory -Force -Path $target | Out-Null
}
foreach($path in @($build,$OutputDirectory,(Split-Path $KeyStorePath))){New-Item -ItemType Directory -Force -Path $path | Out-Null}
Copy-Item -LiteralPath $androidJar -Destination $jar -Force
function Run([string]$file,[string[]]$arguments){& $file @arguments;if($LASTEXITCODE -ne 0){throw "$file beendet mit $LASTEXITCODE"}}
Run (Join-Path $javaBin 'javac.exe') @('-encoding','UTF-8','--release','8','-d',(Join-Path $build 'test-classes'),(Join-Path $main 'java\com\maximumspyware\parody\FakeScanState.java'),(Join-Path $PSScriptRoot 'tests\ScanScriptTest.java'))
Run (Join-Path $javaBin 'java.exe') @('-cp',(Join-Path $build 'test-classes'),'com.maximumspyware.parody.ScanScriptTest')
Run (Join-Path $bt 'aapt2.exe') @('compile','--dir',(Join-Path $main 'res'),'-o',(Join-Path $build 'resources.zip'))
Run (Join-Path $bt 'aapt2.exe') @('link','-o',(Join-Path $build 'unsigned.apk'),'--manifest',(Join-Path $main 'AndroidManifest.xml'),'-I',$jar,'--java',(Join-Path $build 'generated'),'--min-sdk-version','26','--target-sdk-version','36',(Join-Path $build 'resources.zip'))
$sources=@(Get-ChildItem -LiteralPath (Join-Path $main 'java') -Recurse -Filter '*.java' | ForEach-Object FullName); $sources += @(Get-ChildItem -LiteralPath (Join-Path $build 'generated') -Recurse -Filter '*.java' | ForEach-Object FullName)
$sources += @(Get-ChildItem -LiteralPath (Join-Path $projectRoot 'app/src/offline/java') -Recurse -Filter '*.java' | ForEach-Object FullName)
Run (Join-Path $javaBin 'javac.exe') (@('-encoding','UTF-8','--release','8','-classpath',$jar,'-d',(Join-Path $build 'classes'))+$sources)
Run (Join-Path $javaBin 'jar.exe') @('--create','--file',(Join-Path $build 'classes.jar'),'-C',(Join-Path $build 'classes'),'.')
Run (Join-Path $javaBin 'java.exe') @('-cp',(Join-Path $bt 'lib\d8.jar'),'com.android.tools.r8.D8','--release','--min-api','26','--lib',$jar,'--output',(Join-Path $build 'dex'),(Join-Path $build 'classes.jar'))
Add-Type -AssemblyName System.IO.Compression; Add-Type -AssemblyName System.IO.Compression.FileSystem; $zip=[System.IO.Compression.ZipFile]::Open((Join-Path $build 'unsigned.apk'),[System.IO.Compression.ZipArchiveMode]::Update); try {foreach($dex in Get-ChildItem -LiteralPath (Join-Path $build 'dex') -Filter '*.dex'){[System.IO.Compression.ZipFileExtensions]::CreateEntryFromFile($zip,$dex.FullName,$dex.Name,[System.IO.Compression.CompressionLevel]::Optimal)|Out-Null}} finally {$zip.Dispose()}
Run (Join-Path $bt 'zipalign.exe') @('-f','-p','4',(Join-Path $build 'unsigned.apk'),(Join-Path $build 'aligned.apk'))
if(!(Test-Path -LiteralPath $KeyStorePath)){Run (Join-Path $javaBin 'keytool.exe') @('-genkeypair','-keystore',$KeyStorePath,'-storepass','android','-keypass','android','-alias','androiddebugkey','-dname','CN=Maximum Spyware Development,O=Leterel,C=DE','-keyalg','RSA','-keysize','2048','-validity','10000','-noprompt')}
$apk=Join-Path $OutputDirectory 'maximum-spyware-0.2.2.apk'; Run (Join-Path $javaBin 'java.exe') @('-jar',(Join-Path $bt 'lib\apksigner.jar'),'sign','--ks',$KeyStorePath,'--ks-pass','pass:android','--key-pass','pass:android','--ks-key-alias','androiddebugkey','--out',$apk,(Join-Path $build 'aligned.apk')); Run (Join-Path $javaBin 'java.exe') @('-jar',(Join-Path $bt 'lib\apksigner.jar'),'verify','--verbose',$apk); Run (Join-Path $bt 'zipalign.exe') @('-c','-v','4',$apk); Get-FileHash -LiteralPath $apk -Algorithm SHA256 | Format-List; Write-Output "Built APK: $apk"
