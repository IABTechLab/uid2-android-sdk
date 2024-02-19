package com.uid2.dev.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.uid2.UID2Exception
import com.uid2.UID2Manager
import com.uid2.UID2ManagerState.Established
import com.uid2.UID2ManagerState.Expired
import com.uid2.UID2ManagerState.NoIdentity
import com.uid2.UID2ManagerState.OptOut
import com.uid2.UID2ManagerState.RefreshExpired
import com.uid2.UID2ManagerState.Refreshed
import com.uid2.data.IdentityStatus
import com.uid2.data.IdentityStatus.ESTABLISHED
import com.uid2.data.IdentityStatus.EXPIRED
import com.uid2.data.IdentityStatus.INVALID
import com.uid2.data.IdentityStatus.NO_IDENTITY
import com.uid2.data.IdentityStatus.OPT_OUT
import com.uid2.data.IdentityStatus.REFRESHED
import com.uid2.data.IdentityStatus.REFRESH_EXPIRED
import com.uid2.data.UID2Identity
import com.uid2.dev.network.AppUID2Client
import com.uid2.dev.network.RequestType.EMAIL
import com.uid2.dev.ui.MainScreenState.ErrorState
import com.uid2.dev.ui.MainScreenState.LoadingState
import com.uid2.dev.ui.MainScreenState.UserUpdatedState
import com.uid2.dev.utils.toSha256
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface MainScreenAction : ViewModelAction {
    data class EmailChanged(val address: String, val clientSide: Boolean) : MainScreenAction
    data object ResetButtonPressed : MainScreenAction
    data object RefreshButtonPressed : MainScreenAction
}

sealed interface MainScreenState : ViewState {
    data object LoadingState : MainScreenState
    data class UserUpdatedState(val identity: UID2Identity?, val status: IdentityStatus) : MainScreenState
    data class ErrorState(val error: Throwable) : MainScreenState
}

class MainScreenViewModel(
    private val api: AppUID2Client,
    private val manager: UID2Manager,
) : BasicViewModel<MainScreenAction, MainScreenState>() {

    private val _viewState = MutableStateFlow<MainScreenState>(UserUpdatedState(null, NO_IDENTITY))
    override val viewState: StateFlow<MainScreenState> = _viewState.asStateFlow()

    init {
        // Observe the state of the UID2Manager and translate those into our own ViewState. This will happen when the
        // Identity is initial set, or refreshed, or reset.
        viewModelScope.launch {
            manager.state.collect { state ->
                Log.d(TAG, "State Update: $state")

                when (state) {
                    is Established -> _viewState.emit(UserUpdatedState(state.identity, ESTABLISHED))
                    is Refreshed -> _viewState.emit(UserUpdatedState(state.identity, REFRESHED))
                    is NoIdentity -> _viewState.emit(UserUpdatedState(null, NO_IDENTITY))
                    is Expired -> _viewState.emit(UserUpdatedState(state.identity, EXPIRED))
                    is RefreshExpired -> _viewState.emit(UserUpdatedState(null, REFRESH_EXPIRED))
                    is OptOut -> _viewState.emit(UserUpdatedState(null, OPT_OUT))
                    else -> _viewState.emit(UserUpdatedState(null, INVALID))
                }
            }
        }
    }

    override fun processAction(action: MainScreenAction) {
        Log.d(TAG, "Action: $action")

        viewModelScope.launch {
            when (action) {
                is MainScreenAction.EmailChanged -> {
                    _viewState.emit(LoadingState)

                    try {
                        if (action.clientSide) {
                            // Generate the identity via client side token generation.
                            manager.generateIdentity(action.address.toSha256())
                        } else {
                            // We're going to generate the identity as if we've obtained it via a backend service.
                            api.generateIdentity(action.address, EMAIL)?.let {
                                manager.setIdentity(it)
                            }
                        }
                    } catch (ex: UID2Exception) {
                        _viewState.emit(ErrorState(ex))
                    }
                }
                MainScreenAction.RefreshButtonPressed -> {
                    manager.currentIdentity?.let { _viewState.emit(LoadingState) }
                    manager.refreshIdentity()
                }
                MainScreenAction.ResetButtonPressed -> {
                    manager.currentIdentity?.let { _viewState.emit(LoadingState) }
                    manager.resetIdentity()
                }
            }
        }
    }

    private companion object {
        const val TAG = "MainScreenViewModel"
    }
}

class MainScreenViewModelFactory(
    private val api: AppUID2Client,
    private val manager: UID2Manager,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return MainScreenViewModel(api, manager) as T
    }
}
