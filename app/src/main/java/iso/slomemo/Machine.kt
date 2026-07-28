package iso.slomemo

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "machines")
data class Machine(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val position: Int = 0,

    val showSimpleCounter: Boolean = true,
    val showTotalRotation: Boolean = true,
    val showCounterName: Boolean = true,
    val counterHeight: Int = 60,
    val counterFontSize: Int = 32,
    val rotationFontSize: Int = 30,
    val rateFontSize: Int = 65
)