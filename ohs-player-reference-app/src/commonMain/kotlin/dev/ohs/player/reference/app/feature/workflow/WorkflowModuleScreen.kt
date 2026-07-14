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
    containerColor = MaterialTheme.colorScheme.background,
    topBar = { WorkflowTopBar(onBack = onBack, onMenuClick = onMenuClick) },
  ) { innerPadding ->
    Box(
      modifier =
        Modifier.fillMaxSize()
          .background(MaterialTheme.colorScheme.background)
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
    contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 24.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    item {
      WorkflowSummaryCard(
        module = module,
        node = node,
      )
    }

    if (node.sectionTitle.isNotBlank()) {
      item {
        Text(
          text = node.sectionTitle,
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.onBackground,
          fontWeight = FontWeight.Bold,
        )
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
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            rowItems.forEach { item ->
              WorkflowGridCard(
                item = item,
                fallbackIcon = module.icon.toImageVector(),
                modifier = Modifier.weight(1f),
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
            onClick = { handleItemClick(node.id, item, onNodeSelected, onActionClick) },
          )
        }
    }
  }
}

@Composable
private fun WorkflowSummaryCard(
  module: WorkflowModule,
  node: WorkflowNode,
) {
  Card(
    shape = RoundedCornerShape(20.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(18.dp),
      horizontalArrangement = Arrangement.spacedBy(16.dp),
      verticalAlignment = Alignment.Top,
    ) {
      Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
      ) {
        Box(
          modifier = Modifier.size(56.dp),
          contentAlignment = Alignment.Center,
        ) {
          Icon(
            imageVector = module.icon.toImageVector(),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
          )
        }
      }

      Column(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        Text(
          text = module.title,
          style = MaterialTheme.typography.labelLarge,
          color = MaterialTheme.colorScheme.primary,
          fontWeight = FontWeight.Bold,
        )
        Text(
          text = node.title,
          style = MaterialTheme.typography.headlineSmall,
          color = MaterialTheme.colorScheme.onSurface,
          fontWeight = FontWeight.Bold,
        )
        if (node.subtitle.isNotBlank()) {
          Text(
            text = node.subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
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
) {
  val isClickable = item.destinationNodeId != null || item.action != null

  Card(
    modifier = modifier,
    onClick = onClick,
    enabled = isClickable,
    shape = RoundedCornerShape(20.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().height(188.dp).padding(16.dp),
      verticalArrangement = Arrangement.SpaceBetween,
    ) {
      Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Surface(
          shape = RoundedCornerShape(14.dp),
          color = MaterialTheme.colorScheme.primaryContainer,
        ) {
          Box(
            modifier = Modifier.size(48.dp),
            contentAlignment = Alignment.Center,
          ) {
            Icon(
              imageVector = item.icon?.toImageVector() ?: fallbackIcon,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
            )
          }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text(
            text = item.title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
          )
          Text(
            text = item.description.ifBlank { "Open this workflow step to continue." },
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
          text = if (item.action != null) "Open" else "Continue",
          style = MaterialTheme.typography.labelLarge,
          color = MaterialTheme.colorScheme.primary,
          fontWeight = FontWeight.Bold,
        )
        Icon(
          imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
        )
      }
    }
  }
}

@Composable
private fun WorkflowActionCard(
  item: WorkflowNodeItem,
  onClick: () -> Unit,
) {
  val isClickable = item.destinationNodeId != null || item.action != null

  Card(
    modifier = Modifier.fillMaxWidth(),
    onClick = onClick,
    enabled = isClickable,
    shape = RoundedCornerShape(20.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp),
      horizontalArrangement = Arrangement.spacedBy(14.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
      ) {
        Box(
          modifier = Modifier.size(48.dp),
          contentAlignment = Alignment.Center,
        ) {
          Icon(
            imageVector = item.icon?.toImageVector() ?: Icons.Default.Info,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
          )
        }
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
              item.action?.subtitle?.takeIf(String::isNotBlank)
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
private fun WorkflowEmptyState(
  title: String,
  message: String,
) {
  Card(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(20.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 20.dp),
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
