package iso.slomemo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.room.Room
import kotlinx.coroutines.launch
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ExperimentalMaterial3Api // これも必要になる場合があります
import androidx.compose.material3.FilterChip
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.AssistChip // 似た部品を使う場合
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // データベースの準備
        val db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "memo-db"
        ).fallbackToDestructiveMigration()
            .build()

        // --- ここから追加・修正 ---
        setContent {
            val view = androidx.compose.ui.platform.LocalView.current
            if (!view.isInEditMode) {
                androidx.compose.runtime.SideEffect {
                    val window = (view.context as android.app.Activity).window
                    // ステータスバー（上）とナビゲーションバー（下）の背景を黒にする
                    window.statusBarColor = android.graphics.Color.BLACK
                    window.navigationBarColor = android.graphics.Color.BLACK

                    // アイコンや文字を「白」にする設定（背景が黒なので）
                    androidx.core.view.WindowCompat.getInsetsController(
                        window,
                        view
                    ).isAppearanceLightStatusBars = false
                }
            }

            // あなたのプロジェクトのテーマ（例: MyBookmarkAppTheme）で囲む
            // ここでは仮に MaterialTheme で囲みます
            androidx.compose.material3.MaterialTheme {
                androidx.compose.material3.Surface(
                    modifier = androidx.compose.ui.Modifier.fillMaxSize(),
                    color = androidx.compose.material3.MaterialTheme.colorScheme.background
                ) {
                    TestColumnApp(db)
                }
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun TestColumnApp(db: AppDatabase) {
        var columns by remember { mutableStateOf(listOf<ColumnSetting>()) }
        var records by remember { mutableStateOf(listOf<MemoRecord>()) }
        var showSheet by remember { mutableStateOf(false) }
        var newColumnName by remember { mutableStateOf("") }
        val scope = rememberCoroutineScope()
        var selectedColumnId by remember { mutableStateOf<Int?>(null) }
        var newOptionName by remember { mutableStateOf("") }

        fun refreshData() {
            scope.launch {
                columns = db.memoDao().getAllColumns()
                records = db.memoDao().getAllRecords()
            }
        }

        LaunchedEffect(Unit) { refreshData() }

        Scaffold(
            modifier = Modifier.statusBarsPadding(),
            floatingActionButton = {
                FloatingActionButton(onClick = { showSheet = true }) {
                    Icon(Icons.Default.Add, contentDescription = "入力")
                }
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
            ) {

                // --- 1. 履歴表示エリア ---
                Text(
                    "履歴一覧",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.titleLarge
                )

                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(records) { record ->
                        HistoryItem(
                            db = db,
                            record = record,
                            columns = columns,
                            onDelete = { refreshData() } // 削除されたらリストを読み込み直す
                        )
                    }
                }

                // --- 設定エリア (ここから上書き) ---
                Divider()
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState())
                ) {

                    // --- A. 項目（親）の追加 ---
                    Text(
                        "1. 項目を追加 (例: 契機, pt)",
                        style = MaterialTheme.typography.labelSmall
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = newColumnName,
                            onValueChange = { newColumnName = it },
                            modifier = Modifier.weight(1f),
                            label = { Text("項目名") }
                        )
                        Button(
                            onClick = {
                                if (newColumnName.isNotBlank()) {
                                    scope.launch {
                                        db.memoDao().insertColumn(
                                            ColumnSetting(
                                                name = newColumnName,
                                                orderIndex = columns.size
                                            )
                                        )
                                        newColumnName = ""
                                        refreshData()
                                    }
                                }
                            }, modifier = Modifier
                                .padding(start = 8.dp)
                                .height(56.dp)
                        ) {
                            Text("追加")
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // --- B. 選択肢（子）の登録 ---
                    Text(
                        "2. 選択肢を登録 (項目を選んでから入力)",
                        style = MaterialTheme.typography.labelSmall
                    )

                    Row(
                        modifier = Modifier
                            .horizontalScroll(rememberScrollState())
                            .padding(vertical = 8.dp)
                    ) {
                        columns.forEach { col ->
                            FilterChip(
                                selected = selectedColumnId == col.id,
                                onClick = { selectedColumnId = col.id },
                                label = { Text(col.name) }
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                    }

                    if (selectedColumnId != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = newOptionName,
                                onValueChange = { newOptionName = it },
                                modifier = Modifier.weight(1f),
                                label = { Text("例: スイカ, 強チェ") }
                            )
                            Button(
                                onClick = {
                                    if (newOptionName.isNotBlank()) {
                                        scope.launch {
                                            db.memoDao().insertOption(
                                                SelectionOption(
                                                    columnId = selectedColumnId!!,
                                                    optionName = newOptionName
                                                )
                                            )
                                            newOptionName = ""
                                        }
                                    }
                                }, modifier = Modifier
                                    .padding(start = 8.dp)
                                    .height(56.dp)
                            ) {
                                Text("登録")
                            }
                        }
                    }
                }
                // --- 設定エリア (ここまで) ---
            } // ← これは Scaffold 内の Column を閉じるカッコです。消さないよう注意！

            if (showSheet) {
                ModalBottomSheet(onDismissRequest = { showSheet = false }) {
                    InputFormContent(db = db, columns = columns, onSave = {
                        showSheet = false
                        refreshData()
                    })
                }
            }
        }
    }

    @Composable
    fun HistoryItem(
        db: AppDatabase,
        record: MemoRecord,
        columns: List<ColumnSetting>,
        onDelete: () -> Unit // ← 削除した後に画面を更新するための合図を追加
    ) {
        var values by remember { mutableStateOf(listOf<MemoValue>()) }
        val scope = rememberCoroutineScope()

        LaunchedEffect(record.id) {
            values = db.memoDao().getValuesForRecord(record.id)
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 左：時間
                val timeText = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
                    .format(record.timestamp)
                Text(
                    text = timeText,
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.Gray
                )

                Spacer(modifier = Modifier.width(12.dp))

                // 中：データ内容
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(rememberScrollState())
                ) {
                    columns.forEach { col ->
                        val valObj = values.find { it.columnId == col.id }
                        if (valObj != null) {
                            Column(modifier = Modifier.padding(end = 12.dp)) {
                                Text(
                                    col.name,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(valObj.value, style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                    }
                }

                // 右：削除ボタン（ゴミ箱アイコン）
                IconButton(onClick = {
                    scope.launch {
                        db.memoDao().deleteValuesByRecordId(record.id) // 紐付く値を消す
                        db.memoDao().deleteRecord(record)              // 履歴本体を消す
                        onDelete()                                     // 画面をリフレッシュ
                    }
                }) {
                    Icon(Icons.Default.Delete, contentDescription = "削除", tint = Color.LightGray)
                }
            }
        }
    }

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
                var options by remember { mutableStateOf(listOf<SelectionOption>()) }
                LaunchedEffect(column.id) {
                    options = db.memoDao().getOptionsForColumn(column.id)
                }

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
                                onClick = { inputValues[column.id] = option.optionName },
                                label = { Text(option.optionName) },
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
                        // 1. まず「新しい1行」の枠を作る（IDを取得）
                        val newRecordId = db.memoDao().insertRecord(MemoRecord())

                        // 2. 各項目（契機、ptなど）の入力値をループで保存
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
                        // 3. 画面を閉じて、メイン画面を更新（onSave()の中でrefreshDataを呼ぶ）
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
}