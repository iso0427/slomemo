package iso.slomemo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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

        // --- 自動入力ルールの設定用ステート ---
        var showConditionEditDialog by remember { mutableStateOf(false) }
        var selectedOptionForRule by remember { mutableStateOf<String?>(null) }
        var selectedColumnIdForRule by remember { mutableStateOf<Int?>(null) }

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
                                    // ★データ行と同じ計算式で比率を合わせる
                                    val weight = if (col.name.length > 3) 1.5f else 0.7f

                                    Text(
                                        text = col.name,
                                        modifier = Modifier.weight(weight),
                                        style = MaterialTheme.typography.labelMedium,
                                        maxLines = 1,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                }
//                                Spacer(modifier = Modifier.width(0.dp))
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
                                                db.memoDao().insertColumn(
                                                    ColumnSetting(
                                                        name = newColumnName,
                                                        displayOrder = columns.size
                                                    )
                                                );
                                                newColumnName = "";
                                                refreshData()
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
                                // indexを使って位置を特定するために forEachIndexed に変更
                                columns.forEachIndexed { index, col ->
                                    var showColumnMenu by remember { mutableStateOf(false) }

                                    Box {
                                        FilterChip(
                                            selected = selectedColumnId == col.id,
                                            onClick = { selectedColumnId = col.id },
                                            label = {
                                                // 文字の部分を長押し（onLongClick）できるように設定
                                                Text(
                                                    text = col.name,
                                                    modifier = Modifier.combinedClickable(
                                                        onClick = { selectedColumnId = col.id },
                                                        onLongClick = { showColumnMenu = true }
                                                    )
                                                )
                                            },
                                            modifier = Modifier.padding(end = 4.dp)
                                        )

                                        // 長押しした時にふわっと出るメニュー
                                        DropdownMenu(
                                            expanded = showColumnMenu,
                                            onDismissRequest = { showColumnMenu = false }
                                        ) {
                                            DropdownMenuItem(
                                                text = { Text("左へ移動",fontSize = 18.sp) },
                                                enabled = index > 0, // 一番左じゃなければ押せる
                                                onClick = {
                                                    val list = columns.toMutableList()
                                                    val item = list.removeAt(index)
                                                    list.add(index - 1, item)

                                                    // DB保存：リストの全項目に 0, 1, 2... と順番を割り振る
                                                    scope.launch {
                                                        list.forEachIndexed { i, col ->
                                                            db.memoDao().updateColumn(col.copy(displayOrder = i))
                                                        }
                                                        refreshData() // DBから最新（並び順通り）のデータを読み直す
                                                    }
                                                    showColumnMenu = false
                                                }
                                            )
                                            DropdownMenuItem(
                                                text = { Text("右へ移動",fontSize = 18.sp) },
                                                enabled = index < columns.size - 1, // 一番右じゃなければ押せる
                                                onClick = {
                                                    val list = columns.toMutableList()
                                                    val item = list.removeAt(index)
                                                    list.add(index + 1, item)

                                                    // DB保存：リストの全項目に新しい順番を割り振る
                                                    scope.launch {
                                                        list.forEachIndexed { i, col ->
                                                            db.memoDao().updateColumn(col.copy(displayOrder = i))
                                                        }
                                                        refreshData()
                                                    }
                                                    showColumnMenu = false
                                                }
                                            )
                                        }
                                    }
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
                                // --- ここから上書き開始 ---
                                FlowRow(modifier = Modifier.fillMaxWidth()) {
                                    // optIndexを使って位置を特定するために forEachIndexed に変更
                                    col.options.forEachIndexed { optIndex, opt ->
                                        var showOptMenu by remember { mutableStateOf(false) }

                                        // --- InputChip のループ内を以下に書き換え ---
                                        Box {
                                            InputChip(
                                                selected = false,
                                                onClick = { showOptMenu = true }, // ★ タップでメニューを出す
                                                label = {
                                                    Text(text = opt) // ★ modifier.combinedClickable は削除（onClickに統一）
                                                },
                                                // trailingIcon = { ... }  // ★ ここ（Iconの部分）をまるごと削除！
                                                modifier = Modifier.padding(4.dp)
                                            )

                                            // 長押し（今はタップ）で出るメニュー
                                            DropdownMenu(
                                                expanded = showOptMenu,
                                                onDismissRequest = { showOptMenu = false }
                                            ) {
                                                DropdownMenuItem(
                                                    text = { Text("左へ移動", fontSize = 18.sp) },
                                                    enabled = optIndex > 0,
                                                    onClick = {
                                                        scope.launch {
                                                            val opts = col.options.toMutableList()
                                                            val item = opts.removeAt(optIndex)
                                                            opts.add(optIndex - 1, item)
                                                            db.memoDao().updateColumn(col.copy(options = opts))
                                                            refreshData()
                                                        }
                                                        showOptMenu = false
                                                    }
                                                )
                                                DropdownMenuItem(
                                                    text = { Text("右へ移動", fontSize = 18.sp) },
                                                    enabled = optIndex < col.options.size - 1,
                                                    onClick = {
                                                        scope.launch {
                                                            val opts = col.options.toMutableList()
                                                            val item = opts.removeAt(optIndex)
                                                            opts.add(optIndex + 1, item)
                                                            db.memoDao().updateColumn(col.copy(options = opts))
                                                            refreshData()
                                                        }
                                                        showOptMenu = false
                                                    }
                                                )

                                                androidx.compose.material3.Divider()


                                                DropdownMenuItem(
                                                    text = { Text("🛠 条件編集", fontSize = 18.sp, color = Color(0xFF7E57C2)) },
                                                    onClick = {
                                                        selectedOptionForRule = opt
                                                        selectedColumnIdForRule = col.id
                                                        showConditionEditDialog = true
                                                        showOptMenu = false
                                                    }
                                                )

                                                // ★ 新しく「削除」を追加
                                                DropdownMenuItem(
                                                    text = { Text("🗑 削除", fontSize = 18.sp, color = Color.Red) },
                                                    onClick = {
                                                        scope.launch {
                                                            val opts = col.options.toMutableList()
                                                            opts.remove(opt) // この選択肢を消す
                                                            db.memoDao().updateColumn(col.copy(options = opts))

                                                            // 【重要】選択肢を消すなら、その選択肢に紐づく「自動入力ルール」も一緒に消す
                                                            db.memoDao().deleteRulesByTrigger(col.id, opt)

                                                            refreshData()
                                                        }
                                                        showOptMenu = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                                // --- ここまで上書き終了 ---
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
// --- 自動入力ルールの設定ダイアログ ---
            if (showConditionEditDialog && selectedColumnIdForRule != null && selectedOptionForRule != null) {
                var isNextRow by remember { mutableStateOf(false) }
                var targetColId by remember { mutableStateOf<Int?>(null) }
                var targetValue by remember { mutableStateOf("") }

                androidx.compose.ui.window.Dialog(onDismissRequest = { showConditionEditDialog = false }) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White,
                        modifier = Modifier.fillMaxWidth().padding(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("「${selectedOptionForRule}」選択時の自動入力", style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.height(16.dp))

                            Text("発動タイミング", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                androidx.compose.material3.RadioButton(selected = !isNextRow, onClick = { isNextRow = false })
                                Text("同じ行", modifier = Modifier.clickable { isNextRow = false })
                                Spacer(modifier = Modifier.width(16.dp))
                                androidx.compose.material3.RadioButton(selected = isNextRow, onClick = { isNextRow = true })
                                Text("次の行", modifier = Modifier.clickable { isNextRow = true })
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                            Text("対象の項目", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                            FlowRow(modifier = Modifier.fillMaxWidth()) {
                                columns.filter { it.id != selectedColumnIdForRule }.forEach { c ->
                                    FilterChip(
                                        selected = targetColId == c.id,
                                        onClick = { targetColId = c.id },
                                        label = { Text(c.name) },
                                        modifier = Modifier.padding(2.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                            Text("入力する値", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                            OutlinedTextField(
                                value = targetValue,
                                onValueChange = { targetValue = it },
                                placeholder = { Text("例: ━") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )

                            Spacer(modifier = Modifier.height(24.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                androidx.compose.material3.TextButton(onClick = { showConditionEditDialog = false }) { Text("キャンセル") }
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = {
                                        if (targetColId != null) {
                                            scope.launch {
                                                db.memoDao().insertRule(AutoInputRule(
                                                    triggerColumnId = selectedColumnIdForRule!!,
                                                    triggerValue = selectedOptionForRule!!,
                                                    targetColumnId = targetColId!!,
                                                    targetValue = targetValue,
                                                    isNextRow = isNextRow
                                                ))
                                                showConditionEditDialog = false
                                            }
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7E57C2))
                                ) { Text("保存") }
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

                // ★ ここを「左寄せ」に戻す
                Text(
                    text = column.name,
                    style = MaterialTheme.typography.labelLarge,
                    color = Color(0xFF7E57C2),
                    modifier = Modifier
                        .padding(top = 8.dp), // fillMaxWidth() を削除して幅を文字分だけにする
                    textAlign = androidx.compose.ui.text.style.TextAlign.Start // Start（左）に変更
                )

                if (options.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth() // ★横幅いっぱいに広げる
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.Start, // ★ Center から Start に戻す
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
                                    .padding(end = 8.dp) // ★ボタン同士の隙間
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

                            // 2. ★【新規】自動入力（次の行）の発動チェック ★
                            // 新規保存（editingRecordId == null）の時だけ発動
                            if (editingRecordId == null) {
                                val nextRowValues = mutableMapOf<Int, String>()

                                // 入力された各項目について、合致するルールがあるかDBに聞きに行く
                                inputValues.forEach { (cid, txt) ->
                                    val rules = db.memoDao().getRulesByTrigger(cid, txt)
                                    rules.forEach { rule ->
                                        // 「次の行」フラグがONのルールがあればデータを溜める
                                        if (rule.isNextRow) {
                                            nextRowValues[rule.targetColumnId] = rule.targetValue
                                        }
                                    }
                                }

                                // ルールに合致したデータが1つでもあれば、新しい行（2行目）として保存
                                if (nextRowValues.isNotEmpty()) {
                                    val nextRid = db.memoDao().insertRecord(MemoRecord())
                                    nextRowValues.forEach { (targetCid, targetVal) ->
                                        db.memoDao().insertValue(
                                            MemoValue(recordId = nextRid.toInt(), columnId = targetCid, value = targetVal)
                                        )
                                    }
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
                    val timeText = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
                        .format(record.timestamp)
                    Text(
                        text = timeText,
                        modifier = Modifier.width(55.dp), // 時間は固定が一番ズレない
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }

                columns.forEach { col ->
                    val value = values.find { it.columnId == col.id }?.value ?: ""

                    // ★ここがポイント：項目名や中身が長い場合は比率を大きくする
                    val weight = if (col.name.length > 3 || value.length > 3) 1.5f else 0.7f

                    Text(
                        text = value,
                        modifier = Modifier
                            .weight(weight) // 比率で幅を決める
                            .padding(horizontal = 2.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
//                Spacer(modifier = Modifier.width(0.dp))
            }
            Divider(color = Color.LightGray.copy(alpha = 0.3f))
        }
    }
} // ★これが MainActivity を閉じる正真正銘最後のカッコ！