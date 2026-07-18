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
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Instant

class ReferenceAuthSessionCodecTest {

  @Test
  fun roundTripsAnAuthSession() {
    val session =
      AuthSession(
        accessToken = "access-token",
        tokenType = "Bearer",
        refreshToken = "refresh-token",
        issuedAt = Instant.parse("2026-07-18T08:30:00Z"),
        accessTokenExpiresAt = Instant.parse("2026-07-18T09:30:00Z"),
        refreshTokenExpiresAt = Instant.parse("2026-07-18T10:30:00Z"),
        expiresInSeconds = 3600,
        refreshExpiresInSeconds = 7200,
        notBeforePolicy = 0,
        sessionState = "session-state",
        scope = "openid profile",
        firstLogin = false,
        status = "ACTIVE",
      )

    val encoded = ReferenceAuthSessionCodec.encode(session)

    assertEquals(session, ReferenceAuthSessionCodec.decode(encoded))
  }

  @Test
  fun returnsNullForMalformedPayload() {
    assertNull(ReferenceAuthSessionCodec.decode("{not-json"))
  }
}
