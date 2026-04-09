# FluxMic

FluxMic 鍏跺疄鏄湪鍋氫竴浠跺緢鍏蜂綋鐨勪簨锛氳涓€鍙?Android 骞虫澘鎽嗗湪妗屼笂鏃讹紝涓嶅彧鏄€滅浜屽潡灞忓箷鈥濓紝鑰屾槸鐪熺殑鑳藉儚涓€鎶婇敭鐩橀偅鏍锋湁瀛樺湪鎰熴€?
骞虫澘鏄富杈撳叆闈紝Windows 鏄涓讳晶銆俁eceiver 璐熻矗鎶婅繛鎺ユ拺璧锋潵锛屾妸鍔ㄤ綔鎵ц鎺夛紝鎶婄獥鍙ｇ姸鎬佸洖浼犵粰骞虫澘锛屼篃缁х画鎵挎帴褰撳墠 Alpha 闃舵宸茬粡鏈夌殑闊抽閾捐矾銆?
- 鑻辨枃涓婚〉: [README.md](README.md)
- 涓枃鐢ㄦ埛鎵嬪唽: [USER_MANUAL_CN.md](USER_MANUAL_CN.md)
- 鍗忚璇存槑: [protocol.md](protocol.md)

## FluxMic 鏄粈涔?
FluxMic 鐨勪腑蹇冧笉鏄€滆繙绋嬫帶鍒跺彴鈥濓紝鑰屾槸鈥滃钩鏉夸紭鍏堢殑鐜荤拑鍖栭敭鐩樹綋楠屸€濄€?
鎹㈠彞璇濊锛?
- 鍏堟槸 Android 骞虫澘閿洏锛屽啀璋堝埆鐨?- 鎵撳瓧浣撻獙浼樺厛锛屾帶鍒惰兘鍔涙槸杈呭姪
- Overlay 鏄负浜嗗府閿洏鎴愮珛锛屼笉鏄负浜嗘姠涓昏
- Windows Receiver 鏄ˉ鎺ュ涓伙紝涓嶆槸浜у搧涓昏垶鍙?
Touchpad 浠嶇劧鍦ㄤ粨搴撻噷锛屼篃浠嶇劧灞炰簬鍚屼竴瀹舵棌锛屼絾涓嶆槸褰撳墠杩欎釜 bundle 鐨勪富鍙欎簨銆?
## 瀹冪幇鍦ㄨ兘鍋氫粈涔?
- 鍦?Android 骞虫澘涓婅繍琛?60% 鎴?68 閿竷灞€
- 鎶?`Fn`銆乣Caps`銆乣Shift` 鐨勭姸鎬佺洿鎺ュ弽鏄犲湪閿附涓?- 鐢ㄨ交閲?Overlay 鎻愪緵杩炴帴鐘舵€併€佸綋鍓嶇獥鍙ｃ€佺獥鍙ｆ搷浣溿€侀潤闊冲拰闊抽噺
- 鏀寔鍥剧墖鍜岃棰戣儗鏅?- 鏀寔 Wi-Fi 妯″紡鍜屼竴閿?`USB Connect`
- 閫氳繃 Windows Receiver 鎵ц閿洏鍜屾帶鍒跺姩浣?- 淇濈暀褰撳墠 Alpha 瀹炵幇鏂瑰紡涓嬬殑楹﹀厠椋庨摼璺?
## 杩欏涓滆タ鎬庝箞鎷煎湪涓€璧?
### Android 渚?
Android App 灏辨槸涓婚敭鐩橀潰銆?
瀹冭礋璐ｏ細

- 閿洏甯冨眬
- 灞傜姸鎬佸拰鍙鏄犲皠
- Overlay
- 鑳屾櫙鑷畾涔?- 鍙€変繚鐣欑殑楹﹀厠椋庨摼璺?
### Windows 渚?
Windows Receiver 鏄ˉ鎺ュ涓汇€?
瀹冭礋璐ｏ細

- WebSocket server
- 鍔ㄤ綔鎵ц
- 褰撳墠绐楀彛鐘舵€佸洖浼?- 绐楀彛鎿嶄綔
- 闊抽鎺ユ敹涓庤緭鍑?- `USB Connect` 缂栨帓

### 杩炴帴鏂瑰紡

鐜板湪涓昏鏈変袱鏉¤矾锛?
- Wi-Fi 妯″紡锛岃蛋灞€鍩熺綉
- USB 妯″紡锛岃蛋褰撳墠杩欏鍩轰簬 ADB 鐨勬湁绾挎祦绋?
閲嶇偣璇存槑锛氬綋鍓?USB 妯″紡涓嶆槸鈥滆劚绂昏皟璇曠殑鍘熺敓 USB 浼犺緭鈥濄€傚畠渚濊禆 `ADB reverse`锛屾墍浠?Android 蹇呴』寮€鍚?USB 璋冭瘯锛屽苟涓斿钩鏉垮繀椤诲凡缁忎俊浠诲綋鍓嶈繖鍙扮數鑴戙€?
## 浠撳簱缁撴瀯

- `android_app/`: Android Studio 宸ョ▼锛屽寘鍚敭鐩?App銆乀ouchpad App 鍜屽叡浜綉缁滃眰
- `windows_receiver_winui/`: 鎺ㄨ崘浣跨敤鐨?Windows Receiver
- `windows_receiver/`: 鏃х増 Python Receiver
- `build/`: 鏋勫缓涓庢墦鍖呰剼鏈?- `assets/`: 鍏变韩璧勬簮鍜岄┍鍔ㄧ浉鍏崇洰褰?- `samples/`: 甯冨眬鏍蜂緥鍜屽弬鑰冩枃浠?
## 蹇€熷紑濮?
### 1. 鍏堟嬁鍒?release 璧勪骇

甯哥敤鐨?bundle 涓昏鏄細

- `app-release-signed-keyboard.apk`
- `Receiver_WinUI-win-x64.zip`

鍙戝竷椤碉細[GitHub Releases](https://github.com/wtxj00789d/touchpad-keyboard-receiver-source-builds/releases)

### 2. 鍏堟妸 Windows 杩欒竟鍑嗗濂?
瑙ｅ帇 WinUI zip锛屽苟淇濇寔鏁翠釜鐩綍缁撴瀯瀹屾暣銆?
褰撳墠 WinUI zip 鏄交閲?`framework-dependent` 鐗堟湰锛屾墍浠ョ洰鏍囨満鍣ㄦ渶濂藉凡缁忚濂斤細

- `.NET 8 Desktop Runtime (x64)`
- `Windows App Runtime / Windows App SDK Runtime 1.8 (x64)`

濡傛灉浣犳兂瑕佲€滀涪鍒颁竴鍙板共鍑€ Windows 鏈哄櫒涓婂氨鐩存帴璺戔€濈殑鐗堟湰锛岄偅灏卞埆鐢ㄩ粯璁?release 璧勪骇锛岃€屾槸鏀圭敤 self-contained 鏋勫缓銆?
### 3. 鍚姩 Windows Receiver

杩愯锛?
- `Receiver_WinUI.exe`

Receiver 娌¤捣鏉ワ紝骞虫澘杩欒竟涔熷氨娌℃湁鍙繛鎺ョ殑鐩爣銆?
### 4. 鎵撳紑 Android App

鍦ㄥ钩鏉夸笂瀹夎骞舵墦寮€閿洏 App銆?
褰撳墠鎺ㄨ崘鐨勯粯璁や娇鐢ㄦ柟寮忔槸锛氬湪浣犵偣鍑?`USB Connect` 涔嬪墠锛孉ndroid App 宸茬粡澶勪簬鎵撳紑鐘舵€併€?
## 杩炴帴妯″紡

### Wi-Fi 妯″紡

杩欐槸鏈€鐩存帴鐨勫眬鍩熺綉杩炴帴鏂瑰紡銆?
1. 鎵惧埌 PC 鐨?IP锛屾瘮濡?`192.168.1.10`
2. 纭繚 `8765/TCP` 鍙敤
3. 鍦?Android 绔繛鎺?`ws://192.168.1.10:8765`

### USB Connect 妯″紡

褰撳墠 `USB Connect` 杩愯鍦?`ADB reverse` 涔嬩笂銆?
鍓嶆彁鏉′欢锛?
1. Android App 宸茬粡鎵撳紑
2. USB 绾挎敮鎸佹暟鎹紶杈?3. Android 宸插紑鍚?`USB 璋冭瘯`
4. 骞虫澘宸茬粡瀵瑰綋鍓?PC 瀹屾垚 USB 璋冭瘯鎺堟潈 / 淇′换

浣跨敤娴佺▼锛?
1. 鎵撳紑 Windows Receiver
2. 纭 `Server = Running`
3. 鐐瑰嚮 `USB Connect`
4. Receiver 鑷姩澶勭悊 USB / ADB 缂栨帓
5. Android 鑷姩鍒囨崲鍒?`127.0.0.1:8765` 骞惰繛涓?
鎵€浠ヨ繖浠朵簨璇寸櫧浜嗗氨鏄細濡傛灉 USB 璋冭瘯娌″紑锛屾垨鑰呰繖鍙扮數鑴戣繕娌¤骞虫澘淇′换锛孶SB Connect 灏变笉浼氭垚绔嬨€?
## 浠庢簮鐮佹瀯寤?
### Windows Receiver锛圵inUI锛屾帹鑽愶級

- PowerShell: `E:\work\repo\build\build_windows_winui.ps1`
- PowerShell 鑷寘鍚瀯寤? `E:\work\repo\build\build_windows_winui.ps1 -SelfContained`
- Batch: `E:\work\repo\build\build_windows_winui.bat`

榛樿杞婚噺杈撳嚭锛?
- `windows_receiver_winui/dist/`

鑷寘鍚緭鍑猴細

- `windows_receiver_winui/dist-self-contained/`

### Android App

- Debug 閿洏鍖? `E:\work\repo\build\build_android.ps1 -BuildType Debug -Module app`
- Release 閿洏鍖? `E:\work\repo\build\build_android.ps1 -BuildType Release -Module app`
- 宸茬鍚?Release APK: `E:\work\repo\build\sign_android_release.ps1 -Module app`

### 鏃х増 Python Receiver

鏃х増 Python Receiver 浠嶇劧淇濈暀鍦?`windows_receiver/` 閲岋紝涓嶈繃鐜板湪鏇存帹鑽愮洿鎺ョ敤 WinUI Receiver銆?
## 琛ュ厖璇存槑

- Touchpad 杩樺湪锛屽彧鏄繖娆′笉鏄富瑙?- 楹﹀厠椋庨摼璺篃杩樺湪锛屾部鐢ㄥ綋鍓?Alpha 鐨勫疄鐜版柟寮?- 鍔ㄤ綔鎵ц鑳藉姏寤鸿鍙湪鍙俊鐜閲屼娇鐢?