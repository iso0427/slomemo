package iso.slomemo.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [RowEntity::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun rowDao(): RowDao
}