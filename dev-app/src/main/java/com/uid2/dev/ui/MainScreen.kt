package com.uid2.dev.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
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

@Composable
fun MainScreen(viewModel: MainScreenViewModel) {
    val viewState by viewModel.viewState.collectAsState()

    Scaffold(
        bottomBar = {
            ActionButtonView(
                Modifier,
                onResetClick = { viewModel.processAction(ResetButtonPressed) },
                onRefreshClick = { viewModel.processAction(RefreshButtonPressed) }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(10.dp, 10.dp, 10.dp, 10.dp + padding.calculateBottomPadding())) {
            // The top of the View provides a way for the Email Address to be entered.
            EmailInputView(Modifier, onEmailEntered = { viewModel.processAction(EmailChanged(it)) })

            // Depending on the state of the View Model, we will switch in different content view.
            when (val state = viewState) {
                is LoadingState -> LoadingView(Modifier)
                is UserUpdatedState -> UserIdentityView(Modifier, state.identity, state.status)
                is ErrorState -> ErrorView(Modifier, state.error)
            }
        }
    }
}
