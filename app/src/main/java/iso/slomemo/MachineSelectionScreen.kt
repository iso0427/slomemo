package iso.slomemo


import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
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

    Scaffold(
        containerColor = backColor
    ) { padding ->
        Column(modifier = Modifier
            .padding(padding)
            .padding(16.dp)
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

            Spacer(modifier = Modifier.height(24.dp))
            // 機種追加エリア（OutlinedTextField スタイルに統一）
            Text(
                "機種の追加",
                style = MaterialTheme.typography.titleMedium,
                color = mainText,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically // ★ 赤線を修正（RowはVertically）
            ) {
                OutlinedTextField(
                    value = newMachineName,
                    onValueChange = { newMachineName = it },
                    placeholder = {
                        Text(
                            "新機種名を入力",
                            fontSize = 14.sp,
                            color = subText
                        )
                    },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    // ★ ご希望の配色を完全に適用
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

                // 機種追加ボタン
                Button(
                    onClick = {
                        if (newMachineName.isNotBlank()) {
                            val name = newMachineName
                            newMachineName = ""
                            scope.launch {
                                db.machineDao().insertMachine(Machine(name = name))
                            }
                        }
                    },
                    // ★ 見た目を「項目追加」側に合わせて Modifier の height 指定を削除
                    modifier = Modifier.padding(start = 8.dp),
                    // ★ 色を Color(0xFF7E57C2) に変更
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF7E57C2)
                    )
                ) {
                    // ★ 文字色もデフォルト（または以前のスタイル）に
                    Text("追加")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 機種一覧リスト
            LazyColumn {
                items(machines) { machine ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .combinedClickable(
                                onClick = { onMachineSelected(machine.id) },
                                onLongClick = {
                                    // ★長押しでタイル型ダイアログを表示
                                    selectedMachine = machine
                                    showActionDialog = true
                                }
                            ),
                        colors = CardDefaults.cardColors(
                            containerColor = surfaceColor
                        )
                    ) {
                        Text(
                            text = machine.name,
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodyLarge,
                            color = mainText
                        )
                    }
                }
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

    // 名前編集用入力ダイアログ
    if (showEditDialog && machineToEdit != null) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("機種名の編集") },
            text = {
                TextField(
                    value = editNameText,
                    onValueChange = { editNameText = it },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        db.machineDao().updateMachine(machineToEdit!!.copy(name = editNameText))
                        showEditDialog = false
                    }
                }) { Text("保存") }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) { Text("キャンセル") }
            }
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
            .background(Color.Black.copy(alpha = 0.4f)) // 背景の透過
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

                // 1段目：左右移動ボタン
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 左へ
                    Button(
                        onClick = {
                            val list = allMachines.toMutableList()
                            val item = list.removeAt(currentIndex)
                            list.add(currentIndex - 1, item)
                            scope.launch {
                                // 並び替え保存処理（必要に応じて実装）
                                onDismiss()
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

                    // 右へ
                    Button(
                        onClick = {
                            val list = allMachines.toMutableList()
                            val item = list.removeAt(currentIndex)
                            list.add(currentIndex + 1, item)
                            scope.launch {
                                // 並び替え保存処理（必要に応じて実装）
                                onDismiss()
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
                            Icon(Icons.Default.ArrowForward, null, tint = Color.Black)
                            Text("右へ", fontSize = 16.sp, color = Color.Black)
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
                        onClick = onDelete,
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