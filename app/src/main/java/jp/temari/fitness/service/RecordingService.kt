package jp.temari.fitness.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import jp.temari.fitness.MainActivity
import jp.temari.fitness.R
import jp.temari.fitness.TemariApplication
import jp.temari.fitness.config.SensingConfig
import jp.temari.fitness.data.sensing.ActivityStore
import jp.temari.fitness.data.sensing.EpochClassifier
import jp.temari.fitness.data.sensing.StepDetector
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * 1日の動きを記録し続けるサービス（フォアグラウンドサービス）。
 *
 * 「記録しています」というお知らせを出しているあいだ、画面を閉じても Android に止められにくくなる。
 * 種類は health（健康・運動の記録）にしている。
 *
 * 読むセンサー:
 * - 歩数計（TYPE_STEP_COUNTER）: 端末が数えた歩数。電池をほとんど使わず正確。無い端末では加速度から数える。
 * - 加速度（TYPE_ACCELEROMETER）: 揺れの強さ（ENMO）から「家のこと」を見分ける。
 *
 * 10 秒ごとに [EpochClassifier] で判別して [ActivityStore] に足し、1 分ごとにファイルへ保存する。
 */
class RecordingService : Service(), SensorEventListener {

    // センサーの値は画面と同じメインスレッドに届くので、判別の処理もメインスレッドで行う（同時に触らないようにするため）。
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val classifier = EpochClassifier()
    private val stepDetector = StepDetector()

    private lateinit var store: ActivityStore
    private lateinit var clock: Clock
    private var sensorManager: SensorManager? = null
    private var hasStepCounter = false
    private var lastCounterValue: Float? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var started = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        val container = (application as TemariApplication).container
        store = container.activityStore
        clock = container.clock
        createChannel()
        try {
            ServiceCompat.startForeground(
                this,
                NOTIFICATION_ID,
                buildNotification(),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH,
            )
        } catch (e: RuntimeException) {
            // 許可が無いなどで始められなかった。記録が止まっていることはホームの表示で分かる。
            Log.w(TAG, "フォアグラウンドサービスを始められませんでした", e)
            stopSelf()
            return
        }
        started = true
        registerSensors()
        acquireWakeLock()
        store.setRunning(true)
        scope.launch {
            while (isActive) {
                delay(SensingConfig.EPOCH_SECONDS * 1_000L)
                store.addEpoch(LocalDateTime.now(clock), classifier.finish())
            }
        }
        scope.launch {
            while (isActive) {
                delay(SensingConfig.SAVE_INTERVAL_MILLIS)
                store.save(LocalDate.now(clock))
            }
        }
    }

    // START_STICKY: Android がメモリー不足などでこのサービスを止めても、余裕ができたら自動で始め直してもらう。
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onDestroy() {
        scope.cancel()
        if (started) {
            sensorManager?.unregisterListener(this)
            wakeLock?.let { if (it.isHeld) it.release() }
            store.setRunning(false)
            // 止める前に、まだ保存していない分を書き出す。
            CoroutineScope(Dispatchers.IO).launch { store.save(LocalDate.now(clock)) }
        }
        super.onDestroy()
    }

    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                val (x, y, z) = Triple(event.values[0], event.values[1], event.values[2])
                classifier.addAcceleration(x, y, z)
                // 歩数計が無い端末だけ、加速度から歩数を数える。timestamp はナノ秒なのでミリ秒に直す。
                if (!hasStepCounter && stepDetector.add(x, y, z, event.timestamp / 1_000_000)) {
                    classifier.addSteps(1)
                }
            }
            Sensor.TYPE_STEP_COUNTER -> {
                // 歩数計は「端末を起動してからの合計」を返すので、前の値との差がこの間の歩数になる。
                val value = event.values[0]
                val last = lastCounterValue
                // 差がマイナスなのは端末が再起動して 0 に戻ったとき。そのときは数えずに基準だけ替える。
                if (last != null && value >= last) classifier.addSteps((value - last).toInt())
                lastCounterValue = value
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    private fun registerSensors() {
        val manager = getSystemService(SensorManager::class.java) ?: return
        sensorManager = manager
        manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.let {
            manager.registerListener(this, it, SensingConfig.SAMPLING_PERIOD_US, SensingConfig.MAX_REPORT_LATENCY_US)
        }
        val counter = manager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        hasStepCounter = counter != null
        counter?.let {
            manager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL, SensingConfig.MAX_REPORT_LATENCY_US)
        }
        store.setHasStepCounter(hasStepCounter)
    }

    /**
     * 画面が消えても CPU を眠らせないようにする（部分ウェイクロック）。
     *
     * 眠ってしまうと加速度の値が届かず、「家のこと」が数えられないため。そのぶん電池を使うので、
     * 実機で電池の減り方を見て、必要なら時間帯を区切るなどを考える（レビュー記録の保留点）。
     */
    private fun acquireWakeLock() {
        val power = getSystemService(PowerManager::class.java) ?: return
        wakeLock = power.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "$packageName:recording").apply {
            setReferenceCounted(false)
            acquire()
        }
    }

    private fun createChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.recording_channel_name),
            // 音や振動で知らせない、静かなお知らせにする。
            NotificationManager.IMPORTANCE_LOW,
        ).apply { description = getString(R.string.recording_channel_description) }
        getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
    }

    private fun buildNotification() = NotificationCompat.Builder(this, CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_notification)
        .setContentTitle(getString(R.string.recording_notification_title))
        .setContentText(getString(R.string.recording_notification_text))
        .setOngoing(true)
        .setContentIntent(
            // お知らせを押したらアプリを開く。
            PendingIntent.getActivity(
                this,
                0,
                Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
                PendingIntent.FLAG_IMMUTABLE,
            ),
        )
        .build()

    private companion object {
        const val TAG = "RecordingService"
        const val CHANNEL_ID = "recording"
        const val NOTIFICATION_ID = 1
    }
}
