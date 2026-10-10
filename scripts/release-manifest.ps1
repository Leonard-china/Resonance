param([string]$Version)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
if (-not $Version) {
    $versionLine = Get-Content -LiteralPath (Join-Path $projectRoot 'version.properties') -Encoding UTF8 | Where-Object { $_ -match '^versionName=' }
    $Version = $versionLine.Split('=', 2)[1].Trim()
}
$apkPath = Join-Path $projectRoot "composeApp/build/outputs/apk/release/Resonance-$Version-release.apk"
if (-not (Test-Path -LiteralPath $apkPath)) { throw "Release APK 不存在：$apkPath" }
$releaseDirectory = Join-Path $projectRoot "release/$Version"
New-Item -ItemType Directory -Path $releaseDirectory -Force | Out-Null
$files = @($apkPath)
$exePath = Join-Path $projectRoot "composeApp/build/compose/binaries/main-release/exe/Resonance-$Version.exe"
if (Test-Path -LiteralPath $exePath) { $files += $exePath }
$checksums = foreach ($file in $files) {
    $hash = (Get-FileHash -Algorithm SHA256 -LiteralPath $file).Hash.ToLowerInvariant()
    $name = Split-Path -Leaf $file
    Copy-Item -LiteralPath $file -Destination (Join-Path $releaseDirectory $name) -Force
    "$hash  $name"
}
$manifestPath = Join-Path $releaseDirectory 'SHA256SUMS.txt'
[System.IO.File]::WriteAllLines($manifestPath, $checksums, [System.Text.UTF8Encoding]::new($false))
$notesPath = Join-Path $projectRoot "docs/RELEASE_NOTES-$Version.md"
if (-not (Test-Path -LiteralPath $notesPath)) { throw "版本说明不存在：$notesPath" }
$notes = Get-Content -Raw -LiteralPath $notesPath -Encoding UTF8
$marker = '<!-- GENERATED-CHECKSUMS -->'
$endMarker = '<!-- END-GENERATED-CHECKSUMS -->'
$rows = foreach ($file in $files) {
    $item = Get-Item -LiteralPath $file
    "| ``$($item.Name)`` | $('{0:N2} MiB' -f ($item.Length / 1MB)) | ``$((Get-FileHash -Algorithm SHA256 -LiteralPath $file).Hash.ToLowerInvariant())`` |"
}
$section = "$marker`n| 文件 | 大小 | SHA-256 |`n| --- | --- | --- |`n$($rows -join "`n")`n$endMarker"
if ($notes.Contains($marker)) {
    $start = $notes.IndexOf($marker)
    $end = $notes.IndexOf($endMarker) + $endMarker.Length
    $notes = $notes.Substring(0, $start) + $section + $notes.Substring($end)
} else { $notes = $notes.TrimEnd() + "`n`n$section`n" }
[System.IO.File]::WriteAllText($notesPath, $notes, [System.Text.UTF8Encoding]::new($false))
Write-Output "Release artifacts: $releaseDirectory"
Write-Output "Checksums: $manifestPath"
