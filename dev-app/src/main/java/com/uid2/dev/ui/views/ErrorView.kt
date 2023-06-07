package com.uid2.dev.ui.views

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun ErrorView(modifier: Modifier, error: Throwable) {
    Text(
        text = error.toString(),
        modifier = modifier.fillMaxSize(),
    )
}
