// プロジェクト全体の設定。プラグインのバージョンだけ宣言し、実際の適用は app 側で行う。
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
}
