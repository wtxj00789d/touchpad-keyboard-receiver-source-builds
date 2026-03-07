param(
  [switch]$NoVenv
)

$ErrorActionPreference = "Stop"

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$receiverRoot = Resolve-Path (Join-Path $scriptDir "..")
$repoRoot = Resolve-Path (Join-Path $receiverRoot "..")
$venv = Join-Path $receiverRoot ".venv"
$assets = Join-Path $repoRoot "assets\vbcable"

Set-Location $receiverRoot

if (-not $NoVenv) {
  if (-not (Test-Path $venv)) {
    if (Get-Command py -ErrorAction SilentlyContinue) {
      try {
        py -3.11 -m venv $venv
      } catch {
        try {
          py -3 -m venv $venv
        } catch {
          python -m venv $venv
        }
      }
    } else {
      python -m venv $venv
    }
  }
  & "$venv\Scripts\python.exe" -m pip install --upgrade pip
  & "$venv\Scripts\python.exe" -m pip install -r requirements.txt
  $python = "$venv\Scripts\python.exe"
} else {
  $python = "python"
}

$addData = ""
if (Test-Path $assets) {
  $addData = "--add-data `"$assets;assets/vbcable`""
}

$cmd = "$python -m PyInstaller --noconfirm --clean --onefile --windowed --name Receiver --hidden-import pystray._win32 receiver/main.py $addData"
Write-Host "Running: $cmd"
Invoke-Expression $cmd

Write-Host "Done. Output: $receiverRoot\dist\Receiver.exe"
