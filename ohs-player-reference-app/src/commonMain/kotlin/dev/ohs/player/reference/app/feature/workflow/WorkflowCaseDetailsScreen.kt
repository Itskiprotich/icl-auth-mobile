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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Divider
import androidx.compose.material.ScrollableTabRow
import androidx.compose.material.Tab
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
internal fun WorkflowCaseDetailsScreen(
  questionnaireResponseId: String,
  onBack: () -> Unit,
  onTabActionClick: (WorkflowCaseTabActionRequest) -> Unit = {},
  modifier: Modifier = Modifier,
) {
  val screenState by
    produceState<WorkflowCaseDetailsScreenState>(
      initialValue = WorkflowCaseDetailsScreenState.Loading,
      questionnaireResponseId,
    ) {
      value =
        runCatching { loadWorkflowCaseDetails(questionnaireResponseId) }
          .fold(
            onSuccess = { details ->
              if (details == null) {
                WorkflowCaseDetailsScreenState.Error("This case summary could not be found.")
              } else {
                WorkflowCaseDetailsScreenState.Ready(details)
              }
            },
            onFailure = { error ->
              WorkflowCaseDetailsScreenState.Error(
                error.message ?: "The case summary could not be loaded."
              )
            },
          )
    }

  val tabs = (screenState as? WorkflowCaseDetailsScreenState.Ready)?.details?.tabs.orEmpty()
  var selectedTabIndex by rememberSaveable(questionnaireResponseId) { mutableIntStateOf(0) }

  LaunchedEffect(tabs.size) {
    selectedTabIndex = selectedTabIndex.coerceIn(0, (tabs.lastIndex).coerceAtLeast(0))
  }

  val activeTab = tabs.getOrNull(selectedTabIndex)

  Scaffold(
    modifier = modifier.fillMaxSize(),
    topBar = { WorkflowCaseDetailsTopBar(onBack = onBack) },
    floatingActionButton = {
      if (activeTab?.action != null) {
        FloatingActionButton(
          onClick = {
            onTabActionClick(
              WorkflowCaseTabActionRequest(
                questionnaireResponseId = questionnaireResponseId,
                tabId = activeTab.id,
              )
            )
          }
        ) {
          Icon(imageVector = Icons.Default.Add, contentDescription = "Add workflow information")
        }
      }
    },
  ) { innerPadding ->
    Box(
      modifier =
        Modifier.fillMaxSize()
          .background(MaterialTheme.colorScheme.background)
          .padding(innerPadding)
    ) {
      when (val state = screenState) {
        WorkflowCaseDetailsScreenState.Loading ->
          CircularProgressIndicator(
            strokeWidth = 4.dp,
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.42f),
          )

        is WorkflowCaseDetailsScreenState.Error ->
          WorkflowCenteredMessage(title = "Case summary unavailable", message = state.message)

        is WorkflowCaseDetailsScreenState.Ready ->
          WorkflowCaseDetailsContent(
            details = state.details,
            selectedTabIndex = selectedTabIndex,
            onTabSelected = { selectedTabIndex = it },
          )
      }
    }
  }
}

private sealed interface WorkflowCaseDetailsScreenState {
  data object Loading : WorkflowCaseDetailsScreenState

  data class Ready(val details: WorkflowCaseDetails) : WorkflowCaseDetailsScreenState

  data class Error(val message: String) : WorkflowCaseDetailsScreenState
}

@Composable
private fun WorkflowCaseDetailsContent(
  details: WorkflowCaseDetails,
  selectedTabIndex: Int,
  onTabSelected: (Int) -> Unit,
) {
  Column(
    modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    WorkflowCaseOverviewCard(details = details)

    if (details.tabs.isNotEmpty()) {
      ScrollableTabRow(
        selectedTabIndex = selectedTabIndex.coerceIn(0, details.tabs.lastIndex),
        backgroundColor = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.primary,
        edgePadding = 16.dp,
      ) {
        details.tabs.forEachIndexed { index, tab ->
          Tab(
            selected = selectedTabIndex == index,
            onClick = { onTabSelected(index) },
            text = {
              Text(
                text = tab.title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight =
                  if (selectedTabIndex == index) FontWeight.SemiBold else FontWeight.Medium,
              )
            },
          )
        }
      }
    }

    val activeTab = details.tabs.getOrNull(selectedTabIndex)
    if (activeTab == null) {
      WorkflowCenteredMessage(
        title = "No sections available",
        message = "There is no captured summary content for this case yet.",
      )
    } else if (activeTab.sections.isEmpty()) {
      Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        SummaryEmptyCard(message = activeTab.emptyMessage)
      }
    } else {
      LazyColumn(
        modifier = Modifier.weight(1f).fillMaxWidth(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        items(items = activeTab.sections, key = WorkflowCaseDetailSection::id) { section ->
          SummarySectionCard(section = section)
        }
      }
    }
  }
}

@Composable
private fun WorkflowCaseOverviewCard(details: WorkflowCaseDetails, modifier: Modifier = Modifier) {
  val overviewTitle = details.patientName.ifBlank { details.title.ifBlank { details.caseLabel } }
  val epidemicNumber = details.epidemicNumber.orEmpty()

  Card(
    modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp),
    shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
    colors =
      CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(18.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      Text(
        text = overviewTitle,
        style = MaterialTheme.typography.headlineMedium,
        color = MaterialTheme.colorScheme.onSurface,
        fontWeight = FontWeight.Bold,
      )

      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        OverviewStatCard(
          label = "Case",
          value = details.caseLabel,
          icon = Icons.Default.Info,
          modifier = Modifier.weight(if (epidemicNumber.isBlank()) 1f else 1.3f),
        )
        if (epidemicNumber.isNotBlank()) {
          OverviewStatCard(
            label = "EPID No",
            value = epidemicNumber,
            icon = Icons.Default.Search,
            modifier = Modifier.weight(1f),
          )
        }
      }
    }
  }
}

@Composable
private fun OverviewStatCard(
  label: String,
  value: String,
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  modifier: Modifier = Modifier,
) {
  Card(
    modifier = modifier.defaultMinSize(minHeight = 96.dp),
    shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 14.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
        )
        Text(
          text = label,
          style = MaterialTheme.typography.labelLarge,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      Text(
        text = value,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
        fontWeight = FontWeight.SemiBold,
      )
    }
  }
}

@Composable
private fun HighlightRows(highlights: List<WorkflowCaseDetailAnswer>) {
  highlights.chunked(2).forEach { row ->
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(14.dp),
      verticalAlignment = Alignment.Top,
    ) {
      row.forEach { highlight ->
        SummaryField(
          label = highlight.label,
          value = highlight.value,
          modifier = Modifier.weight(1f),
        )
      }

      if (row.size == 1) {
        Spacer(modifier = Modifier.weight(1f))
      }
    }
  }
}

@Composable
private fun SummaryField(label: String, value: String, modifier: Modifier = Modifier) {
  Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
    Text(
      text = label,
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Text(
      text = value.ifBlank { "—" },
      style = MaterialTheme.typography.titleMedium,
      color = MaterialTheme.colorScheme.onSurface,
      fontWeight = FontWeight.Medium,
    )
  }
}

@Composable
private fun SummarySectionCard(section: WorkflowCaseDetailSection, modifier: Modifier = Modifier) {
  Card(
    modifier = modifier.fillMaxWidth(),
    shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      if (section.title.isNotBlank()) {
        Text(
          text = section.title,
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.onSurface,
          fontWeight = FontWeight.SemiBold,
        )
      }

      section.answers.forEachIndexed { index, answer ->
        if (index > 0) {
          Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
        }

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
          Text(
            text = answer.label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          Text(
            text = answer.value,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium,
          )
        }
      }
    }
  }
}

@Composable
private fun SummaryEmptyCard(message: String, modifier: Modifier = Modifier) {
  Card(
    modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp),
    shape = androidx.compose.foundation.shape.RoundedCornerShape(22.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 24.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Text(
        text = "Nothing to show yet",
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
        fontWeight = FontWeight.SemiBold,
      )
      Text(
        text = message,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Spacer(modifier = Modifier.height(4.dp))
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WorkflowCaseDetailsTopBar(onBack: () -> Unit) {
  TopAppBar(
    title = { Text(text = "Case Summary", maxLines = 1, overflow = TextOverflow.Ellipsis) },
    navigationIcon = {
      IconButton(onClick = onBack) {
        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
