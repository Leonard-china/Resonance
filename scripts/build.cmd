@echo off
setlocal
set "GRADLE_USER_HOME=D:\Software\DevelopmentTools\GradleCache"
set "RESONANCE_FFMPEG=D:\Software\Github\FlyMouseFormat\release\exe\FlyingMouse Format\resources\ffmpeg\ffmpeg.exe"
set "RESONANCE_GRADLE=D:\Software\DevelopmentTools\Gradle\gradle-8.10.2\bin\gradle.bat"

if not exist "%RESONANCE_GRADLE%" (
  echo Gradle 8.10.2 was not found on D: drive: %RESONANCE_GRADLE%
  exit /b 1
)

call "%RESONANCE_GRADLE%" :composeApp:desktopTest :composeApp:assembleDebug :composeApp:createDistributable --no-configuration-cache
exit /b %ERRORLEVEL%
