from __future__ import annotations

import sys
from pathlib import Path


def repo_root() -> Path:
    if getattr(sys, "frozen", False):
        return Path(getattr(sys, "_MEIPASS"))
    return Path(__file__).resolve().parents[2]


def assets_vbcable_dir() -> Path:
    if getattr(sys, "frozen", False):
        return repo_root() / "assets" / "vbcable"
    return repo_root() / "assets" / "vbcable"


def windows_receiver_root() -> Path:
    if getattr(sys, "frozen", False):
        return Path.cwd()
    return Path(__file__).resolve().parents[1]
