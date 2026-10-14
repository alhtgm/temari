package jp.temari.fitness.ui.staff

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import jp.temari.fitness.R
import jp.temari.fitness.config.TextSizeOption
import jp.temari.fitness.ui.components.BigButton
import jp.temari.fitness.ui.components.ScreenFrame
import jp.temari.fitness.ui.theme.TemariTheme
import jp.temari.fitness.ui.theme.TextSizePreviewProvider

/**
 * スタッフ・開発用画面。せってい画面の「スタッフ用」を長押しすると開く。
 *
 * step01 では「初回設定のやり直し」（初回設定の画面を順に見るための入口）と「もどる」だけを置く。
 * 確認ダイアログや保存データの消去は step07、シナリオ切り替えは step06 で作る。
 */
@Composable
fun StaffScreen(
    onBack: () -> Unit,
    onRestartOnboarding: () -> Unit,
) {
    ScreenFrame(
        title = stringResource(R.string.staff_title),
        bottomBar = {
            BigButton(text = stringResource(R.string.button_back), onClick = onBack)
        },
    ) {
        BigButton(
            text = stringResource(R.string.staff_button_restart_onboarding),
            onClick = onRestartOnboarding,
            primary = false,
        )
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 780)
@Composable
private fun StaffScreenPreview(
    @PreviewParameter(TextSizePreviewProvider::class) textSize: TextSizeOption,
) {
    TemariTheme(textSize) {
        StaffScreen(onBack = {}, onRestartOnboarding = {})
    }
}
