package jp.temari.fitness.data.sync

import jp.temari.fitness.config.SyncConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * Supabase にログインした状態（セッション）。
 *
 * @property accessToken 通信のたびに付ける「入場券」。1 時間ほどで期限が切れる。
 * @property refreshToken 期限が切れた入場券を新しくするための券。
 * @property expiresAtEpochSeconds 入場券の期限（1970年からの秒数）。
 */
data class Session(
    val accessToken: String,
    val refreshToken: String,
    val expiresAtEpochSeconds: Long,
)

/**
 * Supabase との通信でエラーが返ってきたとき。
 *
 * @property status HTTP の状態番号（401 = 入場券が切れている、403 = 決まりで断られた など）。
 */
class SupabaseException(val status: Int, message: String) : IOException(message)

/**
 * Supabase との通信の窓口。テストでは本物の通信をしない偽物に差し替える。
 */
interface SupabaseApi {
    /** 匿名でログインして、新しいセッションを作る。 */
    suspend fun signInAnonymously(): Session

    /** 期限が切れそうなセッションを新しくする。 */
    suspend fun refresh(refreshToken: String): Session

    /**
     * 表に行を足す。同じキーの行がすでにあれば上書きする（upsert）。
     *
     * @param onConflict 同じ行かどうかを決める列（カンマ区切り）。
     * @param json 送る行の JSON 配列（またはオブジェクト1つ）。
     */
    suspend fun upsert(session: Session, table: String, onConflict: String, json: String)
}

/**
 * Android に最初から入っている HttpURLConnection で Supabase と通信する [SupabaseApi]。
 *
 * 新しいライブラリを入れないために、Supabase の公式のライブラリは使わず、必要な3つの通信だけを自分で書いている。
 * - ログイン: `POST /auth/v1/signup`（何も入れずに送ると匿名ログインになる）
 * - 入場券の更新: `POST /auth/v1/token?grant_type=refresh_token`
 * - 書き込み: `POST /rest/v1/<表>?on_conflict=<列>`（PostgREST という、表を Web から読み書きするしくみ）
 *
 * @param baseUrl Supabase の URL（例: https://xxxx.supabase.co）。
 * @param publishableKey 公開用の鍵（sb_publishable_...）。端末に入れてよい鍵。
 */
class HttpSupabaseApi(
    private val baseUrl: String,
    private val publishableKey: String,
) : SupabaseApi {

    override suspend fun signInAnonymously(): Session =
        parseSession(request("POST", "/auth/v1/signup", body = "{}", token = null, extraHeaders = emptyMap()))

    override suspend fun refresh(refreshToken: String): Session = parseSession(
        request(
            "POST",
            "/auth/v1/token?grant_type=refresh_token",
            body = "{\"refresh_token\":${SyncPayloads.str(refreshToken)}}",
            token = null,
            extraHeaders = emptyMap(),
        ),
    )

    override suspend fun upsert(session: Session, table: String, onConflict: String, json: String) {
        request(
            "POST",
            "/rest/v1/$table?on_conflict=$onConflict",
            body = json,
            token = session.accessToken,
            // merge-duplicates: 同じキーの行は上書きする。return=minimal: 書いた行を送り返さない（通信量を減らす）。
            extraHeaders = mapOf("Prefer" to "resolution=merge-duplicates,return=minimal"),
        )
    }

    private suspend fun request(
        method: String,
        path: String,
        body: String,
        token: String?,
        extraHeaders: Map<String, String>,
    ): String = withContext(Dispatchers.IO) {
        val connection = URL(baseUrl.trimEnd('/') + path).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = method
            connection.connectTimeout = SyncConfig.TIMEOUT_MILLIS
            connection.readTimeout = SyncConfig.TIMEOUT_MILLIS
            connection.doOutput = true
            connection.setRequestProperty("apikey", publishableKey)
            connection.setRequestProperty("Content-Type", "application/json")
            // 入場券があれば付ける（ログインと更新のときは無い）。
            if (token != null) connection.setRequestProperty("Authorization", "Bearer $token")
            extraHeaders.forEach { (k, v) -> connection.setRequestProperty(k, v) }
            connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val text = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            if (status !in 200..299) throw SupabaseException(status, "HTTP $status: ${text.take(300)}")
            text
        } finally {
            connection.disconnect()
        }
    }

    private fun parseSession(text: String): Session {
        val json = JSONObject(text)
        return Session(
            accessToken = json.getString("access_token"),
            refreshToken = json.getString("refresh_token"),
            expiresAtEpochSeconds = json.getLong("expires_at"),
        )
    }
}
