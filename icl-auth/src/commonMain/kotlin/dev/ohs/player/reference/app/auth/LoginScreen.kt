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

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

internal const val LOGIN_USERNAME_TAG = "login_username"
internal const val LOGIN_PASSWORD_TAG = "login_password"
internal const val LOGIN_BUTTON_TAG = "login_button"

@Composable
fun LoginScreen(
  username: String,
  password: String,
  onUsernameChange: (String) -> Unit,
  onPasswordChange: (String) -> Unit,
  onLoginClick: () -> Unit,
  onForgotPasswordClick: () -> Unit,
  onTermsAndConditionsClick: () -> Unit,
  modifier: Modifier = Modifier,
  errorMessage: String? = null,
) {
  val canSubmit = username.isNotBlank() && password.isNotBlank()

  Scaffold(
    modifier = modifier.fillMaxSize(),
    containerColor = MaterialTheme.colorScheme.surface,
    bottomBar = {
      LoginFooter(
        onTermsAndConditionsClick = onTermsAndConditionsClick,
        modifier = Modifier.fillMaxWidth().navigationBarsPadding(),
      )
    },
  ) { innerPadding ->
    Box(
      modifier =
        Modifier.fillMaxSize()
          .systemBarsPadding()
          .background(
            Brush.verticalGradient(
              colors =
                listOf(
                  MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                  MaterialTheme.colorScheme.surface,
                )
            )
          )
          .padding(innerPadding)
          .imePadding(),
      contentAlignment = Alignment.Center,
    ) {
      Column(
        modifier =
          Modifier.widthIn(max = 460.dp)
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp),
      ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          AuthLogo()
          Text(
            text = "Welcome Back",
            modifier = Modifier.padding(top = 20.dp),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
          )
          Text(
            text = "Sign in to continue to the ICL reference experience.",
            modifier = Modifier.padding(top = 8.dp),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
          )
        }

        Card(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(28.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        ) {
          Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
          ) {
            OutlinedTextField(
              value = username,
              onValueChange = onUsernameChange,
              modifier = Modifier.fillMaxWidth().testTag(LOGIN_USERNAME_TAG),
              label = { Text("Username") },
              placeholder = { Text("Enter your username") },
              singleLine = true,
              keyboardOptions =
                KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
            )

            OutlinedTextField(
              value = password,
              onValueChange = onPasswordChange,
              modifier = Modifier.fillMaxWidth().testTag(LOGIN_PASSWORD_TAG),
              label = { Text("Password") },
              placeholder = { Text("Enter your password") },
              singleLine = true,
              visualTransformation = PasswordVisualTransformation(),
              keyboardOptions =
                KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
              keyboardActions = KeyboardActions(onDone = { if (canSubmit) onLoginClick() }),
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
              TextButton(onClick = onForgotPasswordClick) { Text("Forgot password?") }
            }

            if (errorMessage != null) {
              Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
              )
            }

            Button(
              onClick = onLoginClick,
              modifier = Modifier.fillMaxWidth().testTag(LOGIN_BUTTON_TAG),
              enabled = canSubmit,
              shape = RoundedCornerShape(18.dp),
            ) {
              Text("Log in")
            }
          }
        }
      }
    }
  }
}

@Composable
private fun LoginFooter(onTermsAndConditionsClick: () -> Unit, modifier: Modifier = Modifier) {
  Surface(
    modifier = modifier,
    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
    tonalElevation = 4.dp,
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Text(
        text = "By continuing, you agree to the platform Terms and Conditions.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
      )
      TextButton(onClick = onTermsAndConditionsClick) { Text("Terms and Conditions") }
    }
  }
}

@Composable
private fun AuthLogo(modifier: Modifier = Modifier) {
  val primary = MaterialTheme.colorScheme.primary
  val secondary = MaterialTheme.colorScheme.secondary
  val primaryContainer = MaterialTheme.colorScheme.primaryContainer
  val onPrimary = MaterialTheme.colorScheme.onPrimary

  Surface(
    modifier = modifier.semantics { contentDescription = "ICL logo" },
    shape = RoundedCornerShape(28.dp),
    color = Color.Transparent,
  ) {
    Canvas(
      modifier =
        Modifier.size(92.dp)
          .background(
            color = primaryContainer.copy(alpha = 0.4f),
            shape = RoundedCornerShape(28.dp),
          )
          .padding(14.dp)
    ) {
      val width = size.width
      val height = size.height

      val outerHex =
        Path().apply {
          moveTo(width * 0.50f, 0f)
          lineTo(width * 0.90f, height * 0.22f)
          lineTo(width * 0.90f, height * 0.78f)
          lineTo(width * 0.50f, height)
          lineTo(width * 0.10f, height * 0.78f)
          lineTo(width * 0.10f, height * 0.22f)
          close()
        }

      drawPath(
        path = outerHex,
        brush = Brush.linearGradient(colors = listOf(secondary, primary)),
        style = Fill,
      )

      val innerHex =
        Path().apply {
          moveTo(width * 0.50f, height * 0.24f)
          lineTo(width * 0.72f, height * 0.36f)
          lineTo(width * 0.72f, height * 0.64f)
          lineTo(width * 0.50f, height * 0.76f)
          lineTo(width * 0.28f, height * 0.64f)
          lineTo(width * 0.28f, height * 0.36f)
          close()
        }

      drawPath(path = innerHex, color = onPrimary.copy(alpha = 0.92f), style = Fill)
    }
  }
}
