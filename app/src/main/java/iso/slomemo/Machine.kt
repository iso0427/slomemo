package iso.slomemo

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "machines")
data class Machine(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val position: Int = 0
)