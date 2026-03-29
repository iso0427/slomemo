package iso.slomemo

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface MemoDao {

    // --- 項目名（ヘッダー）に関する命令 ---

    // 項目（「pt」「契機」など）を新しく追加する
    @Insert
    suspend fun insertColumn(column: ColumnSetting)

    // 設定されたすべての項目を、順番（orderIndex）通りに取得する
    @Query("SELECT * FROM ColumnSetting ORDER BY orderIndex ASC")
    suspend fun getAllColumns(): List<ColumnSetting>

    // 特定の項目を削除する（もし必要になったとき用）
    @Query("DELETE FROM ColumnSetting WHERE id = :columnId")
    suspend fun deleteColumn(columnId: Int)

    // --- 選択肢（Option）に関する命令 ---

    // 項目に紐付く選択肢（例：「契機」に「強チェ」）を保存する
    @Insert
    suspend fun insertOption(option: SelectionOption)

    // 特定の項目（columnId）に紐付いている選択肢だけをすべて取得する
    @Query("SELECT * FROM SelectionOption WHERE columnId = :columnId")
    suspend fun getOptionsForColumn(columnId: Int): List<SelectionOption>


}