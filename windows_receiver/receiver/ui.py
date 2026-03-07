from __future__ import annotations

import tkinter as tk
from tkinter import messagebox, ttk
from typing import Callable

from .controller import ReceiverController
from .driver import install_vbcable, is_vbcable_installed

try:
    import pystray
    from PIL import Image, ImageDraw

    TRAY_SUPPORTED = True
except Exception:
    pystray = None
    Image = None
    ImageDraw = None
    TRAY_SUPPORTED = False


class ReceiverUI:
    def __init__(
        self,
        root: tk.Tk,
        controller: ReceiverController,
        on_exit: Callable[[], None] | None = None,
    ) -> None:
        self.root = root
        self.controller = controller
        self._on_exit = on_exit

        self.root.title("FluxMic Receiver")
        self.root.geometry("900x620")

        self.server_var = tk.StringVar(value="Stopped")
        self.client_var = tk.StringVar(value="-")
        self.rtt_var = tk.StringVar(value="-")
        self.jitter_var = tk.StringVar(value="-")
        self.mute_var = tk.StringVar(value="False")
        self.actions_var = tk.StringVar(value="Enabled")
        self.codec_var = tk.StringVar(value="PCM16")
        self.driver_var = tk.StringVar(value="Unknown")
        self.device_var = tk.StringVar(value="")

        self._device_map: dict[str, int] = {}
        self._tray_icon = None

        self._build()
        self.refresh_devices()
        self.refresh_driver_status()
        self._startup_driver_check()
        self._tick()

        self.root.bind("<Unmap>", self._on_window_unmap)

    def _build(self) -> None:
        outer = ttk.Frame(self.root, padding=10)
        outer.pack(fill=tk.BOTH, expand=True)

        status = ttk.LabelFrame(outer, text="Connection", padding=10)
        status.pack(fill=tk.X)

        self._kv(status, "Server", self.server_var, 0, 0)
        self._kv(status, "Client IP", self.client_var, 0, 2)
        self._kv(status, "RTT", self.rtt_var, 1, 0)
        self._kv(status, "Jitter", self.jitter_var, 1, 2)
        self._kv(status, "Mute", self.mute_var, 2, 0)
        self._kv(status, "Actions", self.actions_var, 2, 2)
        self._kv(status, "Codec", self.codec_var, 3, 0)

        device = ttk.LabelFrame(outer, text="Audio Device / Driver", padding=10)
        device.pack(fill=tk.X, pady=8)

        ttk.Label(device, text="Driver:").grid(row=0, column=0, sticky="w")
        ttk.Label(device, textvariable=self.driver_var).grid(row=0, column=1, sticky="w", padx=(6, 18))

        ttk.Button(device, text="Install Driver", command=self.install_driver).grid(row=0, column=2, padx=4)
        ttk.Button(device, text="Refresh Devices", command=self.refresh_devices).grid(row=0, column=3, padx=4)

        ttk.Label(device, text="Output Device:").grid(row=1, column=0, sticky="w", pady=(8, 0))
        self.device_combo = ttk.Combobox(device, textvariable=self.device_var, state="readonly", width=60)
        self.device_combo.grid(row=1, column=1, columnspan=2, sticky="we", pady=(8, 0))
        ttk.Button(device, text="Apply", command=self.apply_device).grid(row=1, column=3, padx=4, pady=(8, 0))

        controls = ttk.Frame(outer)
        controls.pack(fill=tk.X, pady=8)

        self.server_btn = ttk.Button(controls, text="Start Server", command=self.toggle_server)
        self.server_btn.pack(side=tk.LEFT)

        self.action_btn = ttk.Button(controls, text="Disable Actions", command=self.toggle_actions)
        self.action_btn.pack(side=tk.LEFT, padx=8)

        self.mute_btn = ttk.Button(controls, text="Toggle Mute", command=self.toggle_mute)
        self.mute_btn.pack(side=tk.LEFT)

        self.tray_btn = ttk.Button(controls, text="Minimize to Tray", command=self.minimize_to_tray)
        self.tray_btn.pack(side=tk.LEFT, padx=8)
        if not TRAY_SUPPORTED:
            self.tray_btn.config(state=tk.DISABLED)

        logs = ttk.LabelFrame(outer, text="Recent Actions / Events", padding=10)
        logs.pack(fill=tk.BOTH, expand=True)

        self.log_list = tk.Listbox(logs, height=18)
        self.log_list.pack(fill=tk.BOTH, expand=True)

    def _kv(self, parent: ttk.Frame, key: str, var: tk.StringVar, row: int, col: int) -> None:
        ttk.Label(parent, text=f"{key}:").grid(row=row, column=col, sticky="w")
        ttk.Label(parent, textvariable=var).grid(row=row, column=col + 1, sticky="w", padx=(6, 20))

    def _startup_driver_check(self) -> None:
        if is_vbcable_installed():
            return
        path_hint = "assets/vbcable/VBCABLE_Setup_x64.exe"
        proceed = messagebox.askyesno(
            "VB-CABLE Not Found",
            "未检测到 VB-CABLE。\n\n"
            "这会导致无法把音频注入系统麦克风。\n"
            f"请先把离线安装器放到: {path_hint}\n\n"
            "是否现在一键安装（需要管理员权限/UAC）？",
        )
        if proceed:
            self.install_driver()

    def refresh_driver_status(self) -> None:
        installed = is_vbcable_installed()
        self.driver_var.set("Installed" if installed else "Missing")

    def refresh_devices(self) -> None:
        devices = self.controller.list_output_devices()
        names: list[str] = []
        self._device_map.clear()
        for idx, name in devices:
            label = f"[{idx}] {name}"
            names.append(label)
            self._device_map[label] = idx

        self.device_combo["values"] = names
        if names:
            current_name = self.controller.snapshot().selected_device
            selected = next((n for n in names if current_name in n), names[0])
            self.device_var.set(selected)
        self.refresh_driver_status()

    def apply_device(self) -> None:
        label = self.device_var.get()
        idx = self._device_map.get(label)
        self.controller.select_output_device(idx)
        self.controller.log(f"Selected output device: {label}")

    def install_driver(self) -> None:
        self.controller.log("Starting VB-CABLE installation flow")
        ok, msg = install_vbcable(log=self.controller.log)
        self.refresh_driver_status()
        self.refresh_devices()
        if ok:
            messagebox.showinfo("Driver", f"安装完成: {msg}")
        else:
            messagebox.showerror("Driver", msg)

    def toggle_server(self) -> None:
        snap = self.controller.snapshot()
        if snap.server_running:
            self.controller.stop_server()
            self.server_btn.config(text="Start Server")
        else:
            self.controller.start_server()
            self.server_btn.config(text="Stop Server")

    def toggle_actions(self) -> None:
        enabled = self.controller.toggle_actions()
        self.action_btn.config(text="Disable Actions" if enabled else "Enable Actions")

    def toggle_mute(self) -> None:
        snap = self.controller.snapshot()
        self.controller.set_mute(not snap.mute)

    def _tick(self) -> None:
        snap = self.controller.snapshot()

        self.server_var.set("Running" if snap.server_running else "Stopped")
        self.client_var.set(snap.client_ip if snap.connected else "-")
        self.rtt_var.set("-" if snap.rtt_ms is None else f"{snap.rtt_ms} ms")
        self.jitter_var.set(f"{snap.jitter_ms} ms")
        self.mute_var.set(str(snap.mute))
        self.actions_var.set("Enabled" if snap.actions_enabled else "Disabled")
        self.codec_var.set(snap.codec)

        logs = self.controller.recent_logs(limit=50)
        self.log_list.delete(0, tk.END)
        for line in logs:
            self.log_list.insert(tk.END, line)

        self.root.after(300, self._tick)

    def _on_window_unmap(self, _event: tk.Event) -> None:
        if self.root.state() == "iconic":
            self.minimize_to_tray()

    def minimize_to_tray(self) -> None:
        if not TRAY_SUPPORTED:
            messagebox.showwarning("Tray", "System tray is not available in this build.")
            return
        if self._tray_icon is not None:
            return

        self.root.withdraw()
        self._start_tray_icon()
        self.controller.log("Window minimized to tray")

    def restore_from_tray(self) -> None:
        self._stop_tray_icon()
        self.root.deiconify()
        self.root.state("normal")
        self.root.lift()
        self.root.focus_force()
        self.controller.log("Window restored from tray")

    def tray_exit(self) -> None:
        self._stop_tray_icon()
        if self._on_exit:
            self._on_exit()
        else:
            self.root.destroy()

    def dispose(self) -> None:
        self._stop_tray_icon()

    def _start_tray_icon(self) -> None:
        image = self._create_tray_image()
        menu = pystray.Menu(
            pystray.MenuItem("Open", self._tray_menu_open),
            pystray.MenuItem("Exit", self._tray_menu_exit),
        )
        self._tray_icon = pystray.Icon("fluxmic_receiver", image, "FluxMic Receiver", menu)
        self._tray_icon.run_detached()

    def _stop_tray_icon(self) -> None:
        if self._tray_icon is not None:
            self._tray_icon.stop()
            self._tray_icon = None

    def _tray_menu_open(self, _icon, _item) -> None:
        self.root.after(0, self.restore_from_tray)

    def _tray_menu_exit(self, _icon, _item) -> None:
        self.root.after(0, self.tray_exit)

    def _create_tray_image(self):
        size = 64
        image = Image.new("RGB", (size, size), (16, 24, 32))
        draw = ImageDraw.Draw(image)
        draw.rectangle((8, 8, 56, 56), fill=(41, 161, 156))
        draw.rectangle((18, 18, 46, 46), fill=(16, 24, 32))
        draw.rectangle((23, 26, 41, 30), fill=(248, 249, 250))
        draw.rectangle((23, 34, 35, 38), fill=(248, 249, 250))
        return image
