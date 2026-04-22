package iso.slomemo

import androidx.room.Database
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.RoomDatabase

// ★ ステップ1：設定を保存する「箱」を定義
@Entity
data class AppSetting(
    @PrimaryKey val id: Int = 1,
    val showTime: Boolean = true
)

@Database(
    entities = [
        Machine::class,
        ColumnSetting::class,
        MemoRecord::class,
        MemoValue::class,
        AppSetting::class,
        AutoInputRule::class
    ],
    version = 8
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