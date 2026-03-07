from __future__ import annotations

import shutil
import subprocess
import tempfile
import time
import zipfile
from pathlib import Path
from typing import Callable

import sounddevice as sd

from .resources import assets_vbcable_dir

LogFn = Callable[[str], None]


def list_output_device_names() -> list[str]:
    names: list[str] = []
    for d in sd.query_devices():
        if d.get("max_output_channels", 0) > 0:
            names.append(str(d.get("name", "")))
    return names


def find_vbcable_output_device() -> str | None:
    # VB-CABLE playback endpoint usually named "CABLE Input"
    for name in list_output_device_names():
        if "CABLE Input" in name:
            return name
    return None


def is_vbcable_installed() -> bool:
    return find_vbcable_output_device() is not None


def _find_installer_candidates(root: Path) -> list[Path]:
    if not root.exists():
        return []
    candidates = list(root.glob("VBCABLE_Setup_x64.exe"))
    candidates += list(root.glob("VBCABLE_Driver_Pack*.zip"))
    candidates += list(root.glob("*.exe"))
    candidates += list(root.glob("*.zip"))
    return candidates


def _extract_zip_to_temp(zip_path: Path, temp_dir: Path) -> Path | None:
    with zipfile.ZipFile(zip_path, "r") as zf:
        zf.extractall(temp_dir)
    for exe in temp_dir.rglob("*Setup*x64*.exe"):
        return exe
    for exe in temp_dir.rglob("*.exe"):
        if "x64" in exe.name.lower():
            return exe
    return None


def _run_elevated_installer(exe_path: Path) -> tuple[bool, str]:
    # Use PowerShell Start-Process -Verb RunAs to trigger UAC.
    escaped = str(exe_path).replace("'", "''")
    cmd = [
        "powershell",
        "-NoProfile",
        "-ExecutionPolicy",
        "Bypass",
        "-Command",
        f"Start-Process -FilePath '{escaped}' -Verb RunAs -Wait",
    ]
    result = subprocess.run(cmd, capture_output=True, text=True)
    if result.returncode != 0:
        message = result.stderr.strip() or result.stdout.strip() or f"exit={result.returncode}"
        return False, message
    return True, "Installer finished"


def install_vbcable(log: LogFn | None = None) -> tuple[bool, str]:
    logger = log or (lambda _: None)
    root = assets_vbcable_dir()
    candidates = _find_installer_candidates(root)
    if not candidates:
        return (
            False,
            f"No VB-CABLE installer found in {root}. Place VBCABLE_Setup_x64.exe or VBCABLE_Driver_Pack*.zip",
        )

    logger(f"Installer candidates: {', '.join(str(c.name) for c in candidates)}")
    temp_dir = Path(tempfile.mkdtemp(prefix="fluxmic_vbcable_"))
    try:
        installer: Path | None = None
        first = candidates[0]
        if first.suffix.lower() == ".zip":
            logger(f"Extracting zip: {first.name}")
            installer = _extract_zip_to_temp(first, temp_dir)
        else:
            installer = temp_dir / first.name
            shutil.copy2(first, installer)

        if installer is None or not installer.exists():
            return False, "Could not locate x64 installer executable in provided package"

        logger(f"Running installer with UAC: {installer}")
        ok, msg = _run_elevated_installer(installer)
        if not ok:
            return False, f"Installer failed: {msg}"

        # Device enumeration may lag a bit.
        for _ in range(8):
            if is_vbcable_installed():
                return True, "VB-CABLE detected"
            time.sleep(1.0)

        return False, "Installer completed but VB-CABLE still not detected. Try rebooting Windows."
    finally:
        shutil.rmtree(temp_dir, ignore_errors=True)
