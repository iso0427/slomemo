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
            val newRecord: MemoRecord,
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
                    if (lastAction.oldRecord == null) {
                        dao.deleteValuesByRecordId(lastAction.newRecord.id)
                        dao.deleteRecordById(lastAction.newRecord.id)
                    } else {
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
                    dao.insertRecord(nextAction.newRecord)
                    dao.deleteValuesByRecordId(nextAction.newRecord.id)
                    nextAction.newValues.forEach { dao.insertValue(it) }
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
            // 1. 保存する「前」の状態をバックアップ（編集時のみデータがある）
            val oldRecord = dao.getRecordById(record.id)
            val oldValues = if (oldRecord != null) dao.getValuesForRecord(record.id) else emptyList()

            // 2. 履歴の箱（Stack）に入れる
            // すでに画面側で作られた「正しいID」が入った record をそのまま使います
            undoStack.push(MemoAction.Update(oldRecord, oldValues, record, values))
            redoStack.clear()

            // 3. 実際の保存（上書き・追記）
            dao.insertRecord(record)
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
}