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

    // データベースの設定
    val db = Room.databaseBuilder(
        application,
        AppDatabase::class.java,
        "memo-db"
    ).fallbackToDestructiveMigration().build()

    val dao = db.memoDao()

    var showTimeSetting = mutableStateOf(true)

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

    // --- ここが履歴の仕組み ---

    sealed class MemoAction {
        data class Reset(val backupRecords: List<MemoRecord>, val backupValues: List<MemoValue>) : MemoAction()
        data class Update(val oldRecord: MemoRecord, val oldValues: List<MemoValue>, val newRecord: MemoRecord, val newValues: List<MemoValue>) : MemoAction()
        data class Delete(val record: MemoRecord, val values: List<MemoValue>) : MemoAction()
        data class CounterUpdate(val counterId: Int, val isIncrement: Boolean) : MemoAction()
        data class CounterReset(val backupValues: List<CounterValue>) : MemoAction()
    }

    // ★エラーの原因：この2行が消えていました。履歴を保存する「箱」です。
    private val undoStack = Stack<MemoAction>()
    private val redoStack = Stack<MemoAction>()

    // ボタンの明るさを決めるスイッチ
    var canUndo = mutableStateOf(false)
    var canRedo = mutableStateOf(false)

    // 箱の中身を確認して、ボタンの明るさを更新する命令
    private fun updateStackStates() {
        canUndo.value = undoStack.isNotEmpty()
        canRedo.value = redoStack.isNotEmpty()
    }

    // 全削除（履歴付き）
    fun resetAllMemosWithHistory(machineId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val currentRecords = dao.getRecordsByMachine(machineId)
            val recordIds = currentRecords.map { it.id }
            val currentValues = dao.getAllValues().filter { it.recordId in recordIds }

            if (currentRecords.isNotEmpty()) {
                undoStack.push(MemoAction.Reset(currentRecords, currentValues))
                redoStack.clear()
                currentRecords.forEach { record ->
                    dao.deleteValuesByRecordId(record.id)
                    dao.deleteRecordById(record.id)
                }
                updateStackStates()
            }
        }
    }

    // 元に戻す（Undo）
    fun undo() {
        viewModelScope.launch(Dispatchers.IO) {
            if (undoStack.isEmpty()) return@launch
            val lastAction = undoStack.pop()
            redoStack.push(lastAction)

            when (lastAction) {
                is MemoAction.Reset -> {
                    lastAction.backupRecords.forEach { dao.insertRecord(it) }
                    lastAction.backupValues.forEach { dao.insertValue(it) }
                }
                is MemoAction.Update -> {
                    dao.insertRecord(lastAction.oldRecord)
                    dao.deleteValuesByRecordId(lastAction.oldRecord.id)
                    lastAction.oldValues.forEach { dao.insertValue(it) }
                }
                is MemoAction.Delete -> {
                    dao.deleteValuesByRecordId(lastAction.record.id)
                    dao.deleteRecordById(lastAction.record.id)
                }
                is MemoAction.CounterUpdate -> {
                    val undoDiff = if (lastAction.isIncrement) -1 else 1
                    dao.adjustCounterValue(lastAction.counterId, undoDiff)
                }
                is MemoAction.CounterReset -> {
                    lastAction.backupValues.forEach { dao.updateCounterValue(it) }
                }
            }
            updateStackStates()
        }
    }

    // やり直し（Redo）
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
                is MemoAction.Update -> {
                    undoStack.push(nextAction)
                    dao.insertRecord(nextAction.newRecord)
                    dao.deleteValuesByRecordId(nextAction.newRecord.id)
                    nextAction.newValues.forEach { dao.insertValue(it) }
                }
                is MemoAction.Delete -> {
                    undoStack.push(nextAction)
                    dao.insertRecord(nextAction.record)
                    nextAction.values.forEach { dao.insertValue(it) }
                }
                is MemoAction.CounterUpdate -> {
                    undoStack.push(nextAction)
                    val redoDiff = if (nextAction.isIncrement) 1 else -1
                    dao.adjustCounterValue(nextAction.counterId, redoDiff)
                }
                is MemoAction.CounterReset -> {
                    undoStack.push(nextAction)
                    nextAction.backupValues.forEach { value ->
                        dao.updateCounterValue(value.copy(count = 0))
                    }
                }
            }
            updateStackStates()
        }
    }

    // メモ保存（履歴付き）
    fun updateMemoWithHistory(record: MemoRecord, values: List<MemoValue>) {
        viewModelScope.launch(Dispatchers.IO) {
            val oldRecord = dao.getRecordById(record.id)
            if (oldRecord == null) {
                undoStack.push(MemoAction.Delete(record, values))
            } else {
                val oldValues = dao.getValuesForRecord(record.id)
                undoStack.push(MemoAction.Update(oldRecord, oldValues, record, values))
            }
            redoStack.clear()
            dao.insertRecord(record)
            values.forEach { dao.insertValue(it) }
            updateStackStates()
        }
    }

    // カウンター更新（履歴付き）
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

    // カウンターリセット（履歴付き）
    fun resetAllCountersWithHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            val currentValues = dao.getAllCounterValues()
            if (currentValues.isNotEmpty()) {
                undoStack.push(MemoAction.CounterReset(currentValues))
                redoStack.clear()
                currentValues.forEach { value ->
                    dao.updateCounterValue(value.copy(count = 0))
                }
                updateStackStates()
            }
        }
    }
}