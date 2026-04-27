package iso.slomemo

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface MachineDao {
    // 機種一覧をすべて取得（名前順）
    @Query("SELECT * FROM machines ORDER BY position ASC")
    fun getAllMachines(): Flow<List<Machine>>

    // 新しい機種を登録
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMachine(machine: Machine): Long

    // 機種を削除（これだけで紐付くデータも消えるように後で設定します）
    @Delete
    suspend fun deleteMachine(machine: Machine)

    // 特定のIDの機種を取得
    @Query("SELECT * FROM machines WHERE id = :id")
    suspend fun getMachineById(id: Int): Machine?

    @Update
    suspend fun updateMachine(machine: Machine)

    @Update
    suspend fun updateMachines(machines: List<Machine>)

    @Query("SELECT MAX(position) FROM machines")
    suspend fun getMaxPosition(): Int?




}