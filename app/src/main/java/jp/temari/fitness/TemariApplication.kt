package jp.temari.fitness

import android.app.Application

/**
 * アプリ全体の入れ物（Application）。アプリが起動したときに最初に1つだけ作られる。
 *
 * [container] を持たせておき、どの画面からも同じリポジトリを使えるようにする。
 */
class TemariApplication : Application() {
    /** アプリ全体で共有する部品。最初に使われたときに作る。 */
    val container: AppContainer by lazy { AppContainer(this) }
}
