# FluxMic

Turn an Android tablet into a tablet-first glass keyboard for a Windows desk.

FluxMic is built around a simple idea: the Android tablet is the primary typing surface, and the Windows Receiver is the host bridge that makes the keyboard, overlay controls, and retained audio path work on the PC side.

- Chinese version: [README_CN.md](README_CN.md)
- User manual (Chinese): [USER_MANUAL_CN.md](USER_MANUAL_CN.md)
- Protocol reference: [protocol.md](protocol.md)

## What It Is

FluxMic is not positioned as a generic remote-control dashboard. Its primary product story is:

- a glass keyboard experience for Android tablets
- a desk-ready primary typing surface with 60% and 68-key layouts
- a lightweight overlay for status, window controls, and limited system controls
- a Windows host bridge for keyboard, window, audio, and transport features

Touchpad remains part of the repository and product family, but it is not the lead story of the current homepage or release bundle.

## Core Capabilities

- Tablet-first glass keyboard experience with 60% and 68-key layouts
- Visible `Fn`, `Caps`, and `Shift` layer mapping on the keycaps
- Overlay for connection state, active window information, window actions, mute, and volume
- Custom background image and video support
- Wi-Fi mode and one-click USB Connect mode
- Windows Receiver with keyboard/action execution and retained microphone path from the current alpha implementation

## System Overview

### Android tablet app

- Primary keyboard surface
- Overlay UI and background customization
- Keyboard input, layout switching, and layer states
- Optional retained microphone path from the current alpha implementation

### Windows Receiver

- WebSocket host
- Action execution bridge
- Window-state feedback and window actions
- Audio receive/output path
- USB Connect orchestration

### Connection modes

- Wi-Fi mode: connect to the PC IP and port over the local network
- USB mode: uses `ADB reverse` to map the Windows Receiver to `127.0.0.1:8765` on Android

Important: the current USB mode is not a native non-debug USB transport. It depends on Android USB debugging and trusting the current PC.

## Repository Layout

- `android_app/`: Android Studio project for the keyboard app, touchpad app, and shared network module
- `windows_receiver_winui/`: recommended Windows Receiver implementation
- `windows_receiver/`: legacy Python receiver
- `build/`: build and packaging scripts
- `assets/`: shared assets, including driver-related folders when present
- `samples/`: sample layouts and related references

## Quick Start

### 1. Download the release assets

Use the latest prerelease bundle from GitHub:

- Android APK: `app-release-signed-keyboard.apk`
- Windows Receiver: `Receiver_WinUI-win-x64.zip`
- Release page: [GitHub Releases](https://github.com/wtxj00789d/touchpad-keyboard-receiver-source-builds/releases)

### 2. Prepare Windows

Extract the WinUI zip and keep the folder structure intact.

The current WinUI release zip is the lightweight `framework-dependent` build. Target machines should have:

- `.NET 8 Desktop Runtime (x64)`
- `Windows App Runtime / Windows App SDK Runtime 1.8 (x64)`

If you need a fully bundled package for a clean Windows machine, use a self-contained build instead of the default release asset.

### 3. Start the Windows Receiver

Run:

- `Receiver_WinUI.exe`

Confirm that the Receiver is running before connecting from Android.

### 4. Open the Android app

Install and open the keyboard app on the tablet. The intended default scenario is that the Android app is already open before using USB Connect.

## Connection Modes

### Wi-Fi mode

1. Find the Windows PC IP address, for example `192.168.1.10`
2. Make sure port `8765/TCP` is allowed
3. Connect from Android to `ws://192.168.1.10:8765`

### USB Connect mode

USB Connect is currently implemented through `ADB reverse`.

Requirements:

1. The Android app is already open
2. The USB cable is physically connected and supports data transfer
3. Android `USB debugging` is enabled
4. The tablet has already trusted the current PC for USB debugging

Flow:

1. Open the Windows Receiver
2. Confirm `Server = Running`
3. Click `USB Connect`
4. The Receiver runs the USB/ADB orchestration
5. Android switches to `127.0.0.1:8765` and automatically connects

Because this flow depends on ADB, USB mode will not work unless USB debugging is enabled and the current PC is trusted by the tablet.

## Build From Source

### Windows Receiver (WinUI, recommended)

- PowerShell: `E:\work\repo\build\build_windows_winui.ps1`
- PowerShell self-contained build: `E:\work\repo\build\build_windows_winui.ps1 -SelfContained`
- Batch: `E:\work\repo\build\build_windows_winui.bat`

Default output:

- `windows_receiver_winui/dist/`

Self-contained output:

- `windows_receiver_winui/dist-self-contained/`

### Android app

- Debug keyboard app: `E:\work\repo\build\build_android.ps1 -BuildType Debug -Module app`
- Release keyboard app: `E:\work\repo\build\build_android.ps1 -BuildType Release -Module app`
- Signed release APK: `E:\work\repo\build\sign_android_release.ps1 -Module app`

### Legacy Python receiver

The legacy receiver is still present in `windows_receiver/`, but the WinUI Receiver is the recommended Windows host.

## Notes

- Touchpad remains in the repository as a family extension, but it is not the main release story here
- The retained microphone path is still part of the system, following the current alpha implementation
- Actions should be used in trusted environments only
