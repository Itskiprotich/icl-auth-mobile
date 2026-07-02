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
package dev.ohs.player.reference.app.auth

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.runComposeUiTest
import dev.ohs.player.reference.app.OhsPlayerTheme
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class LoginScreenTest {

  @Test
  fun loginButton_staysDisabledUntilBothFieldsAreFilled() = runComposeUiTest {
    setContent {
      var username by mutableStateOf("")
      var password by mutableStateOf("")
      OhsPlayerTheme {
        LoginScreen(
          username = username,
          password = password,
          onUsernameChange = { username = it },
          onPasswordChange = { password = it },
          onLoginClick = {},
          onForgotPasswordClick = {},
          onTermsAndConditionsClick = {},
        )
      }
    }

    onNodeWithTag(LOGIN_BUTTON_TAG).assertIsNotEnabled()
    onNodeWithTag(LOGIN_USERNAME_TAG).performTextInput("nurse.jane")
    onNodeWithTag(LOGIN_BUTTON_TAG).assertIsNotEnabled()
    onNodeWithTag(LOGIN_PASSWORD_TAG).performTextInput("password123")
    onNodeWithTag(LOGIN_BUTTON_TAG).assertIsEnabled()
  }

  @Test
  fun loginButton_forwardsEnteredCredentialsThroughStatefulHost() = runComposeUiTest {
    var submittedUsername: String? = null
    var submittedPassword: String? = null

    setContent {
      var username by mutableStateOf("")
      var password by mutableStateOf("")
      OhsPlayerTheme {
        LoginScreen(
          username = username,
          password = password,
          onUsernameChange = { username = it },
          onPasswordChange = { password = it },
          onLoginClick = {
            submittedUsername = username
            submittedPassword = password
          },
          onForgotPasswordClick = {},
          onTermsAndConditionsClick = {},
        )
      }
    }

    onNodeWithTag(LOGIN_USERNAME_TAG).performTextInput("care.worker")
    onNodeWithTag(LOGIN_PASSWORD_TAG).performTextInput("super-secret")
    onNodeWithTag(LOGIN_BUTTON_TAG).performClick()

    assertEquals("care.worker", submittedUsername)
    assertEquals("super-secret", submittedPassword)
  }
}
