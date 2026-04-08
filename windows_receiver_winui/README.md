# Receiver WinUI 3

基于 **WinUI 3** 的 Windows 接收端，功能与旧版 Python `Receiver.exe` 并行存在，不会覆盖旧产物。

## 运行源码

1. 安装 .NET 8 SDK
2. 进入 `windows_receiver_winui/ReceiverWinUI`
3. 执行:
   - `dotnet run -c Release`

## 打包

- PowerShell: `windows_receiver_winui/scripts/build.ps1`
- CMD: `windows_receiver_winui/scripts/build.bat`

产物:

- `windows_receiver_winui/dist/Receiver_WinUI.exe`

说明:

- 新产物文件名固定为 `Receiver_WinUI.exe`
- 旧版 Python 产物 `windows_receiver/dist/Receiver.exe` 保留，不会被替换

## 驱动离线包

启动时会自动检测 VB-CABLE。若缺失会弹窗提示安装。

离线安装包可放置在以下路径之一:

- `assets/vbcable/VBCABLE_Setup_x64.exe`
- `assets/vbcable/VBCABLE_Driver_Pack*.zip`

若未找到离线包，程序会提示缺失路径。

## 窗口功能

- UI 顶部 `Minimize` 按钮可最小化窗口

## USB Connect

当前 WinUI Receiver 启动后会默认自动启动 WebSocket server，并在 Overview 页面直接显示：

- `Server`
- `Server URL`
- `Server Error`
- `USB Status`
- `USB Message`

如果 Android 平板已经打开 FluxMic App，且 USB 线已连接并完成 `USB 调试` 授权：

1. 打开 Receiver
2. 确认 `Server = Running`
3. 点击 `USB Connect`

程序会自动：

1. 确认 server 已运行
2. 执行 `adb reverse tcp:8765 tcp:8765`
3. 向 Android 发送 `com.example.fluxmic.action.USB_CONNECT`

成功后，Android 会自动切换到 `127.0.0.1:8765` 并发起连接，不需要再手动输入地址或再点 Connect。
