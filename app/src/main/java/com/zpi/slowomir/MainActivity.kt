package com.zpi.slowomir

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.zpi.slowomir.ui.navigation.MainScreen
import com.zpi.slowomir.ui.screens.dev.DevViewModel
import com.zpi.slowomir.ui.theme.SłowomirTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val devViewModel: DevViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            SłowomirTheme {
                MainScreen(devViewModel = devViewModel)
            }
        }
    }
}