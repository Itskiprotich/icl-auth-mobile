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
package dev.ohs.player.auth

import icl.ohs.libs.auth.AuthSession
import icl.ohs.libs.auth.IclAuth
import icl.ohs.libs.auth.IclAuthConfig
import icl.ohs.libs.auth.InMemoryAuthSessionStore
import icl.ohs.libs.auth.NotificationListResult
import icl.ohs.libs.auth.NotificationScreenConfig
import icl.ohs.libs.auth.NotificationService
import icl.ohs.libs.auth.resolveNotificationConfig
import icl.ohs.libs.auth.validateNotificationRequest
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.time.Instant
import kotlinx.coroutines.test.runTest

class NotificationClientTest {

  @Test
  fun resolveNotificationConfig_combinesBaseUrlAndEndpoint() {
    IclAuth.initialize(IclAuthConfig(baseAuthUrl = "https://auth.example.com"))

    val config = resolveNotificationConfig(NotificationScreenConfig(endpoint = "/notifications"))

    assertEquals("https://auth.example.com/notifications", config.notificationsUrl)
  }

  @Test
  fun validateNotificationRequest_requiresAnActiveSession() {
    IclAuth.clear()
    val config =
      resolveNotificationConfig(
        screenConfig = NotificationScreenConfig(endpoint = "/notifications"),
        authConfig = IclAuthConfig(baseAuthUrl = "https://auth.example.com"),
      )

    val failure = validateNotificationRequest(config = config, authorizationHeader = null)

    assertEquals(config.messages.missingSession, failure?.message)
  }

  @Test
  fun fetchNotifications_usesBearerTokenAndParsesResponse() = runTest {
    IclAuth.clear()
    val sessionStore =
      InMemoryAuthSessionStore.also {
        it.session =
          AuthSession(
            accessToken = "abc123",
            tokenType = "Bearer",
            issuedAt = Instant.parse("2026-07-18T08:30:00Z"),
          )
      }
    IclAuth.initialize(
      IclAuthConfig(baseAuthUrl = "https://auth.example.com", sessionStore = sessionStore)
    )
    val config = resolveNotificationConfig(NotificationScreenConfig(endpoint = "/notifications"))
    val client =
      HttpClient(
        MockEngine { request ->
          assertEquals(HttpMethod.Get, request.method)
          assertEquals("/notifications", request.url.encodedPath)
          assertEquals("Bearer abc123", request.headers[HttpHeaders.Authorization])

          respond(
            content =
              """{"status":"success","notifications":[{"id":"d4d15281-3122-44db-a288-a5ad927639fa","practitionerId":"cd40811a-b174-45d0-ad63-6ff56ed249df","encounterId":"87c3a08b-7a71-4776-93e7-57dd7219e07e","type":"CASE_SUBMISSION","investigationDate":null,"dueDate":"2026-06-02T10:18:48.416Z","title":"New MEA case submitted","body":"Case KEN-GAR-DAD-2026-MEA-001 (MEA) submitted in GARISSA, DADAAB REFUGEE CAMP.","status":"FAILED","failureReason":null,"caseSubmissionEmailSentAt":"2026-06-02T10:06:16.166Z","createdAt":"2026-06-02T10:06:13.537Z","updatedAt":"2026-06-02T10:18:49.861Z"}]}""",
            status = HttpStatusCode.OK,
            headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
          )
        }
      ) {
        expectSuccess = false
      }
    val service = NotificationService(client)

    try {
      val result = service.fetchNotifications(config = config)

      val success = assertIs<NotificationListResult.Success>(result)
      assertEquals(200, success.value.statusCode)
      assertEquals("success", success.value.status)
      assertEquals(1, success.value.notifications.size)
      assertEquals("New MEA case submitted", success.value.notifications.single().title)
      assertEquals("FAILED", success.value.notifications.single().status)
    } finally {
      client.close()
    }
  }
}
