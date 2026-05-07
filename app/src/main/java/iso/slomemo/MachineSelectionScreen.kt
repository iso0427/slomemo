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
import androidx.compose.material3.AlertDialog
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
                        // ヘッダー行（何のデータか判別するためのType列を追加）
                        append("TYPE,ID,PARENT_ID,NAME,VALUE1,VALUE2,ORDER,BOOL\n")

                        allMachines.forEach { machine ->
                            // 1. 機種データ
                            append("MACHINE,${machine.id},0,${machine.name},,,${machine.position},\n")

                            // 2. その機種に紐づく項目（ColumnSetting）を取得
                            val columns = db.memoDao().getColumnsByMachineDirect(machine.id)
                            columns.forEach { column ->
                                append("COLUMN,${column.id},${machine.id},${column.name},,,${column.displayOrder},${column.showTextField}\n")

                                // 3. 項目に紐づく選択肢（SelectionOption）を取得
                                val options = db.memoDao().getOptionsByColumn(column.id)
                                options.forEach { option ->
                                    append("OPTION,${option.id},${column.id},${option.optionName},,,,\n")
                                }
                            }
                        }

                        // 4. 連動ルール（AutoInputRule）をすべて書き出す
                        val rules = db.memoDao().getAllAutoInputRules()
                        rules.forEach { rule ->
                            append("RULE,${rule.id},,${rule.triggerValue},${rule.targetValue},,${rule.triggerColumnId},${rule.isNextRow}\n")
                            // ※targetColumnIdなどの予備情報は必要に応じて列を追加してください
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
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White.copy(alpha = 0.2f))
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
                                val timeStamp = java.text.SimpleDateFormat("yyyyMMdd_HHmm", java.util.Locale.getDefault()).format(java.util.Date())
                                val fileName = "slomemo_backup_$timeStamp.csv"

                                createCsvLauncher.launch(fileName)
                            },
                            mainText = mainText
                        )
                    }
                }
            }
        }
    }

    // --- 入力用ダイアログ ---
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            containerColor = surfaceColor, // ★ デザイン統一
            title = { Text("新規機種登録", color = mainText) }, // ★ デザイン統一
            text = {
                OutlinedTextField(
                    value = newMachineName,
                    onValueChange = { newMachineName = it },
                    placeholder = {
                        Text("機種名を入力", fontSize = 14.sp, color = subText)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = Color(0xFF474747), // ★ 編集と同じ明るいグレー
                        unfocusedContainerColor = Color(0xFF474747),
                        cursorColor = Color.White,
                        focusedBorderColor = Color.Gray,
                        unfocusedBorderColor = Color.Transparent
                    )
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newMachineName.isNotBlank()) {
                            val name = newMachineName
                            newMachineName = ""
                            scope.launch {
                                // machines が null の場合は空リストとして扱うように修正
                                val currentMachines = machines ?: emptyList()

                                val updatedList =
                                    currentMachines.map { it.copy(position = it.position + 1) }

                                db.machineDao().updateMachines(updatedList)
                                db.machineDao()
                                    .insertMachine(Machine(name = name, position = 0))
                                showAddDialog = false
                            }
                        }
                    }
                ) {
                    Text(
                        "追加",
                        color = Color(0xFF7E57C2),
                        fontWeight = FontWeight.Bold
                    ) // ★ 文字色を紫に
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("キャンセル", color = mainText) // ★ キャンセルは白系で統一
                }
            }
        )
    }

    // ★ タイル型ダイアログの表示（ここを修正！）
    if (showActionDialog && selectedMachine != null) {
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
                showActionDialog = false
                scope.launch {
                    db.machineDao().deleteMachine(selectedMachine!!)
                }
            },
            db = db, // ★追加
            scope = scope, // ★追加
            onRefresh = { /* 必要ならここに更新処理 */ } // ★追加
        )
    }

    // 1. 名前編集用入力ダイアログ
    if (showEditDialog && machineToEdit != null) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            containerColor = surfaceColor,
            title = { Text("機種名の編集", color = mainText) },
            text = {
                OutlinedTextField(
                    value = editNameText,
                    onValueChange = { editNameText = it },
                    placeholder = { Text("機種名を入力", fontSize = 14.sp, color = subText) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = Color(0xFF474747),
                        unfocusedContainerColor = Color(0xFF474747),
                        cursorColor = Color.White,
                        focusedBorderColor = Color.Gray,
                        unfocusedBorderColor = Color.Transparent
                    )
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (editNameText.isNotBlank()) {
                            scope.launch {
                                db.machineDao()
                                    .updateMachine(machineToEdit!!.copy(name = editNameText))
                                showEditDialog = false
                            }
                        }
                    }
                ) {
                    Text("保存", color = Color(0xFF7E57C2), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text("キャンセル", color = mainText)
                }
            }
        )
    }

    // 2. 削除確認用ダイアログ
    if (showDeleteConfirmDialog && selectedMachine != null) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            properties = androidx.compose.ui.window.DialogProperties(
                // システム標準の暗転（Dim）を有効にし、画面全体に広げる
                usePlatformDefaultWidth = true,
                decorFitsSystemWindows = true
            )
        ) {
            // Boxを削除し、直接AlertDialogを配置。これで「寸足らず」が直ります
            AlertDialog(
                onDismissRequest = { showDeleteConfirmDialog = false },
                containerColor = surfaceColor, // 180dpのタイルと同じ背景色
                title = {
                    Text(
                        text = "機種の削除",
                        color = mainText,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Text(
                        text = "「${selectedMachine!!.name}」を削除してもよろしいですか？\n\n※この機種に含まれるすべてのメモも完全に削除されます。",
                        color = mainText,
                        lineHeight = 20.sp
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            scope.launch {
                                db.machineDao().deleteMachine(selectedMachine!!)
                                showDeleteConfirmDialog = false
                                showActionDialog = false
                            }
                        }
                    ) {
                        Text(
                            text = "削除",
                            color = Color(0xFFF44336), // 警告の赤色
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showDeleteConfirmDialog = false }
                    ) {
                        Text(
                            text = "キャンセル",
                            color = mainText
                        )
                    }
                }
            )
        }
    }

    // 3. タイル型アクションダイアログ（これが一番最後にくるように配置）
    if (showActionDialog && selectedMachine != null) {
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
                // ここが正解の処理！
                showDeleteConfirmDialog = true
                showActionDialog = false
            },
            db = db,
            scope = scope,
            onRefresh = { }
        )
    }
}

@Composable
fun MachineActionDialog(
    selectedMachine: Machine,
    allMachines: List<Machine>, // 並び替え判定のために必要
    onDismiss: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    db: AppDatabase,
    scope: kotlinx.coroutines.CoroutineScope,
    onRefresh: () -> Unit // リフレッシュ用
) {

    val currentIndex = allMachines.indexOfFirst { it.id == selectedMachine.id }

    // 画面全体を覆うレイヤー
    Box(
        modifier = Modifier
            .fillMaxSize()
            //.background(Color.Black.copy(alpha = 0.4f))
            .background(Color.White.copy(alpha = 0.105f))
            .clickable { onDismiss() }, // 外側をタップしたら閉じる
        contentAlignment = Alignment.Center
    ) {
        // ダイアログ本体 (参照コードの Surface 構造を完全再現)
        Surface(
            shape = RoundedCornerShape(16.dp),
            shadowElevation = 8.dp,
            color = Color(0xFF252525),
            modifier = Modifier
                .width(180.dp) // 参照コードと同じ幅
                .clickable(enabled = false) { } // ダイアログ内をタップしても閉じないように
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 機種名タイトル
                Text(
                    text = "「${selectedMachine.name}」",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 20.dp)
                )

                // 参照コードの配色ルール
                val canMoveColors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFBBBBBB),
                    contentColor = Color.Black
                )
                val cannotMoveColors = ButtonDefaults.buttonColors(
                    disabledContainerColor = Color(0xFF333333),
                    disabledContentColor = Color.Black
                )

                // 1段目：上下移動ボタン
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 上へ
                    Button(
                        onClick = {
                            val list = allMachines.toMutableList()
                            val item = list.removeAt(currentIndex)
                            list.add(currentIndex - 1, item)

                            scope.launch {
                                // リストの順番通りに 0, 1, 2... と番号を振り直す
                                val updatedList = list.mapIndexed { index, machine ->
                                    machine.copy(position = index)
                                }
                                // DBに一括保存
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
                                Icons.Default.ArrowBack, // 左右で使っているやつ
                                null,
                                modifier = Modifier.graphicsLayer(rotationZ = 90f), // 90度回して上に向ける
                                tint = Color.Black
                            )
                            Text("上へ", fontSize = 16.sp, color = Color.Black)
                        }
                    }

                    // 下へ
                    Button(
                        onClick = {
                            val list = allMachines.toMutableList()
                            val item = list.removeAt(currentIndex)
                            list.add(currentIndex + 1, item)

                            scope.launch {
                                // リストの順番通りに 0, 1, 2... と番号を振り直す
                                val updatedList = list.mapIndexed { index, machine ->
                                    machine.copy(position = index)
                                }
                                // DBに一括保存
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
                            // ★ アイコンを ArrowDown に変更
                            Icon(
                                Icons.Default.ArrowForward, // 左右で使っているやつ
                                null,
                                modifier = Modifier.graphicsLayer(rotationZ = 90f), // 90度回して下に向ける
                                tint = Color.Black
                            )
                            Text("下へ", fontSize = 16.sp, color = Color.Black)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 2段目：編集・削除ボタン
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 編集ボタン
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

                    // 削除ボタン
                    Button(
                        onClick = {
                            onDelete() // これを呼ぶだけでOK！
                        },
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
    fontSize: androidx.compose.ui.unit.TextUnit = 14.sp // ← ここに「文字サイズ」の項目を追加
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = mainText, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(12.dp))
        // 下の fontSize を、上で追加した fontSize に連動させる
        Text(label, color = mainText, fontSize = fontSize)
    }
}