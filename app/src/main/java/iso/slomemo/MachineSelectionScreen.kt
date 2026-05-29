package iso.slomemo

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MachineSelectionScreen(
    db: AppDatabase,
    onMachineSelected: (Int) -> Unit
) {
    val backColor = Color.Black    // 画面全体の背景
    val surfaceColor = Color(0xFF1E1E1E) // メニューやタイルの箱
    val mainText = Color.White           // メインの文字
    val subText = Color.LightGray        // 補足の文字

    val machines by db.machineDao().getAllMachines().collectAsState(initial = null)
    var newMachineName by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    val context = LocalContext.current

    val createCsvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        uri?.let { targetUri ->
            scope.launch {
                try {
                    val allMachines = db.machineDao().getAllMachinesOnce()

                    val csvString = StringBuilder().apply {
                        // ヘッダー行
                        append("TYPE,ID,PARENT_ID,NAME,VALUE1,VALUE2,ORDER,BOOL\n")

                        allMachines.forEach { machine ->
                            // 1. 機種データ
                            append("MACHINE,${machine.id},0,${machine.name},,,${machine.position},\n")

                            // 2. その機種に紐づく項目（ColumnSetting）を取得
                            val columns = db.memoDao().getColumnsByMachineDirect(machine.id)
                            columns.forEach { column ->
                                val optionsString = column.options.joinToString("|")
                                append("COLUMN,${column.id},${machine.id},${column.name},\"${optionsString}\",,${column.displayOrder},${column.showTextField}\n")

                                // 3. 項目に紐づく選択肢（SelectionOption）を取得
                                val options = db.memoDao().getOptionsByColumn(column.id)
                                options.forEach { option ->
                                    append("OPTION,${option.id},${column.id},${option.optionName},,,,\n")
                                }
                            }

                            // 🟢 【修正版】4. その機種に紐づくカウンター設定（CounterSetting）を取得して書き出す
                            // machine.id が Long型の場合を考慮して、.toInt() で型を合わせています
                            val counterSettings =
                                db.memoDao().getCounterSettingsByMachineDirect(machine.id.toInt())
                            counterSettings.forEach { setting ->
                                // COUNTER_SETTING, ID, 機種ID, 名前, 色(Hex値), 計算タイプ, 計算対象タイプ, 計算対象カウンターID
                                append("COUNTER_SETTING,${setting.id},${machine.id},${setting.name},${setting.color},${setting.calcType},${setting.targetType},${setting.targetCounterId ?: ""}\n")
                            }
                        }

                        // バックアップ側の RULE ループ（5. 連動ルール）
                        val rules = db.memoDao().getAllAutoInputRules()
                        rules.forEach { rule ->
                            append("RULE,${rule.id},,${rule.triggerValue},${rule.targetValue},${rule.targetColumnId},${rule.triggerColumnId},${rule.isNextRow}\n")
                        }
                    }.toString()

                    context.contentResolver.openOutputStream(targetUri)?.use { stream ->
                        stream.write(csvString.toByteArray(Charsets.UTF_8))
                    }
                    println("詳細バックアップ成功")
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    val importCsvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { targetUri ->
            scope.launch {
                try {
                    context.contentResolver.openInputStream(targetUri)?.use { inputStream ->
                        val reader = inputStream.bufferedReader()
                        val lines = reader.readLines()
                        if (lines.isEmpty()) return@launch

                        // 1. 既存データの削除（全入れ替えパターン）
                        db.machineDao().deleteAllMachines()
                        db.memoDao().deleteAllRecords()
                        db.memoDao().deleteAllMemoValues()

                        // 🟢 【追加】カウンター設定と現在のカウント値も一旦すべてリセット
                        db.memoDao().deleteAllCounterSettings() // ※この関数の有無は後述
                        db.memoDao().deleteAllCounterValues()   // ※この関数の有無は後述

                        // ルールも一旦リセットする場合
                        db.memoDao().getAllAutoInputRules().forEach {
                            db.memoDao().deleteRulesByTriggerColumn(it.triggerColumnId)
                        }

                        // IDの紐付け直し用マップ
                        val machineIdMap = mutableMapOf<Int, Int>() // 旧ID -> 新ID
                        val columnIdMap = mutableMapOf<Int, Int>()  // 旧ID -> 新ID
                        // 🟢 【追加】カウンターの旧ID -> 新IDのマッピング（計算相手の特定用）
                        val counterIdMap = mutableMapOf<Int, Int>()

                        // 復元後に「他のボタンを計算対象にしている設定」をまとめて更新するためのリスト
                        val pendingTargets = mutableListOf<Pair<Int, Int>>() // 新カウンターID -> 旧ターゲットカウンターID

                        // 2. 解析開始
                        lines.drop(1).forEach { line ->
                            val tokens = line.split(",")
                            if (tokens.size < 8) return@forEach

                            val type = tokens[0]
                            val oldId = tokens[1].toIntOrNull() ?: 0
                            val parentId = tokens[2].toIntOrNull() ?: 0
                            val name = tokens[3].replace("\"", "")

                            when (type) {
                                "MACHINE" -> {
                                    val newId = db.machineDao().insertMachine(
                                        Machine(
                                            name = name,
                                            position = tokens[6].toIntOrNull() ?: 0
                                        )
                                    ).toInt()
                                    machineIdMap[oldId] = newId
                                }

                                "COLUMN" -> {
                                    val newMachineId = machineIdMap[parentId] ?: return@forEach

                                    val optionsList = tokens[4].replace("\"", "").let {
                                        if (it.isEmpty()) emptyList<String>() else it.split("|")
                                    }

                                    val newId = db.memoDao().insertColumnWithIdReturn(
                                        ColumnSetting(
                                            machineId = newMachineId,
                                            name = name,
                                            options = optionsList,
                                            displayOrder = tokens[6].toIntOrNull() ?: 0,
                                            showTextField = tokens[7].toBoolean()
                                        )
                                    ).toInt()
                                    columnIdMap[oldId] = newId
                                }

                                "OPTION" -> {
                                    val newColumnId = columnIdMap[parentId] ?: return@forEach
                                    db.memoDao().insertSelectionOption(
                                        SelectionOption(
                                            columnId = newColumnId,
                                            optionName = name
                                        )
                                    )
                                }

                                // 🟢 【追加】CSVからカウンター設定を読み込んでデータベースに保存
                                "COUNTER_SETTING" -> {
                                    val newMachineId = machineIdMap[parentId] ?: return@forEach
                                    val oldTargetCounterId = tokens[7].toIntOrNull()

                                    // まずはターゲットIDを空（null）の状態でインサートして新しいIDを発行してもらう
                                    val newCounterId = db.memoDao().insertCounterSettingReturnId(
                                        CounterSetting(
                                            machineId = newMachineId,
                                            name = name,
                                            color = tokens[4].toLongOrNull() ?: 0xFFBB86FC,
                                            calcType = tokens[5].toIntOrNull() ?: 0,
                                            targetType = tokens[6].toIntOrNull() ?: 0,
                                            targetCounterId = null // 後から紐付け直す
                                        )
                                    ).toInt()

                                    // マップに登録
                                    counterIdMap[oldId] = newCounterId

                                    // もし計算相手が「他のボタン」だったら、後で紐付け直すためにリストにメモ
                                    if (tokens[6].toIntOrNull() == 1 && oldTargetCounterId != null) {
                                        pendingTargets.add(Pair(newCounterId, oldTargetCounterId))
                                    }

                                    // 履歴（値）はリセット状態にするので、初期値 0 でテーブルを作る
                                    db.memoDao().insertCounterValue(CounterValue(counterId = newCounterId, count = 0))
                                }

                                "RULE" -> {
                                    val oldTriggerId = tokens[6].toIntOrNull() ?: 0
                                    val oldTargetId = tokens[5].toIntOrNull() ?: 0

                                    val newTriggerId = columnIdMap[oldTriggerId] ?: return@forEach
                                    val newTargetId = columnIdMap[oldTargetId] ?: 0

                                    db.memoDao().insertAutoInputRule(
                                        AutoInputRule(
                                            triggerColumnId = newTriggerId,
                                            triggerValue = name,
                                            targetColumnId = newTargetId,
                                            targetValue = tokens[4],
                                            isNextRow = tokens[7].toBoolean()
                                        )
                                    )
                                }
                            }
                        }

                        // 🟢 【追加】すべてのカウンターを入れ終わった後、計算対象のIDを新しいIDに紐付け直す
                        pendingTargets.forEach { (newCounterId, oldTargetId) ->
                            val newTargetId = counterIdMap[oldTargetId]
                            if (newTargetId != null) {
                                db.memoDao().updateCounterTargetId(newCounterId, newTargetId)
                            }
                        }
                    }
                    android.widget.Toast.makeText(
                        context,
                        "データを復元しました",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                } catch (e: Exception) {
                    e.printStackTrace()
                    android.widget.Toast.makeText(
                        context,
                        "インポートに失敗しました",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    // ダイアログの状態管理
    var showEditDialog by remember { mutableStateOf(false) }
    var machineToEdit by remember { mutableStateOf<Machine?>(null) }
    var editNameText by remember { mutableStateOf("") }

    var showActionDialog by remember { mutableStateOf(false) }
    var selectedMachine by remember { mutableStateOf<Machine?>(null) }

    // ダイアログ用の状態（画面のCompose関数の冒頭などで定義）
    var showAddDialog by remember { mutableStateOf(false) }

    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    var menuExpanded by remember { mutableStateOf(false) }

    androidx.activity.compose.BackHandler(enabled = menuExpanded) {
        menuExpanded = false
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = backColor
        ) { padding ->
            Column(
                modifier = Modifier
                    .padding(padding)
                // .padding(64.dp) ← ここが全体にかかっているので、メニューを置くために調整が必要
            ) {
                // ★ 右上のメニューボタン用のRowを追加
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.End // 右寄せ
                ) {
                    Box {
                        IconButton(
                            onClick = { menuExpanded = true },
                            modifier = Modifier.offset(x = 12.dp)
                        ) {
                            Icon(Icons.Default.Menu, null, tint = mainText)
                        }
                    }
                }

                // ここから下のコンテンツは、元の 64.dp パディングを意識して調整
                Column(modifier = Modifier.padding(horizontal = 64.dp)) {

                    // ロゴ画像を表示
                    Image(
                        painter = painterResource(id = R.drawable.logo_slomemo),
                        contentDescription = "SloMemo Logo",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            // ★ここを修正：graphicsLayer でブレンドモードを指定します
                            .graphicsLayer(alpha = 0.99f) // 一部の端末でブレンドを正しく効かせるおまじない
                            .drawWithCache {
                                onDrawWithContent {
                                    drawContent()
                                    // ここでブレンドモードを適用
                                    // 黒背景のみが透過され、光だけが残ります
                                }
                            }
                            // もっと単純にやるならこれだけでもOKな場合が多いです：
                            .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen),
                        contentScale = ContentScale.Fit
                    )

                    Spacer(modifier = Modifier.height(48.dp))

                    // 機種一覧リスト
                    LazyColumn {
                        // データ読み込み中（null）はチラつき防止のため何も表示しない
                        if (machines != null) {

                            // A. 機種リストの表示
                            items(machines!!) { machine ->
                                val interactionSource = remember { MutableInteractionSource() }
                                val isPressed by interactionSource.collectIsPressedAsState()
                                var isActuallyPressed by remember { mutableStateOf(false) }

                                val isSelected =
                                    selectedMachine?.id == machine.id && showActionDialog

                                LaunchedEffect(isActuallyPressed) {
                                    if (isActuallyPressed) {
                                        kotlinx.coroutines.delay(200)
                                        isActuallyPressed = false
                                    }
                                }

                                val buttonBrush = when {
                                    isSelected || isActuallyPressed -> Brush.verticalGradient(
                                        listOf(Color(0xFFEADDFF), Color(0xFFC0A0FF))
                                    )

                                    isPressed -> Brush.verticalGradient(
                                        listOf(Color(0xFF444444), Color(0xFF222222))
                                    )

                                    else -> Brush.verticalGradient(
                                        listOf(
                                            Color(0xFF555555),
                                            Color(0xFF333333)
                                        )
                                    )
                                }

                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color.Transparent,
                                    shadowElevation = if (isActuallyPressed || isSelected) 12.dp else 4.dp
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .background(brush = buttonBrush)
                                            .combinedClickable(
                                                interactionSource = interactionSource,
                                                indication = null,
                                                onClick = {
                                                    isActuallyPressed = true
                                                    scope.launch {
                                                        kotlinx.coroutines.delay(100)
                                                        onMachineSelected(machine.id)
                                                    }
                                                },
                                                onLongClick = {
                                                    selectedMachine = machine
                                                    showActionDialog = true
                                                }
                                            )
                                            .padding(20.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = machine.name,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 30.sp,
                                            color = if (isSelected || isActuallyPressed) Color(
                                                0xFF152200
                                            ) else mainText
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

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
                        .clickable { menuExpanded = false } // 背景タップで閉じる
                ) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 80.dp, end = 4.dp)
                            .width(220.dp),
                        shape = RoundedCornerShape(5.dp),
                        color = surfaceColor,
                        shadowElevation = 8.dp
                    ) {
                        Column(modifier = Modifier.padding(vertical = 8.dp)) {
                            MenuRow(
                                icon = Icons.Default.Edit,
                                label = "新規機種を登録",
                                fontSize = 18.sp,
                                onClick = {
                                    showAddDialog = true
                                    menuExpanded = false
                                },
                                mainText = mainText
                            )
                            MenuRow(
                                icon = Icons.Default.ArrowForward,
                                label = "バックアップ(CSV)",
                                fontSize = 18.sp,
                                onClick = {
                                    menuExpanded = false

                                    // 今日の日付を取得 (例: 20260507_1230)
                                    val timeStamp = java.text.SimpleDateFormat(
                                        "yyyyMMdd_HHmm",
                                        java.util.Locale.getDefault()
                                    ).format(java.util.Date())
                                    val fileName = "slomemo_backup_$timeStamp.csv"

                                    createCsvLauncher.launch(fileName)
                                },
                                mainText = mainText
                            )

                            MenuRow(
                                icon = Icons.Default.ArrowBack, // インポートっぽく「戻る」矢印
                                label = "インポート(CSV)",
                                fontSize = 18.sp,
                                onClick = {
                                    menuExpanded = false
                                    // CSVファイルだけを選択できるように制限して起動
                                    importCsvLauncher.launch(arrayOf("text/csv"))
                                },
                                mainText = mainText
                            )
                        }
                    }
                }
            }
        }
    }

    // --- 入力用ダイアログ (自作レイヤー・デザイン/色/サイズ完全統一版) ---
    if (showAddDialog) {
        androidx.activity.compose.BackHandler {
            showAddDialog = false
        }
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            // 【1枚目：奥】システムと同じ濃さの黒い膜を手動で敷く
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.6f))
            )
            // 【2枚目：手前】重ねたい白の30%透過膜（外側タップで閉じる）
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White.copy(alpha = 0.3f))
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) {
                        showAddDialog = false
                    },
                contentAlignment = Alignment.Center
            ) {
                // AlertDialogの代わりにSurfaceを使ってダイアログの見た目を作る
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF1E1E1E), // 🟢 surfaceColor から 総回転数と同じ「0xFF1E1E1E」に変更！
                    shadowElevation = 8.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(28.dp)
                        .clickable(enabled = false) { }
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "新規機種登録",
                            color = mainText,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(
                            value = newMachineName,
                            onValueChange = { newMachineName = it },
                            textStyle = androidx.compose.ui.text.TextStyle(
                                fontSize = 20.sp,
                                color = Color.White
                            ),
                            placeholder = {
                                Text("機種名を入力", fontSize = 16.sp, color = subText)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedContainerColor = Color(0xFF252525), // 🟢 入力欄の背景もメモ入力と同じ深めのグレーに統一
                                unfocusedContainerColor = Color(0xFF252525), // 🟢 同上
                                cursorColor = Color.White,
                                focusedBorderColor = Color(0xFF7E57C2), // 🟢 フォーカス時の枠線をテーマカラーの紫に
                                unfocusedBorderColor = Color.Transparent
                            )
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        // ボタン配置エリア
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(onClick = { showAddDialog = false }) {
                                Text("キャンセル", color = mainText, fontSize = 18.sp)
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Button(
                                onClick = {
                                    if (newMachineName.isNotBlank()) {
                                        val name = newMachineName
                                        newMachineName = ""
                                        scope.launch {
                                            val currentMachines = machines ?: emptyList()
                                            val updatedList = currentMachines.map { it.copy(position = it.position + 1) }
                                            db.machineDao().updateMachines(updatedList)
                                            db.machineDao().insertMachine(Machine(name = name, position = 0))
                                            showAddDialog = false
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF7E57C2)
                                )
                            ) {
                                Text("追加", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }

    // --- 2. タイル型アクションダイアログ (自作レイヤー・デザイン/背景色完全統一版) ---
    if (showActionDialog && selectedMachine != null) {
        // 🟢 システム戻るボタンで閉じられるように対策
        androidx.activity.compose.BackHandler {
            showActionDialog = false
        }

        MachineActionDialog(
            selectedMachine = selectedMachine!!,
            allMachines = machines ?: emptyList(),
            onDismiss = { showActionDialog = false },
            onRename = {
                showActionDialog = false
                machineToEdit = selectedMachine
                editNameText = selectedMachine!!.name
                showEditDialog = true
            },
            onDelete = {
                showDeleteConfirmDialog = true
                showActionDialog = false
            },
            db = db,
            scope = scope,
            onRefresh = { }
        )
    }

    // --- 3. 名前編集用入力ダイアログ (自作レイヤー・デザイン完全統一版) ---
    if (showEditDialog && machineToEdit != null) {
        androidx.activity.compose.BackHandler {
            showEditDialog = false
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
                        showEditDialog = false
                    },
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF1E1E1E),
                    shadowElevation = 8.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(28.dp)
                        .clickable(enabled = false) { }
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "機種名の編集",
                            color = mainText,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(
                            value = editNameText,
                            onValueChange = { editNameText = it },
                            textStyle = androidx.compose.ui.text.TextStyle(
                                fontSize = 20.sp,
                                color = Color.White
                            ),
                            placeholder = {
                                Text("機種名を入力", fontSize = 16.sp, color = subText)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedContainerColor = Color(0xFF252525), // 🟢 252525のグレーに統一
                                unfocusedContainerColor = Color(0xFF252525),
                                cursorColor = Color.White,
                                focusedBorderColor = Color(0xFF7E57C2),
                                unfocusedBorderColor = Color.Transparent
                            )
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(onClick = { showEditDialog = false }) {
                                Text("キャンセル", color = mainText, fontSize = 18.sp)
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Button(
                                onClick = {
                                    if (editNameText.isNotBlank()) {
                                        scope.launch {
                                            db.machineDao()
                                                .updateMachine(machineToEdit!!.copy(name = editNameText))
                                            showEditDialog = false
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF7E57C2)
                                )
                            ) {
                                Text(
                                    "保存",
                                    color = Color.White,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // --- 4. 削除確認用ダイアログ (自作レイヤー化・デザイン完全統一版) ---
    if (showDeleteConfirmDialog && selectedMachine != null) {
        // 🟢 戻るボタン操作をキャッチ
        androidx.activity.compose.BackHandler {
            showDeleteConfirmDialog = false
        }

        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            // 【1枚目：奥】システムと同じ濃さの黒い膜
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.6f))
            )
            // 【2枚目：手前】重ねたい白の30%透過膜（外側タップで閉じる）
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White.copy(alpha = 0.3f))
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) {
                        showDeleteConfirmDialog = false
                    },
                contentAlignment = Alignment.Center
            ) {
                // 🟢 システムの標準 Dialog + AlertDialog を完全に廃止し、Surface 構造に落とし込んで色を完全統一
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF1E1E1E), // 🟢 完全に同じダークグレーに統一
                    shadowElevation = 8.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(28.dp) // 🟢 横幅サイズを 28.dp パディングに統一
                        .clickable(enabled = false) { }
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "機種の削除",
                            color = mainText,
                            fontSize = 22.sp, // 🟢 文字サイズ拡大
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "「${selectedMachine!!.name}」を削除してもよろしいですか？\n\n※この機種に含まれるすべてのメモも完全に削除されます。",
                            color = mainText,
                            fontSize = 18.sp, // 🟢 文字サイズを18spに拡大
                            lineHeight = 24.sp
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(
                                onClick = { showDeleteConfirmDialog = false }
                            ) {
                                Text(text = "キャンセル", color = mainText, fontSize = 18.sp)
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Button(
                                onClick = {
                                    scope.launch {
                                        db.machineDao().deleteMachine(selectedMachine!!)
                                        showDeleteConfirmDialog = false
                                        showActionDialog = false
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFB3261E) // 警告の赤色
                                )
                            ) {
                                Text(
                                    text = "削除",
                                    color = Color.White,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// 🟢 タイル型アクションダイアログ本体の背景レイヤーを修正
@Composable
fun MachineActionDialog(
    selectedMachine: Machine,
    allMachines: List<Machine>,
    onDismiss: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    db: AppDatabase,
    scope: kotlinx.coroutines.CoroutineScope,
    onRefresh: () -> Unit
) {
    val currentIndex = allMachines.indexOfFirst { it.id == selectedMachine.id }

    // 🟢 2重の自作透過レイヤー（黒0.6 ＋ 白0.3）をここに適用。
    // これにより、長押しした瞬間から2枚目（並び替え）と100%同じ背景色に変わります！
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
                ) { onDismiss() }, // 外側をタップしたら閉じる
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                shadowElevation = 8.dp,
                color = Color(0xFF252525), // タイル部分の背景はそのまま維持
                modifier = Modifier
                    .width(180.dp)
                    .clickable(enabled = false) { }
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "「${selectedMachine.name}」",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(bottom = 20.dp)
                    )

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
                        Button(
                            onClick = {
                                val list = allMachines.toMutableList()
                                val item = list.removeAt(currentIndex)
                                list.add(currentIndex - 1, item)

                                scope.launch {
                                    val updatedList = list.mapIndexed { index, machine ->
                                        machine.copy(position = index)
                                    }
                                    db.machineDao().updateMachines(updatedList)
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
                                Icon(
                                    Icons.Default.ArrowBack,
                                    null,
                                    modifier = Modifier.graphicsLayer(rotationZ = 90f),
                                    tint = Color.Black
                                )
                                Text("上へ", fontSize = 16.sp, color = Color.Black)
                            }
                        }

                        Button(
                            onClick = {
                                val list = allMachines.toMutableList()
                                val item = list.removeAt(currentIndex)
                                list.add(currentIndex + 1, item)

                                scope.launch {
                                    val updatedList = list.mapIndexed { index, machine ->
                                        machine.copy(position = index)
                                    }
                                    db.machineDao().updateMachines(updatedList)
                                }
                            },
                            enabled = currentIndex < allMachines.size - 1,
                            modifier = Modifier
                                .weight(1f)
                                .height(60.dp),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(0.dp),
                            colors = if (currentIndex < allMachines.size - 1) canMoveColors else cannotMoveColors
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Default.ArrowForward,
                                    null,
                                    modifier = Modifier.graphicsLayer(rotationZ = 90f),
                                    tint = Color.Black
                                )
                                Text("下へ", fontSize = 16.sp, color = Color.Black)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = onRename,
                            modifier = Modifier
                                .weight(1f)
                                .height(60.dp),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(0.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6750A4))
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Edit, null, tint = Color.White)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text("編集", fontSize = 16.sp, color = Color.White)
                            }
                        }

                        Button(
                            onClick = { onDelete() },
                            modifier = Modifier
                                .weight(1f)
                                .height(60.dp),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(0.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB3261E))
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Delete, null, tint = Color.White)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text("削除", fontSize = 16.sp, color = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ActionTile(label: String, icon: ImageVector, bgColor: Color, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .size(80.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = bgColor)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = null, tint = Color.White)
            Text(text = label, color = Color.White, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
fun MenuRow(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    mainText: Color,
    fontSize: androidx.compose.ui.unit.TextUnit = 14.sp
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = mainText, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Text(label, color = mainText, fontSize = fontSize)
    }
}

suspend fun importFromCsv(lines: List<String>, db: AppDatabase) {
    db.machineDao().deleteAllMachines()
    db.memoDao().deleteAllRecords()
    db.memoDao().deleteAllMemoValues()

    val machineIdMap = mutableMapOf<Int, Int>()
    val columnIdMap = mutableMapOf<Int, Int>()

    lines.drop(1).forEach { line ->
        val tokens = line.split(",")
        if (tokens.size < 4) return@forEach

        val type = tokens[0]
        val oldId = tokens[1].toIntOrNull() ?: 0
        val parentId = tokens[2].toIntOrNull() ?: 0
        val name = tokens[3].replace("\"", "")

        when (type) {
            "MACHINE" -> {
                val newId = db.machineDao().insertMachine(
                    Machine(name = name, position = tokens[6].toIntOrNull() ?: 0)
                ).toInt()
                machineIdMap[oldId] = newId
            }
            "COLUMN" -> {
                val newMachineId = machineIdMap[parentId] ?: return@forEach
                val newId = db.memoDao().insertColumnWithIdReturn(
                    ColumnSetting(
                        machineId = newMachineId,
                        name = name,
                        displayOrder = tokens[6].toIntOrNull() ?: 0,
                        showTextField = tokens[7].toBoolean()
                    )
                ).toInt()
                columnIdMap[oldId] = newId
            }
            "OPTION" -> {
                // オプション復元
            }
            "RULE" -> {
                // ルール復元
            }
        }
    }
}