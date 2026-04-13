package iso.slomemo

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface MemoDao {

    // --- 項目設定（Column） ---
    @Insert
    suspend fun insertColumn(column: ColumnSetting)

    // --- 履歴本体（Record） ---
    @Insert
    suspend fun insertRecord(record: MemoRecord): Long

    @Query("SELECT * FROM MemoRecord WHERE isDeleted = 0 ORDER BY timestamp ASC")
    suspend fun getAllRecords(): List<MemoRecord>

    @Insert
    suspend fun insertValue(value: MemoValue)

    @Query("SELECT * FROM MemoValue WHERE recordId = :recordId")
    suspend fun getValuesForRecord(recordId: Int): List<MemoValue> // ← これも必要！

    @Query("DELETE FROM MemoRecord WHERE id = :recordId")
    suspend fun deleteRecordById(recordId: Int)

    // その履歴に紐付く「値」もまとめて消す（ゴミを残さないため）
    @Query("DELETE FROM MemoValue WHERE recordId = :recordId")
    suspend fun deleteValuesByRecordId(recordId: Int)

    // 項目本体を削除
    @Delete
    suspend fun deleteColumn(column: ColumnSetting)

    @Update
    suspend fun updateColumn(column: ColumnSetting)

    @Query("SELECT * FROM MemoValue")
    suspend fun getAllValues(): List<MemoValue>

    @Query("SELECT * FROM MemoValue")
    fun getAllValuesFlow(): Flow<List<MemoValue>>

    // MemoDao の interface の中に追加してください
    @androidx.room.Query("SELECT * FROM AppSetting WHERE id = 1")
    fun getSettingFlow(): kotlinx.coroutines.flow.Flow<AppSetting?>

    @androidx.room.Insert(onConflict = androidx.room.OnConflictStrategy.REPLACE)
    suspend fun updateSetting(setting: AppSetting)

    // --- 重複していた部分をこれ1つにまとめます ---
    @Query("SELECT * FROM ColumnSetting ORDER BY displayOrder ASC")
    suspend fun getAllColumns(): List<ColumnSetting>

    @androidx.room.Insert
    suspend fun insertRule(rule: AutoInputRule)

    @androidx.room.Query("SELECT * FROM AutoInputRule")
    suspend fun getAllRules(): List<AutoInputRule>

    @androidx.room.Query("DELETE FROM AutoInputRule WHERE triggerColumnId = :colId AND triggerValue = :value")
    suspend fun deleteRulesByTrigger(colId: Int, value: String)

    @androidx.room.Query("SELECT * FROM AutoInputRule WHERE triggerColumnId = :colId AND triggerValue = :value")
    suspend fun getRulesByTrigger(colId: Int, value: String): List<AutoInputRule>

    @Query("SELECT * FROM ColumnSetting ORDER BY displayOrder ASC")
    fun getAllColumnsDirect(): List<ColumnSetting> // Flow ではなく List で即座に取得する用

    // 1. 連動ルールの削除 (テーブル名が AutoInputRule の場合)
    @Query("DELETE FROM AutoInputRule WHERE triggerColumnId = :columnId")
    suspend fun deleteRulesByTriggerColumn(columnId: Int)

    @Query("DELETE FROM AutoInputRule WHERE targetColumnId = :columnId")
    suspend fun deleteRulesByTargetColumn(columnId: Int)

    // 2. メモの値の削除 (テーブル名が MemoValue の場合)
    @Query("DELETE FROM MemoValue WHERE columnId = :columnId")
    suspend fun deleteValuesByColumnId(columnId: Int)

    @Query("DELETE FROM MemoRecord") // クラス名に合わせる
    suspend fun deleteAllMemoRecords()

    // --- リセット（物理削除を論理削除に変更） ---
    @Query("UPDATE MemoRecord SET isDeleted = 1")
    suspend fun softDeleteAll()

    // --- Undo（フラグを戻して一括復活） ---
    @Query("UPDATE MemoRecord SET isDeleted = 0")
    suspend fun undoDeleteAll()

    // --- (おまけ) 単発の削除も Undo 対応にするなら ---
    @Query("UPDATE MemoRecord SET isDeleted = 1 WHERE id = :recordId")
    suspend fun softDeleteRecordById(recordId: Int)





}