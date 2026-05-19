package iso.slomemo

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

// ① 項目名そのものを保存する
@Entity(tableName = "column_settings")
data class ColumnSetting(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val machineId: Int,
    val name: String,
    val options: List<String> = emptyList(),
    val displayOrder: Int = 0,
    val showTextField: Boolean = false
)

// ② 1行分の「データのまとまり」を管理する
@Entity(tableName = "memo_records")
data class MemoRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val machineId: Int,
    val timestamp: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false
)

// ③ 実際の入力値
@Entity(
    foreignKeys = [
        ForeignKey(entity = MemoRecord::class, parentColumns = ["id"], childColumns = ["recordId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = ColumnSetting::class, parentColumns = ["id"], childColumns = ["columnId"], onDelete = ForeignKey.CASCADE)
    ]
)
data class MemoValue(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val recordId: Int,
    val columnId: Int,
    val value: String
)

// ④ 項目に紐付く「選択肢」
@Entity(
    foreignKeys = [
        ForeignKey(
            entity = ColumnSetting::class,
            parentColumns = ["id"],
            childColumns = ["columnId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class SelectionOption(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val columnId: Int,
    val optionName: String
)

// ⑤ 自動入力のルール
@Entity
data class AutoInputRule(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val triggerColumnId: Int,
    val triggerValue: String,
    val targetColumnId: Int,
    val targetValue: String,
    val isNextRow: Boolean
)

// ⑥ アプリ全体の設定
@Entity(tableName = "app_settings")
data class AppSetting(
    @PrimaryKey val id: Int = 0,
    val showTime: Boolean = true,
    val useMaxBrightness: Boolean = true,
    val showSimpleCounter: Boolean = true,
    val showFlashEffect: Boolean = true,
    val showCounterName: Boolean = true,
    val counterHeight: Int = 60,
    val counterFontSize: Int = 45,
    val rotationFontSize: Int = 45,
    val showTotalRotation: Boolean = true
)

// ⑦ 簡易カウンターの項目（ボタン名と色）を保存する
@Entity(tableName = "counter_settings")
data class CounterSetting(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val machineId: Int,        // 💡★ここを追加！これで機種と紐付けます
    val name: String,         // 「ぶどう」などの名前
    val displayOrder: Int = 0, // 並び順
    val color: Long = 0xFFBB86FC
)

// ⑧ 各カウンターの現在の数値を保存する
// ※アプリを閉じても数字が消えないようにするために必要です
@Entity(tableName = "counter_values")
data class CounterValue(
    @PrimaryKey val counterId: Int, // CounterSettingのidと紐付ける
    val count: Int = 0              // 現在の数値
)

