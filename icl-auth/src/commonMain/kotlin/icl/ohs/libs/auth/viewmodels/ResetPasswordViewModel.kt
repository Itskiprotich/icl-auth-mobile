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
import icl.ohs.libs.auth.models.ResetPasswordFailure
import icl.ohs.libs.auth.models.ResetPasswordReq
import icl.ohs.libs.auth.models.ResetPasswordScreenConfig
import icl.ohs.libs.auth.models.ResetPasswordSuccess
import icl.ohs.libs.auth.network.LoginService
import icl.ohs.libs.auth.network.ResetPasswordAttemptResult
import icl.ohs.libs.auth.network.ResolvedResetPasswordConfig
import icl.ohs.libs.auth.network.buildLoginHttpClient
import icl.ohs.libs.auth.network.resolveResetPasswordConfig
import icl.ohs.libs.auth.network.validateResetPasswordRequest
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ResetPasswordUiState(
  val otp: String = "",
  val newPassword: String = "",
  val confirmPassword: String = "",
  val errorMessage: String? = null,
  val isSubmitting: Boolean = false,
)

sealed class ResetPasswordEvent {
  data class Success(val result: ResetPasswordSuccess) : ResetPasswordEvent()

  data class Failure(val result: ResetPasswordFailure) : ResetPasswordEvent()
}

class ResetPasswordViewModel(
  private val config: ResetPasswordScreenConfig,
  private val identifier: String,
) : ViewModel() {
  private val resolvedConfig = resolveResetPasswordConfig(config)
  private val httpClient = buildLoginHttpClient(resolvedConfig.requestTimeoutMillis)
  private val loginService = LoginService(httpClient)

  private val _uiState = MutableStateFlow(ResetPasswordUiState())
  val uiState: StateFlow<ResetPasswordUiState> = _uiState.asStateFlow()

  private val _events = Channel<ResetPasswordEvent>(Channel.BUFFERED)
  val events: Flow<ResetPasswordEvent> = _events.receiveAsFlow()

  fun onOtpChange(value: String) {
    _uiState.update { it.copy(otp = value, errorMessage = null) }
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
      ResetPasswordReq(otp = state.otp, identifier = identifier, password = state.newPassword)
    val validationFailure =
      validateResetPasswordForm(
        config = resolvedConfig,
        request = request,
        confirmPassword = state.confirmPassword,
      )
    if (validationFailure != null) {
      _uiState.update { it.copy(errorMessage = validationFailure.message) }
      viewModelScope.launch { _events.send(ResetPasswordEvent.Failure(validationFailure)) }
      return
    }

    viewModelScope.launch {
      _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }
      try {
        when (val result = loginService.resetPassword(config = resolvedConfig, request = request)) {
          is ResetPasswordAttemptResult.Success -> {
            _uiState.update { it.copy(errorMessage = null) }
            _events.send(ResetPasswordEvent.Success(result.value))
          }

          is ResetPasswordAttemptResult.Failure -> {
            _uiState.update { it.copy(errorMessage = result.value.message) }
            _events.send(ResetPasswordEvent.Failure(result.value))
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

internal fun validateResetPasswordForm(
  config: ResolvedResetPasswordConfig,
  request: ResetPasswordReq,
  confirmPassword: String,
): ResetPasswordFailure? =
  when {
    confirmPassword.isBlank() ->
      ResetPasswordFailure(message = config.messages.emptyConfirmPassword)
    request.password != confirmPassword ->
      ResetPasswordFailure(message = config.messages.passwordMismatch)
    else -> validateResetPasswordRequest(config = config, request = request)
  }
