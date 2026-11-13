package jp.temari.fitness.config

/**
 * Supabase へデータを送る（同期する）ときの定数。
 */
object SyncConfig {
    /** 記録中に自動で送る間隔（ミリ秒）。15 分。短くすると Web 画面が新しくなるが、電池と通信量を使う。 */
    const val SYNC_INTERVAL_MILLIS = 15 * 60 * 1_000L

    /** 1回の通信で送る 10 秒ごとの記録の最大件数。多すぎると1回の通信が重くなる。 */
    const val EPOCHS_PER_REQUEST = 500

    /**
     * 送れずにためておく 10 秒ごとの記録の上限（件）。約 7 日分。
     * ずっとネットにつながらないときに、端末の容量を使いすぎないよう、古いものから捨てる。
     */
    const val MAX_PENDING_EPOCHS = 60_000

    /** 集計（時間帯ごと・体操）を送り直す日数。今日と昨日の分は、送ったあとも変わることがあるので毎回送る。 */
    const val RESEND_RECENT_DAYS = 1L

    /** 通信がつながるまで・返事が来るまで待つ時間（ミリ秒）。 */
    const val TIMEOUT_MILLIS = 20_000
}
