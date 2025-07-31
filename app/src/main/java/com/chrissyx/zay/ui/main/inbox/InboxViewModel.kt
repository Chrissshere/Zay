package com.chrissyx.zay.ui.main.inbox

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chrissyx.zay.data.models.Message
import com.chrissyx.zay.data.repository.FirebaseRepository
import com.chrissyx.zay.utils.UserPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class InboxViewModel(
    private val firebaseRepository: FirebaseRepository,
    private val userPreferences: UserPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(InboxUiState())
    val uiState: StateFlow<InboxUiState> = _uiState.asStateFlow()

    fun loadMessages() {
        val username = userPreferences.username ?: return
        
        
        _uiState.value = _uiState.value.copy(isLoading = true)
        
        viewModelScope.launch {
            try {
                firebaseRepository.getMessagesForUser(username).collect { messages ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        messages = messages,
                        errorMessage = null
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Failed to load messages: ${e.message}"
                )
            }
        }
    }

    fun refreshMessages() {
        loadMessages()
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}

data class InboxUiState(
    val messages: List<Message> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
) 