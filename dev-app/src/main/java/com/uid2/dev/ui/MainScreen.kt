package com.uid2.dev.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Checkbox
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.LocalMinimumInteractiveComponentEnforcement
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.uid2.dev.ui.MainScreenAction.EmailChanged
import com.uid2.dev.ui.MainScreenAction.RefreshButtonPressed
import com.uid2.dev.ui.MainScreenAction.ResetButtonPressed
import com.uid2.dev.ui.MainScreenState.ErrorState
import com.uid2.dev.ui.MainScreenState.LoadingState
import com.uid2.dev.ui.MainScreenState.UserUpdatedState
import com.uid2.dev.ui.views.ActionButtonView
import com.uid2.dev.ui.views.EmailInputView
import com.uid2.dev.ui.views.ErrorView
import com.uid2.dev.ui.views.LoadingView
import com.uid2.dev.ui.views.UserIdentityView
import com.uid2.devapp.R

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun MainScreen(viewModel: MainScreenViewModel) {
    val viewState by viewModel.viewState.collectAsState()

    Scaffold(
        bottomBar = {
            ActionButtonView(
                Modifier,
                onResetClick = { viewModel.processAction(ResetButtonPressed) },
                onRefreshClick = { viewModel.processAction(RefreshButtonPressed) },
            )
        },
    ) { padding ->
        val checkedState = remember { mutableStateOf(true) }

        Column(modifier = Modifier.padding(10.dp, 10.dp, 10.dp, 10.dp + padding.calculateBottomPadding())) {
            // The top of the View provides a way for the Email Address to be entered.
            EmailInputView(Modifier, onEmailEntered = { viewModel.processAction(EmailChanged(it, checkedState.value)) })

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CompositionLocalProvider(LocalMinimumInteractiveComponentEnforcement provides false) {
                    Checkbox(
                        modifier = Modifier
                            .padding(vertical = 4.dp)
                            .padding(end = 4.dp),
                        checked = checkedState.value,
                        onCheckedChange = { checkedState.value = it },
                    )
                }

                Text(text = stringResource(id = R.string.generate_client_side))
            }

            // Depending on the state of the View Model, we will switch in different content view.
            when (val state = viewState) {
                is LoadingState -> LoadingView(Modifier)
                is UserUpdatedState -> UserIdentityView(Modifier, state.identity, state.status)
                is ErrorState -> ErrorView(Modifier, state.error)
            }
        }
    }
}
