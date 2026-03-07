# Android FluxMic

Android Studio project with 3 modules:

- `:app` - keyboard panel + microphone streaming app
- `:touchpad` - standalone touchpad app
- `:shared_net` - shared protocol + WebSocket client layer

## Build Requirements

- JDK 17 (preferred) or JDK 21
- Android SDK 34
- Gradle wrapper is included (`gradlew` / `gradlew.bat`)

## Build

- Keyboard debug APK: `./gradlew :app:assembleDebug`
- Touchpad debug APK: `./gradlew :touchpad:assembleDebug`

Layouts for keyboard module are in:

- `app/src/main/assets/layouts/`
