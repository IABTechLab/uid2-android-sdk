package com.uid2.dev.ui.views

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.icons.Icons.AutoMirrored.Filled
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp

@Composable
fun IdentityInputView(modifier: Modifier = Modifier, label: String, icon: ImageVector, onEntered: (String) -> Unit) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        val identityData = remember { mutableStateOf(TextFieldValue()) }

        TextField(
            value = identityData.value,
            onValueChange = { identityData.value = it },
            label = { Text(label) },
            singleLine = true,
            modifier = Modifier.weight(1f),
            leadingIcon = {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                )
            },
        )

        FloatingActionButton(
            onClick = { onEntered(identityData.value.text) },
            shape = CircleShape,
            backgroundColor = MaterialTheme.colors.primary,
        ) {
            Icon(
                imageVector = Filled.ArrowForward,
                contentDescription = null,
                tint = Color.White,
            )
        }
    }
}
