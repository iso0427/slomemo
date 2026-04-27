package iso.slomemo

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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
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

    val machines by db.machineDao().getAllMachines().collectAsState(initial = emptyList())
    var newMachineName by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    // ダイアログの状態管理
    var showEditDialog by remember { mutableStateOf(false) }
    var machineToEdit by remember { mutableStateOf<Machine?>(null) }
    var editNameText by remember { mutableStateOf("") }

    var showActionDialog by remember { mutableStateOf(false) }
    var selectedMachine by remember { mutableStateOf<Machine?>(null) }

    // ダイアログ用の状態（画面のCompose関数の冒頭などで定義）
    var showAddDialog by remember { mutableStateOf(false) }

    var showDeleteConfirmDialog by remember { mutableStateOf(false) }


    Scaffold(
        containerColor = backColor
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(64.dp)
        ) {

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
                items(machines) { machine ->
                    // 状態を検知するための準備
                    val interactionSource = remember { MutableInteractionSource() }
                    val isPressed by interactionSource.collectIsPressedAsState()
                    var isActuallyPressed by remember { mutableStateOf(false) }

                    val isSelected = selectedMachine?.id == machine.id && showActionDialog

                    // 2. 押されたら、少しだけ状態を維持するロジック
                    LaunchedEffect(isActuallyPressed) {
                        if (isActuallyPressed) {
                            kotlinx.coroutines.delay(200) // 0.2秒間は必ず色を変える
                            isActuallyPressed = false
                        }
                    }

                    // 色の決定ロジック
                    val buttonBrush = when {
                        // 1. 長押し確定済み または 短いタップの演出中 → 薄い紫
                        isSelected || isActuallyPressed -> Brush.verticalGradient(
                            listOf(
                                Color(
                                    0xFFEADDFF
                                ), Color(0xFFC0A0FF)
                            )
                        )

                        // 2. 「触っている最中」かつ「まだ確定前」 → グレー（今の長押し中はこちら）
                        isPressed -> Brush.verticalGradient(
                            listOf(
                                Color(0xFF444444),
                                Color(0xFF222222)
                            )
                        )

                        // 3. 通常時
                        else -> Brush.verticalGradient(listOf(Color(0xFF555555), Color(0xFF333333)))
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
                                    indication = null, // ★ これで勝手に暗くなるのを防ぐ
                                    onClick = {
                                        isActuallyPressed = true // タップした瞬間にフラグを立てる
                                        scope.launch {
                                            kotlinx.coroutines.delay(100) // 視覚確認のため少し待つ
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
                                color = if (isSelected || isActuallyPressed) Color(0xFF152200) else mainText
                            )
                        }
                    }
                }

// --- 一番下に追加する「新規登録」ボタン ---
                if (machines.isNotEmpty()) {
                    item {
                        val interactionSource = remember { MutableInteractionSource() }
                        val isPressed by interactionSource.collectIsPressedAsState()

                        // 形状や余白を機種ボタンと完全に一致させる
                        val registerBrush = when {
                            isPressed -> Brush.verticalGradient(
                                listOf(
                                    Color(0xFF444444),
                                    Color(0xFF222222)
                                )
                            )

                            else -> Brush.verticalGradient(
                                listOf(
                                    Color(0xFFffffcc),
                                    Color(0xFFbbbb00)
                                )
                            )
                        }

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp), // ★機種ボタンと同じ 6.dp に統一
                            shape = RoundedCornerShape(12.dp),
                            color = Color.Transparent,
                            shadowElevation = if (isPressed) 12.dp else 4.dp
                        ) {
                            Box(
                                modifier = Modifier
                                    .background(brush = registerBrush)
                                    .clickable(
                                        interactionSource = interactionSource,
                                        indication = null,
                                        onClick = { showAddDialog = true }
                                    )
                                    .padding(20.dp), // ★中のパディングも 20.dp で統一
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "＋ 新規登録",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 30.sp, // ★文字サイズも 30.sp に統一
                                    color = Color(0xff000033)
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
                                        // 既存機種の position をずらして、一番上(0)に追加するロジック
                                        val currentMachines = machines
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
        }
    }

    // ★ タイル型ダイアログの表示（ここを修正！）
    if (showActionDialog && selectedMachine != null) {
        MachineActionDialog(
            selectedMachine = selectedMachine!!,
            allMachines = machines, // ★追加
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

    // --- 名前編集用入力ダイアログ ---
    if (showEditDialog && machineToEdit != null) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            containerColor = surfaceColor,
            title = { Text("機種名の編集", color = mainText) },
            text = {
                // ★ こちらも全く同じデザインを適用
                OutlinedTextField(
                    value = editNameText,
                    onValueChange = { editNameText = it },
                    placeholder = {
                        Text("機種名を入力", fontSize = 14.sp, color = subText) // subTextを使用
                    },
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
                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF7E57C2)),
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
                TextButton(onClick = { showEditDialog = false }) { Text("キャンセル") }
            }
        )
    }
    if (showDeleteConfirmDialog && selectedMachine != null) {
        AlertDialog(
            onDismissRequest = {

                // キャンセルした時も、大元のダイアログまで閉じるならここに追加
                showActionDialog = false
            },
            containerColor = surfaceColor,
            title = { Text(text = "機種の削除", color = mainText) },
            text = {
                Text(
                    text = "「${selectedMachine!!.name}」を削除してもよろしいですか？\nこの機種に含まれるすべてのメモも削除されます。",
                    color = mainText
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        db.machineDao().deleteMachine(selectedMachine!!)

                        // ★ ここで順番にフラグを折る！
                        showDeleteConfirmDialog = false
                        showActionDialog = false
                    }
                }) {
                    Text("削除", color = Color(0xFFF44336), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDeleteConfirmDialog = false
                    showActionDialog = false // ここもお好みで
                }) {
                    Text("キャンセル", color = mainText)
                }
            }
        )
    }
    // ★ タイル型ダイアログの表示
    if (showActionDialog && selectedMachine != null) {
        MachineActionDialog(
            selectedMachine = selectedMachine!!,
            allMachines = machines,
            onDismiss = { showActionDialog = false },
            onRename = {
                showActionDialog = false
                machineToEdit = selectedMachine
                editNameText = selectedMachine!!.name
                showEditDialog = true
            },
            onDelete = {
                // ★ ここがポイント！
                // showActionDialog = false を先に書くと、
                // 次のフラグを立てる前にダイアログが消えて、処理が中断されることがあります。

                showDeleteConfirmDialog = true  // 1. まず確認ダイアログのフラグを立てる
                showActionDialog = false        // 2. その後でアクションダイアログを閉じる
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
            .background(Color.White.copy(alpha = 0.2f)) // 背景の透過
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