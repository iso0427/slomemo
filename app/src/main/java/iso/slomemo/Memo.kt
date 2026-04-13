package iso.slomemo

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

// ① 項目名そのものを保存する（例：「pt」「契機」など）
@Entity
data class ColumnSetting(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val options: List<String> = emptyList(),
    val displayOrder: Int = 0,
    val showTextField: Boolean = false
)
// ② 1行分の「データのまとまり」を管理する
@Entity
data class MemoRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false
)

// ③ 実際の入力値（どの行の、どの項目に、何を入れたか）
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

// ④ 項目に紐付く「選択肢」を保存する箱
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

// ⑤ 自動入力のルールを保存する箱
@androidx.room.Entity
data class AutoInputRule(
    @androidx.room.PrimaryKey(autoGenerate = true) val id: Int = 0,
    val triggerColumnId: Int, // 引き金になる項目のID
    val triggerValue: String,  // 引き金になる選択肢（例："BIG"）
    val targetColumnId: Int,   // 自動入力させたい項目のID
    val targetValue: String,   // 自動入力する値（例："━"）
    val isNextRow: Boolean     // false: 同じ行 / true: 次の行
)