package com.uid2.dev.ui

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.StateFlow

/**
 * Actions supported by the ViewModel.
 */
interface ViewModelAction

/**
 * The state represented by the ViewModel.
 */
interface ViewState

/**
 * A simple base class for ViewModels that provide a single Flow of state that represents the user interface. It also
 * provides a mechanism to process user actions on the model itself.
 */
abstract class BasicViewModel<ACTION : ViewModelAction, VIEWSTATE : ViewState> : ViewModel() {

    /**
     * A flow of ViewState that represents the state of the ViewModel.
     */
    abstract val viewState: StateFlow<VIEWSTATE>

    /**
     * Processes the given ViewModelAction.
     */
    abstract fun processAction(action: ACTION)
}
