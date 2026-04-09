# FluxMic

鎶?Android 骞虫澘鍙樻垚妗岄潰涓婄殑骞虫澘浼樺厛鐜荤拑鍖栦富鍔涢敭鐩樸€?
FluxMic 鐨勬牳蹇冩€濊矾寰堢畝鍗曪細Android 骞虫澘鏄富杈撳叆闈紝Windows Receiver 鏄涓绘ˉ鎺ョ锛岃礋璐ｆ妸閿洏銆丱verlay 鎺у埗鍜屼繚鐣欑殑闊抽閾捐矾鎺ュ埌 Windows 渚с€?
- 鑻辨枃涓婚〉: [README.md](README.md)
- 涓枃鐢ㄦ埛鎵嬪唽: [USER_MANUAL_CN.md](USER_MANUAL_CN.md)
- 鍗忚璇存槑: [protocol.md](protocol.md)

## 杩欐槸浠€涔?
FluxMic 涓嶆槸涓€涓硾鐢ㄨ繙绋嬫帶鍒堕潰鏉裤€傚畠褰撳墠鏇村噯纭殑浜у搧瀹氫箟鏄細

- 闈㈠悜 Android 骞虫澘鐨勭幓鐠冨寲閿洏浣撻獙
- 鍙暱鏈熸憜鍦ㄦ闈笂鐨勪富鍔涜緭鍏ラ潰
- 鏀寔 60% 鍜?68 閿袱绉嶄富閿洏甯冨眬
- 甯﹁交閲?Overlay锛岀敤浜庣姸鎬併€佺獥鍙ｆ搷浣滃拰灏戦噺绯荤粺鎺у埗
- 鐢?Windows Receiver 鎻愪緵妗ユ帴涓庡涓昏兘鍔?
Touchpad 浠嶇劧鏄粨搴撳拰浜у搧瀹舵棌鐨勪竴閮ㄥ垎锛屼絾涓嶆槸褰撳墠涓婚〉鍜?release bundle 鐨勪富鍙欎簨銆?
## 鏍稿績鑳藉姏

- 骞虫澘浼樺厛鐨勭幓鐠冨寲閿洏浣撻獙锛屾敮鎸?60% / 68 閿?- `Fn`銆乣Caps`銆乣Shift` 灞傜姸鎬佸湪閿附涓婃槑纭彲瑙?- Overlay 鎻愪緵杩炴帴鐘舵€併€佸綋鍓嶇獥鍙ｃ€佺獥鍙ｆ搷浣溿€侀潤闊冲拰闊抽噺绛夎交鎺у埗
- 鏀寔鑷畾涔夊浘鐗囧拰瑙嗛鑳屾櫙
- 鏀寔 Wi-Fi 妯″紡鍜屼竴閿?`USB Connect` 妯″紡
- Windows Receiver 鎻愪緵鎸夐敭/鍔ㄤ綔鎵ц涓庝繚鐣欑殑 Alpha 楹﹀厠椋庨摼璺?
## 绯荤粺姒傝

### Android 骞虫澘 App

- 涓婚敭鐩樿緭鍏ラ潰
- Overlay 鐣岄潰涓庤儗鏅嚜瀹氫箟
- 閿洏杈撳叆銆佸竷灞€鍒囨崲銆佸眰鐘舵€佺鐞?- 淇濈暀褰撳墠 Alpha 瀹炵幇鏂瑰紡鐨勯害鍏嬮閾捐矾

### Windows Receiver

- WebSocket 瀹夸富
- 鍔ㄤ綔鎵ц妗ユ帴
- 褰撳墠绐楀彛鐘舵€佸洖浼犱笌绐楀彛鎿嶄綔
- 闊抽鎺ユ敹涓庤緭鍑洪摼璺?- `USB Connect` 缂栨帓鑳藉姏

### 杩炴帴妯″紡

- Wi-Fi 妯″紡锛氶€氳繃灞€鍩熺綉杩炴帴 Windows 绔湴鍧€
- USB 妯″紡锛氶€氳繃 `ADB reverse` 鎶?Windows Receiver 鏄犲皠鍒?Android 鐨?`127.0.0.1:8765`

閲嶇偣璇存槑锛氬綋鍓?USB 妯″紡涓嶆槸鈥滆劚绂昏皟璇曠殑鍘熺敓 USB 浼犺緭鈥濓紝鑰屾槸渚濊禆 Android USB 璋冭瘯鑳藉姏銆?
## 浠撳簱缁撴瀯

- `android_app/`: Android Studio 宸ョ▼锛屽寘鍚敭鐩?App銆乀ouchpad App 鍜屽叡浜綉缁滄ā鍧?- `windows_receiver_winui/`: 鎺ㄨ崘浣跨敤鐨?Windows Receiver
- `windows_receiver/`: 鏃х増 Python Receiver
- `build/`: 鏋勫缓涓庢墦鍖呰剼鏈?- `assets/`: 鍏变韩璧勬簮涓庨┍鍔ㄧ浉鍏崇洰褰?- `samples/`: 甯冨眬鏍蜂緥绛夊弬鑰冩枃浠?
## 蹇€熷紑濮?
### 1. 涓嬭浇 release 璧勪骇

GitHub 鏈€鏂伴鍙戝竷 bundle 閲屼富瑕佹湁锛?
- Android APK锛歚app-release-signed-keyboard.apk`
- Windows Receiver锛歚Receiver_WinUI-win-x64.zip`
- 鍙戝竷椤碉細[GitHub Releases](https://github.com/wtxj00789d/touchpad-keyboard-receiver-source-builds/releases)

### 2. 鍑嗗 Windows

瑙ｅ帇 WinUI zip锛屽苟淇濇寔鏁翠釜鐩綍缁撴瀯瀹屾暣銆?
褰撳墠 WinUI release zip 鏄交閲?`framework-dependent` 鐗堟湰锛岀洰鏍囨満鍣ㄥ缓璁鍏堝畨瑁咃細

- `.NET 8 Desktop Runtime (x64)`
- `Windows App Runtime / Windows App SDK Runtime 1.8 (x64)`

濡傛灉浣犻渶瑕佲€滃湪涓€鍙板共鍑€鐨?Windows 鏈哄櫒涓婃嬁鏉ュ嵆璺戔€濈殑鐗堟湰锛岃鏀圭敤 self-contained 鏋勫缓鍖咃紝鑰屼笉鏄粯璁?release 璧勪骇銆?
### 3. 鍚姩 Windows Receiver

杩愯锛?
- `Receiver_WinUI.exe`

鍏堢‘璁?Receiver 宸茬粡鍚姩锛屽啀浠?Android 鍙戣捣杩炴帴銆?
### 4. 鎵撳紑 Android App

鍦ㄥ钩鏉夸笂瀹夎骞舵墦寮€閿洏 App銆傚綋鍓嶆帹鑽愮殑榛樿浣跨敤鍦烘櫙鏄細鍦ㄤ娇鐢?`USB Connect` 涔嬪墠锛孉ndroid App 宸茬粡澶勪簬鎵撳紑鐘舵€併€?
## 杩炴帴妯″紡

### Wi-Fi 妯″紡

1. 鏌ヨ Windows PC 鐨?IP锛屼緥濡?`192.168.1.10`
2. 纭繚 `8765/TCP` 鍙敤
3. Android 杩炴帴鍒?`ws://192.168.1.10:8765`

### USB Connect 妯″紡

褰撳墠 `USB Connect` 鍩轰簬 `ADB reverse`銆?
鍓嶆彁鏉′欢锛?
1. Android App 宸茬粡鎵撳紑
2. USB 绾垮凡鐗╃悊杩炴帴锛屽苟涓旀敮鎸佹暟鎹紶杈?3. Android 宸插紑鍚?`USB 璋冭瘯`
4. 骞虫澘宸茬粡瀵瑰綋鍓?PC 杩涜杩?USB 璋冭瘯鎺堟潈 / 淇′换

浣跨敤娴佺▼锛?
1. 鎵撳紑 Windows Receiver
2. 纭 `Server = Running`
3. 鐐瑰嚮 `USB Connect`
4. Receiver 鑷姩鎵ц USB/ADB 缂栨帓
5. Android 鑷姩鍒囨崲鍒?`127.0.0.1:8765` 骞跺彂璧疯繛鎺?
鍥犱负杩欐潯閾捐矾渚濊禆 ADB锛屾墍浠ュ鏋滄病鏈夊紑鍚?USB 璋冭瘯锛屾垨鑰呭钩鏉胯繕娌℃湁淇′换褰撳墠鐢佃剳锛孶SB 妯″紡灏变笉浼氭垚绔嬨€?
## 浠庢簮鐮佹瀯寤?
### Windows Receiver锛圵inUI锛屾帹鑽愶級

- PowerShell: `E:\work\repo\build\build_windows_winui.ps1`
- PowerShell 鑷寘鍚瀯寤? `E:\work\repo\build\build_windows_winui.ps1 -SelfContained`
- Batch: `E:\work\repo\build\build_windows_winui.bat`

榛樿杈撳嚭锛?
- `windows_receiver_winui/dist/`

鑷寘鍚緭鍑猴細

- `windows_receiver_winui/dist-self-contained/`

### Android App

- Debug 閿洏鍖? `E:\work\repo\build\build_android.ps1 -BuildType Debug -Module app`
- Release 閿洏鍖? `E:\work\repo\build\build_android.ps1 -BuildType Release -Module app`
- 宸茬鍚?Release APK: `E:\work\repo\build\sign_android_release.ps1 -Module app`

### 鏃х増 Python Receiver

鏃х増 Receiver 浠嶇劧淇濈暀鍦?`windows_receiver/` 涓紝浣嗘帹鑽愪紭鍏堜娇鐢?WinUI Receiver銆?
## 璇存槑

- Touchpad 浠嶇劧鍦ㄤ粨搴撲腑淇濈暀锛屼綔涓哄悓涓€瀹舵棌鑳藉姏鎵╁睍锛屼絾涓嶆槸褰撳墠 release 鐨勪富鍙欎簨
- 楹﹀厠椋庨摼璺粛鐒朵繚鐣欙紝娌跨敤褰撳墠 Alpha 鐨勫疄鐜版柟寮?- 鍔ㄤ綔鎵ц寤鸿鍙湪鍙俊鐜涓娇鐢?