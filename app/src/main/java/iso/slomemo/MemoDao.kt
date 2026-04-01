package iso.slomemo

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface MemoDao {

    // --- 項目設定（Column） ---
    @Insert
    suspend fun insertColumn(column: ColumnSetting)

    @Query("SELECT * FROM ColumnSetting")
    suspend fun getAllColumns(): List<ColumnSetting>

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

    // 項目本体を削除
    @Delete
    suspend fun deleteColumn(column: ColumnSetting)

    // 項目に紐付く入力値を削除（重要：これをしないと表がズレる原因になります）
    @Query("DELETE FROM MemoValue WHERE columnId = :columnId")
    suspend fun deleteValuesByColumnId(columnId: Int)

    @Update
    suspend fun updateColumn(column: ColumnSetting)


























}