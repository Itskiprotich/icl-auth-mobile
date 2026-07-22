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
package icl.ohs.libs.auth.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import icl.ohs.libs.auth.models.AuthNotification
import icl.ohs.libs.auth.models.NotificationListFailure
import icl.ohs.libs.auth.models.NotificationListResult
import icl.ohs.libs.auth.models.NotificationListSuccess
import icl.ohs.libs.auth.models.NotificationScreenConfig
import icl.ohs.libs.auth.network.NotificationService
import icl.ohs.libs.auth.network.buildLoginHttpClient
import icl.ohs.libs.auth.network.resolveNotificationConfig
import kotlin.math.absoluteValue
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

internal const val ALL_NOTIFICATION_FILTER = "__all__"

enum class NotificationSortOption(val label: String) {
  Newest("Newest first"),
  Oldest("Oldest first"),
  TitleAscending("Title A-Z"),
  TitleDescending("Title Z-A"),
  DueSoonest("Due date soonest"),
  DueLatest("Due date latest"),
}

data class NotificationCreatedMonthFilter(val key: String, val label: String)

data class NotificationUiState(
  val notifications: List<AuthNotification> = emptyList(),
  val isLoading: Boolean = true,
  val errorMessage: String? = null,
  val searchQuery: String = "",
  val isSearchVisible: Boolean = false,
  val selectedStatusFilter: String = ALL_NOTIFICATION_FILTER,
  val selectedTypeFilter: String = ALL_NOTIFICATION_FILTER,
  val selectedCreatedMonthFilter: String = ALL_NOTIFICATION_FILTER,
  val selectedSortOption: NotificationSortOption = NotificationSortOption.Newest,
  val expandedNotificationId: String? = null,
  val visibleNotifications: List<AuthNotification> = emptyList(),
  val statusOptions: List<String> = emptyList(),
  val typeOptions: List<String> = emptyList(),
  val createdMonthOptions: List<NotificationCreatedMonthFilter> = emptyList(),
)

sealed class NotificationEvent {
  data class LoadSuccess(val result: NotificationListSuccess) : NotificationEvent()

  data class LoadFailure(val result: NotificationListFailure) : NotificationEvent()
}

class NotificationViewModel(private val config: NotificationScreenConfig) : ViewModel() {
  private val resolvedConfig = resolveNotificationConfig(config)
  private val httpClient = buildLoginHttpClient(resolvedConfig.requestTimeoutMillis)
  private val notificationService = NotificationService(httpClient)

  private val _uiState = MutableStateFlow(NotificationUiState())
  val uiState: StateFlow<NotificationUiState> = _uiState.asStateFlow()

  private val _events = Channel<NotificationEvent>(Channel.BUFFERED)
  val events: Flow<NotificationEvent> = _events.receiveAsFlow()

  init {
    loadNotifications()
  }

  fun refresh() {
    loadNotifications()
  }

  private fun loadNotifications() {
    viewModelScope.launch {
      _uiState.update { it.copy(isLoading = true, errorMessage = null) }
      when (val result = notificationService.fetchNotifications(config = resolvedConfig)) {
        is NotificationListResult.Success -> {
          val notifications = result.value.notifications
          _uiState.update { current ->
            current
              .copy(
                notifications = notifications,
                expandedNotificationId =
                  current.expandedNotificationId?.takeIf { id ->
                    notifications.any { it.stableKey() == id }
                  },
              )
              .withComputedState()
          }
          _events.send(NotificationEvent.LoadSuccess(result.value))
        }

        is NotificationListResult.Failure -> {
          _uiState.update { it.copy(errorMessage = result.value.message) }
          _events.send(NotificationEvent.LoadFailure(result.value))
        }
      }
      _uiState.update { it.copy(isLoading = false) }
    }
  }

  fun onSearchQueryChange(query: String) {
    _uiState.update { it.copy(searchQuery = query).withComputedState() }
  }

  fun onSearchVisibilityChange(visible: Boolean) {
    _uiState.update { state ->
      if (visible) state.copy(isSearchVisible = true)
      else state.copy(isSearchVisible = false, searchQuery = "").withComputedState()
    }
  }

  fun onStatusFilterChange(filter: String) {
    _uiState.update { it.copy(selectedStatusFilter = filter).withComputedState() }
  }

  fun onTypeFilterChange(filter: String) {
    _uiState.update { it.copy(selectedTypeFilter = filter).withComputedState() }
  }

  fun onCreatedMonthFilterChange(filter: String) {
    _uiState.update { it.copy(selectedCreatedMonthFilter = filter).withComputedState() }
  }

  fun onSortOptionChange(option: NotificationSortOption) {
    _uiState.update { it.copy(selectedSortOption = option).withComputedState() }
  }

  fun onNotificationExpand(id: String?) {
    _uiState.update { it.copy(expandedNotificationId = id) }
  }

  fun applyFilters(sortOption: NotificationSortOption, statusFilter: String, typeFilter: String) {
    _uiState.update {
      it
        .copy(
          selectedSortOption = sortOption,
          selectedStatusFilter = statusFilter,
          selectedTypeFilter = typeFilter,
        )
        .withComputedState()
    }
  }

  fun clearFilters() {
    _uiState.update {
      it
        .copy(
          selectedSortOption = NotificationSortOption.Newest,
          selectedStatusFilter = ALL_NOTIFICATION_FILTER,
          selectedTypeFilter = ALL_NOTIFICATION_FILTER,
        )
        .withComputedState()
    }
  }

  fun onErrorDismiss() {
    _uiState.update { it.copy(errorMessage = null) }
  }

  override fun onCleared() {
    super.onCleared()
    httpClient.close()
  }

  private fun NotificationUiState.withComputedState(): NotificationUiState {
    val statusOptions = notifications.distinctNotificationValues { it.status }
    val typeOptions = notifications.distinctNotificationValues { it.type }
    val createdMonthOptions = notifications.distinctCreatedMonthFilters()

    val validCreatedMonthFilter =
      if (
        selectedCreatedMonthFilter != ALL_NOTIFICATION_FILTER &&
          createdMonthOptions.none { it.key == selectedCreatedMonthFilter }
      ) {
        ALL_NOTIFICATION_FILTER
      } else {
        selectedCreatedMonthFilter
      }

    val visibleNotifications =
      notifications
        .asSequence()
        .filter { notification ->
          notification.matchesSearch(searchQuery) &&
            notification.matchesCreatedMonthFilter(validCreatedMonthFilter) &&
            notification.matchesFilter(selectedStatusFilter) { it.status } &&
            notification.matchesFilter(selectedTypeFilter) { it.type }
        }
        .sortedBy(selectedSortOption)
        .toList()

    return copy(
      selectedCreatedMonthFilter = validCreatedMonthFilter,
      visibleNotifications = visibleNotifications,
      statusOptions = statusOptions,
      typeOptions = typeOptions,
      createdMonthOptions = createdMonthOptions,
    )
  }
}

// --- Notification helper extensions ---

internal fun AuthNotification.stableKey(): String =
  listOf(id, createdAt, title, encounterId, practitionerId)
    .mapNotNull { it?.trim()?.takeIf(String::isNotBlank) }
    .joinToString(separator = "|")
    .ifBlank { hashCode().toString() }

private fun List<AuthNotification>.distinctNotificationValues(
  selector: (AuthNotification) -> String?
): List<String> =
  mapNotNull(selector).map(String::trim).filter(String::isNotBlank).distinct().sortedBy {
    it.lowercase()
  }

private fun List<AuthNotification>.distinctCreatedMonthFilters():
  List<NotificationCreatedMonthFilter> =
  mapNotNull { it.createdAt.toCreatedMonthFilter() }
    .distinctBy(NotificationCreatedMonthFilter::key)
    .sortedByDescending(NotificationCreatedMonthFilter::key)

private fun AuthNotification.matchesSearch(query: String): Boolean {
  val normalized = query.trim().lowercase()
  if (normalized.isBlank()) return true
  return listOf(title, body, type, status, failureReason).any { value ->
    value?.lowercase()?.contains(normalized) == true
  }
}

private fun AuthNotification.matchesFilter(
  selectedValue: String,
  selector: (AuthNotification) -> String?,
): Boolean {
  if (selectedValue == ALL_NOTIFICATION_FILTER) return true
  return selector(this)?.trim()?.equals(selectedValue, ignoreCase = true) == true
}

private fun AuthNotification.matchesCreatedMonthFilter(selectedMonthKey: String): Boolean {
  if (selectedMonthKey == ALL_NOTIFICATION_FILTER) return true
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

internal fun String?.trimToMeaningfulValue(): String? = this?.trim()?.takeIf(String::isNotBlank)

internal fun String?.displayOrFallback(fallback: String): String =
  trimToMeaningfulValue() ?: fallback

internal fun String?.isMeaningfulValue(): Boolean = trimToMeaningfulValue() != null

internal fun String?.humanizeToken(fallback: String): String {
  val value = trimToMeaningfulValue() ?: return fallback
  return value.split('_', '-', ' ').filter(String::isNotBlank).joinToString(" ") { token ->
    token.lowercase().replaceFirstChar { char ->
      if (char.isLowerCase()) char.titlecase() else char.toString()
    }
  }
}

internal fun String?.toReadableTimestamp(): String {
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

internal fun String?.toRelativeTimestamp(now: Instant = Clock.System.now()): String {
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

private fun Month.toShortMonthName(): String =
  when (this) {
    Month.JANUARY -> "Jan"
    Month.FEBRUARY -> "Feb"
    Month.MARCH -> "Mar"
    Month.APRIL -> "Apr"
    Month.MAY -> "May"
    Month.JUNE -> "Jun"
    Month.JULY -> "Jul"
    Month.AUGUST -> "Aug"
    Month.SEPTEMBER -> "Sep"
    Month.OCTOBER -> "Oct"
    Month.NOVEMBER -> "Nov"
    Month.DECEMBER -> "Dec"
  }

private fun Month.toLongMonthName(): String =
  when (this) {
    Month.JANUARY -> "January"
    Month.FEBRUARY -> "February"
    Month.MARCH -> "March"
    Month.APRIL -> "April"
    Month.MAY -> "May"
    Month.JUNE -> "June"
    Month.JULY -> "July"
    Month.AUGUST -> "August"
    Month.SEPTEMBER -> "September"
    Month.OCTOBER -> "October"
    Month.NOVEMBER -> "November"
    Month.DECEMBER -> "December"
  }
