package iso.slomemo

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        ColumnSetting::class,
        MemoRecord::class,
        MemoValue::class,
        SelectionOption::class
    ],
    version = 4
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun memoDao(): MemoDao
}