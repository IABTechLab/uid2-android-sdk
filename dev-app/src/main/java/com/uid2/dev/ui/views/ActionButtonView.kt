package com.uid2.dev.ui.views

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.uid2.devapp.R

@Composable
fun ActionButtonView(modifier: Modifier, onResetClick: () -> Unit, onRefreshClick: () -> Unit) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        Button(onClick = onResetClick) {
            Text(stringResource(id = R.string.action_reset))
        }

        Button(onClick = onRefreshClick) {
            Text(stringResource(id = R.string.action_refresh))
        }
    }
}
