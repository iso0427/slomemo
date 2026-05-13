package iso.slomemo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.material.icons.filled.Edit
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.room.Room
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    fun calculateVisualWidth(text: String): Float {
        var score = 0f
        var i = 0
        while (i < text.length) {
            val codePoint = text.codePointAt(i)

            score += when {
                codePoint <= 128 -> 0.5f       // 半角英数
                codePoint in 0xFF61..0xFF9F -> 0.5f // 半角カナ
                else -> 1.0f                   // 全角・絵文字もここを通るが、1回しか足されない
            }

            i += Character.charCount(codePoint) // 次の「本物の1文字」へ進む
        }
        return score
    }

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

            val navController = rememberNavController()

            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = Color.Black) {
                    // ★ ここが画面遷移の司令塔（NavHost）
                    NavHost(navController = navController, startDestination = "machine_selection") {

                        // ① 機種選択画面
                        composable("machine_selection") {
                            MachineSelectionScreen(db = db, onMachineSelected = { id ->
                                navController.navigate("memo/$id")
                            })
                        }
                        // ② メモ画面（machineId を受け取る）
                        composable(
                            route = "memo/{machineId}",
                            arguments = listOf(navArgument("machineId") { type = NavType.IntType })
                        ) { backStackEntry ->
                            val machineId = backStackEntry.arguments?.getInt("machineId") ?: 0
                            // 次のステップで TestColumnApp を MemoScreen にリネームして呼び出します
                            MemoScreen(db = db, machineId = machineId)
                        }
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
    fun MemoScreen(db: AppDatabase, machineId: Int) { // 名前変更 & 引数追加

        // --- 1. 色の定義 ---
        val backColor = Color.Black
        val surfaceColor = Color(0xFF1e1e1e)
        val mainText = Color.White
        val subText = Color.LightGray
        val dividerColor = Color(0xFF333333)

        // --- 2. 基本的な状態管理 ---
        var currentScreen by remember { mutableStateOf("main") }
        var columns by remember { mutableStateOf(listOf<ColumnSetting>()) }
        var records by remember { mutableStateOf(listOf<MemoRecord>()) }
        var showInputArea by remember { mutableStateOf(false) }
        var menuExpanded by remember { mutableStateOf(false) }
        var showResetConfirmDialog by remember { mutableStateOf(false) }
        var newColumnName by remember { mutableStateOf("") }
        var selectedColumnId by remember { mutableStateOf<Int?>(null) }
        val scope = rememberCoroutineScope()
        val inputValues = remember { mutableStateMapOf<Int, String>() }
        var editingRecordId by remember { mutableStateOf<Int?>(null) }
        var valuesMap by remember { mutableStateOf<Map<Int, List<MemoValue>>>(emptyMap()) }

        // --- 3. アプリ全体設定 (DB) ---
        // 設定変更をリアルタイムに検知するためのFlow
        val appSettingFromFlow by db.memoDao().getSettingFlow()
            .collectAsState(initial = AppSetting())

        // スイッチの状態（初期値はDBから。なければデフォルト）
        var showSimpleCounter by remember { mutableStateOf(true) }
        var showFlashEffect by remember { mutableStateOf(true) }
        var currentAppSetting by remember { mutableStateOf(AppSetting()) }

        // 既存の showTime も appSettingFromFlow から取得するように統一
        val showTime = appSettingFromFlow?.showTime ?: true

        // --- 4. ダイアログ・メニュー制御 ---
        var showConditionEditDialog by remember { mutableStateOf(false) }
        var selectedOptionForRule by remember { mutableStateOf<String?>(null) }
        var selectedColumnIdForRule by remember { mutableStateOf<Int?>(null) }
        var showColumnMenuId by remember { mutableStateOf<Int?>(null) }
        var showOptionMenuName by remember { mutableStateOf<String?>(null) }
        var showSettingsDeleteDialog by remember { mutableStateOf(false) }
        var showOptionDeleteConfirmDialog by remember { mutableStateOf(false) }
        val viewModel: MainViewModel = viewModel()
        var pendingDeleteColumnId by remember { mutableStateOf<Int?>(null) }
        var machineName by remember { mutableStateOf("読み込み中...") }

// --- 5. カウンター設定・演出 ---
        var selectedHue by remember { mutableStateOf(0f) }
        var isMonotone by remember { mutableStateOf(false) }
        var currentColorByLong by remember { mutableStateOf(0xFFBB86FC) }
        var isFlash by remember { mutableStateOf(false) }
        var flashColor by remember { mutableStateOf(Color.White) }
        var showCounterName by remember { mutableStateOf(true) }

        // DBから取得するカウンター項目
        val counterSettings by db.memoDao().getAllCountersFlow()
            .collectAsState(initial = emptyList())
        val currentCounterValues by db.memoDao().getAllCounterValuesFlow()
            .collectAsState(initial = emptyList())

        var newCounterName by remember { mutableStateOf("") }
        var showCounterMenuSetting by remember { mutableStateOf<CounterSetting?>(null) }

        // --- 6. データの読み込みと更新 ---
        LaunchedEffect(Unit) {
            // 設定の読み込み
            val savedSetting = db.memoDao().getAppSetting()
            if (savedSetting != null) {
                currentAppSetting = savedSetting
                showSimpleCounter = savedSetting.showSimpleCounter
                showFlashEffect = savedSetting.showFlashEffect
                showCounterName = savedSetting.showCounterName
            }
        }

// Flowから変更が流れてきたときに変数を同期させる（他画面での変更対策）
        LaunchedEffect(appSettingFromFlow) {
            appSettingFromFlow?.let {
                showSimpleCounter = it.showSimpleCounter
                showFlashEffect = it.showFlashEffect
                currentAppSetting = it
            }
        }

        fun refreshData() {
            scope.launch {
                val machine = db.machineDao().getMachineById(machineId)
                if (machine != null) machineName = machine.name
                columns = db.memoDao().getColumnsByMachineDirect(machineId)
                records = db.memoDao().getRecordsByMachine(machineId)
                valuesMap = db.memoDao().getAllValues().groupBy { it.recordId }
            }
        }

// カラムの重み計算
        val columnWeights = remember(columns, valuesMap) {
            val maxScores = mutableMapOf<Int, Float>()
            valuesMap.values.flatten().forEach { memoValue ->
                val score = calculateVisualWidth(memoValue.value)
                val currentMax = maxScores[memoValue.columnId] ?: 0f
                if (score > currentMax) maxScores[memoValue.columnId] = score
            }
            columns.associate { col ->
                val headerScore = calculateVisualWidth(col.name)
                val contentMaxScore = maxScores[col.id] ?: 0f
                col.id to maxOf(headerScore, contentMaxScore).coerceAtLeast(2.0f)
            }
        }

        LaunchedEffect(Unit) { refreshData() }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backColor) // ★ Color.Black から変更
        ) {
            // 戻るボタンの制御
            BackHandler(enabled = showColumnMenuId != null || showConditionEditDialog || menuExpanded || showInputArea || currentScreen == "settings" || currentScreen == "counter_settings") { // ★一番後ろに条件を追加
                if (showOptionMenuName != null) {
                    showOptionMenuName = null
                } else if (showColumnMenuId != null) {
                    showColumnMenuId = null
                } else if (showConditionEditDialog) {
                    showConditionEditDialog = false
                } else if (menuExpanded) {
                    menuExpanded = false
                } else if (currentScreen == "counter_settings") { // ★ここを追加
                    currentScreen = "main" // カウンター設定からメインに戻る
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
                containerColor = backColor,
                topBar = {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            // ★ ここを修正：isFlash が true ならボタンの色、false なら backColor
                            .background(if (isFlash) flashColor.copy(alpha = 0.5f) else backColor)
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // タイトルの代わりに透明な「隙間」を入れる
                        if (currentScreen == "main") {
                            // メイン画面（一覧）の時は、今まで通り機種名を表示
                            Text(
                                text = machineName,
                                style = MaterialTheme.typography.titleLarge,
                                color = mainText,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                        } else {
                            // 設定画面（全般・カウンター）の時は、何も表示せず「空間」だけ確保する
                            Spacer(modifier = Modifier.weight(1f))
                        }

                        // メイン画面の時だけ操作ボタンを表示
                        if (currentScreen == "main") {
                            // 1. バイブレーション用の変数を定義（まだ定義していなければ）
                            val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current

                            // Undoボタン（元に戻す）
                            IconButton(
                                onClick = {
                                    // 指へのフィードバック
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    // 実行（Flowにより自動で数字が変わるので、これだけでOK）
                                    viewModel.undo()
                                },
                                enabled = viewModel.canUndo.value
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_undo),
                                    contentDescription = "元に戻す",
                                    tint = if (viewModel.canUndo.value) mainText else mainText.copy(
                                        alpha = 0.3f
                                    )
                                )
                            }

                            // Redoボタン（やり直し）
                            IconButton(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    viewModel.redo()
                                },
                                enabled = viewModel.canRedo.value
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_redo),
                                    contentDescription = "やり直し",
                                    tint = if (viewModel.canRedo.value) mainText else mainText.copy(
                                        alpha = 0.3f
                                    )
                                )
                            }

                            // 4. メニューボタン
                            Box {
                                IconButton(
                                    onClick = { menuExpanded = true },
                                    modifier = Modifier.offset(x = 12.dp)
                                ) {
                                    Icon(Icons.Default.Menu, null, tint = mainText)
                                }
                            }
                        }
                    }
                },
                bottomBar = {
                    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
                    if (currentScreen == "main" && !showInputArea && showSimpleCounter) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF1E1E1E))
                                .padding(8.dp)
                                .navigationBarsPadding(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            counterSettings.forEach { setting ->
                                val count by viewModel.dao.getCounterCountFlow(setting.id)
                                    .collectAsState(initial = 0)

                                val buttonColor = Color(setting.color)

                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(4.dp)
                                        .background(
                                            brush = Brush.verticalGradient(
                                                colors = listOf(
                                                    buttonColor,
                                                    buttonColor.copy(alpha = 0.6f)
                                                )
                                            ), shape = RoundedCornerShape(8.dp)
                                        )
                                        .combinedClickable(
                                            onClick = {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                viewModel.updateCounterWithHistory(setting.id, isIncrement = true)
                                                if (showFlashEffect) {
                                                    scope.launch {
                                                        flashColor = buttonColor
                                                        isFlash = true
                                                        delay(50)
                                                        isFlash = false
                                                    }
                                                }
                                            },
                                            onLongClick = {
                                                if ((count ?: 0) > 0) {
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    viewModel.updateCounterWithHistory(setting.id, isIncrement = false)
                                                }
                                            }
                                        )
                                        .padding(vertical = 12.dp), // ★ 名前を消した分、上下の余白を少し増やしてバランス調整
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center // ★ 垂直方向も中央に
                                ) {
                                    // --- 名前表示の Text(setting.name) を完全に削除しました ---

                                    Text(
                                        text = (count ?: 0).toString(),
                                        color = Color(0xFF111111),
                                        fontSize = 28.sp, // ★ 名前がないので少しサイズアップしてもいいかも！
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }
                        }
                    }
                },
                floatingActionButton = {
                    if (currentScreen == "main" && !showInputArea) {

                        FloatingActionButton(
                            onClick = {
                                inputValues.clear(); editingRecordId =
                                null; showInputArea = true
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
                                    .background(Color(0xFF4A4458))
                                    .padding(vertical = 8.dp)
                                    .height(IntrinsicSize.Min), // ★ 縦線を親の高さ（文字の高さ）に合わせるために必須
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (showTime) {
                                    Box(
                                        modifier = Modifier.width(50.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            "時間",
                                            style = androidx.compose.ui.text.TextStyle(
                                                fontSize = 16.sp       // ★ 時間の見出しも大きく
                                            ),
                                            color = mainText
                                        )
                                    }
                                    // --- 時間の横の縦線 ---
                                    Spacer(
                                        modifier = Modifier
                                            .fillMaxHeight() // 親の高さ（IntrinsicSize.Min）に合わせる
                                            .width(1.5.dp)
                                            .background(Color(0xFF777777))
                                    )
                                }

                                columns.forEachIndexed { index, col ->
                                    val weight = columnWeights[col.id] ?: 1.0f

                                    Box(
                                        modifier = Modifier
                                            .weight(weight)
                                            .padding(horizontal = 4.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = col.name,
                                            style = androidx.compose.ui.text.TextStyle(
                                                fontSize = 16.sp       // ★ 項目名も大きく！

                                            ),
                                            color = mainText,
                                            maxLines = 1,
                                            overflow = TextOverflow.Clip
                                        )
                                    }

                                    // --- 項目間の縦線 ---
                                    if (index < columns.size - 1) {
                                        Spacer(
                                            modifier = Modifier
                                                .fillMaxHeight() // 親の高さに合わせる
                                                .width(1.5.dp)
                                                .background(Color(0xFF777777))
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
                                        columnWeights = columnWeights, // ★ ここに追加！
                                        onRowClick = {
                                            scope.launch {
                                                val currentValues =
                                                    db.memoDao()
                                                        .getValuesForRecord(record.id)
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
                    } else if (currentScreen == "settings") {
                        // --- 設定画面 ---
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            Text(
                                "項目・選択肢の設定",
                                style = MaterialTheme.typography.titleLarge,
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
                                Switch(
                                    // ① 表示状態を ViewModel の変数と連動させる
                                    checked = viewModel.showTimeSetting.value,
                                    onCheckedChange = { isChecked ->
                                        // ② スイッチを動かした瞬間に ViewModel の値を更新（これで画面が即座に変わる）
                                        viewModel.showTimeSetting.value = isChecked

                                        // ③ その後、DBにも保存しておく（次回起動時のため）
                                        scope.launch {
                                            db.memoDao()
                                                .updateSetting(AppSetting(showTime = isChecked))
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
                                                        machineId = machineId, // ★これを追加！
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
                                    var showColumnMenu by remember {
                                        mutableStateOf(
                                            false
                                        )
                                    }

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
                                                        onClick = {
                                                            selectedColumnId = col.id
                                                        },
                                                        onLongClick = {
                                                            selectedColumnId = col.id
                                                            showColumnMenuId = col.id
                                                        }
                                                    )
                                                )
                                            },
                                            // ★ ここから追加：Bの設計思想に基づいた色指定
                                            colors = FilterChipDefaults.filterChipColors(
                                                labelColor = mainText,              // 未選択時の文字色（パキッとした白）
                                                selectedContainerColor = Color(
                                                    0xFFEADDFF
                                                ), // 選択時の背景色（紫）
                                                selectedLabelColor = Color.Black    // 選択時の文字色（白）
                                            ),
                                            // 未選択時に枠線が欲しい場合は以下を追加（不要なら削除してOK）
                                            border = FilterChipDefaults.filterChipBorder(
                                                enabled = true,
                                                selected = selectedColumnId == col.id, // ここは本体の状態と合わせるのがComposeの鉄則です
                                                borderColor = Color.Gray,
                                                borderWidth = 1.dp,
                                                selectedBorderColor = Color.Gray,
                                                selectedBorderWidth = 1.dp
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
                                                    val opts =
                                                        col.options.toMutableList()
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
                                        var showOptMenu by remember {
                                            mutableStateOf(
                                                false
                                            )
                                        }

                                        Box {
                                            InputChip(
                                                selected = (showOptionMenuName == opt),
                                                onClick = {
                                                    showOptionMenuName = opt
                                                },
                                                label = { Text(text = opt) },
                                                colors = InputChipDefaults.inputChipColors(
                                                    labelColor = mainText,                 // 未選択：白
                                                    selectedContainerColor = Color(
                                                        0xFFEADDFF
                                                    ), // 選択（メニュー中）：薄紫
                                                    selectedLabelColor = Color.Black
                                                ),
                                                // ★ 画像の定義通りに全ての必須パラメータを埋める
                                                border = InputChipDefaults.inputChipBorder(
                                                    enabled = true,
                                                    selected = selectedColumnId == col.id,
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
                                        pendingDeleteColumnId = col.id
                                        showSettingsDeleteDialog = true
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFFB3261E)
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "「${col.name}」を削除",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                }
                            }  // --- 設定画面の Column 閉じタグ (既存コードの末尾付近) ---
                        } // Columnの終わり

                    } else if (currentScreen == "counter_settings") {
                        // ★ ここが新設する「簡易カウンター専用の設定画面」 ★
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            Text(
                                "簡易カウンターの設定",
                                style = MaterialTheme.typography.titleLarge,
                                color = mainText
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                            // --- ① カウンターを表示するスイッチ ---
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    // 先に padding を書いてから clickable を書く（判定を内側に閉じ込める）
                                    .padding(vertical = 4.dp)
                                    .clickable {
                                        showSimpleCounter = !showSimpleCounter
                                        scope.launch {
                                            db.memoDao().saveAppSetting(
                                                currentAppSetting.copy(showSimpleCounter = showSimpleCounter)
                                            )
                                        }
                                    }
                                    .padding(vertical = 8.dp), // 合計 12dp に調整
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Switch(
                                    checked = showSimpleCounter,
                                    onCheckedChange = null // 二重反応を防ぐため null 推奨
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "カウンターを表示する",
                                    color = mainText,
                                    fontSize = 18.sp
                                )
                            }

                            // --- ② 下のスイッチ群をまとめる Column ---
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .graphicsLayer {
                                        val sat = if (showSimpleCounter) 1f else 0f
                                        val matrix = androidx.compose.ui.graphics.ColorMatrix()
                                            .apply { setToSaturation(sat) }
                                        colorFilter =
                                            androidx.compose.ui.graphics.ColorFilter.colorMatrix(
                                                matrix
                                            )
                                    }
                                    .alpha(if (showSimpleCounter) 1f else 0.4f)
                                // ↓ pointerInput を少し下にずらすか、Row単位で制御する方が安全です
                            ) {
                                // 1. フラッシュ設定のスイッチ
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .clickable(enabled = showSimpleCounter) { // 無効時はクリックさせない
                                            showFlashEffect = !showFlashEffect
                                            scope.launch {
                                                db.memoDao().saveAppSetting(
                                                    currentAppSetting.copy(showFlashEffect = showFlashEffect)
                                                )
                                            }
                                        }
                                        .padding(vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Switch(
                                        checked = showFlashEffect,
                                        onCheckedChange = null,
                                        enabled = showSimpleCounter
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = "タップ時にフラッシュさせる",
                                        color = mainText,
                                        fontSize = 18.sp
                                    )
                                }

                                // 2. 項目名表示のスイッチ (★ここを追加)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .clickable(enabled = showSimpleCounter) {
                                            showCounterName = !showCounterName
                                            scope.launch {
                                                db.memoDao().saveAppSetting(
                                                    currentAppSetting.copy(showCounterName = showCounterName)
                                                )
                                            }
                                        }
                                        .padding(vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Switch(
                                        checked = showCounterName,
                                        onCheckedChange = null,
                                        enabled = showSimpleCounter
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = "項目名を表示する",
                                        color = mainText,
                                        fontSize = 18.sp
                                    )
                                }

                                // --- カウンター項目の管理セクション ---
                                Spacer(modifier = Modifier.height(24.dp))
                                Text(
                                    "カウンター項目の編集",
                                    color = mainText,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(12.dp))

                                // --- カウンター項目の管理セクション ---
                                // 1. 新しい項目を追加する入力欄（ここから Row を Column に書き換えます）
                                Column(
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    OutlinedTextField(
                                        value = newCounterName,
                                        onValueChange = { newCounterName = it },
                                        label = {
                                            Text(
                                                "ボタン名 (例: ぶどう)",
                                                color = Color.Gray
                                            )
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = mainText,
                                            unfocusedTextColor = mainText,
                                            focusedBorderColor = Color(0xFFBB86FC)
                                        )
                                    )

                                    Spacer(modifier = Modifier.height(16.dp))

                                    // カラーパレット
                                    Text("ボタンの色", color = mainText, fontSize = 14.sp)

                                    // 原色選択
                                    val baseHues = listOf(
                                        0f,
                                        30f,
                                        60f,
                                        90f,
                                        120f,
                                        150f,
                                        180f,
                                        210f,
                                        240f,
                                        270f,
                                        300f,
                                        330f
                                    )
                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.padding(vertical = 12.dp)
                                    ) {
                                        // --- 白黒グラデーションボタン ---
                                        item {
                                            Box(
                                                modifier = Modifier
                                                    .size(40.dp)
                                                    .background(
                                                        brush = Brush.linearGradient(
                                                            listOf(
                                                                Color.White, Color.Gray, Color.Black
                                                            )
                                                        ), shape = RoundedCornerShape(4.dp)
                                                    )
                                                    .border(
                                                        width = if (isMonotone) 3.dp else 0.dp,
                                                        color = Color.White,
                                                        shape = RoundedCornerShape(4.dp)
                                                    )
                                                    .clickable {
                                                        isMonotone = true // 白黒モードON
                                                    }
                                            )
                                        }

                                        // --- 通常の色のボタン ---
                                        items(baseHues) { hue ->
                                            val isSelected =
                                                !isMonotone && selectedHue == hue // 「白黒モードでない」かつ「色が一致」
                                            Box(
                                                modifier = Modifier
                                                    .size(40.dp)
                                                    .background(
                                                        Color.hsl(hue, 0.8f, 0.5f),
                                                        RoundedCornerShape(4.dp)
                                                    )
                                                    .border(
                                                        width = if (isSelected) 3.dp else 0.dp,
                                                        color = Color.White,
                                                        shape = RoundedCornerShape(4.dp)
                                                    )
                                                    .clickable {
                                                        isMonotone = false // 白黒モードを解除！
                                                        selectedHue = hue
                                                    }
                                            )
                                        }
                                    }

                                    // 濃淡選択（グリッド）
                                    val lightnessLevels =
                                        listOf(0.9f, 0.8f, 0.7f, 0.6f, 0.5f, 0.4f, 0.3f, 0.2f)
                                    LazyVerticalGrid(
                                        columns = GridCells.Fixed(4), // 4列にして押しやすく
                                        modifier = Modifier.height(160.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        items(lightnessLevels) { level ->
                                            // isMonotone が true のときは彩度(saturation)を 0 にして白黒にする
                                            val colorVariant = if (isMonotone) {
                                                Color.hsl(0f, 0f, level)
                                            } else {
                                                Color.hsl(selectedHue, 0.7f, level)
                                            }

                                            val colorLong = colorVariant.toArgb().toLong()
                                            val isSelected = currentColorByLong == colorLong

                                            Box(
                                                modifier = Modifier
                                                    .aspectRatio(1.5f)
                                                    .background(
                                                        colorVariant, RoundedCornerShape(4.dp)
                                                    )
                                                    .border(
                                                        width = if (isSelected) 3.dp else 0.dp,
                                                        color = Color.White,
                                                        shape = RoundedCornerShape(4.dp)
                                                    )
                                                    .clickable {
                                                        currentColorByLong = colorLong
                                                    }
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))

                                    // 追加ボタン（Columnの中なので横幅いっぱいに）
                                    Button(
                                        onClick = {
                                            if (newCounterName.isNotBlank()) {
                                                scope.launch {
                                                    db.memoDao().insertCounter(
                                                        CounterSetting(
                                                            name = newCounterName,
                                                            displayOrder = counterSettings.size,
                                                            color = currentColorByLong
                                                        )
                                                    )
                                                    newCounterName = ""
                                                }
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(
                                                0xFFBB86FC
                                            )
                                        )
                                    ) {
                                        Text(
                                            "この色で追加",
                                            color = Color.Black,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                // 2. 登録済みの一覧を表示（削除も可能）
                                Text(
                                    "現在のボタン一覧 (タップで削除)",
                                    color = mainText,
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                // 2. 登録済みの一覧を表示（削除も可能）
                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    counterSettings.forEach { setting ->
                                        InputChip(
                                            selected = false,
                                            onClick = {
                                                // ★ 直接削除せず、メニューを開くように変更
                                                showCounterMenuSetting = setting
                                            },
                                            label = { Text(setting.name, color = Color.Black) },
                                            // ✕ボタン（trailingIcon）はあってもなくても良いですが、
                                            // メニューを開くことがわかるように設定
                                            trailingIcon = {
                                                Icon(
                                                    Icons.Default.Edit,
                                                    null,
                                                    modifier = Modifier.size(16.dp),
                                                    tint = Color.Black
                                                )
                                            },

                                            colors = InputChipDefaults.inputChipColors(
                                                // ★ ここでDBに保存した色（setting.color）を背景色に指定します
                                                containerColor = Color(setting.color),
                                                // 選択されていない時のラベル色なども必要に応じて
                                                labelColor = Color.Black
                                            ),
                                            // 枠線が不要なら border を null にするか、色を合わせる
                                            border = InputChipDefaults.inputChipBorder(
                                                borderColor = Color(setting.color),
                                                enabled = true,
                                                selected = false
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (showOptionDeleteConfirmDialog) {
                        // 削除対象のデータを確認
                        val colId = selectedColumnId
                        val optToRemove = showOptionMenuName

                        if (colId != null && optToRemove != null) {
                            val col = columns.find { it.id == colId }

                            if (col != null) {
                                AlertDialog(
                                    onDismissRequest = {
                                        showOptionDeleteConfirmDialog = false
                                    },
                                    title = {
                                        Text(
                                            text = "選択肢の削除",
                                            color = mainText
                                        )
                                    },
                                    text = {
                                        Text(
                                            text = "「$optToRemove」を削除しますか？",
                                            color = mainText
                                        )
                                    },
                                    containerColor = surfaceColor,
                                    confirmButton = {
                                        TextButton(onClick = {
                                            scope.launch {
                                                val opts =
                                                    col.options.toMutableList()
                                                opts.remove(optToRemove)
                                                db.memoDao()
                                                    .updateColumn(col.copy(options = opts))

                                                db.memoDao()
                                                    .deleteRulesByTrigger(
                                                        col.id,
                                                        optToRemove
                                                    )

                                                refreshData()
                                                showOptionDeleteConfirmDialog =
                                                    false
                                                showOptionMenuName = null
                                            }
                                        }) {
                                            Text("削除", color = Color(0xFFF44336))
                                        }
                                    },
                                    dismissButton = {
                                        TextButton(onClick = {
                                            showOptionDeleteConfirmDialog = false
                                        }) {
                                            Text("キャンセル", color = mainText)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // --- カウンター操作メニュー (カウンター設定用) ---
            if (showCounterMenuSetting != null) {
                androidx.activity.compose.BackHandler {
                    showCounterMenuSetting = null
                }
                val setting = showCounterMenuSetting!!
                // counterSettings内での現在のインデックスを取得（並び替え判定用）
                val currentIndex = counterSettings.indexOf(setting)

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White.copy(alpha = 0.2f)) // 元のコードと同じ白透過
                        .clickable { showCounterMenuSetting = null },
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        shadowElevation = 8.dp,
                        color = Color(0xFF252525), // ダイアログの背景色
                        modifier = Modifier
                            .width(180.dp) // ★ 参照元と全く同じサイズ
                            .clickable(enabled = false) { }
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // 現在のボタンの色プレビュー（アイコンの代わり）
                            Box(
                                modifier = Modifier
                                    .size(24.dp) // アイコンに近いサイズ
                                    .background(Color(setting.color), RoundedCornerShape(4.dp))
                                    .border(
                                        1.dp,
                                        Color.White.copy(alpha = 0.3f),
                                        RoundedCornerShape(4.dp)
                                    )
                            )

                            Text(
                                text = "「${setting.name}」",
                                fontSize = 18.sp, // ★ 参照元のサイズ維持
                                fontWeight = FontWeight.Bold,
                                color = mainText,
                                modifier = Modifier.padding(top = 8.dp, bottom = 16.dp)
                            )

                            // 移動ボタンの配色定義（文字色は常に黒）
                            val canMoveColors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFBBBBBB), // 移動可
                                contentColor = Color.Black          // 文字：黒
                            )
                            val cannotMoveColors = ButtonDefaults.buttonColors(
                                disabledContainerColor = Color(0xFF333333), // 移動不可
                                disabledContentColor = Color.Black          // 文字：黒
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // 左へ移動ボタン
                                Button(
                                    onClick = {
                                        // TODO: ここに入れ替えロジックを実装予定
                                    },
                                    enabled = currentIndex > 0,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(60.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(0.dp),
                                    colors = if (currentIndex > 0) canMoveColors else cannotMoveColors
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(Icons.Default.ArrowBack, null, tint = Color.Black)
                                        Text("左へ", fontSize = 16.sp, color = Color.Black)
                                    }
                                }

                                // 右へ移動ボタン
                                Button(
                                    onClick = {
                                        // TODO: ここに入れ替えロジックを実装予定
                                    },
                                    enabled = currentIndex < counterSettings.size - 1,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(60.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(0.dp),
                                    colors = if (currentIndex < counterSettings.size - 1) canMoveColors else cannotMoveColors
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(Icons.Default.ArrowForward, null, tint = Color.Black)
                                        Text("右へ", fontSize = 16.sp, color = Color.Black)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // --- 下段ボタン ---
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // 戻るボタン（条件編集の枠を利用して見た目を再現）
                                Button(
                                    onClick = { showCounterMenuSetting = null },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(60.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(0.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF6750A4), // 紫
                                        contentColor = Color.White
                                    )
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(Icons.Default.Build, null, tint = Color.White)
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text("戻る", color = Color.White, fontSize = 16.sp)
                                    }
                                }

                                // 削除ボタン
                                Button(
                                    onClick = {
                                        scope.launch {
                                            db.memoDao().deleteCounter(setting)
                                            db.memoDao().deleteCounterValueById(setting.id)
                                            showCounterMenuSetting = null
                                        }
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(60.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(0.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFFB3261E), // 赤
                                        contentColor = Color.White
                                    )
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(Icons.Default.Delete, null, tint = Color.White)
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text("削除", color = Color.White, fontSize = 16.sp)
                                    }
                                }
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
                        .background(Color.White.copy(alpha = 0.2f))
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
                        color = surfaceColor,
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
                                    .padding(
                                        horizontal = 16.dp, vertical = 16.dp
                                    ), // ★ 高さを少しだけ広げて押しやすく
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
                            // --- 簡易カウンターの設定ボタン ---
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        currentScreen = "counter_settings" // ここで新しい画面の名前を指定
                                        menuExpanded = false               // メニューを閉じる
                                    }
                                    .padding(horizontal = 16.dp, vertical = 16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Build,
                                    contentDescription = null,
                                    modifier = Modifier.size(22.dp),
                                    tint = mainText
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "簡易カウンターの設定",
                                    fontSize = 18.sp,
                                    color = mainText,
                                    maxLines = 1,      // 改行禁止
                                    softWrap = false   // 縦書き防止
                                )
                            }
                            // ★ ここに追加：項目3：メモをリセット
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        // リセット確認ダイアログを表示する
                                        showResetConfirmDialog = true
                                        menuExpanded = false
                                    }
                                    .padding(horizontal = 16.dp, vertical = 16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = null,
                                    modifier = Modifier.size(22.dp),
                                    tint = mainText
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "メモをリセット",
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
                        .background(Color.White.copy(alpha = 0.2f))
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
                                                db.memoDao()
                                                    .updateColumn(c.copy(displayOrder = i))
                                            }
                                            refreshData()
                                        }
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
                                        Icon(
                                            Icons.Default.ArrowBack,
                                            null,
                                            tint = Color.Black
                                        )
                                        Text(
                                            "左へ",
                                            fontSize = 16.sp,
                                            color = Color.Black
                                        )
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
                                                db.memoDao()
                                                    .updateColumn(c.copy(displayOrder = i))
                                            }
                                            refreshData()
                                        }
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
                                        Icon(
                                            Icons.Default.ArrowForward,
                                            null,
                                            tint = Color.Black
                                        )
                                        Text(
                                            "右へ",
                                            fontSize = 16.sp,
                                            color = Color.Black
                                        )
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

                androidx.compose.ui.window.Dialog(
                    onDismissRequest = { showConditionEditDialog = false },
                    properties = DialogProperties(
                        usePlatformDefaultWidth = false, // これをfalseにする
                        decorFitsSystemWindows = false
                    )
                ) {
                    // 画面全体を覆うBoxを自分で作ることで、背景色を自由にする
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.White.copy(alpha = 0.2f)) // ← ここで好きな色と透明度を指定！
                            .clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() }) {
                                showConditionEditDialog = false
                            },
                        contentAlignment = Alignment.Center
                    ) {
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
                                        val targetName =
                                            columns.find { it.id == rule.targetColumnId }?.name
                                                ?: "不明"
                                        val timingStr =
                                            if (rule.isNextRow) "次の行" else "同じ行"

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
                                                Icon(
                                                    Icons.Default.Close,
                                                    null,
                                                    tint = Color(0xFFCF6679)
                                                ) // 少し抑えた赤
                                            }
                                        }
                                    }
                                    Divider(
                                        modifier = Modifier.padding(vertical = 8.dp),
                                        color = subText.copy(alpha = 0.3f)
                                    )
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
                                        val isConfigured =
                                            localRules.any { it.targetColumnId == c.id }
                                        FilterChip(
                                            selected = targetColId == c.id,
                                            onClick = {
                                                targetColId = c.id; targetValue = ""
                                            },
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
                                                selected = selectedColumnId == targetColId,
                                                borderColor = if (isConfigured) Color(
                                                    0xFFBB86FC
                                                ) else Color.Gray,
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

                                    // ★ ここを修正：本来の選択肢の後ろに "━" を追加する
                                    val baseOpts = columns.find { it.id == targetColId }?.options
                                        ?: emptyList()
                                    val uiOpts = baseOpts + listOf("━") // + の位置を後ろに入れ替え

                                    FlowRow(modifier = Modifier.fillMaxWidth()) {
                                        // ★ opts ではなく uiOpts を使う
                                        uiOpts.forEach { opt ->
                                            FilterChip(
                                                selected = targetValue == opt,
                                                onClick = { targetValue = opt },
                                                label = { Text(opt) },
                                                colors = FilterChipDefaults.filterChipColors(
                                                    labelColor = mainText,              // 通常時：白
                                                    selectedContainerColor = Color(
                                                        0xFFEADDFF
                                                    ), // 選択時：薄紫
                                                    selectedLabelColor = Color.Black    // 選択時：黒
                                                ),
                                                border = FilterChipDefaults.filterChipBorder(
                                                    //★ 非選択時も選択時も、枠線はピンクで固定
                                                    enabled = true,
                                                    selected = selectedColumnId == targetColId,
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
                                                localRules.forEach {
                                                    db.memoDao().insertRule(it)
                                                }
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
                            .background(Color.White.copy(alpha = 0.2f))
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
                                        onClick = {
                                            scope.launch {
                                                val opts = col.options.toMutableList()
                                                // 現在のインデックス(optIndex)とその一つ前(optIndex - 1)を入れ替える
                                                val targetIndex = optIndex - 1
                                                if (targetIndex >= 0) {
                                                    val temp = opts[optIndex]
                                                    opts[optIndex] = opts[targetIndex]
                                                    opts[targetIndex] = temp

                                                    // DBを更新
                                                    db.memoDao()
                                                        .updateColumn(col.copy(options = opts))
                                                    refreshData()
                                                }
                                            }
                                        },
                                        enabled = optIndex > 0,
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(60.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        contentPadding = PaddingValues(0.dp),
                                        colors = if (optIndex > 0) canMoveColors else cannotMoveColors
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Icon(
                                                Icons.Default.ArrowBack,
                                                null,
                                                tint = Color.Black
                                            )

                                            Text(
                                                "左へ",
                                                fontSize = 16.sp,
                                                color = Color.Black
                                            )
                                        }
                                    }

                                    // 右へ移動ボタン
                                    Button(
                                        onClick = {
                                            scope.launch {
                                                val opts = col.options.toMutableList()
                                                val targetIndex = optIndex + 1
                                                if (targetIndex < opts.size) {
                                                    val temp = opts[optIndex]
                                                    opts[optIndex] = opts[targetIndex]
                                                    opts[targetIndex] = temp

                                                    db.memoDao()
                                                        .updateColumn(col.copy(options = opts))
                                                    refreshData()
                                                }
                                            }
                                        },
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

                                            Text(
                                                "右へ",
                                                fontSize = 16.sp,
                                                color = Color.Black
                                            )
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
                                            .height(60.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        contentPadding = PaddingValues(0.dp),
                                        // ★ MachineActionDialog と同じ紫に変更
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFF6750A4),
                                            contentColor = Color.White
                                        )
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Icon(
                                                Icons.Default.Build,
                                                null,
                                                tint = Color.White,
                                                // modifier = Modifier.size(20.dp)
                                            )

                                            Spacer(modifier = Modifier.height(2.dp))

                                            Text(
                                                "条件編集",
                                                color = Color.White,
                                                fontSize = 16.sp, // ★ MachineActionDialog と同じ 16.sp に変更
                                                maxLines = 1,
                                                softWrap = false,
                                                overflow = TextOverflow.Visible
                                            )
                                        }
                                    }

                                    // 2. 削除ボタン
                                    Button(
                                        onClick = {
                                            showOptionDeleteConfirmDialog = true
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(60.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        contentPadding = PaddingValues(0.dp),
                                        // ★ MachineActionDialog と同じ赤に変更
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFFB3261E),
                                            contentColor = Color.White
                                        )
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Icon(
                                                Icons.Default.Delete,
                                                null,
                                                tint = Color.White,
                                                // modifier = Modifier.size(20.dp)
                                            )

                                            Spacer(modifier = Modifier.height(2.dp))

                                            Text(
                                                "削除",
                                                color = Color.White,
                                                fontSize = 16.sp, // ★ MachineActionDialog と同じ 16.sp に変更
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
                                machineId = machineId,
                                db = db,
                                viewModel = viewModel,
                                columns = columns,
                                inputValues = inputValues,
                                editingRecordId = editingRecordId,
                                // true と書かずに、viewModelが持っている設定値を渡す
                                showTime = viewModel.showTimeSetting.value,
                                onSave = { showInputArea = false; refreshData() },
                                mainText = Color.White,
                                subText = Color.LightGray,
                                isDarkMode = true
                            )
                        }
                    }
                }
            }
            if (showSettingsDeleteDialog) {
                val colToDelete = columns.find { it.id == pendingDeleteColumnId }
                if (colToDelete != null) {
                    AlertDialog(
                        onDismissRequest = {
                            showSettingsDeleteDialog = false
                            pendingDeleteColumnId = null
                        },
                        title = { Text(text = "項目の削除", color = mainText) },
                        text = {
                            Text(
                                text = "「${colToDelete.name}」を削除してもよろしいですか？\nこの項目に含まれるすべてのデータも削除されます。",
                                color = mainText
                            )
                        },
                        containerColor = surfaceColor,
                        confirmButton = {
                            TextButton(onClick = {
                                scope.launch {
                                    db.memoDao().deleteColumn(colToDelete)
                                    refreshData()
                                    showSettingsDeleteDialog = false
                                    pendingDeleteColumnId = null
                                }
                            }) {
                                Text("削除", color = Color(0xFFF44336))
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = {
                                showSettingsDeleteDialog = false
                                pendingDeleteColumnId = null
                            }) {
                                Text("キャンセル", color = mainText)
                            }
                        }
                    )
                }
            }
            if (showResetConfirmDialog) {
                AlertDialog(
                    onDismissRequest = { showResetConfirmDialog = false },
                    title = { Text(text = "メモをリセット", color = mainText) },
                    text = {
                        Text(
                            text = "すべてのメモを削除しますか？",
                            color = mainText
                        )
                    },
                    containerColor = surfaceColor,
                    confirmButton = {
                        TextButton(onClick = {
                            // ★修正：scope.launch と delay を使って、削除を待ってから画面を更新する
                            scope.launch {
                                viewModel.resetAllMemosWithHistory(machineId) // ★ machineId を渡す
                                kotlinx.coroutines.delay(150) // 0.15秒待ってDB書き込みを確実に待機
                                refreshData()
                                showResetConfirmDialog = false
                            }
                        }) {
                            Text("リセット", color = Color(0xFFF44336))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showResetConfirmDialog = false }) {
                            Text("キャンセル", color = mainText)
                        }
                    }
                )
            }
        } // ← これが Box の閉じカッコ
    } // ← これが TestColumnApp 関数の閉じカッコ

    @Composable
    fun InputFormContent(
        machineId: Int,
        db: AppDatabase,
        viewModel: MainViewModel,
        columns: List<ColumnSetting>,
        inputValues: SnapshotStateMap<Int, String>,
        editingRecordId: Int?,
        showTime: Boolean,
        onSave: () -> Unit,
        mainText: Color,
        subText: Color,
        isDarkMode: Boolean
    ) {
        val scope = rememberCoroutineScope()
        var showDeleteConfirmDialog by remember { mutableStateOf(false) }

        // 1. プレビュー用のデータ作成（今の入力内容を反映）
        // inputValues.toMap() を remember の鍵にすることで、入力のたびにプレビューが更新されます
        val previewValues = remember(inputValues.toMap()) {
            columns.map { col ->
                MemoValue(
                    recordId = editingRecordId ?: 0,
                    columnId = col.id,
                    value = inputValues[col.id] ?: ""
                )
            }
        }

        // 2. プレビュー用の「幅」を計算（一覧と同じロジック）
        val columnWeights = remember(columns, previewValues) {
            val maxScores = mutableMapOf<Int, Float>()
            previewValues.forEach { memoValue ->
                // ★ もし calculateVisualWidth で赤線が出るなら viewModel.calculateVisualWidth に書き換えてみてください
                val score = calculateVisualWidth(memoValue.value)
                val currentMax = maxScores[memoValue.columnId] ?: 0f
                if (score > currentMax) maxScores[memoValue.columnId] = score
            }
            columns.associate { col ->
                val headerScore = calculateVisualWidth(col.name)
                val contentMaxScore = maxScores[col.id] ?: 0f
                col.id to maxOf(headerScore, contentMaxScore).coerceAtLeast(2.0f)
            }
        }

        // 3. プレビュー用のダミーレコード
        val previewRecord = remember(editingRecordId) {
            MemoRecord(
                id = editingRecordId ?: 0,
                machineId = machineId,
                timestamp = System.currentTimeMillis()
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp) // 全体の余白
        ) {
            // --- 【固定エリア】タイトルとプレビュー ---
            Text(
                text = if (editingRecordId != null) "メモを編集" else "新規メモ入力",
                style = MaterialTheme.typography.headlineSmall,
                color = mainText
            )

            Spacer(modifier = Modifier.height(8.dp))

            // --- プレビューエリア ---
            Text(text = "プレビュー", fontSize = 14.sp, color = Color(0xFFBB86FC))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                HistoryRow(
                    db = db,
                    record = previewRecord,
                    columns = columns,
                    values = previewValues,
                    showTime = showTime,
                    columnWeights = columnWeights,
                    onRowClick = {},
                    onDelete = {},
                    mainText = mainText,
                    subText = subText,
                    dividerColor = Color.Gray
                )
            }

            // Spacer(modifier = Modifier.height(16.dp))
            // Divider(color = Color.DarkGray, thickness = 1.dp)
            // ------------------------------------------

            Column(
                modifier = Modifier
                    .weight(1f) // これでプレビュー以外の隙間を全部埋める
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()) // ここだけでスクロールさせる
            ) {
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
                                val bgColor =
                                    if (isSelected) Color(0xFF7E57C2) else Color(0xFF333333)
                                val textColor = if (isSelected) Color.White else mainText

                                Surface(
                                    onClick = {
                                        val oldValue = currentValue // 変更前の値を覚えとく
                                        val newValue = if (isSelected) "" else option
                                        inputValues[column.id] = newValue

                                        scope.launch {
                                            // 1. 【打ち消し】前の値(oldValue)で発動していた連動をクリアする
                                            if (oldValue.isNotBlank()) {
                                                val oldRules = db.memoDao()
                                                    .getRulesByTrigger(column.id, oldValue)
                                                oldRules.forEach { rule ->
                                                    // 「同じ行」かつ「連動先が自分以外」なら、一旦空にする
                                                    if (!rule.isNextRow && rule.targetColumnId != column.id) {
                                                        inputValues[rule.targetColumnId] = ""
                                                    }
                                                }
                                            }

                                            // 2. 【発動】新しい値(newValue)で連動を上書きする
                                            if (newValue.isNotBlank()) {
                                                val newRules = db.memoDao()
                                                    .getRulesByTrigger(column.id, newValue)
                                                newRules.forEach { rule ->
                                                    if (!rule.isNextRow && rule.targetColumnId != column.id) {
                                                        inputValues[rule.targetColumnId] =
                                                            rule.targetValue
                                                    }
                                                }
                                            }
                                        }
                                    },
                                    // ... (以下略)
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
                                        Text(
                                            text = option,
                                            color = textColor,
                                            fontSize = 14.sp
                                        )
                                    }
                                }
                            }
                        }
                    } // ★if の閉じ

                    // 2. 手入力欄
                    if (options.isEmpty() || column.showTextField) {
                        OutlinedTextField(
                            value = currentValue,
                            onValueChange = { newValue ->
                                inputValues[column.id] = newValue

                                // --- ここから連動処理 ---
                                scope.launch {
                                    val rules = db.memoDao().getRulesByTrigger(column.id, newValue)
                                    rules.forEach { rule ->
                                        if (!rule.isNextRow && rule.targetColumnId != column.id) {
                                            inputValues[rule.targetColumnId] = rule.targetValue
                                        }
                                    }
                                }
                                // --- ここまで ---
                            },
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
                            // 1. IDと時刻の確定
                            val currentRid: Int
                            val currentTimestamp: Long

                            if (editingRecordId != null) {
                                // 【編集の場合】既存のレコードを取得して、その「時刻」をそのまま使う
                                val existingRecord =
                                    db.memoDao().getRecordById(editingRecordId)
                                currentRid = editingRecordId
                                currentTimestamp =
                                    existingRecord?.timestamp
                                        ?: System.currentTimeMillis()
                            } else {
                                // 【新規の場合】新しいレコードを作成
                                currentTimestamp = System.currentTimeMillis()
                                currentRid = db.memoDao()
                                    .insertRecord(
                                        MemoRecord(
                                            machineId = machineId, // ★ ここで使う！
                                            timestamp = currentTimestamp
                                        )
                                    ).toInt()
                            }

                            // 2. データの保存
                            val newValues =
                                inputValues.filter { it.value.isNotBlank() }
                                    .map { (cid, txt) ->
                                        MemoValue(
                                            recordId = currentRid,
                                            columnId = cid,
                                            value = txt
                                        )
                                    }

                            if (editingRecordId != null) {
                                // 編集時：時刻を維持したままアップデート
                                viewModel.updateMemoWithHistory(
                                    MemoRecord(
                                        id = currentRid,
                                        machineId = machineId,
                                        timestamp = currentTimestamp
                                    ), // ★時刻を維持！
                                    newValues
                                )
                            } else {
                                // 新規時：そのまま保存
                                newValues.forEach { db.memoDao().insertValue(it) }
                            }

                            // --- 3. 連動チェック（AutoInputRule） ---
                            // 編集・新規どちらの場合も、入力された値をもとに連動を走らせる
                            inputValues.forEach { (cid, txt) ->
                                val rules = db.memoDao().getRulesByTrigger(cid, txt)
                                rules.forEach { rule ->
                                    if (rule.isNextRow) {
                                        val allRecords = db.memoDao().getRecordsByMachine(machineId)
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
                                            val newNextRid = db.memoDao().insertRecord(
                                                MemoRecord(machineId = machineId)
                                            )
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

                            // --- 4. 仕上げ ---
                            delay(150) // DBへの書き込み完了を少し待つ
                            onSave()
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(
                            0xFF7E57C2
                        )
                    )
                ) {
                    Text(
                        if (editingRecordId != null) "変更を保存" else "メモに追加",
                        color = mainText,
                        fontSize = 20.sp
                    )
                }

                if (editingRecordId != null) {
                    Button(
                        onClick = { showDeleteConfirmDialog = true },
                        modifier = Modifier.size(44.dp),
                        shape = RoundedCornerShape(22.dp),
                        contentPadding = PaddingValues(0.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(
                                0xFFB3261E
                            )
                        )
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "削除",
                            tint = Color.White
                        )
                    }
                }
            }

            if (showDeleteConfirmDialog) {
                AlertDialog(
                    onDismissRequest = { showDeleteConfirmDialog = false },
                    title = { Text(text = "削除", color = mainText) },
                    text = { Text(text = "この行を削除しますか？", color = mainText) },
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
        columnWeights: Map<Int, Float>,
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
                    .height(IntrinsicSize.Min) // 縦線を出すために必須
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (showTime) {
                    val timeText =
                        java.text.SimpleDateFormat(
                            "HH:mm",
                            java.util.Locale.getDefault()
                        )
                            .format(record.timestamp)
                    Text(
                        text = timeText,
                        modifier = Modifier.width(50.dp),
                        style = androidx.compose.ui.text.TextStyle(
                            fontSize = 16.sp, // ★12sp前後から18spへ
                        ),
                        color = mainText,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )

                    // ★ 縦線1：時間とデータの境目
                    Spacer(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(1.5.dp) // 少し太くしました
                            .background(dividerColor) // 透過なし！
                    )
                }

                columns.forEachIndexed { index, col ->
                    val value = values.find { it.columnId == col.id }?.value ?: ""
                    val weight = columnWeights[col.id] ?: 1.0f
                    // ★ println を Log.d に書き換え（タグを付ける）
                    android.util.Log.d("SloMemoDebug", "列名: ${col.name}, weight: $weight")
                    Box(
                        modifier = Modifier
                            .weight(weight)
                            .padding(horizontal = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = value,
                            style = androidx.compose.ui.text.TextStyle(
                                fontSize = 16.sp, // ★ここをガツンと大きく！
                            ),
                            color = mainText,
                            maxLines = 1,
                            overflow = TextOverflow.Clip
                        )
                    }

                    // ★ 縦線2：項目ごとの区切り線
                    if (index < columns.size - 1) {
                        Spacer(
                            modifier = Modifier
                                .fillMaxHeight()
                                .width(1.5.dp) // 少し太くしました
                                .background(dividerColor) // 透過なし！
                        )
                    }
                }
            }
            Divider(color = dividerColor, thickness = 1.dp) // 横線
        }
    }
}