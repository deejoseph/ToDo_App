package com.example.todoapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.delay
import kotlin.math.absoluteValue
import androidx.compose.ui.graphics.Shape
import androidx.compose.animation.core.FastOutLinearInEasing

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // 使用 Material3 主题容器
            Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                TodoInputScreen()
            }
        }
    }
}

data class TodoItem(
    val id: Long = System.currentTimeMillis() + (0..9999).random(), // 随机且唯一的ID
    val taskName: String,
    val isDone: Boolean
)

@Composable
fun TodoInputScreen() {
    var textState by remember { mutableStateOf("") }
    val todoList = remember { mutableStateListOf<TodoItem>() }

    // 状态 1：当前正在请求确认删除的任务
    var itemPendingDelete by remember { mutableStateOf<TodoItem?>(null) }

    // --- 删除确认对话框 ---
    if (itemPendingDelete != null) {
        AlertDialog(
            onDismissRequest = { itemPendingDelete = null },
            title = { Text("确认删除") },
            text = { Text("确定要删除任务 \"${itemPendingDelete?.taskName}\" 吗？") },
            confirmButton = {
                TextButton(onClick = {
                    itemPendingDelete?.let { todoList.remove(it) }
                    itemPendingDelete = null
                }) {
                    Text("确定", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { itemPendingDelete = null }) {
                    Text("取消")
                }
            }
        )
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        // --- 顶部输入区域保持不变 ---
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextField(
                value = textState,
                onValueChange = { textState = it },
                label = { Text("输入新任务") },
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            FilledIconButton(onClick = {
                if (textState.isNotBlank()) {
                    todoList.add(TodoItem(taskName = textState, isDone = false))
                    textState = ""
                }
            }, modifier = Modifier.size(56.dp)) {
                Icon(Icons.Default.Add, contentDescription = "添加")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            items(items = todoList, key = { it.id }) { item ->
                val dismissState = rememberSwipeToDismissBoxState(
                    confirmValueChange = { value ->
                        // 【关键修改 1】：拦截原生的逻辑删除动作。
                        // 我们不让 dismissState 自己结算，而是手动控制弹窗。
                        false
                    },
                    positionalThreshold = { distance -> distance * 0.6f }
                )

                // 记录锁定的滑动方向
                val lockedDirection = remember(dismissState.targetValue) {
                    if (dismissState.targetValue != SwipeToDismissBoxValue.Settled) {
                        if (dismissState.dismissDirection == SwipeToDismissBoxValue.EndToStart) -1f else 1f
                    } else null
                }

                // 判断是否达到了触发阈值
                val isTargetingDismiss = dismissState.targetValue != SwipeToDismissBoxValue.Settled
                val isConfirming = itemPendingDelete == item

                // 【关键修改 2】：当 targetValue 达到边界时，主动开启弹窗
                LaunchedEffect(dismissState.targetValue) {
                    if (dismissState.targetValue != SwipeToDismissBoxValue.Settled) {
                        itemPendingDelete = item
                    }
                }

                // 当取消删除时，重置滑动状态
                LaunchedEffect(itemPendingDelete) {
                    if (itemPendingDelete == null && dismissState.currentValue != SwipeToDismissBoxValue.Settled) {
                        dismissState.reset()
                    }
                }

                // 位移动画判定
                val extraTranslation by animateFloatAsState(
                    targetValue = if (isConfirming || isTargetingDismiss) {
                        val dir = lockedDirection ?: (if (dismissState.dismissDirection == SwipeToDismissBoxValue.EndToStart) -1f else 1f)
                        2000f * dir
                    } else 0f,
                    animationSpec = tween(600, easing = FastOutLinearInEasing),
                    label = "FlyOut"
                )

                val cardShape = RoundedCornerShape(12.dp)

                SwipeToDismissBox(
                    state = dismissState,
                    backgroundContent = {
                        val offset = try { dismissState.requireOffset().absoluteValue } catch (e: Exception) { 0f }
                        // 只要有位移或正在确认，背景就显示
                        val alpha by animateFloatAsState(if (offset > 10f || isConfirming) 0.8f else 0f)

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(vertical = 4.dp)
                                .clip(cardShape)
                                .background(Color.Red.copy(alpha = alpha)),
                            contentAlignment = if (dismissState.dismissDirection == SwipeToDismissBoxValue.StartToEnd)
                                Alignment.CenterStart else Alignment.CenterEnd
                        ) {
                            if (offset > 10f || isConfirming) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.padding(horizontal = 24.dp)
                                )
                            }
                        }
                    },
                    content = {
                        Box(
                            modifier = Modifier.graphicsLayer {
                                // 只要还在动画中，就应用位移
                                translationX = if (dismissState.currentValue == SwipeToDismissBoxValue.Settled &&
                                    dismissState.targetValue == SwipeToDismissBoxValue.Settled) 0f else extraTranslation
                            }
                        ) {
                            TodoItemRow(
                                taskName = item.taskName,
                                isDone = item.isDone,
                                shape = cardShape,
                                onStatusChange = { newStatus ->
                                    val index = todoList.indexOf(item)
                                    if (index != -1) todoList[index] = item.copy(isDone = newStatus)
                                }
                            )
                        }
                    }
                )
            }
        }
    }
}

// 抽取出来的一个单独的“行”组件
@Composable
fun TodoItemRow(taskName: String, isDone: Boolean, shape: Shape, onStatusChange: (Boolean) -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp), // 增加上下间距
        shape = shape, // <--- 确保 Card 使用了传入的 shape
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. 任务文字：放在左边，占据剩余所有空间
            Text(
                text = taskName,
                modifier = Modifier.weight(1f), // 核心代码：把 Checkbox 挤到右边去
                style = MaterialTheme.typography.bodyLarge.copy(
                    // 如果完成了，给文字加一个中划线效果，看起来更专业
                    textDecoration = if (isDone) TextDecoration.LineThrough else TextDecoration.None
                )
            )

            // 2. 复选框：现在它会乖乖待在最右边
            Checkbox(
                checked = isDone,
                onCheckedChange = { onStatusChange(it) }
            )
        }
    }
}