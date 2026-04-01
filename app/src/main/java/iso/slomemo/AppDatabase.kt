package iso.slomemo

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        ColumnSetting::class,
        MemoRecord::class,
        MemoValue::class,
        // SelectionOption::class はもう使わないので消してもOKです
    ],
    version = 5 // ★バージョンを「4」から「5」に上げてください
)
@androidx.room.TypeConverters(Converters::class) // ★この1行を追加！
abstract class AppDatabase : RoomDatabase() {
    abstract fun memoDao(): MemoDao
}

class Converters {
    @androidx.room.TypeConverter
    fun fromString(value: String): List<String> {
        return if (value.isEmpty()) emptyList() else value.split(",")
    }

    @androidx.room.TypeConverter
    fun fromList(list: List<String>): String {
        return list.joinToString(",")
    }
}