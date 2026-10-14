package jp.temari.fitness

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import jp.temari.fitness.config.TextSizeOption
import jp.temari.fitness.ui.navigation.AppNavHost
import jp.temari.fitness.ui.theme.TemariTheme

/**
 * アプリの入口となる画面（Activity）。
 *
 * Compose ではこの Activity が1つだけあり、画面の切り替えは [AppNavHost] が行う。
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Android 15 以降は画面の端まで描くのが標準なので、それに合わせる（余白は ScreenFrame 側で取る）。
        enableEdgeToEdge()
        val settingsFlow = (application as TemariApplication).container.settingsRepository.settings
        setContent {
            val settings by settingsFlow.collectAsStateWithLifecycle(initialValue = null)
            // 保存された文字の大きさをアプリ全体のテーマに渡す。せってい で変えるとすべての画面がすぐ変わる。
            TemariTheme(textSize = settings?.textSize ?: TextSizeOption.Large) {
                AppNavHost()
            }
        }
    }
}
