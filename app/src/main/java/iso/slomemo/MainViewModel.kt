package iso.slomemo

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import iso.slomemo.db.AppDatabase
import iso.slomemo.db.RowEntity
import kotlinx.coroutines.launch
import androidx.compose.runtime.mutableStateListOf

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = Room.databaseBuilder(
        application,
        AppDatabase::class.java,
        "slomemo-db"
    ).build()

    private val dao = db.rowDao()

    var rows = mutableStateListOf<RowEntity>()
        private set

    init {
        viewModelScope.launch {
            val data = dao.getAll()

            if (data.isEmpty()) {
                repeat(100) {
                    dao.insert(RowEntity())
                }
                rows.addAll(dao.getAll())
            } else {
                rows.addAll(data)
            }
        }
    }

    fun updateRow(index: Int, newRow: RowEntity) {
        rows[index] = newRow

        viewModelScope.launch {
            dao.update(newRow)
        }
    }
}