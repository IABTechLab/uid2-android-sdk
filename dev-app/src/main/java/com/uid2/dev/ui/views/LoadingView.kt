package com.uid2.dev.ui.views

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun LoadingView(modifier: Modifier) {
    Box(modifier.fillMaxSize()) {
        CircularProgressIndicator(
            modifier = Modifier.size(64.dp).align(Alignment.Center)
        )
    }
}
