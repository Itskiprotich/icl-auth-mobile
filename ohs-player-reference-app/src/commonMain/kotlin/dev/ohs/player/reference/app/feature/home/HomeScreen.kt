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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.ohs.player.reference.app.feature.workflow.WorkflowCardSpec
import kotlin.time.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@Composable
fun HomeScreen(
  uiState: HomeShellUiState,
  workflows: List<WorkflowCardSpec>,
  onWorkflowClick: (WorkflowCardSpec) -> Unit,
  modifier: Modifier = Modifier,
  onNotificationsClick: () -> Unit = {},
) {
  BoxWithConstraints(
    modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
  ) {
    val layout = homeLayoutForWidth(maxWidth)
    Column(modifier = Modifier.fillMaxSize()) {
      HomeHeader(
        displayName = uiState.displayName,
        horizontalPadding = layout.horizontalPadding,
        greetingStyle = layout.greetingStyle,
        nameStyle = layout.nameStyle,
        onNotificationsClick = onNotificationsClick,
      )

      LazyVerticalGrid(
        columns = GridCells.Fixed(layout.columns),
        modifier = Modifier.fillMaxSize(),
        contentPadding =
          PaddingValues(
            start = layout.horizontalPadding,
            end = layout.horizontalPadding,
            bottom = 100.dp, // space for floating nav
            top = 20.dp,
          ),
        horizontalArrangement = Arrangement.spacedBy(layout.cardSpacing),
        verticalArrangement = Arrangement.spacedBy(layout.cardSpacing),
      ) {
        item(span = { GridItemSpan(maxLineSpan) }) { WorkflowSectionHeader() }

        if (workflows.isEmpty()) {
          item(span = { GridItemSpan(maxLineSpan) }) { EmptyWorkflowCard() }
        } else {
          items(items = workflows, key = WorkflowCardSpec::key) { workflow ->
            WorkflowCard(
              workflow = workflow,
              onClick = { onWorkflowClick(workflow) },
              compact = layout.compactCards,
            )
          }
        }
      }
    }
  }
}

// --- Layout helpers ---

private data class HomeLayout(
  val columns: Int,
  val horizontalPadding: Dp,
  val cardSpacing: Dp,
  val compactCards: Boolean,
  val greetingStyle: @Composable () -> TextStyle,
  val nameStyle: @Composable () -> TextStyle,
)

@Composable
private fun homeLayoutForWidth(width: Dp): HomeLayout =
  when {
    width < 360.dp ->
      HomeLayout(
        columns = 1,
        horizontalPadding = 16.dp,
        cardSpacing = 12.dp,
        compactCards = true,
        greetingStyle = { MaterialTheme.typography.titleLarge },
        nameStyle = { MaterialTheme.typography.bodyMedium },
      )
    width < 600.dp ->
      HomeLayout(
        columns = 2,
        horizontalPadding = 16.dp,
        cardSpacing = 12.dp,
        compactCards = true,
        greetingStyle = { MaterialTheme.typography.headlineSmall },
        nameStyle = { MaterialTheme.typography.titleSmall },
      )
    width < 900.dp ->
      HomeLayout(
        columns = 3,
        horizontalPadding = 20.dp,
        cardSpacing = 16.dp,
        compactCards = false,
        greetingStyle = { MaterialTheme.typography.headlineMedium },
        nameStyle = { MaterialTheme.typography.titleMedium },
      )
    else ->
      HomeLayout(
        columns = 4,
        horizontalPadding = 24.dp,
        cardSpacing = 18.dp,
        compactCards = false,
        greetingStyle = { MaterialTheme.typography.headlineLarge },
        nameStyle = { MaterialTheme.typography.titleLarge },
      )
  }

// --- Header ---

@Composable
private fun HomeHeader(
  displayName: String,
  horizontalPadding: Dp,
  greetingStyle: @Composable () -> TextStyle,
  nameStyle: @Composable () -> TextStyle,
  onNotificationsClick: () -> Unit,
) {
  val headerShape = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp)

  Box(
    modifier =
      Modifier.fillMaxWidth()
        .clip(headerShape)
        .background(
          brush =
            Brush.verticalGradient(
              colors =
                listOf(
                  MaterialTheme.colorScheme.primary,
                  MaterialTheme.colorScheme.primary.copy(alpha = 0.88f),
                )
            )
        )
        .windowInsetsPadding(WindowInsets.statusBars)
        .padding(horizontal = horizontalPadding)
        .padding(top = 10.dp, bottom = 24.dp)
  ) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        NotificationButton(onClick = onNotificationsClick)
      }

      Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
          text = greetingForCurrentTime(),
          style = greetingStyle(),
          color = MaterialTheme.colorScheme.onPrimary,
          fontWeight = FontWeight.Bold,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )

        Text(
          text = displayName,
          style = nameStyle(),
          color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.80f),
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }
  }
}

@Composable
private fun NotificationButton(onClick: () -> Unit) {
  Box {
    Surface(
      onClick = onClick,
      modifier = Modifier.size(40.dp),
      shape = RoundedCornerShape(12.dp),
      color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.15f),
      border =
        BorderStroke(width = 1.dp, color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.25f)),
    ) {
      Box(contentAlignment = Alignment.Center) {
        Icon(
          imageVector = Icons.Rounded.Notifications,
          contentDescription = "Notifications",
          modifier = Modifier.size(20.dp),
          tint = MaterialTheme.colorScheme.onPrimary,
        )
      }
    }

    // Unread indicator dot
    Box(
      modifier =
        Modifier.align(Alignment.TopEnd)
          .padding(top = 7.dp, end = 7.dp)
          .size(7.dp)
          .clip(CircleShape)
          .background(MaterialTheme.colorScheme.errorContainer)
    )
  }
}

// --- Section header ---

@Composable
private fun WorkflowSectionHeader(modifier: Modifier = Modifier) {
  Column(modifier = modifier.fillMaxWidth().padding(bottom = 4.dp)) {
    Text(
      text = "Clinical Modules",
      style = MaterialTheme.typography.titleLarge,
      color = MaterialTheme.colorScheme.onSurface,
      fontWeight = FontWeight.Bold,
    )

    Spacer(modifier = Modifier.height(10.dp))
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f))
    Spacer(modifier = Modifier.height(2.dp))
  }
}

// --- Workflow cards ---

@Composable
private fun WorkflowCard(workflow: WorkflowCardSpec, onClick: () -> Unit, compact: Boolean) {
  Card(
    onClick = onClick,
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(20.dp),
    border = BorderStroke(width = 1.dp, color = MaterialTheme.colorScheme.outlineVariant),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp, pressedElevation = 4.dp),
  ) {
    Column(
      modifier =
        Modifier.fillMaxWidth()
          .padding(
            horizontal = if (compact) 14.dp else 18.dp,
            vertical = if (compact) 14.dp else 18.dp,
          ),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      WorkflowIcon(
        icon = workflow.icon,
        tint = workflow.color ?: MaterialTheme.colorScheme.primary,
        compact = compact,
      )

      Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
          text = workflow.title,
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.onSurface,
          fontWeight = FontWeight.SemiBold,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
        )

        Text(
          text = workflow.description,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = if (compact) 2 else 3,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }
  }
}

@Composable
private fun WorkflowIcon(icon: ImageVector, tint: Color, compact: Boolean) {
  val containerSize = if (compact) 44.dp else 50.dp
  val iconSize = if (compact) 22.dp else 26.dp
  val cornerRadius = if (compact) 13.dp else 15.dp

  Surface(
    modifier = Modifier.size(containerSize),
    shape = RoundedCornerShape(cornerRadius),
    color = tint.copy(alpha = 0.10f),
    border = BorderStroke(width = 1.dp, color = tint.copy(alpha = 0.20f)),
  ) {
    Box(contentAlignment = Alignment.Center) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint = tint,
        modifier = Modifier.size(iconSize),
      )
    }
  }
}

@Composable
private fun EmptyWorkflowCard() {
  Surface(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(20.dp),
    color = MaterialTheme.colorScheme.surface,
    border = BorderStroke(width = 1.dp, color = MaterialTheme.colorScheme.outlineVariant),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(32.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Text(
        text = "No modules configured",
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
        fontWeight = FontWeight.SemiBold,
      )
      Text(
        text = "Reporting workflows will appear here once configured by your administrator.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}

// --- Helpers ---

private fun greetingForCurrentTime(): String {
  val hour = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).hour
  return when (hour) {
    in 5..11 -> "Good Morning"
    in 12..16 -> "Good Afternoon"
    in 17..21 -> "Good Evening"
    else -> "Welcome Back"
  }
}
