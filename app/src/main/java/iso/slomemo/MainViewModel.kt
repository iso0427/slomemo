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

    val db = Room.databaseBuilder(
        application,
        AppDatabase::class.java,
        "memo-db"
    ).fallbackToDestructiveMigration().build()

    val dao = db.memoDao()

    var showTimeSetting = mutableStateOf(true)

    // --- Undo/Redo 履歴の定義 ---
    sealed class MemoAction {
        // カウンター
        data class CounterUpdate(val counterId: Int, val isIncrement: Boolean) : MemoAction()
        data class CounterReset(val backupValues: List<CounterValue>) : MemoAction()

        // メモの保存・更新
        data class Update(
            val oldRecord: MemoRecord?,
            val oldValues: List<MemoValue>,
            val newRecord: MemoRecord?,
            val newValues: List<MemoValue>
        ) : MemoAction()

        // メモの全削除
        data class ResetMemos(
            val backupRecords: List<MemoRecord>,
            val backupValues: List<MemoValue>
        ) : MemoAction()
    }

    private val undoStack = Stack<MemoAction>()
    private val redoStack = Stack<MemoAction>()

    var canUndo = mutableStateOf(false)
    var canRedo = mutableStateOf(false)

    private fun updateStackStates() {
        canUndo.value = undoStack.isNotEmpty()
        canRedo.value = redoStack.isNotEmpty()
    }

    // --- Undo (元に戻す) ---
    fun undo() {
        viewModelScope.launch(Dispatchers.IO) {
            if (undoStack.isEmpty()) return@launch

            val lastAction = undoStack.pop()
            redoStack.push(lastAction)

            when (lastAction) {
                is MemoAction.CounterUpdate -> {
                    val undoDiff = if (lastAction.isIncrement) -1 else 1
                    dao.adjustCounterValue(lastAction.counterId, undoDiff)
                }
                is MemoAction.CounterReset -> {
                    lastAction.backupValues.forEach { dao.updateCounterValue(it) }
                }
                is MemoAction.Update -> {
                    // 💡【超・最終解決ポイント】
                    // ログが証明した通り、プログラムの都合で oldRecord == null には絶対になりません。
                    // なので、「保存する前に、文字データ（oldValues）が存在していたかどうか」で判定します。
                    // oldValuesが空っぽということは、文字が何も入っていなかった「実質、完全な新規追加」です！
                    if (lastAction.oldRecord == null || lastAction.oldValues.isEmpty()) {

                        // 【新規追加の取り消し】時間（Record）も文字（Values）も両方完全に抹消！
                        lastAction.newRecord?.let { record ->
                            dao.deleteValuesByRecordId(record.id)
                            dao.deleteRecordById(record.id)
                        }
                        lastAction.newValues.firstOrNull()?.recordId?.let { id ->
                            dao.deleteValuesByRecordId(id)
                            dao.deleteRecordById(id)
                        }
                    } else {
                        // 【本物の編集の取り消し】元々あった文字や状態に戻す
                        dao.insertRecord(lastAction.oldRecord)
                        dao.deleteValuesByRecordId(lastAction.oldRecord.id)
                        lastAction.oldValues.forEach { dao.insertValue(it) }
                    }
                }
                is MemoAction.ResetMemos -> {
                    lastAction.backupRecords.forEach { dao.insertRecord(it) }
                    lastAction.backupValues.forEach { dao.insertValue(it) }
                }
            }
            updateStackStates()
        }
    }

    // --- Redo (やり直し) ---
    fun redo() {
        viewModelScope.launch(Dispatchers.IO) {
            if (redoStack.isEmpty()) return@launch

            val nextAction = redoStack.pop()
            undoStack.push(nextAction)

            when (nextAction) {
                is MemoAction.CounterUpdate -> {
                    val redoDiff = if (nextAction.isIncrement) 1 else -1
                    dao.adjustCounterValue(nextAction.counterId, redoDiff)
                }
                is MemoAction.CounterReset -> {
                    nextAction.backupValues.forEach { dao.updateCounterValue(it.copy(count = 0)) }
                }
                is MemoAction.Update -> {
                    if (nextAction.newRecord == null) {
                        // 新しいデータがnull（削除アクション）なら、DBから消す
                        // oldRecordは必ず存在するので、そのIDを使って消します
                        nextAction.oldRecord?.let {
                            dao.deleteValuesByRecordId(it.id)
                            dao.deleteRecordById(it.id)
                        }
                    } else {
                        // 普通の保存・編集なら、今まで通り上書き保存
                        dao.insertRecord(nextAction.newRecord)
                        dao.deleteValuesByRecordId(nextAction.newRecord.id)
                        nextAction.newValues.forEach { dao.insertValue(it) }
                    }
                }
                is MemoAction.ResetMemos -> {
                    nextAction.backupRecords.forEach { record ->
                        dao.deleteValuesByRecordId(record.id)
                        dao.deleteRecordById(record.id)
                    }
                }
            }
            updateStackStates()
        }
    }

    // --- 各種操作 (履歴保存つき) ---

    fun updateMemoWithHistory(record: MemoRecord, values: List<MemoValue>) {
        viewModelScope.launch(Dispatchers.IO) {
            // 1. 保存する「前」の状態をバックアップ
            val oldRecord = dao.getRecordById(record.id)
            val oldValues = if (oldRecord != null) dao.getValuesForRecord(record.id) else emptyList()

            // 2. 履歴の箱（Stack）に入れる
            undoStack.push(MemoAction.Update(oldRecord, oldValues, record, values))
            redoStack.clear()

            // 3. 【修正：新規・編集どちらの場合も、必ずレコードを上書き確定させる】
            // これによりRoomが親レコードの生存を正しく認識し、Undo時の削除が100%通るようになります。
            dao.insertRecord(record)

            // 値を保存
            values.forEach { dao.insertValue(it) }

            // 4. ボタンを白く光らせる
            viewModelScope.launch(Dispatchers.Main) {
                updateStackStates()
            }
        }
    }

    fun resetAllMemosWithHistory(machineId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val currentRecords = dao.getRecordsByMachine(machineId)
            val currentValues = mutableListOf<MemoValue>()
            currentRecords.forEach {
                currentValues.addAll(dao.getValuesForRecord(it.id))
            }

            if (currentRecords.isNotEmpty()) {
                undoStack.push(MemoAction.ResetMemos(currentRecords, currentValues))
                redoStack.clear()

                currentRecords.forEach { record ->
                    dao.deleteValuesByRecordId(record.id)
                    dao.deleteRecordById(record.id)
                }
                updateStackStates()
            }
        }
    }

    fun updateCounterWithHistory(counterId: Int, isIncrement: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val current = dao.getCounterValue(counterId)
            if (current == null) {
                dao.updateCounterValue(CounterValue(counterId = counterId, count = if (isIncrement) 1 else 0))
            } else {
                dao.adjustCounterValue(counterId, if (isIncrement) 1 else -1)
            }

            undoStack.push(MemoAction.CounterUpdate(counterId, isIncrement))
            redoStack.clear()
            updateStackStates()
        }
    }

    fun resetAllCountersWithHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            val currentValues = dao.getAllCounterValues()
            if (currentValues.isNotEmpty()) {
                undoStack.push(MemoAction.CounterReset(currentValues))
                redoStack.clear()
                currentValues.forEach { dao.updateCounterValue(it.copy(count = 0)) }
                updateStackStates()
            }
        }
    }

    init {
        loadSettings()
    }

    fun loadSettings() {
        viewModelScope.launch(Dispatchers.IO) {
            val setting = dao.getSetting()
            if (setting != null) {
                viewModelScope.launch(Dispatchers.Main) {
                    showTimeSetting.value = setting.showTime
                }
            }
        }
    }
    fun deleteMemoWithHistory(recordId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            // 1. 消す前にデータをバックアップ
            val oldRecord = dao.getRecordById(recordId) ?: return@launch
            val oldValues = dao.getValuesForRecord(recordId)

            // 2. 「Update」の仕組みを応用して履歴に入れる
            //（古いデータがあり、新しいデータが空っぽ = 削除という意味になります）
            undoStack.push(MemoAction.Update(oldRecord, oldValues, null, emptyList()))
            redoStack.clear()

            // 3. 実際に削除する
            dao.deleteValuesByRecordId(recordId)
            dao.deleteRecordById(recordId)

            // 4. ボタンを白く光らせる
            viewModelScope.launch(Dispatchers.Main) {
                updateStackStates()
            }
        }
    }
}