# FluxMic

FluxMic is a simple idea with a very specific goal: let an Android tablet sit on a desk and feel useful as a real keyboard, not just a second screen full of buttons.

The tablet is the main surface. Windows is the host side. The Receiver keeps the connection alive, executes actions, reports window state back to the tablet, and preserves the retained audio path from the current alpha implementation.

- Chinese version: [README_CN.md](README_CN.md)
- Chinese user manual: [USER_MANUAL_CN.md](USER_MANUAL_CN.md)
- Protocol reference: [protocol.md](protocol.md)

## What FluxMic Is

FluxMic is centered on a tablet-first glass keyboard experience for Windows desks.

That means:

- Android tablet first, not a Windows-first control panel
- typing comes before everything else
- the overlay exists to support the keyboard, not to compete with it
- the Windows Receiver is the bridge host, not the star of the show

Touchpad is still in the repository and still part of the family, but it is not the main story of the current bundle.

## What It Can Do

- Run a 60% or 68-key keyboard layout on an Android tablet
- Show `Fn`, `Caps`, and `Shift` state directly on the keycaps
- Add a lightweight overlay for connection state, active window info, window controls, mute, and volume
- Use custom image or video backgrounds
- Connect over Wi-Fi or through one-click USB Connect
- Send keyboard and control actions through the Windows Receiver
- Keep the microphone path from the current alpha implementation available

## How The Pieces Fit Together

### On the Android side

The Android app is the main keyboard surface.

It owns:

- the keyboard layouts
- key layers and visible remapping
- the overlay
- background customization
- the optional retained mic path

### On the Windows side

The Windows Receiver is the host bridge.

It handles:

- the WebSocket server
- action execution
- active-window feedback
- window actions
- audio receive/output
- USB Connect orchestration

### Connection modes

There are two practical ways to connect:

- Wi-Fi mode, for the normal local-network path
- USB mode, for the current ADB-based wired flow

Important: the current USB mode is not a native non-debug USB transport. It works through `ADB reverse`, so Android USB debugging must be enabled and the tablet must trust the current PC.

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

If the Receiver is not up, the tablet has nothing to talk to.

### 4. Open the Android app

Install the keyboard app on the tablet and open it.

The intended default flow is that the Android app is already open before you use USB Connect.

## Connection Modes

### Wi-Fi mode

This is the straightforward network path.

1. Find the PC IP address, for example `192.168.1.10`
2. Make sure `8765/TCP` is available
3. Connect from Android to `ws://192.168.1.10:8765`

### USB Connect mode

USB Connect currently rides on top of `ADB reverse`.

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
