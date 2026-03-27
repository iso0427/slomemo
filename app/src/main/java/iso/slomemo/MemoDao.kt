package iso.slomemo

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface MemoDao {

    @Insert
    suspend fun insert(memo: Memo)

    @Query("SELECT * FROM Memo")
    suspend fun getAll(): List<Memo>
}