$ErrorActionPreference = "Stop"

$repoRoot = Resolve-Path (Join-Path $PSScriptRoot "..")
$projectDir = Join-Path $repoRoot "ReceiverWinUI"
$projectFile = Join-Path $projectDir "ReceiverWinUI.csproj"
$tempPublishDir = Join-Path $projectDir "bin\publish\winui"
$distDir = Join-Path $repoRoot "dist"

Write-Host "Publishing ReceiverWinUI project..."
if (Test-Path $tempPublishDir) {
    Remove-Item -Recurse -Force $tempPublishDir
}
New-Item -ItemType Directory -Force $tempPublishDir | Out-Null
New-Item -ItemType Directory -Force $distDir | Out-Null

dotnet publish $projectFile `
    -c Release `
    -r win-x64 `
    --self-contained true `
    /p:PublishSingleFile=true `
    /p:IncludeNativeLibrariesForSelfExtract=true `
    /p:IncludeAllContentForSelfExtract=true `
    /p:PublishTrimmed=false `
    -o $tempPublishDir

$sourceExe = Join-Path $tempPublishDir "ReceiverWinUI.exe"
if (-not (Test-Path $sourceExe)) {
    throw "Publish output missing: $sourceExe"
}

$targetExe = Join-Path $distDir "Receiver_WinUI.exe"
try {
    Copy-Item -Force $sourceExe $targetExe
} catch {
    throw "Failed to write $targetExe . Close running Receiver_WinUI.exe and rebuild."
}

$assetsSource = Join-Path $tempPublishDir "assets"
if (Test-Path $assetsSource) {
    $assetsTarget = Join-Path $distDir "assets"
    if (Test-Path $assetsTarget) {
        Remove-Item -Recurse -Force $assetsTarget
    }
    Copy-Item -Recurse -Force $assetsSource $assetsTarget
}

Write-Host "WinUI build done: $targetExe"
