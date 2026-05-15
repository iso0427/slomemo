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

    // --- ここからやり直し用の「空」の機能 ---

    // 履歴の定義（一旦、最小限に）
    sealed class MemoAction

    private val undoStack = Stack<MemoAction>()
    private val redoStack = Stack<MemoAction>()

    var canUndo = mutableStateOf(false)
    var canRedo = mutableStateOf(false)

    private fun updateStackStates() {
        canUndo.value = undoStack.isNotEmpty()
        canRedo.value = redoStack.isNotEmpty()
    }

    fun undo() {}
    fun redo() {}

    // ★ 他のファイルでエラーになっている関数を「空」で用意します
    // これで赤文字が消えるはずです

    fun updateMemoWithHistory(record: MemoRecord, values: List<MemoValue>) {
        viewModelScope.launch(Dispatchers.IO) {
            // 今は履歴を残さず、普通に保存するだけ
            dao.insertRecord(record)
            values.forEach { dao.insertValue(it) }
        }
    }

    fun resetAllMemosWithHistory(machineId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            // 今は履歴を残さず、普通に消すだけ
            val currentRecords = dao.getRecordsByMachine(machineId)
            currentRecords.forEach { record ->
                dao.deleteValuesByRecordId(record.id)
                dao.deleteRecordById(record.id)
            }
        }
    }

    fun updateCounterWithHistory(counterId: Int, isIncrement: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            // 今は履歴を残さず、普通に増減させるだけ
            val diff = if (isIncrement) 1 else -1
            dao.adjustCounterValue(counterId, diff)
        }
    }

    fun resetAllCountersWithHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            // 今は履歴を残さず、普通に0にするだけ
            val currentValues = dao.getAllCounterValues()
            currentValues.forEach { value ->
                dao.updateCounterValue(value.copy(count = 0))
            }
        }
    }

    // --- ここまで ---

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