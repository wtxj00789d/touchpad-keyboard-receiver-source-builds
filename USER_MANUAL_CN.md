# FluxMic 使用说明书（中文）

本文档面向最终使用者，目标是把 Android 平板作为“键盘面板 + 无线麦克风”接到 Windows 11。

## 1. 你会得到什么

- Android 端：全屏按键面板 + 麦克风推流
- Windows 端：`Receiver.exe` 接收音频和按键动作
- 音频链路：Android -> Windows `CABLE Input` -> 其它软件选择 `CABLE Output` 作为麦克风

## 2. 准备条件

1. Windows 11 电脑一台
2. Android 平板一台（已授权麦克风权限）
3. 同一局域网 Wi-Fi，或可用 USB + ADB reverse
4. VB-CABLE 离线安装包（如仓库未附带）

离线包放置路径：

- `assets/vbcable/VBCABLE_Setup_x64.exe`
- `assets/vbcable/VBCABLE_Driver_Pack*.zip`

## 3. 直接使用（不改代码）

### 3.1 Windows 端

1. 打开 `windows_receiver/dist/Receiver.exe`
2. 首次启动若提示未检测到 VB-CABLE，点击安装（会触发 UAC）
3. 在 Receiver 界面确认：
   - `Server = Running`
   - 输出设备选择 `CABLE Input (VB-Audio Virtual Cable)`
   - `Actions = Enabled`（按需开关）

### 3.2 Android 端

1. 安装 APK：`android_app/app/build/outputs/apk/release/app-release-signed.apk`
2. 启动 App，授权麦克风权限
3. 在顶部输入连接地址并点击 Connect

## 4. 连接方式

### 4.1 Wi-Fi 模式

1. 在 Windows 查看本机 IP（如 `192.168.1.10`）
2. 防火墙放行 TCP `8765`
3. Android 填写：`ws://192.168.1.10:8765`

### 4.2 ADB reverse 模式

1. 电脑执行：`adb devices`
2. 电脑执行：`adb reverse tcp:8765 tcp:8765`
3. Android 填写：`ws://127.0.0.1:8765`

### 4.3 USB Connect 一键有线模式

前提：

1. Android 平板已经打开 FluxMic App
2. 平板已开启 `USB 调试`，并已授权这台电脑
3. USB 数据线已连接

步骤：

1. 打开 `windows_receiver_winui/dist/Receiver_WinUI.exe`
2. 在 Overview 页面确认：
   - `Server = Running`
   - 能看到 `USB Status` / `USB Message`
3. 点击 `USB Connect`
4. 等待 Windows 端完成 USB/ADB 编排
5. Android 会自动切换到 `127.0.0.1:8765` 并自动连接

说明：

- 正常情况下不需要再手动输入 localhost
- 正常情况下不需要再点击 Android 端的 `Connect`
- 如果失败，请先检查 `USB Status`、`USB Message` 和 `Server Error`

## 5. 日常使用

1. 在平板按键面板点击按键，Windows 会执行对应动作（KEY/TEXT/MACRO/MOUSE）
2. 平板麦克风音频会实时发送到 Windows Receiver
3. 其它应用（OBS/会议软件）把麦克风设备选为 `CABLE Output (VB-Audio Virtual Cable)`
4. Receiver 可随时切换 `Enable/Disable Actions` 防止误触

## 6. 声音路由检查

1. Receiver 输出设备必须是 `CABLE Input`
2. 目标软件输入设备必须是 `CABLE Output`
3. 若监听到回声，关闭系统对该设备的监听

## 7. 从源码重建（可选）

### 7.1 生成 Windows `Receiver.exe`

1. 运行：`build/build_windows.ps1`
2. 产物：`windows_receiver/dist/Receiver.exe`

### 7.2 生成 Android APK

1. Debug：`build/build_android.ps1 -BuildType Debug`
2. Release：`build/build_android.ps1 -BuildType Release`
3. 已签名 Release：`build/sign_android_release.ps1`

已签名产物：

- `android_app/app/build/outputs/apk/release/app-release-signed.apk`

## 8. 常见问题

### 8.1 没有声音

1. Android 是否已连接、未静音
2. Receiver 输出设备是否是 `CABLE Input`
3. 目标软件输入设备是否是 `CABLE Output`

### 8.2 延迟偏高

1. 使用 5GHz Wi-Fi
2. 减少网络抖动源
3. 降低 jitter 目标值（默认 60ms）

### 8.3 动作没有触发

1. Windows 端 `Actions` 是否开启
2. 当前布局里的 key 是否配置了合法 `action`
3. Windows 是否拦截了目标快捷键（例如某些系统级组合键）

## 9. 当前已知限制

1. 当前 USB 一键连接仍依赖 `ADB reverse`
2. Opus 音频编码仍未接入，当前以 PCM 链路为主
3. 虚拟麦克风仍依赖 VB-CABLE 或等价方案
