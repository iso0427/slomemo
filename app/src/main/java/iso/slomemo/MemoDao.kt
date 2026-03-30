package iso.slomemo

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query

@Dao
interface MemoDao {

    // --- 項目設定（Column） ---
    @Insert
    suspend fun insertColumn(column: ColumnSetting)

    @Query("SELECT * FROM ColumnSetting ORDER BY orderIndex ASC")
    suspend fun getAllColumns(): List<ColumnSetting>

    // --- 選択肢（Option） ---
    @Insert
    suspend fun insertOption(option: SelectionOption)

    @Query("SELECT * FROM SelectionOption WHERE columnId = :columnId")
    suspend fun getOptionsForColumn(columnId: Int): List<SelectionOption>

    // --- 履歴本体（Record） ---
    @Insert
    suspend fun insertRecord(record: MemoRecord): Long

    @Query("SELECT * FROM MemoRecord ORDER BY timestamp DESC")
    suspend fun getAllRecords(): List<MemoRecord> // ← これが MainActivity で必要！

    // --- 入力された値（Value） ---
    @Insert
    suspend fun insertValue(value: MemoValue)

    @Query("SELECT * FROM MemoValue WHERE recordId = :recordId")
    suspend fun getValuesForRecord(recordId: Int): List<MemoValue> // ← これも必要！

    // 履歴（行）を削除する
    @Delete
    suspend fun deleteRecord(record: MemoRecord)

    // その履歴に紐付く「値」もまとめて消す（ゴミを残さないため）
    @Query("DELETE FROM MemoValue WHERE recordId = :recordId")
    suspend fun deleteValuesByRecordId(recordId: Int)






























}