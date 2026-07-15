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
package dev.ohs.player.reference.app.feature.home.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileTopBar(
  onSettingsClick: () -> Unit,
  modifier: Modifier = Modifier,
  onRefreshClick: () -> Unit,
  isRefreshing: Boolean,
) {
  TopAppBar(
    modifier = modifier,
    title = {
      Text(
        text = "Profile",
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
      )
    },
    actions = {
      Box(modifier = Modifier.padding(4.dp)) {
        FilledTonalIconButton(
          onClick = onRefreshClick,
          enabled = !isRefreshing,
          modifier = Modifier.align(Alignment.TopEnd),
        ) {
          if (isRefreshing) {
            CircularProgressIndicator(modifier = Modifier.size(19.dp), strokeWidth = 2.dp)
          } else {
            Icon(imageVector = Icons.Default.Refresh, contentDescription = "Refresh profile")
          }
        }
      }
      IconButton(onClick = onSettingsClick) {
        Icon(imageVector = Icons.Default.Settings, contentDescription = "Settings")
      }
    },
    colors =
      TopAppBarDefaults.topAppBarColors(
        containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        actionIconContentColor = MaterialTheme.colorScheme.onSurface,
      ),
  )
}
