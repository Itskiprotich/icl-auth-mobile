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
package icl.ohs.libs.auth.models

data class NotificationMessages(
  val missingNotificationsUrl: String = "Configure the notifications URL to continue.",
  val missingSession: String = "Sign in again to view your notifications.",
  val unauthorized: String = "Your session is no longer valid. Please sign in again.",
  val networkError: String = "Unable to reach the notifications service. Please try again.",
  val serverError: String = "Unable to load notifications right now. Please try again.",
  val unexpectedError: String = "Something went wrong while loading notifications.",
  val invalidResponse: String = "Received an unexpected notifications response.",
  val emptyStateTitle: String = "No notifications yet",
  val emptyStateMessage: String = "New alerts will appear here once they are available.",
  val noResultsTitle: String = "No matching notifications",
  val noResultsMessage: String = "Try adjusting your search or filters.",
)

data class NotificationScreenConfig(
  val endpoint: String = "/notifications",
  val title: String = "Notifications",
  val showBackButton: Boolean = true,
  val requestHeaders: Map<String, String> = emptyMap(),
  val requestTimeoutMillis: Long? = null,
  val responseMessageKeys: List<String>? = null,
  val messages: NotificationMessages? = null,
  val responseMessageResolver: ((statusCode: Int, responseBody: String) -> String?)? = null,
)

data class AuthNotification(
  val id: String? = null,
  val practitionerId: String? = null,
  val encounterId: String? = null,
  val type: String? = null,
  val investigationDate: String? = null,
  val dueDate: String? = null,
  val title: String? = null,
  val body: String? = null,
  val status: String? = null,
  val failureReason: String? = null,
  val caseSubmissionEmailSentAt: String? = null,
  val createdAt: String? = null,
  val updatedAt: String? = null,
)

data class NotificationListSuccess(
  val statusCode: Int,
  val responseBody: String,
  val status: String? = null,
  val notifications: List<AuthNotification> = emptyList(),
)

data class NotificationListFailure(
  val message: String,
  val statusCode: Int? = null,
  val responseBody: String? = null,
)

internal data class ResolvedNotificationConfig(
  val notificationsUrl: String?,
  val requestHeaders: Map<String, String>,
  val requestTimeoutMillis: Long,
  val responseMessageKeys: List<String>,
  val messages: NotificationMessages,
  val responseMessageResolver: ((statusCode: Int, responseBody: String) -> String?)?,
)

internal sealed interface NotificationListResult {
  data class Success(val value: NotificationListSuccess) : NotificationListResult

  data class Failure(val value: NotificationListFailure) : NotificationListResult
}
