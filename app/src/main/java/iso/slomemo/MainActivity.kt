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
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
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

        val scope = rememberCoroutineScope()
        val inputValues = remember { mutableStateMapOf<Int, String>() }
        var editingRecordId by remember { mutableStateOf<Int?>(null) }
        var valuesMap by remember { mutableStateOf<Map<Int, List<MemoValue>>>(emptyMap()) }

        // --- 設定の読み込み ---
        val appSetting by db.memoDao().getSettingFlow().collectAsState(initial = AppSetting())
        val showTime = appSetting?.showTime ?: true

        fun refreshData() {
            scope.launch {
                columns = db.memoDao().getAllColumns()
                records = db.memoDao().getAllRecords()
                valuesMap = db.memoDao().getAllValues().groupBy { it.recordId }
            }
        }

        LaunchedEffect(Unit) { refreshData() }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            BackHandler(enabled = menuExpanded) { menuExpanded = false }
            BackHandler(enabled = showInputArea) { showInputArea = false }
            BackHandler(enabled = currentScreen == "settings") { currentScreen = "main" }

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
                                modifier = Modifier.offset(x = 12.dp)
                            ) {
                                Icon(Icons.Default.Menu, null, tint = Color.Black)
                            }
                            DropdownMenu(
                                expanded = menuExpanded,
                                onDismissRequest = { menuExpanded = false },
                                offset = DpOffset(x = (-12).dp, y = 0.dp),
                                modifier = Modifier.background(Color.White)
                            ) {
                                DropdownMenuItem(text = {
                                    Text(
                                        "+  実戦データ入力",
                                        fontSize = 18.sp,
                                        color = Color.Black
                                    )
                                }, onClick = { showInputArea = true; menuExpanded = false })
                                DropdownMenuItem(text = {
                                    Text(
                                        "⚙  項目・選択肢の設定",
                                        fontSize = 18.sp,
                                        color = Color.Black
                                    )
                                }, onClick = { currentScreen = "settings"; menuExpanded = false })
                            }
                        }
                    }
                },
                floatingActionButton = {
                    if (currentScreen == "main" && !showInputArea) {
                        FloatingActionButton(
                            onClick = {
                                inputValues.clear(); editingRecordId = null; showInputArea = true
                            },
                            containerColor = Color(0xFF7E57C2),
                            contentColor = Color.White
                        ) { Icon(Icons.Default.Add, "入力") }
                    }
                }
            ) { padding ->
                Box(
                    modifier = Modifier
                        .padding(padding)
                        .fillMaxSize()
                ) {
                    if (currentScreen == "main") {
                        Column(modifier = Modifier.fillMaxSize()) {
                            // --- 修正：一覧のヘッダー（項目名） ---
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFFEADDFF).copy(alpha = 0.5f))
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (showTime) {
                                    Text(
                                        text = "時間",
                                        modifier = Modifier.width(60.dp), // ★幅を少し広げて固定
                                        style = MaterialTheme.typography.labelMedium,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center // ★中央揃え
                                    )
                                }
                                columns.forEach { col ->
                                    Text(
                                        text = col.name,
                                        modifier = Modifier.weight(1f), // ★均等に広げる
                                        style = MaterialTheme.typography.labelMedium,
                                        maxLines = 1,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center // ★中央揃え
                                    )
                                }
                                // 右端の調整用（削除ボタン等のスペースがあれば）
                                Spacer(modifier = Modifier.width(8.dp))
                            }

                            // --- 履歴データ一覧 ---
                            LazyColumn(modifier = Modifier.fillMaxSize()) {
                                items(records, key = { it.id }) { record ->
                                    HistoryRow(
                                        db = db,
                                        record = record,
                                        columns = columns,
                                        values = valuesMap[record.id] ?: emptyList(),
                                        showTime = showTime,
                                        onRowClick = {
                                            scope.launch {
                                                val currentValues =
                                                    db.memoDao().getValuesForRecord(record.id)
                                                inputValues.clear()
                                                currentValues.forEach {
                                                    inputValues[it.columnId] = it.value
                                                }
                                                editingRecordId = record.id
                                                showInputArea = true
                                            }
                                        },
                                        onDelete = { refreshData() }
                                    )
                                }
                            }
                        }
                    } else {
                        // --- 設定画面 ---
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            Text("全体設定", style = MaterialTheme.typography.titleMedium)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        scope.launch {
                                            db.memoDao()
                                                .updateSetting(AppSetting(showTime = !showTime))
                                        }
                                    }
                                    .padding(vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Switch(checked = showTime, onCheckedChange = { isChecked ->
                                    scope.launch {
                                        db.memoDao().updateSetting(AppSetting(showTime = isChecked))
                                    }
                                })
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("一覧に時間を表示する", color = Color.Black)
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Divider()
                            Spacer(modifier = Modifier.height(16.dp))
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
                            Spacer(modifier = Modifier.height(24.dp))
                            Divider()
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "項目の編集・選択肢の編集",
                                style = MaterialTheme.typography.titleMedium
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
                                        label = { Text(col.name) },
                                        modifier = Modifier.padding(end = 4.dp)
                                    )
                                }
                            }
                            selectedColumnId?.let { colId ->
                                val col = columns.find { it.id == colId } ?: return@let
                                var newOptionName by remember(col.id) { mutableStateOf("") }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "項目名の編集",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Gray
                                )
                                var editingName by remember(col.id) { mutableStateOf(col.name) }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    TextField(
                                        value = editingName,
                                        onValueChange = { editingName = it },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Button(onClick = {
                                        scope.launch {
                                            db.memoDao()
                                                .updateColumn(col.copy(name = editingName)); refreshData()
                                        }
                                    }) { Text("保存") }
                                }
                                Spacer(modifier = Modifier.height(16.dp))
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
                                            )
                                                db.memoDao()
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
                                                        col.options.toMutableList(); opts.remove(
                                                    opt
                                                )
                                                    db.memoDao()
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
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.4f))
                        .clickable { showInputArea = false })
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.5f)
                        .align(Alignment.BottomCenter)
                        .background(Color.White)
                        .navigationBarsPadding()
                        .clickable(enabled = false) { }
                ) {
                    Column {
                        Divider(thickness = 2.dp, color = Color(0xFF7E57C2))
                        InputFormContent(
                            db = db,
                            columns = columns,
                            inputValues = inputValues,
                            editingRecordId = editingRecordId,
                            onSave = { showInputArea = false; refreshData() })
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
        editingRecordId: Int?,
        onSave: () -> Unit
    ) {
        val scope = rememberCoroutineScope()
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = if (editingRecordId != null) "履歴を編集" else "新規メモ入力",
                style = MaterialTheme.typography.headlineSmall
            )
            Spacer(modifier = Modifier.height(16.dp))

            columns.forEach { column ->
                val options = column.options
                val currentValue = inputValues[column.id] ?: ""

                // ★項目名の中央揃え
                Text(
                    text = column.name,
                    style = MaterialTheme.typography.labelLarge,
                    color = Color(0xFF7E57C2),
                    modifier = Modifier
                        .fillMaxWidth() // 横幅いっぱいに広げて中央に寄せる
                        .padding(top = 8.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                if (options.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth() // ★横幅いっぱいに広げる
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.Center, // ★ボタン群を「中央寄せ」にする
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        options.forEach { option ->
                            val isSelected = (currentValue == option)
                            val bgColor = if (isSelected) Color(0xFF7E57C2) else Color(0xFFF0F0F0)
                            val textColor = if (isSelected) Color.White else Color.Black

                            Surface(
                                onClick = {
                                    inputValues[column.id] = if (isSelected) "" else option
                                },
                                shape = RoundedCornerShape(8.dp),
                                color = bgColor,
                                modifier = Modifier
                                    .height(40.dp)
                                    .padding(horizontal = 4.dp) // ★ボタン同士の隙間
                                // .weight(1f, fill = false) // ←これがあると端まで広がろうとするので、一旦消すか調整
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                ) {
                                    Text(text = option, color = textColor, fontSize = 14.sp)
                                }
                            }
                        }
                    }
                } else {
                    OutlinedTextField(
                        value = currentValue,
                        onValueChange = { inputValues[column.id] = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // 保存・削除ボタンエリア
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 32.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = {
                        scope.launch {
                            val rid = if (editingRecordId != null) {
                                db.memoDao().deleteValuesByRecordId(editingRecordId)
                                editingRecordId.toLong()
                            } else {
                                db.memoDao().insertRecord(MemoRecord())
                            }
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
                            onSave()
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7E57C2))
                ) {
                    Text(if (editingRecordId != null) "変更を保存" else "保存して履歴に追加")
                }

                if (editingRecordId != null) {
                    Button(
                        onClick = {
                            scope.launch {
                                db.memoDao().deleteValuesByRecordId(editingRecordId)
                                db.memoDao().deleteRecordById(editingRecordId)
                                onSave()
                            }
                        },
                        modifier = Modifier.size(56.dp),
                        shape = RoundedCornerShape(28.dp),
                        contentPadding = PaddingValues(0.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB3261E))
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "削除", tint = Color.White)
                    }
                }
            }
        }
    }

    @Composable
    fun HistoryRow(
        db: AppDatabase,
        record: MemoRecord,
        columns: List<ColumnSetting>,
        values: List<MemoValue>,
        showTime: Boolean,
        onRowClick: () -> Unit,
        onDelete: () -> Unit
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onRowClick() }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (showTime) {
                    // ★timeText が赤くならないように、ここでしっかり定義
                    val timeText =
                        java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
                            .format(record.timestamp)
                    Text(
                        text = timeText,
                        modifier = Modifier.width(50.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center // ★中央揃え
                    )
                }

                columns.forEach { col ->
                    val value = values.find { it.columnId == col.id }?.value ?: ""
                    Text(
                        text = value,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 2.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center // ★中央揃え
                    )
                }
            }
            Divider(color = Color.LightGray.copy(alpha = 0.3f))
        }
    }
} // ★これが MainActivity を閉じる正真正銘最後のカッコ！