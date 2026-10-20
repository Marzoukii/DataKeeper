package com.marzouki.datasaver

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.marzouki.datasaver.presentation.DataKeeperApp
import com.marzouki.datasaver.ui.theme.DataKeeperTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DataKeeperTheme {
                DataKeeperApp()
            }
        }
    }
}
