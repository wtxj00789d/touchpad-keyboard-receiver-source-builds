# Receiver WinUI (WPF)

WinUI/WPF 风格的 Windows 接收端，功能与旧版 Python `Receiver.exe` 并行存在，不会覆盖旧产物。

## 运行源码

1. 安装 .NET 8 SDK
2. 进入 `windows_receiver_winui/ReceiverWinUI`
3. 执行：
   - `dotnet run -c Release`

## 打包

- PowerShell: `windows_receiver_winui/scripts/build.ps1`
- CMD: `windows_receiver_winui/scripts/build.bat`

产物：

- `windows_receiver_winui/dist/Receiver_WinUI.exe`

说明：

- 新产物文件名固定为 `Receiver_WinUI.exe`
- 旧版 Python 产物 `windows_receiver/dist/Receiver.exe` 保留，不会被替换

## 驱动离线包

启动时会自动检测 VB-CABLE。若缺失会弹窗提示安装。

离线安装包可放置在以下路径之一：

- `assets/vbcable/VBCABLE_Setup_x64.exe`
- `assets/vbcable/VBCABLE_Driver_Pack*.zip`

若未找到离线包，程序会提示缺失路径。

## 托盘功能

- 点击窗口最小化按钮会自动最小化到系统托盘
- UI 顶部 `Minimize To Tray` 按钮可手动最小化到托盘
- 托盘菜单支持 `Open` / `Exit`
