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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
                        options = listOf("━"),
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
        // 1. ダークモードの状態（とりあえずは変数で管理。後でDB保存も可能）
        val isDarkMode = true

// 2. モードによって切り替わる色の定義（色の司令塔）
        val backColor = if (isDarkMode) Color(0xFF121212) else Color.White      // 画面全体の背景
        val surfaceColor = if (isDarkMode) Color(0xFF1E1E1E) else Color.White   // メニューやダイアログの箱
        val mainText = if (isDarkMode) Color.White else Color.Black           // メインの文字
        val subText = if (isDarkMode) Color.LightGray else Color.Gray          // 補足の文字
        val dividerColor = if (isDarkMode) Color(0xFF333333) else Color(0xFFEEEEEE) // 区切り線
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
                .background(backColor) // ★ Color.Black から変更
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
                containerColor = backColor, // ★ Color.White から変更
                topBar = {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(backColor) // ★ Color.White から変更
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 左側のタイトル
                        Text(
                            text = if (currentScreen == "main") "実戦データ" else "設定",
                            style = MaterialTheme.typography.titleLarge,
                            color = mainText
                        )

                        // メイン画面の時だけ右側にメニューを表示
                        if (currentScreen == "main") {
                            Box {
                                IconButton(
                                    onClick = { menuExpanded = true },
                                    modifier = Modifier.offset(x = 12.dp)
                                ) {
                                    Icon(Icons.Default.Menu, null, tint = mainText)
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
                                    .background(
                                        if (isDarkMode) Color(0xFF4A4458) else Color(
                                            0xFFEADDFF
                                        ).copy(alpha = 0.5f)
                                    )
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (showTime) {
                                    Box(
                                        modifier = Modifier.width(50.dp), // ★正確に60.dp確保
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            "時間",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = mainText
                                        )
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
                                            color = mainText, // ★ ここを mainText に変更（または追加）
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
                                        onDelete = { refreshData() },
                                        mainText = mainText,
                                        subText = subText,
                                        dividerColor = dividerColor
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
                            Text(
                                "全体設定",
                                style = MaterialTheme.typography.titleMedium,
                                color = mainText
                            )
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
                                Text("時間を表示する", color = mainText)
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                            Divider(color = dividerColor) // ★区切り線も変数に
                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                "項目の追加",
                                style = MaterialTheme.typography.titleMedium,
                                color = mainText
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                OutlinedTextField(
                                    value = newColumnName,
                                    onValueChange = { newColumnName = it },
                                    placeholder = {
                                        Text(
                                            "新しい項目",
                                            fontSize = 14.sp,
                                            color = subText
                                        )
                                    },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    shape = RoundedCornerShape(8.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = mainText,
                                        unfocusedTextColor = mainText,
                                        focusedContainerColor = Color(0xFF252525),
                                        unfocusedContainerColor = Color(0xFF252525),
                                        cursorColor = mainText,
                                        focusedBorderColor = Color.Gray,
                                        unfocusedBorderColor = Color.Transparent
                                    )
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
                                                )
                                                newColumnName = ""
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
                                style = MaterialTheme.typography.titleMedium,
                                color = mainText
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
                                                    // ★ color = mainText は削除（下の colors で一括管理するため）
                                                    modifier = Modifier.combinedClickable(
                                                        onClick = { selectedColumnId = col.id },
                                                        onLongClick = {
                                                            showColumnMenuId = col.id
                                                        }
                                                    )
                                                )
                                            },
                                            // ★ ここから追加：Bの設計思想に基づいた色指定
                                            colors = FilterChipDefaults.filterChipColors(
                                                labelColor = mainText,              // 未選択時の文字色（パキッとした白）
                                                selectedContainerColor = Color(0xFFEADDFF), // 選択時の背景色（紫）
                                                selectedLabelColor = Color.Black    // 選択時の文字色（白）
                                            ),
                                            // 未選択時に枠線が欲しい場合は以下を追加（不要なら削除してOK）
                                            border = FilterChipDefaults.filterChipBorder(
                                                enabled = true,
                                                selected = selectedColumnId == col.id, // ここは本体の状態と合わせるのがComposeの鉄則です
                                                borderColor = Color.Gray,
                                                borderWidth = 1.dp,
                                                selectedBorderColor = Color.Gray,      // 選択中もグレーの枠線を出す
                                                selectedBorderWidth = 1.dp             // 選択中も1.dpを維持
                                            ),
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
                                    style = MaterialTheme.typography.titleMedium,
                                    color = mainText
                                )
                                var editingName by remember(col.id) { mutableStateOf(col.name) }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    // 他の入力欄（Bのデザイン）に統一
                                    OutlinedTextField(
                                        value = editingName,
                                        onValueChange = { editingName = it },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true,
                                        shape = RoundedCornerShape(8.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = mainText,
                                            unfocusedTextColor = mainText,
                                            focusedContainerColor = Color(0xFF252525),
                                            unfocusedContainerColor = Color(0xFF252525),
                                            cursorColor = mainText,
                                            focusedBorderColor = Color.Gray,
                                            unfocusedBorderColor = Color.Transparent
                                        )
                                    )

                                    // ボタンの形状を「項目の追加」と同じスタイルに統一
                                    Button(
                                        onClick = {
                                            scope.launch {
                                                db.memoDao()
                                                    .updateColumn(col.copy(name = editingName))
                                                refreshData()
                                            }
                                        },
                                        modifier = Modifier.padding(start = 8.dp), // ボタン形状を統一
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFF7E57C2)
                                        )
                                    ) {
                                        Text("保存", color = Color.White)
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                Text(
                                    "入力設定",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = mainText
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
                                        style = MaterialTheme.typography.titleMedium,
                                        color = mainText
                                    )
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                Text(
                                    text = "「${col.name}」の選択肢一覧",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = mainText
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    OutlinedTextField(
                                        value = newOptionName,
                                        onValueChange = { newOptionName = it },
                                        placeholder = {
                                            Text(
                                                "新しい選択肢",
                                                fontSize = 14.sp,
                                                color = subText
                                            )
                                        },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true,
                                        shape = RoundedCornerShape(8.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = mainText,
                                            unfocusedTextColor = mainText,
                                            focusedContainerColor = Color(0xFF252525),
                                            unfocusedContainerColor = Color(0xFF252525),
                                            cursorColor = mainText,
                                            focusedBorderColor = Color.Gray,
                                            unfocusedBorderColor = Color.Transparent
                                        )
                                    )
                                    Button(
                                        onClick = {
                                            if (newOptionName.isNotBlank()) {
                                                scope.launch {
                                                    val opts = col.options.toMutableList()
                                                    opts.add(newOptionName)
                                                    db.memoDao()
                                                        .updateColumn(col.copy(options = opts))
                                                    newOptionName = ""
                                                    refreshData()
                                                }
                                            }
                                        },
                                        modifier = Modifier.padding(start = 8.dp), // ボタン形状を統一
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFF7E57C2)
                                        )
                                    ) { Text("追加") } // 文言も「追加」で統一
                                }
                                FlowRow(modifier = Modifier.fillMaxWidth()) {
                                    // optIndexを使って位置を特定するために forEachIndexed に変更
                                    col.options.forEachIndexed { optIndex, opt ->
                                        var showOptMenu by remember { mutableStateOf(false) }

                                        Box {
                                            InputChip(
                                                selected = (showOptionMenuName == opt),
                                                onClick = {
                                                    showOptionMenuName = opt
                                                },
                                                label = { Text(text = opt) },
                                                colors = InputChipDefaults.inputChipColors(
                                                    labelColor = mainText,                 // 未選択：白
                                                    selectedContainerColor = Color(0xFFEADDFF), // 選択（メニュー中）：薄紫
                                                    selectedLabelColor = Color.Black
                                                ),
                                                // ★ 画像の定義通りに全ての必須パラメータを埋める
                                                border = InputChipDefaults.inputChipBorder(
                                                    enabled = true,
                                                    selected = (showOptionMenuName == opt), // 本体と合わせる
                                                    borderColor = Color.Gray,
                                                    borderWidth = 1.dp,
                                                    selectedBorderColor = Color.Gray,      // 選択中（薄紫）でも枠線を出す
                                                    selectedBorderWidth = 1.dp
                                                ),
                                                modifier = Modifier.padding(4.dp)
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(24.dp))
                                Button(
                                    onClick = {
                                        scope.launch {
                                            db.memoDao().deleteColumn(col)
                                            selectedColumnId = null
                                            refreshData()
                                        }
                                    },
                                    // 色と横幅いっぱいの設定は元のコードを維持
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFFB3261E)
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    // ★ テキストの内容とサイズのみを変更
                                    Text(
                                        text = "「${col.name}」を削除",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // --- メニュー専用レイヤー (自作ガードレール) ---
            if (menuExpanded)
            {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable { menuExpanded = false }
                ) {
                    // メニュー本体
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 80.dp, end = 4.dp) // ★ 1. 【位置】ここをいじると場所が変わる
                            .width(220.dp),                  // ★ 2. 【幅】ここをいじると横幅が変わる
                        shape = RoundedCornerShape(5.dp),    // 角の丸み
                        shadowElevation = 8.dp,
                        color = surfaceColor, // ★ Color.White から変更
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {

                            // --- 項目2：設定 ---
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        currentScreen = "settings"
                                        menuExpanded = false
                                    }
                                    .padding(horizontal = 16.dp, vertical = 16.dp), // ★ 高さを少しだけ広げて押しやすく
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = androidx.compose.material.icons.Icons.Default.Settings,
                                    contentDescription = null,
                                    modifier = Modifier.size(22.dp),
                                    tint = mainText
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "項目・選択肢の設定",
                                    fontSize = 18.sp,
                                    color = mainText
                                )
                            }
                        }
                    }
                }
            }
            // --- 手順4：項目移動メニュー (長押し用レイヤー) ---
            if (showColumnMenuId != null) {
                val targetIndex = columns.indexOfFirst { it.id == showColumnMenuId }
                val targetCol = columns.find { it.id == showColumnMenuId }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.4f))
                        .clickable { showColumnMenuId = null },
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        shadowElevation = 8.dp,
                        color = Color(0xFF252525),
                        modifier = Modifier
                            .width(180.dp)
                            .clickable(enabled = false) { }
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "「${targetCol?.name}」",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = mainText,
                                modifier = Modifier.padding(bottom = 20.dp)
                            )

                            // 決定した配色ルール
                            val canMoveColumnColors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFBBBBBB), // 移動可：明るいグレー
                                contentColor = Color.Black          // 文字：黒
                            )
                            val cannotMoveColumnColors = ButtonDefaults.buttonColors(
                                disabledContainerColor = Color(0xFF333333), // 移動不可：暗いグレー
                                disabledContentColor = Color.Black          // 無効時も文字は黒
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // 左へ移動ボタン
                                Button(
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
                                    },
                                    enabled = targetIndex > 0,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(60.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(0.dp),
                                    colors = if (targetIndex > 0) canMoveColumnColors else cannotMoveColumnColors
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(Icons.Default.ArrowBack, null, tint = Color.Black)
                                        Text("左へ", fontSize = 16.sp, color = Color.Black)
                                    }
                                }

                                // 右へ移動ボタン
                                Button(
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
                                    },
                                    enabled = targetIndex < columns.size - 1,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(60.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(0.dp),
                                    colors = if (targetIndex < columns.size - 1) canMoveColumnColors else cannotMoveColumnColors
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(Icons.Default.ArrowForward, null, tint = Color.Black)
                                        Text("右へ", fontSize = 16.sp, color = Color.Black)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            //--- 自動入力ルールの設定ダイアログ (デザイン統一版) ---
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
                        // ★ 背景色をダークに
                        color = Color(0xFF252525),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "「${selectedOptionForRule}」選択時の連動入力",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                color = mainText // ★ タイトルを白文字に
                            )
                            Spacer(modifier = Modifier.height(16.dp))

                            // --- 一覧表示 ---
                            if (localRules.isNotEmpty()) {
                                Text(
                                    "追加予定の連動",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = subText // ★ グレーに変更
                                )
                                localRules.forEach { rule ->
                                    val targetName = columns.find { it.id == rule.targetColumnId }?.name ?: "不明"
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
                                            fontSize = 14.sp,
                                            color = mainText // ★ 白文字に
                                        )
                                        IconButton(
                                            onClick = { localRules.remove(rule) },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(Icons.Default.Close, null, tint = Color(0xFFCF6679)) // 少し抑えた赤
                                        }
                                    }
                                }
                                Divider(modifier = Modifier.padding(vertical = 8.dp), color = subText.copy(alpha = 0.3f))
                            }

                            // --- タイミング設定 (ラジオボタン) ---
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                androidx.compose.material3.RadioButton(
                                    selected = !isNextRow,
                                    onClick = { isNextRow = false },
                                    // ★ ラジオボタンの色も紫系に合わせる
                                    colors = androidx.compose.material3.RadioButtonDefaults.colors(
                                        selectedColor = Color(0xFFBB86FC),
                                        unselectedColor = subText
                                    )
                                )
                                Text(
                                    "同じ行",
                                    modifier = Modifier.clickable { isNextRow = false },
                                    fontSize = 14.sp,
                                    color = mainText
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                androidx.compose.material3.RadioButton(
                                    selected = isNextRow,
                                    onClick = { isNextRow = true },
                                    colors = androidx.compose.material3.RadioButtonDefaults.colors(
                                        selectedColor = Color(0xFFBB86FC),
                                        unselectedColor = subText
                                    )
                                )
                                Text(
                                    "次の行",
                                    modifier = Modifier.clickable { isNextRow = true },
                                    fontSize = 14.sp,
                                    color = mainText
                                )
                            }

                            // --- 対象項目設定 (チップ一覧) ---
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                "対象の項目",
                                style = MaterialTheme.typography.labelSmall,
                                color = subText
                            )
                            FlowRow(modifier = Modifier.fillMaxWidth()) {
                                columns.forEach { c ->
                                    val isConfigured = localRules.any { it.targetColumnId == c.id }
                                    FilterChip(
                                        selected = targetColId == c.id,
                                        onClick = { targetColId = c.id; targetValue = "" },
                                        label = {
                                            Text(if (c.id == selectedColumnIdForRule) "${c.name}(自分)" else c.name)
                                        },
                                        // ★ チップのデザインを統一
                                        colors = FilterChipDefaults.filterChipColors(
                                            labelColor = mainText,                     // 通常：白
                                            selectedContainerColor = Color(0xFFEADDFF), // 選択：薄紫
                                            selectedLabelColor = Color.Black            // 選択時文字：黒
                                        ),
                                        border = FilterChipDefaults.filterChipBorder(
                                            enabled = true,
                                            selected = targetColId == c.id,
                                            borderColor = if (isConfigured) Color(0xFFBB86FC) else Color.Gray,
                                            borderWidth = 1.dp,
                                            selectedBorderColor = Color.Gray,
                                            selectedBorderWidth = 1.dp
                                        ),
                                        modifier = Modifier.padding(2.dp)
                                    )
                                }
                            }

                            // --- 入力値設定 (チップ一覧) ---
                            if (targetColId != null) {
                                Spacer(modifier = Modifier.height(12.dp))
                                val opts = columns.find { it.id == targetColId }?.options ?: emptyList()
                                FlowRow(modifier = Modifier.fillMaxWidth()) {
                                    // --- 入力値設定 (下のチップ一覧) ---
                                    opts.forEach { opt ->
                                        FilterChip(
                                            selected = targetValue == opt,
                                            onClick = { targetValue = opt },
                                            label = { Text(opt) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                labelColor = mainText,              // 通常時：白
                                                selectedContainerColor = Color(0xFFEADDFF), // 選択時：薄紫
                                                selectedLabelColor = Color.Black    // 選択時：黒
                                            ),
                                            border = FilterChipDefaults.filterChipBorder(
                                                enabled = true,
                                                selected = targetValue == opt,
                                                // ★ 非選択時も選択時も、枠線はピンクで固定
                                                borderColor = Color(0xFFFFCDD2),
                                                borderWidth = 1.dp,
                                                selectedBorderColor = Color(0xFFFFCDD2),
                                                selectedBorderWidth = 1.dp
                                            ),
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
                                    // ★ 追加ボタンを少し明るい紫に
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFFBB86FC),
                                        contentColor = Color.Black
                                    )
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
                                }) {
                                    Text("キャンセル", color = subText)
                                }
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
                                        containerColor = Color(0xFFBB86FC)
                                    )
                                ) {
                                    Text("保存", color = Color.Black)
                                }
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
                            .background(Color.Black.copy(alpha = 0.4f)) // 背景を黒の半透明に
                            .clickable { showOptionMenuName = null },
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            shadowElevation = 8.dp,
                            color = Color(0xFF252525), // ダイアログの背景色をダークに
                            modifier = Modifier
                                .width(180.dp) // ★ Cのサイズを維持
                                .clickable(enabled = false) { }
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "「$opt」",
                                    fontSize = 18.sp, // ★ Cの文字サイズを維持
                                    fontWeight = FontWeight.Bold,
                                    color = mainText,
                                    modifier = Modifier.padding(bottom = 16.dp)
                                )

                                // 移動ボタンの配色定義（文字色は常に黒）
                                val canMoveColors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFBBBBBB), // 移動可：かなり明るいグレー
                                    contentColor = Color.Black          // 文字：黒
                                )
                                val cannotMoveColors = ButtonDefaults.buttonColors(
                                    disabledContainerColor = Color(0xFF333333), // 移動不可：少し暗めのグレー
                                    disabledContentColor = Color.Black          // 文字：黒（無効時も黒）
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    // 左へ移動ボタン
                                    Button(
                                        onClick = { /* 左移動処理 */ },
                                        enabled = optIndex > 0,
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(60.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        contentPadding = PaddingValues(0.dp),
                                        colors = if (optIndex > 0) canMoveColors else cannotMoveColors
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Icon(Icons.Default.ArrowBack, null, tint = Color.Black)
                                            Text("左へ", fontSize = 14.sp, color = Color.Black)
                                        }
                                    }

                                    // 右へ移動ボタン
                                    Button(
                                        onClick = { /* 右移動処理 */ },
                                        enabled = optIndex < col.options.size - 1,
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(60.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        contentPadding = PaddingValues(0.dp),
                                        colors = if (optIndex < col.options.size - 1) canMoveColors else cannotMoveColors
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Icon(
                                                Icons.Default.ArrowForward,
                                                null,
                                                tint = Color.Black
                                            )
                                            Text("右へ", fontSize = 14.sp, color = Color.Black)
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                // --- 機能系ボタン（条件編集・削除） ---
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    // 1. 条件編集ボタン
                                    Button(
                                        onClick = {
                                            selectedOptionForRule = opt
                                            selectedColumnIdForRule = col.id
                                            showConditionEditDialog = true
                                            showOptionMenuName = null
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(60.dp), // ★ Cの高さ
                                        shape = RoundedCornerShape(12.dp),
                                        contentPadding = PaddingValues(0.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isDarkMode) Color(0xFF4A4458) else Color(
                                                0xFFEADDFF
                                            )
                                        )
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            val tintColor =
                                                if (isDarkMode) Color(0xFFD0BCFF) else Color(
                                                    0xFF7E57C2
                                                )
                                            Icon(
                                                Icons.Default.Build,
                                                null,
                                                tint = tintColor,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Text(
                                                "条件編集",
                                                color = tintColor,
                                                fontSize = 14.sp, // ★ Cのサイズ
                                                maxLines = 1,
                                                softWrap = false,
                                                overflow = TextOverflow.Visible
                                            )
                                        }
                                    }

                                    // 2. 削除ボタン
                                    Button(
                                        onClick = {
                                            scope.launch {
                                                val opts = col.options.toMutableList()
                                                opts.remove(opt)
                                                db.memoDao().updateColumn(col.copy(options = opts))
                                                db.memoDao().deleteRulesByTrigger(col.id, opt)
                                                refreshData()
                                            }
                                            showOptionMenuName = null
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(60.dp), // ★ Cの高さ
                                        shape = RoundedCornerShape(12.dp),
                                        contentPadding = PaddingValues(0.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isDarkMode) Color(0xFF8C1D18).copy(
                                                alpha = 0.5f
                                            ) else Color(0xFFFFEBEE)
                                        )
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            val deleteColor =
                                                if (isDarkMode) Color(0xFFFFB4AB) else Color.Red
                                            Icon(
                                                Icons.Default.Delete,
                                                null,
                                                tint = deleteColor,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Text(
                                                "削除",
                                                color = deleteColor,
                                                fontSize = 14.sp, // ★ Cのサイズ
                                                maxLines = 1,
                                                softWrap = false,
                                                overflow = TextOverflow.Visible
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            // --- 3. 入力エリア (オーバーレイ) ---
            if (showInputArea) {
                // 画面全体を覆う半透明レイヤー
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White.copy(alpha = 0.2f))
                        .clickable { showInputArea = false }
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            // ★ Bの挙動：内容に合わせて伸縮、最大90%
                            .heightIn(
                                min = 0.dp,
                                max = androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp.dp * 0.9f
                            )
                            .clickable(enabled = false) { }, // 背後のクリックを遮断
                        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                        color = Color(0xFF121212) // backColor
                    ) {
                        // ★ 隙間管理：ナビゲーションバー分の余白を内側に持たせる
                        Column(modifier = Modifier.navigationBarsPadding()) {
                            InputFormContent(
                                db = db,
                                columns = columns,
                                inputValues = inputValues,
                                editingRecordId = editingRecordId,
                                onSave = { showInputArea = false; refreshData() },
                                mainText = Color.White,
                                subText = Color.LightGray,
                                isDarkMode = true // 常にダークモード
                            )
                        }
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
        onSave: () -> Unit,
        mainText: Color,
        subText: Color,
        isDarkMode: Boolean
    ) {
        val scope = rememberCoroutineScope()
        var showDeleteConfirmDialog by remember { mutableStateOf(false) }

        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = if (editingRecordId != null) "メモを編集" else "新規メモ入力",
                style = MaterialTheme.typography.headlineSmall,
                color = mainText
            )
            Spacer(modifier = Modifier.height(16.dp))

            columns.forEach { column ->
                val options = column.options
                val currentValue = inputValues[column.id] ?: ""

                Text(
                    text = column.name,
                    style = MaterialTheme.typography.labelLarge,
                    color = Color(0xFFBB86FC),
                    modifier = Modifier.padding(top = 8.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Start
                )

                // ★修正ポイント：if 文で Row を囲む
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
                            val bgColor = if (isSelected) Color(0xFF7E57C2) else Color(0xFF333333)
                            val textColor = if (isSelected) Color.White else mainText

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
                } // ★if の閉じ

                // 2. 手入力欄
                if (options.isEmpty() || column.showTextField) {
                    OutlinedTextField(
                        value = currentValue,
                        onValueChange = { inputValues[column.id] = it },
                        placeholder = {
                            if (options.isNotEmpty()) {
                                Text("入力欄", fontSize = 12.sp, color = subText)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp), // Bの角丸
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = mainText,
                            unfocusedTextColor = mainText,
                            focusedContainerColor = Color(0xFF252525),
                            unfocusedContainerColor = Color(0xFF252525),
                            cursorColor = mainText,
                            focusedBorderColor = Color(0xFF7E57C2),
                            unfocusedBorderColor = Color.Transparent
                        )
                    )
                }
            } // columns.forEach の閉じ

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
                                    // ★ここを修正：メソッド呼び出しを正しく記述
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
                                        val allRecords = db.memoDao().getAllRecords()
                                        val currentIndex =
                                            allRecords.indexOfFirst { it.id == currentRid }
                                        val nextRecord =
                                            if (currentIndex != -1 && currentIndex + 1 < allRecords.size) {
                                                allRecords[currentIndex + 1]
                                            } else null

                                        if (nextRecord != null) {
                                            db.memoDao().insertValue(
                                                MemoValue(
                                                    recordId = nextRecord.id,
                                                    columnId = rule.targetColumnId,
                                                    value = rule.targetValue
                                                )
                                            )
                                        } else {
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
                    Text(
                        if (editingRecordId != null) "変更を保存" else "メモに追加",
                        color = Color.White
                    )
                }

                if (editingRecordId != null) {
                    Button(
                        onClick = { showDeleteConfirmDialog = true },
                        modifier = Modifier.size(56.dp),
                        shape = RoundedCornerShape(28.dp),
                        contentPadding = PaddingValues(0.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB3261E))
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "削除", tint = Color.White)
                    }
                }
            }

            if (showDeleteConfirmDialog) {
                AlertDialog(
                    onDismissRequest = { showDeleteConfirmDialog = false },
                    title = { Text(text = "削除", color = mainText) },
                    text = { Text(text = "この行を削除してもよろしいですか？", color = mainText) },
                    containerColor = Color(0xFF1E1E1E),
                    confirmButton = {
                        TextButton(onClick = {
                            scope.launch {
                                if (editingRecordId != null) {
                                    db.memoDao().deleteValuesByRecordId(editingRecordId)
                                    db.memoDao().deleteRecordById(editingRecordId)
                                }
                                showDeleteConfirmDialog = false
                                onSave()
                            }
                        }) {
                            Text("削除", color = Color(0xFFF44336))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDeleteConfirmDialog = false }) {
                            Text("キャンセル", color = mainText)
                        }
                    }
                )
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
        onDelete: () -> Unit,
        mainText: Color,
        subText: Color,
        dividerColor: Color
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
                        color = subText, // ★ 1. Color.Gray から「補足文字色」に変更
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
                            color = mainText, // ★ 2. ここを追加：モードに合わせて白/黒切り替え
                            maxLines = 1
                        )
                    }
                }
            } // ★ 3. Color.LightGray から「区切り線色」に変更
            Divider(color = dividerColor)
        }
    }
}