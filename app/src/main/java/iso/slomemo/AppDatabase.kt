package iso.slomemo

import androidx.room.Database
import androidx.room.RoomDatabase


@Database(
    entities = [
        Machine::class,
        ColumnSetting::class,
        MemoRecord::class,
        MemoValue::class,
        AppSetting::class,
        AutoInputRule::class,
        SelectionOption::class,
        CounterSetting::class, // ★追加
        CounterValue::class    // ★追加
    ],
    version = 10 // ★ 9から10に上げる
)
@androidx.room.TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun memoDao(): MemoDao
    abstract fun machineDao(): MachineDao
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