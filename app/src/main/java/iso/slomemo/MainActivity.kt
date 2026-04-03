package iso.slomemo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.PopupProperties
import androidx.room.Room
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "memo-db"
        ).fallbackToDestructiveMigration().build()

        setContent {
            val view = androidx.compose.ui.platform.LocalView.current
            if (!view.isInEditMode) {
                SideEffect {
                    val window = (view.context as android.app.Activity).window
                    window.statusBarColor = android.graphics.Color.BLACK
                    window.navigationBarColor = android.graphics.Color.BLACK
                    val controller =
                        androidx.core.view.WindowCompat.getInsetsController(window, view)
                    controller.isAppearanceLightStatusBars = false
                    controller.isAppearanceLightNavigationBars = false
                }
            }

            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = Color.Black) {
                    TestColumnApp(db)
                }
            }
        }
    }

    @OptIn(
        ExperimentalFoundationApi::class,
        ExperimentalMaterial3Api::class,
        ExperimentalLayoutApi::class
    )
    @Composable
    fun TestColumnApp(db: AppDatabase) {
        var currentScreen by remember { mutableStateOf("main") }
        var columns by remember { mutableStateOf(listOf<ColumnSetting>()) }
        var records by remember { mutableStateOf(listOf<MemoRecord>()) }
        var showInputArea by remember { mutableStateOf(false) }
        var menuExpanded by remember { mutableStateOf(false) }

        var newColumnName by remember { mutableStateOf("") }
        var selectedColumnId by remember { mutableStateOf<Int?>(null) }
        var newOptionName by remember { mutableStateOf("") }

        val scope = rememberCoroutineScope()
        val inputValues = remember { mutableStateMapOf<Int, String>() }
        var editingRecordId by remember { mutableStateOf<Int?>(null) }

        fun refreshData() {
            scope.launch {
                columns = db.memoDao().getAllColumns()
                records = db.memoDao().getAllRecords()
            }
        }
        LaunchedEffect(Unit) { refreshData() }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {

            BackHandler(enabled = menuExpanded) {
                menuExpanded = false
            }

            BackHandler(enabled = showInputArea) {
                showInputArea = false
            }

            BackHandler(enabled = currentScreen == "settings") {
                currentScreen = "main"
            }

            // --- 2. メインの画面構造 ---
            Scaffold(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding(),
                containerColor = Color.White,
                topBar = {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White)
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (currentScreen == "main") "実戦データ" else "設定",
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.Black
                        )
                        Box {
                            IconButton(
                                onClick = { menuExpanded = true },
                                modifier = Modifier.offset(x = 12.dp, y = 0.dp)
                            ) {
                                Icon(Icons.Default.Menu, null, tint = Color.Black)
                            }

                            DropdownMenu(
                                expanded = menuExpanded,
                                onDismissRequest = { menuExpanded = false },
                                offset = DpOffset(x = (-12).dp, y = 0.dp),
                                modifier = Modifier.background(Color.White),
                                properties = PopupProperties(
                                    dismissOnBackPress = true
                                )
                            ) {
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            "+  実戦データ入力",
                                            fontSize = 18.sp,
                                            color = Color.Black
                                        )
                                    },
                                    onClick = {
                                        showInputArea = true
                                        menuExpanded = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            "⚙  項目・選択肢の設定",
                                            fontSize = 18.sp,
                                            color = Color.Black
                                        )
                                    },
                                    onClick = {
                                        currentScreen = "settings"
                                        menuExpanded = false
                                    }
                                )
                            }
                        }
                    }
                },
                floatingActionButton = {
                    if (currentScreen == "main" && !showInputArea) {
                        FloatingActionButton(
                            onClick = {
                                inputValues.clear()
                                editingRecordId = null
                                showInputArea = true
                            },
                            containerColor = Color(0xFF7E57C2),
                            contentColor = Color.White
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "入力")
                        }
                    }
                }
            ) { padding ->
                Box(
                    modifier = Modifier
                        .padding(padding)
                        .fillMaxSize()
                ) {
                    if (currentScreen == "main") {
                        // --- 履歴表示 ---
                        Column(modifier = Modifier.fillMaxSize()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFFEADDFF).copy(alpha = 0.5f))
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "時間",
                                    modifier = Modifier.width(50.dp),
                                    style = MaterialTheme.typography.labelMedium
                                )
                                columns.forEach { col ->
                                    Text(
                                        col.name,
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(horizontal = 2.dp),
                                        style = MaterialTheme.typography.labelMedium,
                                        maxLines = 1
                                    )
                                }
                                Spacer(modifier = Modifier.width(32.dp))
                            }
                            LazyColumn(modifier = Modifier.fillMaxSize()) {
                                items(records) { record ->
                                    HistoryRow(
                                        db = db,
                                        record = record,
                                        columns = columns,
                                        onRowClick = {
                                            scope.launch {
                                                val currentValues =
                                                    db.memoDao().getValuesForRecord(record.id)
                                                inputValues.clear()
                                                currentValues.forEach {
                                                    inputValues[it.columnId] = it.value
                                                }

                                                editingRecordId = record.id // 【追加】これを編集モードにする
                                                showInputArea = true
                                            }
                                        },
                                        onDelete = { refreshData() }
                                    )
                                }
                            }
                        }
                    } else {
                        // --- 【設定画面】 ---
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            Text("項目の追加", style = MaterialTheme.typography.titleMedium)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                TextField(
                                    value = newColumnName,
                                    onValueChange = { newColumnName = it },
                                    label = { Text("項目名") },
                                    modifier = Modifier.weight(1f)
                                )
                                Button(
                                    onClick = {
                                        if (newColumnName.isNotBlank()) {
                                            scope.launch {
                                                db.memoDao()
                                                    .insertColumn(ColumnSetting(name = newColumnName)); newColumnName =
                                                ""; refreshData()
                                            }
                                        }
                                    },
                                    modifier = Modifier.padding(start = 8.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF7E57C2)
                                    )
                                ) { Text("追加") }
                            }

                            Spacer(modifier = Modifier.height(24.dp)); Divider(); Spacer(
                            modifier = Modifier.height(
                                16.dp
                            )
                        )

                            Text(
                                "項目の編集・選択肢の編集",
                                style = MaterialTheme.typography.titleMedium
                            )
                            // 項目選択チップ
                            Row(
                                modifier = Modifier
                                    .horizontalScroll(rememberScrollState())
                                    .padding(vertical = 8.dp)
                            ) {
                                columns.forEach { col ->
                                    FilterChip(
                                        selected = selectedColumnId == col.id,
                                        onClick = { selectedColumnId = col.id },
                                        label = { Text(col.name) },
                                        modifier = Modifier.padding(end = 4.dp)
                                    )
                                }
                            }

                            // 項目が選択されている場合に編集欄を表示
                            selectedColumnId?.let { colId ->
                                val col = columns.find { it.id == colId } ?: return@let

                                // 項目名の編集欄
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "項目名の編集",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Gray
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    TextField(
                                        value = col.name,
                                        onValueChange = { newName ->
                                            // 空文字でもDBに送るようにする（これで全部消せる）
                                            scope.launch {
                                                db.memoDao().updateColumn(col.copy(name = newName))
                                                refreshData()
                                            }
                                        },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true,
                                        placeholder = { Text("項目名を入力...") } // 空になった時にガイドを出す
                                    )
                                    Spacer(modifier = Modifier.width(48.dp))
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                // 選択肢の追加・一覧
                                Text(
                                    "「${col.name}」の選択肢一覧",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    TextField(
                                        value = newOptionName,
                                        onValueChange = { newOptionName = it },
                                        label = { Text("新しい選択肢") },
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(onClick = {
                                        if (newOptionName.isNotBlank()) {
                                            scope.launch {
                                                val opts = col.options.toMutableList(); opts.add(
                                                newOptionName
                                            ); db.memoDao()
                                                .updateColumn(col.copy(options = opts)); newOptionName =
                                                ""; refreshData()
                                            }
                                        }
                                    }) { Icon(Icons.Default.Add, null) }
                                }
                                FlowRow(modifier = Modifier.fillMaxWidth()) {
                                    col.options.forEach { opt ->
                                        InputChip(
                                            selected = false,
                                            onClick = {
                                                scope.launch {
                                                    val opts =
                                                        col.options.toMutableList(); opts.remove(opt); db.memoDao()
                                                    .updateColumn(col.copy(options = opts)); refreshData()
                                                }
                                            },
                                            label = { Text(opt) },
                                            trailingIcon = {
                                                Icon(
                                                    Icons.Default.Close,
                                                    null,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            },
                                            modifier = Modifier.padding(4.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(24.dp))
                                Button(
                                    onClick = {
                                        scope.launch {
                                            db.memoDao().deleteColumn(col); selectedColumnId =
                                            null; refreshData()
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFFB3261E)
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                ) { Text("この項目自体を削除する") }
                            }
                        }
                    }
                }
            }
            // --- 3. 入力エリア (オーバーレイ) ---
            if (showInputArea) {
                // 背後を暗くするレイヤー（ここは変更なし）
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.4f))
                        .clickable { showInputArea = false }
                )

                // 入力フォーム本体
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.5f) // ★ここで画面の半分（50%）に固定
                        .align(Alignment.BottomCenter)
                        .background(Color.White)
                        .navigationBarsPadding()
                        .clickable(enabled = false) { }
                ) {
                    Column {
                        // 紫色のライン
                        Divider(thickness = 2.dp, color = Color(0xFF7E57C2))

                        // TestColumnApp の中にある InputFormContent を呼び出している部分
                        InputFormContent(
                            db = db,
                            columns = columns,
                            inputValues = inputValues,
                            editingRecordId = editingRecordId, // 【追加】
                            onSave = {
                                showInputArea = false
                                refreshData()
                            }
                        )
                    }
                }
            }
        }
    }

    @Composable
    fun InputFormContent(
        db: AppDatabase,
        columns: List<ColumnSetting>,
        inputValues: SnapshotStateMap<Int, String>,
        editingRecordId: Int?, // ← これを 4つ目の引数として追加！
        onSave: () -> Unit
    ) {
        val scope = rememberCoroutineScope()
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            Text("新規メモ入力", style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(16.dp))
            columns.forEach { column ->
                Text(
                    text = column.name,
                    style = MaterialTheme.typography.labelLarge,
                    color = Color(0xFF7E57C2)
                )
                OutlinedTextField(
                    value = inputValues[column.id] ?: "",
                    onValueChange = { inputValues[column.id] = it },
                    modifier = Modifier.fillMaxWidth()
                )
                if (column.options.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .padding(vertical = 8.dp)
                            .horizontalScroll(rememberScrollState())
                    ) {
                        column.options.forEach { option ->
                            SuggestionChip(
                                onClick = { inputValues[column.id] = option },
                                label = { Text(option) },
                                modifier = Modifier.padding(end = 4.dp)
                            )
                        }
                    }
                }
            }
            Button(
                onClick = {
                    scope.launch {
                        // 1. レコードIDの確定
                        val rid = if (editingRecordId != null) {
                            // 【重要】既存の値を物理削除して、クリーンな状態にする
                            db.memoDao().deleteValuesByRecordId(editingRecordId)
                            editingRecordId.toLong()
                        } else {
                            db.memoDao().insertRecord(MemoRecord())
                        }

                        // 2. 新しい値を一つずつ保存
                        inputValues.forEach { (cid, txt) ->
                            if (txt.isNotBlank()) {
                                db.memoDao().insertValue(
                                    MemoValue(
                                        recordId = rid.toInt(),
                                        columnId = cid,
                                        value = txt
                                    )
                                )
                            }
                        }

                        // 3. 【ここが重要】保存が終わったことを親に伝えて画面を閉じる
                        onSave()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 32.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7E57C2))
            ) {
                Text(if (editingRecordId != null) "変更を保存" else "保存して履歴に追加")
            }
        }
    }

    @Composable
    fun HistoryRow(
        db: AppDatabase,
        record: MemoRecord,
        columns: List<ColumnSetting>,
        onRowClick: () -> Unit, // ← 「タップされた時の動き」を受け取れるように追加
        onDelete: () -> Unit
    ) {
        var values by remember { mutableStateOf(listOf<MemoValue>()) }
        val scope = rememberCoroutineScope()

        LaunchedEffect(record.id) {
            values = db.memoDao().getValuesForRecord(record.id)
        }

        Column(
            // modifier に .clickable { onRowClick() } を追加して、行全体をボタンにします
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onRowClick() }
        ) {
            Row(
                modifier = Modifier
                    .padding(vertical = 4.dp, horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val timeText = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
                    .format(record.timestamp)
                Text(
                    text = timeText,
                    modifier = Modifier.width(50.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )

                columns.forEach { col ->
                    val valObj = values.find { it.columnId == col.id }
                    Text(
                        text = valObj?.value ?: "-",
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 2.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1
                    )
                }

                IconButton(onClick = {
                    scope.launch {
                        db.memoDao().deleteValuesByRecordId(record.id)
                        db.memoDao().deleteRecord(record)
                        onDelete()
                    }
                }, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "削除",
                        tint = Color.LightGray.copy(alpha = 0.5f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Divider(thickness = 0.5.dp, color = Color.LightGray.copy(alpha = 0.2f))
        }
    }
}