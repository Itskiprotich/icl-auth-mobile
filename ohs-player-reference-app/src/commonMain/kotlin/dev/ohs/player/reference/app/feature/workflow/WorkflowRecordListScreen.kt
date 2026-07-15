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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.ohs.player.reference.app.feature.home.components.RecordListMessage
import dev.ohs.player.reference.app.feature.home.components.RecordListTopBar
import dev.ohs.player.reference.app.feature.workflow.models.RecordFilters

private const val RECORDS_PER_PAGE = 50

@Composable
fun WorkflowRecordListScreen(
  title: String,
  subtitle: String,
  resource: String,
  onBack: () -> Unit,
  modifier: Modifier = Modifier,
) {
  var showFilters by rememberSaveable { mutableStateOf(false) }

  var filters by remember { mutableStateOf(RecordFilters()) }

  val screenState by
    produceState<RecordListScreenState>(
      initialValue = RecordListScreenState.Loading,
      title,
      subtitle,
      resource,
    ) {
      value =
        runCatching {
            if (WorkflowFhirStore.isPersistenceAvailable) {
              RecordListScreenState.Ready(
                collection =
                  buildStoredResponseCollection(
                    title = title,
                    subtitle = subtitle,
                    responses = WorkflowFhirStore.listQuestionnaireResponses(),
                  ),
                sourceLabel = "Local FHIR Store",
              )
            } else {
              val assetCollection = WorkflowRecordsStore.records(resource)

              RecordListScreenState.Ready(
                collection =
                  assetCollection.copy(
                    title = title.ifBlank { assetCollection.title },
                    subtitle = subtitle.ifBlank { assetCollection.subtitle },
                  ),
                sourceLabel = "Sample Records",
              )
            }
          }
          .getOrElse { error ->
            RecordListScreenState.Error(error.message ?: "The case list could not be loaded.")
          }
    }

  Scaffold(
    modifier = modifier,
    containerColor = MaterialTheme.colorScheme.background,
    topBar = {
      RecordListTopBar(
        title = title,
        onBack = onBack,
        onFilterClick = { showFilters = true },
        filtersActive = filters.isActive,
      )
    },
  ) { innerPadding ->
    Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
      when (val state = screenState) {
        RecordListScreenState.Loading -> {
          Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
          }
        }

        is RecordListScreenState.Error -> {
          Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            RecordListMessage(title = "Case list unavailable", message = state.message)
          }
        }

        is RecordListScreenState.Ready -> {
          WorkflowRecordListContent(collection = state.collection, filters = filters)

          if (showFilters) {
            val counties =
              state.collection.records
                .mapNotNull { it.fieldValue("County") }
                .filter(String::isNotBlank)
                .distinct()
                .sorted()

            val subCounties =
              state.collection.records
                .filter { record ->
                  filters.county == null ||
                    record.fieldValue("County").equals(filters.county, ignoreCase = true)
                }
                .mapNotNull { it.fieldValue("Sub County", "Sub-county", "Subcounty") }
                .filter(String::isNotBlank)
                .distinct()
                .sorted()

            CaseFilterDialog(
              counties = counties,
              subCounties = subCounties,
              initialFilters = filters,
              onDismiss = { showFilters = false },
              onApply = {
                filters = it
                showFilters = false
              },
              onClear = {
                filters = RecordFilters()
                showFilters = false
              },
            )
          }
        }
      }
    }
  }
}

private sealed interface RecordListScreenState {
  data object Loading : RecordListScreenState

  data class Ready(val collection: WorkflowRecordCollection, val sourceLabel: String) :
    RecordListScreenState

  data class Error(val message: String) : RecordListScreenState
}

@Composable
private fun WorkflowRecordListContent(
  collection: WorkflowRecordCollection,
  filters: RecordFilters,
) {
  var query by rememberSaveable(collection.id) { mutableStateOf("") }

  var currentPage by
    rememberSaveable(collection.id, query, filters.county, filters.subCounty) {
      mutableIntStateOf(0)
    }

  val filteredRecords =
    remember(collection, query, filters) {
      collection.records.filter { record ->
        val matchesQuery =
          query.isBlank() ||
            record.title.contains(query, ignoreCase = true) ||
            record.subtitle.contains(query, ignoreCase = true) ||
            record.status.contains(query, ignoreCase = true) ||
            record.meta.any { it.contains(query, ignoreCase = true) } ||
            record.fields.any { field ->
              field.label.contains(query, ignoreCase = true) ||
                field.value.contains(query, ignoreCase = true)
            }

        val matchesCounty =
          filters.county == null ||
            record.fieldValue("County").equals(filters.county, ignoreCase = true)

        val matchesSubCounty =
          filters.subCounty == null ||
            record
              .fieldValue("Sub County", "Sub-county", "Subcounty")
              .equals(filters.subCounty, ignoreCase = true)

        matchesQuery && matchesCounty && matchesSubCounty
      }
    }

  val totalPages =
    remember(filteredRecords.size) {
      if (filteredRecords.isEmpty()) {
        1
      } else {
        (filteredRecords.size + RECORDS_PER_PAGE - 1) / RECORDS_PER_PAGE
      }
    }

  LaunchedEffect(totalPages) { currentPage = currentPage.coerceIn(0, totalPages - 1) }

  val pageStart = currentPage * RECORDS_PER_PAGE

  val pageRecords = filteredRecords.drop(pageStart).take(RECORDS_PER_PAGE)

  val listState = rememberLazyListState()

  LaunchedEffect(currentPage) { listState.scrollToItem(0) }
  Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
    LazyColumn(
      state = listState,
      modifier = Modifier.weight(1f).fillMaxSize().background(MaterialTheme.colorScheme.background),
      contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 28.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      item {
        CaseSearchField(
          query = query,
          onQueryChange = {
            query = it
            currentPage = 0
          },
        )
      }

      item {
        ResultsSummary(
          totalResults = filteredRecords.size,
          currentPage = currentPage,
          totalPages = totalPages,
        )
      }

      if (pageRecords.isEmpty()) {
        item {
          RecordListMessage(
            title = "No cases found",
            message = collection.emptyMessage,
            modifier = Modifier.fillMaxWidth(),
          )
        }
      } else {
        items(items = pageRecords, key = WorkflowRecord::id) { record ->
          MeaslesCaseCard(record = record)
        }
      }
    }

    PaginationControls(
      currentPage = currentPage,
      totalPages = totalPages,
      onPreviousClick = {
        if (currentPage > 0) {
          currentPage--
        }
      },
      onNextClick = {
        if (currentPage < totalPages - 1) {
          currentPage++
        }
      },
    )
  }
}

@Composable
private fun ResultsSummary(
  totalResults: Int,
  currentPage: Int,
  totalPages: Int,
  modifier: Modifier = Modifier,
) {
  val firstResult =
    if (totalResults == 0) {
      0
    } else {
      currentPage * RECORDS_PER_PAGE + 1
    }

  val lastResult = minOf((currentPage + 1) * RECORDS_PER_PAGE, totalResults)

  Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
    Text(
      text = "Showing $firstResult–$lastResult of $totalResults results",
      style = MaterialTheme.typography.titleMedium,
      color = MaterialTheme.colorScheme.onSurface,
      fontWeight = FontWeight.SemiBold,
    )

    if (totalResults > 0) {
      Text(
        text = "Page ${currentPage + 1} of $totalPages",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}

@Composable
private fun PaginationControls(
  currentPage: Int,
  totalPages: Int,
  onPreviousClick: () -> Unit,
  onNextClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Row(
    modifier = modifier.fillMaxWidth().padding(top = 8.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    OutlinedButton(onClick = onPreviousClick, enabled = currentPage > 0) {
      Icon(imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = null)

      Spacer(modifier = Modifier.width(4.dp))

      Text("Previous")
    }

    Text(
      text = "${currentPage + 1} / $totalPages",
      style = MaterialTheme.typography.labelLarge,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    Button(onClick = onNextClick, enabled = currentPage < totalPages - 1) {
      Text("Next")

      Spacer(modifier = Modifier.width(4.dp))

      Icon(imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
    }
  }
}

@Composable
private fun CaseSearchField(
  query: String,
  onQueryChange: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  OutlinedTextField(
    value = query,
    onValueChange = onQueryChange,
    modifier = modifier.fillMaxWidth().heightIn(min = 64.dp),
    shape = RoundedCornerShape(24.dp),
    singleLine = true,
    placeholder = { Text(text = "Search for case", style = MaterialTheme.typography.titleMedium) },
    leadingIcon = {
      Icon(
        imageVector = Icons.Default.Search,
        contentDescription = null,
        modifier = Modifier.size(28.dp),
      )
    },
    trailingIcon = {
      if (query.isNotBlank()) {
        IconButton(onClick = { onQueryChange("") }) {
          Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear search")
        }
      }
    },
    colors =
      OutlinedTextFieldDefaults.colors(
        focusedContainerColor = MaterialTheme.colorScheme.surface,
        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
        focusedBorderColor = MaterialTheme.colorScheme.outline,
        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
      ),
  )
}

@Composable
private fun MeaslesCaseCard(record: WorkflowRecord, modifier: Modifier = Modifier) {
  val name = record.fieldValue("Name")?.takeIf(String::isNotBlank) ?: record.title

  val epidNumber = record.fieldValue("EPID No", "EPID No.", "EPID Number").orEmpty()

  val county = record.fieldValue("County").orEmpty()

  val subCounty = record.fieldValue("Sub County", "Sub-county", "Subcounty").orEmpty()

  val onsetDate = record.fieldValue("Onset of illness", "Onset Date").orEmpty()

  val labResult = record.fieldValue("Lab Results", "Lab Result").orEmpty()

  val finalClassification = record.fieldValue("Final Classification", "Classification").orEmpty()

  Card(
    modifier = modifier.fillMaxWidth(),
    shape = RoundedCornerShape(20.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = BorderStroke(width = 1.dp, color = MaterialTheme.colorScheme.outlineVariant),
    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      TwoColumnCaseRow(
        leftLabel = "Name",
        leftValue = name,
        rightLabel = "EPID No:",
        rightValue = epidNumber,
      )

      TwoColumnCaseRow(
        leftLabel = "County:",
        leftValue = county,
        rightLabel = "Sub County:",
        rightValue = subCounty,
      )

      TwoColumnCaseRow(
        leftLabel = "Onset of illness:",
        leftValue = onsetDate,
        rightLabel = "Lab Results:",
        rightValue = labResult,
        rightValueColor = labResultColor(labResult),
      )

      if (finalClassification.isNotBlank()) {
        CaseValue(
          label = "Final Classification:",
          value = finalClassification,
          valueColor = classificationColor(finalClassification),
        )
      }
    }
  }
}

@Composable
private fun TwoColumnCaseRow(
  leftLabel: String,
  leftValue: String,
  rightLabel: String,
  rightValue: String,
  modifier: Modifier = Modifier,
  leftValueColor: Color = MaterialTheme.colorScheme.onSurface,
  rightValueColor: Color = MaterialTheme.colorScheme.onSurface,
) {
  Row(
    modifier = modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(16.dp),
    verticalAlignment = Alignment.Top,
  ) {
    CaseValue(
      label = leftLabel,
      value = leftValue,
      valueColor = leftValueColor,
      modifier = Modifier.weight(1f),
    )

    CaseValue(
      label = rightLabel,
      value = rightValue,
      valueColor = rightValueColor,
      modifier = Modifier.weight(1f),
    )
  }
}

@Composable
private fun CaseValue(
  label: String,
  value: String,
  modifier: Modifier = Modifier,
  valueColor: Color = MaterialTheme.colorScheme.onSurface,
) {
  Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
    Text(
      text = label,
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    Text(
      text = value.ifBlank { "—" },
      style = MaterialTheme.typography.titleMedium,
      color = valueColor,
      fontWeight = FontWeight.Medium,
    )
  }
}

@Composable
private fun labResultColor(result: String): Color =
  when {
    result.contains("positive", ignoreCase = true) -> MaterialTheme.colorScheme.error

    result.contains("negative", ignoreCase = true) -> MaterialTheme.colorScheme.onSurface

    result.contains("pending", ignoreCase = true) -> MaterialTheme.colorScheme.onSurfaceVariant

    else -> MaterialTheme.colorScheme.onSurface
  }

@Composable
private fun classificationColor(classification: String): Color =
  when {
    classification.contains("discarded", ignoreCase = true) -> MaterialTheme.colorScheme.tertiary

    classification.contains("confirmed", ignoreCase = true) -> MaterialTheme.colorScheme.error

    classification.contains("pending", ignoreCase = true) ->
      MaterialTheme.colorScheme.onSurfaceVariant

    else -> MaterialTheme.colorScheme.onSurface
  }

private fun WorkflowRecord.fieldValue(vararg labels: String): String? =
  fields
    .firstOrNull { field ->
      labels.any { label ->
        field.label
          .trim()
          .removeSuffix(":")
          .equals(label.trim().removeSuffix(":"), ignoreCase = true)
      }
    }
    ?.value

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CaseFilterDialog(
  counties: List<String>,
  subCounties: List<String>,
  initialFilters: RecordFilters,
  onDismiss: () -> Unit,
  onApply: (RecordFilters) -> Unit,
  onClear: () -> Unit,
) {
  var selectedCounty by remember(initialFilters) { mutableStateOf(initialFilters.county) }

  var selectedSubCounty by remember(initialFilters) { mutableStateOf(initialFilters.subCounty) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Text(
        text = "Filter cases",
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
      )
    },
    text = {
      Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(18.dp),
      ) {
        FilterDropdown(
          label = "County",
          options = counties,
          selectedValue = selectedCounty,
          onValueSelected = {
            selectedCounty = it
            selectedSubCounty = null
          },
        )

        FilterDropdown(
          label = "Sub-county",
          options = subCounties,
          selectedValue = selectedSubCounty,
          enabled = selectedCounty != null,
          onValueSelected = { selectedSubCounty = it },
        )
      }
    },
    confirmButton = {
      TextButton(
        onClick = { onApply(RecordFilters(county = selectedCounty, subCounty = selectedSubCounty)) }
      ) {
        Text("Apply filters")
      }
    },
    dismissButton = {
      Row {
        TextButton(onClick = onClear) { Text("Clear") }

        TextButton(onClick = onDismiss) { Text("Cancel") }
      }
    },
  )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterDropdown(
  label: String,
  options: List<String>,
  selectedValue: String?,
  onValueSelected: (String?) -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
) {
  var expanded by remember { mutableStateOf(false) }

  ExposedDropdownMenuBox(
    expanded = expanded,
    onExpandedChange = {
      if (enabled) {
        expanded = !expanded
      }
    },
    modifier = modifier.fillMaxWidth(),
  ) {
    OutlinedTextField(
      value = selectedValue.orEmpty(),
      onValueChange = {},
      modifier = Modifier.fillMaxWidth().menuAnchor(),
      readOnly = true,
      enabled = enabled,
      label = { Text(label) },
      placeholder = { Text("All $label") },
      trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
      shape = RoundedCornerShape(16.dp),
    )

    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
      DropdownMenuItem(
        text = { Text("All") },
        onClick = {
          onValueSelected(null)
          expanded = false
        },
      )

      options.forEach { option ->
        DropdownMenuItem(
          text = { Text(option) },
          onClick = {
            onValueSelected(option)
            expanded = false
          },
        )
      }
    }
  }
}
