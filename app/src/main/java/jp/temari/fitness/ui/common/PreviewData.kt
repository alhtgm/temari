package jp.temari.fitness.ui.common

import jp.temari.fitness.data.ActivityRepository
import jp.temari.fitness.data.fake.FakeActivityRepository
import jp.temari.fitness.data.fake.MockScenario
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId

/**
 * プレビュー（Android Studio で画面を確かめる機能）で使うデータ。
 *
 * モックのリポジトリを、固定した日時（2026年10月15日 木曜 15時）で動かして作る。
 */
object PreviewData {
    private val zone: ZoneId = ZoneId.of("Asia/Tokyo")

    /** プレビューで「今」とみなす日時の時計。 */
    val clock: Clock = Clock.fixed(
        LocalDate.of(2026, 10, 15).atTime(15, 0).atZone(zone).toInstant(),
        zone,
    )

    /** [scenario] のデータを返すリポジトリ。 */
    fun repository(scenario: MockScenario = MockScenario.Normal): ActivityRepository =
        FakeActivityRepository(clock).apply { setScenario(scenario) }
}
