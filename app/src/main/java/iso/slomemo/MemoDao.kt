package iso.slomemo

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface MemoDao {


    @androidx.room.Query("SELECT * FROM AppSetting WHERE id = 1")
    fun getSettingFlow(): kotlinx.coroutines.flow.Flow<AppSetting?>

    @androidx.room.Insert(onConflict = androidx.room.OnConflictStrategy.REPLACE)
    suspend fun updateSetting(setting: AppSetting)

    @androidx.room.Insert
    suspend fun insertRule(rule: AutoInputRule)

    @androidx.room.Query("SELECT * FROM AutoInputRule")
    suspend fun getAllRules(): List<AutoInputRule>

    @androidx.room.Query("DELETE FROM AutoInputRule WHERE triggerColumnId = :colId AND triggerValue = :value")
    suspend fun deleteRulesByTrigger(colId: Int, value: String)

    @androidx.room.Query("SELECT * FROM AutoInputRule WHERE triggerColumnId = :colId AND triggerValue = :value")
    suspend fun getRulesByTrigger(colId: Int, value: String): List<AutoInputRule>




    @Insert
    suspend fun insertColumn(column: ColumnSetting)

    @Insert(onConflict = androidx.room.OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: MemoRecord): Long

    @Insert(onConflict = androidx.room.OnConflictStrategy.REPLACE)
    suspend fun insertValue(value: MemoValue)



    @Delete
    suspend fun deleteColumn(column: ColumnSetting)



    @Update
    suspend fun updateColumn(column: ColumnSetting)

    @Query("DELETE FROM AutoInputRule WHERE targetColumnId = :columnId")
    suspend fun deleteRulesByTargetColumn(columnId: Int)

    @Query("DELETE FROM AutoInputRule WHERE triggerColumnId = :columnId")
    suspend fun deleteRulesByTriggerColumn(columnId: Int)

    @Query("DELETE FROM MemoRecord")
    suspend fun deleteAllMemoRecords()

    @Query("DELETE FROM MemoRecord")
    suspend fun deleteAllRecords()

    @Query("DELETE FROM MemoRecord WHERE id = :recordId")
    suspend fun deleteRecordById(recordId: Int)

    @Query("DELETE FROM MemoValue")
    suspend fun deleteAllMemoValues()

    @Query("DELETE FROM MemoValue WHERE columnId = :columnId")
    suspend fun deleteValuesByColumnId(columnId: Int)

    @Query("DELETE FROM MemoValue WHERE recordId = :recordId")
    suspend fun deleteValuesByRecordId(recordId: Int)

    @Query("SELECT * FROM ColumnSetting ORDER BY displayOrder ASC")
    fun getAllColumnsDirect(): List<ColumnSetting>

    @Query("SELECT * FROM ColumnSetting ORDER BY displayOrder ASC")
    suspend fun getAllColumns(): List<ColumnSetting>

    @Query("SELECT * FROM MemoValue")
    fun getAllValuesFlow(): Flow<List<MemoValue>>

    @Query("SELECT * FROM MemoValue")
    suspend fun getAllValues(): List<MemoValue>

    @Query("SELECT * FROM MemoRecord WHERE id = :id")
    suspend fun getRecordById(id: Int): MemoRecord?

    @Query("SELECT * FROM MemoRecord WHERE isDeleted = 0 ORDER BY timestamp ASC")
    suspend fun getAllRecords(): List<MemoRecord>

    @Query("SELECT * FROM MemoValue WHERE recordId = :recordId")
    suspend fun getValuesForRecord(recordId: Int): List<MemoValue>

    @Query("SELECT * FROM AutoInputRule")
    suspend fun getAllAutoInputRules(): List<AutoInputRule>

    @Query("UPDATE MemoRecord SET isDeleted = 1")
    suspend fun softDeleteAll()

    @Query("UPDATE MemoRecord SET isDeleted = 0")
    suspend fun undoDeleteAll()

    @Query("UPDATE MemoRecord SET isDeleted = 1 WHERE id = :recordId")
    suspend fun softDeleteRecordById(recordId: Int)






}