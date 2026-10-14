package jp.temari.fitness.data

import java.time.LocalDate

/**
 * ビンゴのマスの種類。並び順がそのままカードの位置（左上から右へ、3つごとに次の行）になる。
 *
 * マスの名前や説明の文字は画面側で strings.xml から取る（データの層に画面の文字を持たせないため）。
 */
enum class BingoCellId {
    MorningActive,
    GoalOnce,
    LifeMaster,
    NightLittle,
    Free,
    WalkGood,
    RecordStreak,
    GoalThreeTimes,
    EveryDay,
}

/**
 * ビンゴの1マス。
 *
 * @property achieved 達成したか（開いているか）。
 */
data class BingoCell(
    val id: BingoCellId,
    val achieved: Boolean,
)

/**
 * 1週間分のビンゴカード（3×3）。
 *
 * @property weekStart その週の月曜日。
 * @property cells 9マス。左上から右へ、行ごとに並ぶ。
 */
data class BingoCard(
    val weekStart: LocalDate,
    val cells: List<BingoCell>,
) {
    /** そろった列。1つの列は、そろったマスの番号（0〜8）3つで表す。 */
    val completedLines: List<List<Int>>
        get() = LINES.filter { line -> line.all { cells.getOrNull(it)?.achieved == true } }

    /** 1列でもそろっているか。 */
    val hasBingo: Boolean get() = completedLines.isNotEmpty()

    companion object {
        /** カードの1辺のマスの数。 */
        const val SIZE = 3

        /** 縦・横・ななめ、全部で8本の列。 */
        val LINES: List<List<Int>> = buildList {
            for (i in 0 until SIZE) {
                add(List(SIZE) { col -> i * SIZE + col }) // 横
                add(List(SIZE) { row -> row * SIZE + i }) // 縦
            }
            add(List(SIZE) { it * SIZE + it }) // 左上から右下
            add(List(SIZE) { it * SIZE + (SIZE - 1 - it) }) // 右上から左下
        }
    }
}
