# Windows Receiver

运行：

1. `py -3.11 -m venv .venv`
2. `.\.venv\Scripts\Activate.ps1`
3. `pip install -r requirements.txt`
4. `python -m receiver.main`

打包：

- PowerShell: `scripts/build.ps1`
- CMD: `scripts/build.bat`

产物：`dist/Receiver.exe`

托盘功能：

- 点击窗口最小化按钮会自动最小化到系统托盘
- 也可点击 UI 按钮 `Minimize to Tray`
- 托盘右键菜单支持 `Open`（恢复窗口）和 `Exit`（退出程序）

## 驱动离线包

Receiver 启动时会检查 VB-CABLE。若缺失会弹窗提示安装。

离线安装器请放在仓库：

- `../assets/vbcable/VBCABLE_Setup_x64.exe`

也支持：

- `../assets/vbcable/VBCABLE_Driver_Pack*.zip`

打包时会把 `assets/vbcable` 一起打进 `Receiver.exe`（若目录存在文件）。
