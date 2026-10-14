package jp.temari.fitness.ui.onboarding

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import jp.temari.fitness.R
import jp.temari.fitness.config.ActivityConfig
import jp.temari.fitness.config.TextSizeOption
import jp.temari.fitness.ui.components.BigButton
import jp.temari.fitness.ui.components.GoalStepper
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
 * 初回設定の1段階ぶんの画面（ViewModel や Android の機能とつなぐ側）。
 *
 * 通知の許可や電池の設定のように Android の画面を呼ぶ処理はここで行い、表示は [OnboardingScreen] に任せる。
 *
 * @param onNext つぎの段階へ進む（完了画面ではホームへ進む）。保存などを済ませてから呼ぶ。
 * @param onBack 1つ前の段階へ戻る。
 */
@Composable
fun OnboardingRoute(
    step: OnboardingStep,
    onNext: () -> Unit,
    onBack: () -> Unit,
    viewModel: OnboardingViewModel = viewModel(factory = OnboardingViewModel.Factory),
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // 呼び名の入力中の文字。保存済みの呼び名が読み込まれたら、そこから始める。
    var nickname by rememberSaveable(settings?.nickname) { mutableStateOf(settings?.nickname.orEmpty()) }

    // 通知の許可を求める。許可されても断られても次へ進む（仕様: 断られても先に進める）。
    // 許可のダイアログから戻った直後は画面がまだ操作できる状態（RESUMED）になっていないので、
    // 答えが出たことだけを覚えておき、画面が戻ってきてから（LifecycleResumeEffect）進む。
    var notificationAnswered by remember { mutableStateOf(false) }
    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { notificationAnswered = true }
    LifecycleResumeEffect(notificationAnswered) {
        if (notificationAnswered) {
            notificationAnswered = false
            onNext()
        }
        onPauseOrDispose { }
    }

    // 電池の設定画面から戻ってきたら、外せたかどうかを確かめ直す。
    var batteryExempt by remember { mutableStateOf(isIgnoringBatteryOptimizations(context)) }
    val batteryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { batteryExempt = isIgnoringBatteryOptimizations(context) }

    OnboardingScreen(
        step = step,
        nickname = nickname,
        onNicknameChange = { nickname = it.take(ActivityConfig.NICKNAME_MAX_LENGTH) },
        notificationGranted = isNotificationGranted(context),
        batteryExempt = batteryExempt,
        onOpenBatterySettings = {
            try {
                // この画面では「許可」を押すだけで外せる（パッケージ名でこのアプリを指定する）。
                batteryLauncher.launch(
                    Intent(
                        Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                        "package:${context.packageName}".toUri(),
                    ),
                )
            } catch (_: ActivityNotFoundException) {
                // 端末によっては上の画面が無いので、電池の最適化の一覧画面を開く。
                batteryLauncher.launch(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
            }
        },
        goal = settings?.dailyGoal ?: ActivityConfig.DEFAULT_DAILY_GOAL,
        onGoalChange = viewModel::onGoalChange,
        onNext = {
            when (step) {
                OnboardingStep.Name -> viewModel.onNameSubmit(nickname, onNext)
                OnboardingStep.Notification ->
                    if (needsNotificationRequest(context)) {
                        notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        onNext()
                    }
                OnboardingStep.Done -> viewModel.onComplete(onNext)
                else -> onNext()
            }
        },
        onBack = onBack,
    )
}

/**
 * 初回設定の1段階ぶんの画面（表示だけ）。6つの段階を同じ形の画面で表す。
 *
 * @param notificationGranted 通知がすでに許可されているか。
 * @param batteryExempt 電池の最適化からすでに外れているか。
 */
@Composable
fun OnboardingScreen(
    step: OnboardingStep,
    nickname: String,
    onNicknameChange: (String) -> Unit,
    notificationGranted: Boolean,
    batteryExempt: Boolean,
    onOpenBatterySettings: () -> Unit,
    goal: Int,
    onGoalChange: (Int) -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit,
) {
    val nextLabel = when {
        step == OnboardingStep.Done -> R.string.onboarding_button_start
        // 電池の設定がまだなら「あとで」として進める。
        step == OnboardingStep.Battery && !batteryExempt -> R.string.onboarding_button_later
        else -> R.string.button_next
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
        when (step) {
            OnboardingStep.Welcome -> {
                BodyText(R.string.onboarding_welcome_line1)
                BodyText(R.string.onboarding_welcome_line2)
                BodyText(R.string.onboarding_welcome_line3)
            }
            OnboardingStep.Name -> {
                BodyText(R.string.onboarding_name_text)
                OutlinedTextField(
                    value = nickname,
                    onValueChange = onNicknameChange,
                    textStyle = MaterialTheme.typography.bodyLarge,
                    placeholder = {
                        Text(
                            stringResource(R.string.settings_nickname_placeholder),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            OnboardingStep.Notification -> {
                BodyText(R.string.onboarding_notification_text)
                if (notificationGranted) DoneText(R.string.onboarding_notification_granted)
            }
            OnboardingStep.Battery -> {
                BodyText(R.string.onboarding_battery_text)
                if (batteryExempt) {
                    DoneText(R.string.onboarding_battery_done)
                } else {
                    BigButton(
                        text = stringResource(R.string.onboarding_battery_button),
                        onClick = onOpenBatterySettings,
                        primary = false,
                    )
                }
            }
            OnboardingStep.Goal -> {
                BodyText(R.string.onboarding_goal_text)
                Text(
                    text = stringResource(R.string.onboarding_goal_recommended, ActivityConfig.DEFAULT_DAILY_GOAL),
                    style = MaterialTheme.typography.bodyLarge,
                )
                GoalStepper(goal = goal, onGoalChange = onGoalChange)
            }
            OnboardingStep.Done -> BodyText(R.string.onboarding_done_text)
        }
    }
}

@Composable
private fun BodyText(@StringRes text: Int) {
    Text(text = stringResource(text), style = MaterialTheme.typography.bodyLarge)
}

/** 済んでいることを伝える文字（緑と ✓）。 */
@Composable
private fun DoneText(@StringRes text: Int) {
    Text(
        text = stringResource(text),
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.secondary,
    )
}

/** 通知が許可されているか。Android 12 以前は許可が要らないので常に true。 */
private fun isNotificationGranted(context: Context): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
        PackageManager.PERMISSION_GRANTED

/** 通知の許可を求める必要があるか（Android 13 以上で、まだ許可されていない）。 */
private fun needsNotificationRequest(context: Context): Boolean = !isNotificationGranted(context)

/** このアプリが電池の最適化から外れているか。 */
private fun isIgnoringBatteryOptimizations(context: Context): Boolean =
    context.getSystemService(PowerManager::class.java)?.isIgnoringBatteryOptimizations(context.packageName) == true

/** プレビュー用: 指定した段階の画面を出す。 */
@Composable
private fun PreviewStep(step: OnboardingStep) {
    OnboardingScreen(
        step = step,
        nickname = "はなこ",
        onNicknameChange = {},
        notificationGranted = false,
        batteryExempt = false,
        onOpenBatterySettings = {},
        goal = ActivityConfig.DEFAULT_DAILY_GOAL,
        onGoalChange = {},
        onNext = {},
        onBack = {},
    )
}

@Preview(showBackground = true, widthDp = 360, heightDp = 780, name = "ようこそ")
@Composable
private fun OnboardingWelcomePreview(
    @PreviewParameter(TextSizePreviewProvider::class) textSize: TextSizeOption,
) {
    TemariTheme(textSize) { PreviewStep(OnboardingStep.Welcome) }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 780, name = "よび名")
@Composable
private fun OnboardingNamePreview(
    @PreviewParameter(TextSizePreviewProvider::class) textSize: TextSizeOption,
) {
    TemariTheme(textSize) { PreviewStep(OnboardingStep.Name) }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 780, name = "電池の設定")
@Composable
private fun OnboardingBatteryPreview(
    @PreviewParameter(TextSizePreviewProvider::class) textSize: TextSizeOption,
) {
    TemariTheme(textSize) { PreviewStep(OnboardingStep.Battery) }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 780, name = "1日の目標")
@Composable
private fun OnboardingGoalPreview(
    @PreviewParameter(TextSizePreviewProvider::class) textSize: TextSizeOption,
) {
    TemariTheme(textSize) { PreviewStep(OnboardingStep.Goal) }
}
