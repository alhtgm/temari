package jp.temari.fitness.ui.bingo

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
 * ビンゴ画面。1週間で1枚の 3×3 のビンゴを見る。
 *
 * step01 ではタイトルと「もどる」だけを置く。ビンゴのマスは step05 で作る。
 */
@Composable
fun BingoScreen(onBack: () -> Unit) {
    ScreenFrame(
        title = stringResource(R.string.bingo_title),
        bottomBar = {
            BigButton(text = stringResource(R.string.button_back), onClick = onBack)
        },
    ) {
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 780)
@Composable
private fun BingoScreenPreview(
    @PreviewParameter(TextSizePreviewProvider::class) textSize: TextSizeOption,
) {
    TemariTheme(textSize) {
        BingoScreen(onBack = {})
    }
}
