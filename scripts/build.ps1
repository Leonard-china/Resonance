param(
    [switch]$NoBump,
    [string]$SetVersion
)

$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent $PSScriptRoot
Set-Location -LiteralPath $projectRoot

# ---- 1. 版本号自增逻辑（每次构建均自动变更版本号）----
$versionFile = Join-Path $projectRoot "version.properties"
$currentVersion = "0.2.0"
$currentCode = 8

if (Test-Path -LiteralPath $versionFile) {
    $propsContent = Get-Content -LiteralPath $versionFile -Encoding UTF8
    foreach ($line in $propsContent) {
        if ($line -match '^\s*versionName\s*=\s*(.+)$') { $currentVersion = $Matches[1].Trim() }
        if ($line -match '^\s*versionCode\s*=\s*(\d+)\s*$') { $currentCode = [int]$Matches[1] }
    }
}

$newVersion = $currentVersion
$newCode = $currentCode

if ($SetVersion) {
    $newVersion = $SetVersion.Trim()
    $newCode = $currentCode + 1
} elseif (-not $NoBump) {
    $parts = $currentVersion.Split('.')
    if ($parts.Length -ge 3 -and [int]::TryParse($parts[2], [ref]$null)) {
        $patch = [int]$parts[2] + 1
        $newVersion = "$($parts[0]).$($parts[1]).$patch"
    } else {
        $newVersion = "$currentVersion.1"
    }
    $newCode = $currentCode + 1
}

if ($newVersion -ne $currentVersion -or $newCode -ne $currentCode) {
    Write-Host ">>> 自动递增版本: v$currentVersion (code $currentCode) -> v$newVersion (code $newCode)" -ForegroundColor Cyan
    $versionContent = "versionName=$newVersion`nversionCode=$newCode`n"
    Set-Content -LiteralPath $versionFile -Value $versionContent -Encoding UTF8

    $modelFile = Join-Path $projectRoot "composeApp\src\commonMain\kotlin\com\resonance\player\model\LibraryModels.kt"
    if (Test-Path -LiteralPath $modelFile) {
        $content = Get-Content -LiteralPath $modelFile -Raw -Encoding UTF8
        $updated = $content -replace 'const val APP_VERSION = "[^"]*"', "const val APP_VERSION = `"$newVersion`""
        Set-Content -LiteralPath $modelFile -Value $updated -Encoding UTF8 -NoNewline
    }
} else {
    Write-Host ">>> 当前构建版本: v$currentVersion (code $currentCode)" -ForegroundColor Cyan
}

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

Write-Host "`n================ 构建产物汇总 (v$newVersion) ================" -ForegroundColor Green

$apkPath = Join-Path $projectRoot "composeApp\build\outputs\apk\release\Resonance-$newVersion-release.apk"
if (Test-Path -LiteralPath $apkPath) {
    $apkItem = Get-Item -LiteralPath $apkPath
    $apkHash = (Get-FileHash -Algorithm SHA256 -LiteralPath $apkPath).Hash
    Write-Host "Android APK  : $($apkItem.FullName)" -ForegroundColor Yellow
    Write-Host "文件大小     : $('{0:N2} MB' -f ($apkItem.Length / 1MB)) ($($apkItem.Length) 字节)"
    Write-Host "SHA256       : $apkHash"
}

$exePath = Join-Path $projectRoot "composeApp\build\compose\binaries\main-release\exe\Resonance-$newVersion.exe"
if (Test-Path -LiteralPath $exePath) {
    $exeItem = Get-Item -LiteralPath $exePath
    $exeHash = (Get-FileHash -Algorithm SHA256 -LiteralPath $exePath).Hash
    Write-Host "Windows EXE  : $($exeItem.FullName)" -ForegroundColor Yellow
    Write-Host "文件大小     : $('{0:N2} MB' -f ($exeItem.Length / 1MB)) ($($exeItem.Length) 字节)"
    Write-Host "SHA256       : $exeHash"
}
Write-Host "========================================================`n" -ForegroundColor Green


