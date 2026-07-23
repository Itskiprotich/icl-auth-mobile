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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
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
                  WorkflowModuleScreenState.Ready(
                    module = module,
                    node = node.withRuntimeSummaries(),
                  )
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

  Column(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
    // Gradient header — same design language as HomeScreen
    val headerTitle =
      when (val s = screenState) {
        is WorkflowModuleScreenState.Ready -> s.node.title.ifBlank { s.module.title }
        is WorkflowModuleScreenState.Missing -> s.title
        WorkflowModuleScreenState.Loading -> ""
      }
    val headerSubtitle =
      when (val s = screenState) {
        is WorkflowModuleScreenState.Ready ->
          if (s.node.items.any { it.action != null }) "Select an action to proceed"
          else "Select a form to proceed"
        else -> ""
      }

    WorkflowGradientHeader(title = headerTitle, subtitle = headerSubtitle, onBack = onBack)

    // Body
    Box(modifier = Modifier.fillMaxSize()) {
      when (val state = screenState) {
        WorkflowModuleScreenState.Loading ->
          Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(
              color = MaterialTheme.colorScheme.primary,
              trackColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.42f),
            )
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

// ---------------------------------------------------------------------------
// State
// ---------------------------------------------------------------------------

private sealed interface WorkflowModuleScreenState {
  data object Loading : WorkflowModuleScreenState

  data class Ready(val module: WorkflowModule, val node: WorkflowNode) : WorkflowModuleScreenState

  data class Missing(val title: String, val message: String) : WorkflowModuleScreenState
}

// ---------------------------------------------------------------------------
// Gradient header (mirrors HomeScreen HomeHeader)
// ---------------------------------------------------------------------------

@Composable
private fun WorkflowGradientHeader(title: String, subtitle: String, onBack: () -> Unit) {
  val shape = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp)

  Box(
    modifier =
      Modifier.fillMaxWidth()
        .clip(shape)
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
        .padding(horizontal = 16.dp)
        .padding(top = 8.dp, bottom = 24.dp)
  ) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
      // Back button row
      Surface(
        onClick = onBack,
        modifier = Modifier.size(40.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.15f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.25f)),
      ) {
        Box(contentAlignment = Alignment.Center) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "Back",
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.size(20.dp),
          )
        }
      }

      // Title + subtitle
      Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        if (title.isNotBlank()) {
          Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onPrimary,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
          )
        }
        if (subtitle.isNotBlank()) {
          Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.75f),
          )
        }
      }
    }
  }
}

// ---------------------------------------------------------------------------
// Node content
// ---------------------------------------------------------------------------

@Composable
private fun WorkflowNodeContent(
  module: WorkflowModule,
  node: WorkflowNode,
  onNodeSelected: (String) -> Unit,
  onActionClick: (String, String) -> Unit,
) {
  // Any node tagged presentation = BOTTOM_SHEET in the catalog is a dependent sub-step
  // (e.g. picking between County/Sub County vs Community questionnaire) rather than an
  // independent workflow, so instead of pushing a new screen we surface its items as a
  // bottom sheet grid. This dispatch is shared by the on-screen list AND by items tapped
  // from inside an already-open bottom sheet, so BOTTOM_SHEET nodes chain and compose
  // anywhere in the workflow graph, not just for this one screen.
  var bottomSheetNode by remember(node.id) { mutableStateOf<WorkflowNode?>(null) }

  fun dispatchItemClick(originNodeId: String, item: WorkflowNodeItem) {
    val destinationId = item.destinationNodeId
    if (destinationId != null) {
      val destinationNode = module.findNode(destinationId)
      if (
        destinationNode != null &&
          destinationNode.presentation == WorkflowNodePresentation.BOTTOM_SHEET
      ) {
        bottomSheetNode = destinationNode
      } else {
        bottomSheetNode = null
        onNodeSelected(destinationId)
      }
      return
    }
    item.action?.let {
      bottomSheetNode = null
      onActionClick(originNodeId, item.id)
    }
  }

  fun onItemClick(item: WorkflowNodeItem) = dispatchItemClick(node.id, item)

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(start = 16.dp, top = 20.dp, end = 16.dp, bottom = 40.dp),
    verticalArrangement = Arrangement.spacedBy(0.dp),
  ) {
    if (node.sectionTitle.isNotBlank()) {
      item {
        Text(
          text = node.sectionTitle,
          style = MaterialTheme.typography.labelLarge,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          fontWeight = FontWeight.SemiBold,
          modifier = Modifier.padding(bottom = 12.dp),
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
      node.items.any { it.action != null } -> {
        // Terminal action level — show full action cards WITH icons
        item {
          Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
          ) {
            Column {
              node.items.forEachIndexed { index, item ->
                WorkflowActionRow(item = item, onClick = { onItemClick(item) })
                if (index < node.items.lastIndex) {
                  HorizontalDivider(
                    modifier = Modifier.padding(start = 72.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                  )
                }
              }
            }
          }
        }
      }
      else -> {
        // Navigation level — clean text-only list, NO icons
        item {
          Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
          ) {
            Column {
              node.items.forEachIndexed { index, item ->
                WorkflowFormRow(item = item, onClick = { onItemClick(item) })
                if (index < node.items.lastIndex) {
                  HorizontalDivider(
                    modifier = Modifier.padding(start = 20.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                  )
                }
              }
            }
          }
        }
      }
    }
  }

  bottomSheetNode?.let { sheetNode ->
    WorkflowActionsBottomSheet(
      node = sheetNode,
      onDismiss = { bottomSheetNode = null },
      onItemClick = { item -> dispatchItemClick(sheetNode.id, item) },
    )
  }
}

// ---------------------------------------------------------------------------
// Bottom sheet — a reusable grid presentation for any BOTTOM_SHEET-tagged node.
// e.g. tapping "Add" on the Social Investigation Form surfaces this sheet with
// County/Sub County Questionnaire and Community Questionnaire as a 2-up grid,
// since those are dependent categories of one form rather than independent
// workflows. Any other node in the catalog can opt into the same presentation.
// ---------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WorkflowActionsBottomSheet(
  node: WorkflowNode,
  onDismiss: () -> Unit,
  onItemClick: (WorkflowNodeItem) -> Unit,
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  // Resolve record counts (e.g. "3 Entries") the same way the full-screen action list does,
  // so the bottom sheet grid stays consistent with that presentation.
  val resolvedNode by
    produceState(initialValue = node, node.id) { value = node.withRuntimeSummaries() }

  ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 32.dp),
      verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
      Text(
        text = node.title,
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onSurface,
        fontWeight = FontWeight.Bold,
      )
      if (node.subtitle.isNotBlank()) {
        Text(
          text = node.subtitle,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.padding(bottom = 12.dp),
        )
      } else {
        Spacer(modifier = Modifier.padding(top = 8.dp))
      }

      if (resolvedNode.items.isEmpty()) {
        WorkflowEmptyState(title = node.title, message = emptyMessageFor(node.layout, node.title))
      } else {
        resolvedNode.items.chunked(2).forEach { rowItems ->
          Row(
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            rowItems.forEach { item ->
              Box(modifier = Modifier.weight(1f)) {
                WorkflowBottomSheetActionCard(item = item, onClick = { onItemClick(item) })
              }
            }
            if (rowItems.size == 1) {
              Spacer(modifier = Modifier.weight(1f))
            }
          }
        }
      }
    }
  }
}

@Composable
private fun WorkflowBottomSheetActionCard(item: WorkflowNodeItem, onClick: () -> Unit) {
  val isClickable = item.destinationNodeId != null || item.action != null
  val icon = item.action.leadingIcon(item.icon?.toImageVector() ?: Icons.AutoMirrored.Filled.List)
  val (iconBg, iconTint) =
    when (item.action?.type) {
      WorkflowActionType.QUESTIONNAIRE ->
        MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.primary
      WorkflowActionType.RECORD_LIST ->
        MaterialTheme.colorScheme.secondaryContainer to
          MaterialTheme.colorScheme.onSecondaryContainer
      null -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.primary
    }

  Surface(
    onClick = onClick,
    enabled = isClickable,
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(20.dp),
    color = MaterialTheme.colorScheme.surface,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 20.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Surface(shape = CircleShape, color = iconBg, modifier = Modifier.size(48.dp)) {
        Box(contentAlignment = Alignment.Center) {
          Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(22.dp),
          )
        }
      }
      Text(
        text = item.title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurface,
        fontWeight = FontWeight.SemiBold,
        textAlign = TextAlign.Center,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
      )
      if (!item.trailingValue.isNullOrBlank()) {
        Text(
          text = "${item.trailingValue} ${item.trailingLabel ?: ""}".trim(),
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
  }
}

// ---------------------------------------------------------------------------
// Form row — navigation items, NO icon (disease / sub-form selection)
// ---------------------------------------------------------------------------

@Composable
private fun WorkflowFormRow(item: WorkflowNodeItem, onClick: () -> Unit) {
  val isClickable = item.destinationNodeId != null || item.action != null

  Surface(
    onClick = onClick,
    enabled = isClickable,
    modifier = Modifier.fillMaxWidth(),
    color = MaterialTheme.colorScheme.surface,
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
      horizontalArrangement = Arrangement.spacedBy(12.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
          text = item.title,
          style = MaterialTheme.typography.bodyLarge,
          color = MaterialTheme.colorScheme.onSurface,
          fontWeight = FontWeight.Medium,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
        )
        if (!item.trailingValue.isNullOrBlank()) {
          Text(
            text = "${item.trailingValue} ${item.trailingLabel ?: ""}".trim(),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }

      if (isClickable) {
        Icon(
          imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.outline,
          modifier = Modifier.size(20.dp),
        )
      }
    }
  }
}

// ---------------------------------------------------------------------------
// Action row — terminal items WITH icons (Add New Case / View Cases)
// ---------------------------------------------------------------------------

@Composable
private fun WorkflowActionRow(item: WorkflowNodeItem, onClick: () -> Unit) {
  val isClickable = item.destinationNodeId != null || item.action != null
  val icon = item.action.leadingIcon(item.icon?.toImageVector() ?: Icons.AutoMirrored.Filled.List)
  val (iconBg, iconTint) =
    when (item.action?.type) {
      WorkflowActionType.QUESTIONNAIRE ->
        MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.primary
      WorkflowActionType.RECORD_LIST ->
        MaterialTheme.colorScheme.secondaryContainer to
          MaterialTheme.colorScheme.onSecondaryContainer
      null -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.primary
    }

  Surface(
    onClick = onClick,
    enabled = isClickable,
    modifier = Modifier.fillMaxWidth(),
    color = MaterialTheme.colorScheme.surface,
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
      horizontalArrangement = Arrangement.spacedBy(14.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      // Icon container
      Surface(shape = RoundedCornerShape(14.dp), color = iconBg, modifier = Modifier.size(44.dp)) {
        Box(contentAlignment = Alignment.Center) {
          Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(22.dp),
          )
        }
      }

      Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
          text = item.title,
          style = MaterialTheme.typography.bodyLarge,
          color = MaterialTheme.colorScheme.onSurface,
          fontWeight = FontWeight.SemiBold,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
        )
      }

      Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
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
        }
        if (isClickable) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.outline,
            modifier = Modifier.size(20.dp),
          )
        }
      }
    }
  }
}

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

private fun WorkflowAction?.leadingIcon(fallback: ImageVector): ImageVector =
  when (this?.type) {
    WorkflowActionType.QUESTIONNAIRE -> Icons.Filled.AddCircle
    WorkflowActionType.RECORD_LIST -> Icons.AutoMirrored.Filled.List
    null -> fallback
  }

@Composable
private fun WorkflowEmptyState(title: String, message: String) {
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
private fun WorkflowMessageScreen(title: String, subtitle: String) {
  Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
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

private suspend fun WorkflowNode.withRuntimeSummaries(): WorkflowNode =
  copy(items = items.map { it.withRuntimeSummary() })

private suspend fun WorkflowNodeItem.withRuntimeSummary(): WorkflowNodeItem {
  val recordListAction = action?.takeIf { it.type == WorkflowActionType.RECORD_LIST } ?: return this
  val count = loadWorkflowRecordCount(recordListAction.resource)
  return copy(trailingValue = count.toString(), trailingLabel = trailingLabel ?: "Cases")
}

private fun emptyMessageFor(layout: WorkflowNodeLayout, moduleTitle: String): String =
  when (layout) {
    WorkflowNodeLayout.GRID -> "No forms are configured for $moduleTitle yet."
    WorkflowNodeLayout.ACTIONS -> "No actions are configured for this step yet."
  }
