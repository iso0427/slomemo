package iso.slomemo.db

import androidx.room.*

@Dao
interface RowDao {

    @Query("SELECT * FROM rows")
    suspend fun getAll(): List<RowEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(row: RowEntity)

    @Update
    suspend fun update(row: RowEntity)

    @Query("DELETE FROM rows")
    suspend fun deleteAll()
}