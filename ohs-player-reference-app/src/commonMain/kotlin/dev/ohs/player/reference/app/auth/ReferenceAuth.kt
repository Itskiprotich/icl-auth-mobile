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

import icl.ohs.libs.auth.IclAuth
import icl.ohs.libs.auth.models.AuthSessionStore
import icl.ohs.libs.auth.models.IclAuthConfig
import icl.ohs.libs.auth.models.InMemoryAuthSessionStore

private const val AUTH_BASE_URL = "https://auth.nphiis.health.go.ke"

fun initializeReferenceAuth(sessionStore: AuthSessionStore = InMemoryAuthSessionStore) {
  IclAuth.initialize(IclAuthConfig(baseAuthUrl = AUTH_BASE_URL, sessionStore = sessionStore))
}

fun initializeReferenceAuthIfNeeded(sessionStore: AuthSessionStore = InMemoryAuthSessionStore) {
  if (!IclAuth.isInitialized) {
    initializeReferenceAuth(sessionStore)
  }
}
