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

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private val NavAccentHighlightBg = Color(0x33FF6A3D)

@Composable
fun BottomNavItem(selected: Boolean, label: String, icon: ImageVector, onClick: () -> Unit) {
  val tint by
    animateColorAsState(
      if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
      label = "navTint",
    )

  Column(
    modifier =
      Modifier.clickable(
          interactionSource = remember { MutableInteractionSource() },
          indication = null,
          onClick = onClick,
        )
        .padding(horizontal = 12.dp, vertical = 2.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(4.dp),
  ) {
    // Small highlight capsule sits ONLY behind the icon when active —
    // this is what gives the screenshot's "Tasks" tab its glow.
    Box(
      modifier =
        Modifier.size(40.dp)
          .background(
            color = if (selected) NavAccentHighlightBg else Color.Transparent,
            shape = CircleShape,
          ),
      contentAlignment = Alignment.Center,
    ) {
      Icon(imageVector = icon, contentDescription = label, tint = tint)
    }
    Text(
      text = label,
      style = MaterialTheme.typography.labelMedium,
      color = tint,
      fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
    )
  }
}
