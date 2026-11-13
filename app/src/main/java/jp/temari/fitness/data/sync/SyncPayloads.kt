package jp.temari.fitness.data.sync

import jp.temari.fitness.data.sensing.ActivityLog
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.util.Locale

/**
 * 利用者の情報（Supabase の app_users 表の1行）。
 *
 * @property lastRecordedAt 端末で最後に記録した日時。まだなら null。
 */
data class UserInfo(
    val userId: String,
    val nickname: String,
    val dailyGoal: Int,
    val recording: Boolean,
    val lastRecordedAt: LocalDateTime?,
)

/**
 * Supabase に送る JSON を作る。
 *
 * JSON のライブラリ（org.json）は単体テストの環境では動かないので、ここでは文字を組み立てて作る。
 * こうしておくと、送る中身が正しいかを単体テストで確かめられる。
 *
 * @param zone 端末の時刻を、世界共通の時刻（タイムゾーンつき）に直すのに使う。
 */
class SyncPayloads(private val zone: ZoneId) {

    /** app_users 表に送る1行。 */
    fun user(info: UserInfo, now: LocalDateTime): String = obj(
        "id" to str(info.userId),
        "nickname" to str(info.nickname),
        "daily_goal" to info.dailyGoal.toString(),
        "recording" to info.recording.toString(),
        "last_recorded_at" to (info.lastRecordedAt?.let { str(timestamp(it)) } ?: "null"),
        "last_synced_at" to str(timestamp(now)),
    )

    /** block_records 表に送る行（[since] 以降の日の、時間帯ごとの集計）。 */
    fun blocks(userId: String, log: ActivityLog, since: LocalDate?): String = array(
        log.days
            .filterKeys { since == null || !it.isBefore(since) }
            .toSortedMap()
            .flatMap { (date, day) ->
                day.blocks.toSortedMap().map { (block, record) ->
                    obj(
                        "user_id" to str(userId),
                        "date" to str(date.toString()),
                        "block" to str(block.name),
                        "steps" to record.steps.toString(),
                        "life_seconds" to record.lifeSeconds.toString(),
                    )
                }
            },
    )

    /** daily_extras 表に送る行（[since] 以降の日の体操の回数）。 */
    fun extras(userId: String, log: ActivityLog, since: LocalDate?): String = array(
        log.days
            .filterKeys { since == null || !it.isBefore(since) }
            .filterValues { it.exercises > 0 }
            .toSortedMap()
            .map { (date, day) ->
                obj(
                    "user_id" to str(userId),
                    "date" to str(date.toString()),
                    "exercises" to day.exercises.toString(),
                )
            },
    )

    /** epochs 表に送る行。 */
    fun epochs(userId: String, records: List<EpochRecord>): String = array(
        records.map { r ->
            obj(
                "user_id" to str(userId),
                "recorded_at" to str(timestamp(r.at)),
                "steps" to r.steps.toString(),
                "life_seconds" to r.lifeSeconds.toString(),
                // 小数点はどの国の設定でも「.」にする（端末の言語によって「,」になるのを防ぐ）。
                "enmo_mg" to String.format(Locale.ROOT, "%.2f", r.enmoMg),
                "walking" to r.walking.toString(),
            )
        },
    )

    /** 端末の日時を「2026-11-13T10:00:10+09:00」の形にする。 */
    private fun timestamp(at: LocalDateTime): String = OffsetDateTime.of(at, zone.rules.getOffset(at)).toString()

    companion object {
        /** JSON の文字列（"..."）にする。特別な文字はエスケープする（よび名に「"」などが入っても壊れないように）。 */
        fun str(value: String): String = buildString {
            append('"')
            value.forEach { c ->
                when {
                    c == '"' -> append("\\\"")
                    c == '\\' -> append("\\\\")
                    c == '\n' -> append("\\n")
                    c == '\r' -> append("\\r")
                    c == '\t' -> append("\\t")
                    c < ' ' -> append(String.format(Locale.ROOT, "\\u%04x", c.code))
                    else -> append(c)
                }
            }
            append('"')
        }

        private fun obj(vararg fields: Pair<String, String>): String =
            fields.joinToString(",", "{", "}") { (k, v) -> "${str(k)}:$v" }

        private fun array(items: List<String>): String = items.joinToString(",", "[", "]")
    }
}
