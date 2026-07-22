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
import icl.ohs.libs.auth.models.SetNewPasswordFailure
import icl.ohs.libs.auth.models.SetNewPasswordReq
import icl.ohs.libs.auth.models.SetNewPasswordScreenConfig
import icl.ohs.libs.auth.models.SetNewPasswordSuccess
import icl.ohs.libs.auth.network.LoginService
import icl.ohs.libs.auth.network.ResolvedSetNewPasswordConfig
import icl.ohs.libs.auth.network.SetNewPasswordAttemptResult
import icl.ohs.libs.auth.network.buildLoginHttpClient
import icl.ohs.libs.auth.network.resolveSetNewPasswordConfig
import icl.ohs.libs.auth.network.validateSetNewPasswordRequest
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SetNewPasswordUiState(
  val currentPassword: String = "",
  val newPassword: String = "",
  val confirmPassword: String = "",
  val errorMessage: String? = null,
  val isSubmitting: Boolean = false,
)

sealed class SetNewPasswordEvent {
  data class Success(val result: SetNewPasswordSuccess) : SetNewPasswordEvent()

  data class Failure(val result: SetNewPasswordFailure) : SetNewPasswordEvent()
}

class SetNewPasswordViewModel(
  private val config: SetNewPasswordScreenConfig,
  private val idNumber: String,
) : ViewModel() {
  private val resolvedConfig = resolveSetNewPasswordConfig(config)
  private val httpClient = buildLoginHttpClient(resolvedConfig.requestTimeoutMillis)
  private val loginService = LoginService(httpClient)

  private val _uiState = MutableStateFlow(SetNewPasswordUiState())
  val uiState: StateFlow<SetNewPasswordUiState> = _uiState.asStateFlow()

  private val _events = Channel<SetNewPasswordEvent>(Channel.BUFFERED)
  val events: Flow<SetNewPasswordEvent> = _events.receiveAsFlow()

  fun onCurrentPasswordChange(value: String) {
    _uiState.update { it.copy(currentPassword = value, errorMessage = null) }
  }

  fun onNewPasswordChange(value: String) {
    _uiState.update { it.copy(newPassword = value, errorMessage = null) }
  }

  fun onConfirmPasswordChange(value: String) {
    _uiState.update { it.copy(confirmPassword = value, errorMessage = null) }
  }

  fun onErrorDismiss() {
    _uiState.update { it.copy(errorMessage = null) }
  }

  fun onSubmitClick() {
    val state = _uiState.value
    if (state.isSubmitting) return

    val request =
      SetNewPasswordReq(
        temporaryPassword = state.currentPassword,
        idNumber = idNumber,
        password = state.newPassword,
      )
    val validationFailure =
      validateSetNewPasswordForm(
        config = resolvedConfig,
        request = request,
        confirmPassword = state.confirmPassword,
      )
    if (validationFailure != null) {
      _uiState.update { it.copy(errorMessage = validationFailure.message) }
      viewModelScope.launch { _events.send(SetNewPasswordEvent.Failure(validationFailure)) }
      return
    }

    viewModelScope.launch {
      _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }
      try {
        when (
          val result = loginService.setNewPassword(config = resolvedConfig, request = request)
        ) {
          is SetNewPasswordAttemptResult.Success -> {
            _uiState.update { it.copy(errorMessage = null) }
            _events.send(SetNewPasswordEvent.Success(result.value))
          }

          is SetNewPasswordAttemptResult.Failure -> {
            _uiState.update { it.copy(errorMessage = result.value.message) }
            _events.send(SetNewPasswordEvent.Failure(result.value))
          }
        }
      } finally {
        _uiState.update { it.copy(isSubmitting = false) }
      }
    }
  }

  override fun onCleared() {
    super.onCleared()
    httpClient.close()
  }
}

internal fun validateSetNewPasswordForm(
  config: ResolvedSetNewPasswordConfig,
  request: SetNewPasswordReq,
  confirmPassword: String,
): SetNewPasswordFailure? =
  when {
    confirmPassword.isBlank() ->
      SetNewPasswordFailure(message = config.messages.emptyConfirmPassword)
    request.password != confirmPassword ->
      SetNewPasswordFailure(message = config.messages.passwordMismatch)
    else -> validateSetNewPasswordRequest(config = config, request = request)
  }
