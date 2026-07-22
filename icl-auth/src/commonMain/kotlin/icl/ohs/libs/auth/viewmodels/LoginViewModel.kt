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
import icl.ohs.libs.auth.models.LoginFailure
import icl.ohs.libs.auth.models.LoginScreenConfig
import icl.ohs.libs.auth.models.LoginSuccess
import icl.ohs.libs.auth.network.LoginAttemptResult
import icl.ohs.libs.auth.network.LoginService
import icl.ohs.libs.auth.network.buildLoginHttpClient
import icl.ohs.libs.auth.network.resolveLoginConfig
import icl.ohs.libs.auth.network.validateLoginRequest
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LoginUiState(
  val username: String = "",
  val password: String = "",
  val errorMessage: String? = null,
  val isSubmitting: Boolean = false,
)

sealed class LoginEvent {
  data class Success(val result: LoginSuccess) : LoginEvent()

  data class Failure(val result: LoginFailure) : LoginEvent()

  data class ForgotPassword(val username: String) : LoginEvent()
}

class LoginViewModel(private val config: LoginScreenConfig) : ViewModel() {
  private val resolvedConfig = resolveLoginConfig(config)
  private val httpClient = buildLoginHttpClient(resolvedConfig.requestTimeoutMillis)
  private val loginService = LoginService(httpClient)

  private val _uiState = MutableStateFlow(LoginUiState())
  val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

  private val _events = Channel<LoginEvent>(Channel.BUFFERED)
  val events: Flow<LoginEvent> = _events.receiveAsFlow()

  fun onUsernameChange(value: String) {
    _uiState.update { it.copy(username = value, errorMessage = null) }
  }

  fun onPasswordChange(value: String) {
    _uiState.update { it.copy(password = value, errorMessage = null) }
  }

  fun onErrorDismiss() {
    _uiState.update { it.copy(errorMessage = null) }
  }

  fun onForgotPasswordClick() {
    val username = _uiState.value.username.trim()
    if (username.isBlank()) {
      _uiState.update { it.copy(errorMessage = resolvedConfig.messages.emptyUsername) }
      return
    }
    viewModelScope.launch { _events.send(LoginEvent.ForgotPassword(username)) }
  }

  fun onLoginClick() {
    val state = _uiState.value
    if (state.isSubmitting) return

    val validationFailure =
      validateLoginRequest(
        config = resolvedConfig,
        username = state.username,
        password = state.password,
      )
    if (validationFailure != null) {
      _uiState.update { it.copy(errorMessage = validationFailure.message) }
      viewModelScope.launch { _events.send(LoginEvent.Failure(validationFailure)) }
      return
    }

    viewModelScope.launch {
      _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }
      try {
        when (
          val result =
            loginService.login(
              config = resolvedConfig,
              username = state.username,
              password = state.password,
            )
        ) {
          is LoginAttemptResult.Success -> {
            _uiState.update { it.copy(errorMessage = null) }
            _events.send(LoginEvent.Success(result.value))
          }

          is LoginAttemptResult.Failure -> {
            _uiState.update { it.copy(errorMessage = result.value.message) }
            _events.send(LoginEvent.Failure(result.value))
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
