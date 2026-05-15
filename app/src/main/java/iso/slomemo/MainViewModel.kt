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
    sealed class MemoAction {
        // カウンターのIDと、増やしたのか減らしたのかを覚えるルール
        data class CounterUpdate(
            val counterId: Int,
            val isIncrement: Boolean
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

    fun undo() {
        viewModelScope.launch(Dispatchers.IO) {
            // 箱が空っぽなら何もしない
            if (undoStack.isEmpty()) return@launch

            // ① 箱から「一番新しい記憶」を取り出す
            val lastAction = undoStack.pop()

            // ② 「やり直し(Redo)」のために、別の箱へ移動しておく
            redoStack.push(lastAction)

            // ③ 取り出した記憶の内容に合わせて、逆の操作をする
            when (lastAction) {
                is MemoAction.CounterUpdate -> {
                    // 「増やした」記憶なら「1減らす」、「減らした」記憶なら「1増やす」
                    val undoDiff = if (lastAction.isIncrement) -1 else 1
                    dao.adjustCounterValue(lastAction.counterId, undoDiff)
                }
            }

            // ④ ボタンの明るさを最新の状態にする
            updateStackStates()
        }
    }
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
            // 実際に数字を増減させる
            val diff = if (isIncrement) 1 else -1
            dao.adjustCounterValue(counterId, diff)

            // ★ここを追加：箱に今の操作を覚えさせる
            undoStack.push(MemoAction.CounterUpdate(counterId, isIncrement))
            redoStack.clear() // 新しい操作をしたのでやり直し（Redo）はクリア

            // ★ここを追加：ボタンを明るくする
            updateStackStates()
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