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
import androidx.compose.foundation.layout.fillMaxWidth
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
) {
  Surface(
    modifier =
      Modifier.fillMaxWidth()
        .windowInsetsPadding(WindowInsets.navigationBars)
        .padding(horizontal = 20.dp, vertical = 12.dp),
    color = MaterialTheme.colorScheme.onPrimary,
    shadowElevation = 12.dp,
    shape = RoundedCornerShape(28.dp), // rounded on ALL corners = floating pill
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
      horizontalArrangement = Arrangement.SpaceEvenly,
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
