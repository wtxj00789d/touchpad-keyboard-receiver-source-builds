# FluxMic

FluxMic is about one very particular idea: turning an Android tablet into something that can actually live on your desk as a keyboard, not just as a spare screen with controls on it.

The keyboard is the center. The upper half of the screen is there to support it with context, status, and a few useful controls. The Windows Receiver sits quietly on the other side and makes the whole setup work.

- Chinese version: [README_CN.md](README_CN.md)
- Chinese user manual: [USER_MANUAL_CN.md](USER_MANUAL_CN.md)
- Protocol reference: [protocol.md](protocol.md)

## What FluxMic Is

FluxMic is a tablet-first glass keyboard experience for Windows desks.

It is built around a few simple priorities:

- typing comes first
- the tablet should feel like a real desk companion, not a novelty panel
- the background is part of the experience, not an afterthought
- the overlay should add useful context without taking over the screen
- the Windows Receiver is the bridge host, not the main stage

Touchpad is still part of the repository and still part of the broader family, but it is not the lead story of the current bundle.

## Core Features

### A keyboard you can make your own

FluxMic supports custom image and video backgrounds, and that is a core feature, not just decoration. The point is not to make the keyboard flashy for a minute. The point is to let the tablet feel like it belongs on your desk and matches the space around it.

### An overlay that earns its space

The overlay is also a core part of the product. It fills the natural empty area above the keyboard with the things you actually want nearby:

- connection state
- current window information
- window actions
- mute
- volume

It is meant to feel present and useful, not crowded and control-panel-like.

### A tablet-first keyboard flow

- 60% and 68-key layouts
- visible `Fn`, `Caps`, and `Shift` state on the keycaps
- a glass keyboard layout designed to stay on screen and stay useful

### Two practical connection paths

- Wi-Fi mode for the normal local-network setup
- one-click USB Connect for the current wired workflow

### Windows-side bridge

The Windows Receiver handles:

- the WebSocket host
- action execution
- active-window feedback
- window actions
- the retained microphone path from the current alpha implementation

## A Couple Of Real Use Stories

### Desk typing, not desk fiddling

You drop the tablet in front of your monitor in the morning, open FluxMic, and leave it there. The background is your own image or video. The bottom half is your keyboard. The top half quietly shows whether you are connected, what window is active, and gives you just enough control to minimize, maximize, mute, or adjust volume without breaking your typing rhythm.

### A keyboard that stays out of your way

FluxMic is not trying to turn every inch of the screen into a button. Most of the time, it just sits there like a keyboard should. When you look up, the overlay tells you what is going on. When you need a quick action, it is already there. Then you go back to typing.

## How The Pieces Fit Together

### Android tablet app

The Android app is the main keyboard surface.

It owns:

- the keyboard layouts
- key layers and visible remapping
- the overlay
- background customization
- the optional retained mic path

### Windows Receiver

The Windows Receiver is the host bridge.

It handles:

- the WebSocket server
- action execution
- active-window feedback
- window actions
- audio receive/output
- USB Connect orchestration

## Connection Modes

### Wi-Fi mode

This is the straightforward local-network path.

1. Find the PC IP address, for example `192.168.1.10`
2. Make sure `8765/TCP` is available
3. Connect from Android to `ws://192.168.1.10:8765`

### USB Connect mode

USB Connect is convenient, but it is still based on `ADB reverse`.

Requirements:

1. The Android app is already open
2. The USB cable supports data transfer
3. Android `USB debugging` is enabled
4. The tablet has already trusted the current PC for USB debugging

Flow:

1. Open the Windows Receiver
2. Confirm `Server = Running`
3. Click `USB Connect`
4. The Receiver handles the USB / ADB orchestration
5. Android switches to `127.0.0.1:8765` and connects automatically

So yes: if USB debugging is off, or the tablet has not trusted the current PC, USB Connect will not work.

Important: this is not a native non-debug USB transport.

## Repository Layout

- `android_app/`: Android Studio project for the keyboard app, touchpad app, and shared network layer
- `windows_receiver_winui/`: recommended Windows Receiver
- `windows_receiver/`: legacy Python receiver
- `build/`: build and packaging scripts
- `assets/`: shared assets and driver-related folders
- `samples/`: sample layouts and related references

## Quick Start

### 1. Grab the release assets

The usual bundle is:

- `app-release-signed-keyboard.apk`
- `Receiver_WinUI-win-x64.zip`

Release page: [GitHub Releases](https://github.com/wtxj00789d/touchpad-keyboard-receiver-source-builds/releases)

### 2. Prepare Windows

Extract the WinUI zip and keep the folder structure intact.

The current WinUI zip is the lightweight `framework-dependent` build, so the target machine should already have:

- `.NET 8 Desktop Runtime (x64)`
- `Windows App Runtime / Windows App SDK Runtime 1.8 (x64)`

If you want something that can be dropped onto a clean Windows machine and run immediately, use a self-contained build instead.

### 3. Start the Windows Receiver

Run:

- `Receiver_WinUI.exe`

### 4. Open the Android app

Install the keyboard app on the tablet and open it.

The intended default flow is that the Android app is already open before you use USB Connect.

## Build From Source

### Windows Receiver (WinUI, recommended)

- PowerShell: `E:\work\repo\build\build_windows_winui.ps1`
- PowerShell self-contained build: `E:\work\repo\build\build_windows_winui.ps1 -SelfContained`
- Batch: `E:\work\repo\build\build_windows_winui.bat`

Default lightweight output:

- `windows_receiver_winui/dist/`

Self-contained output:

- `windows_receiver_winui/dist-self-contained/`

### Android app

- Debug keyboard app: `E:\work\repo\build\build_android.ps1 -BuildType Debug -Module app`
- Release keyboard app: `E:\work\repo\build\build_android.ps1 -BuildType Release -Module app`
- Signed release APK: `E:\work\repo\build\sign_android_release.ps1 -Module app`

### Legacy Python receiver

The old Python receiver is still present in `windows_receiver/`, but the WinUI Receiver is the preferred Windows host.

## Notes

- Touchpad is still here, just not leading this release
- The microphone path is still retained from the current alpha implementation
- Action execution should be used in trusted environments only
