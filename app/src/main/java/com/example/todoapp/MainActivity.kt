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
            items(items = todoList, key = { it.id })
            { item ->
                // 为每一个 Item 创建独立的状态
                val dismissState = rememberSwipeToDismissBoxState(
                    confirmValueChange = { value ->
                        // 只有当滑动意图非常明确（已经到达目标状态）时才返回 true
                        value == SwipeToDismissBoxValue.StartToEnd || value == SwipeToDismissBoxValue.EndToStart
                    },
                    // 将阈值设为屏幕宽度的 50% 或更高
                    // 这样用户在滑动过程中，卡片不会“迫不及待”地自己飞走
                    positionalThreshold = { distance -> distance * 0.5f }
                )
                // 1. 获取当前的像素偏移量
                val offset = try { dismissState.requireOffset() } catch (e: Exception) { 0f }
                // 2. 只有当状态已经是“被删除”时，才增加一个巨大的额外位移
                // 如果向右滑，推向正无穷；向左滑，推向负无穷
                val extraTranslation = when (dismissState.targetValue) {
                    SwipeToDismissBoxValue.StartToEnd -> 2000f // 足够飞出任何手机屏幕
                    SwipeToDismissBoxValue.EndToStart -> -2000f
                    else -> 0f
                }

                // 放在 SwipeToDismissBox 的上面
                // 【关键修复】：监听状态的彻底改变
                LaunchedEffect(dismissState.currentValue) {
                    if (dismissState.currentValue != SwipeToDismissBoxValue.Settled) {
                        // 此时用户已经松手，且状态机已经确认要 Dismiss
                        // 等待默认的“飞出”动画播完
                        delay(300)
                        todoList.remove(item)
                    }
                }

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
                        // 3. 关键点：使用 graphicsLayer 手动接管最后的位移
                        Box(
                            modifier = Modifier.graphicsLayer {
                                // 当滑动超过阈值开始自动飞出时，extraTranslation 会立刻介入
                                // 让卡片以极快的速度飞向视野之外
                                translationX = if (dismissState.targetValue != SwipeToDismissBoxValue.Settled) {
                                    extraTranslation
                                } else {
                                    0f
                                }
                            }
                        ) {
                            TodoItemRow(
                                taskName = item.taskName,
                                isDone = item.isDone,
                                shape = cardShape,
                                onStatusChange = { /* ... */ }
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