/*
 * Copyright 2026 Open Health Stack Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package icl.ohs.libs.auth.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import icl.ohs.libs.auth.screens.ForgotPasswordScreenConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ForgotPasswordUiState(
  val identifier: String = "",
  val errorMessage: String? = null,
  val isSubmitting: Boolean = false,
  val isSubmitted: Boolean = false,
)

class ForgotPasswordViewModel(
  private val config: ForgotPasswordScreenConfig,
  initialIdentifier: String = "",
) : ViewModel() {
  private val _uiState = MutableStateFlow(ForgotPasswordUiState(identifier = initialIdentifier))
  val uiState: StateFlow<ForgotPasswordUiState> = _uiState.asStateFlow()

  fun onIdentifierChange(value: String) {
    _uiState.update { it.copy(identifier = value, errorMessage = null) }
  }

  fun onErrorDismiss() {
    _uiState.update { it.copy(errorMessage = null) }
  }

  fun onIAlreadyHaveCodeClick(onNavigate: (String) -> Unit) {
    val trimmed = _uiState.value.identifier.trim()
    if (trimmed.isBlank()) {
      _uiState.update { it.copy(errorMessage = config.emptyEmailMessage) }
      return
    }
    onNavigate(trimmed)
  }

  fun submit(onSubmit: suspend (String) -> Result<Unit>) {
    val state = _uiState.value
    if (state.isSubmitting) return
    if (state.identifier.isBlank()) {
      _uiState.update { it.copy(errorMessage = config.emptyEmailMessage) }
      return
    }
    viewModelScope.launch {
      _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }
      try {
        onSubmit(state.identifier)
          .onSuccess { _uiState.update { it.copy(isSubmitted = true) } }
          .onFailure { e ->
            _uiState.update { it.copy(errorMessage = e.message ?: config.emptyEmailMessage) }
          }
      } finally {
        _uiState.update { it.copy(isSubmitting = false) }
      }
    }
  }
}
