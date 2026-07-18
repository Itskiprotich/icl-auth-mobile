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
package dev.ohs.player.reference.app.auth

import icl.ohs.libs.auth.AuthSession
import kotlin.time.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

internal object ReferenceAuthSessionCodec {
  private val json = Json { ignoreUnknownKeys = true }

  fun encode(session: AuthSession): String =
    json.encodeToString(StoredAuthSession.serializer(), session.toStoredAuthSession())

  fun decode(serialized: String): AuthSession? =
    serialized.trim().takeIf(String::isNotBlank)?.let { payload ->
      runCatching { json.decodeFromString(StoredAuthSession.serializer(), payload).toAuthSession() }
        .getOrNull()
    }
}

@Serializable
private data class StoredAuthSession(
  val accessToken: String,
  val tokenType: String,
  val refreshToken: String? = null,
  val issuedAt: String,
  val accessTokenExpiresAt: String? = null,
  val refreshTokenExpiresAt: String? = null,
  val expiresInSeconds: Long? = null,
  val refreshExpiresInSeconds: Long? = null,
  val notBeforePolicy: Long? = null,
  val sessionState: String? = null,
  val scope: String? = null,
  val firstLogin: Boolean? = null,
  val status: String? = null,
)

private fun AuthSession.toStoredAuthSession(): StoredAuthSession =
  StoredAuthSession(
    accessToken = accessToken,
    tokenType = tokenType,
    refreshToken = refreshToken,
    issuedAt = issuedAt.toString(),
    accessTokenExpiresAt = accessTokenExpiresAt?.toString(),
    refreshTokenExpiresAt = refreshTokenExpiresAt?.toString(),
    expiresInSeconds = expiresInSeconds,
    refreshExpiresInSeconds = refreshExpiresInSeconds,
    notBeforePolicy = notBeforePolicy,
    sessionState = sessionState,
    scope = scope,
    firstLogin = firstLogin,
    status = status,
  )

private fun StoredAuthSession.toAuthSession(): AuthSession =
  AuthSession(
    accessToken = accessToken,
    tokenType = tokenType,
    refreshToken = refreshToken,
    issuedAt = Instant.parse(issuedAt),
    accessTokenExpiresAt = accessTokenExpiresAt?.let(Instant::parse),
    refreshTokenExpiresAt = refreshTokenExpiresAt?.let(Instant::parse),
    expiresInSeconds = expiresInSeconds,
    refreshExpiresInSeconds = refreshExpiresInSeconds,
    notBeforePolicy = notBeforePolicy,
    sessionState = sessionState,
    scope = scope,
    firstLogin = firstLogin,
    status = status,
  )
