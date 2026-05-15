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

    var showTimeSetting = mutableStateOf(true)

    init {
        loadSettings()
    }

    fun loadSettings() {
        viewModelScope.launch(Dispatchers.IO) {
            val setting = dao.getSetting() // getSetting() は AppSetting を 1件取得する想定
            if (setting != null) {
                // メインスレッドで値を反映
                viewModelScope.launch(Dispatchers.Main) {
                    showTimeSetting.value = setting.showTime
                }
            }
        }
    }

    // 1. MemoAction の定義を拡張
    sealed class MemoAction {
        // リセット用（既存）
        data class Reset(
            val backupRecords: List<MemoRecord>,
            val backupValues: List<MemoValue>
        ) : MemoAction()

        // 編集（Update）用（既存）
        data class Update(
            val oldRecord: MemoRecord,
            val oldValues: List<MemoValue>,
            val newRecord: MemoRecord,
            val newValues: List<MemoValue>
        ) : MemoAction()

        // ★ 追加：カウンター操作用
        data class CounterUpdate(
            val counterId: Int,
            val isIncrement: Boolean // 増やした操作なら true、減らした操作なら false
        ) : MemoAction()

        data class CounterReset(
            val backupValues: List<CounterValue>
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

    fun resetAllMemosWithHistory(machineId: Int) { // ★ 引数に machineId を追加
        viewModelScope.launch(Dispatchers.IO) {
            // ★ 機種別のレコードを取得するように修正
            val currentRecords = dao.getRecordsByMachine(machineId)

            // Valueの方は、全取得してからこの機種に関連するものだけに絞り込む
            // (またはDaoに getValuesByMachine(machineId) を作っても良いですが、一旦これで)
            val recordIds = currentRecords.map { it.id }
            val currentValues = dao.getAllValues().filter { it.recordId in recordIds }

            if (currentRecords.isNotEmpty()) {
                // 履歴に積む
                undoStack.push(MemoAction.Reset(currentRecords, currentValues))
                redoStack.clear()

                // ★ 削除も「その機種に関連するもの」だけに限定する
                // もし Dao に deleteValuesByMachine 等がない場合は、
                // recordIds をループして消すか、Daoに専用メソッドを追加してください。
                currentRecords.forEach { record ->
                    dao.deleteValuesByRecordId(record.id)
                    dao.deleteRecordById(record.id)
                }

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

                is MemoAction.Update -> {
                    redoStack.push(lastAction)
                    dao.insertRecord(lastAction.oldRecord)
                    dao.deleteValuesByRecordId(lastAction.oldRecord.id)
                    lastAction.oldValues.forEach { dao.insertValue(it) }
                }
                // ★ここを追加：カウンターを元に戻す処理
                is MemoAction.CounterUpdate -> {
                    redoStack.push(lastAction)
                    // 元に戻すので、増やしたなら -1、減らしたなら +1 する
                    val undoDiff = if (lastAction.isIncrement) -1 else 1
                    dao.adjustCounterValue(lastAction.counterId, undoDiff)
                }
                is MemoAction.CounterReset -> {
                    redoStack.push(lastAction)
                    lastAction.backupValues.forEach { dao.updateCounterValue(it) }
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

                is MemoAction.Update -> {
                    undoStack.push(nextAction)
                    dao.insertRecord(nextAction.newRecord)
                    dao.deleteValuesByRecordId(nextAction.newRecord.id)
                    nextAction.newValues.forEach { dao.insertValue(it) }
                }
                // ★ここを追加：カウンターをやり直す処理
                is MemoAction.CounterUpdate -> {
                    undoStack.push(nextAction)
                    // やり直しなので、もともとの操作と同じ数値を送る
                    val redoDiff = if (nextAction.isIncrement) 1 else -1
                    dao.adjustCounterValue(nextAction.counterId, redoDiff)
                }
                is MemoAction.CounterReset -> {
                    undoStack.push(nextAction)
                    // やり直し（Redo）なので、全ての値を 0 に書き換える
                    nextAction.backupValues.forEach { value ->
                        dao.updateCounterValue(value.copy(count = 0))
                    }
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

    fun updateCounterWithHistory(counterId: Int, isIncrement: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val diff = if (isIncrement) 1 else -1

            // 1. まず、今の値がDBにあるか確認する
            val current = dao.getCounterValue(counterId)

            if (current == null) {
                // 2-A. データがなければ、新規作成して保存する（初期値を 1 または 0 に）
                val newValue = if (isIncrement) 1 else 0
                dao.updateCounterValue(CounterValue(counterId = counterId, count = newValue))
            } else {
                // 2-B. すでにデータがあれば、既存の adjustCounterValue で増減させる
                dao.adjustCounterValue(counterId, diff)
            }

            // Undo/Redoなどの既存処理
            undoStack.push(MemoAction.CounterUpdate(counterId, isIncrement))
            redoStack.clear()
            updateStackStates()
        }
    }

    fun resetAllCountersWithHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            // 1. 現在の全カウンターの値をバックアップ
            val currentValues = dao.getAllCounterValues()

            if (currentValues.isNotEmpty()) {
                // 2. 履歴に積む（CounterResetアクション）
                undoStack.push(MemoAction.CounterReset(currentValues))
                redoStack.clear()

                // 3. 全てのカウンターを 0 に更新する
                currentValues.forEach { value ->
                    dao.updateCounterValue(value.copy(count = 0))
                }

                updateStackStates()
            }
        }
    }
}