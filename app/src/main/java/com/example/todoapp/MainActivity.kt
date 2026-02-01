package com.example.todoapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.ui.draw.clip

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
    // 使用 mutableStateListOf，这样当列表改变时，界面会自动刷新
    // 现在列表里存的是 TodoItem 对象，而不仅仅是字符串
    val todoList = remember { mutableStateListOf<TodoItem>() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // --- 顶部输入区域 ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = textState,
                onValueChange = { textState = it },
                label = { Text("输入新任务") },
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            FilledIconButton(
                onClick = {
                    if (textState.isNotBlank()) {
                        // 创建一个新的 TodoItem 对象并添加
                        todoList.add(TodoItem(taskName = textState, isDone = false))
                        textState = "" // 清空输入框
                    }
                },
                modifier = Modifier.size(56.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "添加")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- 下方列表区域 ---
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(
                items = todoList,
                // 【关键修复 1】使用唯一的 id 作为 Key，防止状态复用导致的自动删除
                key = { it.id }
            ) { item ->
                // 为每一个 Item 创建独立的状态
                val dismissState = rememberSwipeToDismissBoxState(
                    confirmValueChange = { value ->
                        if (value == SwipeToDismissBoxValue.StartToEnd || value == SwipeToDismissBoxValue.EndToStart) {
                            // 【关键修复 2】直接在这里删除，利用 Compose 的自动动画
                            todoList.remove(item)
                            true
                        } else {
                            false
                        }
                    }
                )

                val cardShape = RoundedCornerShape(12.dp)
                // 2. 滑动包装器
                SwipeToDismissBox(
                    state = dismissState,
                    backgroundContent = {
                        // 1. 核心修复：使用 requireOffset() 代替 .offset
                        // 记得导入 kotlin.math.absoluteValue
                        val offset = try { dismissState.requireOffset().absoluteValue } catch (e: Exception) { 0f }

                        val isSwiping = offset > 0.5f

                        // 2. 这里的算法逻辑保持不变
                        // 滑动 100-150 像素左右就达到满透明度，这样触发非常灵敏
                        val enhancedAlpha = if (isSwiping) {
                            (0.2f + (offset / 150f)).coerceIn(0f, 1f)
                        } else {
                            0f
                        }

                        val iconScale = if (isSwiping) {
                            (0.5f + (enhancedAlpha * 0.6f)).coerceIn(0.5f, 1.1f)
                        } else {
                            0f
                        }

                        // ... 后面的 Box 和 Icon 代码保持不变 ...
                        val direction = dismissState.dismissDirection
                        val alignment = when (direction) {
                            SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
                            SwipeToDismissBoxValue.EndToStart -> Alignment.CenterEnd
                            else -> Alignment.Center
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(vertical = 4.dp) // 1. 必须与下方 Card 的 vertical padding 一致
                                .clip(cardShape)           // 2. 裁剪背景，使其拥有和 Card 一样的圆角
                                .background(Color.Red.copy(alpha = enhancedAlpha * 0.9f)),
                            contentAlignment = when (direction) {
                                SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
                                SwipeToDismissBoxValue.EndToStart -> Alignment.CenterEnd
                                else -> Alignment.Center
                            }
                        ) {
                            if (isSwiping) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = enhancedAlpha),
                                    modifier = Modifier
                                        .padding(horizontal = 24.dp)
                                        .graphicsLayer {
                                            val scale = (0.5f + (enhancedAlpha * 0.6f)).coerceIn(0.5f, 1.1f)
                                            scaleX = scale
                                            scaleY = scale
                                        }
                                )
                            }
                        }
                    },
                    // 关键修改：允许从左向右滑动
                    enableDismissFromStartToEnd = true,
                    enableDismissFromEndToStart = true,
                    content = {
                        // 调用你之前的卡片组件
                        TodoItemRow(
                            taskName = item.taskName,
                            isDone = item.isDone,
                            shape = cardShape, // 记得你之前定义的圆角
                            onStatusChange = { checked ->
                                // 更新勾选状态
                                val index = todoList.indexOf(item)
                                if (index != -1) {
                                    todoList[index] = item.copy(isDone = checked)
                                }
                            }
                        )
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