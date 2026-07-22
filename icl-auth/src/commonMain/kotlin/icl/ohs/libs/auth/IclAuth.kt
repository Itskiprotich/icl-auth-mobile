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

import icl.ohs.libs.auth.models.AuthSession
import icl.ohs.libs.auth.models.AuthSessionStore
import icl.ohs.libs.auth.models.IclAuthConfig
import icl.ohs.libs.auth.models.ProviderProfile
import icl.ohs.libs.auth.models.ProviderUser
import icl.ohs.libs.auth.network.buildLoginHttpClient
import icl.ohs.libs.auth.network.parseProviderProfile
import icl.ohs.libs.auth.network.resolveAuthUrl
import io.ktor.client.HttpClient
import io.ktor.client.request.accept
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.coroutines.CancellationException

object IclAuth {

  private var configuration: IclAuthConfig? = null
  private var providerProfile: ProviderProfile? = null
  private val sessionStore: AuthSessionStore?
    get() = configuration?.sessionStore

  val isInitialized: Boolean
    get() = configuration != null

  val hasSession: Boolean
    get() = currentSession() != null

  val hasProviderProfile: Boolean
    get() = providerProfile != null

  fun initialize(config: IclAuthConfig) {
    configuration = config
  }

  fun clearSession() {
    sessionStore?.session = null
    providerProfile = null
  }

  fun clear() {
    clearSession()
    configuration = null
  }

  fun currentSession(): AuthSession? = sessionStore?.session

  fun currentProviderProfile(): ProviderProfile? = providerProfile

  fun currentProviderUser(): ProviderUser? = providerProfile?.user

  fun hasValidAccessToken(now: Instant = Clock.System.now()): Boolean =
    currentValidSession(now) != null

  fun currentAccessToken(now: Instant = Clock.System.now()): String? =
    currentValidSession(now)?.accessToken

  fun currentTokenType(now: Instant = Clock.System.now()): String? =
    currentValidSession(now)?.tokenType

  fun currentAuthorizationHeader(now: Instant = Clock.System.now()): String? =
    currentValidSession(now)?.authorizationHeader

  fun currentAuthHeaders(now: Instant = Clock.System.now()): Map<String, String> =
    currentAuthorizationHeader(now)?.let { mapOf("Authorization" to it) }.orEmpty()

  suspend fun refreshProviderProfile(): ProviderProfile? {
    val config = currentConfiguration() ?: return currentProviderProfile()
    val httpClient = buildLoginHttpClient(config.requestTimeoutMillis)

    return try {
      refreshProviderProfile(httpClient)
    } finally {
      httpClient.close()
    }
  }

  internal fun currentConfiguration(): IclAuthConfig? = configuration

  private fun currentValidSession(now: Instant): AuthSession? =
    currentSession()?.takeIf { it.isAccessTokenValid(now) }

  internal fun updateSession(
    session: AuthSession?,
    sessionStore: AuthSessionStore? = this.sessionStore,
  ) {
    sessionStore?.session = session
  }

  internal fun updateProviderProfile(providerProfile: ProviderProfile?) {
    this.providerProfile = providerProfile
  }

  internal suspend fun refreshProviderProfile(httpClient: HttpClient): ProviderProfile? {
    val authConfig = currentConfiguration() ?: return currentProviderProfile()
    val session = currentValidSession(Clock.System.now()) ?: return currentProviderProfile()
    val providerProfileUrl =
      resolveAuthUrl(
        baseAuthUrl = authConfig.baseAuthUrl,
        endpoint = authConfig.providerProfileEndpoint,
      ) ?: return currentProviderProfile()

    return try {
      val response =
        httpClient.get(providerProfileUrl) {
          accept(ContentType.Application.Json)
          authConfig.defaultRequestHeaders.forEach { (name, value) -> header(name, value) }
          header(HttpHeaders.Authorization, session.authorizationHeader)
        }
      val responseBody = response.bodyAsText()

      if (response.status.isSuccess()) {
        parseProviderProfile(responseBody)?.also(::updateProviderProfile)
      } else {
        currentProviderProfile()
      }
    } catch (error: Throwable) {
      if (error is CancellationException) {
        throw error
      }

      currentProviderProfile()
    }
  }
}
