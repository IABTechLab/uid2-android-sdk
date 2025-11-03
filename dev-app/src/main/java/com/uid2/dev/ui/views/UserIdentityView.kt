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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.uid2.data.IdentityStatus
import com.uid2.data.IdentityStatus.ESTABLISHED
import com.uid2.data.IdentityStatus.EXPIRED
import com.uid2.data.IdentityStatus.INVALID
import com.uid2.data.IdentityStatus.NO_IDENTITY
import com.uid2.data.IdentityStatus.OPT_OUT
import com.uid2.data.IdentityStatus.REFRESHED
import com.uid2.data.IdentityStatus.REFRESH_EXPIRED
import com.uid2.data.UID2Identity
import com.uid2.devapp.R

@Composable
fun UserIdentityView(modifier: Modifier, identity: UID2Identity?, status: IdentityStatus) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(0.dp, 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            modifier = Modifier.padding(bottom = 10.dp),
            text = stringResource(id = R.string.current_identity),
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
        )

        identity?.let {
            UserIdentityParameter(stringResource(R.string.identity_advertising_token), identity.advertisingToken)
            UserIdentityParameter(stringResource(R.string.identity_refresh_token), identity.refreshToken)
            UserIdentityParameter(
                stringResource(R.string.identity_identity_expires),
                identity.identityExpires.toString(),
            )
            UserIdentityParameter(stringResource(R.string.identity_refresh_from), identity.refreshFrom.toString())
            UserIdentityParameter(stringResource(R.string.identity_refresh_expires), identity.refreshExpires.toString())
            UserIdentityParameter(stringResource(R.string.identity_refresh_response_key), identity.refreshResponseKey)
        }

        UserIdentityParameter(stringResource(R.string.identity_status), status.toUserString())
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

@Composable
private fun IdentityStatus.toUserString(): String {
    return stringResource(
        when (this) {
            ESTABLISHED -> R.string.status_established
            REFRESHED -> R.string.status_refreshed
            NO_IDENTITY -> R.string.status_no_identity
            EXPIRED -> R.string.status_expired
            INVALID -> R.string.status_invalid
            REFRESH_EXPIRED -> R.string.status_refresh_expired
            OPT_OUT -> R.string.status_opt_out
        },
    )
}
