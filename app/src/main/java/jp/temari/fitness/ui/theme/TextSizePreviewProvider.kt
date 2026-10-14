package jp.temari.fitness.ui.theme

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import jp.temari.fitness.config.TextSizeOption

/**
 * プレビュー用に「大」「特大」の2種類を渡すクラス。
 *
 * `@PreviewParameter(TextSizePreviewProvider::class)` を付けると、1つのプレビュー関数から2枚の絵が作られる。
 */
class TextSizePreviewProvider : PreviewParameterProvider<TextSizeOption> {
    override val values: Sequence<TextSizeOption> = TextSizeOption.entries.asSequence()
}
