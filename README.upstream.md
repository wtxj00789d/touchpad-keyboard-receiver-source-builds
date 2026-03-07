# FluxMic Panel (Android) + Windows Receiver

把 Android 平板变成“Flux 风格键盘面板 + 无线麦克风 / 触摸板”，通过 WebSocket 把音频和动作发到 Windows 11，并在 Windows 端注入到虚拟麦克风设备。

## 目录结构

- `android_app/` Android Studio 工程（`:app` 键盘+麦克风、`:touchpad` 触摸板、`:shared_net` 共享网络层）
- `windows_receiver/` Windows 接收端源码（Python + Tkinter，旧版）
- `windows_receiver_winui/` Windows 接收端源码（WPF/.NET 8，推荐）
- `protocol.md` 音频帧与 JSON 消息协议
- `USER_MANUAL_CN.md` 最终用户使用说明书（中文）
- `samples/layouts/` 键盘布局 JSON 示例
- `assets/vbcable/` 虚拟音频驱动离线安装包放置目录
- `build/` 一键构建脚本

## 当前实现状态

### 已实现（可运行）

1. Android 全屏键盘面板（JSON 布局、多页 Tab、按压高亮、ripple、长按、滑动选键、防误触）
2. Android 麦克风采集（48k/mono/16bit，20ms 帧）并通过 WebSocket 二进制帧发送（PCM）
3. Android Touchpad 独立 APK（单指移动、双指滚动、左右键、双击、按住拖拽）
4. Android 与 Windows 通过同一 WebSocket 连接传输：
   - binary: audio frame
   - text json: action/control/state/ping
5. Windows WebSocket Server（默认 `0.0.0.0:8765`，支持多客户端并发）
6. Windows 音频接收 + jitter buffer（默认 60ms）+ 输出设备可选
7. Windows 动作执行（KEY/TEXT/MACRO/MOUSE，含触摸板相对移动/滚轮/按键）
8. Windows UI：连接状态、RTT/抖动、驱动状态、输出设备选择、启停、动作开关、最近日志
9. 启动时自动检测 VB-CABLE；缺失时弹窗并可一键安装（UAC 提权）

### 编解码说明

- 已实现：`PCM16`（codec=0）
- 预留接口：`OPUS`（codec=1）
- 如需启用 Opus，可在后续阶段接入本地 Opus 库（建议离线放入仓库，避免在线拉取）

## 快速开始

### 1) Windows Receiver（Python 旧版）

1. 进入 `windows_receiver/`
2. 安装依赖：
   - `py -3.11 -m venv .venv`
   - `.\.venv\Scripts\Activate.ps1`
   - `pip install -r requirements.txt`
3. 运行：`python -m receiver.main`

### 1b) Windows Receiver（WinUI/WPF 推荐）

1. 进入 `windows_receiver_winui/ReceiverWinUI/`
2. 运行：`dotnet run -c Release`

打包：

- `build/build_windows_winui.ps1`
- 产物：`windows_receiver_winui/dist/Receiver_WinUI.exe`
- 不会覆盖旧版：`windows_receiver/dist/Receiver.exe`

首次启动会检测 VB-CABLE：
- 若已安装：继续运行
- 若未安装：提示安装。程序会在 `assets/vbcable/` 中查找离线安装包（见下文）

### 2) Android App

1. Android Studio 打开 `android_app/`
2. 使用 JDK 17 或 JDK 21 同步 Gradle
3. 键盘+麦克风 APK：安装 `:app` 模块（首次运行授权麦克风）
4. 触摸板 APK：安装 `:touchpad` 模块（无麦克风权限）
5. 在两个 App 中都填入同一 Receiver WebSocket 地址后连接

说明：仓库已包含 `gradle-wrapper.jar`，可直接使用 `android_app/gradlew`。

### 3) 连接模式

#### Wi-Fi 模式

1. 在 PC 查 IP（例如 `192.168.1.10`）
2. Windows 放行 `8765/TCP`
3. Android 连接：`ws://192.168.1.10:8765`

#### ADB reverse 模式

1. `adb devices`
2. `adb reverse tcp:8765 tcp:8765`
3. Android 连接：`ws://127.0.0.1:8765`

## VB-CABLE 离线包放置（许可与替代方案）

由于驱动分发许可可能变化，仓库默认不内置安装器二进制。请手动放置离线包到：

- `assets/vbcable/VBCABLE_Setup_x64.exe`

可选也支持：

- `assets/vbcable/VBCABLE_Driver_Pack*.zip`（程序会自动解压并查找 x64 安装器）

如果缺失，Receiver 会明确弹窗提示并引导手动放置。

## Windows 声音设置

- Receiver 输出设备选 `CABLE Input (VB-Audio Virtual Cable)`
- 其它软件把麦克风选为 `CABLE Output (VB-Audio Virtual Cable)`

## 常见问题

1. 无声
- 检查 Android 是否已连接且麦克风未静音
- 检查 Receiver 输出设备是否选到 `CABLE Input`
- 检查目标软件是否选了 `CABLE Output` 作为麦克风

2. 延迟高
- 降低 jitter buffer（例如 60ms -> 40ms）
- 确认局域网信号稳定，尽量 5GHz Wi-Fi

3. 回声
- 禁止系统同时监听 CABLE Output
- 避免扬声器外放被平板再次采集

4. 动作不执行
- Receiver 中确认 `Actions: Enabled`
- 某些目标程序需要管理员权限，Receiver 也需管理员运行

5. 安装驱动失败
- 手动运行 `VBCABLE_Setup_x64.exe`（管理员）
- 安装后重启 Receiver 并刷新设备列表

## 打包

- Windows（Python 旧版）: `build/build_windows.ps1` 或 `build/build_windows.bat`
- Windows（WinUI/WPF）: `build/build_windows_winui.ps1` 或 `build/build_windows_winui.bat`
- Android: `build/build_android.ps1` 或 `build/build_android.bat`
  - Keyboard Debug: `build\\build_android.ps1 -BuildType Debug -Module app`
  - Keyboard Release: `build\\build_android.ps1 -BuildType Release -Module app`
  - Touchpad Debug: `build\\build_android.ps1 -BuildType Debug -Module touchpad`
  - Touchpad Release: `build\\build_android.ps1 -BuildType Release -Module touchpad`
  - 说明文档：`build/build_android.md`

### Android 已签名 Release APK

- `build\\sign_android_release.ps1`
- 输出：`android_app\\app\\build\\outputs\\apk\\release\\app-release-signed.apk`

## 安全提示

- 默认会处理所有已连接客户端发送的动作
- 提供动作执行总开关，必要时可随时暂停
- 建议只在可信局域网使用
