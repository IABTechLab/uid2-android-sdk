package com.uid2.dev.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.uid2.dev.network.AppUID2Client
import com.uid2.dev.network.AppUID2ClientException
import com.uid2.dev.network.RequestType.EMAIL
import com.uid2.dev.ui.MainScreenState.ErrorState
import com.uid2.dev.ui.MainScreenState.EmptyState
import com.uid2.dev.ui.MainScreenState.LoadingState
import com.uid2.dev.ui.MainScreenState.UserUpdatedState
import com.uid2.UID2Manager
import com.uid2.UID2ManagerState.Established
import com.uid2.UID2ManagerState.Refreshed
import com.uid2.data.UID2Identity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface MainScreenAction : ViewModelAction {
    data class EmailChanged(val address: String): MainScreenAction
    object ResetButtonPressed: MainScreenAction
    object RefreshButtonPressed: MainScreenAction
}

sealed interface MainScreenState : ViewState {
    object EmptyState : MainScreenState
    object LoadingState : MainScreenState
    data class UserUpdatedState(val identity: UID2Identity) : MainScreenState
    data class ErrorState(val error: Throwable) : MainScreenState
}

class MainScreenViewModel(
    private val api: AppUID2Client,
    private val manager: UID2Manager
) : BasicViewModel<MainScreenAction, MainScreenState>() {

    private val _viewState = MutableStateFlow<MainScreenState>(EmptyState)
    override val viewState: StateFlow<MainScreenState> = _viewState.asStateFlow()

    init {
        // Observe the state of the UID2Manager and translate those into our own ViewState. This will happen when the
        // Identity is initial set, or refreshed, or reset.
        viewModelScope.launch {
            manager.state.collect { state ->
                Log.d(TAG, "State Update: $state")

                when (state) {
                    is Established -> _viewState.emit(UserUpdatedState(state.identity))
                    is Refreshed -> _viewState.emit(UserUpdatedState(state.identity))
                    else ->  _viewState.emit(EmptyState)
                }
            }
        }
    }

    override fun processAction(action: MainScreenAction) {
        Log.d(TAG, "Action: $action")

        viewModelScope.launch {
            _viewState.emit(LoadingState)
            when (action) {
                is MainScreenAction.EmailChanged -> {
                    try {
                        // For Development purposes, we are required to generate the initial Identity before then
                        // passing it onto the SDK to be managed.
                        api.generateIdentity(action.address, EMAIL)?.let {
                            manager.setIdentity(it)
                        }
                    } catch (ex: AppUID2ClientException) {
                        _viewState.emit(ErrorState(ex))
                    }
                }

                MainScreenAction.RefreshButtonPressed -> manager.refreshIdentity()
                MainScreenAction.ResetButtonPressed -> manager.resetIdentity()
            }
        }
    }

    private companion object {
        const val TAG = "MainScreenViewModel"
    }
}

class MainScreenViewModelFactory(
    private val api: AppUID2Client,
    private val manager: UID2Manager
): ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return MainScreenViewModel(api, manager) as T
    }
}
