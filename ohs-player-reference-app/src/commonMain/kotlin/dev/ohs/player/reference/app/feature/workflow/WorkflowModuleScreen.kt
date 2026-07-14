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
package dev.ohs.player.reference.app.feature.workflow

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
fun WorkflowModuleScreen(
  moduleId: String,
  nodeId: String? = null,
  onBack: () -> Unit,
  onNodeSelected: (String) -> Unit,
  modifier: Modifier = Modifier,
  onActionClick: (String, String) -> Unit = { _, _ -> },
  onMenuClick: () -> Unit = {},
) {
  val screenState by
    produceState<WorkflowModuleScreenState>(
      initialValue = WorkflowModuleScreenState.Loading,
      moduleId,
      nodeId,
    ) {
      value =
        runCatching {
            val module = WorkflowCatalogStore.module(moduleId)
            when {
              module == null ->
                WorkflowModuleScreenState.Missing(
                  title = "Workflow unavailable",
                  message = "This workflow is not configured in the bundled catalog.",
                )
              else -> {
                val node = module.resolveNode(nodeId)
                if (node == null) {
                  WorkflowModuleScreenState.Missing(
                    title = module.title,
                    message = "This step is not configured yet.",
                  )
                } else {
                  WorkflowModuleScreenState.Ready(module = module, node = node)
                }
              }
            }
          }
          .getOrElse { error ->
            WorkflowModuleScreenState.Missing(
              title = "Workflow unavailable",
              message = error.message ?: "The workflow catalog could not be loaded.",
            )
          }
    }

  Scaffold(
    modifier = modifier,
    containerColor = Color.Transparent,
    topBar = { WorkflowTopBar(onBack = onBack, onMenuClick = onMenuClick) },
  ) { innerPadding ->
    Box(
      modifier =
        Modifier.fillMaxSize()
          .background(
            Brush.verticalGradient(
              listOf(
                MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                MaterialTheme.colorScheme.background,
                MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.16f),
              )
            )
          )
          .padding(innerPadding),
    ) {
      when (val state = screenState) {
        WorkflowModuleScreenState.Loading ->
          Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
          }
        is WorkflowModuleScreenState.Missing ->
          WorkflowMessageScreen(title = state.title, subtitle = state.message)
        is WorkflowModuleScreenState.Ready ->
          WorkflowNodeContent(
            module = state.module,
            node = state.node,
            onNodeSelected = onNodeSelected,
            onActionClick = onActionClick,
          )
      }
    }
  }
}

private sealed interface WorkflowModuleScreenState {
  data object Loading : WorkflowModuleScreenState

  data class Ready(
    val module: WorkflowModule,
    val node: WorkflowNode,
  ) : WorkflowModuleScreenState

  data class Missing(
    val title: String,
    val message: String,
  ) : WorkflowModuleScreenState
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WorkflowTopBar(
  onBack: () -> Unit,
  onMenuClick: () -> Unit,
) {
  TopAppBar(
    title = {},
    navigationIcon = {
      IconButton(onClick = onBack) {
        Icon(
          imageVector = Icons.AutoMirrored.Filled.ArrowBack,
          contentDescription = "Back",
        )
      }
    },
    actions = {
      IconButton(onClick = onMenuClick) {
        Icon(
          imageVector = Icons.Default.MoreVert,
          contentDescription = "More options",
        )
      }
    },
    colors =
      TopAppBarDefaults.topAppBarColors(
        containerColor = MaterialTheme.colorScheme.primary,
        navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
        actionIconContentColor = MaterialTheme.colorScheme.onPrimary,
      ),
  )
}

@Composable
private fun WorkflowNodeContent(
  module: WorkflowModule,
  node: WorkflowNode,
  onNodeSelected: (String) -> Unit,
  onActionClick: (String, String) -> Unit,
) {
  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(bottom = 32.dp),
    verticalArrangement = Arrangement.spacedBy(22.dp),
  ) {
    item {
      WorkflowHeroHeader(
        module = module,
        node = node,
      )
    }

    if (node.sectionTitle.isNotBlank()) {
      item {
        Column(
          modifier = Modifier.padding(horizontal = 24.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          Text(
            text = "Ready For Action",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
          )
          Text(
            text = node.sectionTitle,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Bold,
          )
        }
      }
    }

    when {
      node.items.isEmpty() ->
        item {
          WorkflowEmptyState(
            title = node.title,
            message = emptyMessageFor(node.layout, module.title),
          )
        }
      node.layout == WorkflowNodeLayout.GRID ->
        items(node.items.chunked(2)) { rowItems ->
          Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
          ) {
            rowItems.forEachIndexed { index, item ->
              WorkflowGridCard(
                item = item,
                fallbackIcon = module.icon.toImageVector(),
                modifier = Modifier.weight(1f),
                accentIndex = index,
                onClick = { handleItemClick(node.id, item, onNodeSelected, onActionClick) },
              )
            }

            if (rowItems.size == 1) {
              Spacer(modifier = Modifier.weight(1f))
            }
          }
        }
      else ->
        items(node.items) { item ->
          WorkflowActionCard(
            item = item,
            modifier = Modifier.padding(horizontal = 24.dp),
            onClick = { handleItemClick(node.id, item, onNodeSelected, onActionClick) },
          )
        }
    }
  }
}

@Composable
private fun WorkflowHeroHeader(
  module: WorkflowModule,
  node: WorkflowNode,
) {
  val gradientColors =
    listOf(
      MaterialTheme.colorScheme.primary,
      MaterialTheme.colorScheme.tertiary,
    )

  Surface(
    modifier = Modifier.fillMaxWidth(),
    color = Color.Transparent,
    shadowElevation = 14.dp,
    shape = RoundedCornerShape(bottomStart = 30.dp, bottomEnd = 30.dp),
  ) {
    Column(
      modifier =
        Modifier.fillMaxWidth()
          .background(Brush.horizontalGradient(gradientColors))
          .padding(horizontal = 24.dp, vertical = 24.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
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
          WorkflowLabelPill(
            label = "Modular Workflow",
            containerColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.14f),
            contentColor = MaterialTheme.colorScheme.onPrimary,
          )
          Text(
            text = node.title,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onPrimary,
            fontWeight = FontWeight.Bold,
          )
          if (node.subtitle.isNotBlank()) {
            Text(
              text = node.subtitle,
              style = MaterialTheme.typography.bodyLarge,
              color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.86f),
            )
          }
        }

        Box(
          modifier =
            Modifier.size(72.dp)
              .clip(RoundedCornerShape(24.dp))
              .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.14f)),
          contentAlignment = Alignment.Center,
        ) {
          Icon(
            imageVector = module.icon.toImageVector(),
            contentDescription = null,
            modifier = Modifier.size(34.dp),
            tint = MaterialTheme.colorScheme.onPrimary,
          )
        }
      }

      Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        WorkflowMetricCard(label = "Module", value = module.title)
        WorkflowMetricCard(label = "Step", value = node.items.size.toString())
      }
    }
  }
}

@Composable
private fun WorkflowGridCard(
  item: WorkflowNodeItem,
  fallbackIcon: ImageVector,
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
  val isClickable = item.destinationNodeId != null || item.action != null

  Card(
    modifier = modifier,
    onClick = onClick,
    enabled = isClickable,
    shape = RoundedCornerShape(28.dp),
    colors =
      CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surface,
      ),
    border =
      BorderStroke(
        1.dp,
        accentColor.copy(alpha = 0.16f),
      ),
    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
  ) {
    Column(
      modifier =
        Modifier.fillMaxWidth()
          .height(214.dp)
          .padding(horizontal = 18.dp, vertical = 18.dp),
      verticalArrangement = Arrangement.SpaceBetween,
    ) {
      Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Box(
          modifier =
            Modifier.size(58.dp)
              .clip(RoundedCornerShape(18.dp))
              .background(accentColor.copy(alpha = 0.12f)),
          contentAlignment = Alignment.Center,
        ) {
          Icon(
            imageVector = item.icon?.toImageVector() ?: fallbackIcon,
            contentDescription = null,
            tint = accentColor,
            modifier = Modifier.size(28.dp),
          )
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text(
            text = item.title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
          )
          Text(
            text = item.description.ifBlank { "Open this workflow module to continue reporting." },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
          )
        }
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = if (item.action != null) "Launch" else "Continue",
          style = MaterialTheme.typography.labelLarge,
          color = accentColor,
          fontWeight = FontWeight.Bold,
        )
        Icon(
          imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
          contentDescription = null,
          tint = accentColor,
        )
      }
    }
  }
}

@Composable
private fun WorkflowActionCard(
  item: WorkflowNodeItem,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val isClickable = item.destinationNodeId != null || item.action != null

  Card(
    modifier = modifier.fillMaxWidth(),
    onClick = onClick,
    enabled = isClickable,
    shape = RoundedCornerShape(24.dp),
    colors =
      CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surface,
      ),
    border =
      BorderStroke(
        1.dp,
        MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
      ),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 18.dp),
      horizontalArrangement = Arrangement.spacedBy(14.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Box(
        modifier =
          Modifier.size(52.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          imageVector = item.icon?.toImageVector() ?: Icons.Default.Info,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
        )
      }

      Column(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        Text(
          text = item.title,
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.onSurface,
          fontWeight = FontWeight.Bold,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
        )
        Text(
          text =
            item.description.ifBlank {
              item.action?.subtitle
                ?.takeIf(String::isNotBlank)
                ?: "Open this destination to continue working on this case."
            },
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
        )
      }

      if (!item.trailingValue.isNullOrBlank()) {
        Column(horizontalAlignment = Alignment.End) {
          Text(
            text = item.trailingValue,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
          )
          item.trailingLabel?.takeIf(String::isNotBlank)?.let { label ->
            Text(
              text = label,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      } else if (isClickable) {
        Icon(
          imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.outline,
        )
      }
    }
  }
}

@Composable
private fun WorkflowMetricCard(
  label: String,
  value: String,
) {
  Surface(
    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.12f),
    shape = RoundedCornerShape(20.dp),
  ) {
    Column(
      modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
      verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
      Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.76f),
      )
      Text(
        text = value,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onPrimary,
        fontWeight = FontWeight.Bold,
      )
    }
  }
}

@Composable
private fun WorkflowLabelPill(
  label: String,
  containerColor: Color,
  contentColor: Color,
) {
  Surface(
    color = containerColor,
    shape = RoundedCornerShape(999.dp),
  ) {
    Text(
      text = label,
      modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
      style = MaterialTheme.typography.labelLarge,
      color = contentColor,
      fontWeight = FontWeight.Bold,
    )
  }
}

@Composable
private fun WorkflowEmptyState(
  title: String,
  message: String,
) {
  Card(
    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
    shape = RoundedCornerShape(26.dp),
    colors =
      CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surface,
      ),
    border =
      BorderStroke(
        1.dp,
        MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
      ),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 22.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onSurface,
        fontWeight = FontWeight.Bold,
      )
      Text(
        text = message,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}

@Composable
private fun WorkflowMessageScreen(
  title: String,
  subtitle: String,
) {
  Box(
    modifier = Modifier.fillMaxSize(),
    contentAlignment = Alignment.Center,
  ) {
    Column(
      modifier = Modifier.padding(24.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Text(
        text = title,
        style = MaterialTheme.typography.headlineSmall,
        color = MaterialTheme.colorScheme.onBackground,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
      )
      Text(
        text = subtitle,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
      )
    }
  }
}

private fun handleItemClick(
  nodeId: String,
  item: WorkflowNodeItem,
  onNodeSelected: (String) -> Unit,
  onActionClick: (String, String) -> Unit,
) {
  item.destinationNodeId?.let(onNodeSelected) ?: item.action?.let { onActionClick(nodeId, item.id) }
}

private fun emptyMessageFor(layout: WorkflowNodeLayout, moduleTitle: String): String =
  when (layout) {
    WorkflowNodeLayout.GRID -> "No forms are configured for $moduleTitle yet."
    WorkflowNodeLayout.ACTIONS -> "No actions are configured for this step yet."
  }
