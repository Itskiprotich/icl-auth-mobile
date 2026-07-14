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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.delay

internal enum class AuthMessageBannerType {
  Success,
  Error,
}

@Composable
internal fun AuthMessageBanner(
  message: String,
  type: AuthMessageBannerType,
  onDismiss: () -> Unit,
  modifier: Modifier = Modifier,
  durationMillis: Long = 2_000,
) {
  LaunchedEffect(message, type) {
    delay(durationMillis.milliseconds)
    onDismiss()
  }

  // Derive banner colors from the host MaterialTheme so embedded auth screens
  // automatically match the calling application's palette.
  val backgroundColor =
    when (type) {
      AuthMessageBannerType.Success -> MaterialTheme.colorScheme.tertiaryContainer
      AuthMessageBannerType.Error -> MaterialTheme.colorScheme.errorContainer
    }
  val accentColor =
    when (type) {
      AuthMessageBannerType.Success -> MaterialTheme.colorScheme.tertiary
      AuthMessageBannerType.Error -> MaterialTheme.colorScheme.error
    }
  val contentColor =
    when (type) {
      AuthMessageBannerType.Success -> MaterialTheme.colorScheme.onTertiaryContainer
      AuthMessageBannerType.Error -> MaterialTheme.colorScheme.onErrorContainer
    }

  Surface(
    modifier = modifier.widthIn(max = 520.dp),
    color = backgroundColor,
    shape = RoundedCornerShape(20.dp),
    tonalElevation = 8.dp,
    shadowElevation = 10.dp,
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 14.dp),
      horizontalArrangement = Arrangement.spacedBy(12.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Box(modifier = Modifier.size(12.dp).background(accentColor, CircleShape))
      Text(
        text = message,
        style = MaterialTheme.typography.bodyMedium,
        color = contentColor,
        fontWeight = FontWeight.Medium,
      )
    }
  }
}
