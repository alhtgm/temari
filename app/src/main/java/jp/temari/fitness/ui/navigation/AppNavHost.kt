package jp.temari.fitness.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import jp.temari.fitness.ui.bingo.BingoScreen
import jp.temari.fitness.ui.home.HomeScreen
import jp.temari.fitness.ui.onboarding.OnboardingScreen
import jp.temari.fitness.ui.onboarding.OnboardingStep
import jp.temari.fitness.ui.review.ReviewScreen
import jp.temari.fitness.ui.settings.SettingsScreen
import jp.temari.fitness.ui.staff.StaffScreen

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
 */
@Composable
fun AppNavHost(navController: NavHostController = rememberNavController()) {
    // 初回設定を済ませたかどうかの保存は step07 で作る。今は常にホームから始める。
    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(
                onOpenReview = { navController.navigate(Routes.REVIEW) },
                onOpenBingo = { navController.navigate(Routes.BINGO) },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
            )
        }
        composable(Routes.REVIEW) {
            ReviewScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.BINGO) {
            BingoScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onOpenStaff = { navController.navigate(Routes.STAFF) },
            )
        }
        composable(Routes.STAFF) {
            StaffScreen(
                onBack = { navController.popBackStack() },
                onRestartOnboarding = {
                    navController.navigate(Routes.onboarding(OnboardingStep.Welcome))
                },
            )
        }
        // 初回設定の6画面は、段階ごとに別のルートにする（Android の戻る操作で1段階ずつ戻れるように）。
        OnboardingStep.entries.forEach { step ->
            composable(Routes.onboarding(step)) {
                OnboardingScreen(
                    step = step,
                    onNext = {
                        val next = step.next
                        if (next != null) {
                            navController.navigate(Routes.onboarding(next))
                        } else {
                            // 完了したらホームへ。それまでの画面は履歴から消し、戻る操作で初回設定に戻らないようにする。
                            navController.navigate(Routes.HOME) {
                                popUpTo(navController.graph.id) { inclusive = true }
                            }
                        }
                    },
                    onBack = { navController.popBackStack() },
                )
            }
        }
    }
}
