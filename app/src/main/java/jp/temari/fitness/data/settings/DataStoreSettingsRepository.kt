package jp.temari.fitness.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import jp.temari.fitness.config.ActivityConfig
import jp.temari.fitness.config.TextSizeOption
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

/** 設定を保存するファイル（DataStore）。アプリの中で1つだけ作られるよう、ここで定義する。 */
private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "user_settings")

/**
 * DataStore（端末の中に小さな値を保存するしくみ）を使った [SettingsRepository]。
 *
 * アプリを終了しても値が残る。
 */
class DataStoreSettingsRepository(context: Context) : SettingsRepository {

    private val dataStore = context.applicationContext.settingsDataStore

    /** 保存するときの名前（キー）。 */
    private object Keys {
        val TEXT_SIZE = stringPreferencesKey("text_size")
        val NICKNAME = stringPreferencesKey("nickname")
        val DAILY_GOAL = intPreferencesKey("daily_goal")
        val USER_ID = stringPreferencesKey("user_id")
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
    }

    override val settings: Flow<UserSettings> = dataStore.data.map { prefs ->
        UserSettings(
            // 知らない値が入っていたとき（将来の版で選択肢を変えた場合など）は「大」にしておく。
            textSize = TextSizeOption.entries.firstOrNull { it.name == prefs[Keys.TEXT_SIZE] }
                ?: TextSizeOption.Large,
            nickname = prefs[Keys.NICKNAME].orEmpty(),
            dailyGoal = prefs[Keys.DAILY_GOAL] ?: ActivityConfig.DEFAULT_DAILY_GOAL,
            userId = prefs[Keys.USER_ID],
            onboardingCompleted = prefs[Keys.ONBOARDING_COMPLETED] ?: false,
        )
    }

    override suspend fun setTextSize(textSize: TextSizeOption) {
        dataStore.edit { it[Keys.TEXT_SIZE] = textSize.name }
    }

    override suspend fun setNickname(nickname: String) {
        dataStore.edit { it[Keys.NICKNAME] = nickname.trim().take(ActivityConfig.NICKNAME_MAX_LENGTH) }
    }

    override suspend fun setDailyGoal(goal: Int) {
        dataStore.edit {
            it[Keys.DAILY_GOAL] = goal.coerceIn(ActivityConfig.MIN_DAILY_GOAL, ActivityConfig.MAX_DAILY_GOAL)
        }
    }

    override suspend fun ensureUserId(): String {
        var id = ""
        // 読み出しと書き込みを edit の中でまとめて行い、2回呼ばれても ID が2つ作られないようにする。
        dataStore.edit { prefs ->
            id = prefs[Keys.USER_ID] ?: UUID.randomUUID().toString().also { prefs[Keys.USER_ID] = it }
        }
        return id
    }

    override suspend fun setOnboardingCompleted(completed: Boolean) {
        dataStore.edit { it[Keys.ONBOARDING_COMPLETED] = completed }
    }

    override suspend fun clearAll() {
        dataStore.edit { it.clear() }
    }
}
