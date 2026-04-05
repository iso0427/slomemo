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
        ColumnSetting::class,
        MemoRecord::class,
        MemoValue::class,
        AppSetting::class // ★ ステップ3：ここに追加！
    ],
    version = 6 // ★ バージョンを「5」から「6」に上げてください
)
@androidx.room.TypeConverters(Converters::class)
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