# step01: 土台

## 1. やったこと
- Android プロジェクト（Kotlin / Compose / Material 3）を作り、SPEC_UI.md 3章の画面をすべて「タイトル＋ボタンだけ」の空の状態で用意した。
- Navigation Compose でホーム中心のハブ型の画面遷移を作り、すべての画面に大きな「もどる」を置いた。
- 高齢者向けのテーマ（高コントラストの色、文字サイズ 大／特大、内容の最大幅 480dp、縦向き固定）を作った。

## 2. 追加・変更したファイルと役割
| ファイル | 役割 |
|---|---|
| `settings.gradle.kts` / `build.gradle.kts` / `app/build.gradle.kts` | ビルドの設定。minSdk 30、targetSdk 36、compileSdk 37 |
| `gradle/libs.versions.toml` | ライブラリのバージョン一覧 |
| `gradle.properties` | ビルドの細かい設定（日本語を含むパスでもビルドできるようにする設定を含む） |
| `gradlew` / `gradlew.bat` / `gradle/wrapper/` | Gradle を自動で用意して実行するスクリプト（Gradle 9.8.1） |
| `.gitignore` | Git に入れないファイル（`local.properties`、ビルド結果など） |
| `app/src/main/AndroidManifest.xml` | アプリの宣言。縦向き固定とタブレットでも固定を効かせる設定 |
| `app/src/main/res/values/strings.xml` | 画面に出す文字列すべて |
| `MainActivity.kt` | アプリの入口。テーマと画面遷移を表示する |
| `config/UiConfig.kt` | 最大幅 480dp、ボタン高さ 64dp、間隔 16dp、長押し 2 秒 などの定数 |
| `config/TextSizeConfig.kt` | 文字の大きさの定数と、大／特大の設定（`TextSizeOption`、特大は 1.25 倍） |
| `ui/theme/Color.kt` / `Type.kt` / `Theme.kt` | 色、文字スタイル、テーマ（`TemariTheme`） |
| `ui/theme/TextSizePreviewProvider.kt` | プレビューを 大／特大 の2種類で出すための部品 |
| `ui/components/ScreenFrame.kt` | 全画面共通の枠（タイトル・スクロールできる中身・下に固定のボタン・最大幅 480dp） |
| `ui/components/BigButton.kt` | 高さ 64dp 以上・文字付きの大きなボタン |
| `ui/navigation/AppNavHost.kt` | 画面遷移の定義（ルート名 `Routes`） |
| `ui/home/HomeScreen.kt` | ホーム（ふりかえり・ビンゴ・せってい へのボタン） |
| `ui/review/ReviewScreen.kt` | ふりかえり（空） |
| `ui/bingo/BingoScreen.kt` | ビンゴ（空） |
| `ui/settings/SettingsScreen.kt` | せってい（空）＋「スタッフ用」の 2 秒長押し |
| `ui/staff/StaffScreen.kt` | スタッフ・開発用（「初回設定のやり直し」ボタンのみ） |
| `ui/onboarding/OnboardingScreen.kt` | 初回設定 6 画面（ようこそ→よび名→お知らせの許可→電池の設定→1日の目標→じゅんびができました） |
| `docs/SPEC_UI.md` | CLAUDE.md の参照先に合わせて、ルートから `docs/` へ移動（中身は変更なし） |

## 3. 実機で確認する手順と、期待される見た目・動き
インストールは開発者の指示があったときに行う。APK は `app/build/outputs/apk/debug/app-debug.apk`。
（例: `adb install -r app/build/outputs/apk/debug/app-debug.apk`。adb は `%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe`）

1. アプリを開く → 「ホーム」のタイトルと、青い「ふりかえり」「ビンゴ」、一番下に白い枠だけの「せってい」が出る。
2. タブレットでは、内容が画面の中央に幅 480dp ほどで寄っている。横に回しても縦向きのまま。
3. 「ふりかえり」→ タイトルと下に「もどる」。「もどる」でホームへ。「ビンゴ」も同じ。
4. 「せってい」→ 下に「もどる」と、その下に灰色の「スタッフ用」。
   - 「スタッフ用」を軽く押す・1 秒で離す → 何も起きない。
   - 2 秒押し続ける → 「スタッフ・開発用」画面へ。「もどる」で せってい に戻る。
5. スタッフ・開発用 の「初回設定のやり直し」→ ようこそ →（つぎへ）よび名 → お知らせの許可 → 電池の設定 → 1日の目標 → じゅんびができました。
   - ようこそ 以外には「もどる」があり、1 つ前に戻る。
   - 最後の「はじめましょう」でホームへ。ここで Android の戻る操作をするとアプリが閉じる（初回設定には戻らない）。
6. 端末の文字サイズ設定を最大にしても、ボタンの文字が切れずにボタンが縦に伸びる。

## 4. 判断を保留した点
- **ビルド環境**: PC に Android SDK が無かったため、Google のコマンドラインツールで `%LOCALAPPDATA%\Android\Sdk` に導入した（platform 37 / build-tools / platform-tools）。Android Studio を後で入れても同じ場所を使える。
- **compileSdk 37**: CLAUDE.md の「テンプレートの既定値」に合わせて最新の 37 にした。targetSdk は指定どおり 36。
- **ライブラリのバージョン**: 2026年10月時点の安定版（AGP 9.4.1、Kotlin 2.4.21、Compose BOM 2026.09.00、Navigation 2.10.2）。
- **タブレットでの縦向き固定**: Android 16 は targetSdk 36 のアプリでも大きい画面で縦向き固定を無視する。マニフェストの `PROPERTY_COMPAT_ALLOW_RESTRICTED_RESIZABILITY` で一時的に回避した。targetSdk を 37 に上げると効かなくなる。
- **初回設定への入口**: 初回設定の保存は step07 なので、今は起動すると常にホームが出る。初回設定の画面を確認できるよう、仕様 3.5 にある「初回設定のやり直し」ボタンを先に置いた（確認ダイアログと保存データの消去は step07）。
- **ViewModel**: まだ表示するデータが無いので `XxxViewModel.kt` は作っていない。データを扱う step03 以降で各画面に追加する。
- **スクロール**: 特大＋端末の文字サイズ最大で画面に入りきらないときのため、画面の中身はスクロールできるようにした（「もどる」などの下のボタンは固定で常に見える）。利用者にスワイプを求めないよう、各画面の中身は1画面に収まる量にする方針。
- **アプリ名**: 仕様に無かったので仮に「てまりフィットネス」とした。アイコンもまだ無い（Android 標準のアイコンが出る）。
- **色**: ダークモードと端末の壁紙に合わせた色（ダイナミックカラー）は使わず、常に白背景・黒文字にした。
- **呼び名の画面のタイトル**: やさしい言葉にするため「よび名」、通知は「お知らせの許可」とした。
- **OneDrive 上のプロジェクト**: ビルド中に OneDrive の同期でファイルが掴まれてビルドが失敗することがある。起きたら OneDrive の外（例: `C:\dev\temari`）に移すことを検討する。
