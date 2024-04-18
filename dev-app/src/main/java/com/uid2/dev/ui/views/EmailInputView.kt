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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.Icons.AutoMirrored.Filled
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Email
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.uid2.devapp.R

@Composable
fun EmailInputView(modifier: Modifier, onEmailEntered: (String) -> Unit) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        val emailAddress = remember { mutableStateOf(TextFieldValue()) }

        TextField(
            value = emailAddress.value,
            onValueChange = { emailAddress.value = it },
            label = { Text(stringResource(R.string.email)) },
            singleLine = true,
            modifier = Modifier.weight(1f),
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Email,
                    contentDescription = stringResource(R.string.email_icon_content_description),
                )
            },
        )

        FloatingActionButton(
            onClick = { onEmailEntered(emailAddress.value.text) },
            shape = CircleShape,
            backgroundColor = MaterialTheme.colors.primary,
        ) {
            Icon(
                imageVector = Filled.ArrowForward,
                contentDescription = stringResource(R.string.email_submit_content_description),
                tint = Color.White,
            )
        }
    }
}
