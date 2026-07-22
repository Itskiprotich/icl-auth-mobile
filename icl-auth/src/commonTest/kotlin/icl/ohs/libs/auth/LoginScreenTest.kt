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
package icl.ohs.libs.auth

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.runComposeUiTest
import icl.ohs.libs.auth.models.LoginScreenConfig
import icl.ohs.libs.auth.screens.LOGIN_PASSWORD_CLEAR_BUTTON_TAG
import icl.ohs.libs.auth.screens.LOGIN_PASSWORD_TAG
import icl.ohs.libs.auth.screens.LOGIN_PASSWORD_VISIBILITY_BUTTON_TAG
import icl.ohs.libs.auth.screens.LOGIN_USERNAME_CLEAR_BUTTON_TAG
import icl.ohs.libs.auth.screens.LOGIN_USERNAME_TAG
import icl.ohs.libs.auth.screens.LoginScreen
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class LoginScreenTest {

  @Test
  fun loginFields_useIconActions_insteadOfTextButtons() = runComposeUiTest {
    setContent {
      MaterialTheme {
        LoginScreen(config = LoginScreenConfig(endpoint = "/login"), onLoginSuccess = {})
      }
    }

    onNodeWithTag(LOGIN_USERNAME_TAG).performTextInput("nurse")
    onNodeWithTag(LOGIN_PASSWORD_TAG).performTextInput("secret")

    onNodeWithTag(LOGIN_USERNAME_CLEAR_BUTTON_TAG).assertIsDisplayed()
    onNodeWithTag(LOGIN_PASSWORD_CLEAR_BUTTON_TAG).assertIsDisplayed()
    onNodeWithTag(LOGIN_PASSWORD_VISIBILITY_BUTTON_TAG)
      .assertIsDisplayed()
      .assert(hasContentDescription("Show password"))

    onAllNodesWithText("Clear").assertCountEquals(0)
    onAllNodesWithText("Show").assertCountEquals(0)
    onAllNodesWithText("Hide").assertCountEquals(0)

    onNodeWithTag(LOGIN_USERNAME_CLEAR_BUTTON_TAG).performClick()
    onAllNodesWithTag(LOGIN_USERNAME_CLEAR_BUTTON_TAG).assertCountEquals(0)

    onNodeWithTag(LOGIN_PASSWORD_VISIBILITY_BUTTON_TAG).performClick()
    onNodeWithTag(LOGIN_PASSWORD_VISIBILITY_BUTTON_TAG)
      .assert(hasContentDescription("Hide password"))

    onNodeWithTag(LOGIN_PASSWORD_CLEAR_BUTTON_TAG).performClick()
    onAllNodesWithTag(LOGIN_PASSWORD_CLEAR_BUTTON_TAG).assertCountEquals(0)
  }
}
