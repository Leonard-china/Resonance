$ErrorActionPreference = "Stop"

$env:GRADLE_USER_HOME = "D:\Software\DevelopmentTools\GradleCache"
$env:RESONANCE_FFMPEG = "D:\Software\Github\FlyMouseFormat\release\exe\FlyingMouse Format\resources\ffmpeg\ffmpeg.exe"
$gradle = "D:\Software\DevelopmentTools\Gradle\gradle-8.10.2\bin\gradle.bat"

if (-not (Test-Path -LiteralPath $gradle)) {
    throw "Gradle 8.10.2 not found at $gradle. Keep the Gradle distribution on D: to avoid C: drive downloads."
}

& $gradle :composeApp:desktopTest :composeApp:assembleDebug :composeApp:createDistributable --no-configuration-cache
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
