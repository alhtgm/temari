package jp.temari.fitness.data

import jp.temari.fitness.config.JourneyConfig

/**
 * 「たび」の今の位置。
 *
 * @property distanceKm この周で進んだ距離（km）。
 * @property lap 何周目か（1 から）。京都に着いたら、また日本橋から2周目を始める。
 * @property passedIndex 最後に通った宿場の番号。
 * @property nextIndex つぎの宿場の番号。
 * @property kmToNext つぎの宿場まで、あと何 km か。
 * @property routeFraction 終点までの進み具合（0〜1）。
 */
data class JourneyProgress(
    val distanceKm: Double,
    val lap: Int,
    val passedIndex: Int,
    val nextIndex: Int,
    val kmToNext: Double,
    val routeFraction: Float,
) {
    companion object {
        /**
         * これまでの歩数から、今の位置を計算する。
         *
         * @param stations 各宿場までの距離（km）。最初は 0、最後が終点。
         */
        fun of(
            totalSteps: Int,
            strideMeters: Double = JourneyConfig.STRIDE_METERS,
            stations: List<Double> = JourneyConfig.STATION_KM,
        ): JourneyProgress {
            val routeKm = stations.last()
            val totalKm = totalSteps.coerceAtLeast(0) * strideMeters / 1000.0
            // 終点を過ぎたら、何周目かを数えて、その周の中での距離にする。
            val lap = (totalKm / routeKm).toInt() + 1
            val distance = totalKm - (lap - 1) * routeKm
            val passed = stations.indexOfLast { it <= distance }.coerceAtLeast(0)
            val next = (passed + 1).coerceAtMost(stations.lastIndex)
            return JourneyProgress(
                distanceKm = distance,
                lap = lap,
                passedIndex = passed,
                nextIndex = next,
                kmToNext = (stations[next] - distance).coerceAtLeast(0.0),
                routeFraction = (distance / routeKm).toFloat().coerceIn(0f, 1f),
            )
        }
    }
}
