package com.example.todoapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutLinearInEasing
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Shape

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                TodoInputScreen()
            }
        }
    }
}

data class TodoItem(
    val id: Long = System.currentTimeMillis() + (0..9999).random(),
    val taskName: String,
    val isDone: Boolean,
    val isDeleting: Boolean = false
)

@Composable
fun TodoInputScreen() {
    var textState by remember { mutableStateOf("") }
    val todoList = remember { mutableStateListOf<TodoItem>() }
    var itemPendingDelete by remember { mutableStateOf<TodoItem?>(null) }

    // --- 删除确认对话框 ---
    if (itemPendingDelete != null) {
        AlertDialog(
            onDismissRequest = { itemPendingDelete = null },
            title = { Text("确认删除") },
            text = { Text("确定要删除任务 \"${itemPendingDelete?.taskName}\" 吗？") },
            confirmButton = {
                TextButton(onClick = {
                    val index = todoList.indexOf(itemPendingDelete)
                    if (index != -1) {
                        todoList[index] = todoList[index].copy(isDeleting = true)
                    }
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
                    confirmValueChange = { false },
                    positionalThreshold = { distance -> distance * 0.6f }
                )

                val lockedDirection = remember(dismissState.targetValue) {
                    if (dismissState.targetValue != SwipeToDismissBoxValue.Settled) {
                        if (dismissState.dismissDirection == SwipeToDismissBoxValue.EndToStart) -1f else 1f
                    } else null
                }

                val isTargetingDismiss = dismissState.targetValue != SwipeToDismissBoxValue.Settled
                val isConfirming = itemPendingDelete == item

                val finalAlpha by animateFloatAsState(
                    targetValue = if (item.isDeleting) 0f else 1f,
                    animationSpec = tween(durationMillis = 500),
                    label = "FinalFadeOut",
                    finishedListener = {
                        if (item.isDeleting) { todoList.remove(item) }
                    }
                )

                val extraTranslation by animateFloatAsState(
                    targetValue = if (isConfirming || isTargetingDismiss) {
                        val dir = lockedDirection ?: (if (dismissState.dismissDirection == SwipeToDismissBoxValue.EndToStart) -1f else -1f)
                        2000f * dir
                    } else 0f,
                    animationSpec = tween(600, easing = FastOutLinearInEasing),
                    label = "FlyOut"
                )

                LaunchedEffect(dismissState.targetValue) {
                    if (dismissState.targetValue != SwipeToDismissBoxValue.Settled) {
                        itemPendingDelete = item
                    }
                }

                LaunchedEffect(itemPendingDelete) {
                    if (itemPendingDelete == null && dismissState.currentValue != SwipeToDismissBoxValue.Settled) {
                        dismissState.reset()
                    }
                }

                val cardShape = RoundedCornerShape(12.dp)

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateItem(
                            placementSpec = tween(durationMillis = 400),
                            fadeOutSpec = null
                        )
                ) {
                    SwipeToDismissBox(
                        state = dismissState,
                        backgroundContent = { Box(Modifier.fillMaxSize()) },
                        content = {
                            Box(
                                modifier = Modifier.graphicsLayer {
                                    this.alpha = finalAlpha
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
                                        if (index != -1) {
                                            todoList[index] = item.copy(isDone = newStatus)
                                        }
                                    }
                                )
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun TodoItemRow(taskName: String, isDone: Boolean, shape: Shape, onStatusChange: (Boolean) -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = shape,
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
            Text(
                text = taskName,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge.copy(
                    textDecoration = if (isDone) TextDecoration.LineThrough else TextDecoration.None
                )
            )

            Checkbox(
                checked = isDone,
                onCheckedChange = { onStatusChange(it) }
            )
        }
    }
}