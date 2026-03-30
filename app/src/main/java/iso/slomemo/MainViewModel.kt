package iso.slomemo

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.room.Room

class MainViewModel(application: Application) : AndroidViewModel(application) {

    // ① 新しい場所にある AppDatabase を使う
    val db = Room.databaseBuilder(
        application,
        AppDatabase::class.java,
        "slomemo-db"
    )
        // 構造が変わったので、古いデータがある場合は一旦消して作り直す設定
        .fallbackToDestructiveMigration()
        .build()

    // ② 新しい memoDao を使う
    val dao = db.memoDao()

    // ※ ここから下の「rows」などの処理は、
    // 今後「入力したメモ一覧を表示する機能」を作る際に、
    // 新しいデータ構造に合わせて書き直していきます。
}