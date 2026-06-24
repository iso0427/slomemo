package iso.slomemo

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SubdirectoryArrowLeft
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.LocalMinimumInteractiveComponentEnforcement
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.room.Room
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// 🟢 どこにも囲まれていない「外側」のここにポツンと貼り付けます！
val SevenSegmentFontFamily = FontFamily(
    Font(R.font.dseg7classic_bold, FontWeight.Normal)
)

class MainActivity : ComponentActivity() {

    private var onOverlayNavigationRequested: ((Int) -> Unit)? = null

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
        installSplashScreen()
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

            // 🟢 修正：アプリが完全に終了している状態からの起動でも、一発で指定のメモ画面を開く
            LaunchedEffect(Unit) {
                // 1. アプリ起動後に「📄」が押された時のリアルタイム受け取り口
                onOverlayNavigationRequested = { id ->
                    navController.navigate("memo/$id") {
                        popUpTo("machine_selection") { inclusive = true }
                    }
                }

                // 2. アプリが完全に終了した状態から「📄」で新しく起動した時の直接チェック
                val startupIntent = (view.context as? android.app.Activity)?.intent
                if (startupIntent?.action == "ACTION_OPEN_MEMO_FROM_OVERLAY") {
                    val targetId = startupIntent.getIntExtra("TARGET_MACHINE_ID", -1)
                    if (targetId != -1) {
                        navController.navigate("memo/$targetId") {
                            popUpTo("machine_selection") { inclusive = true }
                        }
                    }
                }
            }

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
                            MemoScreen(
                                db = db,
                                machineId = machineId,
                                navController = navController
                            )
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
    fun MemoScreen(db: AppDatabase, machineId: Int, navController: NavController) {

        // --- 1. 色の定義 ---
        val backColor = Color.Black
        val context = androidx.compose.ui.platform.LocalContext.current

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

        // --- 3. 画面の切り替えを監視して、サービスへ確実にON/OFFを送る ---
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route

        // 共通の判定・送信処理を関数化
        val updateOverlayVisibility = {
            val prefs =
                context.getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
            val isRunning = prefs.getBoolean("overlay_running", false)
            if (isRunning) {
                val isAtMemoRoute = currentRoute?.startsWith("memo/") == true
                val isPureMemoScreen = isAtMemoRoute && (currentScreen == "main")

                if (isPureMemoScreen) {
                    val hideIntent = android.content.Intent(context, OverlayService::class.java)
                        .apply { action = "ACTION_HIDE_OVERLAY" }
                    context.startService(hideIntent)
                } else {
                    val showIntent = android.content.Intent(context, OverlayService::class.java)
                        .apply { action = "ACTION_SHOW_OVERLAY" }
                    context.startService(showIntent)
                }
            }
        }

        // 1. アプリ内での画面切り替えを監視
        androidx.compose.runtime.LaunchedEffect(currentRoute, currentScreen) {
            updateOverlayVisibility()
        }

        // 2. 🟢 ホーム画面から「アプリに戻ってきた瞬間」に表示状態を正しく再判定する
        androidx.lifecycle.compose.LifecycleEventEffect(androidx.lifecycle.Lifecycle.Event.ON_START) {
            updateOverlayVisibility()
        }

        // 3. 🟢 アプリから離れて「ホーム画面に戻った瞬間」は、どこにいても必ずカウンターを再表示する
        androidx.lifecycle.compose.LifecycleEventEffect(androidx.lifecycle.Lifecycle.Event.ON_STOP) {
            val prefs =
                context.getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
            val isRunning = prefs.getBoolean("overlay_running", false)
            if (isRunning) {
                val showIntent = android.content.Intent(context, OverlayService::class.java)
                    .apply { action = "ACTION_SHOW_OVERLAY" }
                context.startService(showIntent)
            }
        }

        // --- 4. アプリ全体設定 (DB) ---
        // 設定変更をリアルタイムに検知するためのFlow
        val appSettingFromFlow by db.memoDao().getSettingFlow()
            .collectAsState(initial = AppSetting())
        // スイッチの状態（初期値はDBから。なければデフォルト）
        var showSimpleCounter by remember { mutableStateOf(true) }
        var showFlashEffect by remember { mutableStateOf(true) }
        var currentAppSetting: AppSetting by remember { mutableStateOf(AppSetting()) }
        var useMaxBrightness by remember(currentAppSetting) { mutableStateOf(currentAppSetting.useMaxBrightness) }

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
        // 💡 1. カウンターのボタン（色や並び順）を、今の機種（machineId）だけで絞り込んで監視
        val counterSettings by db.memoDao().getCountersByMachineFlow(machineId)
            .collectAsState(initial = emptyList())

        // 💡 2. カウンターの数字（カウント数）も、今の機種（machineId）に紐づくものだけを監視
        val currentCounterValues by db.memoDao().getCounterValuesByMachineFlow(machineId)
            .collectAsState(initial = emptyList())
        var newCounterName by remember { mutableStateOf("") }
        var showCounterMenuSetting by remember { mutableStateOf<CounterSetting?>(null) }
        var showColorEditPanel by remember { mutableStateOf(false) }
        var showAddCounterDialog by remember { mutableStateOf(false) }

        // 🟢 【新規追加】自動計算設定ダイアログ用の状態
        var showCalcSettingDialog by remember { mutableStateOf<CounterSetting?>(null) }
        var selectedCalcType by remember { mutableStateOf(0) }
        var selectedTargetType by remember { mutableStateOf(0) }
        var selectedTargetCounterId by remember { mutableStateOf<Int?>(null) }
        var showCalcEditPanel by remember { mutableStateOf(false) }

        var editingCounterId by remember { mutableStateOf<Int?>(null) }

        // 💡 回転数管理・ダイアログ用の一元化された状態変数
        var showRotationDialog by remember { mutableStateOf(false) }
        var currentRotation by remember { mutableStateOf("0000") }
        var addRotation by remember { mutableStateOf("0000") }
        var startRotation by remember { mutableStateOf("0000") }
        var editingTargetId by remember { mutableStateOf<String?>(null) } // これも必要かと思います

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

            // 🟢 データベースから現在の回転数を読み込んで反映
            val savedRotation = db.memoDao().getRotationValue(machineId)
            // 🟢 【修正】プロパティ名を currentRotation に変更
            currentRotation = savedRotation?.currentRotation ?: "0000"
            // 🟢 【追加】開始値を読み込んで反映
            startRotation = savedRotation?.startRotation ?: "0000"

            val machine = db.machineDao().getMachineById(machineId)
            if (machine != null) machineName = machine.name
            columns = db.memoDao().getColumnsByMachineDirect(machineId)
            records = db.memoDao().getRecordsByMachine(machineId)
            valuesMap = db.memoDao().getAllValues().groupBy { it.recordId }
        }

        val refreshData = {
            scope.launch {
                val machine = db.machineDao().getMachineById(machineId)
                if (machine != null) machineName = machine.name
                columns = db.memoDao().getColumnsByMachineDirect(machineId)
                records = db.memoDao().getRecordsByMachine(machineId)
                valuesMap = db.memoDao().getAllValues().groupBy { it.recordId }
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

        LaunchedEffect(Unit) {
            refreshData()
        }

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
                            .background(if (isFlash) flashColor.copy(alpha = 0.5f) else backColor)
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (currentScreen == "main") {
                            Text(
                                text = machineName,
                                style = MaterialTheme.typography.titleLarge,
                                color = mainText,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                        } else {
                            Spacer(modifier = Modifier.weight(1f))
                        }

                        if (currentScreen == "main") {
                            val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current

                            if (showSimpleCounter) {
                                val overlayPrefs = context.getSharedPreferences(
                                    "app_prefs",
                                    android.content.Context.MODE_PRIVATE
                                )

                                // 🟢 修正：SharedPreferencesを監視し、値が変更されたら即座にisOverlayRunningを更新する
                                val isOverlayRunning by produceState(
                                    initialValue = overlayPrefs.getBoolean(
                                        "overlay_running",
                                        false
                                    ), overlayPrefs
                                ) {
                                    val listener =
                                        android.content.SharedPreferences.OnSharedPreferenceChangeListener { prefs, key ->
                                            if (key == "overlay_running") value =
                                                prefs.getBoolean("overlay_running", false)
                                        }
                                    overlayPrefs.registerOnSharedPreferenceChangeListener(listener)
                                    awaitDispose {
                                        overlayPrefs.unregisterOnSharedPreferenceChangeListener(
                                            listener
                                        )
                                    }
                                }

                                Switch(
                                    checked = isOverlayRunning,
                                    onCheckedChange = { isChecked ->
                                        // （onCheckedChange内の処理はそのまま維持）
                                        if (isChecked) {
                                            if (android.provider.Settings.canDrawOverlays(this@MainActivity)) {
                                                overlayPrefs.edit()
                                                    .putBoolean("overlay_running", true).apply()

                                                val startIntent = Intent(
                                                    this@MainActivity,
                                                    OverlayService::class.java
                                                ).apply {
                                                    putExtra("TARGET_MACHINE_ID", machineId)
                                                }
                                                startService(startIntent)

                                                updateOverlayVisibility()
                                            } else {
                                                val intent = Intent(
                                                    android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                                    android.net.Uri.parse("package:$packageName")
                                                )
                                                startActivity(intent)
                                            }
                                        } else {
                                            val intent = Intent(
                                                this@MainActivity,
                                                OverlayService::class.java
                                            )
                                            stopService(intent)
                                            overlayPrefs.edit().putBoolean("overlay_running", false)
                                                .apply()
                                        }
                                    },
                                    modifier = Modifier.padding(end = 24.dp)
                                )
                            }
                            // Undoボタン
                            IconButton(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    viewModel.undo()
                                    scope.launch {
                                        refreshData()
                                        kotlinx.coroutines.delay(100)
                                        refreshData()
                                    }
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

                            // Redoボタン
                            IconButton(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    viewModel.redo()
                                    scope.launch {
                                        refreshData()
                                        kotlinx.coroutines.delay(100)
                                        refreshData()
                                    }
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

                            // メニューボタン
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
                floatingActionButton = {
                    if (currentScreen == "main" && !showInputArea) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp) // ボタン同士の間隔
                        ) {
                            // 📄 左側：最新メモの編集ボタン（★一番最近のメモを取得＆色を変更）
                            val isMemoNotEmpty = !records.isNullOrEmpty() // メモが空じゃないか判定
                            val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current

                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .background(
                                        color = if (isMemoNotEmpty) Color(0xFF009688) else Color(
                                            0xFF444444
                                        ),
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                    .clickable(enabled = isMemoNotEmpty) { // 空ならタップ不可
                                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)

                                        // 💡 【変更】最古ではなく、リストの「一番最後＝一番最近のメモ」を安全に取得
                                        val latestRecord = records?.lastOrNull()
                                        if (latestRecord != null) {
                                            editingRecordId = latestRecord.id
                                            // 最新データの入力値を非同期で復元して入力欄を開く
                                            scope.launch {
                                                val currentValues =
                                                    db.memoDao().getValuesForRecord(latestRecord.id)
                                                inputValues.clear()
                                                currentValues.forEach {
                                                    inputValues[it.columnId] = it.value
                                                }
                                                showInputArea = true
                                            }
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "最新のメモを編集",
                                    // 🎨 アイコン色も背景に合わせて調整（有効なら締まりのある濃い暗色、無効ならグレー）
                                    tint = if (isMemoNotEmpty) Color(0xFFFFFFFF) else Color.Gray
                                )
                            }

                            // ➕ 右側：通常の新規追加ボタン（いつもの紫）
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .background(
                                        color = Color(0xFF7E57C2), // いつもの紫
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                    .clickable {
                                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                        inputValues.clear()
                                        editingRecordId = null
                                        showInputArea = true
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "新規追加",
                                    tint = Color.White,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                    }
                },
                bottomBar = {
                    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
                    if (currentScreen == "main" && !showInputArea && showSimpleCounter) {
                        val allCountsMap = counterSettings.associate { setting ->
                            setting.id to viewModel.dao.getCounterCountFlow(setting.id)
                                .collectAsState(initial = 0).value
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF1E1E1E))
                                .padding(8.dp)
                                .navigationBarsPadding(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (currentAppSetting.showTotalRotation) {
                                Box(
                                    modifier = Modifier
                                        .wrapContentWidth()
                                        .height(currentAppSetting.counterHeight.dp)
                                        .background(
                                            Color(0xFF2A2A2A),
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .combinedClickable(
                                            onClick = {},
                                            onLongClick = {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                showRotationDialog = true
                                            }
                                        )
                                        .padding(horizontal = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        // 🟢 差分を計算して表示（現在 - 開始）
                                        text = ((currentRotation.toIntOrNull()
                                            ?: 0) - (startRotation.toIntOrNull() ?: 0)).toString()
                                            .padStart(4, '0'),
                                        color = Color.White,
                                        fontSize = currentAppSetting.rotationFontSize.sp,
                                        fontWeight = FontWeight.Normal,
                                        fontFamily = SevenSegmentFontFamily,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }

                            counterSettings.forEach { setting ->
                                val count = allCountsMap[setting.id] ?: 0
                                val buttonColor = Color(setting.color)
                                val rateTextPair =
                                    remember(count, currentRotation, allCountsMap, setting) {
                                        if (count == 0 || setting.calcType == 0) return@remember null
                                        val targetValue = if (setting.targetType == 0) {
                                            // 🟢 「現在 - 開始」を計算の基準にする
                                            (currentRotation.toIntOrNull() ?: 0) - (startRotation.toIntOrNull() ?: 0)
                                        } else {
                                            allCountsMap[setting.targetCounterId] ?: 0
                                        }
                                        if (targetValue <= 0) return@remember Pair("-.-", "")
                                        when (setting.calcType) {
                                            1 -> {
                                                val result = targetValue.toDouble() / count
                                                Pair("1/", String.format("%.1f", result))
                                            }

                                            2 -> {
                                                val result = (count.toDouble() / targetValue) * 100
                                                Pair(String.format("%.1f", result), "%")
                                            }

                                            else -> null
                                        }
                                    }

                                Row(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(currentAppSetting.counterHeight.dp)
                                        .background(
                                            brush = Brush.verticalGradient(
                                                colors = listOf(
                                                    buttonColor,
                                                    buttonColor.copy(alpha = 0.6f)
                                                )
                                            ),
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .combinedClickable(
                                            onClick = {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                viewModel.updateCounterWithHistory(
                                                    setting.id,
                                                    isIncrement = true
                                                )
                                                if (showFlashEffect) {
                                                    scope.launch {
                                                        val activity =
                                                            context as? android.app.Activity
                                                        val window = activity?.window
                                                        val params = window?.attributes

                                                        // 💡 修正ポイント：すでに画面がフラッシュ中（isFlash == true）の場合は、
                                                        // すでに元の輝度は固定（あるいは保存済み）とみなして、1.0の誤保存を防ぎます。
                                                        // フラッシュ中ではない（通常状態）のときだけ、本当の元の輝度を保存します。
                                                        if (!isFlash) {
                                                            // クラスの上部に退避させるか、現在のスコープ外で保持するのが理想ですが、
                                                            // 連打時の1.0上書きを防ぐため、1.0（MAX値）以外の時だけバックアップを取るガードを入れます。
                                                            val currentB =
                                                                params?.screenBrightness ?: -1f
                                                            if (currentB < 1.0f) {
                                                                // クラス全体で共有する変数にしていない場合、ローカルに持つのを避けるため
                                                                // 以下の「1.0未満の時だけ更新する」ロジックでガードします。
                                                                // より確実にやるために、if (isFlash) 中は輝度変更ロジックの侵入自体を制御します。
                                                            }
                                                        }

                                                        // ─── 一番確実でシンプルなスマートガード ───
                                                        if (!isFlash) { // 🟢 まだ光っていない時だけ、元の輝度を記憶する
                                                            val originalBrightness =
                                                                params?.screenBrightness ?: -1f

                                                            if (useMaxBrightness) {
                                                                params?.screenBrightness = 1f
                                                                window?.attributes = params
                                                            }

                                                            flashColor = buttonColor
                                                            isFlash = true
                                                            delay(100)
                                                            isFlash = false

                                                            if (useMaxBrightness) {
                                                                params?.screenBrightness =
                                                                    originalBrightness
                                                                window?.attributes = params
                                                            }
                                                        } else {
                                                            // 🟢 すでに光っている最中に連打されたら、エフェクトの色だけ更新して
                                                            // ディレイ（光る時間）を少しだけ延長、輝度の復帰は最初のコルーチンに任せる
                                                            flashColor = buttonColor
                                                            // 100msのディレイ中に再度タップされた際、何もしないことで元のコルーチンが安全に元の輝度に戻してくれます
                                                        }
                                                    }
                                                }
                                            },
                                            onLongClick = {
                                                if (count > 0) {
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    viewModel.updateCounterWithHistory(
                                                        setting.id,
                                                        isIncrement = false
                                                    )
                                                }
                                            }
                                        )
                                        .padding(horizontal = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    if (rateTextPair != null) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center,
                                            modifier = Modifier.padding(end = 6.dp)
                                        ) {
                                            Text(
                                                text = rateTextPair.first,
                                                color = Color(0xFF222222),
                                                fontSize = (currentAppSetting.rateFontSize * 0.35f).coerceAtLeast(
                                                    10f
                                                ).sp,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1
                                            )
                                            Text(
                                                text = rateTextPair.second,
                                                color = Color(0xFF222222),
                                                fontSize = (currentAppSetting.rateFontSize * 0.35f).coerceAtLeast(
                                                    10f
                                                ).sp,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1
                                            )
                                        }
                                    }

                                    Text(
                                        text = count.toString(),
                                        color = Color(0xFF111111),
                                        fontSize = (currentAppSetting.counterFontSize * 1.0f).sp,
                                        fontWeight = FontWeight.Normal,
                                        fontFamily = SevenSegmentFontFamily
                                    )
                                }
                            }
                        }
                    }
                },
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
                                Text(
                                    "時間を表示する",
                                    color = mainText,
                                    fontSize = 18.sp
                                )
                            }




                            Spacer(modifier = Modifier.height(16.dp))
                            Divider(color = dividerColor) // ★区切り線も変数に
                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                "項目の追加",
                                style = MaterialTheme.typography.titleMedium,
                                color = mainText,
                                fontSize = 18.sp
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
                                color = mainText,
                                fontSize = 18.sp
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
                                    color = mainText,
                                    fontSize = 18.sp
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
                                    color = mainText,
                                    fontSize = 18.sp
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
                                        color = mainText,
                                        fontSize = 18.sp
                                    )
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                Text(
                                    text = "「${col.name}」の選択肢一覧",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = mainText,
                                    fontSize = 18.sp
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
                                        style = MaterialTheme.typography.titleMedium,
                                        fontSize = 18.sp
                                    )
                                }
                            }  // --- 設定画面の Column 閉じタグ (既存コードの末尾付近) ---
                        } // Columnの終わり

                    } else if (currentScreen == "counter_settings") {
                        // ★ ここが「簡易カウンター専用の設定画面」 ★
                        Box(modifier = Modifier.fillMaxSize()) {

                            val prefs = context.getSharedPreferences(
                                "app_prefs",
                                android.content.Context.MODE_PRIVATE
                            )

                            // 【1】画面のON/OFF状態（isServiceRunning）の管理
                            // 🟢 修正：単なる初期値代入ではなく、画面が再描画されるたびにプリファレンスから最新状態を取得するようにします
                            var isServiceRunning by remember { mutableStateOf(false) }

                            LaunchedEffect(Unit) {
                                val prefs = context.getSharedPreferences(
                                    "app_prefs",
                                    android.content.Context.MODE_PRIVATE
                                )
                                isServiceRunning = prefs.getBoolean("overlay_running", false)
                            }

                            // 🟢 修正：通知エリアが閉じられてアプリ画面にフォーカスが戻った瞬間（hasWindowFocus）を検知して再読込する
                            val contextActivity = context as? androidx.activity.ComponentActivity
                            androidx.compose.runtime.DisposableEffect(contextActivity) {
                                val listener =
                                    android.view.ViewTreeObserver.OnWindowFocusChangeListener { hasFocus ->
                                        // 通知エリアが閉じられて、アプリ画面に操作権（フォーカス）が戻ってきた場合
                                        if (hasFocus) {
                                            val latestStatus =
                                                prefs.getBoolean("overlay_running", false)
                                            if (!latestStatus) {
                                                isServiceRunning = false
                                            } else {
                                                isServiceRunning = true
                                            }
                                        }
                                    }

                                val view = contextActivity?.window?.decorView
                                view?.viewTreeObserver?.addOnWindowFocusChangeListener(listener)

                                onDispose {
                                    view?.viewTreeObserver?.removeOnWindowFocusChangeListener(
                                        listener
                                    )
                                }
                            }

                            // 【4】画面重ね合わせ権限用のランチャー
                            val overlayPermissionLauncher = rememberLauncherForActivityResult(
                                contract = ActivityResultContracts.StartActivityForResult()
                            ) {
                                if (Settings.canDrawOverlays(context)) {
                                    isServiceRunning = true
                                    prefs.edit().putBoolean("overlay_running", true).apply()
                                    val intent = Intent(context, OverlayService::class.java).apply {
                                        putExtra("TARGET_MACHINE_ID", machineId)
                                    }
                                    // 🟢 修正：startForegroundService から通常の startService に変更
                                    context.startService(intent)
                                } else {
                                    isServiceRunning = false
                                }
                            }

                            // 【5】ここから設定画面のレイアウト
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
                                        .clickable {
                                            val nextChecked = !showSimpleCounter

                                            // DBの設定を更新
                                            scope.launch {
                                                db.memoDao().saveAppSetting(
                                                    currentAppSetting.copy(showSimpleCounter = nextChecked)
                                                )
                                            }

                                            // 「カウンターを表示する」をOFFにされた場合
                                            if (!nextChecked) {
                                                // 1. サービスを完全に終了
                                                val intent = Intent(
                                                    this@MainActivity,
                                                    OverlayService::class.java
                                                )
                                                stopService(intent)

                                                // 2. データのフラグをOFFにする
                                                context.getSharedPreferences(
                                                    "app_prefs",
                                                    android.content.Context.MODE_PRIVATE
                                                )
                                                    .edit().putBoolean("overlay_running", false)
                                                    .apply()

                                                // 3. 🟢 修正：常駐カウンタースイッチのStateを明示的にOFFにする
                                                isServiceRunning = false
                                            }

                                            // showSimpleCounter の状態更新
                                            showSimpleCounter = nextChecked
                                        }
                                        .padding(vertical = 12.dp),
                                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                                ) {
                                    Switch(
                                        checked = showSimpleCounter,
                                        onCheckedChange = null // clickableで制御するためnull
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = "カウンターを表示する",
                                        color = mainText,
                                        fontSize = 18.sp
                                    )
                                }

                                // --- ② 下のスイッチ群をまとめる Box ---
                                Box(modifier = Modifier.fillMaxWidth()) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .graphicsLayer {
                                                val sat = if (showSimpleCounter) 1f else 0f
                                                val matrix =
                                                    androidx.compose.ui.graphics.ColorMatrix()
                                                        .apply { setToSaturation(sat) }
                                                colorFilter =
                                                    androidx.compose.ui.graphics.ColorFilter.colorMatrix(
                                                        matrix
                                                    )
                                            }
                                            .alpha(if (showSimpleCounter) 1f else 0.4f),
                                        verticalArrangement = Arrangement.spacedBy(0.dp)
                                    ) {
                                        // --- ②-1 総回転数を表示するスイッチ ---
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable(enabled = showSimpleCounter) {
                                                    val nextValue =
                                                        !currentAppSetting.showTotalRotation
                                                    scope.launch {
                                                        db.memoDao().saveAppSetting(
                                                            currentAppSetting.copy(showTotalRotation = nextValue)
                                                        )
                                                    }
                                                }
                                                .padding(vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Switch(
                                                checked = currentAppSetting.showTotalRotation,
                                                onCheckedChange = null,
                                                enabled = showSimpleCounter
                                            )
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Text(
                                                text = "総回転数を表示する",
                                                color = mainText,
                                                fontSize = 18.sp
                                            )
                                        }

                                        // --- ②-2 タップ時にヘッダーをフラッシュ ---
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable(enabled = showSimpleCounter) {
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
                                                text = "タップ時にボタンの色にフラッシュ",
                                                color = mainText,
                                                fontSize = 18.sp
                                            )
                                        }

                                        // --- ②-3 画面輝度を上げるスイッチ ---
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable(enabled = showSimpleCounter) {
                                                    val nextValue =
                                                        !currentAppSetting.useMaxBrightness
                                                    scope.launch {
                                                        db.memoDao().saveAppSetting(
                                                            currentAppSetting.copy(useMaxBrightness = nextValue)
                                                        )
                                                    }
                                                }
                                                .padding(vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Switch(
                                                checked = currentAppSetting.useMaxBrightness,
                                                onCheckedChange = null,
                                                enabled = showSimpleCounter
                                            )
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Text(
                                                text = "タップ時に画面をフラッシュ",
                                                color = mainText,
                                                fontSize = 18.sp
                                            )
                                        }

                                        // --- ① 常駐カウンターを表示するスイッチ（画面重ね合わせ） ---
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    if (isServiceRunning) {
                                                        // 🟢 安全にOFFにする処理：直接stopServiceを呼ぶ
                                                        isServiceRunning = false
                                                        prefs.edit()
                                                            .putBoolean("overlay_running", false)
                                                            .apply()
                                                        val intent = Intent(
                                                            context,
                                                            OverlayService::class.java
                                                        )
                                                        context.stopService(intent)
                                                    } else {
                                                        // 🟢 ONにする処理：まず通知の権限チェックを挟み込む
                                                        (context as? MainActivity)?.checkAndRequestNotificationPermission {
                                                            // 通知が許可されたら（または元々許可されていれば）ここが実行される
                                                            if (Settings.canDrawOverlays(context)) {
                                                                isServiceRunning = true
                                                                prefs.edit().putBoolean(
                                                                    "overlay_running",
                                                                    true
                                                                ).apply()
                                                                val intent = Intent(
                                                                    context,
                                                                    OverlayService::class.java
                                                                ).apply {
                                                                    putExtra(
                                                                        "TARGET_MACHINE_ID",
                                                                        machineId
                                                                    )
                                                                }
                                                                // 通常のバックグラウンドサービスとして安全に起動
                                                                context.startService(intent)
                                                            } else {
                                                                val intent =
                                                                    Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
                                                                        data =
                                                                            Uri.parse("package:${context.packageName}")
                                                                    }
                                                                overlayPermissionLauncher.launch(
                                                                    intent
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                                .padding(vertical = 0.dp), // 🟢 大切なデザイン設定を残しています
                                            verticalAlignment = Alignment.CenterVertically // 🟢 大切なデザイン設定を残しています
                                        ) {
                                            Switch(
                                                checked = isServiceRunning,
                                                onCheckedChange = { isChecked ->
                                                    if (!isChecked) {
                                                        // スイッチを直接OFFにした時も安全に即時停止
                                                        isServiceRunning = false
                                                        prefs.edit()
                                                            .putBoolean("overlay_running", false)
                                                            .apply()
                                                        val intent = Intent(
                                                            context,
                                                            OverlayService::class.java
                                                        )
                                                        context.stopService(intent)
                                                    } else {
                                                        if (Settings.canDrawOverlays(context)) {
                                                            isServiceRunning = true
                                                            prefs.edit()
                                                                .putBoolean("overlay_running", true)
                                                                .apply()
                                                            val intent = Intent(
                                                                context,
                                                                OverlayService::class.java
                                                            ).apply {
                                                                putExtra(
                                                                    "TARGET_MACHINE_ID",
                                                                    machineId
                                                                )
                                                            }
                                                            context.startService(intent)
                                                        } else {
                                                            val intent = Intent(
                                                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                                                Uri.parse("package:${context.packageName}")
                                                            )
                                                            overlayPermissionLauncher.launch(intent)
                                                        }
                                                    }
                                                }
                                            )
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Text(
                                                text = "常駐カウンターを表示する",
                                                color = mainText,
                                                fontSize = 18.sp
                                            )
                                        }


                                        Spacer(modifier = Modifier.height(20.dp))

                                        Text(
                                            text = "カウンターの高さ",
                                            color = mainText,
                                            fontSize = 18.sp
                                        )

                                        val heightOptions = listOf(30, 45, 60, 75, 90)

                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(top = 2.dp, bottom = 12.dp),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            heightOptions.forEach { hValue ->
                                                val isSelected =
                                                    currentAppSetting.counterHeight == hValue

                                                Box(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .height(32.dp)
                                                        .background(
                                                            color = if (isSelected) Color(0xFFBB86FC) else Color(
                                                                0xFF333333
                                                            ),
                                                            shape = RoundedCornerShape(8.dp)
                                                        )
                                                        .clickable(enabled = showSimpleCounter) {
                                                            scope.launch {
                                                                // 🟢 連動処理をすべて削除し、純粋に高さだけを保存します
                                                                db.memoDao().saveAppSetting(
                                                                    currentAppSetting.copy(
                                                                        counterHeight = hValue
                                                                    )
                                                                )
                                                                // 🟢 今回新しく追加するコード（メモ帳にも高さを保存する）
                                                                prefs.edit().putInt(
                                                                    "counter_height",
                                                                    hValue
                                                                ).apply()
                                                            }
                                                        },
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = when (hValue) {
                                                            30 -> "1"
                                                            45 -> "2"
                                                            60 -> "3"
                                                            75 -> "4"
                                                            90 -> "5"
                                                            else -> ""
                                                        },
                                                        color = if (isSelected) Color.Black else Color.White,
                                                        fontSize = 24.sp,
                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                                    )
                                                }
                                            }
                                        }

                                        Text(
                                            text = "総回転数文字サイズ",
                                            color = mainText,
                                            fontSize = 18.sp
                                        )

                                        val rotationFontSizeOptions = listOf(10, 20, 30, 40, 50)

                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(top = 2.dp, bottom = 12.dp),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            rotationFontSizeOptions.forEach { rValue ->
                                                // 🟢 高さ制限（isEnabled）を撤廃
                                                val isSelected =
                                                    currentAppSetting.rotationFontSize == rValue

                                                Box(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .height(32.dp)
                                                        .background(
                                                            // 🟢 常に有効な状態の色設定に変更
                                                            color = if (isSelected) Color(0xFFBB86FC) else Color(
                                                                0xFF333333
                                                            ),
                                                            shape = RoundedCornerShape(8.dp)
                                                        )
                                                        .clickable(enabled = showSimpleCounter) { // 🟢 isEnabled条件を削除
                                                            scope.launch {
                                                                db.memoDao().saveAppSetting(
                                                                    currentAppSetting.copy(
                                                                        rotationFontSize = rValue
                                                                    )
                                                                )
                                                            }
                                                        },
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = when (rValue) {
                                                            10 -> "1"
                                                            20 -> "2"
                                                            30 -> "3"
                                                            40 -> "4"
                                                            50 -> "5"
                                                            else -> ""
                                                        },
                                                        color = if (isSelected) Color.Black else Color.White,
                                                        fontSize = 24.sp,
                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                                    )
                                                }
                                            }
                                        }

                                        Text(
                                            text = "カウンター文字サイズ",
                                            color = mainText,
                                            fontSize = 18.sp
                                        )

                                        val fontSizeOptions = listOf(20, 26, 32, 38, 44)

                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(top = 2.dp, bottom = 12.dp),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            fontSizeOptions.forEach { fValue ->
                                                // 🟢 高さ制限（isEnabled）を撤廃
                                                val isSelected =
                                                    currentAppSetting.counterFontSize == fValue

                                                Box(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .height(32.dp)
                                                        .background(
                                                            // 🟢 常に有効な状態の色設定に変更
                                                            color = if (isSelected) Color(0xFFBB86FC) else Color(
                                                                0xFF333333
                                                            ),
                                                            shape = RoundedCornerShape(8.dp)
                                                        )
                                                        .clickable(enabled = showSimpleCounter) { // 🟢 isEnabled条件を削除
                                                            scope.launch {
                                                                db.memoDao().saveAppSetting(
                                                                    currentAppSetting.copy(
                                                                        counterFontSize = fValue
                                                                    )
                                                                )
                                                            }
                                                        },
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = when (fValue) {
                                                            20 -> "1"
                                                            26 -> "2"
                                                            32 -> "3"
                                                            38 -> "4"
                                                            44 -> "5"
                                                            else -> ""
                                                        },
                                                        color = if (isSelected) Color.Black else Color.White,
                                                        fontSize = 24.sp,
                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                                    )
                                                }
                                            }
                                        }

                                        Text(
                                            text = "確率表示文字サイズ",
                                            color = mainText,
                                            fontSize = 18.sp
                                        )

                                        val rateFontSizeOptions = listOf(45, 55, 65, 75, 85)

                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(top = 2.dp, bottom = 12.dp),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            rateFontSizeOptions.forEach { rFValue ->
                                                // 🟢 高さ制限（isEnabled）を撤廃
                                                val isSelected =
                                                    currentAppSetting.rateFontSize == rFValue

                                                Box(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .height(32.dp)
                                                        .background(
                                                            // 🟢 常に有効な状態の色設定に変更
                                                            color = if (isSelected) Color(0xFFBB86FC) else Color(
                                                                0xFF333333
                                                            ),
                                                            shape = RoundedCornerShape(8.dp)
                                                        )
                                                        .clickable(enabled = showSimpleCounter) { // 🟢 isEnabled条件を削除
                                                            scope.launch {
                                                                db.memoDao().saveAppSetting(
                                                                    currentAppSetting.copy(
                                                                        rateFontSize = rFValue
                                                                    )
                                                                )
                                                            }
                                                        },
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = when (rFValue) {
                                                            45 -> "1"
                                                            55 -> "2"
                                                            65 -> "3"
                                                            75 -> "4"
                                                            85 -> "5"
                                                            else -> ""
                                                        },
                                                        color = if (isSelected) Color.Black else Color.White,
                                                        fontSize = 24.sp,
                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                                    )
                                                }
                                            }
                                        }

                                        // --- 確率表示文字サイズの Row の閉じ括弧 のすぐ下（状態管理・完全分離版） ---

                                        Text(
                                            text = "常駐カウンターの高さ",
                                            color = mainText,
                                            fontSize = 18.sp
                                        )

                                        val customHeightOptions = listOf(30, 45, 60, 75, 90)

                                        // 🟢 1. Composeがリアルタイムに検知して画面を書き換えるための「状態」を作成します
                                        var overlaySavedHeight by remember {
                                            mutableStateOf(
                                                prefs.getInt(
                                                    "counter_overlay_height",
                                                    60
                                                )
                                            )
                                        }

                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(top = 2.dp, bottom = 12.dp),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            customHeightOptions.forEach { hValue ->
                                                // 🟢 作成した状態（overlaySavedHeight）を監視してハイライトを切り替える
                                                val isSelected = overlaySavedHeight == hValue

                                                Box(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .height(32.dp)
                                                        .background(
                                                            color = if (isSelected) Color(0xFFBB86FC) else Color(
                                                                0xFF333333
                                                            ),
                                                            shape = RoundedCornerShape(8.dp)
                                                        )
                                                        .clickable {
                                                            // 🟢 2. タップされた瞬間に状態を直接書き換えることで、ハイライトがその場で即座に移動します
                                                            overlaySavedHeight = hValue

                                                            scope.launch {
                                                                // バックグラウンド（SharedPreferences）にもしっかり保存
                                                                prefs.edit().putInt(
                                                                    "counter_overlay_height",
                                                                    hValue
                                                                ).apply()
                                                            }
                                                        },
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = when (hValue) {
                                                            30 -> "1"
                                                            45 -> "2"
                                                            60 -> "3"
                                                            75 -> "4"
                                                            90 -> "5"
                                                            else -> ""
                                                        },
                                                        color = if (isSelected) Color.Black else Color.White,
                                                        fontSize = 24.sp,
                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                                    )
                                                }
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(20.dp))

                                        Text(
                                            "カウントボタンの追加",
                                            color = mainText,
                                            fontSize = 18.sp
                                        )

                                        Button(
                                            onClick = { showAddCounterDialog = true },
                                            modifier = Modifier.fillMaxWidth(),
                                            enabled = showSimpleCounter, // タップの有効・無効はこれに連動
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = Color(0xFFBB86FC),
                                                contentColor = Color.Black,
                                                // 🟢 無効化された時も、有効な時と「全く同じ色」をあえて指定する
                                                disabledContainerColor = Color(0xFFBB86FC),
                                                disabledContentColor = Color.Black
                                            )
                                        ) {
                                            Text(
                                                "色を選んで追加する",
                                                color = Color.Black,
                                                fontSize = 18.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(20.dp))

                                        Text(
                                            "現在のボタン一覧(タップで入れ替え・色変更・削除)",
                                            color = mainText,
                                            fontSize = 18.sp
                                        )

                                        // 横並び（FlowRow）のチップ一覧
                                        FlowRow(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            counterSettings.forEachIndexed { index, setting ->
                                                key("row_${setting.id}") {
                                                    val letterName = ('A' + index).toString()

                                                    CompositionLocalProvider(
                                                        LocalMinimumInteractiveComponentEnforcement provides false
                                                    ) {
                                                        InputChip(
                                                            selected = false,
                                                            onClick = {
                                                                showCounterMenuSetting = setting
                                                            },
                                                            enabled = showSimpleCounter, // 🟢 1. タップの有効・無効を連動させる
                                                            label = {
                                                                Box(
                                                                    modifier = Modifier.fillMaxSize(),
                                                                    contentAlignment = Alignment.Center
                                                                ) {
                                                                    Text(
                                                                        letterName,
                                                                        color = Color.Black,
                                                                        fontSize = 18.sp,
                                                                        fontWeight = FontWeight.Bold
                                                                    )
                                                                }
                                                            },
                                                            modifier = Modifier
                                                                .width(44.dp)
                                                                .height(32.dp),
                                                            colors = InputChipDefaults.inputChipColors(
                                                                containerColor = Color(setting.color),
                                                                labelColor = Color.Black,
                                                                // 🟢 2. 無効化された時も元の色を維持する
                                                                disabledContainerColor = Color(
                                                                    setting.color
                                                                ),
                                                                disabledLabelColor = Color.Black
                                                            ),
                                                            border = InputChipDefaults.inputChipBorder(
                                                                borderColor = Color(setting.color),
                                                                // 🟢 3. ここは無効時も枠線の色を維持するため、常に true または同じ色にする
                                                                disabledBorderColor = Color(setting.color),
                                                                enabled = true,
                                                                selected = false
                                                            )
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(20.dp))

                                        Text(
                                            "カウント処理一覧(タップで計算処理編集)",
                                            color = mainText,
                                            fontSize = 18.sp
                                        )

                                        // ==========================================
                                        // ② 下段：縦並び（Column）のチップ＋処理説明一覧
                                        // ==========================================
                                        Column(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            counterSettings.forEachIndexed { index, setting ->
                                                key("col_${setting.id}") {
                                                    val letterName = ('A' + index).toString()

                                                    val calcDescription = when (setting.calcType) {
                                                        1 -> {
                                                            if (setting.targetType == 0) {
                                                                "$letterName / 総回転数 (1/X)"
                                                            } else {
                                                                val tIdx =
                                                                    counterSettings.indexOfFirst { it.id == setting.targetCounterId }
                                                                val tLetter =
                                                                    if (tIdx >= 0) ('A' + tIdx).toString() else "?"
                                                                "$letterName / $tLetter (1/X)"
                                                            }
                                                        }

                                                        2 -> {
                                                            if (setting.targetType == 0) {
                                                                "$letterName / 総回転数 (%)"
                                                            } else {
                                                                val tIdx =
                                                                    counterSettings.indexOfFirst { it.id == setting.targetCounterId }
                                                                val tLetter =
                                                                    if (tIdx >= 0) ('A' + tIdx).toString() else "?"
                                                                "$letterName / $tLetter (%)"
                                                            }
                                                        }

                                                        else -> "カウントのみ"
                                                    }

                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        CompositionLocalProvider(
                                                            LocalMinimumInteractiveComponentEnforcement provides false
                                                        ) {
                                                            InputChip(
                                                                modifier = Modifier
                                                                    .width(44.dp)
                                                                    .height(32.dp),
                                                                selected = false,
                                                                onClick = {
                                                                    showCalcSettingDialog = setting
                                                                    selectedCalcType =
                                                                        setting.calcType
                                                                    selectedTargetType =
                                                                        setting.targetType
                                                                    selectedTargetCounterId =
                                                                        setting.targetCounterId
                                                                },
                                                                enabled = showSimpleCounter, // 🟢 1. タップの有効・無効を連動
                                                                label = {
                                                                    Box(
                                                                        modifier = Modifier.fillMaxSize(),
                                                                        contentAlignment = Alignment.Center
                                                                    ) {
                                                                        Text(
                                                                            letterName,
                                                                            color = Color.Black,
                                                                            fontSize = 18.sp,
                                                                            fontWeight = FontWeight.Bold,
                                                                            maxLines = 1
                                                                        )
                                                                    }
                                                                },
                                                                colors = InputChipDefaults.inputChipColors(
                                                                    containerColor = Color(setting.color),
                                                                    labelColor = Color.Black,
                                                                    // 🟢 2. 無効化された時も元の色を維持
                                                                    disabledContainerColor = Color(
                                                                        setting.color
                                                                    ),
                                                                    disabledLabelColor = Color.Black
                                                                ),
                                                                border = InputChipDefaults.inputChipBorder(
                                                                    borderColor = Color(setting.color),
                                                                    // 🟢 3. 無効時の枠線の色も維持
                                                                    disabledBorderColor = Color(
                                                                        setting.color
                                                                    ),
                                                                    enabled = true,
                                                                    selected = false
                                                                )
                                                            )
                                                        }

                                                        Spacer(modifier = Modifier.width(12.dp))

                                                        Text(
                                                            text = calcDescription,
                                                            color = if (setting.calcType == 0) subText else mainText,
                                                            fontSize = 14.sp,
                                                            fontWeight = if (setting.calcType == 0) FontWeight.Normal else FontWeight.Medium
                                                        )
                                                    }
                                                }
                                            }
                                        } // ②縦並びColumnの閉じ
                                    } // スイッチ群まとめColumnの閉じ
                                } // スイッチ群まとめBoxの閉じ
                            } // 設定画面コンテンツ全体のColumnの閉じ

                            // 🌟 移動してきたダイアログの設置場所です（Boxの直上になるので正しく重なります）
                            if (showCalcSettingDialog != null) {
                                val currentSetting = showCalcSettingDialog!!
                                val otherCounters =
                                    counterSettings.filter { it.id != currentSetting.id }
                                val currentIdx =
                                    counterSettings.indexOfFirst { it.id == currentSetting.id }
                                val currentLetter =
                                    if (currentIdx >= 0) ('A' + currentIdx).toString() else ""

                                androidx.activity.compose.BackHandler {
                                    showCalcSettingDialog = null
                                }

                                Box(
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color.Black.copy(alpha = 0.6f))
                                    )

                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color.White.copy(alpha = 0.3f))
                                            .clickable(
                                                indication = null,
                                                interactionSource = remember { MutableInteractionSource() }
                                            ) {
                                                showCalcSettingDialog = null
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(16.dp),
                                            color = Color(0xFF252525),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(16.dp)
                                                .clickable(enabled = false) { }
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(20.dp)
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.padding(bottom = 20.dp)
                                                ) {
                                                    val chipColor =
                                                        showCalcSettingDialog?.color?.let { Color(it) }
                                                            ?: Color.Gray

                                                    // 🟢 ここ！先頭に幅44dp、高さ32dpの固定サイズでチップを配置
                                                    Box(
                                                        modifier = Modifier
                                                            .width(44.dp)   // ★ここを変えたかった！
                                                            .height(32.dp)  // 一覧のチップと同じ高さ
                                                            .background(
                                                                color = chipColor,
                                                                shape = RoundedCornerShape(8.dp)
                                                            ),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text(
                                                            text = currentLetter,
                                                            color = Color.Black,
                                                            fontSize = 18.sp,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }

                                                    // チップと文字の間に少しだけすき間を開ける
                                                    Spacer(modifier = Modifier.width(8.dp))

                                                    Text(
                                                        text = "の出現率設定",
                                                        color = Color.White,
                                                        fontSize = 20.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }

                                                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                                                    val calcOptions = listOf(
                                                        "なし（カウントのみ）" to 0,
                                                        "分数 (1/X)" to 1,
                                                        "パーセント (%)" to 2
                                                    )
                                                    calcOptions.forEach { (label, value) ->
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .clickable {
                                                                    selectedCalcType = value
                                                                }
                                                                .padding(vertical = 4.dp)
                                                        ) {
                                                            androidx.compose.material3.RadioButton(
                                                                selected = (selectedCalcType == value),
                                                                onClick = {
                                                                    selectedCalcType = value
                                                                },
                                                                colors = androidx.compose.material3.RadioButtonDefaults.colors(
                                                                    selectedColor = Color(0xFFBB86FC),
                                                                    unselectedColor = Color.Gray
                                                                )
                                                            )
                                                            Spacer(modifier = Modifier.width(8.dp))
                                                            Text(
                                                                label,
                                                                color = mainText,
                                                                fontSize = 16.sp
                                                            )
                                                        }
                                                    }
                                                }

                                                if (selectedCalcType != 0) {
                                                    Spacer(modifier = Modifier.height(16.dp))
                                                    Divider(color = Color.Gray.copy(alpha = 0.2f))
                                                    Spacer(modifier = Modifier.height(16.dp))

                                                    Text(
                                                        "■ 2. 計算の分母（対象）",
                                                        color = Color.LightGray,
                                                        fontSize = 14.sp
                                                    )
                                                    Spacer(modifier = Modifier.height(8.dp))

                                                    @OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
                                                    FlowRow(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.spacedBy(
                                                            8.dp
                                                        )
                                                    ) {
                                                        FilterChip(
                                                            selected = (selectedTargetType == 0),
                                                            onClick = {
                                                                selectedTargetType = 0
                                                                selectedTargetCounterId = null
                                                            },
                                                            label = { Text("総回転数") },
                                                            colors = FilterChipDefaults.filterChipColors(
                                                                labelColor = mainText,
                                                                selectedContainerColor = Color(
                                                                    0xFFEADDFF
                                                                ),
                                                                selectedLabelColor = Color.Black
                                                            ),
                                                            modifier = Modifier.padding(vertical = 4.dp)
                                                        )

                                                        otherCounters.forEach { other ->
                                                            val oIdx =
                                                                counterSettings.indexOfFirst { it.id == other.id }
                                                            val oLetter =
                                                                if (oIdx >= 0) ('A' + oIdx).toString() else ""

                                                            FilterChip(
                                                                selected = (selectedTargetType == 1 && selectedTargetCounterId == other.id),
                                                                onClick = {
                                                                    selectedTargetType = 1
                                                                    selectedTargetCounterId =
                                                                        other.id
                                                                },
                                                                label = { Text("ボタン [$oLetter] (${other.name})") },
                                                                colors = FilterChipDefaults.filterChipColors(
                                                                    labelColor = mainText,
                                                                    selectedContainerColor = Color(
                                                                        0xFFFFCDD2
                                                                    ),
                                                                    selectedLabelColor = Color.Black
                                                                ),
                                                                modifier = Modifier.padding(vertical = 4.dp)
                                                            )
                                                        }
                                                    }
                                                }

                                                Spacer(modifier = Modifier.height(24.dp))

                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.End
                                                ) {
                                                    TextButton(onClick = {
                                                        showCalcSettingDialog = null
                                                    }) {
                                                        Text("キャンセル", color = Color.LightGray)
                                                    }
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Button(
                                                        onClick = {
                                                            scope.launch {
                                                                val updatedSetting =
                                                                    currentSetting.copy(
                                                                        calcType = selectedCalcType,
                                                                        targetType = if (selectedCalcType == 0) 0 else selectedTargetType,
                                                                        targetCounterId = if (selectedCalcType == 0) null else selectedTargetCounterId
                                                                    )
                                                                db.memoDao()
                                                                    .updateCounter(updatedSetting)
                                                                showCalcSettingDialog = null
                                                                refreshData()
                                                            }
                                                        },
                                                        enabled = (selectedCalcType == 0) || (selectedTargetType == 0) || (selectedTargetType == 1 && selectedTargetCounterId != null),
                                                        colors = ButtonDefaults.buttonColors(
                                                            containerColor = Color(
                                                                0xFFBB86FC
                                                            )
                                                        )
                                                    ) {
                                                        Text("適用", color = Color.Black)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            } // 🌟ダイアログの if 閉じ
                        } // 🌟新設した最外枠 Box の閉じ
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

// ========================================================
// ① カウンター操作メニュー (完全に独立)
// ========================================================
            if (showCounterMenuSetting != null) {
                androidx.activity.compose.BackHandler {
                    showCounterMenuSetting = null
                }
                val setting = showCounterMenuSetting!!
                val currentIndex = counterSettings.indexOfFirst { it.id == setting.id }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.6f))
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White.copy(alpha = 0.3f))
                        .clickable { showCounterMenuSetting = null },
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
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
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(Color(setting.color), RoundedCornerShape(8.dp))
                                    .border(
                                        2.dp,
                                        Color.White.copy(alpha = 0.5f),
                                        RoundedCornerShape(8.dp)
                                    )
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                            val canMoveColors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFBBBBBB),
                                contentColor = Color.Black
                            )
                            val cannotMoveColors = ButtonDefaults.buttonColors(
                                disabledContainerColor = Color(0xFF333333),
                                disabledContentColor = Color.Black
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // 左へ移動
                                Button(
                                    onClick = {
                                        val list = counterSettings.toMutableList()
                                        val idx = list.indexOfFirst { it.id == setting.id }
                                        if (idx > 0) {
                                            val current = list[idx]
                                            val target = list[idx - 1]
                                            val newCurrent =
                                                current.copy(displayOrder = target.displayOrder)
                                            val newTarget =
                                                target.copy(displayOrder = current.displayOrder)
                                            scope.launch {
                                                db.memoDao().updateCounter(newCurrent)
                                                db.memoDao().updateCounter(newTarget)
                                            }
                                        }
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

                                // 右へ移動
                                Button(
                                    onClick = {
                                        val list = counterSettings.toMutableList()
                                        val idx = list.indexOfFirst { it.id == setting.id }
                                        if (idx >= 0 && idx < list.size - 1) {
                                            val current = list[idx]
                                            val target = list[idx + 1]
                                            val newCurrent =
                                                current.copy(displayOrder = target.displayOrder)
                                            val newTarget =
                                                target.copy(displayOrder = current.displayOrder)
                                            scope.launch {
                                                db.memoDao().updateCounter(newCurrent)
                                                db.memoDao().updateCounter(newTarget)
                                            }
                                        }
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

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // 色変更ボタン
                                Button(
                                    onClick = {
                                        editingCounterId = setting.id    // 🌟 1. どのボタンを変更するかIDを記憶！
                                        showColorEditPanel = true        // 2. 色選択パネルを開く
                                        showCounterMenuSetting = null    // 3. メニューは一旦閉じる
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(60.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(0.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(
                                            0xFF6750A4
                                        ), contentColor = Color.White
                                    )
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            imageVector = Icons.Default.Palette,
                                            contentDescription = "色変更",
                                            tint = Color.White
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text("色変更", color = Color.White, fontSize = 16.sp)
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
                                        containerColor = Color(
                                            0xFFB3261E
                                        ), contentColor = Color.White
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
            } // 👈 🌟 ここで「操作メニュー」の波括弧を完全に閉じます！独立成功！

            // ========================================================
            // ② 色編集パネル (操作メニューの【外側】に並列に配置)
            // ========================================================
            // 記憶しておいたIDを使って、対象のデータを安全に引っ張ってきます
            val currentEditingSetting = counterSettings.find { it.id == editingCounterId }

            if (showColorEditPanel && currentEditingSetting != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.6f)) // 🟢 膜の濃さを統一
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White.copy(alpha = 0.3f)) // 🟢 膜の白さを統一
                        .clickable {
                            showColorEditPanel = false
                            showCounterMenuSetting =
                                currentEditingSetting // 🌟 外側をタップしてキャンセルした時もメニューに戻す
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .clickable(enabled = false) { },
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFF252525)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "新しい色を選択",
                                color = Color.White,
                                fontSize = 18.sp,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )

                            // 1. 原色選択 (LazyRow)
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
                                item {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .background(
                                                brush = Brush.linearGradient(
                                                    listOf(
                                                        Color.White,
                                                        Color.Gray,
                                                        Color.Black
                                                    )
                                                ), shape = RoundedCornerShape(4.dp)
                                            )
                                            .border(
                                                width = if (isMonotone) 3.dp else 0.dp,
                                                color = Color.White,
                                                shape = RoundedCornerShape(4.dp)
                                            )
                                            .clickable { isMonotone = true }
                                    )
                                }
                                items(baseHues) { hue ->
                                    val isSelected = !isMonotone && selectedHue == hue
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .background(
                                                Color.hsl(hue, 0.8f, 0.5f),
                                                RoundedCornerShape(4.dp)
                                            )
                                            .border(
                                                width = if (isSelected) 3.dp else 0.dp,
                                                color = Color.White,
                                                shape = RoundedCornerShape(4.dp)
                                            )
                                            .clickable { isMonotone = false; selectedHue = hue }
                                    )
                                }
                            }

                            // 2. 濃淡選択 (グリッド)
                            val lightnessLevels =
                                listOf(0.9f, 0.8f, 0.7f, 0.6f, 0.5f, 0.4f, 0.3f, 0.2f)
                            val paletteHeight = 90
                            val spacing = 8
                            val itemHeight = ((paletteHeight - spacing) / 2) - 4

                            LazyVerticalGrid(
                                columns = GridCells.Fixed(4),
                                modifier = Modifier.height(paletteHeight.dp),
                                horizontalArrangement = Arrangement.spacedBy(spacing.dp),
                                verticalArrangement = Arrangement.spacedBy(spacing.dp),
                                userScrollEnabled = false
                            ) {
                                items(lightnessLevels) { level ->
                                    val colorVariant = if (isMonotone) {
                                        Color.hsl(0f, 0f, level)
                                    } else {
                                        Color.hsl(selectedHue, 0.7f, level)
                                    }
                                    val colorLong = colorVariant.toArgb().toLong()
                                    val isSelected = currentColorByLong == colorLong

                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(itemHeight.dp)
                                            .background(colorVariant, RoundedCornerShape(4.dp))
                                            .border(
                                                width = if (isSelected) 3.dp else 0.dp,
                                                color = Color.White,
                                                shape = RoundedCornerShape(4.dp)
                                            )
                                            .clickable { currentColorByLong = colorLong }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // 3. 【更新用】色を確定するボタン
                            Button(
                                onClick = {
                                    scope.launch {
                                        // 🌟 メニューの外側に出たので、currentEditingSetting を使ってコピーを作成・保存します
                                        val updatedSetting =
                                            currentEditingSetting.copy(color = currentColorByLong)
                                        db.memoDao().updateCounter(updatedSetting)

                                        showColorEditPanel = false
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
                                    "この色に変更する",
                                    color = Color.Black,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // 閉じるボタン（キャンセル）
                            Button(
                                onClick = {
                                    showColorEditPanel = false
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 16.dp)
                            ) {
                                Text("閉じる", fontSize = 18.sp)
                            }
                        }
                    }
                }
            }

            if (showAddCounterDialog) {
                // 🟢 Dialog で包むことで、Android OS に「これはダイアログだよ」と教えます。
                // これにより、戻るボタン（ジェスチャー）で正しく閉じるようになります！
                androidx.compose.ui.window.Dialog(
                    onDismissRequest = { showAddCounterDialog = false }, // 戻るキーを押したときに閉じる
                    properties = DialogProperties(
                        usePlatformDefaultWidth = false
                    )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.White.copy(alpha = 0.3f))
                            .clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() }
                            ) { showAddCounterDialog = false }, // 外側タップで閉じる
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth(0.9f)
                                .clickable(enabled = false) { }, // ダイアログ内タップで閉じない
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xFF252525)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    "追加するボタンの色を選択",
                                    color = Color.White,
                                    fontSize = 18.sp,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )

                                // 1. 原色選択 (LazyRow)
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
                                    item {
                                        Box(
                                            modifier = Modifier
                                                .size(32.dp)
                                                .background(
                                                    brush = Brush.linearGradient(
                                                        listOf(
                                                            Color.White,
                                                            Color.Gray,
                                                            Color.Black
                                                        )
                                                    ),
                                                    shape = RoundedCornerShape(4.dp)
                                                )
                                                .border(
                                                    width = if (isMonotone) 3.dp else 0.dp,
                                                    color = Color.White,
                                                    shape = RoundedCornerShape(4.dp)
                                                )
                                                .clickable { isMonotone = true }
                                        )
                                    }
                                    items(baseHues) { hue ->
                                        val isSelected = !isMonotone && selectedHue == hue
                                        Box(
                                            modifier = Modifier
                                                .size(32.dp)
                                                .background(
                                                    Color.hsl(hue, 0.8f, 0.5f),
                                                    RoundedCornerShape(4.dp)
                                                )
                                                .border(
                                                    width = if (isSelected) 3.dp else 0.dp,
                                                    color = Color.White,
                                                    shape = RoundedCornerShape(4.dp)
                                                )
                                                .clickable { isMonotone = false; selectedHue = hue }
                                        )
                                    }
                                }

                                // 2. 濃淡選択 (グリッド)
                                val lightnessLevels =
                                    listOf(0.9f, 0.8f, 0.7f, 0.6f, 0.5f, 0.4f, 0.3f, 0.2f)
                                val paletteHeight = 90
                                val spacing = 8
                                val itemHeight = ((paletteHeight - spacing) / 2) - 4

                                LazyVerticalGrid(
                                    columns = GridCells.Fixed(4),
                                    modifier = Modifier.height(paletteHeight.dp),
                                    horizontalArrangement = Arrangement.spacedBy(spacing.dp),
                                    verticalArrangement = Arrangement.spacedBy(spacing.dp),
                                    userScrollEnabled = false
                                ) {
                                    items(lightnessLevels) { level ->
                                        val colorVariant = if (isMonotone) {
                                            Color.hsl(0f, 0f, level)
                                        } else {
                                            Color.hsl(selectedHue, 0.7f, level)
                                        }
                                        val colorLong = colorVariant.toArgb().toLong()
                                        val isSelected = currentColorByLong == colorLong

                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(itemHeight.dp)
                                                .background(colorVariant, RoundedCornerShape(4.dp))
                                                .border(
                                                    width = if (isSelected) 3.dp else 0.dp,
                                                    color = Color.White,
                                                    shape = RoundedCornerShape(4.dp)
                                                )
                                                .clickable { currentColorByLong = colorLong }
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(24.dp))

                                // 3. 確定ボタン
                                Button(
                                    onClick = {
                                        scope.launch {
                                            db.memoDao().insertCounter(
                                                CounterSetting(
                                                    machineId = machineId,
                                                    name = "",
                                                    displayOrder = counterSettings.size,
                                                    color = currentColorByLong
                                                )
                                            )
                                            showAddCounterDialog = false
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
                                        "この色で追加する",
                                        color = Color.Black,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                // 💡 「閉じる」ボタンも、他のダイアログと統一感を持たせるために TextButton か、
                                // もしくは不要なら削っても大丈夫ですが、一旦そのまま機能するように残してあります
                                Button(
                                    onClick = { showAddCounterDialog = false },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 12.dp)
                                ) {
                                    Text("閉じる", fontSize = 18.sp)
                                }
                            }
                        }
                    }
                }
            }

// --- メニュー専用レイヤー (自作ガードレール) ---
            if (menuExpanded) {
                Box(
                    modifier = Modifier.fillMaxSize()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.6f))
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.White.copy(alpha = 0.3f))
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
            }
            // --- 手順4：項目移動メニュー (長押し用レイヤー) ---
            if (showColumnMenuId != null) {
                val targetIndex = columns.indexOfFirst { it.id == showColumnMenuId }
                val targetCol = columns.find { it.id == showColumnMenuId }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White.copy(alpha = 0.3f))
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

                androidx.activity.compose.BackHandler {
                    showConditionEditDialog = false
                }

                LaunchedEffect(selectedColumnIdForRule, selectedOptionForRule) {
                    // ❌ showOptionMenuName = null  <- 💡 ここからは削除！
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
                Box(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // 【1枚目：奥】黒い膜
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.6f))
                    )
                    // 画面全体を覆うBox
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.White.copy(alpha = 0.3f))
                            .clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() }) {
                                showConditionEditDialog = false
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
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
                                    val baseOpts =
                                        columns.find { it.id == targetColId }?.options
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
                            .background(Color.Black.copy(alpha = 0.6f))
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.White.copy(alpha = 0.3f))
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

                                            // 🌟 ここで同時にフラグを操作する（色変更と全く同じ！）
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
                // 🟢 画面全体を覆うレイヤー（2重の膜を表現する外枠）
                Box(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // 🟢 【1枚目：奥】システムと同じ濃さの黒い膜を手動で敷く
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.6f))
                    )

                    // 🟢 【2枚目：手前】重ねたい白の30%透過膜（外側タップで閉じる）
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.White.copy(alpha = 0.3f))
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
                            text = "すべてのメモを削除しますか？\nカウンターも0に戻ります",
                            color = mainText
                        )
                    },
                    containerColor = surfaceColor,
                    confirmButton = {
                        TextButton(onClick = {
                            scope.launch {
                                // 1. メモ履歴をリセット
                                viewModel.resetAllMemosWithHistory(machineId)

                                // 2. カウンターをリセット
                                viewModel.resetAllCountersWithHistory()

                                // 3. 画面上の表示を "0000" にリセット
                                currentRotation = "0000"
                                startRotation = "0000" // 🟢 追加
                                addRotation = "0000"   // 🟢 追加

                                // 4. データベースをリセット
                                db.memoDao().saveRotationValue(
                                    RotationValue(
                                        machineId = machineId,
                                        startRotation = "0000",
                                        currentRotation = "0000",
                                        addRotation = "0000"
                                    )
                                )

                                kotlinx.coroutines.delay(150)
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
        }

        // ==========================================
        // 💡 総回転数入力ダイアログ (3行・指定順序・中央寄せ)
        // ==========================================
        // 🟢 この行のすぐ上で変数を準備します
        var tempStart by remember { mutableStateOf(startRotation) }
        var tempCurrent by remember { mutableStateOf(currentRotation) }
        var tempAdd by remember { mutableStateOf(addRotation) }
        val haptic = LocalHapticFeedback.current

        LaunchedEffect(showRotationDialog) {
            if (showRotationDialog) {
                tempStart = startRotation
                tempCurrent = currentRotation
                tempAdd = addRotation
            }
        }
        if (showRotationDialog) {
            androidx.activity.compose.BackHandler { showRotationDialog = false }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .navigationBarsPadding()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.6f))
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White.copy(alpha = 0.3f))
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) { showRotationDialog = false },
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Surface(
                        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                        color = Color(0xFF1E1E1E),
                        shadowElevation = 8.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight()
                            .clickable(enabled = false) { }
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .border(1.dp, Color.Gray, RoundedCornerShape(4.dp))
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                // 🟢 temp 変数を使って計算するように修正
                                val total = (tempCurrent.toIntOrNull() ?: 0) +
                                        (tempAdd.toIntOrNull() ?: 0) -
                                        (tempStart.toIntOrNull() ?: 0)

                                Text(
                                    text = total.toString().padStart(4, '0'),
                                    color = Color.White,
                                    fontSize = 38.sp,
                                    fontWeight = FontWeight.Normal, // 太字からNormalに変更
                                    fontFamily = SevenSegmentFontFamily, // 🟢 共通のフォントに合わせました
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // 2. 開始・現在・加算の表示 (横並び)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                val items = listOf(
                                    "開始" to tempStart,
                                    "現在" to tempCurrent,
                                    "加算" to tempAdd
                                )
                                items.forEach { (label, value) ->
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        // 🟢 ラベルの表示名を変更
                                        Text(
                                            text = if (label == "開始") "開始(長押し)" else label,
                                            color = Color.LightGray,
                                            fontSize = 20.sp
                                        )
                                        Box(
                                            modifier = Modifier
                                                .padding(top = 4.dp)
                                                .border(
                                                    1.dp,
                                                    if (editingTargetId == when (label) {
                                                            "開始" -> "start"; "現在" -> "current"; else -> "add"
                                                        }
                                                    ) Color.Cyan else Color.Gray,
                                                    RoundedCornerShape(4.dp)
                                                )
                                                // 🟢 長押しとタップの判定ロジック
                                                .pointerInput(Unit) {
                                                    detectTapGestures(
                                                        onLongPress = {
                                                            // 🟢 振動させる
                                                            haptic.performHapticFeedback(
                                                                HapticFeedbackType.LongPress
                                                            )
                                                            if (label == "開始") {
                                                                editingTargetId = "start"
                                                            }
                                                        },
                                                        onTap = {
                                                            if (label != "開始") {
                                                                editingTargetId = when (label) {
                                                                    "現在" -> "current"
                                                                    else -> "add"
                                                                }
                                                            }
                                                        }
                                                    )
                                                }
                                                .padding(horizontal = 12.dp, vertical = 8.dp)
                                        ) {
                                            Text(
                                                text = value,
                                                color = if (editingTargetId == when (label) {
                                                        "開始" -> "start"; "現在" -> "current"; else -> "add"
                                                    }
                                                ) Color.Cyan else Color.White,
                                                fontSize = 30.sp,
                                                fontWeight = FontWeight.Normal, // 統一
                                                fontFamily = SevenSegmentFontFamily, // 🟢 共通化
                                                maxLines = 1,
                                                softWrap = false
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            SimpleTenKey(
                                onNumberClick = { num ->
                                    when (editingTargetId) {
                                        "start" -> tempStart = (tempStart + num).takeLast(4)
                                        "current" -> tempCurrent = (tempCurrent + num).takeLast(4)
                                        "add" -> tempAdd = (tempAdd + num).takeLast(4)
                                    }
                                },
                                onActionClick = { action ->
                                    if (action == "C") {
                                        when (editingTargetId) {
                                            "start" -> tempStart = "0000"
                                            "current" -> tempCurrent = "0000"
                                            "add" -> tempAdd = "0000"
                                        }
                                    } else if (action == "⏎") {
                                        // 1. 「確定」の計算：現在＋加算
                                        val newCurrent = (tempCurrent.toIntOrNull()
                                            ?: 0) + (tempAdd.toIntOrNull() ?: 0)

                                        // 2. メインの変数に反映
                                        startRotation = tempStart
                                        currentRotation = newCurrent.toString().padStart(4, '0')
                                        addRotation = "0000" // 加算はリセット

                                        // 3. データベース更新
                                        scope.launch {
                                            db.memoDao().updateAllRotationValues(
                                                machineId = machineId,
                                                start = startRotation,
                                                current = currentRotation,
                                                add = "0000"
                                            )
                                        }
                                        showRotationDialog = false
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

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

        // 1. プレビュー用のデータ作成
        val previewValues = remember(inputValues.toMap()) {
            columns.map { col ->
                MemoValue(
                    recordId = editingRecordId ?: 0,
                    columnId = col.id,
                    value = inputValues[col.id] ?: ""
                )
            }
        }

        // 2. プレビュー用の「幅」を計算
        val columnWeights = remember(columns, previewValues) {
            val maxScores = mutableMapOf<Int, Float>()
            previewValues.forEach { memoValue ->
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

        // 🟢 元通りのむき出しの Column 構造（余計な Box/Surface は全削除）
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
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
                modifier = Modifier.fillMaxWidth()
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

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                columns.forEach { column ->
                    val options = column.options
                    val currentValue = inputValues[column.id] ?: ""

                    // 選択状態の判定（値が入っているか）
                    val isFilled = currentValue.isNotBlank()

                    // ★ここから修正：TextをRowで囲ってチェックマークと並べる
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        // ★修正：丸い背景付きのチェックアイコン
                        Box(
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .size(24.dp)
                                .background(
                                    color = if (isFilled) Color(0xFF4CAF50) else Color(0xFF333333),
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = androidx.compose.material.icons.Icons.Default.Check,
                                contentDescription = "入力済み",
                                tint = if (isFilled) Color.White else Color.Transparent,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Text(
                            text = column.name,
                            style = MaterialTheme.typography.labelLarge,
                            color = Color(0xFFBB86FC),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Start
                        )
                    }
                    // ★ここまで

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
                                        val oldValue = currentValue
                                        val newValue = if (isSelected) "" else option
                                        inputValues[column.id] = newValue

                                        scope.launch {
                                            if (oldValue.isNotBlank()) {
                                                val oldRules = db.memoDao()
                                                    .getRulesByTrigger(column.id, oldValue)
                                                oldRules.forEach { rule ->
                                                    if (!rule.isNextRow && rule.targetColumnId != column.id) {
                                                        inputValues[rule.targetColumnId] = ""
                                                    }
                                                }
                                            }

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
                    }

                    if (options.isEmpty() || column.showTextField) {
                        OutlinedTextField(
                            value = currentValue,
                            onValueChange = { newValue ->
                                inputValues[column.id] = newValue

                                scope.launch {
                                    val rules = db.memoDao().getRulesByTrigger(column.id, newValue)
                                    rules.forEach { rule ->
                                        if (!rule.isNextRow && rule.targetColumnId != column.id) {
                                            inputValues[rule.targetColumnId] = rule.targetValue
                                        }
                                    }
                                }
                            },
                            placeholder = {
                                if (options.isNotEmpty()) {
                                    Text("入力欄", fontSize = 12.sp, color = subText)
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp),
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
                            val validInputs = inputValues.filter { it.value.isNotBlank() }

                            if (validInputs.isEmpty()) {
                                onSave()
                                return@launch
                            }

                            val currentRid: Int
                            val currentTimestamp: Long

                            if (editingRecordId != null) {
                                val existingRecord = db.memoDao().getRecordById(editingRecordId)
                                currentRid = editingRecordId
                                currentTimestamp =
                                    existingRecord?.timestamp ?: System.currentTimeMillis()

                                val newValues = validInputs.map { (cid, txt) ->
                                    MemoValue(recordId = currentRid, columnId = cid, value = txt)
                                }.toMutableList()

                                inputValues.forEach { (cid, txt) ->
                                    val rules = db.memoDao().getRulesByTrigger(cid, txt)
                                    rules.forEach { rule ->
                                        if (!rule.isNextRow && cid != rule.targetColumnId) {
                                            newValues.add(
                                                MemoValue(
                                                    recordId = currentRid,
                                                    columnId = rule.targetColumnId,
                                                    value = rule.targetValue
                                                )
                                            )
                                        }
                                    }
                                }

                                viewModel.updateMemoWithHistory(
                                    MemoRecord(
                                        id = currentRid,
                                        machineId = machineId,
                                        timestamp = currentTimestamp
                                    ),
                                    newValues
                                )
                            } else {
                                currentTimestamp = System.currentTimeMillis()
                                val generatedLongId = db.memoDao().insertRecord(
                                    MemoRecord(machineId = machineId, timestamp = currentTimestamp)
                                )
                                currentRid = generatedLongId.toInt()

                                val newValues = validInputs.map { (cid, txt) ->
                                    MemoValue(recordId = currentRid, columnId = cid, value = txt)
                                }.toMutableList()

                                inputValues.forEach { (cid, txt) ->
                                    val rules = db.memoDao().getRulesByTrigger(cid, txt)
                                    rules.forEach { rule ->
                                        if (!rule.isNextRow && cid != rule.targetColumnId) {
                                            newValues.add(
                                                MemoValue(
                                                    recordId = currentRid,
                                                    columnId = rule.targetColumnId,
                                                    value = rule.targetValue
                                                )
                                            )
                                        }
                                    }
                                }

                                viewModel.updateMemoWithHistory(
                                    MemoRecord(
                                        id = currentRid,
                                        machineId = machineId,
                                        timestamp = currentTimestamp
                                    ),
                                    newValues
                                )
                            }

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
                                            val newNextRid = db.memoDao()
                                                .insertRecord(MemoRecord(machineId = machineId))
                                            db.memoDao().insertValue(
                                                MemoValue(
                                                    recordId = newNextRid.toInt(),
                                                    columnId = rule.targetColumnId,
                                                    value = rule.targetValue
                                                )
                                            )
                                        }
                                    }
                                }
                            }

                            delay(150)
                            onSave()
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7E57C2))
                ) {
                    Text(
                        text = if (editingRecordId != null) "変更を保存" else "メモに追加",
                        color = mainText,
                        fontSize = 20.sp // 💡 文字サイズを20spに見やすく維持
                    )
                }

                if (editingRecordId != null) {
                    Button(
                        onClick = { showDeleteConfirmDialog = true },
                        modifier = Modifier.size(44.dp),
                        shape = RoundedCornerShape(22.dp),
                        contentPadding = PaddingValues(0.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB3261E))
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "削除",
                            tint = Color.White
                        )
                    }
                }
            }

            // --- 削除確認ダイアログ ---
            if (showDeleteConfirmDialog) {
                AlertDialog(
                    onDismissRequest = { showDeleteConfirmDialog = false },
                    title = { Text(text = "削除", color = mainText) },
                    text = { Text(text = "この行を削除しますか？", color = mainText) },
                    containerColor = Color(0xFF1E1E1E),
                    confirmButton = {
                        TextButton(onClick = {
                            scope.launch {
                                val idToDelete = editingRecordId
                                if (idToDelete != null) {
                                    viewModel.deleteMemoWithHistory(idToDelete)
                                    delay(150)
                                    onSave()
                                }
                                showDeleteConfirmDialog = false
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
                            .height(16.dp) // ★ここを追加して高さを固定
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

    @Composable
    fun SimpleTenKey(
        onNumberClick: (String) -> Unit,
        onActionClick: (String) -> Unit
    ) {
        val keys = listOf(
            listOf("1", "2", "3"),
            listOf("4", "5", "6"),
            listOf("7", "8", "9"),
            listOf("C", "0", "⏎")
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Gray, RoundedCornerShape(8.dp))
                .padding(8.dp)
        ) {
            keys.forEach { row ->
                Row(modifier = Modifier.fillMaxWidth()) {
                    row.forEach { key ->
                        // 🟢 Buttonの外側でフィードバックのインスタンスを取得します
                        val haptic = LocalHapticFeedback.current

                        Button(
                            onClick = {
                                // 🟢 ボタンを押した瞬間に振動させる
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)

                                if (key == "C" || key == "⏎") onActionClick(key)
                                else onNumberClick(key)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .padding(2.dp),
                            shape = RoundedCornerShape(percent = 50),
                            elevation = null,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF6A5ACD),
                                contentColor = Color.White
                            ),
                            contentPadding = PaddingValues(12.dp)
                        ) {
                            if (key == "⏎") {
                                Icon(
                                    imageVector = Icons.Filled.SubdirectoryArrowLeft,
                                    contentDescription = "確定",
                                    modifier = Modifier.size(36.dp) // 🟢 サイズを固定します
                                )
                            } else {
                                Box(
                                    modifier = Modifier.height(36.dp), // 🟢 高さをBoxで統一
                                    contentAlignment = Alignment.Center // 🟢 中身を中央揃え
                                ) {
                                    Text(
                                        text = key,
                                        fontSize = 36.sp,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // 🟢 修正：他の関数の外側（クラスの末尾）に配置して赤線を解消
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)

        if (intent.action == "ACTION_OPEN_MEMO_FROM_OVERLAY") {
            val targetId = intent.getIntExtra("TARGET_MACHINE_ID", -1)
            if (targetId != -1) {
                // Compose側に「画面をメモ画面に切り替えて、このIDを表示せよ」と通知
                onOverlayNavigationRequested?.invoke(targetId)
            }
        }
    }

    // 🟢 追加：ファイルの最後、閉じカッコの直前にこの関数を貼り付けます
    fun checkAndRequestNotificationPermission(onPermissionGranted: () -> Unit) {
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            val hasPermission = androidx.core.content.ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED

            if (!hasPermission) {
                androidx.core.app.ActivityCompat.requestPermissions(
                    this,
                    arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                    101
                )
            } else {
                onPermissionGranted()
            }
        } else {
            onPermissionGranted()
        }
    }
} // 💡 これがファイルの一番最後にある、クラス全体の閉じカッコです