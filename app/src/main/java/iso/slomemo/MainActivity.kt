package iso.slomemo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.room.Room
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 【修正1】全画面表示（EdgeToEdge）を有効にする
        enableEdgeToEdge()

        val db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "memo-db"
        ).fallbackToDestructiveMigration().build()

        setContent {
            // 【修正2】システムバー（上下の帯）の色とアイコンの色を「強制」する
            val view = androidx.compose.ui.platform.LocalView.current
            if (!view.isInEditMode) {
                androidx.compose.runtime.SideEffect {
                    val window = (view.context as android.app.Activity).window

                    // 背景を「黒」に固定
                    window.statusBarColor = android.graphics.Color.BLACK
                    window.navigationBarColor = android.graphics.Color.BLACK

                    // 文字を「白」にする (isAppearanceLightStatusBars = false)
                    val controller =
                        androidx.core.view.WindowCompat.getInsetsController(window, view)
                    controller.isAppearanceLightStatusBars = false
                    controller.isAppearanceLightNavigationBars = false
                }
            }

            androidx.compose.material3.MaterialTheme {
                // 【修正3】Surfaceの背景色を黒にすると、ステータスバーとの隙間が目立たなくなります
                androidx.compose.material3.Surface(
                    modifier = androidx.compose.ui.Modifier.fillMaxSize(),
                    color = androidx.compose.ui.graphics.Color.Black // ここを黒に変更
                ) {
                    TestColumnApp(db)
                }
            }
        }
    }

    @OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
    @Composable
    fun TestColumnApp(db: AppDatabase) {
        var currentScreen by remember { mutableStateOf("main") }
        var columns by remember { mutableStateOf(listOf<ColumnSetting>()) }
        var records by remember { mutableStateOf(listOf<MemoRecord>()) }
        var showSheet by remember { mutableStateOf(false) }
        var expanded by remember { mutableStateOf(false) }

        var newColumnName by remember { mutableStateOf("") }
        var selectedColumnId by remember { mutableStateOf<Int?>(null) }
        var newOptionName by remember { mutableStateOf("") }

        val scope = rememberCoroutineScope()

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
            // --- ここから一括管理（書き換え後） ---

// 1. メニュー用の網（expandedがtrueの時だけ有効化）
            androidx.activity.compose.BackHandler(enabled = expanded) {
                expanded = false
            }

// 2. ボトムシート用の網（showSheetがtrueの時だけ有効化）
            androidx.activity.compose.BackHandler(enabled = showSheet) {
                showSheet = false
            }

// 3. 設定画面用の網（設定画面かつ、メニューもシートも閉じてる時だけ有効化）
            androidx.activity.compose.BackHandler(enabled = currentScreen == "settings" && !expanded && !showSheet) {
                currentScreen = "main"
            }

// --- ここまで ---

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
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (currentScreen == "main") "実戦データ" else "項目・選択肢の設定",
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.Black
                        )

                        Box {
                            IconButton(
                                onClick = { expanded = true },
                                modifier = Modifier.offset(x = (10).dp, y = 0.dp)
                            ) {

                                Icon(
                                    Icons.Default.Menu,
                                    contentDescription = "メニュー",
                                    tint = Color.Black
                                )
                            }

                            // 【ここを追加！】メニューが開いている間、戻るボタンでメニューを閉じる
                            androidx.activity.compose.BackHandler(enabled = expanded) {
                                expanded = false
                            }

                            DropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false },
                                modifier = Modifier.background(Color.White), // ★末尾にカンマを追加
                                offset = androidx.compose.ui.unit.DpOffset(x = (-10).dp, y = 0.dp)
                            ) {

                                val menuTextStyle = androidx.compose.ui.text.TextStyle(
                                    fontSize = 18.sp,
                                    color = Color.Black
                                )

                                DropdownMenuItem(
                                    text = { Text("実戦データ入力", style = menuTextStyle) },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.Add,
                                            null,
                                            tint = Color.Gray
                                        )
                                    },
                                    onClick = {
                                        currentScreen = "main"
                                        expanded = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("項目・選択肢の設定", style = menuTextStyle) },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.Settings,
                                            null,
                                            tint = Color.Gray
                                        )
                                    },
                                    onClick = {
                                        currentScreen = "settings"
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                },
                // ★追加：メイン画面の時だけ、右下に入力ボタンを出す
                floatingActionButton = {
                    if (currentScreen == "main") {
                        androidx.compose.material3.FloatingActionButton(
                            onClick = { showSheet = true },
                            containerColor = MaterialTheme.colorScheme.primary
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "入力")
                        }
                    }
                }
                // 181行目付近
            ) { padding ->

                if (currentScreen == "main") {
                    Column(
                        modifier = Modifier
                            .padding(padding)
                            .fillMaxSize()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .padding(vertical = 8.dp, horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "時間",
                                modifier = Modifier.width(50.dp),
                                style = MaterialTheme.typography.labelMedium
                            )
                            columns.forEach { col ->
                                Text(
                                    text = col.name,
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(horizontal = 2.dp),
                                    style = MaterialTheme.typography.labelMedium,
                                    maxLines = 1
                                )
                            }
                            Spacer(modifier = Modifier.width(32.dp))
                        }
                        LazyColumn(modifier = Modifier.weight(1f)) {
                            items(records) { record ->
                                HistoryRow(
                                    db = db,
                                    record = record,
                                    columns = columns,
                                    onDelete = { refreshData() })
                            }
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .padding(padding)
                            .fillMaxSize()
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        // ...（以下、設定画面の内容：変更なし）
                        // ...（ここから下の「項目の追加」などは変更なし）
                        Text("項目の追加", style = MaterialTheme.typography.titleMedium)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TextField(
                                value = newColumnName,
                                onValueChange = { newColumnName = it },
                                label = { Text("項目名（例：BB回数）") },
                                modifier = Modifier.weight(1f)
                            )
                            Button(
                                onClick = {
                                    if (newColumnName.isNotBlank()) {
                                        scope.launch {
                                            db.memoDao()
                                                .insertColumn(ColumnSetting(name = newColumnName))
                                            newColumnName = ""
                                            refreshData()
                                        }
                                    }
                                },
                                modifier = Modifier.padding(start = 8.dp)
                            ) { Text("追加") }
                        }

                        Spacer(modifier = Modifier.height(24.dp))
                        Divider()
                        Spacer(modifier = Modifier.height(16.dp))

                        Text("選択肢の編集", style = MaterialTheme.typography.titleMedium)
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

                        selectedColumnId?.let { colId ->
                            val selectedColumn = columns.find { it.id == colId }
                            selectedColumn?.let { col ->
                                Spacer(modifier = Modifier.height(8.dp))
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
                                                val currentOptions = col.options.toMutableList()
                                                currentOptions.add(newOptionName)
                                                db.memoDao()
                                                    .updateColumn(col.copy(options = currentOptions))
                                                newOptionName = ""
                                                refreshData()
                                            }
                                        }
                                    }) { Icon(Icons.Default.Add, contentDescription = "追加") }
                                }

                                @OptIn(ExperimentalLayoutApi::class)
                                FlowRow(modifier = Modifier.fillMaxWidth()) {
                                    col.options.forEach { opt ->
                                        InputChip(
                                            selected = false,
                                            onClick = {
                                                scope.launch {
                                                    val currentOptions = col.options.toMutableList()
                                                    currentOptions.remove(opt)
                                                    db.memoDao()
                                                        .updateColumn(col.copy(options = currentOptions))
                                                    refreshData()
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

                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = {
                                        scope.launch {
                                            db.memoDao().deleteColumn(col)
                                            selectedColumnId = null
                                            refreshData()
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                    modifier = Modifier.fillMaxWidth()
                                ) { Text("この項目自体を削除する") }
                            }
                        }
                    }
                }

                if (showSheet) {
                    ModalBottomSheet(
                        onDismissRequest = {
                            // 戻るボタンやスワイプでここが呼ばれる
                            showSheet = false
                        }
                    ) {
                        InputFormContent(
                            db = db,
                            columns = columns,
                            onSave = {
                                showSheet = false // 保存時も確実に閉じる
                                refreshData()
                            }
                        )
                    }
                }
            } // Scaffoldの終わり
        } // Boxの終わり
    } // TestColumnApp関数の終わり

    // --- ここから下は別の関数として独立させる ---

    @Composable
    fun InputFormContent(db: AppDatabase, columns: List<ColumnSetting>, onSave: () -> Unit) {
        val inputValues = remember { mutableStateMapOf<Int, String>() }
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
                val options = column.options

                Text(
                    text = column.name,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                OutlinedTextField(
                    value = inputValues[column.id] ?: "",
                    onValueChange = { inputValues[column.id] = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("${column.name}を入力...") }
                )

                if (options.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .padding(vertical = 8.dp)
                            .horizontalScroll(rememberScrollState())
                    ) {
                        options.forEach { option ->
                            SuggestionChip(
                                onClick = { inputValues[column.id] = option },
                                label = { Text(option) },
                                modifier = Modifier.padding(end = 4.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            Button(
                onClick = {
                    scope.launch {
                        val newRecordId = db.memoDao().insertRecord(MemoRecord())
                        inputValues.forEach { (columnId, text) ->
                            if (text.isNotBlank()) {
                                db.memoDao().insertValue(
                                    MemoValue(
                                        recordId = newRecordId.toInt(),
                                        columnId = columnId,
                                        value = text
                                    )
                                )
                            }
                        }
                        onSave()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 32.dp)
            ) {
                Text("保存して履歴に追加")
            }
        }
    }

    @Composable
    fun HistoryRow(
        db: AppDatabase,
        record: MemoRecord,
        columns: List<ColumnSetting>,
        onDelete: () -> Unit
    ) {
        var values by remember { mutableStateOf(listOf<MemoValue>()) }
        val scope = rememberCoroutineScope()

        LaunchedEffect(record.id) {
            values = db.memoDao().getValuesForRecord(record.id)
        }

        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
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
} // ← これが MainActivity クラス自体の閉じカッコ（一番最後に1つだけ！）