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

    $prevEAP = $ErrorActionPreference
    $ErrorActionPreference = "SilentlyContinue"
    try {
        foreach ($candidate in $candidates) {
            $resolvedCandidates = if ($candidate.Contains('*')) {
                Get-Item -Path $candidate -ErrorAction SilentlyContinue
            } else {
                Get-Item -LiteralPath $candidate -ErrorAction SilentlyContinue
            }
            foreach ($resolved in $resolvedCandidates) {
                $java = Join-Path $resolved.FullName "bin\java.exe"
                if (-not (Test-Path -LiteralPath $java)) { continue }
                $versionInfo = Start-Process -FilePath $java -ArgumentList "-version" -NoNewWindow -Wait -PassThru -RedirectStandardError "$env:TEMP\java_ver.txt"
                if (Test-Path "$env:TEMP\java_ver.txt") {
                    $versionLine = Get-Content "$env:TEMP\java_ver.txt" | Select-Object -First 1
                    Remove-Item "$env:TEMP\java_ver.txt" -ErrorAction SilentlyContinue
                    if ($versionLine -match 'version "(\d+)') {
                        $major = [int]$Matches[1]
                        if ($major -ge 17 -and $major -le 21) { return $resolved.FullName }
                    }
                }
            }
        }
    } finally {
        $ErrorActionPreference = $prevEAP
    }
    throw "Resonance requires JDK 17-21."
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
    throw "Gradle not found."
}

& $gradle :composeApp:allTests :composeApp:assembleRelease :composeApp:packageReleaseExe --no-configuration-cache
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

