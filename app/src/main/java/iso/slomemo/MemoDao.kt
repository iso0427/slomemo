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

    // --- SelectionOption（選択肢）関連 ---
    @Query("SELECT * FROM SelectionOption WHERE columnId = :columnId")
    suspend fun getOptionsByColumn(columnId: Int): List<SelectionOption>

    @Insert
    suspend fun insertColumnWithIdReturn(column: ColumnSetting): Long

    @Insert
    suspend fun insertSelectionOption(option: SelectionOption)

    @Insert
    suspend fun insertAutoInputRule(rule: AutoInputRule)

// --- 簡易カウンター（CounterSetting / CounterValue）関連 ---

    // カウンターのボタン項目をすべて取得（並び順通り）
    @Query("SELECT * FROM counter_settings ORDER BY displayOrder ASC")
    fun getAllCountersFlow(): Flow<List<CounterSetting>>

    // 特定のカウンターの現在の値を取得
    @Query("SELECT * FROM counter_values WHERE counterId = :counterId LIMIT 1")
    suspend fun getCounterValue(counterId: Int): CounterValue?

    // 全カウンターの値をまとめて取得（Flowで監視用）
    @Query("SELECT * FROM counter_values")
    fun getAllCounterValuesFlow(): Flow<List<CounterValue>>

    // ボタン項目を追加
    @Insert
    suspend fun insertCounter(counter: CounterSetting): Long

    // ボタン項目を更新（名前や並び順の変更用）
    @Update
    suspend fun updateCounter(counter: CounterSetting)

    // ボタン項目を削除
    @Delete
    suspend fun deleteCounter(counter: CounterSetting)

    // カウンターの数値を保存・更新（REPLACEなので上書き）
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateCounterValue(value: CounterValue)

    // ボタン項目が削除された時に、その数値データも削除する用
    @Query("DELETE FROM counter_values WHERE counterId = :counterId")
    suspend fun deleteCounterValueById(counterId: Int)








}