# FluxMic

FluxMic 想做的事情其实很具体：让一台 Android 平板摆在桌上时，不只是“旁边那块带点按钮的屏幕”，而是真的能像一把键盘那样长期存在。

键盘是中心。屏幕上半部分不是为了堆功能，而是为了给键盘补上状态、上下文和少量真正顺手的控制。Windows Receiver 则安静地待在另一边，把整套体验接起来。

- 英文主页：[README.md](README.md)
- 中文用户手册：[USER_MANUAL_CN.md](USER_MANUAL_CN.md)
- 协议说明：[protocol.md](protocol.md)

## FluxMic 是什么

FluxMic 的核心是“平板优先的玻璃化键盘体验”。

它背后的优先级很简单：

- 打字优先
- 平板应该像桌面伙伴，而不是新奇控制板
- 背景是体验的一部分，不是附带装饰
- Overlay 要提供有用信息，但不能抢主角
- Windows Receiver 是桥接宿主，不是产品舞台

`touchpad` 仍然在仓库里，也仍然属于这个产品家族的一部分，只是它不是当前这个 bundle 的主叙事。

## 核心功能

### 一块真正能变成你的桌面一部分的键盘

FluxMic 支持自定义图片和视频背景，而且这不是边角功能。重点不是让键盘短暂地“花一点”，而是让这块平板更像你桌面的一部分，看起来就是愿意一直摆在那里的东西。

### 一层值得占据上半区的 Overlay

Overlay 也是核心功能点。它把键盘上方天然会空出来的区域，变成真正有用但不过度的部分。

你会在这里看到：

- 连接状态
- 当前窗口信息
- 窗口操作
- 静音
- 音量

它应该让你觉得顺手，而不是像打开了一个控制台。

### 为平板而生的键盘主流程

- 60% 和 68 键布局
- `Fn`、`Caps`、`Shift` 状态直接体现在键帽上
- 一套适合长期停留在屏幕上的玻璃键盘布局

### 两条实用的连接路径

- Wi-Fi 模式，用于常规局域网连接
- 一键 `USB Connect`，用于当前有线工作流

### Windows 侧桥接能力

Windows Receiver 负责：

- WebSocket 宿主
- 动作执行
- 当前窗口状态回传
- 窗口操作
- 沿用当前 Alpha 方案的麦克风链路

## 两个更接近日常办公的使用故事

### 桌面主力打字，而不是桌面折腾

早上把平板放到显示器前，打开 FluxMic，然后就让它待在那里。背景是你自己的图片或视频，下半部分是键盘，上半部分安静地告诉你有没有连上、当前是什么窗口，也让你顺手做最小化、最大化、静音、调音量这些小动作，不用老是把节奏打断。

### 一块不会老想抢你注意力的键盘

FluxMic 不是想把屏幕每一寸都做成按钮。大多数时候，它就像一把键盘该有的样子待在那里。你抬眼的时候，Overlay 刚好把需要的东西放在上面；你需要轻控制的时候，它也已经在那里；然后你继续打字。

## 这套东西怎么拼在一起

### Android 平板 App

Android App 就是主键盘面。

它负责：

- 键盘布局
- 层状态和可视映射
- Overlay
- 背景自定义
- 可选保留的麦克风链路

### Windows Receiver

Windows Receiver 是桥接宿主。

它负责：

- WebSocket server
- 动作执行
- 当前窗口状态回传
- 窗口操作
- 音频接收与输出
- `USB Connect` 编排

## 连接模式

### Wi-Fi 模式

这是最直接的局域网连接方式。

1. 找到 PC 的 IP，例如 `192.168.1.10`
2. 确认 `8765/TCP` 可用
3. Android 连接到 `ws://192.168.1.10:8765`

### USB Connect 模式

USB Connect 很方便，但它本质上仍然基于 `ADB reverse`。

前提：

1. Android App 已经打开
2. USB 线支持数据传输
3. Android 已开启 `USB 调试`
4. 平板已经对当前这台电脑完成 USB 调试授权 / 信任

流程：

1. 打开 Windows Receiver
2. 确认 `Server = Running`
3. 点击 `USB Connect`
4. Receiver 完成 USB / ADB 编排
5. Android 自动切到 `127.0.0.1:8765` 并发起连接

所以，是的：如果 USB 调试没开，或者平板还没信任这台电脑，`USB Connect` 就不会工作。

另外要说明，这不是“脱离调试的原生 USB 传输”。

## 仓库结构

- `android_app/`：Android Studio 工程，包含键盘 App、touchpad App 和共享网络层
- `windows_receiver_winui/`：推荐使用的 Windows Receiver
- `windows_receiver/`：旧版 Python Receiver
- `build/`：构建和打包脚本
- `assets/`：共享资源和驱动相关目录
- `samples/`：示例布局和相关参考

## 快速开始

### 1. 获取发布资产

常用的 bundle 包含：

- `app-release-signed-keyboard.apk`
- `Receiver_WinUI-win-x64.zip`

发布页：[GitHub Releases](https://github.com/wtxj00789d/touchpad-keyboard-receiver-source-builds/releases)

### 2. 准备 Windows 端

解压 WinUI 压缩包时，请保持目录结构完整。

当前这个 WinUI 包是轻量 `framework-dependent` 构建，所以目标机器最好已经安装：

- `.NET 8 Desktop Runtime (x64)`
- `Windows App Runtime / Windows App SDK Runtime 1.8 (x64)`

如果你需要的是“扔到一台干净 Windows 机器上就直接跑”的版本，请改用 self-contained 构建。

### 3. 启动 Windows Receiver

运行：

- `Receiver_WinUI.exe`

### 4. 打开 Android App

在平板上安装键盘 App 并打开它。

推荐的默认流程是：在使用 `USB Connect` 之前，Android App 已经处于打开状态。

## 从源码构建

### Windows Receiver（推荐用 WinUI）

- PowerShell：`E:\work\repo\build\build_windows_winui.ps1`
- PowerShell 自包含构建：`E:\work\repo\build\build_windows_winui.ps1 -SelfContained`
- Batch：`E:\work\repo\build\build_windows_winui.bat`

默认轻量输出：

- `windows_receiver_winui/dist/`

自包含输出：

- `windows_receiver_winui/dist-self-contained/`

### Android App

- Debug 键盘 App：`E:\work\repo\build\build_android.ps1 -BuildType Debug -Module app`
- Release 键盘 App：`E:\work\repo\build\build_android.ps1 -BuildType Release -Module app`
- 已签名 Release APK：`E:\work\repo\build\sign_android_release.ps1 -Module app`

已签名产物：

- `android_app/app/build/outputs/apk/release/app-release-signed.apk`

### 旧版 Python Receiver

旧版 Python Receiver 仍保留在 `windows_receiver/` 中，但目前推荐使用 WinUI Receiver 作为 Windows 端宿主。

## 说明

- `touchpad` 仍然在仓库里，只是不是当前这个 release 的主角
- 麦克风链路仍然沿用当前 Alpha 的实现
- 动作执行默认应只在受信任环境中使用
