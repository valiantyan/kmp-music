package com.yanhao.kmpmusic

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent

/**
 * Android 入口 Activity。
 */
class MainActivity : ComponentActivity() {
    /** 初始化共享 Compose App。 */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            App()
        }
    }
}
