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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    fun calculateVisualWidth(text: String): Float {
        if (text.isEmpty()) return 1.0f // 空でも1文字分の幅を確保
        var width = 0f
        for (char in text) {
            if (char.code in 0x00..0x7F || char.code in 0xFF61..0xFF9F) {
                width += 0.52f // 半角を少しだけ広めに（フォントの遊び分）
            } else {
                width += 1.0f
            }
        }
        return width
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "memo-db"
        ).fallbackToDestructiveMigration().build()

        lifecycleScope.launch(Dispatchers.IO) {
            val dao = db.memoDao()
            // 1. 今、項目が空っぽかどうか確認する（Direct版をDaoに追加しておくこと！）
            val currentColumns = dao.getAllColumnsDirect()

            // 2. 空っぽなら、いつもの「pt」「G数」「契機」を勝手に入れる
            if (currentColumns.isEmpty()) {
                dao.insertColumn(
                    ColumnSetting(
                        name = "pt",
                        options = listOf(
                            "000",
                            "100",
                            "200",
                            "300",
                            "400",
                            "500",
                            "600",
                            "700",
                            "800",
                            "900",
                            "━"
                        ),
                        displayOrder = 0
                    )
                )
                dao.insertColumn(
                    ColumnSetting(
                        name = "契機",
                        options = listOf("pt", "強チェ", "ﾁｬﾝｽ目", "ﾏｷﾞﾁｬﾚ", "黒江ﾁｬﾚ", "━"),
                        displayOrder = 1
                    )
                )
                dao.insertColumn(
                    ColumnSetting(
                        name = "種別",
                        options = listOf("BIG", "みたま", "AT", "エピボ", "アリナ", "━"),
                        displayOrder = 2
                    )
                )
                dao.insertColumn(
                    ColumnSetting(
                        name = "AT",
                        options = listOf("〇", "✕", "━"),
                        displayOrder = 3
                    )
                )
                dao.insertColumn(
                    ColumnSetting(
                        name = "BIG終了画面",
                        options = listOf("デフォルト", "さな", "フェリシア", "━"),
                        displayOrder = 4
                    )
                )
                dao.insertColumn(
                    ColumnSetting(
                        name = "AT終了画面",
                        options = listOf("デフォルト", "マギウス", "みかづき荘", "━"),
                        displayOrder = 5
                    )
                )
                dao.insertColumn(
                    ColumnSetting(
                        name = "STORY",
                        options = listOf( "━"),
                        displayOrder = 6
                    )
                )
            }
        }

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
        val appSetting by db.memoDao().getSettingFlow().collectAsState(initial = AppSetting())
        val showTime = appSetting?.showTime ?: true
        var showConditionEditDialog by remember { mutableStateOf(false) }
        var selectedOptionForRule by remember { mutableStateOf<String?>(null) }
        var selectedColumnIdForRule by remember { mutableStateOf<Int?>(null) }
        var showColumnMenuId by remember { mutableStateOf<Int?>(null) }
        var showOptionMenuName by remember { mutableStateOf<String?>(null) }

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
            // 戻るボタンの制御（既存）
            BackHandler(enabled = showColumnMenuId != null || showConditionEditDialog || menuExpanded || showInputArea || currentScreen == "settings") {
                if (showOptionMenuName != null) {
                    showOptionMenuName = null // ★ 選択肢メニューを閉じる
                } else if (showColumnMenuId != null) {
                    showColumnMenuId = null
                } else if (showConditionEditDialog) {
                    showConditionEditDialog = false
                } else if (menuExpanded) {
                    menuExpanded = false
                } else if (showInputArea) {
                    showInputArea = false
                } else if (currentScreen == "settings") {
                    currentScreen = "main"
                }
            }
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
                        // 左側のタイトル
                        Text(
                            text = if (currentScreen == "main") "実戦データ" else "設定",
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.Black
                        )

                        // メイン画面の時だけ右側にメニューを表示
                        if (currentScreen == "main") {
                            Box {
                                IconButton(
                                    onClick = { menuExpanded = true },
                                    modifier = Modifier.offset(x = 12.dp)
                                ) {
                                    Icon(Icons.Default.Menu, null, tint = Color.Black)
                                }

                            }
                        } // if の閉じ
                    } // Row の閉じ
                }, // topBar の閉じ
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
                                    Box(
                                        modifier = Modifier.width(50.dp), // ★正確に60.dp確保
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("時間", style = MaterialTheme.typography.labelMedium)
                                    }
                                }
                                // --- 一覧のヘッダー（項目名） ---
                                columns.forEach { col ->
                                    val weight = calculateVisualWidth(col.name).coerceIn(1.0f, 2.5f)

                                    Box(
                                        modifier = Modifier.weight(weight),
                                        contentAlignment = Alignment.Center // ★ここが中央の基準点
                                    ) {
                                        Text(
                                            text = col.name,
                                            style = MaterialTheme.typography.labelMedium,
                                            maxLines = 1,
                                            // textAlign = ... はあえて書かない（Boxに任せる）
                                        )
                                    }
                                }
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

                                    // --- 設定画面の項目並び替えチップ部分 ---
                                    Box {
                                        FilterChip(
                                            selected = selectedColumnId == col.id,
                                            onClick = { selectedColumnId = col.id },
                                            label = {
                                                Text(
                                                    text = col.name,
                                                    modifier = Modifier.combinedClickable(
                                                        onClick = { selectedColumnId = col.id },
                                                        onLongClick = {
                                                            // ★ ここ！古いメニューを出す代わりに ID を入れる
                                                            showColumnMenuId = col.id
                                                        }
                                                    )
                                                )
                                            },
                                            modifier = Modifier.padding(end = 4.dp)
                                        )
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
                                    "入力設定",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Gray
                                )
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            scope.launch {
                                                db.memoDao()
                                                    .updateColumn(col.copy(showTextField = !col.showTextField))
                                                refreshData()
                                            }
                                        }
                                        .padding(vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Switch(
                                        checked = col.showTextField,
                                        onCheckedChange = { isChecked ->
                                            scope.launch {
                                                db.memoDao()
                                                    .updateColumn(col.copy(showTextField = isChecked))
                                                refreshData()
                                            }
                                        }
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        "入力欄を表示する",
                                        fontSize = 14.sp,
                                        color = Color.Black
                                    )
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

                                        Box {
                                            InputChip(
                                                selected = false,
                                                onClick = {
                                                    // クリック時はメニューを開くフラグを立てる
                                                    showOptionMenuName = opt
                                                },
                                                label = { Text(text = opt) },
                                                modifier = Modifier.padding(4.dp)
                                            )
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

            // --- メニュー専用レイヤー (自作ガードレール) ---
            if (menuExpanded) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Red.copy(alpha = 0.2f)) // 赤い膜
                        .clickable { menuExpanded = false }
                ) {
                    // メニュー本体をここに自作する（DropdownMenuの代わりにSurfaceで置く）
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 56.dp, end = 16.dp) // 位置を調整
                            .width(200.dp),
                        shape = RoundedCornerShape(8.dp),
                        shadowElevation = 8.dp,
                        color = Color.White
                    ) {
                        Column {
                            DropdownMenuItem(
                                text = { Text("+ 実戦データ入力") },
                                onClick = { showInputArea = true; menuExpanded = false }
                            )
                            DropdownMenuItem(
                                text = { Text("⚙ 項目・選択肢の設定") },
                                onClick = { currentScreen = "settings"; menuExpanded = false }
                            )
                        }
                    }
                }
            }

            // --- 手順4：項目移動メニュー (長押し用レイヤー) ---
            if (showColumnMenuId != null) {
                // 今長押しされた項目が、全体の中で何番目かを探す
                val targetIndex = columns.indexOfFirst { it.id == showColumnMenuId }
                val targetCol = columns.find { it.id == showColumnMenuId }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Blue.copy(alpha = 0.2f)) // 開発用：移動メニューは青！
                        .clickable { showColumnMenuId = null }, // 外側タップで閉じる
                    contentAlignment = Alignment.Center // 画面の真ん中にメニューを出す
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        shadowElevation = 8.dp,
                        color = Color.White,
                        modifier = Modifier
                            .width(200.dp)
                            .clickable(enabled = false) { }
                    ) {
                        Column {
                            DropdownMenuItem(
                                text = { Text("左へ移動", fontSize = 18.sp) },
                                enabled = targetIndex > 0,
                                onClick = {
                                    val list = columns.toMutableList()
                                    val item = list.removeAt(targetIndex)
                                    list.add(targetIndex - 1, item)
                                    scope.launch {
                                        list.forEachIndexed { i, c ->
                                            db.memoDao().updateColumn(c.copy(displayOrder = i))
                                        }
                                        refreshData()
                                    }
                                    showColumnMenuId = null
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("右へ移動", fontSize = 18.sp) },
                                enabled = targetIndex < columns.size - 1,
                                onClick = {
                                    val list = columns.toMutableList()
                                    val item = list.removeAt(targetIndex)
                                    list.add(targetIndex + 1, item)
                                    scope.launch {
                                        list.forEachIndexed { i, c ->
                                            db.memoDao().updateColumn(c.copy(displayOrder = i))
                                        }
                                        refreshData()
                                    }
                                    showColumnMenuId = null
                                }
                            )
                        }
                    }
                }
            }

// --- 自動入力ルールの設定ダイアログ (以前の構成に戻した修正版) ---
            if (showConditionEditDialog && selectedColumnIdForRule != null && selectedOptionForRule != null) {
                val localRules = remember { mutableStateListOf<AutoInputRule>() }
                var isNextRow by remember { mutableStateOf(false) }
                var targetColId by remember { mutableStateOf<Int?>(null) }
                var targetValue by remember { mutableStateOf("") }

                LaunchedEffect(selectedColumnIdForRule, selectedOptionForRule) {
                    scope.launch(Dispatchers.IO) {
                        val existingRules = db.memoDao().getRulesByTrigger(
                            selectedColumnIdForRule!!,
                            selectedOptionForRule!!
                        )
                        launch(Dispatchers.Main) {
                            localRules.clear()
                            localRules.addAll(existingRules)
                        }
                    }
                }

                androidx.compose.ui.window.Dialog(onDismissRequest = {
                    showConditionEditDialog = false
                }) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "「${selectedOptionForRule}」選択時の自動入力",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(16.dp))

                            // --- 一覧表示 (以前の構成) ---
                            if (localRules.isNotEmpty()) {
                                Text(
                                    "追加予定の連動",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.Gray
                                )
                                localRules.forEach { rule ->
                                    val targetName =
                                        columns.find { it.id == rule.targetColumnId }?.name
                                            ?: "不明"
                                    val timingStr = if (rule.isNextRow) "次の行" else "同じ行"

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            "・[$timingStr] $targetName → ${rule.targetValue}",
                                            fontSize = 14.sp
                                        )
                                        IconButton(
                                            onClick = { localRules.remove(rule) },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(Icons.Default.Close, null, tint = Color.Red)
                                        }
                                    }
                                }
                                Divider(modifier = Modifier.padding(vertical = 8.dp))
                            }

                            // --- タイミング設定 (ラジオボタン) ---
                            Text(
                                "発動タイミング",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.Gray
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                androidx.compose.material3.RadioButton(
                                    selected = !isNextRow,
                                    onClick = { isNextRow = false })
                                Text(
                                    "同じ行",
                                    modifier = Modifier.clickable { isNextRow = false },
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                androidx.compose.material3.RadioButton(
                                    selected = isNextRow,
                                    onClick = { isNextRow = true })
                                Text(
                                    "次の行",
                                    modifier = Modifier.clickable { isNextRow = true },
                                    fontSize = 14.sp
                                )
                            }

                            // --- 対象項目設定 (チップ一覧) ---
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                "対象の項目",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.Gray
                            )
                            FlowRow(modifier = Modifier.fillMaxWidth()) {
                                columns.forEach { c ->
                                    val isConfigured = localRules.any { it.targetColumnId == c.id }
                                    FilterChip(
                                        selected = targetColId == c.id,
                                        onClick = { targetColId = c.id; targetValue = "" },
                                        label = { Text(if (c.id == selectedColumnIdForRule) "${c.name}(自分)" else c.name) },
                                        colors = androidx.compose.material3.FilterChipDefaults.filterChipColors(
                                            containerColor = if (isConfigured) Color(0xFFE3F2FD) else Color.Transparent,
                                            labelColor = if (isConfigured) Color(0xFF1976D2) else Color.Black
                                        ),
                                        modifier = Modifier.padding(2.dp)
                                    )
                                }
                            }

                            // --- 入力値設定 (チップ一覧) ---
                            if (targetColId != null) {
                                Spacer(modifier = Modifier.height(12.dp))
                                val opts =
                                    columns.find { it.id == targetColId }?.options ?: emptyList()
                                FlowRow(modifier = Modifier.fillMaxWidth()) {
                                    opts.forEach { opt ->
                                        FilterChip(
                                            selected = targetValue == opt,
                                            onClick = { targetValue = opt },
                                            label = { Text(opt) },
                                            modifier = Modifier.padding(2.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(12.dp))

                                Button(
                                    onClick = {
                                        if (targetColId != null && targetValue.isNotEmpty()) {
                                            localRules.removeAll { it.targetColumnId == targetColId && it.isNextRow == isNextRow }
                                            localRules.add(
                                                AutoInputRule(
                                                    triggerColumnId = selectedColumnIdForRule!!,
                                                    triggerValue = selectedOptionForRule!!,
                                                    targetColumnId = targetColId!!,
                                                    targetValue = targetValue,
                                                    isNextRow = isNextRow
                                                )
                                            )
                                            targetColId = null
                                            targetValue = ""
                                        }
                                    },
                                    enabled = targetColId != null && targetValue.isNotEmpty(),
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)
                                ) {
                                    Text("この連動を追加する")
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                androidx.compose.material3.TextButton(onClick = {
                                    showConditionEditDialog = false
                                }) { Text("キャンセル") }
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = {
                                        scope.launch {
                                            db.memoDao().deleteRulesByTrigger(
                                                selectedColumnIdForRule!!,
                                                selectedOptionForRule!!
                                            )
                                            localRules.forEach { db.memoDao().insertRule(it) }
                                            showConditionEditDialog = false
                                            refreshData()
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(
                                            0xFF7E57C2
                                        )
                                    )
                                ) { Text("保存") }
                            }
                        }
                    }
                }
            }

            // --- 手順3：選択肢操作メニュー (選択肢用レイヤー) ---
            if (showOptionMenuName != null && selectedColumnId != null) {
                val col = columns.find { it.id == selectedColumnId }
                val opt = showOptionMenuName!!

                if (col != null) {
                    val optIndex = col.options.indexOf(opt)

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Green.copy(alpha = 0.2f)) // 開発用：選択肢は緑！
                            .clickable { showOptionMenuName = null },
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            shadowElevation = 8.dp,
                            color = Color.White,
                            modifier = Modifier
                                .width(220.dp)
                                .clickable(enabled = false) { }
                        ) {
                            Column {
                                Text(
                                    text = "「$opt」の操作",
                                    modifier = Modifier.padding(16.dp),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = Color.Gray
                                )
                                Divider()
                                DropdownMenuItem(
                                    text = { Text("左へ移動") },
                                    enabled = optIndex > 0,
                                    onClick = {
                                        scope.launch {
                                            val opts = col.options.toMutableList()
                                            val item = opts.removeAt(optIndex)
                                            opts.add(optIndex - 1, item)
                                            db.memoDao().updateColumn(col.copy(options = opts))
                                            refreshData()
                                        }
                                        showOptionMenuName = null
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("右へ移動") },
                                    enabled = optIndex < col.options.size - 1,
                                    onClick = {
                                        scope.launch {
                                            val opts = col.options.toMutableList()
                                            val item = opts.removeAt(optIndex)
                                            opts.add(optIndex + 1, item)
                                            db.memoDao().updateColumn(col.copy(options = opts))
                                            refreshData()
                                        }
                                        showOptionMenuName = null
                                    }
                                )
                                Divider()
                                DropdownMenuItem(
                                    text = { Text("🛠 条件編集", color = Color(0xFF7E57C2)) },
                                    onClick = {
                                        selectedOptionForRule = opt
                                        selectedColumnIdForRule = col.id
                                        showConditionEditDialog = true
                                        showOptionMenuName = null // メニューを閉じてからダイアログへ
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("🗑 削除", color = Color.Red) },
                                    onClick = {
                                        scope.launch {
                                            val opts = col.options.toMutableList()
                                            opts.remove(opt)
                                            db.memoDao().updateColumn(col.copy(options = opts))
                                            db.memoDao().deleteRulesByTrigger(col.id, opt)
                                            refreshData()
                                        }
                                        showOptionMenuName = null
                                    }
                                )
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

                // 項目名の表示
                Text(
                    text = column.name,
                    style = MaterialTheme.typography.labelLarge,
                    color = Color(0xFF7E57C2),
                    modifier = Modifier.padding(top = 8.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Start
                )

                // 【修正：ここから】
                // 1. 選択肢ボタン（あれば表示）
                if (options.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.Start,
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
                                    .padding(end = 8.dp)
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
                }

                // 2. 手入力欄（「選択肢が空」または「設定がON」なら表示）
                if (options.isEmpty() || column.showTextField) {
                    OutlinedTextField(
                        value = currentValue,
                        onValueChange = { inputValues[column.id] = it },
                        placeholder = {
                            if (options.isNotEmpty()) Text("入力欄", fontSize = 12.sp)
                        },
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
                            // 1. 現在の行を保存
                            val rid = if (editingRecordId != null) {
                                db.memoDao().deleteValuesByRecordId(editingRecordId)
                                editingRecordId.toLong()
                            } else {
                                db.memoDao().insertRecord(MemoRecord())
                            }
                            val currentRid = rid.toInt()

                            inputValues.forEach { (cid, txt) ->
                                if (txt.isNotBlank()) {
                                    db.memoDao().insertValue(
                                        MemoValue(
                                            recordId = currentRid,
                                            columnId = cid,
                                            value = txt
                                        )
                                    )
                                }
                            }

                            // 2. 連動チェック（自分自身も含めて実行）
                            inputValues.forEach { (cid, txt) ->
                                val rules = db.memoDao().getRulesByTrigger(cid, txt)
                                rules.forEach { rule ->
                                    if (rule.isNextRow) {
                                        // --- 「次の行」への連動処理 ---
                                        val allRecords = db.memoDao().getAllRecords()
                                        val currentIndex =
                                            allRecords.indexOfFirst { it.id == currentRid }

                                        val nextRecord =
                                            if (currentIndex != -1 && currentIndex + 1 < allRecords.size) {
                                                allRecords[currentIndex + 1]
                                            } else null

                                        if (nextRecord != null) {
                                            // 次の行が存在すれば、その行の値を更新（自分と同じ項目でもOK）
                                            db.memoDao().insertValue(
                                                MemoValue(
                                                    recordId = nextRecord.id,
                                                    columnId = rule.targetColumnId,
                                                    value = rule.targetValue
                                                )
                                            )
                                        } else {
                                            // 次の行がなければ、新しく作成
                                            val newNextRid = db.memoDao().insertRecord(MemoRecord())
                                            db.memoDao().insertValue(
                                                MemoValue(
                                                    recordId = newNextRid.toInt(),
                                                    columnId = rule.targetColumnId,
                                                    value = rule.targetValue
                                                )
                                            )
                                        }
                                    } else {
                                        // --- 「同じ行」の連動処理 ---
                                        // 無限ループ防止のため、自分自身以外の場合のみ即時上書き
                                        if (cid != rule.targetColumnId) {
                                            db.memoDao().insertValue(
                                                MemoValue(
                                                    recordId = currentRid,
                                                    columnId = rule.targetColumnId,
                                                    value = rule.targetValue
                                                )
                                            )
                                        }
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
                    Text(if (editingRecordId != null) "変更を保存" else "メモに追加")
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
                    val timeText =
                        java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
                            .format(record.timestamp)
                    Text(
                        text = timeText,
                        modifier = Modifier.width(50.dp), // 時間は固定が一番ズレない
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }

                // --- HistoryRow 内の各列 ---
                columns.forEach { col ->
                    val value = values.find { it.columnId == col.id }?.value ?: ""

                    // ★重要：計算式をヘッダーと「完全に」一致させる
                    // 中身(value)ではなく、項目名(col.name)の幅だけで一旦固定してみてください
                    val weight = calculateVisualWidth(col.name).coerceIn(1.0f, 2.5f)

                    Box(
                        modifier = Modifier.weight(weight),
                        contentAlignment = Alignment.Center // ★ヘッダーと全く同じ基準点
                    ) {
                        Text(
                            text = value,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1
                        )
                    }
                }
            } // ← Row (HistoryRow内のデータ行) の閉じカッコ
            Divider(color = Color.LightGray.copy(alpha = 0.3f))
        }
    }
}