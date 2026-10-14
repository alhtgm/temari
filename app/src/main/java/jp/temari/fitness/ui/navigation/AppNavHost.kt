package jp.temari.fitness.ui.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import jp.temari.fitness.ui.bingo.BingoRoute
import jp.temari.fitness.ui.home.HomeRoute
import jp.temari.fitness.ui.onboarding.OnboardingRoute
import jp.temari.fitness.ui.onboarding.OnboardingStep
import jp.temari.fitness.ui.review.ReviewRoute
import jp.temari.fitness.ui.settings.SettingsRoute
import jp.temari.fitness.ui.staff.StaffRoute

/** 画面の行き先の名前（ルート）。Navigation Compose はこの文字列で画面を区別する。 */
object Routes {
    const val HOME = "home"
    const val REVIEW = "review"
    const val BINGO = "bingo"
    const val SETTINGS = "settings"
    const val STAFF = "staff"

    /** 初回設定の各段階のルート。例: "onboarding/Name" */
    fun onboarding(step: OnboardingStep): String = "onboarding/${step.name}"
}

/**
 * アプリ全体の画面遷移。ホームを中心にしたハブ型（SPEC_UI.md 3章）。
 *
 * 「もどる」は popBackStack（ひとつ前の画面に戻る）で行うので、Android の戻る操作とも同じ動きになる。
 *
 * @param startDestination 最初に出す画面。初回設定が済んでいればホーム、まだなら初回設定の最初の画面。
 */
@Composable
fun AppNavHost(
    startDestination: String,
    navController: NavHostController = rememberNavController(),
) {
    NavHost(navController = navController, startDestination = startDestination) {
        composable(Routes.HOME) { entry ->
            HomeRoute(
                onOpenReview = { entry.ifResumed { navController.navigate(Routes.REVIEW) } },
                onOpenBingo = { entry.ifResumed { navController.navigate(Routes.BINGO) } },
                onOpenSettings = { entry.ifResumed { navController.navigate(Routes.SETTINGS) } },
            )
        }
        composable(Routes.REVIEW) { entry ->
            ReviewRoute(onBack = { entry.ifResumed { navController.popBackStack() } })
        }
        composable(Routes.BINGO) { entry ->
            BingoRoute(onBack = { entry.ifResumed { navController.popBackStack() } })
        }
        composable(Routes.SETTINGS) { entry ->
            SettingsRoute(
                onBack = { entry.ifResumed { navController.popBackStack() } },
                onOpenStaff = { entry.ifResumed { navController.navigate(Routes.STAFF) } },
            )
        }
        composable(Routes.STAFF) { entry ->
            StaffRoute(
                onBack = { entry.ifResumed { navController.popBackStack() } },
                onRestartOnboarding = {
                    // 設定を消したので、それまでの画面の履歴もすべて消して初回設定の最初から始める。
                    navController.navigate(Routes.onboarding(OnboardingStep.Welcome)) {
                        popUpTo(navController.graph.id) { inclusive = true }
                    }
                },
            )
        }
        // 初回設定の6画面は、段階ごとに別のルートにする（Android の戻る操作で1段階ずつ戻れるように）。
        OnboardingStep.entries.forEach { step ->
            composable(Routes.onboarding(step)) { entry ->
                OnboardingRoute(
                    step = step,
                    onNext = {
                        entry.ifResumed {
                            val next = step.next
                            if (next != null) {
                                navController.navigate(Routes.onboarding(next))
                            } else {
                                // 完了したらホームへ。履歴を消し、戻る操作で初回設定に戻らないようにする。
                                navController.navigate(Routes.HOME) {
                                    popUpTo(navController.graph.id) { inclusive = true }
                                }
                            }
                        }
                    },
                    onBack = { entry.ifResumed { navController.popBackStack() } },
                )
            }
        }
    }
}

/**
 * この画面が今いちばん手前に出ているときだけ [action] を行う。
 *
 * ボタンを素早く2回押すと、画面が切り替わる途中で2回目が届き、2画面進んだり戻りすぎたりする。
 * 高齢の利用者は2回押しが起きやすいので、切り替わりの途中（RESUMED でないとき）の操作は無視する。
 */
private inline fun NavBackStackEntry.ifResumed(action: () -> Unit) {
    if (lifecycle.currentState == Lifecycle.State.RESUMED) action()
}
