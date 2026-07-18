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
package dev.ohs.player.reference.app.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun ReferenceAppLoader(
  message: String,
  modifier: Modifier = Modifier,
  subtitle: String? = null,
  showScrim: Boolean = false,
) {
  val scrimColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.86f)

  Box(
    modifier =
      modifier
        .fillMaxSize()
        .then(
          if (showScrim) {
            Modifier.background(scrimColor)
          } else {
            Modifier
          }
        ),
    contentAlignment = Alignment.Center,
  ) {
    Surface(
      shape = RoundedCornerShape(30.dp),
      color = MaterialTheme.colorScheme.surface,
      tonalElevation = 10.dp,
      shadowElevation = 18.dp,
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)),
    ) {
      Column(
        modifier = Modifier.padding(horizontal = 28.dp, vertical = 26.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp),
      ) {
        ReferenceAppLogoMark()
        CircularProgressIndicator(
          strokeWidth = 4.dp,
          color = MaterialTheme.colorScheme.primary,
          trackColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.42f),
        )
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          Text(
            text = message,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
          )
          subtitle?.takeIf(String::isNotBlank)?.let { detail ->
            Text(
              text = detail,
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              textAlign = TextAlign.Center,
            )
          }
        }
      }
    }
  }
}

@Composable
private fun ReferenceAppLogoMark(modifier: Modifier = Modifier) {
  val primary = MaterialTheme.colorScheme.primary
  val secondary = MaterialTheme.colorScheme.secondary
  val primaryContainer = MaterialTheme.colorScheme.primaryContainer
  val onPrimary = MaterialTheme.colorScheme.onPrimary

  Surface(
    modifier = modifier.semantics { contentDescription = "Reference app logo" },
    shape = RoundedCornerShape(28.dp),
    color = Color.Transparent,
  ) {
    Canvas(
      modifier =
        Modifier.size(96.dp)
          .background(
            color = primaryContainer.copy(alpha = 0.42f),
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

      val innerLeaf =
        Path().apply {
          moveTo(width * 0.52f, height * 0.18f)
          quadraticTo(width * 0.78f, height * 0.34f, width * 0.66f, height * 0.58f)
          quadraticTo(width * 0.58f, height * 0.74f, width * 0.42f, height * 0.74f)
          quadraticTo(width * 0.24f, height * 0.74f, width * 0.26f, height * 0.54f)
          quadraticTo(width * 0.28f, height * 0.32f, width * 0.52f, height * 0.18f)
          close()
        }

      val centerPulse =
        Path().apply {
          moveTo(width * 0.22f, height * 0.52f)
          lineTo(width * 0.38f, height * 0.52f)
          lineTo(width * 0.46f, height * 0.36f)
          lineTo(width * 0.56f, height * 0.70f)
          lineTo(width * 0.64f, height * 0.52f)
          lineTo(width * 0.78f, height * 0.52f)
          lineTo(width * 0.78f, height * 0.60f)
          lineTo(width * 0.60f, height * 0.60f)
          lineTo(width * 0.54f, height * 0.74f)
          lineTo(width * 0.44f, height * 0.42f)
          lineTo(width * 0.36f, height * 0.60f)
          lineTo(width * 0.22f, height * 0.60f)
          close()
        }

      drawPath(path = outerHex, color = primary, style = Fill)
      drawPath(path = innerLeaf, color = secondary, style = Fill)
      drawPath(path = centerPulse, color = onPrimary, style = Fill)
      drawCircle(
        color = secondary.copy(alpha = 0.18f),
        center = Offset(width * 0.82f, height * 0.18f),
        radius = width * 0.08f,
      )
    }
  }
}
