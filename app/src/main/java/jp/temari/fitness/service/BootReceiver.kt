package jp.temari.fitness.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import jp.temari.fitness.TemariApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * 端末を再起動したとき・アプリを更新したときに、記録を自動で始め直す。
 *
 * 利用者が自分でアプリを開かなくても記録が続くようにするため（「何もしなくていい」アプリにするため）。
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED && intent.action != Intent.ACTION_MY_PACKAGE_REPLACED) return
        // 設定を読むのに少し時間がかかるので、goAsync で「まだ終わっていない」と Android に伝えておく。
        val pending = goAsync()
        val container = (context.applicationContext as TemariApplication).container
        CoroutineScope(Dispatchers.Default).launch {
            try {
                container.recordingController.startIfReady()
            } finally {
                pending.finish()
            }
        }
    }
}
