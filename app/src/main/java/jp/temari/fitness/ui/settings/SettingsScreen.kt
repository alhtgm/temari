package jp.temari.fitness.ui.settings

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import jp.temari.fitness.R
import jp.temari.fitness.config.TextSizeOption
import jp.temari.fitness.config.UiConfig
import jp.temari.fitness.ui.components.BigButton
import jp.temari.fitness.ui.components.ScreenFrame
import jp.temari.fitness.ui.theme.TemariTheme
import jp.temari.fitness.ui.theme.TextSizePreviewProvider

/**
 * せってい画面（利用者向け）。
 *
 * step01 ではタイトル・「もどる」・スタッフ用の入口（2秒の長押し）だけを置く。
 * 文字の大きさ・呼び名・目標の変更は step06 で作る。
 */
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenStaff: () -> Unit,
) {
    ScreenFrame(
        title = stringResource(R.string.settings_title),
        bottomBar = {
            BigButton(text = stringResource(R.string.button_back), onClick = onBack)
            Text(
                text = stringResource(R.string.settings_staff_entry),
                style = MaterialTheme.typography.bodySmall,
                // 利用者の目に留まりにくいよう、灰色で目立たなくする。
                color = MaterialTheme.colorScheme.outline,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(UiConfig.ScreenPadding)
                    .longPressFor(UiConfig.STAFF_LONG_PRESS_MILLIS, onOpenStaff),
            )
        },
    ) {
    }
}

/**
 * 指を [millis] ミリ秒 離さずに押し続けたときだけ [onLongPress] を呼ぶ。
 *
 * Compose 標準の長押しは 0.5 秒ほどで反応してしまい、利用者がうっかりスタッフ用画面に入るおそれがあるため自作する。
 */
private fun Modifier.longPressFor(millis: Long, onLongPress: () -> Unit): Modifier =
    pointerInput(millis, onLongPress) {
        awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false)
            // 時間内に指が離れた（またはずれた）ら released が true になる。時間切れなら false のまま。
            var released = false
            withTimeoutOrNull(millis) {
                waitForUpOrCancellation()
                released = true
            }
            if (!released) onLongPress()
        }
    }

@Preview(showBackground = true, widthDp = 360, heightDp = 780)
@Composable
private fun SettingsScreenPreview(
    @PreviewParameter(TextSizePreviewProvider::class) textSize: TextSizeOption,
) {
    TemariTheme(textSize) {
        SettingsScreen(onBack = {}, onOpenStaff = {})
    }
}
