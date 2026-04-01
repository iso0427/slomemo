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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
        var currentScreen by remember { mutableStateOf("main") } // "main" か "settings" を入れる
        var columns by remember { mutableStateOf(listOf<ColumnSetting>()) }
        var records by remember { mutableStateOf(listOf<MemoRecord>()) }
        var showSheet by remember { mutableStateOf(false) }
        var newColumnName by remember { mutableStateOf("") }
        val scope = rememberCoroutineScope()
        var selectedColumnId by remember { mutableStateOf<Int?>(null) }
        var newOptionName by remember { mutableStateOf("") }
        val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
        val view = androidx.compose.ui.platform.LocalView.current

        if (!view.isInEditMode) {
            androidx.compose.runtime.SideEffect {
                val window = (view.context as android.app.Activity).window
                // ステータスバーとナビゲーションバーを「黒」に固定
                window.statusBarColor = android.graphics.Color.BLACK
                window.navigationBarColor = android.graphics.Color.BLACK

                // 文字を「白」にする
                val controller = androidx.core.view.WindowCompat.getInsetsController(window, view)
                controller.isAppearanceLightStatusBars = false
                controller.isAppearanceLightNavigationBars = false
            }
        }


        fun refreshData() {
            scope.launch {
                columns = db.memoDao().getAllColumns()
                records = db.memoDao().getAllRecords()
            }
        }
        LaunchedEffect(Unit) { refreshData() }

        // --- 右側からメニューを出すための設定 ---
        CompositionLocalProvider(androidx.compose.ui.platform.LocalLayoutDirection provides androidx.compose.ui.unit.LayoutDirection.Rtl) {
            ModalNavigationDrawer(
                drawerState = drawerState,
                drawerContent = {
                    // ↓ ここから
                    CompositionLocalProvider(androidx.compose.ui.platform.LocalLayoutDirection provides androidx.compose.ui.unit.LayoutDirection.Ltr) {
                        ModalDrawerSheet(modifier = Modifier.width(300.dp)) {
                            Text(
                                "メニュー",
                                modifier = Modifier.padding(16.dp),
                                style = MaterialTheme.typography.titleLarge
                            )
                            Divider()

                            androidx.compose.material3.TextButton(onClick = { scope.launch { drawerState.close() } }) {
                                Text(
                                    "実戦データ入力",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp)
                                )
                            }

                            androidx.compose.material3.TextButton(onClick = { scope.launch { drawerState.close() } }) {
                                Text(
                                    "項目・選択肢の設定",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp)
                                )
                            }
                        }
                    }
                }
            ) {
                // ★ここを追加：メイン画面は「左から右(Ltr)」に戻す！
                CompositionLocalProvider(androidx.compose.ui.platform.LocalLayoutDirection provides androidx.compose.ui.unit.LayoutDirection.Ltr) {

                    // 外側のBoxで画面全体の背景（ステータスバーの裏側）を黒にする
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black)
                    ) {
                        Scaffold(
                            modifier = Modifier
                                .fillMaxSize()
                                .statusBarsPadding(), // ★ここで「ステータスバーの下から」と指定

                            // ここを背景色（白など）に戻す
                            containerColor = MaterialTheme.colorScheme.background,

                            topBar = {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .statusBarsPadding()
                                        .background(Color.White) // 背景を白に
                                        .padding(horizontal = 16.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "実戦データ",
                                        style = MaterialTheme.typography.titleLarge,
                                        color = Color.Black
                                    )

                                    // メニュー部分
                                    Box {
                                        var expanded by remember { mutableStateOf(false) }

                                        IconButton(
                                            onClick = { expanded = true },
                                            // ★「≡」ボタン自体の位置を Offset で調整
                                            // x をプラスにすると右へ、マイナスにすると左へ動きます
                                            modifier = Modifier.offset(x = (12).dp, y = 0.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Menu,
                                                contentDescription = "メニュー",
                                                tint = Color.Black
                                            )
                                        }

                                        DropdownMenu(
                                            expanded = expanded,
                                            onDismissRequest = { expanded = false },
                                            // ★メニュー窓の位置を Offset で調整
                                            // ボタンの位置をずらした分、ここも調整すると綺麗に重なります
                                            offset = androidx.compose.ui.unit.DpOffset(x = (-12).dp, y = 0.dp),
                                            modifier = Modifier.background(Color.White)
                                        ) {
                                            val menuTextStyle = androidx.compose.ui.text.TextStyle(
                                                fontSize = 18.sp,
                                                color = Color.Black
                                            )

                                            DropdownMenuItem(
                                                text = { Text("実戦データ入力", style = menuTextStyle) },
                                                leadingIcon = { Icon(Icons.Default.Add, null, tint = Color.Gray) },
                                                onClick = { expanded = false }
                                            )
                                            DropdownMenuItem(
                                                text = { Text("項目・選択肢の設定", style = menuTextStyle) },
                                                leadingIcon = { Icon(Icons.Default.Settings, null, tint = Color.Gray) },
                                                onClick = { expanded = false }
                                            )
                                        }
                                    }
                                }
                            },


                        ) { padding ->
                            Column(
                                modifier = Modifier
                                    .padding(padding)
                                    .fillMaxSize()
                            ) {
                                // ヘッダー
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

                                // 履歴リスト
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
}