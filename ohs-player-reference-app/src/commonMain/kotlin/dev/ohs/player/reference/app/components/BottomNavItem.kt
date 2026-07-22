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
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun BottomNavItem(selected: Boolean, label: String, icon: ImageVector, onClick: () -> Unit) {
  // M3 NavigationBar indicator pattern: soft pill behind icon changes colour on selection.
  val iconTint by
    animateColorAsState(
      targetValue =
        if (selected) MaterialTheme.colorScheme.onSecondaryContainer
        else MaterialTheme.colorScheme.onSurfaceVariant,
      animationSpec = tween(durationMillis = 200),
      label = "navIconTint",
    )
  val labelColor by
    animateColorAsState(
      targetValue =
        if (selected) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.onSurfaceVariant,
      animationSpec = tween(durationMillis = 200),
      label = "navLabelColor",
    )
  val pillColor by
    animateColorAsState(
      targetValue =
        if (selected) MaterialTheme.colorScheme.secondaryContainer
        else MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0f),
      animationSpec = tween(durationMillis = 200),
      label = "navPillColor",
    )
  val pillWidth by
    animateDpAsState(
      targetValue = if (selected) 56.dp else 32.dp,
      animationSpec = tween(durationMillis = 200),
      label = "navPillWidth",
    )

  Column(
    modifier =
      Modifier.clickable(
          interactionSource = remember { MutableInteractionSource() },
          indication = null,
          onClick = onClick,
        )
        .padding(horizontal = 8.dp, vertical = 2.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(2.dp),
  ) {
    // Animated pill indicator — widens when selected (M3 NavigationBar pattern)
    Box(
      modifier =
        Modifier.height(28.dp)
          .widthIn(min = pillWidth)
          .background(color = pillColor, shape = RoundedCornerShape(50)),
      contentAlignment = Alignment.Center,
    ) {
      Icon(
        imageVector = icon,
        contentDescription = label,
        tint = iconTint,
        modifier = Modifier.size(20.dp),
      )
    }

    Text(
      text = label,
      style = MaterialTheme.typography.labelSmall,
      color = labelColor,
      fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
    )
  }
}
