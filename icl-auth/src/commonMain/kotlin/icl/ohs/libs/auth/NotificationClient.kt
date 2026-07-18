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

import io.ktor.client.HttpClient
import io.ktor.client.request.accept
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject

internal class NotificationService(private val httpClient: HttpClient) {

  suspend fun fetchNotifications(config: ResolvedNotificationConfig): NotificationListResult {
    val validationFailure = validateNotificationRequest(config)
    if (validationFailure != null) {
      return NotificationListResult.Failure(validationFailure)
    }

    val notificationsUrl =
      config.notificationsUrl
        ?: return NotificationListResult.Failure(
          NotificationListFailure(message = config.messages.missingNotificationsUrl)
        )
    val authorizationHeader =
      IclAuth.currentAuthorizationHeader()
        ?: return NotificationListResult.Failure(
          NotificationListFailure(message = config.messages.missingSession)
        )

    return try {
      val response =
        httpClient.get(notificationsUrl) {
          accept(ContentType.Application.Json)
          config.requestHeaders.forEach { (name, value) -> header(name, value) }
          header(HttpHeaders.Authorization, authorizationHeader)
        }
      val responseBody = response.bodyAsText()

      if (response.status.isSuccess()) {
        val parsedResponse =
          parseNotificationsResponse(responseBody)
            ?: return NotificationListResult.Failure(
              NotificationListFailure(
                message = config.messages.invalidResponse,
                statusCode = response.status.value,
                responseBody = responseBody,
              )
            )

        NotificationListResult.Success(
          NotificationListSuccess(
            statusCode = response.status.value,
            responseBody = responseBody,
            status = parsedResponse.status,
            notifications = parsedResponse.notifications,
          )
        )
      } else {
        NotificationListResult.Failure(
          NotificationListFailure(
            message =
              resolveNotificationFailureMessage(
                config = config,
                statusCode = response.status.value,
                responseBody = responseBody,
              ),
            statusCode = response.status.value,
            responseBody = responseBody,
          )
        )
      }
    } catch (error: Throwable) {
      if (error is CancellationException) {
        throw error
      }

      NotificationListResult.Failure(
        NotificationListFailure(message = config.messages.networkError)
      )
    }
  }
}

internal data class ParsedNotificationsResponse(
  val status: String? = null,
  val notifications: List<AuthNotification> = emptyList(),
)

internal fun validateNotificationRequest(
  config: ResolvedNotificationConfig,
  authorizationHeader: String? = IclAuth.currentAuthorizationHeader(),
): NotificationListFailure? =
  when {
    config.notificationsUrl.isNullOrBlank() ->
      NotificationListFailure(message = config.messages.missingNotificationsUrl)
    authorizationHeader.isNullOrBlank() ->
      NotificationListFailure(message = config.messages.missingSession)
    else -> null
  }

internal fun resolveNotificationFailureMessage(
  config: ResolvedNotificationConfig,
  statusCode: Int,
  responseBody: String,
): String =
  config.responseMessageResolver?.invoke(statusCode, responseBody)?.takeIf(String::isNotBlank)
    ?: extractResponseMessage(responseBody = responseBody, keys = config.responseMessageKeys)
    ?: when {
      statusCode == 401 || statusCode == 403 -> config.messages.unauthorized
      statusCode >= 500 -> config.messages.serverError
      else -> config.messages.unexpectedError
    }

internal fun parseNotificationsResponse(responseBody: String): ParsedNotificationsResponse? {
  val json = parseJsonObject(responseBody) ?: return null
  return ParsedNotificationsResponse(
    status = json.rawStringValue("status"),
    notifications =
      json.arrayValue("notifications")?.mapNotNull { it.toAuthNotification() }.orEmpty(),
  )
}

internal fun resolveNotificationConfig(
  screenConfig: NotificationScreenConfig,
  authConfig: IclAuthConfig? = IclAuth.currentConfiguration(),
): ResolvedNotificationConfig =
  ResolvedNotificationConfig(
    notificationsUrl =
      resolveAuthUrl(baseAuthUrl = authConfig?.baseAuthUrl, endpoint = screenConfig.endpoint),
    requestHeaders = authConfig?.defaultRequestHeaders.orEmpty() + screenConfig.requestHeaders,
    requestTimeoutMillis =
      screenConfig.requestTimeoutMillis ?: authConfig?.requestTimeoutMillis ?: 15_000,
    responseMessageKeys =
      screenConfig.responseMessageKeys
        ?: authConfig?.responseMessageKeys
        ?: listOf("message", "error", "detail"),
    messages = screenConfig.messages ?: NotificationMessages(),
    responseMessageResolver = screenConfig.responseMessageResolver,
  )

internal fun JsonArray.toAuthNotifications(): List<AuthNotification> = mapNotNull {
  it.toAuthNotification()
}

internal fun kotlinx.serialization.json.JsonElement.toAuthNotification(): AuthNotification? {
  val json = runCatching { jsonObject }.getOrNull() ?: return null
  return AuthNotification(
    id = json.rawStringValue("id"),
    practitionerId = json.rawStringValue("practitionerId"),
    encounterId = json.rawStringValue("encounterId"),
    type = json.rawStringValue("type"),
    investigationDate = json.rawStringValue("investigationDate"),
    dueDate = json.rawStringValue("dueDate"),
    title = json.rawStringValue("title"),
    body = json.rawStringValue("body"),
    status = json.rawStringValue("status"),
    failureReason = json.rawStringValue("failureReason"),
    caseSubmissionEmailSentAt = json.rawStringValue("caseSubmissionEmailSentAt"),
    createdAt = json.rawStringValue("createdAt"),
    updatedAt = json.rawStringValue("updatedAt"),
  )
}

internal fun kotlinx.serialization.json.JsonObject.arrayValue(key: String): JsonArray? =
  runCatching { this[key]?.jsonArray }.getOrNull()
