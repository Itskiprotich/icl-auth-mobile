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
import icl.ohs.libs.auth.AuthSessionStore
import kotlinx.browser.window

private const val AUTH_SESSION_KEY = "reference.auth.session"

class WebAuthSessionStore : AuthSessionStore {
  override var session: AuthSession?
    get() {
      val serializedSession = window.localStorage.getItem(AUTH_SESSION_KEY) ?: return null
      return ReferenceAuthSessionCodec.decode(serializedSession)
        ?: run {
          window.localStorage.removeItem(AUTH_SESSION_KEY)
          null
        }
    }
    set(value) {
      if (value == null) {
        window.localStorage.removeItem(AUTH_SESSION_KEY)
      } else {
        window.localStorage.setItem(AUTH_SESSION_KEY, ReferenceAuthSessionCodec.encode(value))
      }
    }
}
