package jp.temari.fitness.ui.home

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
 * ホーム画面。アプリの中心で、ここから各画面へ1段だけ進む。
 *
 * step01 では行き先のボタンだけを置く。活動ポイントなどの表示は step03 で作る。
 */
@Composable
fun HomeScreen(
    onOpenReview: () -> Unit,
    onOpenBingo: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    ScreenFrame(
        title = stringResource(R.string.home_title),
        bottomBar = {
            // せってい は利用者がよく使うものではないので、控えめな見た目で一番下に置く。
            BigButton(
                text = stringResource(R.string.home_button_settings),
                onClick = onOpenSettings,
                primary = false,
            )
        },
    ) {
        BigButton(text = stringResource(R.string.home_button_review), onClick = onOpenReview)
        BigButton(text = stringResource(R.string.home_button_bingo), onClick = onOpenBingo)
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 780)
@Composable
private fun HomeScreenPreview(
    @PreviewParameter(TextSizePreviewProvider::class) textSize: TextSizeOption,
) {
    TemariTheme(textSize) {
        HomeScreen(onOpenReview = {}, onOpenBingo = {}, onOpenSettings = {})
    }
}
