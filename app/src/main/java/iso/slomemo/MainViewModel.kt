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
        "slomemo-db"
    )
        // 構造が変わったので、古いデータがある場合は一旦消して作り直す設定
        .fallbackToDestructiveMigration()
        .build()

    // ② 新しい memoDao を使う
    val dao = db.memoDao()


// --- ここから追加 ---

    // 操作の種類を定義（今回は「リセット」を履歴に積む）
    sealed class MemoAction {
        data class Reset(val backupRecords: List<MemoRecord>) : MemoAction()
        // 今後、単発の削除などもここに追加できる
    }

    // Undo用スタック（過去に戻る）
    private val undoStack = Stack<MemoAction>()

    // Redo用スタック（進み直す）
    private val redoStack = Stack<MemoAction>()

    // 状態監視用（ボタンの有効/無効を切り替えるのに使う）
    var canUndo = mutableStateOf(false)
    var canRedo = mutableStateOf(false)

    private fun updateStackStates() {
        canUndo.value = undoStack.isNotEmpty()
        canRedo.value = redoStack.isNotEmpty()
    }

    // --- 修正版：MainViewModel.kt ---

    // リセット実行
    fun resetAllMemosWithHistory() {
        // ★ Dispatchers.IO を追加
        viewModelScope.launch(Dispatchers.IO) {
            val currentRecords = dao.getAllRecords()
            if (currentRecords.isNotEmpty()) {
                undoStack.push(MemoAction.Reset(currentRecords))
                redoStack.clear()

                dao.deleteAllMemoRecords()
                updateStackStates()
            }
        }
    }

    // Undo（元に戻す）
    fun undo() {
        // ★ Dispatchers.IO を追加
        viewModelScope.launch(Dispatchers.IO) {
            if (undoStack.isEmpty()) return@launch
            val lastAction = undoStack.pop()

            when (lastAction) {
                is MemoAction.Reset -> {
                    redoStack.push(lastAction)
                    // データを書き戻す
                    lastAction.backupRecords.forEach { record ->
                        dao.insertRecord(record)
                    }
                }
            }
            updateStackStates()
        }
    }

    // Redo（やり直し）
    fun redo() {
        // ★ Dispatchers.IO を追加
        viewModelScope.launch(Dispatchers.IO) {
            if (redoStack.isEmpty()) return@launch
            val nextAction = redoStack.pop()

            when (nextAction) {
                is MemoAction.Reset -> {
                    undoStack.push(nextAction)
                    dao.deleteAllMemoRecords()
                }
            }
            updateStackStates()
        }
    }
}