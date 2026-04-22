package iso.slomemo

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MachineSelectionScreen(
    db: AppDatabase,
    onMachineSelected: (Int) -> Unit // 機種が選ばれた時のアクション
) {
    // データベースから機種一覧をリアルタイムで取得
    val machines by db.machineDao().getAllMachines().collectAsState(initial = emptyList())
    var newMachineName by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = { TopAppBar(title = { Text("機種選択") }) }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {
            // 機種追加エリア
            Row(modifier = Modifier.fillMaxWidth()) {
                TextField(
                    value = newMachineName,
                    onValueChange = { newMachineName = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("新機種名を入力") }
                )
                Button(
                    onClick = {
                        if (newMachineName.isNotBlank()) {
                            // 本来はViewModelでやるべきですが、まずはシンプルに
                            val name = newMachineName
                            newMachineName = ""
                            kotlinx.coroutines.launch {
                                db.machineDao().insertMachine(Machine(name = name))
                            }
                        }
                    },
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Text("追加")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 機種一覧リスト
            LazyColumn {
                items(machines) { machine ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable { onMachineSelected(machine.id) } // IDを渡して画面遷移
                    ) {
                        Text(
                            text = machine.name,
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        }
    }
}