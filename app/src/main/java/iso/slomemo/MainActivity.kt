package iso.slomemo

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // DB作成
        val db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "memo-db"
        ).build()

        setContent {
            MemoApp(db)
        }
    }
}

@Composable
fun MemoApp(db: AppDatabase) {

    var text by remember { mutableStateOf("") }
    var memos by remember { mutableStateOf(listOf<Memo>()) }

    val scope = rememberCoroutineScope()

    // 初回読み込み
    LaunchedEffect(Unit) {
        memos = db.memoDao().getAll()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        Row(verticalAlignment = Alignment.CenterVertically) {

            TextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("メモ入力") }
            )

            Spacer(modifier = Modifier.width(8.dp))

            Button(onClick = {
                if (text.isNotBlank()) {
                    scope.launch {
                        db.memoDao().insert(Memo(text = text))
                        memos = db.memoDao().getAll()
                        text = ""
                    }
                }
            }) {
                Text("追加")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn {
            items(memos) { memo ->
                Text(
                    text = memo.text,
                    modifier = Modifier.padding(8.dp)
                )
            }
        }
    }
}