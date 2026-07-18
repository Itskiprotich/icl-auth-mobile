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
import androidx.compose.foundation.clickable
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
import dev.ohs.player.reference.app.feature.workflow.models.RecordFilterSelection
import dev.ohs.player.reference.app.feature.workflow.models.RecordFilters

private const val RECORDS_PER_PAGE = 50

@Composable
fun WorkflowRecordListScreen(
  title: String,
  subtitle: String,
  resource: String,
  onBack: () -> Unit,
  onRecordClick: (WorkflowRecord) -> Unit = {},
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
            RecordListScreenState.Ready(
              collection =
                loadWorkflowRecordCollection(
                  resource = resource,
                  title = title,
                  subtitle = subtitle,
                )
            )
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
          CircularProgressIndicator(
            strokeWidth = 4.dp,
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.42f),
          )
        }

        is RecordListScreenState.Error -> {
          Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            RecordListMessage(title = "Case list unavailable", message = state.message)
          }
        }

        is RecordListScreenState.Ready -> {
          WorkflowRecordListContent(
            collection = state.collection,
            filters = filters,
            onRecordClick = onRecordClick,
          )

          if (showFilters) {
            val availableFilters = state.collection.records.availableFilterDescriptors()

            CaseFilterDialog(
              records = state.collection.records,
              availableFilters = availableFilters,
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

  data class Ready(val collection: WorkflowRecordCollection) : RecordListScreenState

  data class Error(val message: String) : RecordListScreenState
}

@Composable
private fun WorkflowRecordListContent(
  collection: WorkflowRecordCollection,
  filters: RecordFilters,
  onRecordClick: (WorkflowRecord) -> Unit,
) {
  var query by rememberSaveable(collection.id) { mutableStateOf("") }

  var currentPage by
    rememberSaveable(
      collection.id,
      query,
      filters.primary?.label,
      filters.primary?.value,
      filters.secondary?.label,
      filters.secondary?.value,
    ) {
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

        matchesQuery &&
          record.matchesFilter(filters.primary) &&
          record.matchesFilter(filters.secondary)
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
          WorkflowCaseCard(record = record, onClick = { onRecordClick(record) })
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
private fun WorkflowCaseCard(
  record: WorkflowRecord,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val layout = record.cardLayout()
  val isClickable = !record.references?.questionnaireResponseId.isNullOrBlank()

  Card(
    modifier = modifier.fillMaxWidth().clickable(enabled = isClickable, onClick = onClick),
    shape = RoundedCornerShape(20.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = BorderStroke(width = 1.dp, color = MaterialTheme.colorScheme.outlineVariant),
    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        CaseValue(
          label = layout.titleLabel,
          value = layout.titleValue,
          modifier = Modifier.weight(1f),
        )

        layout.badge?.let { badge ->
          Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(4.dp),
          ) {
            Text(
              text = badge.label,
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
              text = badge.value.ifBlank { "—" },
              style = MaterialTheme.typography.titleMedium,
              color = caseFieldColor(badge.label, badge.value),
              fontWeight = FontWeight.Medium,
            )
          }
        }

        if (isClickable) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = "Open case summary",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }

      layout.rows.forEach { (leftField, rightField) ->
        if (rightField == null) {
          CaseValue(
            label = leftField.label,
            value = leftField.value,
            valueColor = caseFieldColor(leftField.label, leftField.value),
          )
        } else {
          TwoColumnCaseRow(
            leftLabel = leftField.label,
            leftValue = leftField.value,
            rightLabel = rightField.label,
            rightValue = rightField.value,
            leftValueColor = caseFieldColor(leftField.label, leftField.value),
            rightValueColor = caseFieldColor(rightField.label, rightField.value),
          )
        }
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
      text = value,
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

@Composable
private fun caseFieldColor(label: String, value: String): Color =
  when {
    label.matchesAnyLabel("Lab Results", "Lab Result") -> labResultColor(value)
    label.matchesAnyLabel("Final Classification", "Classification") -> classificationColor(value)
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
  records: List<WorkflowRecord>,
  availableFilters: List<WorkflowRecordFilterDescriptor>,
  initialFilters: RecordFilters,
  onDismiss: () -> Unit,
  onApply: (RecordFilters) -> Unit,
  onClear: () -> Unit,
) {
  val primaryFilter = availableFilters.getOrNull(0)
  val secondaryFilter = availableFilters.getOrNull(1)
  var selectedPrimary by
    remember(initialFilters, primaryFilter) {
      mutableStateOf(initialFilters.primary?.takeIf { it.label == primaryFilter?.label }?.value)
    }
  var selectedSecondary by
    remember(initialFilters, secondaryFilter) {
      mutableStateOf(initialFilters.secondary?.takeIf { it.label == secondaryFilter?.label }?.value)
    }

  val secondaryOptions =
    remember(records, primaryFilter, secondaryFilter, selectedPrimary) {
      if (secondaryFilter == null) {
        emptyList()
      } else {
        val matchingRecords =
          if (primaryFilter != null && !selectedPrimary.isNullOrBlank()) {
            records.filter { record ->
              record.fieldValue(primaryFilter.label).equals(selectedPrimary, ignoreCase = true)
            }
          } else {
            records
          }
        matchingRecords.optionsForFilter(secondaryFilter.label)
      }
    }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Text(
        text = "Filter records",
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
      )
    },
    text = {
      if (availableFilters.isEmpty()) {
        Text(
          text = "No workflow-specific filters are available for this record list yet.",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      } else {
        Column(
          modifier = Modifier.fillMaxWidth(),
          verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
          primaryFilter?.let { descriptor ->
            FilterDropdown(
              label = descriptor.label,
              options = descriptor.options,
              selectedValue = selectedPrimary,
              onValueSelected = {
                selectedPrimary = it
                selectedSecondary = null
              },
            )
          }

          secondaryFilter?.let { descriptor ->
            FilterDropdown(
              label = descriptor.label,
              options = secondaryOptions,
              selectedValue = selectedSecondary,
              enabled = primaryFilter == null || selectedPrimary != null,
              onValueSelected = { selectedSecondary = it },
            )
          }
        }
      }
    },
    confirmButton = {
      TextButton(
        onClick = {
          onApply(
            RecordFilters(
              primary =
                primaryFilter?.let { descriptor ->
                  selectedPrimary?.let { RecordFilterSelection(descriptor.label, it) }
                },
              secondary =
                secondaryFilter?.let { descriptor ->
                  selectedSecondary?.let { RecordFilterSelection(descriptor.label, it) }
                },
            )
          )
        }
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

private data class WorkflowRecordFilterDescriptor(val label: String, val options: List<String>)

private data class WorkflowCaseCardField(val label: String, val value: String)

private data class WorkflowCaseCardFieldSpec(val label: String, val aliases: List<String>)

private data class WorkflowCaseCardLayout(
  val titleLabel: String,
  val titleValue: String,
  val badge: WorkflowCaseCardField?,
  val rows: List<Pair<WorkflowCaseCardField, WorkflowCaseCardField?>>,
)

private fun List<WorkflowRecord>.availableFilterDescriptors():
  List<WorkflowRecordFilterDescriptor> {
  val valuesByLabel = linkedMapOf<String, MutableSet<String>>()
  val displayLabels = linkedMapOf<String, String>()

  forEach { record ->
    record.fields.forEach { field ->
      val label = field.label.removeSuffix(":").trim()
      val value = field.value.trim()
      if (label.isBlank() || value.isBlank()) {
        return@forEach
      }
      val key = label.normalizeCaseKey()
      if (key !in displayLabels) {
        displayLabels[key] = label
      }
      valuesByLabel.getOrPut(key) { linkedSetOf() }.add(value)
    }
  }

  return valuesByLabel
    .mapNotNull { (key, values) ->
      if (values.size < 2) {
        null
      } else {
        WorkflowRecordFilterDescriptor(
          label = displayLabels.getValue(key),
          options = values.sorted(),
        )
      }
    }
    .sortedWith(
      compareBy<WorkflowRecordFilterDescriptor>(
        { descriptor ->
          FILTER_LABEL_PRIORITY.indexOf(descriptor.label.normalizeCaseKey()).let { index ->
            if (index >= 0) index else Int.MAX_VALUE
          }
        },
        { descriptor -> descriptor.label },
      )
    )
    .take(2)
}

private fun List<WorkflowRecord>.optionsForFilter(label: String): List<String> =
  mapNotNull { it.fieldValue(label) }.filter(String::isNotBlank).distinct().sorted()

private fun WorkflowRecord.matchesFilter(filter: RecordFilterSelection?): Boolean =
  filter == null || fieldValue(filter.label).equals(filter.value, ignoreCase = true)

private fun WorkflowRecord.cardLayout(): WorkflowCaseCardLayout {
  val titleValue =
    fieldValue("Name", "Patient Name", "Case Name", "Client Name")
      ?.takeIf(String::isNotBlank)
      ?.takeUnless { candidate ->
        candidate.equals("Submitted Case", ignoreCase = true) ||
          candidate.endsWith(" Case", ignoreCase = true)
      }
      .orEmpty()
  val detailFields =
    CASE_LIST_FIELD_SPECS.map { spec ->
      WorkflowCaseCardField(
        label = spec.label,
        value = fieldValue(*spec.aliases.toTypedArray()).orEmpty(),
      )
    }

  return WorkflowCaseCardLayout(
    titleLabel = "Patient Name",
    titleValue = titleValue,
    badge = null,
    rows = detailFields.chunked(2).map { row -> row.first() to row.getOrNull(1) },
  )
}

private fun String.matchesAnyLabel(vararg labels: String): Boolean =
  labels.any { label ->
    trim().removeSuffix(":").equals(label.trim().removeSuffix(":"), ignoreCase = true)
  }

private fun String.normalizeCaseKey(): String = trim().lowercase()

private val FILTER_LABEL_PRIORITY =
  listOf(
    "county",
    "sub county",
    "sub-county",
    "subcounty",
    "status",
    "outcome",
    "lab results",
    "final classification",
    "reporting facility",
  )

private val CARD_FIELD_PRIORITY =
  listOf(
    listOf(
      "EPID No",
      "EPID No.",
      "EPID Number",
      "Case ID",
      "Case Number",
      "Identifier",
      "Record ID",
    ),
    listOf("County", "District"),
    listOf("Sub County", "Sub-county", "Subcounty"),
    listOf("Onset of illness", "Onset Date", "Date of onset of illness"),
    listOf("Lab Results", "Lab Result"),
    listOf("Final Classification", "Classification"),
    listOf("Outcome"),
    listOf("Reporting Facility", "Health Facility", "Facility"),
    listOf("Status"),
  )

private val CASE_LIST_FIELD_SPECS =
  listOf(
    WorkflowCaseCardFieldSpec(
      label = "EPID No",
      aliases =
        listOf(
          "EPID No",
          "EPID No.",
          "EPID Number",
          "Case ID",
          "Case Number",
          "Identifier",
          "Record ID",
        ),
    ),
    WorkflowCaseCardFieldSpec(label = "County", aliases = listOf("County", "District")),
    WorkflowCaseCardFieldSpec(
      label = "Sub County",
      aliases = listOf("Sub County", "Sub-county", "Subcounty"),
    ),
    WorkflowCaseCardFieldSpec(
      label = "Onset of illness",
      aliases = listOf("Onset of illness", "Onset Date", "Date of onset of illness"),
    ),
    WorkflowCaseCardFieldSpec(label = "Lab Results", aliases = listOf("Lab Results", "Lab Result")),
    WorkflowCaseCardFieldSpec(
      label = "Final Classification",
      aliases = listOf("Final Classification", "Classification"),
    ),
  )

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
