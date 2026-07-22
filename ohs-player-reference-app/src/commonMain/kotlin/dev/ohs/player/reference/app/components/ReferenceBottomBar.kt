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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ReferenceBottomBar(
  homeSelected: Boolean,
  profileSelected: Boolean,
  onHomeClick: () -> Unit,
  onProfileClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Surface(
    modifier =
      modifier
        .windowInsetsPadding(WindowInsets.navigationBars)
        .padding(horizontal = 32.dp, vertical = 10.dp),
    color = MaterialTheme.colorScheme.surface,
    shadowElevation = 16.dp,
    tonalElevation = 2.dp,
    shape = RoundedCornerShape(32.dp),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      BottomNavItem(
        selected = homeSelected,
        label = "Home",
        icon = Icons.Default.Home,
        onClick = onHomeClick,
      )
      BottomNavItem(
        selected = profileSelected,
        label = "Profile",
        icon = Icons.Default.Person,
        onClick = onProfileClick,
      )
    }
  }
}
