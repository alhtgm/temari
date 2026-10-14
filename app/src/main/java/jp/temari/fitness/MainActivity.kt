package jp.temari.fitness

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import jp.temari.fitness.config.TextSizeOption
import jp.temari.fitness.ui.navigation.AppNavHost
import jp.temari.fitness.ui.navigation.Routes
import jp.temari.fitness.ui.onboarding.OnboardingStep
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
            // 最初の画面は、設定を読み込んだ時点で1回だけ決める。
            // （あとで初回設定を終えたときに最初の画面が変わると、画面の履歴がおかしくなるため）
            var startDestination by remember { mutableStateOf<String?>(null) }
            val loaded = settings
            if (startDestination == null && loaded != null) {
                startDestination = if (loaded.onboardingCompleted) {
                    Routes.HOME
                } else {
                    Routes.onboarding(OnboardingStep.Welcome)
                }
            }
            // 保存された文字の大きさをアプリ全体のテーマに渡す。せってい で変えるとすべての画面がすぐ変わる。
            TemariTheme(textSize = loaded?.textSize ?: TextSizeOption.Large) {
                val start = startDestination
                if (start == null) {
                    // 設定を読み込むまでのほんの一瞬は、背景だけを出す。
                    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
                } else {
                    AppNavHost(startDestination = start)
                }
            }
        }
    }
}
