$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent $PSScriptRoot
Set-Location -LiteralPath $projectRoot

function Get-CompatibleJavaHome {
    $candidates = @(
        $env:JAVA_HOME,
        "D:\DevelopmentEnvironmentTools\Java\JDK17",
        "C:\Program Files\Eclipse Adoptium\jdk-21*",
        "C:\Program Files\Eclipse Adoptium\jdk-17*"
    ) | Where-Object { -not [string]::IsNullOrWhiteSpace($_) }

    foreach ($candidate in $candidates) {
        $resolvedCandidates = if ($candidate.Contains('*')) {
            Get-Item -Path $candidate -ErrorAction SilentlyContinue
        } else {
            Get-Item -LiteralPath $candidate -ErrorAction SilentlyContinue
        }
        foreach ($resolved in $resolvedCandidates) {
            $java = Join-Path $resolved.FullName "bin\java.exe"
            if (-not (Test-Path -LiteralPath $java)) { continue }
            $versionLine = (& $java -version 2>&1 | Select-Object -First 1).ToString()
            if ($versionLine -match 'version "(\d+)') {
                $major = [int]$Matches[1]
                if ($major -ge 17 -and $major -le 21) { return $resolved.FullName }
            }
        }
    }
    throw "Resonance 需要 JDK 17-21。当前 JAVA_HOME 无效或版本不兼容；JDK 25 不能用于 Gradle 8.10.2。"
}

$env:JAVA_HOME = Get-CompatibleJavaHome
if (-not $env:GRADLE_USER_HOME -and (Test-Path -LiteralPath "D:\Software\DevelopmentTools\GradleCache")) {
    $env:GRADLE_USER_HOME = "D:\Software\DevelopmentTools\GradleCache"
}
if (-not $env:RESONANCE_FFMPEG -and (Test-Path -LiteralPath "D:\Software\Github\FlyMouseFormat\release\exe\FlyingMouse Format\resources\ffmpeg\ffmpeg.exe")) {
    $env:RESONANCE_FFMPEG = "D:\Software\Github\FlyMouseFormat\release\exe\FlyingMouse Format\resources\ffmpeg\ffmpeg.exe"
}

$installedGradle = "D:\Software\DevelopmentTools\Gradle\gradle-8.10.2\bin\gradle.bat"
$gradle = if (Test-Path -LiteralPath $installedGradle) { $installedGradle } else { Join-Path $projectRoot "gradlew.bat" }

if (-not (Test-Path -LiteralPath $gradle)) {
    throw "找不到 Gradle 8.10.2 或项目 Gradle Wrapper。"
}

& $gradle :composeApp:allTests :composeApp:assembleRelease :composeApp:packageReleaseExe --no-configuration-cache
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
