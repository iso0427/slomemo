package iso.slomemo

import androidx.room.Database
import androidx.room.RoomDatabase

// entities の中に、新しく作った3つのクラスをすべて書き並べます
// version は、構造を変えたので「2」に上げます
@Database(
    entities = [
        ColumnSetting::class,
        MemoRecord::class,
        MemoValue::class
    ],
    version = 2
)
abstract class AppDatabase : RoomDatabase() {
    // ここも後でそれぞれのDao（操作命令セット）を追加しますが、
    // まずはコンパイルが通るように「memoDao」だけ残しておきます
    abstract fun memoDao(): MemoDao
}