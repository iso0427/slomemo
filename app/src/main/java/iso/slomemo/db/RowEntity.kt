package iso.slomemo.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "rows")
data class RowEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val pt: String = "",
    val keiki: String = "",
    val shubetsu: String = "",
    val at: String = "",
    val bigEnd: String = "",
    val atEnd: String = "",
    val story: String = ""
)