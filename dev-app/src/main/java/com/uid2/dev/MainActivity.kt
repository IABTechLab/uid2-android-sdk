package com.uid2.dev

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material.MaterialTheme
import androidx.compose.ui.Modifier
import com.uid2.EUIDManager
import com.uid2.UID2Manager
import com.uid2.dev.network.AppUID2Client
import com.uid2.dev.ui.MainScreen
import com.uid2.dev.ui.MainScreenViewModel
import com.uid2.dev.ui.MainScreenViewModelFactory
import com.uid2.dev.utils.getMetadata
import com.uid2.dev.utils.isEnvironmentEUID
import com.uid2.devapp.R

class MainActivity : ComponentActivity() {

    private val viewModel: MainScreenViewModel by viewModels {
        val isEUID = getMetadata().isEnvironmentEUID()
        MainScreenViewModelFactory(
            AppUID2Client.fromContext(baseContext),
            if (isEUID) EUIDManager.getInstance() else UID2Manager.getInstance(),
            isEUID,
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (getMetadata().isEnvironmentEUID()) {
            setTitle(R.string.app_name_euid)
        }

        setContent {
            Box(Modifier.safeDrawingPadding()) {
                // the rest of the app
                MaterialTheme {
                    MainScreen(viewModel)
                }
            }
        }
    }
}
