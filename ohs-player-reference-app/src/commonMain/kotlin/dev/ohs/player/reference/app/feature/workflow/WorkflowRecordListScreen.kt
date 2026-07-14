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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.distinctUntilChanged

@Composable
fun WorkflowRecordListScreen(
  title: String,
  subtitle: String,
  resource: String,
  onBack: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val screenState by
    produceState<RecordListScreenState>(
      initialValue = RecordListScreenState.Loading,
      resource,
    ) {
      value =
        runCatching { WorkflowRecordsStore.records(resource) }
          .fold(
            onSuccess = RecordListScreenState::Ready,
            onFailure = {
              RecordListScreenState.Error(
                it.message ?: "The record collection could not be loaded.",
              )
            },
          )
    }

  Scaffold(
    modifier = modifier,
    containerColor = Color.Transparent,
    topBar = {
      RecordListTopBar(
        title = title,
        onBack = onBack,
      )
    },
  ) { innerPadding ->
    Box(
      modifier =
        Modifier.fillMaxSize()
          .background(
            Brush.verticalGradient(
              listOf(
                MaterialTheme.colorScheme.primary.copy(alpha = 0.06f),
                MaterialTheme.colorScheme.background,
                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.18f),
              )
            )
          )
          .padding(innerPadding),
    ) {
      when (val state = screenState) {
        RecordListScreenState.Loading ->
          Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
          }
        is RecordListScreenState.Error ->
          RecordListMessage(
            title = "Case list unavailable",
            message = state.message,
          )
        is RecordListScreenState.Ready ->
          WorkflowRecordListContent(
            collection = state.collection,
            screenTitle = title,
            screenSubtitle = subtitle,
          )
      }
    }
  }
}

private sealed interface RecordListScreenState {
  data object Loading : RecordListScreenState

  data class Ready(val collection: WorkflowRecordCollection) : RecordListScreenState

  data class Error(val message: String) : RecordListScreenState
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecordListTopBar(
  title: String,
  onBack: () -> Unit,
) {
  TopAppBar(
    title = { Text(title) },
    navigationIcon = {
      IconButton(onClick = onBack) {
        Icon(
          imageVector = Icons.AutoMirrored.Filled.ArrowBack,
          contentDescription = "Back",
        )
      }
    },
    colors =
      TopAppBarDefaults.topAppBarColors(
        containerColor = MaterialTheme.colorScheme.primary,
        titleContentColor = MaterialTheme.colorScheme.onPrimary,
        navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
      ),
  )
}

@Composable
private fun WorkflowRecordListContent(
  collection: WorkflowRecordCollection,
  screenTitle: String,
  screenSubtitle: String,
) {
  var query by rememberSaveable(collection.id) { mutableStateOf("") }
  var visibleCount by rememberSaveable(collection.id, query) {
    mutableIntStateOf(collection.pageSize.coerceAtLeast(1))
  }
  val listState = rememberLazyListState()
  val filteredRecords =
    remember(collection, query) {
      if (query.isBlank()) {
        collection.records
      } else {
        val normalizedQuery = query.trim().lowercase()
        collection.records.filter { record ->
          record.title.contains(normalizedQuery, ignoreCase = true) ||
            record.subtitle.contains(normalizedQuery, ignoreCase = true) ||
            record.status.contains(normalizedQuery, ignoreCase = true) ||
            record.meta.any { it.contains(normalizedQuery, ignoreCase = true) } ||
            record.fields.any {
              it.label.contains(normalizedQuery, ignoreCase = true) ||
                it.value.contains(normalizedQuery, ignoreCase = true)
            }
        }
      }
    }

  LaunchedEffect(filteredRecords.size, collection.pageSize, query) {
    visibleCount = filteredRecords.size.coerceAtMost(collection.pageSize.coerceAtLeast(1))
  }

  LaunchedEffect(listState, filteredRecords.size, visibleCount, collection.pageSize) {
    snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0 }
      .distinctUntilChanged()
      .collect { index ->
        val pageSize = collection.pageSize.coerceAtLeast(1)
        if (index >= visibleCount - 2 && visibleCount < filteredRecords.size) {
          visibleCount = (visibleCount + pageSize).coerceAtMost(filteredRecords.size)
        }
      }
  }

  val visibleRecords = filteredRecords.take(visibleCount)
  val hasMore = visibleCount < filteredRecords.size

  LazyColumn(
    state = listState,
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(start = 24.dp, top = 20.dp, end = 24.dp, bottom = 28.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    item {
      CaseListHeroCard(
        title = screenTitle,
        subtitle = screenSubtitle.ifBlank { collection.subtitle },
        filteredCount = filteredRecords.size,
        totalCount = collection.records.size,
      )
    }

    item {
      OutlinedTextField(
        value = query,
        onValueChange = { query = it },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        singleLine = true,
        label = { Text("Search cases") },
        placeholder = { Text("Search by patient, status, or field value") },
        leadingIcon = {
          Icon(
            imageVector = Icons.Default.Search,
            contentDescription = null,
          )
        },
      )
    }

    if (visibleRecords.isEmpty()) {
      item {
        RecordListMessage(
          title = "No cases found",
          message = collection.emptyMessage,
        )
      }
    } else {
      items(visibleRecords, key = { it.id }) { record ->
        WorkflowRecordCard(record = record)
      }
    }

    if (hasMore) {
      item {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
          Text(
            text = "Loading more records as you browse",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
    }
  }
}

@Composable
private fun CaseListHeroCard(
  title: String,
  subtitle: String,
  filteredCount: Int,
  totalCount: Int,
) {
  Card(
    shape = RoundedCornerShape(30.dp),
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
      modifier =
        Modifier.fillMaxWidth()
          .background(
            Brush.verticalGradient(
              listOf(
                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.72f),
                MaterialTheme.colorScheme.surface,
              )
            )
          )
          .padding(22.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
          text = "Paginated Records Host",
          style = MaterialTheme.typography.labelLarge,
          color = MaterialTheme.colorScheme.primary,
          fontWeight = FontWeight.Bold,
        )
        Text(
          text = title,
          style = MaterialTheme.typography.headlineMedium,
          color = MaterialTheme.colorScheme.onSurface,
          fontWeight = FontWeight.Bold,
        )
        Text(
          text = subtitle.ifBlank { "Browse, search, and paginate records from the workflow asset." },
          style = MaterialTheme.typography.bodyLarge,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }

      Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        CaseListMetricChip(label = "Visible", value = filteredCount.toString())
        CaseListMetricChip(label = "Total", value = totalCount.toString())
      }
    }
  }
}

@Composable
private fun CaseListMetricChip(
  label: String,
  value: String,
) {
  Surface(
    shape = RoundedCornerShape(18.dp),
    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
      )
      Text(
        text = value,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurface,
        fontWeight = FontWeight.Bold,
      )
    }
  }
}

@Composable
private fun WorkflowRecordCard(record: WorkflowRecord) {
  Card(
    shape = RoundedCornerShape(24.dp),
    colors =
      CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surface,
      ),
    border =
      BorderStroke(
        1.dp,
        record.statusTone.borderColor().copy(alpha = 0.16f),
      ),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 18.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
      ) {
        Column(
          modifier = Modifier.weight(1f),
          verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          Text(
            text = record.title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
          )
          if (record.subtitle.isNotBlank()) {
            Text(
              text = record.subtitle,
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
        if (record.status.isNotBlank()) {
          Surface(
            color = record.statusTone.containerColor(),
            shape = RoundedCornerShape(999.dp),
          ) {
            Text(
              text = record.status,
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
              style = MaterialTheme.typography.labelLarge,
              color = record.statusTone.contentColor(),
              fontWeight = FontWeight.Bold,
            )
          }
        }
      }

      if (record.meta.isNotEmpty()) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          record.meta.take(3).forEach { meta ->
            Surface(
              color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
              shape = RoundedCornerShape(999.dp),
            ) {
              Text(
                text = meta,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }
      }

      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        record.fields.forEach { field ->
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
          ) {
            Text(
              text = field.label,
              modifier = Modifier.weight(1f),
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
              text = field.value,
              modifier = Modifier.weight(1f),
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurface,
              fontWeight = FontWeight.SemiBold,
            )
          }
        }
      }
    }
  }
}

@Composable
private fun RecordListMessage(
  title: String,
  message: String,
) {
  Card(
    shape = RoundedCornerShape(24.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(22.dp),
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
private fun WorkflowRecordTone.containerColor(): Color =
  when (this) {
    WorkflowRecordTone.NEUTRAL -> MaterialTheme.colorScheme.surfaceVariant
    WorkflowRecordTone.SUCCESS -> MaterialTheme.colorScheme.tertiaryContainer
    WorkflowRecordTone.WARNING -> MaterialTheme.colorScheme.primaryContainer
    WorkflowRecordTone.CRITICAL -> MaterialTheme.colorScheme.errorContainer
    WorkflowRecordTone.INFO -> MaterialTheme.colorScheme.secondaryContainer
  }

@Composable
private fun WorkflowRecordTone.contentColor(): Color =
  when (this) {
    WorkflowRecordTone.NEUTRAL -> MaterialTheme.colorScheme.onSurfaceVariant
    WorkflowRecordTone.SUCCESS -> MaterialTheme.colorScheme.onTertiaryContainer
    WorkflowRecordTone.WARNING -> MaterialTheme.colorScheme.onPrimaryContainer
    WorkflowRecordTone.CRITICAL -> MaterialTheme.colorScheme.onErrorContainer
    WorkflowRecordTone.INFO -> MaterialTheme.colorScheme.onSecondaryContainer
  }

@Composable
private fun WorkflowRecordTone.borderColor(): Color =
  when (this) {
    WorkflowRecordTone.NEUTRAL -> MaterialTheme.colorScheme.outline
    WorkflowRecordTone.SUCCESS -> MaterialTheme.colorScheme.tertiary
    WorkflowRecordTone.WARNING -> MaterialTheme.colorScheme.primary
    WorkflowRecordTone.CRITICAL -> MaterialTheme.colorScheme.error
    WorkflowRecordTone.INFO -> MaterialTheme.colorScheme.secondary
  }
