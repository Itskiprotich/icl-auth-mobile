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
package icl.ohs.libs.starter.app.feature.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import icl.ohs.libs.starter.app.feature.workflow.WorkflowCardSpec
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

@Composable
fun HomeScreen(
  uiState: HomeShellUiState,
  workflows: List<WorkflowCardSpec>,
  onWorkflowClick: (WorkflowCardSpec) -> Unit,
  modifier: Modifier = Modifier,
  onNotificationsClick: () -> Unit = {},
  onMenuClick: () -> Unit = {},
) {
  val screenBrush =
    Brush.verticalGradient(
      listOf(
        MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
        MaterialTheme.colorScheme.background,
        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.22f),
      )
    )

  LazyColumn(
    modifier = modifier.background(screenBrush).fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(24.dp),
  ) {
    item {
      HomeHeroHeader(
        uiState = uiState,
        workflowCount = workflows.size,
        onNotificationsClick = onNotificationsClick,
        onMenuClick = onMenuClick,
      )
    }

    item {
      Column(
        modifier = Modifier.padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        Text(
          text = "Surveillance Workspace",
          style = MaterialTheme.typography.labelLarge,
          color = MaterialTheme.colorScheme.primary,
          fontWeight = FontWeight.Bold,
        )
        Text(
          text = "Reporting Workflows",
          style = MaterialTheme.typography.headlineMedium,
          color = MaterialTheme.colorScheme.onBackground,
          fontWeight = FontWeight.Bold,
        )
        Text(
          text = "Choose a reporting pathway and continue with the exact tools your team needs.",
          style = MaterialTheme.typography.bodyLarge,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }

    if (workflows.isEmpty()) {
      item {
        Surface(
          modifier = Modifier.padding(horizontal = 24.dp),
          shape = RoundedCornerShape(28.dp),
          color = MaterialTheme.colorScheme.surface,
          border =
            BorderStroke(
              1.dp,
              MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
            ),
        ) {
          Text(
            text = "No workflows are configured yet.",
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
    } else {
      itemsIndexed(workflows.chunked(2)) { rowIndex, rowItems ->
        Row(
          modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
          horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
          rowItems.forEachIndexed { columnIndex, workflow ->
            WorkflowCard(
              workflow = workflow,
              modifier = Modifier.weight(1f),
              accentIndex = rowIndex + columnIndex,
              onClick = { onWorkflowClick(workflow) },
            )
          }

          if (rowItems.size == 1) {
            Spacer(modifier = Modifier.weight(1f))
          }
        }
      }
    }

    item { Spacer(modifier = Modifier.height(24.dp)) }
  }
}

@Composable
private fun HomeHeroHeader(
  uiState: HomeShellUiState,
  workflowCount: Int,
  onNotificationsClick: () -> Unit,
  onMenuClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val gradientColors =
    listOf(
      MaterialTheme.colorScheme.primary,
      MaterialTheme.colorScheme.tertiary,
    )

  Column(modifier = modifier.fillMaxWidth()) {
    Box(
      modifier =
        Modifier.fillMaxWidth()
          .background(Brush.horizontalGradient(gradientColors))
          .padding(horizontal = 20.dp, vertical = 18.dp),
    ) {
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        IconButton(onClick = onMenuClick) {
          Icon(
            imageVector = Icons.Default.MoreVert,
            contentDescription = "More options",
            tint = MaterialTheme.colorScheme.onPrimary,
          )
        }
      }
    }

    Surface(
      modifier = Modifier.fillMaxWidth(),
      color = Color.Transparent,
      shadowElevation = 16.dp,
      shape = RoundedCornerShape(bottomStart = 34.dp, bottomEnd = 34.dp),
    ) {
      Column(
        modifier =
          Modifier.fillMaxWidth()
            .background(Brush.horizontalGradient(gradientColors))
            .padding(horizontal = 24.dp, vertical = 26.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.Top,
        ) {
          Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
              HomePill(label = "Medical Surveillance")
              HomePill(label = uiState.locationLabel)
            }
            Text(
              text = greetingForCurrentTime(),
              style = MaterialTheme.typography.displaySmall,
              color = MaterialTheme.colorScheme.onPrimary,
              fontWeight = FontWeight.Bold,
            )
            Text(
              text = uiState.displayName,
              style = MaterialTheme.typography.headlineMedium,
              color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.92f),
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
            Text(
              text = uiState.roleLabel.ifBlank { "Provider account" },
              style = MaterialTheme.typography.bodyLarge,
              color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.74f),
              maxLines = 2,
              overflow = TextOverflow.Ellipsis,
            )
          }

          NotificationAction(
            isRefreshing = uiState.isRefreshingProfile,
            onClick = onNotificationsClick,
          )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
          HomeMetricCard(
            label = "Workflows",
            value = workflowCount.toString(),
            modifier = Modifier.weight(1f),
          )
          HomeMetricCard(
            label = "Status",
            value = if (uiState.providerProfile != null) "Connected" else "Offline",
            modifier = Modifier.weight(1f),
          )
        }
      }
    }
  }
}

@Composable
private fun HomeMetricCard(
  label: String,
  value: String,
  modifier: Modifier = Modifier,
) {
  Surface(
    modifier = modifier,
    shape = RoundedCornerShape(24.dp),
    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.12f),
    border =
      BorderStroke(
        1.dp,
        MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.16f),
      ),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
      verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
      Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.74f),
      )
      Text(
        text = value,
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onPrimary,
        fontWeight = FontWeight.Bold,
      )
    }
  }
}

@Composable
private fun HomePill(label: String) {
  Surface(
    shape = RoundedCornerShape(999.dp),
    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.12f),
  ) {
    Text(
      text = label,
      modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
      style = MaterialTheme.typography.labelLarge,
      color = MaterialTheme.colorScheme.onPrimary,
      fontWeight = FontWeight.Bold,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )
  }
}

@Composable
private fun NotificationAction(
  isRefreshing: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Surface(
    modifier = modifier,
    onClick = onClick,
    shape = RoundedCornerShape(28.dp),
    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.08f),
    border =
      BorderStroke(
        1.dp,
        MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f),
      ),
  ) {
    Box(
      modifier = Modifier.size(88.dp),
      contentAlignment = Alignment.Center,
    ) {
      if (isRefreshing) {
        CircularProgressIndicator(
          modifier = Modifier.size(30.dp),
          strokeWidth = 2.5.dp,
          color = MaterialTheme.colorScheme.onPrimary,
        )
      } else {
        Icon(
          imageVector = Icons.Default.Notifications,
          contentDescription = "Notifications",
          tint = MaterialTheme.colorScheme.onPrimary,
          modifier = Modifier.size(34.dp),
        )
      }

      Box(
        modifier =
          Modifier.align(Alignment.TopEnd)
            .padding(top = 18.dp, end = 18.dp)
            .size(16.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.error)
      )
    }
  }
}

@Composable
private fun WorkflowCard(
  workflow: WorkflowCardSpec,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  accentIndex: Int = 0,
) {
  val accentColors =
    listOf(
      MaterialTheme.colorScheme.primary,
      MaterialTheme.colorScheme.tertiary,
      MaterialTheme.colorScheme.secondary,
    )
  val accentColor = accentColors[accentIndex % accentColors.size]

  Card(
    modifier = modifier.aspectRatio(0.92f),
    onClick = onClick,
    shape = RoundedCornerShape(30.dp),
    colors =
      CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surface,
      ),
    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    border =
      BorderStroke(
        1.dp,
        accentColor.copy(alpha = 0.14f),
      ),
  ) {
    Column(
      modifier =
        Modifier.fillMaxWidth()
          .background(
            Brush.verticalGradient(
              listOf(
                accentColor.copy(alpha = 0.08f),
                MaterialTheme.colorScheme.surface,
              )
            )
          )
          .padding(20.dp),
      verticalArrangement = Arrangement.SpaceBetween,
    ) {
      Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Box(
            modifier =
              Modifier.size(72.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(accentColor.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
          ) {
            Icon(
              imageVector = workflow.icon,
              contentDescription = null,
              tint = accentColor,
              modifier = Modifier.size(32.dp),
            )
          }

          Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = accentColor,
            modifier = Modifier.size(28.dp),
          )
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Text(
            text = workflow.title,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
          )
          Text(
            text = workflow.description,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
          )
        }
      }

      Text(
        text = "Open module",
        style = MaterialTheme.typography.labelLarge,
        color = accentColor,
        fontWeight = FontWeight.Bold,
      )
    }
  }
}

private fun greetingForCurrentTime(): String {
  val hour = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).hour
  return when (hour) {
    in 5..11 -> "Good Morning"
    in 12..16 -> "Good Afternoon"
    in 17..21 -> "Good Evening"
    else -> "Welcome Back"
  }
}
