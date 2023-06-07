package com.uid2.dev

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material.MaterialTheme
import com.uid2.UID2Manager
import com.uid2.dev.network.AppUID2Client
import com.uid2.dev.ui.MainScreen
import com.uid2.dev.ui.MainScreenViewModel
import com.uid2.dev.ui.MainScreenViewModelFactory

class MainActivity : ComponentActivity() {

    private val viewModel: MainScreenViewModel by viewModels {
        MainScreenViewModelFactory(
            AppUID2Client.fromContext(baseContext),
            UID2Manager.getInstance(),
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                MainScreen(viewModel)
            }
        }
    }
}
