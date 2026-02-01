package com.example.todoapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add

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
    // 假设我们先用一个列表存任务（后面会教你如何真正显示它）
    val todoList = remember { mutableStateListOf<String>() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // 使用 Row 让输入框和按钮并排
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically // 垂直居中对齐
        ) {
            // 1. 输入框 (使用 weight 占据左侧剩余的所有空间)
            TextField(
                value = textState,
                onValueChange = { textState = it },
                label = { Text("输入新任务") },
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.width(8.dp)) // 给输入框和按钮之间留点缝隙

            // 2. 圆形添加按钮
            FilledIconButton(
                onClick = {
                    if (textState.isNotBlank()) {
                        todoList.add(textState)
                        textState = "" // 清空输入框
                    }
                },
                modifier = Modifier.size(56.dp) // 设置按钮大小
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "添加"
                )
            }
        }

        // 这里预留给以后的列表显示
    }
}