package iso.slomemo

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey

// ① 項目名そのものを保存する（例：「pt」「契機」など）
@Entity
data class ColumnSetting(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,        // 項目名
    val orderIndex: Int      // 並び順
)

// ② 1行分の「データのまとまり」を管理する
@Entity
data class MemoRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    // ↓ これを追加！現在の時間をミリ秒で保存します
    val timestamp: Long = System.currentTimeMillis()
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
    val recordId: Int, // どの行か
    val columnId: Int, // どの項目か
    val value: String  // 入力された文字
) // ← ここでしっかり閉じる必要がありました！

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