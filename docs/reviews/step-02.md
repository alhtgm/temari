# step02: データの層とモック

## 1. やったこと
- SPEC_UI.md 4章のデータモデル（1日の活動・時間帯ごとの活動・記録の状態・ビンゴカード）と `ActivityRepository` を作った。
- 6章の5つのシナリオを返す `FakeActivityRepository` と、ビンゴの判定（`BingoRules`）を作った。
- シナリオごとの値とビンゴの判定を確かめる単体テスト 17 件を書き、`./gradlew test` が通ることを確認した。

## 2. 追加・変更したファイルと役割
| ファイル | 役割 |
|---|---|
| `config/ActivityConfig.kt` | 時間帯の区切り、歩数→ポイントの換算（÷10・仮）、目標の初期値 600（仮）、目標の増減幅・上下限、「あと少し」の割合 80% |
| `config/BingoConfig.kt` | ビンゴの「一定以上」のしきい値（すべて仮の値） |
| `data/TimeBlock.kt` | 朝・昼・午後・夜 の時間帯 |
| `data/ActivityModels.kt` | `TimeBlockActivity`、`DailyActivity`、`RecordingStatus` |
| `data/BingoModels.kt` | `BingoCellId`（9マスの種類と並び）、`BingoCell`、`BingoCard`（そろった列の計算つき） |
| `data/BingoRules.kt` | 1週間の活動から各マスが開いたかを決める計算 |
| `data/ActivityRepository.kt` | 画面がデータを受け取る唯一の窓口（インターフェース） |
| `data/fake/MockScenario.kt` | 5つのシナリオと、シナリオを切り替える窓口 `ScenarioSwitcher` |
| `data/fake/MockProfiles.kt` | 各シナリオの数字（今日の割合、時間帯の山、今週の各日の割合） |
| `data/fake/FakeActivityRepository.kt` | モックの実装。時計と目標を外から受け取る |
| `test/.../FakeActivityRepositoryTest.kt` | シナリオごとの値のテスト（10件） |
| `test/.../BingoRulesTest.kt` | ビンゴ判定・時間帯のテスト（7件） |
| `gradle/libs.versions.toml`、`app/build.gradle.kts` | テスト用に JUnit 4 と kotlinx-coroutines-test を追加（開発者の了承済み） |

## 3. 実機で確認する手順と、期待される見た目・動き
このステップは画面に組み込まないので、実機での見た目の変化はない。
- `./gradlew test` を実行 → `BUILD SUCCESSFUL`。
- 結果は `app/build/reports/tests/testDebugUnitTest/index.html` をブラウザで開くと一覧で見られる（17件すべて成功）。

## 4. 判断を保留した点
- **`BingoCell` の title / description を持たせなかった**: 仕様の例では `BingoCell(id, title, description, achieved)` だが、画面の文字はすべて strings.xml に置くルールがあるため、データには種類（`BingoCellId`）だけを持たせ、名前と説明は画面側で strings.xml から取ることにした。
- **`weekActivities()` は記録が取れた日だけを返す**: 「3日つづけて記録」「毎日コツコツ」を判定するため、記録が無い日はリストに入れない（活動 0 の日とは区別する）。グラフ側では無い日を 0 として描く。
- **シナリオ切り替えの窓口を分けた**: 本物の計測にはシナリオが無いので、`ActivityRepository` には入れず `ScenarioSwitcher` として分けた。スタッフ・開発用画面だけが使う。
- **`resumeRecording()` を `ActivityRepository` に追加**: ホームの「再開する」ボタンのため。
- **今日のモックデータと時刻**: まだ来ていない時間帯にはデータを入れない（8時なら朝だけ）。そのうえで合計はシナリオどおり（例: 目標の 70%）になるよう、始まった時間帯に割り振り直している。0〜5時はどの時間帯にも入らないので、画面確認のため1日分を出す。
- **シナリオの数字は木曜を想定して選んだ**: 「ふつうの日でビンゴがそろわない」「よく動いた日でそろう」はテストで木曜 15時に固定して確かめている。曜日によっては（例: 月曜は過去の日が無いので）開くマスの数が変わる。
- **「よく動いた日」は複数の列がそろう**: 仕様は「1列そろう」なので、少なくとも1列そろうことをテストしている。
- **しきい値はすべて仮**（`BingoConfig` のコメントにも明記）。
