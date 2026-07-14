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
package dev.ohs.player.reference.app.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun ProfileScreen(
  uiState: HomeShellUiState,
  onRefreshClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val user = uiState.user

  LazyColumn(
    modifier = modifier.background(MaterialTheme.colorScheme.background),
    contentPadding = PaddingValues(24.dp),
    verticalArrangement = Arrangement.spacedBy(20.dp),
  ) {
    item {
      Card(
        shape = RoundedCornerShape(32.dp),
        colors =
          CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
          ),
      ) {
        Column(
          modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 28.dp),
          verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
          Box(
            modifier =
              Modifier.size(72.dp)
                .background(
                  color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.08f),
                  shape = RoundedCornerShape(24.dp),
                ),
            contentAlignment = Alignment.Center,
          ) {
            Icon(
              imageVector = Icons.Default.Person,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.onPrimaryContainer,
              modifier = Modifier.size(34.dp),
            )
          }

          Text(
            text = uiState.displayName,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            fontWeight = FontWeight.Bold,
          )
          Text(
            text = uiState.roleLabel.ifBlank { "Provider" },
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.88f),
          )
          Text(
            text = uiState.locationLabel,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.72f),
          )
        }
      }
    }

    item {
      Button(onClick = onRefreshClick, enabled = !uiState.isRefreshingProfile) {
        if (uiState.isRefreshingProfile) {
          CircularProgressIndicator(
            modifier = Modifier.size(18.dp),
            strokeWidth = 2.dp,
            color = MaterialTheme.colorScheme.onPrimary,
          )
        } else {
          Text("Refresh profile")
        }
      }
    }

    item {
      Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      ) {
        Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
          Text(
            text = "Account Details",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
          )
          HorizontalDivider(
            modifier = Modifier.padding(vertical = 16.dp),
            color = MaterialTheme.colorScheme.outlineVariant,
          )

          ProfileDetail(label = "ID Number", value = user?.idNumber.orEmpty())
          ProfileDetail(label = "Phone", value = user?.phone.orEmpty())
          ProfileDetail(label = "Email", value = user?.email.orEmpty())
          ProfileDetail(
            label = "Subcounty",
            value = user?.locationInfo?.subCountyName.orEmpty(),
          )
          ProfileDetail(label = "County", value = user?.locationInfo?.countyName.orEmpty())
          ProfileDetail(label = "Country", value = user?.locationInfo?.countryName.orEmpty())
        }
      }
    }
  }
}

@Composable
private fun ProfileDetail(
  label: String,
  value: String,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier.fillMaxWidth().padding(vertical = 8.dp),
    verticalArrangement = Arrangement.spacedBy(4.dp),
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.labelLarge,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Text(
      text = value.ifBlank { "Not provided" },
      style = MaterialTheme.typography.bodyLarge,
      color = MaterialTheme.colorScheme.onSurface,
      fontWeight = FontWeight.Medium,
    )
  }
}
