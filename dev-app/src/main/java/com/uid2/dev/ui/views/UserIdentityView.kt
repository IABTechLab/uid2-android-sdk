package com.uid2.dev.ui.views

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.uid2.data.UID2Identity

@Composable
fun UserIdentityView(modifier: Modifier, identity: UID2Identity) {
    Column(
        modifier = modifier.verticalScroll(rememberScrollState()).padding(0.dp, 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        UserIdentityParameter("Advertising Token", identity.advertisingToken)
        UserIdentityParameter("Refresh Token", identity.refreshToken)
        UserIdentityParameter("Identity Expires", identity.identityExpires.toString())
        UserIdentityParameter("Refresh From", identity.refreshFrom.toString())
        UserIdentityParameter("Refresh Expires", identity.refreshExpires.toString())
        UserIdentityParameter("Refresh Response Key", identity.refreshResponseKey)
    }
}

@Composable
private fun UserIdentityParameter(title: String, subtitle: String) {
    Column {
        Text(text = title, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Text(text = subtitle, fontSize = 16.sp, fontWeight = FontWeight.Normal)
        Divider(thickness = 1.dp)
    }
}
