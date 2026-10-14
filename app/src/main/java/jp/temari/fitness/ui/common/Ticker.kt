package jp.temari.fitness.ui.common

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.time.Clock
import java.time.LocalDateTime

/** 「今」を流し直す間隔（ミリ秒）。あいさつや時間帯の表示が、時刻が変わったら追いつくように 1 分ごとにする。 */
private const val TICK_MILLIS = 60_000L

/**
 * 今の日時を 1 分ごとに流す Flow。
 *
 * アプリを開いたまま 10 時をまたいでも、「おはようございます」が「こんにちは」に変わるようにするため。
 */
fun nowTicker(clock: Clock): Flow<LocalDateTime> = flow {
    while (true) {
        emit(LocalDateTime.now(clock))
        delay(TICK_MILLIS)
    }
}
