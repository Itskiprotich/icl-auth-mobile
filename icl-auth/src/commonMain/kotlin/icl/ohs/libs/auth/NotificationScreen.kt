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
package icl.ohs.libs.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlin.math.absoluteValue
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

private const val ALL_NOTIFICATION_FILTER = "__all__"

private enum class NotificationSortOption(val label: String) {
  Newest("Newest first"),
  Oldest("Oldest first"),
  TitleAscending("Title A-Z"),
  TitleDescending("Title Z-A"),
  DueSoonest("Due date soonest"),
  DueLatest("Due date latest"),
}

private data class NotificationCreatedMonthFilter(val key: String, val label: String)

private val NotificationFilterActionIcon: ImageVector by lazy {
  ImageVector.Builder(
      name = "NotificationFilterActionIcon",
      defaultWidth = 24.dp,
      defaultHeight = 24.dp,
      viewportWidth = 24f,
      viewportHeight = 24f,
    )
    .apply {
      path(fill = SolidColor(Color.Black), pathFillType = PathFillType.NonZero) {
        moveTo(4f, 7f)
        lineTo(20f, 7f)
        lineTo(20f, 9f)
        lineTo(4f, 9f)
        close()

        moveTo(7f, 11f)
        lineTo(17f, 11f)
        lineTo(17f, 13f)
        lineTo(7f, 13f)
        close()

        moveTo(10f, 15f)
        lineTo(14f, 15f)
        lineTo(14f, 17f)
        lineTo(10f, 17f)
        close()
      }
    }
    .build()
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun NotificationScreen(
  config: NotificationScreenConfig = NotificationScreenConfig(),
  modifier: Modifier = Modifier,
  onBackClick: () -> Unit = {},
  onLoadSuccess: (NotificationListSuccess) -> Unit = {},
  onLoadFailure: (NotificationListFailure) -> Unit = {},
) {
  val resolvedConfig = remember(config) { resolveNotificationConfig(config) }
  val httpClient =
    remember(resolvedConfig.requestTimeoutMillis) {
      buildLoginHttpClient(resolvedConfig.requestTimeoutMillis)
    }
  val notificationService = remember(httpClient) { NotificationService(httpClient) }

  DisposableEffect(httpClient) { onDispose { httpClient.close() } }

  var notifications by remember { mutableStateOf<List<AuthNotification>>(emptyList()) }
  var isLoading by rememberSaveable { mutableStateOf(true) }
  var errorMessage by rememberSaveable { mutableStateOf<String?>(null) }
  var searchQuery by rememberSaveable { mutableStateOf("") }
  var isSearchVisible by rememberSaveable { mutableStateOf(false) }
  var isFilterDialogVisible by rememberSaveable { mutableStateOf(false) }
  var isOverflowMenuExpanded by rememberSaveable { mutableStateOf(false) }
  var selectedStatusFilter by rememberSaveable { mutableStateOf(ALL_NOTIFICATION_FILTER) }
  var selectedTypeFilter by rememberSaveable { mutableStateOf(ALL_NOTIFICATION_FILTER) }
  var selectedCreatedMonthFilter by rememberSaveable { mutableStateOf(ALL_NOTIFICATION_FILTER) }
  var selectedSortOptionName by rememberSaveable {
    mutableStateOf(NotificationSortOption.Newest.name)
  }
  var expandedNotificationId by rememberSaveable { mutableStateOf<String?>(null) }
  var refreshToken by rememberSaveable { mutableStateOf(0) }

  LaunchedEffect(resolvedConfig.notificationsUrl, refreshToken) {
    isLoading = true
    errorMessage = null

    when (val result = notificationService.fetchNotifications(config = resolvedConfig)) {
      is NotificationListResult.Success -> {
        notifications = result.value.notifications
        expandedNotificationId =
          expandedNotificationId?.takeIf { id ->
            result.value.notifications.any { notification -> notification.stableKey() == id }
          }
        onLoadSuccess(result.value)
      }

      is NotificationListResult.Failure -> {
        errorMessage = result.value.message
        onLoadFailure(result.value)
      }
    }

    isLoading = false
  }

  val statusOptions = notifications.distinctNotificationValues(AuthNotification::status)
  val typeOptions = notifications.distinctNotificationValues(AuthNotification::type)
  val createdMonthOptions = notifications.distinctCreatedMonthFilters()
  val selectedSortOption =
    NotificationSortOption.entries.firstOrNull { it.name == selectedSortOptionName }
      ?: NotificationSortOption.Newest

  LaunchedEffect(createdMonthOptions, selectedCreatedMonthFilter) {
    if (
      selectedCreatedMonthFilter != ALL_NOTIFICATION_FILTER &&
        createdMonthOptions.none { option -> option.key == selectedCreatedMonthFilter }
    ) {
      selectedCreatedMonthFilter = ALL_NOTIFICATION_FILTER
    }
  }

  val visibleNotifications =
    notifications
      .asSequence()
      .filter { notification ->
        notification.matchesSearch(searchQuery) &&
          notification.matchesCreatedMonthFilter(selectedCreatedMonthFilter) &&
          notification.matchesFilter(selectedStatusFilter, AuthNotification::status) &&
          notification.matchesFilter(selectedTypeFilter, AuthNotification::type)
      }
      .sortedBy(selectedSortOption)
      .toList()

  Scaffold(
    modifier = modifier.fillMaxSize(),
    containerColor = MaterialTheme.colorScheme.surface,
    topBar = {
      Column(
        modifier =
          Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).statusBarsPadding()
      ) {
        TopAppBar(
          title = {
            if (isSearchVisible) {
              OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                placeholder = { Text(text = "Search notifications") },
                singleLine = true,
                shape = RoundedCornerShape(18.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                trailingIcon = {
                  if (searchQuery.isNotBlank()) {
                    IconButton(onClick = { searchQuery = "" }) {
                      Icon(imageVector = Icons.Filled.Close, contentDescription = "Clear search")
                    }
                  }
                },
                colors =
                  androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    disabledBorderColor = Color.Transparent,
                  ),
              )
            } else {
              Column {
                Text(text = config.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                  text = "${visibleNotifications.size} of ${notifications.size} notifications",
                  style = MaterialTheme.typography.labelMedium,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }
          },
          navigationIcon = {
            if (isSearchVisible) {
              IconButton(
                onClick = {
                  isSearchVisible = false
                  searchQuery = ""
                }
              ) {
                Icon(imageVector = Icons.Filled.Close, contentDescription = "Close search")
              }
            } else if (config.showBackButton) {
              IconButton(onClick = onBackClick) {
                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
              }
            }
          },
          actions = {
            if (!isSearchVisible) {
              IconButton(onClick = { isSearchVisible = true }) {
                Icon(imageVector = Icons.Filled.Search, contentDescription = "Search")
              }
              IconButton(onClick = { isFilterDialogVisible = true }) {
                Icon(imageVector = NotificationFilterActionIcon, contentDescription = "Filters")
              }
              Box {
                IconButton(onClick = { isOverflowMenuExpanded = true }) {
                  Icon(imageVector = Icons.Filled.MoreVert, contentDescription = "More options")
                }
                DropdownMenu(
                  expanded = isOverflowMenuExpanded,
                  onDismissRequest = { isOverflowMenuExpanded = false },
                ) {
                  DropdownMenuItem(
                    text = { Text(text = "Refresh") },
                    leadingIcon = {
                      Icon(imageVector = Icons.Filled.Refresh, contentDescription = null)
                    },
                    onClick = {
                      isOverflowMenuExpanded = false
                      refreshToken += 1
                    },
                  )
                }
              }
            }
          },
        )

        if (createdMonthOptions.isNotEmpty()) {
          NotificationCreatedMonthFilterRow(
            options = createdMonthOptions,
            selectedKey = selectedCreatedMonthFilter,
            onSelected = { selectedCreatedMonthFilter = it },
          )
        }
      }
    },
  ) { innerPadding ->
    Box(modifier = Modifier.fillMaxSize().padding(innerPadding).imePadding()) {
      if (errorMessage != null) {
        AuthMessageBanner(
          message = errorMessage.orEmpty(),
          type = AuthMessageBannerType.Error,
          onDismiss = { errorMessage = null },
          modifier =
            Modifier.align(Alignment.TopCenter).padding(start = 16.dp, top = 8.dp, end = 16.dp),
          durationMillis = 3_000,
        )
      }

      when {
        isLoading && notifications.isEmpty() -> {
          CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }

        visibleNotifications.isEmpty() -> {
          NotificationEmptyState(
            title =
              if (notifications.isEmpty()) {
                resolvedConfig.messages.emptyStateTitle
              } else {
                resolvedConfig.messages.noResultsTitle
              },
            message =
              if (notifications.isEmpty()) {
                resolvedConfig.messages.emptyStateMessage
              } else {
                resolvedConfig.messages.noResultsMessage
              },
            modifier = Modifier.align(Alignment.Center),
          )
        }

        else -> {
          LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            items(items = visibleNotifications, key = { it.stableKey() }) { notification ->
              NotificationCard(
                notification = notification,
                expanded = expandedNotificationId == notification.stableKey(),
                onClick = {
                  expandedNotificationId =
                    if (expandedNotificationId == notification.stableKey()) {
                      null
                    } else {
                      notification.stableKey()
                    }
                },
              )
            }
          }
        }
      }
    }
  }

  if (isFilterDialogVisible) {
    NotificationFilterDialog(
      selectedSortOption = selectedSortOption,
      selectedStatusFilter = selectedStatusFilter,
      statusOptions = statusOptions,
      selectedTypeFilter = selectedTypeFilter,
      typeOptions = typeOptions,
      onDismiss = { isFilterDialogVisible = false },
      onApply = { sortOption, statusFilter, typeFilter ->
        selectedSortOptionName = sortOption.name
        selectedStatusFilter = statusFilter
        selectedTypeFilter = typeFilter
        isFilterDialogVisible = false
      },
      onClear = {
        selectedSortOptionName = NotificationSortOption.Newest.name
        selectedStatusFilter = ALL_NOTIFICATION_FILTER
        selectedTypeFilter = ALL_NOTIFICATION_FILTER
        isFilterDialogVisible = false
      },
    )
  }
}

@Composable
private fun NotificationCreatedMonthFilterRow(
  options: List<NotificationCreatedMonthFilter>,
  selectedKey: String,
  onSelected: (String) -> Unit,
) {
  LazyRow(
    modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
    contentPadding = PaddingValues(horizontal = 16.dp),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    item(key = ALL_NOTIFICATION_FILTER) {
      FilterChip(
        selected = selectedKey == ALL_NOTIFICATION_FILTER,
        onClick = { onSelected(ALL_NOTIFICATION_FILTER) },
        label = { Text("All") },
      )
    }
    items(items = options, key = NotificationCreatedMonthFilter::key) { option ->
      FilterChip(
        selected = selectedKey == option.key,
        onClick = { onSelected(option.key) },
        label = { Text(option.label) },
      )
    }
  }
}

@Composable
private fun NotificationCard(
  notification: AuthNotification,
  expanded: Boolean,
  onClick: () -> Unit,
) {
  Surface(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(22.dp),
    tonalElevation = 2.dp,
    shadowElevation = 0.dp,
    color = MaterialTheme.colorScheme.surfaceContainerLow,
    onClick = onClick,
  ) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Box(
              modifier =
                Modifier.size(8.dp)
                  .clip(CircleShape)
                  .background(notification.statusIndicatorColor())
            )
            Text(
              text = notification.title.displayOrFallback("Untitled notification"),
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.SemiBold,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
          Text(
            text = notification.body.displayOrFallback("No additional details available."),
            style = MaterialTheme.typography.bodyMedium,
            maxLines = if (expanded) Int.MAX_VALUE else 2,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          Text(
            text = notification.createdAt.toRelativeTimestamp(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }

        Surface(
          modifier = Modifier.padding(start = 12.dp),
          shape = CircleShape,
          color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.9f),
          contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ) {
          Icon(
            imageVector =
              if (expanded) {
                Icons.Filled.KeyboardArrowDown
              } else {
                Icons.AutoMirrored.Filled.KeyboardArrowRight
              },
            contentDescription = if (expanded) "Collapse notification" else "Expand notification",
            modifier = Modifier.padding(4.dp),
          )
        }
      }

      if (expanded) {
        Spacer(modifier = Modifier.height(10.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
        Spacer(modifier = Modifier.height(10.dp))
        NotificationDetailBlock(notification = notification)
      }
    }
  }
}

@Composable
private fun NotificationDetailBlock(notification: AuthNotification) {
  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      NotificationLabelChip(
        label = notification.type.humanizeToken(fallback = "General"),
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
      )
      NotificationLabelChip(
        label = notification.status.humanizeToken(fallback = "Unknown"),
        containerColor = notification.statusContainerColor(),
        contentColor = notification.statusContentColor(),
      )
    }
    NotificationKeyValueRow(label = "Created", value = notification.createdAt.toReadableTimestamp())
    NotificationKeyValueRow(label = "Updated", value = notification.updatedAt.toReadableTimestamp())
    if (notification.dueDate.isMeaningfulValue()) {
      NotificationKeyValueRow(label = "Due", value = notification.dueDate.toReadableTimestamp())
    }
    NotificationKeyValueRow(
      label = "Investigation date",
      value = notification.investigationDate.toReadableTimestamp(),
    )
    NotificationKeyValueRow(
      label = "Submission email sent",
      value = notification.caseSubmissionEmailSentAt.toReadableTimestamp(),
    )
    NotificationKeyValueRow(
      label = "Failure reason",
      value = notification.failureReason.displayOrFallback("Not available"),
    )
  }
}

@Composable
private fun NotificationKeyValueRow(label: String, value: String) {
  Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
    Text(
      text = label,
      style = MaterialTheme.typography.labelMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Text(text = value, style = MaterialTheme.typography.bodyMedium)
  }
}

@Composable
private fun NotificationLabelChip(label: String, containerColor: Color, contentColor: Color) {
  Surface(shape = RoundedCornerShape(999.dp), color = containerColor, contentColor = contentColor) {
    Text(
      text = label,
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
      style = MaterialTheme.typography.labelMedium,
      fontWeight = FontWeight.Medium,
    )
  }
}

@Composable
private fun NotificationFilterDialog(
  selectedSortOption: NotificationSortOption,
  selectedStatusFilter: String,
  statusOptions: List<String>,
  selectedTypeFilter: String,
  typeOptions: List<String>,
  onDismiss: () -> Unit,
  onApply: (NotificationSortOption, String, String) -> Unit,
  onClear: () -> Unit,
) {
  var draftSortOption by remember(selectedSortOption) { mutableStateOf(selectedSortOption) }
  var draftStatusFilter by remember(selectedStatusFilter) { mutableStateOf(selectedStatusFilter) }
  var draftTypeFilter by remember(selectedTypeFilter) { mutableStateOf(selectedTypeFilter) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(text = "Sort and filter notifications") },
    text = {
      Column(
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(18.dp),
      ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Text(text = "Sort by", style = MaterialTheme.typography.labelLarge)
          NotificationSortOption.entries.forEach { option ->
            Row(
              modifier =
                Modifier.fillMaxWidth()
                  .clip(RoundedCornerShape(16.dp))
                  .background(
                    if (draftSortOption == option) {
                      MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                    } else {
                      Color.Transparent
                    }
                  )
                  .padding(horizontal = 4.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              RadioButton(
                selected = draftSortOption == option,
                onClick = { draftSortOption = option },
              )
              Text(
                text = option.label,
                modifier = Modifier.padding(start = 4.dp),
                style = MaterialTheme.typography.bodyMedium,
              )
            }
          }
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Text(text = "Status", style = MaterialTheme.typography.labelLarge)
          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            FilterChip(
              selected = draftStatusFilter == ALL_NOTIFICATION_FILTER,
              onClick = { draftStatusFilter = ALL_NOTIFICATION_FILTER },
              label = { Text("All statuses") },
            )
            statusOptions.forEach { status ->
              FilterChip(
                selected = draftStatusFilter == status,
                onClick = { draftStatusFilter = status },
                label = { Text(status.humanizeToken(fallback = status)) },
              )
            }
          }
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Text(text = "Type", style = MaterialTheme.typography.labelLarge)
          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            FilterChip(
              selected = draftTypeFilter == ALL_NOTIFICATION_FILTER,
              onClick = { draftTypeFilter = ALL_NOTIFICATION_FILTER },
              label = { Text("All types") },
            )
            typeOptions.forEach { type ->
              FilterChip(
                selected = draftTypeFilter == type,
                onClick = { draftTypeFilter = type },
                label = { Text(type.humanizeToken(fallback = type)) },
              )
            }
          }
        }
      }
    },
    confirmButton = {
      TextButton(onClick = { onApply(draftSortOption, draftStatusFilter, draftTypeFilter) }) {
        Text(text = "Apply")
      }
    },
    dismissButton = {
      Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        TextButton(onClick = onClear) { Text(text = "Clear") }
        TextButton(onClick = onDismiss) { Text(text = "Cancel") }
      }
    },
  )
}

@Composable
private fun NotificationEmptyState(title: String, message: String, modifier: Modifier = Modifier) {
  Column(
    modifier = modifier.widthIn(max = 340.dp).padding(horizontal = 24.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    Text(
      text = title,
      style = MaterialTheme.typography.headlineSmall,
      fontWeight = FontWeight.SemiBold,
    )
    Text(
      text = message,
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
  }
}

private fun List<AuthNotification>.distinctNotificationValues(
  selector: (AuthNotification) -> String?
): List<String> =
  mapNotNull(selector).map(String::trim).filter(String::isNotBlank).distinct().sortedBy {
    it.lowercase()
  }

private fun AuthNotification.matchesSearch(query: String): Boolean {
  val normalizedQuery = query.trim().lowercase()
  if (normalizedQuery.isBlank()) {
    return true
  }

  return listOf(title, body, type, status, failureReason).any { value ->
    value?.lowercase()?.contains(normalizedQuery) == true
  }
}

private fun AuthNotification.stableKey(): String =
  listOf(id, createdAt, title, encounterId, practitionerId)
    .mapNotNull { it?.trim()?.takeIf(String::isNotBlank) }
    .joinToString(separator = "|")
    .ifBlank { hashCode().toString() }

private fun AuthNotification.matchesFilter(
  selectedValue: String,
  selector: (AuthNotification) -> String?,
): Boolean {
  if (selectedValue == ALL_NOTIFICATION_FILTER) {
    return true
  }

  return selector(this)?.trim()?.equals(selectedValue, ignoreCase = true) == true
}

private fun AuthNotification.matchesCreatedMonthFilter(selectedMonthKey: String): Boolean {
  if (selectedMonthKey == ALL_NOTIFICATION_FILTER) {
    return true
  }

  return createdAt.toCreatedMonthFilter()?.key == selectedMonthKey
}

private fun Sequence<AuthNotification>.sortedBy(
  option: NotificationSortOption
): Sequence<AuthNotification> =
  when (option) {
    NotificationSortOption.Newest ->
      sortedWith(
        compareByDescending<AuthNotification> { it.createdAt.normalizedTimestampForDescending() }
          .thenBy { it.title.normalizedText() }
      )

    NotificationSortOption.Oldest ->
      sortedWith(
        compareBy<AuthNotification> { it.createdAt.normalizedTimestampForAscending() }
          .thenBy { it.title.normalizedText() }
      )

    NotificationSortOption.TitleAscending ->
      sortedWith(
        compareBy<AuthNotification> { it.title.normalizedText() }
          .thenByDescending { it.createdAt.normalizedTimestampForDescending() }
      )

    NotificationSortOption.TitleDescending ->
      sortedWith(
        compareByDescending<AuthNotification> { it.title.normalizedText() }
          .thenByDescending { it.createdAt.normalizedTimestampForDescending() }
      )

    NotificationSortOption.DueSoonest ->
      sortedWith(
        compareBy<AuthNotification> { it.dueDate.normalizedTimestampForAscending() }
          .thenByDescending { it.createdAt.normalizedTimestampForDescending() }
      )

    NotificationSortOption.DueLatest ->
      sortedWith(
        compareByDescending<AuthNotification> { it.dueDate.normalizedTimestampForDescending() }
          .thenByDescending { it.createdAt.normalizedTimestampForDescending() }
      )
  }

private fun String?.normalizedTimestampForAscending(): String = trimToMeaningfulValue() ?: "\uFFFF"

private fun String?.normalizedTimestampForDescending(): String = trimToMeaningfulValue().orEmpty()

private fun String?.normalizedText(): String = trimToMeaningfulValue()?.lowercase().orEmpty()

private fun String?.trimToMeaningfulValue(): String? = this?.trim()?.takeIf(String::isNotBlank)

private fun String?.displayOrFallback(fallback: String): String =
  trimToMeaningfulValue() ?: fallback

private fun String?.isMeaningfulValue(): Boolean = trimToMeaningfulValue() != null

private fun String?.humanizeToken(fallback: String): String {
  val value = trimToMeaningfulValue() ?: return fallback
  return value.split('_', '-', ' ').filter(String::isNotBlank).joinToString(" ") { token ->
    token.lowercase().replaceFirstChar { char ->
      if (char.isLowerCase()) char.titlecase() else char.toString()
    }
  }
}

private fun String?.toReadableTimestamp(): String {
  val value = trimToMeaningfulValue() ?: return "Not available"
  val localDateTime =
    runCatching { Instant.parse(value).toLocalDateTime(TimeZone.currentSystemDefault()) }
      .getOrNull() ?: return value.replace('T', ' ').removeSuffix("Z")

  val month = localDateTime.month.toShortMonthName()
  val minute = localDateTime.minute.toString().padStart(2, '0')
  val hour = localDateTime.hour.toString().padStart(2, '0')
  return "${localDateTime.day} $month ${localDateTime.year}, $hour:$minute"
}

private fun String?.toCreatedMonthFilter(): NotificationCreatedMonthFilter? {
  val value = trimToMeaningfulValue() ?: return null
  val localDateTime =
    runCatching { Instant.parse(value).toLocalDateTime(TimeZone.currentSystemDefault()) }
      .getOrNull() ?: return null
  val monthNumber = (localDateTime.month.ordinal + 1).toString().padStart(2, '0')
  return NotificationCreatedMonthFilter(
    key = "${localDateTime.year}-$monthNumber",
    label = "${localDateTime.month.toLongMonthName()} ${localDateTime.year}",
  )
}

private fun String?.toRelativeTimestamp(now: Instant = Clock.System.now()): String {
  val value = trimToMeaningfulValue() ?: return "Not available"
  val instant = runCatching { Instant.parse(value) }.getOrNull() ?: return toReadableTimestamp()
  val seconds = (now - instant).inWholeSeconds
  val isPast = seconds >= 0
  val absoluteSeconds = seconds.absoluteValue

  return when {
    absoluteSeconds < 45 -> if (isPast) "now" else "in a few seconds"
    absoluteSeconds < 90 -> relativeTimeLabel(1, "minute", isPast)
    absoluteSeconds < 45 * 60 -> relativeTimeLabel(absoluteSeconds / 60, "minute", isPast)
    absoluteSeconds < 90 * 60 -> relativeTimeLabel(1, "hour", isPast)
    absoluteSeconds < 22 * 60 * 60 -> relativeTimeLabel(absoluteSeconds / (60 * 60), "hour", isPast)
    absoluteSeconds < 36 * 60 * 60 -> if (isPast) "yesterday" else "tomorrow"
    absoluteSeconds < 7 * 24 * 60 * 60 ->
      relativeTimeLabel(absoluteSeconds / (24 * 60 * 60), "day", isPast)
    else -> toReadableTimestamp()
  }
}

private fun relativeTimeLabel(value: Long, unit: String, isPast: Boolean): String {
  val amount = if (value <= 1L) "1 $unit" else "$value ${unit}s"
  return if (isPast) "$amount ago" else "in $amount"
}

private fun List<AuthNotification>.distinctCreatedMonthFilters():
  List<NotificationCreatedMonthFilter> =
  mapNotNull { notification -> notification.createdAt.toCreatedMonthFilter() }
    .distinctBy(NotificationCreatedMonthFilter::key)
    .sortedByDescending(NotificationCreatedMonthFilter::key)

private fun kotlinx.datetime.Month.toShortMonthName(): String =
  when (this) {
    kotlinx.datetime.Month.JANUARY -> "Jan"
    kotlinx.datetime.Month.FEBRUARY -> "Feb"
    kotlinx.datetime.Month.MARCH -> "Mar"
    kotlinx.datetime.Month.APRIL -> "Apr"
    kotlinx.datetime.Month.MAY -> "May"
    kotlinx.datetime.Month.JUNE -> "Jun"
    kotlinx.datetime.Month.JULY -> "Jul"
    kotlinx.datetime.Month.AUGUST -> "Aug"
    kotlinx.datetime.Month.SEPTEMBER -> "Sep"
    kotlinx.datetime.Month.OCTOBER -> "Oct"
    kotlinx.datetime.Month.NOVEMBER -> "Nov"
    kotlinx.datetime.Month.DECEMBER -> "Dec"
  }

private fun kotlinx.datetime.Month.toLongMonthName(): String =
  when (this) {
    kotlinx.datetime.Month.JANUARY -> "January"
    kotlinx.datetime.Month.FEBRUARY -> "February"
    kotlinx.datetime.Month.MARCH -> "March"
    kotlinx.datetime.Month.APRIL -> "April"
    kotlinx.datetime.Month.MAY -> "May"
    kotlinx.datetime.Month.JUNE -> "June"
    kotlinx.datetime.Month.JULY -> "July"
    kotlinx.datetime.Month.AUGUST -> "August"
    kotlinx.datetime.Month.SEPTEMBER -> "September"
    kotlinx.datetime.Month.OCTOBER -> "October"
    kotlinx.datetime.Month.NOVEMBER -> "November"
    kotlinx.datetime.Month.DECEMBER -> "December"
  }

@Composable
private fun AuthNotification.statusIndicatorColor(): Color =
  when {
    status.containsToken("fail") -> MaterialTheme.colorScheme.error
    status.containsToken("success") || status.containsToken("sent") ->
      MaterialTheme.colorScheme.tertiary
    else -> MaterialTheme.colorScheme.primary
  }

@Composable
private fun AuthNotification.statusContainerColor(): Color =
  when {
    status.containsToken("fail") -> MaterialTheme.colorScheme.errorContainer
    status.containsToken("success") || status.containsToken("sent") ->
      MaterialTheme.colorScheme.tertiaryContainer
    else -> MaterialTheme.colorScheme.secondaryContainer
  }

@Composable
private fun AuthNotification.statusContentColor(): Color =
  when {
    status.containsToken("fail") -> MaterialTheme.colorScheme.onErrorContainer
    status.containsToken("success") || status.containsToken("sent") ->
      MaterialTheme.colorScheme.onTertiaryContainer
    else -> MaterialTheme.colorScheme.onSecondaryContainer
  }

private fun String?.containsToken(token: String): Boolean =
  trimToMeaningfulValue()?.contains(token, ignoreCase = true) == true
