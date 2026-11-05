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

    @Query("DELETE FROM memo_values")
    suspend fun deleteAllMemoValues()

    @Query("DELETE FROM memo_values WHERE columnId = :columnId")
    suspend fun deleteValuesByColumnId(columnId: Int)

    @Query("DELETE FROM memo_values WHERE recordId = :recordId")
    suspend fun deleteValuesByRecordId(recordId: Int)

    @Query("SELECT * FROM column_settings WHERE machineId = :machineId ORDER BY displayOrder ASC")
    suspend fun getColumnsByMachineDirect(machineId: Int): List<ColumnSetting>

    @Query("SELECT * FROM column_settings WHERE machineId = :machineId ORDER BY displayOrder ASC")
    fun getColumnsByMachine(machineId: Int): Flow<List<ColumnSetting>>

    @Query("SELECT * FROM memo_values")
    fun getAllValuesFlow(): Flow<List<MemoValue>>

    @Query("SELECT * FROM memo_values")
    suspend fun getAllValues(): List<MemoValue>

    @Query("SELECT * FROM memo_records WHERE id = :id")
    suspend fun getRecordById(id: Int): MemoRecord?

    @Query("SELECT * FROM memo_records WHERE machineId = :machineId AND isDeleted = 0 ORDER BY timestamp ASC")
    suspend fun getRecordsByMachine(machineId: Int): List<MemoRecord>

    @Query("SELECT * FROM memo_values WHERE recordId = :recordId")
    suspend fun getValuesForRecord(recordId: Int): List<MemoValue>

    // --- 削除（ソフトデリート）関連 ---
    @Query("UPDATE memo_records SET isDeleted = 1")
    suspend fun softDeleteAll()

    @Query("UPDATE memo_records SET isDeleted = 0")
    suspend fun undoDeleteAll()

    @Query("UPDATE memo_records SET isDeleted = 1 WHERE id = :recordId")
    suspend fun softDeleteRecordById(recordId: Int)

    // --- SelectionOption（選択肢）関連 ---
    @Query("SELECT * FROM selection_options WHERE columnId = :columnId") // 👈 大文字の SelectionOption から小文字の selection_options に変更
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

    @Query("SELECT * FROM app_settings WHERE id = 0")
    suspend fun getAppSetting(): AppSetting?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveAppSetting(setting: AppSetting)

    // カラム名は counterId と count なので、それに合わせます
    @Query("UPDATE counter_values SET count = count + :diff WHERE counterId = :counterId")
    suspend fun adjustCounterValue(counterId: Int, diff: Int)

    @Query("SELECT count FROM counter_values WHERE counterId = :counterId")
    fun getCounterCountFlow(counterId: Int): Flow<Int?>

    @Update
    suspend fun updateCounters(counters: List<CounterSetting>)

    @Query("SELECT * FROM counter_values")
    suspend fun getAllCounterValues(): List<CounterValue>

    // ① 機種ごとのカウンターボタンを取得するFlow
    @Query("SELECT * FROM counter_settings WHERE machineId = :machineId ORDER BY displayOrder ASC")
    fun getCountersByMachineFlow(machineId: Int): Flow<List<CounterSetting>>

    // ② 機種ごとのカウンターの「数字」だけを取得するFlow
    @Query("SELECT cv.* FROM counter_values cv INNER JOIN counter_settings cs ON cv.counterId = cs.id WHERE cs.machineId = :machineId")
    fun getCounterValuesByMachineFlow(machineId: Int): Flow<List<CounterValue>>

    @Query("SELECT * FROM rotation_values WHERE machineId = :machineId")
    suspend fun getRotationValue(machineId: Int): RotationValue?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveRotationValue(rotationValue: RotationValue)

    // 🟢 引数の型を Long から Int に変更しました
    @Query("SELECT * FROM counter_settings WHERE machineId = :machineId")
    suspend fun getCounterSettingsByMachineDirect(machineId: Int): List<CounterSetting>

    // カウンター設定をすべて削除する
    @Query("DELETE FROM counter_settings")
    suspend fun deleteAllCounterSettings()

    // カウンターの値をすべて削除する
    @Query("DELETE FROM counter_values")
    suspend fun deleteAllCounterValues()

    // カウンター設定を保存して、生成された新しい一意のID(Long)を返す
    @Insert
    suspend fun insertCounterSettingReturnId(counterSetting: CounterSetting): Long

    // 初期値（0）のカウンター値を保存する
    @Insert(onConflict = androidx.room.OnConflictStrategy.REPLACE)
    suspend fun insertCounterValue(counterValue: CounterValue)

    // 計算相手のカウンターIDだけを後から更新する
    @Query("UPDATE counter_settings SET targetCounterId = :targetId WHERE id = :id")
    suspend fun updateCounterTargetId(id: Int, targetId: Int)

    @Query("UPDATE rotation_values SET startRotation = :start, currentRotation = :current, addRotation = :add WHERE machineId = :machineId")
    suspend fun updateAllRotationValues(machineId: Int, start: String, current: String, add: String)

    // MemoDao.kt
    // 修正前
    //@Query("UPDATE MemoValue SET value = :newValue WHERE columnId = :columnId AND value = :oldValue")
    //suspend fun updateMemoValuesText(columnId: Int, oldValue: String, newValue: String)

    // 修正後（テーブル名を memo_values に統一）
    @Query("UPDATE memo_values SET value = :newValue WHERE columnId = :columnId AND value = :oldValue")
    suspend fun updateMemoValuesText(columnId: Int, oldValue: String, newValue: String): Int // 👈 ": Int" を追加

    // --- 機種ごとの設定（MachineSetting）関連 ---

    // 1. 指定した機種の設定を取得する（Flow版：リアルタイム監視用）
    @Query("SELECT * FROM machine_settings WHERE machineId = :machineId LIMIT 1")
    fun getMachineSettingFlow(machineId: Int): Flow<MachineSetting?>

    // 2. 指定した機種の設定を取得する（一時取得用）
    @Query("SELECT * FROM machine_settings WHERE machineId = :machineId LIMIT 1")
    suspend fun getMachineSetting(machineId: Int): MachineSetting?

    // 3. 機種ごとの設定を保存・更新する（存在しなければ新規作成、あれば上書き）
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveMachineSetting(setting: MachineSetting)


}