package jp.temari.fitness.data.sync

import jp.temari.fitness.config.SyncConfig
import jp.temari.fitness.data.sensing.EpochResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDateTime

/**
 * まだ Supabase に送っていない、10 秒ごとの記録1件。
 *
 * @property at 区切りを締めた日時（端末の時刻）。
 */
data class EpochRecord(
    val at: LocalDateTime,
    val steps: Int,
    val lifeSeconds: Int,
    val enmoMg: Float,
    val walking: Boolean,
) {
    companion object {
        /** 判別の結果から作る。 */
        fun of(at: LocalDateTime, result: EpochResult) =
            EpochRecord(at, result.steps, result.lifeSeconds, result.meanEnmoMg, result.walking)
    }
}

/**
 * 送る前の 10 秒ごとの記録をためておく待ち行列。ネットにつながらない間もためておき、つながったら古い順に送る。
 *
 * アプリが止まっても消えないよう、ファイルにも書き出す（1行1件のカンマ区切り）。
 *
 * @param file 保存するファイル。null ならファイルに保存しない（テスト用）。
 * @param maxSize ためておく最大の件数。超えたら古いものから捨てる。
 */
class EpochQueue(
    private val file: File?,
    private val maxSize: Int = SyncConfig.MAX_PENDING_EPOCHS,
) {
    // 記録するサービス（メインスレッド）と送信（別のスレッド）の両方から触るので、触るときは lock で順番にする。
    private val lock = Any()
    private val items: ArrayDeque<EpochRecord> = ArrayDeque(load())

    /** ためている件数。 */
    val size: Int get() = synchronized(lock) { items.size }

    /** 1件足す。上限を超えたら、いちばん古いものを捨てる。 */
    fun add(record: EpochRecord) = synchronized(lock) {
        items.addLast(record)
        while (items.size > maxSize) items.removeFirst()
    }

    /** 古い順に最大 [count] 件を返す（取り出さない。送れたら [removeFirst] で消す）。 */
    fun peek(count: Int): List<EpochRecord> = synchronized(lock) { items.take(count) }

    /** 古い順に [count] 件を消す（送れた分）。 */
    fun removeFirst(count: Int) = synchronized(lock) {
        repeat(minOf(count, items.size)) { items.removeFirst() }
    }

    /** ファイルに書き出す。 */
    suspend fun save() {
        val target = file ?: return
        val text = synchronized(lock) { items.joinToString("\n") { encode(it) } }
        withContext(Dispatchers.IO) { writeAtomically(target, text) }
    }

    private fun load(): List<EpochRecord> {
        val source = file ?: return emptyList()
        if (!source.exists()) return emptyList()
        // 読めない行（途中で止まって壊れた行など）は飛ばす。
        return source.readLines().mapNotNull { decode(it) }.takeLast(maxSize)
    }

    companion object {
        /** 1件を1行の文字にする。例: `2026-11-13T10:00:10,12,0,31.5,1` */
        fun encode(r: EpochRecord): String =
            "${r.at},${r.steps},${r.lifeSeconds},${r.enmoMg},${if (r.walking) 1 else 0}"

        /** [encode] の文字を読み戻す。読めなければ null。 */
        fun decode(line: String): EpochRecord? = runCatching {
            val p = line.trim().split(',')
            EpochRecord(LocalDateTime.parse(p[0]), p[1].toInt(), p[2].toInt(), p[3].toFloat(), p[4] == "1")
        }.getOrNull()
    }
}

/**
 * いったん別のファイルに書いてから名前を替える。書いている途中でアプリが止まっても、前の内容が壊れないように。
 */
internal fun writeAtomically(target: File, text: String) {
    val temp = File(target.parentFile, target.name + ".tmp")
    temp.writeText(text)
    if (!temp.renameTo(target)) {
        target.delete()
        temp.renameTo(target)
    }
}
