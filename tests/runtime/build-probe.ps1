param(
    [Parameter(Mandatory=$true)][string]$SdkPath,
    [string]$JavaHome = $env:JAVA_HOME
)
$ErrorActionPreference='Stop'
Set-StrictMode -Version Latest
if (!$JavaHome) { throw 'Supply -JavaHome or JAVA_HOME for a JDK.' }
$repo=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..\..'))
$build=Join-Path $repo 'build\lifecycle-probe'
$sdk=(Resolve-Path -LiteralPath $SdkPath).Path
$bt=Join-Path $sdk 'build-tools\36.0.0'
$sdkJar=Join-Path $sdk 'platforms\android-36\android.jar'
$androidJar=Join-Path $build 'android.jar'
$javaBin=Join-Path $JavaHome 'bin'
function Run([string]$File,[string[]]$Arguments) {
    & $File @Arguments
    if($LASTEXITCODE -ne 0){throw "$File exited $LASTEXITCODE"}
}
foreach($part in @('classes','dex')) {
    $target=[IO.Path]::GetFullPath((Join-Path $build $part))
    if(!$target.StartsWith($build+[IO.Path]::DirectorySeparatorChar)){throw 'Invalid build cleanup path'}
    if(Test-Path -LiteralPath $target){Remove-Item -LiteralPath $target -Recurse -Force}
    New-Item -ItemType Directory -Force -Path $target | Out-Null
}
Copy-Item -LiteralPath $sdkJar -Destination $androidJar -Force
Run (Join-Path $bt 'aapt2.exe') @('link','-o',"$build\unsigned.apk",'--manifest',"$PSScriptRoot\AndroidManifest.xml",'-I',$androidJar)
Run (Join-Path $javaBin 'javac.exe') @('-encoding','UTF-8','--release','8','-classpath',$androidJar,'-d',"$build\classes","$PSScriptRoot\UiProbe.java")
Run (Join-Path $javaBin 'jar.exe') @('--create','--file',"$build\classes.jar",'-C',"$build\classes",'.')
Run (Join-Path $javaBin 'java.exe') @('-cp',"$bt\lib\d8.jar",'com.android.tools.r8.D8','--release','--min-api','26','--lib',$androidJar,'--output',"$build\dex","$build\classes.jar")
Add-Type -AssemblyName System.IO.Compression
Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip=[IO.Compression.ZipFile]::Open("$build\unsigned.apk",[IO.Compression.ZipArchiveMode]::Update)
try{[IO.Compression.ZipFileExtensions]::CreateEntryFromFile($zip,"$build\dex\classes.dex",'classes.dex')|Out-Null}finally{$zip.Dispose()}
Run (Join-Path $bt 'zipalign.exe') @('-f','-p','4',"$build\unsigned.apk","$build\aligned.apk")
$key=Join-Path $build 'probe.keystore'
if(!(Test-Path -LiteralPath $key)) {
    Run (Join-Path $javaBin 'keytool.exe') @('-genkeypair','-keystore',$key,'-storepass','android','-keypass','android','-alias','androiddebugkey','-dname','CN=Emulator QA','-keyalg','RSA','-keysize','2048','-validity','3650','-noprompt')
}
Run (Join-Path $javaBin 'java.exe') @('-jar',"$bt\lib\apksigner.jar",'sign','--ks',$key,'--ks-pass','pass:android','--out',"$build\probe.apk","$build\aligned.apk")
Run (Join-Path $javaBin 'java.exe') @('-jar',"$bt\lib\apksigner.jar",'verify',"$build\probe.apk")
Write-Output "Probe built: $build\probe.apk"
