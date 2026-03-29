package iso.slomemo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.room.Room
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "memo-db"
        ).fallbackToDestructiveMigration() // 構造が変わった時にデータをリセットする設定
            .build()

        setContent {
            TestColumnApp(db)
        }
    }
}

@Composable
fun TestColumnApp(db: AppDatabase) {
    var columnName by remember { mutableStateOf("") }
    var columns by remember { mutableStateOf(listOf<ColumnSetting>()) }
    val scope = rememberCoroutineScope()

    // 起動時に現在の項目一覧を読み込む
    LaunchedEffect(Unit) {
        columns = db.memoDao().getAllColumns()
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("項目設定テスト", style = MaterialTheme.typography.headlineMedium)

        Row(modifier = Modifier.padding(vertical = 8.dp)) {
            TextField(
                value = columnName,
                onValueChange = { columnName = it },
                label = { Text("項目名を入力を（例：pt）") },
                modifier = Modifier.weight(1f)
            )
            Button(onClick = {
                if (columnName.isNotBlank()) {
                    scope.launch {
                        // 新しい項目を保存
                        val newCol = ColumnSetting(name = columnName, orderIndex = columns.size)
                        db.memoDao().insertColumn(newCol)
                        // 再読み込み
                        columns = db.memoDao().getAllColumns()
                        columnName = ""
                    }
                }
            }) {
                Text("追加")
            }
        }

        Text("現在の項目一覧：", modifier = Modifier.padding(top = 16.dp))

        LazyColumn {
            items(columns) { column ->
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Text(text = column.name, modifier = Modifier.padding(16.dp))
                }
            }
        }
    }
}