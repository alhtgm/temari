package jp.temari.fitness.ui.onboarding

import androidx.annotation.StringRes
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
 * 初回設定の各段階。並び順がそのまま画面の順番になる。
 *
 * @property titleRes 画面のタイトルの文字列。
 */
enum class OnboardingStep(@StringRes val titleRes: Int) {
    Welcome(R.string.onboarding_welcome_title),
    Name(R.string.onboarding_name_title),
    Notification(R.string.onboarding_notification_title),
    Battery(R.string.onboarding_battery_title),
    Goal(R.string.onboarding_goal_title),
    Done(R.string.onboarding_done_title),
    ;

    /** つぎの段階。最後（完了）なら null。 */
    val next: OnboardingStep? get() = entries.getOrNull(ordinal + 1)
}

/**
 * 初回設定の1段階ぶんの画面。6つの段階を同じ形の画面で表す。
 *
 * step01 ではタイトルとボタンだけを置く。名前の入力や通知の許可などは step07 で作る。
 *
 * @param onNext 「つぎへ」（完了画面では「はじめましょう」）を押したときの処理。
 * @param onBack 「もどる」を押したときの処理。最初の画面（ようこそ）では「もどる」を出さない。
 */
@Composable
fun OnboardingScreen(
    step: OnboardingStep,
    onNext: () -> Unit,
    onBack: () -> Unit,
) {
    val nextLabel = if (step == OnboardingStep.Done) {
        R.string.onboarding_button_start
    } else {
        R.string.button_next
    }
    ScreenFrame(
        title = stringResource(step.titleRes),
        bottomBar = {
            BigButton(text = stringResource(nextLabel), onClick = onNext)
            if (step != OnboardingStep.Welcome) {
                BigButton(
                    text = stringResource(R.string.button_back),
                    onClick = onBack,
                    primary = false,
                )
            }
        },
    ) {
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 780)
@Composable
private fun OnboardingScreenPreview(
    @PreviewParameter(TextSizePreviewProvider::class) textSize: TextSizeOption,
) {
    TemariTheme(textSize) {
        OnboardingScreen(step = OnboardingStep.Name, onNext = {}, onBack = {})
    }
}
