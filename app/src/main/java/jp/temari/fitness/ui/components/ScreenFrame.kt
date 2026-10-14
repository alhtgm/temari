package jp.temari.fitness.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import jp.temari.fitness.config.UiConfig

/**
 * すべての画面で使う共通の枠。
 *
 * - 内容の幅は最大 480dp にして中央に寄せる（タブレットでもスマホに近い見た目にするため）。
 * - 上にタイトル、真ん中にスクロールできる内容、下に固定のボタン（「もどる」など）を置く。
 * - 下のボタンはスクロールしても動かないので、特大の文字で内容が長くなっても必ず押せる。
 *
 * @param title 画面のタイトル。null ならタイトルを出さない（ホームのあいさつなど、画面側で出す場合）。
 * @param bottomBar 画面下に固定するボタンなど。
 * @param content 画面の中身。
 */
@Composable
fun ScreenFrame(
    title: String?,
    modifier: Modifier = Modifier,
    bottomBar: @Composable ColumnScope.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            // 画面上のステータスバーや下のナビゲーションバーに内容が隠れないようにする。
            .safeDrawingPadding(),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = UiConfig.MaxContentWidth)
                .fillMaxWidth()
                .fillMaxHeight()
                .padding(UiConfig.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(UiConfig.ButtonSpacing),
        ) {
            if (title != null) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    // 読み上げ機能（TalkBack）に「見出し」だと伝える。
                    modifier = Modifier.semantics { heading() },
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(UiConfig.ButtonSpacing),
                content = content,
            )
            bottomBar()
        }
    }
}
