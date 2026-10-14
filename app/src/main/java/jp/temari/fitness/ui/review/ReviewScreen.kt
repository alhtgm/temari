package jp.temari.fitness.ui.review

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
 * ふりかえり画面。きょう／今週 の活動をグラフで見る。
 *
 * step01 ではタイトルと「もどる」だけを置く。グラフは step04 で作る。
 */
@Composable
fun ReviewScreen(onBack: () -> Unit) {
    ScreenFrame(
        title = stringResource(R.string.review_title),
        bottomBar = {
            BigButton(text = stringResource(R.string.button_back), onClick = onBack)
        },
    ) {
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 780)
@Composable
private fun ReviewScreenPreview(
    @PreviewParameter(TextSizePreviewProvider::class) textSize: TextSizeOption,
) {
    TemariTheme(textSize) {
        ReviewScreen(onBack = {})
    }
}
