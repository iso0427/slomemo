package iso.slomemo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
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

        WindowCompat.setDecorFitsSystemWindows(window, true)

        val db = Room.databaseBuilder(
            applicationContext, AppDatabase::class.java, "memo-db"
        ).fallbackToDestructiveMigration().build()

        lifecycleScope.launch(Dispatchers.IO) {
            val dao = db.memoDao()
            // 1. 今、項目が空っぽかどうか確認する（Direct版をDaoに追加しておくこと！）
            val currentColumns = dao.getAllColumnsDirect()

            // 2. 空っぽなら、いつもの「pt」「G数」「契機」を勝手に入れる
            if (currentColumns.isEmpty()) {
                dao.insertColumn(
                    ColumnSetting(
                        name = "pt", options = listOf(
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
                        ), displayOrder = 0
                    )
                )
                dao.insertColumn(
                    ColumnSetting(
                        name = "契機", options = listOf(
                            "pt",
                            "強チェ",
                            "ﾁｬﾝｽ目",
                            "ﾏｷﾞﾁｬﾚ",
                            "黒江ﾁｬﾚ",
                            "復活",
                            "引き戻し",
                            "直撃",
                            "中段チェリー",
                            "フリーズ",
                            "━"
                        ), displayOrder = 1
                    )
                )
                dao.insertColumn(
                    ColumnSetting(
                        name = "種別",
                        options = listOf("BIG", "みたま", "AT", "エピボ", "アリナ", "覚醒", "━"),
                        displayOrder = 2
                    )
                )
                dao.insertColumn(
                    ColumnSetting(
                        name = "AT", options = listOf("〇", "✕", "━"), displayOrder = 3
                    )
                )
                dao.insertColumn(
                    ColumnSetting(
                        name = "BIG終了画面", options = listOf(
                            "デフォルト",
                            "フェリシア",
                            "さな",
                            "やちよ&鶴乃",
                            "ももこﾁｰﾑ",
                            "ﾊﾞｹｰｼｮﾝ",
                            "2nd",
                            "1st",
                            "キュゥべえ",
                            "━"
                        ), displayOrder = 4
                    )
                )
                dao.insertColumn(
                    ColumnSetting(
                        name = "AT終了画面", options = listOf(
                            "デフォルト", "マギウス", "みかづき荘", "ED", "まどか", "━"
                        ), displayOrder = 5
                    )
                )
                dao.insertColumn(
                    ColumnSetting(
                        name = "STORY", options = listOf("━"), displayOrder = 6
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

                    val controller = WindowCompat.getInsetsController(window, view)
                    // false にすると文字が白くなる（黒背景用）
                    controller.isAppearanceLightStatusBars = false
                    controller.isAppearanceLightNavigationBars = false
                }
            }

            MaterialTheme {
                // 1. スナックバーの状態を管理する変数をここで作る
                val snackbarHostState = remember { SnackbarHostState() }

                // 2. Scaffold（アプリの土台）を導入して、スナックバーを表示できるようにする
                Scaffold(
                    // ここでスナックバーが出る場所を指定
                    snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                    // 背景色をSurfaceに合わせて黒に
                    containerColor = Color.Black
                ) { padding ->
                    // 3. 元々のコンテンツを Scaffold の中に配置
                    // padding は Scaffold が計算してくれる余白です
                    Box(modifier = Modifier.padding(padding)) {
                        // ここで snackbarHostState を渡せるように引数を追加するか、
                        // TestColumnApp の中で CompositionLocal などを使う形になります
                        TestColumnApp(db, snackbarHostState)
                    }
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
    fun TestColumnApp(db: AppDatabase, snackbarHostState: SnackbarHostState) {
        // 1. 【変更】モード判定をなくし、常に true (ダークモード) として扱う
        val isDarkMode = true

        // 2. 【変更】色をダークモード専用の固定値にする
        val backColor = Color(0xFF121212)      // 画面全体の背景
        val surfaceColor = Color(0xFF3A3A3A)   // メニューやダイアログの箱
        val mainText = Color.White             // メインの文字（常に白）
        val subText = Color.LightGray          // 補足の文字
        val dividerColor = Color(0xFF333333)   // 区切り線

        // --- 以下、状態管理などはそのまま ---
        var currentScreen by remember { mutableStateOf("main") }
        var columns by remember { mutableStateOf(listOf<ColumnSetting>()) }
        var records by remember { mutableStateOf(listOf<MemoRecord>()) }
        var showInputArea by remember { mutableStateOf(false) }
        var menuExpanded by remember { mutableStateOf(false) }
        var showResetDialog by remember { mutableStateOf(false) }
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

        // 削除ダイアログを表示するかどうか
        // どのルールを消すか一時保存する変数

        // 他の remember 変数が並んでいるところに置く
        var showOptionDeleteConfirm by remember { mutableStateOf(false) }
        var pendingDeleteTarget by remember { mutableStateOf<Pair<ColumnSetting, String>?>(null) }
        //項目削除の確認用
        var showColumnDeleteConfirm by remember { mutableStateOf(false) }
        //どの項目を消そうとしているか保持する
        var pendingDeleteColumn by remember { mutableStateOf<ColumnSetting?>(null) }
        val currentRules = remember { mutableStateListOf<AutoInputRule>() }

        // 履歴を保存するリスト
        var undoStack by remember { mutableStateOf(listOf<List<AutoInputRule>>()) }
        var redoStack by remember { mutableStateOf(listOf<List<AutoInputRule>>()) }

        // 状態が変わる前にこれを呼んで履歴に保存する関数
        fun saveHistory() {
            undoStack = undoStack + listOf(currentRules.toList())
            redoStack = emptyList() // 新しい操作をしたら Redo 履歴はクリア
        }

        // Undo: 一つ前の状態に戻す
        val performUndo = {
            if (undoStack.isNotEmpty()) {
                redoStack = redoStack + listOf(currentRules.toList())
                val lastState = undoStack.last()
                undoStack = undoStack.dropLast(1)
                currentRules.clear()
                currentRules.addAll(lastState)
            }
        }
        // Redo: Undoした操作をやり直す
        val performRedo = {
            if (redoStack.isNotEmpty()) {
                undoStack = undoStack + listOf(currentRules.toList())
                val nextState = redoStack.last()
                redoStack = redoStack.dropLast(1)
                currentRules.clear()
                currentRules.addAll(nextState)
            }
        }
        fun refreshData() {
            scope.launch {
                columns = db.memoDao().getAllColumns()
                records = db.memoDao().getAllRecords()
                valuesMap = db.memoDao().getAllValues().groupBy { it.recordId }
                val rulesFromDb = db.memoDao().getAllAutoInputRules()
                currentRules.clear()
                currentRules.addAll(rulesFromDb)
            }
        }

        LaunchedEffect(Unit) { refreshData() }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backColor)
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
                modifier = Modifier.fillMaxSize(),
                containerColor = backColor,
                // Scaffoldの自動余白を完全にオフにする
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                topBar = {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(backColor)
                            // ステータスバー分を自動で開けず、手動で最小限の余白（4dpなど）にする
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

                        // 右側のボタン群（Undo/Redo/Menu）
                        if (currentScreen == "main") {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { performUndo() }, enabled = undoStack.isNotEmpty()) {
                                    Icon(Icons.Default.ArrowBack, "Undo", tint = if (undoStack.isNotEmpty()) mainText else Color.Gray)
                                }
                                IconButton(onClick = { performRedo() }, enabled = redoStack.isNotEmpty()) {
                                    Icon(Icons.Default.ArrowForward, "Redo", tint = if (redoStack.isNotEmpty()) mainText else Color.Gray)
                                }
                                IconButton(onClick = { menuExpanded = true }, modifier = Modifier.offset(x = 12.dp)) {
                                    Icon(Icons.Default.Menu, null, tint = mainText)
                                }
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
                            // ナビゲーションバー（画面下の線）に被らないギリギリまで下げる
                            modifier = Modifier.navigationBarsPadding(),
                            containerColor = Color(0xFF7E57C2),
                            contentColor = Color.White
                        ) { Icon(Icons.Default.Add, "入力") }
                    }
                }
            ) { padding ->
                // コンテンツ表示エリア
                Box(modifier = Modifier.fillMaxSize()) {
                    if (currentScreen == "main") {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                // topBarが被らないように、上側だけ余白を作る
                                .padding(top = padding.calculateTopPadding())
                        ) {
                            // --- 一覧のヘッダー ---
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(if (isDarkMode) Color(0xFF4A4458) else Color(0xFFEADDFF).copy(alpha = 0.5f))
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
                                verticalAlignment = Alignment.CenterVertically) {
                                Switch(checked = showTime, onCheckedChange = { isChecked ->
                                    scope.launch {
                                        db.memoDao().updateSetting(AppSetting(showTime = isChecked))
                                    }
                                })
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("時間を表示する", color = mainText)
                            }

                            //Spacer(modifier = Modifier.height(16.dp))
                            Divider()
                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                "項目の追加",
                                style = MaterialTheme.typography.titleMedium,
                                color = mainText
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                TextField(
                                    value = newColumnName,
                                    onValueChange = { newColumnName = it },
                                    // ★ label から placeholder に変更してスッキリさせる
                                    placeholder = { Text("新しい項目を入力...", color = subText) },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    shape = RoundedCornerShape(8.dp),
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = Color(0xFF252525),
                                        unfocusedContainerColor = Color(0xFF252525),

                                        focusedTextColor = mainText,
                                        unfocusedTextColor = mainText,

                                        cursorColor = Color(0xFFEADDFF),
                                        focusedIndicatorColor = Color(0xFFEADDFF),
                                        unfocusedIndicatorColor = Color.Transparent
                                    )
                                )

                                Spacer(modifier = Modifier.width(8.dp)) // padding から Spacer に変更して統一

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
                                    // ★ 保存/選択肢追加と同じ濃い紫
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF7E57C2)
                                    ),
                                ) {
                                    Text("追加", color = Color.White)
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))
//                            Divider()
//                            Spacer(modifier = Modifier.height(16.dp))

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

                                    // --- 設定画面の項目並び替えチップ部分 ---
                                    Box {
                                        FilterChip(
                                            selected = selectedColumnId == col.id,
                                            onClick = { selectedColumnId = col.id },
                                            label = {
                                                Text(
                                                    text = col.name,
                                                    color = if (selectedColumnId == col.id) Color.Black else mainText,
                                                    modifier = Modifier.combinedClickable(
                                                        onClick = { selectedColumnId = col.id },
                                                        onLongClick = {
                                                            showColumnMenuId = col.id
                                                        })
                                                )
                                            },
                                            colors = FilterChipDefaults.filterChipColors(
                                                containerColor = Color(0xFF333333),
                                                selectedContainerColor = Color(0xFFEADDFF)
                                            ),
                                            // ★ ここを FilterChipDefaults から BorderStroke に書き換え！
                                            border = androidx.compose.foundation.BorderStroke(
                                                width = 1.dp,
                                                color = if (selectedColumnId == col.id) Color(
                                                    0xFFEADDFF
                                                ) else Color.Gray
                                            ),
                                            modifier = Modifier.padding(end = 4.dp)
                                        )
                                    }
                                }
                            }
                            Text(
                                text = "※項目を長押しで左右に移動",
                                style = MaterialTheme.typography.labelSmall,
                                color = subText,
                                textAlign = TextAlign.End, // ★ 右側に寄せる
                                modifier = Modifier
                                    .fillMaxWidth()        // ★ 幅いっぱいに広げないと右に寄らないので必須
                                    .padding(top = 4.dp, end = 8.dp, bottom = 8.dp) // 余白を微調整
                            )

                            selectedColumnId?.let { colId ->
                                val col = columns.find { it.id == colId } ?: return@let
                                var newOptionName by remember(col.id) { mutableStateOf("") }

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    "項目名の編集",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = mainText,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )

                                var editingName by remember(col.id) { mutableStateOf(col.name) }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    TextField(
                                        value = editingName,
                                        onValueChange = { editingName = it },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true,
                                        // ★ 角を丸くしてチップと親和性を出す
                                        shape = RoundedCornerShape(8.dp),
                                        colors = TextFieldDefaults.colors(
                                            // ★ 背景：窓(3A)より暗く、画面底(00)より明るい 0xFF252525
                                            focusedContainerColor = Color(0xFF252525),
                                            unfocusedContainerColor = Color(0xFF252525),

                                            // 文字色（白）
                                            focusedTextColor = mainText,
                                            unfocusedTextColor = mainText,

                                            // ★ 下線とアクセント：あの「薄い紫」をここで活用
                                            cursorColor = Color(0xFFEADDFF),
                                            focusedIndicatorColor = Color(0xFFEADDFF),
                                            unfocusedIndicatorColor = Color.Transparent, // 非フォーカス時は線を消すとスッキリ

                                            // プレースホルダー色
                                            unfocusedPlaceholderColor = subText,
                                            focusedPlaceholderColor = subText
                                        )
                                    )

                                    // 保存ボタンなどの続き...
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Button(
                                        onClick = {
                                            scope.launch {
                                                db.memoDao()
                                                    .updateColumn(col.copy(name = editingName))
                                                refreshData()
                                            }
                                        },
                                        // ★ 色の指定を追加
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFF7E57C2)
                                        )
                                    ) {
                                        Text("保存", color = Color.White) // 文字は常に白でOK
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
                                    verticalAlignment = Alignment.CenterVertically) {
                                    Switch(
                                        checked = col.showTextField,
                                        onCheckedChange = { isChecked ->
                                            scope.launch {
                                                db.memoDao()
                                                    .updateColumn(col.copy(showTextField = isChecked))
                                                refreshData()
                                            }
                                        })
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        "入力欄を表示する",

                                        color = mainText
                                    )
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                Text(
                                    text = "「${col.name}」の選択肢一覧",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = mainText,
                                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                                )

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    TextField(
                                        value = newOptionName,
                                        onValueChange = { newOptionName = it },
                                        placeholder = {
                                            Text(
                                                "新しい選択肢を入力...", color = subText
                                            )
                                        },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true,
                                        shape = RoundedCornerShape(8.dp),
                                        colors = TextFieldDefaults.colors(
                                            focusedContainerColor = Color(0xFF252525),
                                            unfocusedContainerColor = Color(0xFF252525),
                                            focusedTextColor = mainText,
                                            unfocusedTextColor = mainText,
                                            cursorColor = Color(0xFFEADDFF),
                                            focusedIndicatorColor = Color(0xFFEADDFF),
                                            unfocusedIndicatorColor = Color.Transparent
                                        )
                                    )

                                    Spacer(modifier = Modifier.width(8.dp))

                                    // --- 保存/新しい項目と同じスタイルの「追加」ボタン ---
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
                                        // ★ ここをさっきのボタンと統一
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFF7E57C2)
                                        ),
                                    ) {
                                        Text("追加", color = Color.White)
                                    }
                                }

                                FlowRow(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp)
                                ) {
                                    col.options.forEachIndexed { optIndex, opt ->
                                        // ★ CompositionLocalProvider をループの内側に移動
                                        CompositionLocalProvider(
                                            LocalMinimumInteractiveComponentSize provides Dp.Unspecified
                                        ) {
                                            InputChip(
                                                selected = showOptionMenuName == opt,
                                                onClick = { showOptionMenuName = opt },
                                                label = {
                                                    Text(
                                                        text = opt,
                                                        color = if (showOptionMenuName == opt) Color.Black else mainText,
                                                        style = MaterialTheme.typography.bodySmall
                                                    )
                                                },
                                                colors = InputChipDefaults.inputChipColors(
                                                    containerColor = Color(0xFF333333),
                                                    selectedContainerColor = Color(0xFFEADDFF)
                                                ),
                                                border = null,
                                                modifier = Modifier
                                                    .padding(4.dp)
                                                    .height(32.dp) // これで高さがシュッとします
                                                    .border(
                                                        width = 1.dp,
                                                        color = if (showOptionMenuName == opt) Color(
                                                            0xFFEADDFF
                                                        ) else Color.Gray,
                                                        shape = RoundedCornerShape(8.dp)
                                                    )
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(24.dp))

                                //--- この項目自体を削除するボタン ---
                                Button(
                                    onClick = {
                                        pendingDeleteColumn = col
                                        showColumnDeleteConfirm = true
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        // ★ ダークモードに馴染む少し深めの赤
                                        containerColor = Color(0xFF8C1D18).copy(alpha = 0.8f)
                                    ),
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Text(
                                        text = "「${col.name}」を削除する", color = mainText
                                    )
                                }
                            }
                        }
                    }
                }
                if (showColumnDeleteConfirm && pendingDeleteColumn != null) {
                    AlertDialog(onDismissRequest = {
                        showColumnDeleteConfirm = false
                        pendingDeleteColumn = null
                    }, title = {
                        Text("項目の削除", color = mainText)
                    }, text = {
                        Text(
                            "「${pendingDeleteColumn?.name}」を削除しますか？\nこの項目に関連するメモや連動設定もすべて削除されます。",
                            color = mainText
                        )
                    }, containerColor = Color(0xFF1E1E1E), confirmButton = {
                        TextButton(
                            onClick = {
                                // ★ ここで実際の削除処理を実行
                                val target = pendingDeleteColumn
                                if (target != null) {
                                    scope.launch {
                                        // 1. 項目自体を消す
                                        db.memoDao().deleteColumn(target)

                                        // 2. ★ここを追加：その項目に紐づくメモの値を消す
                                        db.memoDao().deleteValuesByColumnId(target.id)

                                        // 3. ★ここを追加：その項目がきっかけの連動ルールを消す
                                        db.memoDao().deleteRulesByTriggerColumn(target.id)

                                        // 4. ★ここを追加：その項目が入力先の連動ルールも消す
                                        db.memoDao().deleteRulesByTargetColumn(target.id)
                                        // IDをリセットして画面を更新
                                        selectedColumnId = null
                                        refreshData()
                                    }
                                }
                                showColumnDeleteConfirm = false
                                pendingDeleteColumn = null
                            }) {
                            Text("削除", color = Color(0xFFF44336))
                        }
                    }, dismissButton = {
                        TextButton(onClick = {
                            showColumnDeleteConfirm = false
                            pendingDeleteColumn = null
                        }) {
                            Text("キャンセル", color = mainText)
                        }
                    })
                }
            }

            // --- メニュー専用レイヤー (自作ガードレール) ---
            if (menuExpanded) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Transparent) // alpha 0.0f と同じ
                        .clickable { menuExpanded = false }) {
                    // メニュー本体
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 80.dp, end = 4.dp)
                            .width(220.dp), // ★ 項目が減るので、少し幅を広げると文字が収まりやすくなります
                        shape = RoundedCornerShape(5.dp),
                        shadowElevation = 8.dp,
                        color = surfaceColor,
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            // --- 項目：設定 (「実戦データ入力」は削除しました) ---
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        currentScreen = "settings"
                                        menuExpanded = false
                                    }
                                    .padding(horizontal = 16.dp, vertical = 16.dp),
                                verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = null,
                                    tint = mainText,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "項目・選択肢の設定", // ここだけが残ります
                                    fontSize = 18.sp, color = mainText
                                )
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        // 直接消さずに、まずはダイアログの「スイッチ」を入れる
                                        showResetDialog = true
                                    }
                                    .padding(horizontal = 16.dp, vertical = 16.dp),
                                verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Delete, null, tint = mainText, // ここだけ赤にする
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("メモのリセット", fontSize = 18.sp, color = mainText)
                            }
                        }
                    }
                }
            }
            // --- リセット確認ダイアログ ---
            if (showResetDialog) {
                AlertDialog(
                    onDismissRequest = { showResetDialog = false },
                    containerColor = Color.White, // 背景を白に固定して「警告」を目立たせる
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // 警告アイコンを追加
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = Color.Red
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "メモのリセット",
                                color = Color.Red,
                                fontWeight = FontWeight.Bold // 太字
                            )
                        }
                    },
                    text = {
                        Text(
                            text = "保存されているすべてのメモが削除されます。",
                            color = Color.Black // 白背景で確実に見えるように黒に固定
                        )
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                scope.launch {
                                    db.memoDao().softDeleteAll()
                                    refreshData()
                                    showResetDialog = false
                                    menuExpanded = false
                                }
                            }) {
                            // 実行ボタンも太字にして強調
                            Text(
                                "リセットする", color = Color.Red, fontWeight = FontWeight.ExtraBold
                            )
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showResetDialog = false }) {
                            // キャンセルは少し控えめな色（黒系）
                            Text("キャンセル", color = Color.Black)
                        }
                    })
            }
            // --- 手順4：項目移動メニュー (長押し用レイヤー) ---
            if (showColumnMenuId != null) {
                val col = columns.find { it.id == showColumnMenuId }

                if (col != null) {
                    val colIndex = columns.indexOf(col)

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Transparent)
                            .clickable { showColumnMenuId = null },
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            shadowElevation = 8.dp,
                            color = surfaceColor, // ★ 0xFF3A3A3A
                            modifier = Modifier
                                .width(180.dp)
                                .clickable(enabled = false) { }) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "${col.name}",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = mainText, // 白文字
                                    modifier = Modifier.padding(bottom = 16.dp)
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    // --- 左へ移動 ---
                                    val canMoveLeft = colIndex > 0
                                    Button(
                                        onClick = {
                                            scope.launch {
                                                val list = columns.toMutableList()
                                                val item = list.removeAt(colIndex)
                                                list.add(colIndex - 1, item)
                                                // 並び順(displayOrder)を一括更新
                                                list.forEachIndexed { i, c ->
                                                    db.memoDao()
                                                        .updateColumn(c.copy(displayOrder = i))
                                                }
                                                refreshData()
                                            }
                                            showColumnMenuId = null
                                        },
                                        enabled = canMoveLeft,
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(60.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        contentPadding = PaddingValues(0.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            // ★ 有効なら薄紫、無効なら暗いグレー
                                            containerColor = if (canMoveLeft) Color(0xFFEADDFF) else Color(
                                                0xFF333333
                                            ),
                                            disabledContainerColor = Color(0xFF222222),
                                            disabledContentColor = Color.Black
                                        )
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Icon(
                                                Icons.Default.ArrowBack,
                                                null,
                                                tint = Color.Black,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Text(
                                                "左へ", color = Color.Black, fontSize = 14.sp
                                            )
                                        }
                                    }

                                    // --- 右へ移動 ---
                                    val canMoveRight = colIndex < columns.size - 1
                                    Button(
                                        onClick = {
                                            scope.launch {
                                                val list = columns.toMutableList()
                                                val item = list.removeAt(colIndex)
                                                list.add(colIndex + 1, item)
                                                // 並び順(displayOrder)を一括更新
                                                list.forEachIndexed { i, c ->
                                                    db.memoDao()
                                                        .updateColumn(c.copy(displayOrder = i))
                                                }
                                                refreshData()
                                            }
                                            showColumnMenuId = null
                                        },
                                        enabled = canMoveRight,
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(60.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        contentPadding = PaddingValues(0.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (canMoveLeft) Color(0xFFEADDFF) else Color(
                                                0xFF333333
                                            ), disabledContainerColor = Color(0xFF222222),
                                            // ★ 無効時も中身の色を「黒」に指定
                                            disabledContentColor = Color.Black
                                        )
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            if (canMoveRight) Color.Black else subText
                                            Icon(
                                                Icons.Default.ArrowForward,
                                                null,
                                                tint = Color.Black,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Text(
                                                "左へ", color = Color.Black, fontSize = 14.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            //--- 自動入力ルールの設定ダイアログ (以前の構成に戻した修正版) ---
            if (showConditionEditDialog && selectedColumnIdForRule != null && selectedOptionForRule != null) {
                val localRules = remember { mutableStateListOf<AutoInputRule>() }
                var isNextRow by remember { mutableStateOf(false) }
                var targetColId by remember { mutableStateOf<Int?>(null) }
                var targetValue by remember { mutableStateOf("") }

                LaunchedEffect(selectedColumnIdForRule, selectedOptionForRule) {
                    scope.launch(Dispatchers.IO) {
                        val existingRules = db.memoDao().getRulesByTrigger(
                            selectedColumnIdForRule!!, selectedOptionForRule!!
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
                        color = surfaceColor,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "「${selectedOptionForRule}」選択時の連動入力",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = mainText // ★ 文字色を白に
                            )
                            Spacer(modifier = Modifier.height(16.dp))

                            // --- 一覧表示 (以前の構成) ---
                            if (localRules.isNotEmpty()) {
                                Text(
                                    "追加予定の連動",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = subText
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
                                            fontSize = 14.sp,
                                            color = mainText
                                        )
                                        IconButton(
                                            onClick = { localRules.remove(rule) },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(Icons.Default.Close, null, tint = Color.Red)
                                        }
                                    }
                                }
                            }

                            // --- タイミング設定 (ラジオボタン) ---
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                androidx.compose.material3.RadioButton(
                                    selected = !isNextRow, onClick = { isNextRow = false },
                                    // ★ ラジオボタンの色も紫系に合わせる
                                    colors = androidx.compose.material3.RadioButtonDefaults.colors(
                                        selectedColor = Color(0xFFEADDFF), unselectedColor = subText
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
                                    selected = isNextRow, onClick = { isNextRow = true })
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
                                            Text(
                                                if (c.id == selectedColumnIdForRule) "${c.name}(自分)" else c.name,
                                                // ★ ここにも mainText を適用（念のため）
                                                color = if (targetColId == c.id) Color.White else mainText
                                            )
                                        },
                                        colors = FilterChipDefaults.filterChipColors(
                                            // 選択されていない時の背景
                                            containerColor = if (isConfigured) Color(0xFF1976D2).copy(
                                                alpha = 0.0f
                                            ) else Color.Transparent,
                                            // 選択されていない時の文字色（★ Black から mainText へ！）
                                            labelColor = if (isConfigured) Color(0xFF64B5F6) else mainText,
                                            // 選択されている時の色
                                            selectedContainerColor = Color(0xFF7E57C2),
                                            selectedLabelColor = Color.White
                                        ),
                                        modifier = Modifier.padding(2.dp)
                                    )
                                }
                            }

                            // --- 入力値設定 (チップ一覧) ---
                            if (targetColId != null) {
                                Spacer(modifier = Modifier.height(12.dp))

                                Text(
                                    "連動対象", // または「連動内容」など
                                    style = MaterialTheme.typography.labelSmall,
                                    color = subText // 他のタイトルと合わせて subText (薄いグレー) にするのがオススメ
                                )

                                val opts =
                                    columns.find { it.id == targetColId }?.options ?: emptyList()
                                FlowRow(modifier = Modifier.fillMaxWidth()) {
                                    opts.forEach { opt ->
                                        FilterChip(
                                            selected = targetValue == opt,
                                            onClick = { targetValue = opt },
                                            label = {
                                                Text(
                                                    text = opt,
                                                    // ★ 選択時は黒（背景が薄紫になるため）、未選択時は白
                                                    color = if (targetValue == opt) Color.Black else mainText
                                                )
                                            },
                                            // ★ 枠線の赤線対策 兼 デザイン統一
                                            border = androidx.compose.foundation.BorderStroke(
                                                width = 1.dp,
                                                color = if (targetValue == opt) Color(0xFFEADDFF) else Color.Gray
                                            ),
                                            colors = FilterChipDefaults.filterChipColors(
                                                // isConfigured を使わず、濃いグレーなどで固定
                                                containerColor = Color(0xFF1976D2).copy(alpha = 0.2f),
                                                selectedContainerColor = Color(0xFFEADDFF)
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
                                    // ★ ボタンの色も少し紫を混ぜると統一感が出ます（お好みで！）
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF4A4458),
                                        contentColor = Color.White
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
                                TextButton(onClick = {
                                    showConditionEditDialog = false
                                }) { Text("キャンセル", color = Color(0xFFEADDFF)) }
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = {
                                        scope.launch {
                                            db.memoDao().deleteRulesByTrigger(
                                                selectedColumnIdForRule!!, selectedOptionForRule!!
                                            )
                                            localRules.forEach { db.memoDao().insertRule(it) }
                                            showConditionEditDialog = false
                                            refreshData()
                                        }
                                    }, colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(
                                            0xFF7E57C2
                                        )
                                    )
                                ) {
                                    Text("設定を保存", color = Color.White)
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
                            .background(Color.Green.copy(alpha = 0.0f)) // 選択肢は緑
                            .clickable { showOptionMenuName = null },
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            shadowElevation = 8.dp,
                            // ★ 窓自体の背景色：surfaceColor (0xFF1E1E1E) が設定されていればそれを使います
                            // もし設定が不安なら、直接 Color(0xFF1E1E1E) と書いてもOKです
                            color = surfaceColor,
                            modifier = Modifier
                                .width(180.dp)
                                .clickable(enabled = false) { }) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                // ★ タイトル文字の色を mainText (白) に固定
                                Text(
                                    text = "$opt",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = mainText, // ← ここ！
                                    modifier = Modifier.padding(bottom = 16.dp)
                                )

                                // --- 移動ボタンのペア ---
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    // --- 左へボタン ---
                                    val canMoveLeft = optIndex > 0
                                    Button(
                                        onClick = {
                                            scope.launch {
                                                val opts = col.options.toMutableList()
                                                val item = opts.removeAt(optIndex)
                                                opts.add(optIndex - 1, item)
                                                db.memoDao().updateColumn(col.copy(options = opts))
                                                refreshData()
                                            }
                                            showOptionMenuName = null
                                        },
                                        enabled = canMoveLeft,
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(60.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        contentPadding = PaddingValues(0.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (canMoveLeft) Color(0xFFEADDFF) else Color(
                                                0xFF333333
                                            ), disabledContainerColor = Color(0xFF222222),
                                            // ★ 無効時も中身の色を「黒」に指定
                                            disabledContentColor = Color.Black
                                        )
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            // ★ 有効なら黒、無効ならグレー
                                            if (canMoveLeft) Color.Black else subText
                                            Icon(
                                                Icons.Default.ArrowBack,
                                                null,
                                                tint = Color.Black,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Text(
                                                "左へ", color = Color.Black, fontSize = 14.sp
                                            )
                                        }
                                    }

                                    // --- 右へボタン ---
                                    val canMoveRight = optIndex < col.options.size - 1
                                    Button(
                                        onClick = {
                                            scope.launch {
                                                val opts = col.options.toMutableList()
                                                val item = opts.removeAt(optIndex)
                                                opts.add(optIndex + 1, item)
                                                db.memoDao().updateColumn(col.copy(options = opts))
                                                refreshData()
                                            }
                                            showOptionMenuName = null
                                        },
                                        enabled = canMoveRight,
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(60.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        contentPadding = PaddingValues(0.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            // ★ ルール適用：有効なら薄紫、無効なら暗いグレー
                                            containerColor = if (canMoveRight) Color(0xFFEADDFF) else Color(
                                                0xFF333333
                                            ), disabledContainerColor = Color(0xFF222222)
                                        )
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            if (canMoveRight) Color.Black else subText
                                            Icon(
                                                Icons.Default.ArrowForward,
                                                null,
                                                tint = Color.Black,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Text(
                                                "右へ", color = Color.Black, fontSize = 14.sp
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(20.dp))

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
                                            .height(60.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        contentPadding = PaddingValues(0.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            // ★ ダークモード時は深めの紫、ライトモード時は薄い紫
                                            containerColor = if (isDarkMode) Color(0xFF4A4458) else Color(
                                                0xFFEADDFF
                                            )
                                        )
                                    ) {
                                        Column(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            val tintColor =
                                                if (isDarkMode) Color(0xFFD0BCFF) else Color(
                                                    0xFF7E57C2
                                                ) // 文字/アイコン色も調整
                                            Icon(
                                                Icons.Default.Build,
                                                null,
                                                tint = tintColor,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Text(
                                                "条件編集",
                                                color = tintColor,
                                                fontSize = 14.sp,
                                                maxLines = 1,
                                                softWrap = false,
                                                overflow = TextOverflow.Visible
                                            )
                                        }
                                    }

                                    // 2. 削除ボタン
                                    Button(
                                        onClick = {
                                            // ★ 直接 scope.launch せずに、ターゲットを保存してダイアログを出す
                                            pendingDeleteTarget = col to opt
                                            showOptionDeleteConfirm = true
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(60.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        contentPadding = PaddingValues(0.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            // ★ ダークモード時は深めの赤、ライトモード時は薄い赤
                                            containerColor = if (isDarkMode) Color(0xFF8C1D18).copy(
                                                alpha = 0.5f
                                            ) else Color(0xFFFFEBEE)
                                        )
                                    ) {
                                        Column(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
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
                                                fontSize = 14.sp,
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
                // 画面全体を覆うレイヤー（背景を少し暗くする）
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White.copy(alpha = 0.2f))
                        .clickable { showInputArea = false }) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            // ★ ここを heightIn に変更！
                            // 内容が少なければ最小(min) 0dp から始まり、
                            // 内容が増えても最大(max) 画面の90%で止まるようになります。
                            .heightIn(
                                min = 0.dp,
                                max = androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp.dp * 0.9f
                            )
                            .align(Alignment.BottomCenter)
                            .clickable(enabled = false) { },
                        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                        color = backColor
                    ) {
                        // navigationBarsPadding() は Surface の内側にかけるのが正解
                        Column(modifier = Modifier.navigationBarsPadding()) {
                            InputFormContent(
                                db = db,
                                columns = columns,
                                inputValues = inputValues,
                                editingRecordId = editingRecordId,
                                onSave = { showInputArea = false; refreshData() },
                                mainText = mainText,
                                subText = subText,
                                isDarkMode = isDarkMode
                            )
                        }
                    }
                }
            }
        }
        if (showOptionDeleteConfirm && pendingDeleteTarget != null) {
            val (targetCol, targetOpt) = pendingDeleteTarget!!

            AlertDialog(onDismissRequest = {
                showOptionDeleteConfirm = false
                pendingDeleteTarget = null
            }, title = { Text("選択肢の削除", color = mainText) }, text = {
                Text(
                    "「${targetOpt}」を削除しますか？\n選択肢を削除しても関連する連動ルールは残ります。",
                    color = mainText
                )
            }, containerColor = Color(0xFF1E1E1E), confirmButton = {
                TextButton(
                    onClick = {
                        // ★ ここで本来の削除処理を実行！
                        scope.launch {
                            val opts = targetCol.options.toMutableList()
                            opts.remove(targetOpt)
                            db.memoDao().updateColumn(targetCol.copy(options = opts))
                            db.memoDao().deleteRulesByTrigger(targetCol.id, targetOpt)
                            refreshData()
                        }
                        showOptionDeleteConfirm = false
                        pendingDeleteTarget = null
                        showOptionMenuName = null // メニューも閉じる
                    }) {
                    Text("削除", color = Color(0xFFF44336))
                }
            }, dismissButton = {
                TextButton(onClick = {
                    showOptionDeleteConfirm = false
                    pendingDeleteTarget = null
                }) {
                    Text("キャンセル", color = mainText)
                }
            })
        }
    }

    @Composable
    fun InputFormContent(
        db: AppDatabase,
        columns: List<ColumnSetting>,
        inputValues: SnapshotStateMap<Int, String>,
        editingRecordId: Int?,
        onSave: () -> Unit,
        // ★ ここから追加
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

                // 項目名の表示
                Text(
                    text = column.name,
                    style = MaterialTheme.typography.labelLarge,
                    color = Color(0xFFBB86FC),
                    modifier = Modifier.padding(top = 8.dp),
                    textAlign = TextAlign.Start
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
                            val bgColor =
                                if (isSelected) Color(0xFF7E57C2) else if (isDarkMode) Color(
                                    0xFF333333
                                ) else Color(0xFFF0F0F0)
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
                }

                // 2. 手入力欄
                if (options.isEmpty() || column.showTextField) {
                    OutlinedTextField(
                        value = currentValue,
                        onValueChange = { inputValues[column.id] = it },
                        placeholder = {
                            if (options.isNotEmpty()) {
                                Text("入力欄", fontSize = 12.sp, color = subText) // ★ プレースホルダーの色
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        // --- ★ ここから色指定を追加 ---
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = mainText,       // 入力中の文字色
                            unfocusedTextColor = mainText,     // 入力していない時の文字色
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            cursorColor = mainText,            // カーソルの色
                            focusedBorderColor = Color(0xFF7E57C2), // フォーカス時の枠線の色（紫など）
                            unfocusedBorderColor = subText     // フォーカス外の枠線の色
                        )
                    )
                }
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
                                            recordId = currentRid, columnId = cid, value = txt
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
                            // scope.launch { ... } だった部分を以下に変更
                            showDeleteConfirmDialog = true // ★ 旗を立てるだけにする
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
            if (showDeleteConfirmDialog) {
                AlertDialog(
                    onDismissRequest = {
                        showDeleteConfirmDialog = false
                    }, // 外側をタップで閉じる
                    title = {
                        Text(text = "削除", color = mainText)
                    }, text = {
                        Text(text = "この行を削除してもよろしいですか？", color = mainText)
                    }, containerColor = Color(0xFF1E1E1E), // ダークモードに合う背景色
                    confirmButton = {
                        TextButton(
                            onClick = {
                                // ★ ここで実際の削除処理を行う
                                scope.launch {
                                    if (editingRecordId != null) {
                                        db.memoDao().deleteValuesByRecordId(editingRecordId)
                                        db.memoDao().deleteRecordById(editingRecordId)
                                    }
                                    showDeleteConfirmDialog = false // ダイアログを閉じる
                                    onSave() // 前の画面に戻る
                                }
                            }) {
                            Text("削除", color = Color(0xFFF44336)) // 削除は赤色に
                        }
                    }, dismissButton = {
                        TextButton(onClick = { showDeleteConfirmDialog = false }) {
                            Text("キャンセル", color = mainText)
                        }
                    })
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
                .clickable { onRowClick() }) {
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
                        textAlign = TextAlign.Center
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