package iso.slomemo

import android.app.Application
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Stack

class MainViewModel(application: Application) : AndroidViewModel(application) {

    // ① 新しい場所にある AppDatabase を使う
    val db = Room.databaseBuilder(
        application,
        AppDatabase::class.java,
        "memo-db"
    )
        // 構造が変わったので、古いデータがある場合は一旦消して作り直す設定
        .fallbackToDestructiveMigration()
        .build()

    // ② 新しい memoDao を使う
    val dao = db.memoDao()

    // 1. MemoAction の定義を「値」も持てるように変更
    sealed class MemoAction {
        // リセット用（既存）
        data class Reset(
            val backupRecords: List<MemoRecord>,
            val backupValues: List<MemoValue>
        ) : MemoAction()

        // ★ 追加：編集（Update）用
        data class Update(
            val oldRecord: MemoRecord,      // 編集前の行
            val oldValues: List<MemoValue>, // 編集前の値
            val newRecord: MemoRecord,      // 編集後の行
            val newValues: List<MemoValue>  // 編集後の値
        ) : MemoAction()
    }

    private val undoStack = Stack<MemoAction>()
    private val redoStack = Stack<MemoAction>()
    var canUndo = mutableStateOf(false)
    var canRedo = mutableStateOf(false)

    private fun updateStackStates() {
        // Mainスレッドで更新する必要があります
        viewModelScope.launch(Dispatchers.Main) {
            canUndo.value = undoStack.isNotEmpty()
            canRedo.value = redoStack.isNotEmpty()
        }
    }

    fun resetAllMemosWithHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            val currentRecords = dao.getAllRecords()
            val currentValues = dao.getAllValues() // ★ 全データを取得

            if (currentRecords.isNotEmpty()) {
                // ★ 両方を履歴に積む
                undoStack.push(MemoAction.Reset(currentRecords, currentValues))
                redoStack.clear()

                dao.deleteAllMemoValues()
                dao.deleteAllRecords()
                updateStackStates()
            }
        }
    }

    fun undo() {
        viewModelScope.launch(Dispatchers.IO) {
            if (undoStack.isEmpty()) return@launch
            val lastAction = undoStack.pop()

            when (lastAction) {
                is MemoAction.Reset -> {
                    redoStack.push(lastAction)
                    lastAction.backupRecords.forEach { dao.insertRecord(it) }
                    lastAction.backupValues.forEach { dao.insertValue(it) }
                }
                // ★ 追加：編集の Undo
                is MemoAction.Update -> {
                    redoStack.push(lastAction)
                    // 「編集前」の状態で上書きする
                    dao.insertRecord(lastAction.oldRecord)
                    // 一度紐付いている値を消してから古い値を入れ直す
                    dao.deleteValuesByRecordId(lastAction.oldRecord.id)
                    lastAction.oldValues.forEach { dao.insertValue(it) }
                }
            }
            updateStackStates()
        }
    }

    fun redo() {
        viewModelScope.launch(Dispatchers.IO) {
            if (redoStack.isEmpty()) return@launch
            val nextAction = redoStack.pop()

            when (nextAction) {
                is MemoAction.Reset -> {
                    undoStack.push(nextAction)
                    dao.deleteAllMemoValues()
                    dao.deleteAllRecords()
                }
                // ★ 追加：編集の Redo
                is MemoAction.Update -> {
                    undoStack.push(nextAction)
                    // 「編集後」の状態で再度上書きする
                    dao.insertRecord(nextAction.newRecord)
                    dao.deleteValuesByRecordId(nextAction.newRecord.id)
                    nextAction.newValues.forEach { dao.insertValue(it) }
                }
            }
            updateStackStates()
        }
    }

    fun updateMemoWithHistory(record: MemoRecord, values: List<MemoValue>) {
        viewModelScope.launch(Dispatchers.IO) {
            // 1. 現在のDBから「編集前」のデータを取得（バックアップ）
            val oldRecord = dao.getRecordById(record.id) ?: return@launch
            val oldValues = dao.getValuesForRecord(record.id)

            // 2. 「編集前」と「編集後」をセットにして履歴に積む
            undoStack.push(
                MemoAction.Update(
                    oldRecord = oldRecord,
                    oldValues = oldValues,
                    newRecord = record,
                    newValues = values
                )
            )

            // 3. Redoスタックをクリア（新しく操作したので進む履歴は消す）
            redoStack.clear()

            // 4. DBを更新（REPLACE設定なので insert で上書きされる）
            dao.insertRecord(record)
            values.forEach { dao.insertValue(it) }

            // 5. ボタンの状態を更新
            updateStackStates()
        }
    }
}