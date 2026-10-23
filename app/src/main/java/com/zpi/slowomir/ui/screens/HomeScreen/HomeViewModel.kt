package com.zpi.slowomir.ui.screens.HomeScreen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zpi.slowomir.overlay.OverlayContext
import com.zpi.slowomir.overlay.commands.Command
import com.zpi.slowomir.overlay.commands.CommandResult
import com.zpi.slowomir.overlay.commands.TapCommand
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val tapCommandFactory: TapCommand.Factory
) : ViewModel() {

    private val _uiState = MutableStateFlow<CommandResult?>(null)
    val uiState: StateFlow<CommandResult?> = _uiState.asStateFlow()

    fun executeTapAt(x: Float, y: Float) {
        viewModelScope.launch {
            val tapCommand: Command = tapCommandFactory.create(x, y)

            val context = OverlayContext("", false)

            if (!tapCommand.canExecute(context)) {
                _uiState.value = CommandResult.InvalidContext("Target ($x, $y) out of screen bounds")
                return@launch
            }

            val result = tapCommand.execute()
            _uiState.value = result
        }
    }
}
