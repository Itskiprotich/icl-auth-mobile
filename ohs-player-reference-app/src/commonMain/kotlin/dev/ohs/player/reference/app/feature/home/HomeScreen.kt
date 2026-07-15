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
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
  onMoreClick: () -> Unit = {},
) {
  BoxWithConstraints(
    modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
  ) {
    val layout = homeLayoutForWidth(maxWidth)
    Column(modifier = Modifier.fillMaxSize()) {
      HomeHeader(
        displayName = uiState.displayName,
        horizontalPadding = layout.horizontalPadding,
        onNotificationsClick = onNotificationsClick,
        onMoreClick = onMoreClick,
      )

      LazyVerticalGrid(
        columns = GridCells.Fixed(layout.columns),
        modifier = Modifier.fillMaxSize(),
        contentPadding =
          PaddingValues(
            start = layout.horizontalPadding,
            end = layout.horizontalPadding,
            bottom = 32.dp,
            top = 30.dp,
          ),
        horizontalArrangement = Arrangement.spacedBy(layout.cardSpacing),
        verticalArrangement = Arrangement.spacedBy(layout.cardSpacing),
      ) {
        if (workflows.isEmpty()) {
          item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
            EmptyWorkflowCard()
          }
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

private data class HomeLayout(
  val columns: Int,
  val horizontalPadding: Dp,
  val cardSpacing: Dp,
  val compactCards: Boolean,
)

private fun homeLayoutForWidth(width: Dp): HomeLayout =
  when {
    width < 360.dp ->
      HomeLayout(columns = 1, horizontalPadding = 15.dp, cardSpacing = 12.dp, compactCards = true)

    width < 600.dp ->
      HomeLayout(columns = 2, horizontalPadding = 15.dp, cardSpacing = 12.dp, compactCards = true)

    width < 900.dp ->
      HomeLayout(columns = 3, horizontalPadding = 15.dp, cardSpacing = 16.dp, compactCards = false)

    else ->
      HomeLayout(columns = 4, horizontalPadding = 15.dp, cardSpacing = 18.dp, compactCards = false)
  }

@Composable
private fun HomeHeader(
  displayName: String,
  horizontalPadding: Dp,
  onNotificationsClick: () -> Unit,
  onMoreClick: () -> Unit,
) {
  val headerShape = RoundedCornerShape(bottomStart = 34.dp, bottomEnd = 34.dp)

  Column(
    modifier =
      Modifier.fillMaxWidth()
        .clip(headerShape)
        .background(MaterialTheme.colorScheme.primaryContainer)
        .padding(10.dp)
        .windowInsetsPadding(WindowInsets.statusBars)
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().height(58.dp).padding(horizontal = horizontalPadding),
      horizontalArrangement = Arrangement.End,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      IconButton(onClick = onMoreClick) {
        Icon(
          imageVector = Icons.Rounded.MoreVert,
          contentDescription = "More options",
          tint = MaterialTheme.colorScheme.onPrimaryContainer,
        )
      }
    }

    Row(
      modifier =
        Modifier.fillMaxWidth()
          .padding(start = horizontalPadding, end = horizontalPadding, top = 18.dp, bottom = 30.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Column(
        modifier = Modifier.weight(1f).padding(end = 16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        Text(
          text = greetingForCurrentTime(),
          style = MaterialTheme.typography.headlineSmall,
          color = MaterialTheme.colorScheme.onPrimaryContainer,
          fontWeight = FontWeight.Bold,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )

        Text(
          text = displayName,
          style = MaterialTheme.typography.titleSmall,
          color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.84f),
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }

      NotificationButton(onClick = onNotificationsClick)
    }
  }
}

@Composable
private fun NotificationButton(onClick: () -> Unit) {
  Box {
    Surface(
      onClick = onClick,
      modifier = Modifier.size(58.dp),
      shape = RoundedCornerShape(20.dp),
      color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.12f),
      border =
        BorderStroke(width = 1.dp, color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.55f)),
    ) {
      Box(contentAlignment = Alignment.Center) {
        Icon(
          imageVector = Icons.Rounded.Notifications,
          contentDescription = "Notifications",
          modifier = Modifier.size(27.dp),
          tint = MaterialTheme.colorScheme.onPrimaryContainer,
        )
      }
    }

    Box(
      modifier =
        Modifier.align(Alignment.TopEnd)
          .padding(top = 10.dp, end = 9.dp)
          .size(11.dp)
          .clip(CircleShape)
          .background(MaterialTheme.colorScheme.error)
    )
  }
}

@Composable
private fun WorkflowCard(workflow: WorkflowCardSpec, onClick: () -> Unit, compact: Boolean) {
  Card(
    onClick = onClick,
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(22.dp),
    border = BorderStroke(width = 1.dp, color = MaterialTheme.colorScheme.outlineVariant),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp, pressedElevation = 3.dp),
  ) {
    Column(
      modifier =
        Modifier.fillMaxWidth()
          .padding(
            horizontal = if (compact) 16.dp else 20.dp,
            vertical = if (compact) 16.dp else 20.dp,
          ),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      WorkflowIcon(
        icon = workflow.icon,
        tint = workflow.color ?: MaterialTheme.colorScheme.primary,
        compact = compact,
      )

      Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Column(
          modifier = Modifier.weight(1f).padding(end = 8.dp),
          verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
          Text(
            text = workflow.title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
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

        Icon(
          imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.padding(top = 5.dp).size(18.dp),
        )
      }

      Spacer(modifier = Modifier.height(if (compact) 2.dp else 8.dp))
    }
  }
}

@Composable
private fun WorkflowIcon(icon: ImageVector, tint: Color, compact: Boolean) {
  val containerSize = if (compact) 48.dp else 54.dp
  val iconSize = if (compact) 25.dp else 29.dp

  Surface(
    modifier = Modifier.size(containerSize),
    shape = RoundedCornerShape(if (compact) 15.dp else 17.dp),
    color = tint.copy(alpha = 0.10f),
    border = BorderStroke(width = 1.dp, color = tint.copy(alpha = 0.22f)),
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
    shape = RoundedCornerShape(22.dp),
    color = MaterialTheme.colorScheme.surface,
    border = BorderStroke(width = 1.dp, color = MaterialTheme.colorScheme.outlineVariant),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(28.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      Text(
        text = "No workflows available",
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
        fontWeight = FontWeight.Bold,
      )

      Text(
        text = "Configured reporting workflows will appear here.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
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
