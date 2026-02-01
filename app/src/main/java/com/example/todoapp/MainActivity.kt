package com.example.todoapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.ui.graphics.Color

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

@Composable
fun TodoInputScreen() {
    var textState by remember { mutableStateOf("") }
    // 使用 mutableStateListOf，这样当列表改变时，界面会自动刷新
    val todoList = remember { mutableStateListOf<Pair<String, Boolean>>() }

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
                        // 添加一个 Pair，保存 任务内容 和 完成状态(默认false)
                        todoList.add(textState to false)
                        textState = ""
                    }
                },
                modifier = Modifier.size(56.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "添加")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- 下方列表区域 ---
        // LazyColumn 相当于传统的 RecyclerView
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(
                items = todoList,
                // 关键点：提供一个唯一的 key，这样删除动画才不会乱
                key = { it.first + todoList.indexOf(it) }
            ) { todoPair ->
                // 1. 定义滑动状态
                val dismissState = rememberSwipeToDismissBoxState(
                    confirmValueChange = { value ->
                        // 核心修复：必须同时允许这两个值
                        if (value == SwipeToDismissBoxValue.StartToEnd ||
                            value == SwipeToDismissBoxValue.EndToStart) {
                            todoList.remove(todoPair)
                            true // 返回 true 表示确认删除，不再弹回
                        } else {
                            false
                        }
                    }
                )

                // 2. 滑动包装器
                SwipeToDismissBox(
                    state = dismissState,
                    backgroundContent = {
                        // 1. 获取滑动的进度和方向
                        val direction = dismissState.dismissDirection
                        val targetValue = dismissState.targetValue

                        // 2. 核心修复：只有当目标状态不再是 Settled（静止）或者确实有方向偏移时，才显示颜色
                        // 或者判断 dismissState.progress > 0f (部分版本适用)
                        val isSwiping = targetValue != SwipeToDismissBoxValue.Settled

                        val color = if (isSwiping) {
                            Color.Red.copy(alpha = 0.8f)
                        } else {
                            Color.Transparent // 静止状态下背景透明
                        }

                        val alignment = when (direction) {
                            SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
                            SwipeToDismissBoxValue.EndToStart -> Alignment.CenterEnd
                            else -> Alignment.Center
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(color)
                                .padding(horizontal = 20.dp),
                            contentAlignment = alignment
                        ) {
                            // 只有在真正滑动时才显示图标
                            if (isSwiping) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "删除",
                                    tint = Color.White
                                )
                            }
                        }
                    },
                    // 关键修改：允许从左向右滑动
                    enableDismissFromStartToEnd = true,
                    // 允许从右向左滑动
                    enableDismissFromEndToStart = true,
                    content = {
                        // 之前的任务行 UI
                        TodoItemRow(
                            taskName = todoPair.first,
                            isDone = todoPair.second,
                            onStatusChange = { checked ->
                                val index = todoList.indexOf(todoPair)
                                todoList[index] = todoPair.first to checked
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
fun TodoItemRow(taskName: String, isDone: Boolean, onStatusChange: (Boolean) -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp), // 增加上下间距
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