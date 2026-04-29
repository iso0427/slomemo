package iso.slomemo

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface MemoDao {

    // --- 設定（AppSetting）関連 ---
    @Query("SELECT * FROM app_settings WHERE id = 0 LIMIT 1")
    suspend fun getSetting(): AppSetting?

    @Query("SELECT * FROM app_settings WHERE id = 0")
    fun getSettingFlow(): Flow<AppSetting?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateSetting(setting: AppSetting)

    // --- 自動入力ルール（AutoInputRule）関連 ---
    @Insert
    suspend fun insertRule(rule: AutoInputRule)

    @Query("SELECT * FROM AutoInputRule")
    suspend fun getAllRules(): List<AutoInputRule>

    @Query("SELECT * FROM AutoInputRule")
    suspend fun getAllAutoInputRules(): List<AutoInputRule>

    @Query("DELETE FROM AutoInputRule WHERE triggerColumnId = :colId AND triggerValue = :value")
    suspend fun deleteRulesByTrigger(colId: Int, value: String)

    @Query("SELECT * FROM AutoInputRule WHERE triggerColumnId = :colId AND triggerValue = :value")
    suspend fun getRulesByTrigger(colId: Int, value: String): List<AutoInputRule>

    @Query("DELETE FROM AutoInputRule WHERE targetColumnId = :columnId")
    suspend fun deleteRulesByTargetColumn(columnId: Int)

    @Query("DELETE FROM AutoInputRule WHERE triggerColumnId = :columnId")
    suspend fun deleteRulesByTriggerColumn(columnId: Int)

    // --- 機種・項目・データ関連 ---
    @Insert
    suspend fun insertColumn(column: ColumnSetting)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: MemoRecord): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertValue(value: MemoValue)

    @Delete
    suspend fun deleteColumn(column: ColumnSetting)

    @Update
    suspend fun updateColumn(column: ColumnSetting)

    @Query("DELETE FROM memo_records")
    suspend fun deleteAllRecords()

    @Query("DELETE FROM memo_records WHERE id = :recordId")
    suspend fun deleteRecordById(recordId: Int)

    @Query("DELETE FROM MemoValue")
    suspend fun deleteAllMemoValues()

    @Query("DELETE FROM MemoValue WHERE columnId = :columnId")
    suspend fun deleteValuesByColumnId(columnId: Int)

    @Query("DELETE FROM MemoValue WHERE recordId = :recordId")
    suspend fun deleteValuesByRecordId(recordId: Int)

    @Query("SELECT * FROM column_settings WHERE machineId = :machineId ORDER BY displayOrder ASC")
    suspend fun getColumnsByMachineDirect(machineId: Int): List<ColumnSetting>

    @Query("SELECT * FROM column_settings WHERE machineId = :machineId ORDER BY displayOrder ASC")
    fun getColumnsByMachine(machineId: Int): Flow<List<ColumnSetting>>

    @Query("SELECT * FROM MemoValue")
    fun getAllValuesFlow(): Flow<List<MemoValue>>

    @Query("SELECT * FROM MemoValue")
    suspend fun getAllValues(): List<MemoValue>

    @Query("SELECT * FROM memo_records WHERE id = :id")
    suspend fun getRecordById(id: Int): MemoRecord?

    @Query("SELECT * FROM memo_records WHERE machineId = :machineId AND isDeleted = 0 ORDER BY timestamp ASC")
    suspend fun getRecordsByMachine(machineId: Int): List<MemoRecord>

    @Query("SELECT * FROM MemoValue WHERE recordId = :recordId")
    suspend fun getValuesForRecord(recordId: Int): List<MemoValue>

    // --- 削除（ソフトデリート）関連 ---
    @Query("UPDATE memo_records SET isDeleted = 1")
    suspend fun softDeleteAll()

    @Query("UPDATE memo_records SET isDeleted = 0")
    suspend fun undoDeleteAll()

    @Query("UPDATE memo_records SET isDeleted = 1 WHERE id = :recordId")
    suspend fun softDeleteRecordById(recordId: Int)
}