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

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.ohs.player.reference.app.feature.home.components.ProfileTopBar

@Composable
fun ProfileScreen(
  uiState: HomeShellUiState,
  onRefreshClick: () -> Unit,
  modifier: Modifier = Modifier,
  onClearCacheClick: () -> Unit = {},
  onClearAppDataClick: () -> Unit = {},
  onChangePasswordClick: () -> Unit = {},
  onLogoutClick: () -> Unit = {},
  onSettingsClick: () -> Unit = {},
) {
  val user = uiState.user

  var confirmationAction by remember { mutableStateOf<ProfileConfirmationAction?>(null) }
  Scaffold(
    modifier = modifier.fillMaxSize(),
    containerColor = MaterialTheme.colorScheme.background,
    topBar = {
      ProfileTopBar(
        onRefreshClick = onRefreshClick,
        onSettingsClick = onSettingsClick,
        isRefreshing = uiState.isRefreshingProfile,
      )
    },
  ) { innerPadding ->
    BoxWithConstraints(
      modifier =
        modifier
          .fillMaxSize()
          .background(MaterialTheme.colorScheme.background)
          .padding(innerPadding)
    ) {
      val layout = profileLayout(maxWidth)

      LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding =
          PaddingValues(
            start = layout.horizontalPadding,
            end = layout.horizontalPadding,
            top = 20.dp,
            bottom = 40.dp,
          ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp),
      ) {
        item {
          ProfileOverviewCard(
            displayName = uiState.displayName,
            role = uiState.roleLabel.ifBlank { "Provider" },
            modifier = Modifier.fillMaxWidth().widthIn(max = layout.maximumContentWidth),
          )
        }

        item {
          ProfileSectionCard(
            title = "Personal Information",
            modifier = Modifier.fillMaxWidth().widthIn(max = layout.maximumContentWidth),
          ) {
            ProfileInformationRow(label = "ID Number", value = user?.idNumber.orEmpty())

            ProfileInformationDivider()

            ProfileInformationRow(label = "Role", value = uiState.roleLabel)

            ProfileInformationDivider()
            ProfileInformationRow(
              label = "Country",
              value = user?.locationInfo?.countryName.orEmpty(),
            )
            ProfileInformationDivider()
            ProfileInformationRow(
              label = "County",
              value = user?.locationInfo?.countyName.orEmpty(),
            )

            ProfileInformationDivider()

            ProfileInformationRow(
              label = "Sub-county",
              value = user?.locationInfo?.subCountyName.orEmpty(),
            )

            ProfileInformationDivider()

            ProfileInformationRow(label = "Ward", value = user?.locationInfo?.wardName.orEmpty())

            ProfileInformationDivider()

            ProfileInformationRow(
              label = "Facility",
              value = user?.locationInfo?.facilityName.orEmpty(),
            )
          }
        }

        item {
          ProfileSectionCard(
            title = "Contact Information",
            modifier = Modifier.fillMaxWidth().widthIn(max = layout.maximumContentWidth),
          ) {
            ProfileContactRow(
              icon = Icons.Default.Email,
              label = "Email",
              value = user?.email.orEmpty(),
            )

            ProfileInformationDivider(startPadding = 58.dp)

            ProfileContactRow(
              icon = Icons.Default.Phone,
              label = "Phone",
              value = user?.phone.orEmpty(),
            )

            ProfileInformationDivider(startPadding = 58.dp)

            ProfileContactRow(
              icon = Icons.Default.LocationOn,
              label = "Location",
              value = uiState.locationLabel,
            )
          }
        }

        item {
          ProfileSectionCard(
            title = "Data Management",
            modifier = Modifier.fillMaxWidth().widthIn(max = layout.maximumContentWidth),
          ) {
            ProfileActionRow(
              icon = Icons.Default.Delete,
              title = "Clear cache",
              description = "Remove temporary files stored on this device.",
              onClick = { confirmationAction = ProfileConfirmationAction.ClearCache },
            )

            ProfileInformationDivider(startPadding = 58.dp)

            ProfileActionRow(
              icon = Icons.Default.Delete,
              title = "Clear app data",
              description = "Remove locally stored app data and preferences.",
              contentColor = MaterialTheme.colorScheme.error,
              onClick = { confirmationAction = ProfileConfirmationAction.ClearAppData },
            )
          }
        }

        item {
          ProfileSectionCard(
            title = "Account",
            modifier = Modifier.fillMaxWidth().widthIn(max = layout.maximumContentWidth),
          ) {
            ProfileActionRow(
              icon = Icons.Default.Lock,
              title = "Change password",
              description = "Update the password used to access your account.",
              onClick = onChangePasswordClick,
            )

            ProfileInformationDivider(startPadding = 58.dp)

            ProfileActionRow(
              icon = Icons.Default.ExitToApp,
              title = "Log out",
              description = "Sign out of this account on the current device.",
              contentColor = MaterialTheme.colorScheme.error,
              onClick = { confirmationAction = ProfileConfirmationAction.Logout },
            )
          }
        }
      }
    }
  }

  confirmationAction?.let { action ->
    ProfileConfirmationDialog(
      action = action,
      onDismiss = { confirmationAction = null },
      onConfirm = {
        confirmationAction = null

        when (action) {
          ProfileConfirmationAction.ClearCache -> onClearCacheClick()
          ProfileConfirmationAction.ClearAppData -> onClearAppDataClick()
          ProfileConfirmationAction.Logout -> onLogoutClick()
        }
      },
    )
  }
}

private data class ProfileLayout(val horizontalPadding: Dp, val maximumContentWidth: Dp)

private fun profileLayout(width: Dp): ProfileLayout =
  when {
    width < 360.dp -> ProfileLayout(horizontalPadding = 14.dp, maximumContentWidth = 760.dp)

    width < 600.dp -> ProfileLayout(horizontalPadding = 18.dp, maximumContentWidth = 760.dp)

    else -> ProfileLayout(horizontalPadding = 32.dp, maximumContentWidth = 820.dp)
  }

@Composable
private fun ProfileOverviewCard(displayName: String, role: String, modifier: Modifier = Modifier) {
  Card(
    modifier = modifier,
    shape = RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 28.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(18.dp),
    ) {
      ProfileAvatar(displayName = displayName, modifier = Modifier.size(76.dp))

      Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
          text = displayName.ifBlank { "User profile" },
          style = MaterialTheme.typography.headlineSmall,
          color = MaterialTheme.colorScheme.onPrimaryContainer,
          fontWeight = FontWeight.Bold,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
        )

        if (role.isNotBlank()) {
          Surface(
            shape = RoundedCornerShape(50),
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.10f),
          ) {
            Text(
              text = role,
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
              style = MaterialTheme.typography.labelLarge,
              color = MaterialTheme.colorScheme.onPrimaryContainer,
              fontWeight = FontWeight.SemiBold,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
        }
      }
    }
  }
}

@Composable
private fun ProfileAvatar(displayName: String, modifier: Modifier = Modifier) {
  val initials =
    displayName
      .trim()
      .split(Regex("\\s+"))
      .filter(String::isNotBlank)
      .take(2)
      .mapNotNull { it.firstOrNull()?.uppercaseChar() }
      .joinToString("")

  Surface(
    modifier = modifier.size(80.dp),
    shape = RoundedCornerShape(26.dp),
    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.10f),
    border =
      BorderStroke(
        width = 1.dp,
        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.15f),
      ),
  ) {
    Box(contentAlignment = Alignment.Center) {
      if (initials.isNotBlank()) {
        Text(
          text = initials,
          style = MaterialTheme.typography.headlineMedium,
          color = MaterialTheme.colorScheme.onPrimaryContainer,
          fontWeight = FontWeight.Bold,
        )
      } else {
        Icon(
          imageVector = Icons.Default.Person,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.onPrimaryContainer,
          modifier = Modifier.size(36.dp),
        )
      }
    }
  }
}

@Composable
private fun ProfileSectionCard(
  title: String,
  modifier: Modifier = Modifier,
  content: @Composable () -> Unit,
) {
  Card(
    modifier = modifier,
    shape = RoundedCornerShape(26.dp),
    border = BorderStroke(width = 1.dp, color = MaterialTheme.colorScheme.outlineVariant),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
  ) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 22.dp)) {
      Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onSurface,
        fontWeight = FontWeight.Bold,
      )

      Spacer(modifier = Modifier.size(10.dp))

      content()
    }
  }
}

@Composable
private fun ProfileInformationRow(label: String, value: String, modifier: Modifier = Modifier) {
  Row(
    modifier = modifier.fillMaxWidth().padding(vertical = 12.dp),
    horizontalArrangement = Arrangement.spacedBy(16.dp),
    verticalAlignment = Alignment.Top,
  ) {
    Text(
      text = label,
      modifier = Modifier.weight(0.42f),
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    Text(
      text = value.ifBlank { "Not provided" },
      modifier = Modifier.weight(0.58f),
      style = MaterialTheme.typography.bodyLarge,
      color =
        if (value.isBlank()) {
          MaterialTheme.colorScheme.onSurfaceVariant
        } else {
          MaterialTheme.colorScheme.onSurface
        },
      fontWeight = FontWeight.Medium,
      textAlign = TextAlign.End,
    )
  }
}

@Composable
private fun ProfileContactRow(
  icon: ImageVector,
  label: String,
  value: String,
  modifier: Modifier = Modifier,
) {
  Row(
    modifier = modifier.fillMaxWidth().padding(vertical = 12.dp),
    horizontalArrangement = Arrangement.spacedBy(14.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    ProfileIconContainer(icon = icon)

    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
      Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontWeight = FontWeight.Medium,
      )

      Text(
        text = value.ifBlank { "Not provided" },
        style = MaterialTheme.typography.bodyLarge,
        color =
          if (value.isBlank()) {
            MaterialTheme.colorScheme.onSurfaceVariant
          } else {
            MaterialTheme.colorScheme.onSurface
          },
        fontWeight = FontWeight.Medium,
        maxLines = 3,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}

@Composable
private fun ProfileActionRow(
  icon: ImageVector,
  title: String,
  description: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  contentColor: Color = MaterialTheme.colorScheme.primary,
) {
  Row(
    modifier = modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 14.dp),
    horizontalArrangement = Arrangement.spacedBy(14.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Surface(
      modifier = Modifier.size(44.dp),
      shape = RoundedCornerShape(14.dp),
      color = contentColor.copy(alpha = 0.10f),
    ) {
      Box(contentAlignment = Alignment.Center) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = contentColor,
          modifier = Modifier.size(22.dp),
        )
      }
    }

    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
      Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = contentColor,
        fontWeight = FontWeight.SemiBold,
      )

      Text(
        text = description,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}

@Composable
private fun ProfileIconContainer(icon: ImageVector, modifier: Modifier = Modifier) {
  Surface(
    modifier = modifier.size(44.dp),
    shape = RoundedCornerShape(14.dp),
    color = MaterialTheme.colorScheme.secondaryContainer,
  ) {
    Box(contentAlignment = Alignment.Center) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSecondaryContainer,
        modifier = Modifier.size(22.dp),
      )
    }
  }
}

@Composable
private fun ProfileInformationDivider(modifier: Modifier = Modifier, startPadding: Dp = 0.dp) {
  HorizontalDivider(
    modifier = modifier.padding(start = startPadding),
    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f),
  )
}

private enum class ProfileConfirmationAction {
  ClearCache,
  ClearAppData,
  Logout,
}

@Composable
private fun ProfileConfirmationDialog(
  action: ProfileConfirmationAction,
  onDismiss: () -> Unit,
  onConfirm: () -> Unit,
) {
  val title =
    when (action) {
      ProfileConfirmationAction.ClearCache -> "Clear cache?"
      ProfileConfirmationAction.ClearAppData -> "Clear app data?"
      ProfileConfirmationAction.Logout -> "Log out?"
    }

  val message =
    when (action) {
      ProfileConfirmationAction.ClearCache ->
        "Temporary files stored by the application will be removed."

      ProfileConfirmationAction.ClearAppData ->
        "Locally stored application data will be removed. This action may require you to sign in again."

      ProfileConfirmationAction.Logout -> "You will be signed out of the current account."
    }

  val confirmationText =
    when (action) {
      ProfileConfirmationAction.ClearCache -> "Clear cache"
      ProfileConfirmationAction.ClearAppData -> "Clear data"
      ProfileConfirmationAction.Logout -> "Log out"
    }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(text = title, fontWeight = FontWeight.Bold) },
    text = { Text(text = message) },
    confirmButton = {
      TextButton(onClick = onConfirm) {
        Text(
          text = confirmationText,
          color =
            if (action == ProfileConfirmationAction.ClearCache) {
              MaterialTheme.colorScheme.primary
            } else {
              MaterialTheme.colorScheme.error
            },
        )
      }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
  )
}
