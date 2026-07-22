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

import icl.ohs.libs.auth.models.AuthSession
import icl.ohs.libs.auth.models.AuthSessionStore
import platform.Foundation.NSUserDefaults

private const val AUTH_SESSION_KEY = "reference.auth.session"

class AppleAuthSessionStore(
  private val userDefaults: NSUserDefaults = NSUserDefaults.standardUserDefaults
) : AuthSessionStore {

  override var session: AuthSession?
    get() {
      val serializedSession = userDefaults.stringForKey(AUTH_SESSION_KEY) ?: return null
      return ReferenceAuthSessionCodec.decode(serializedSession)
        ?: run {
          userDefaults.removeObjectForKey(AUTH_SESSION_KEY)
          null
        }
    }
    set(value) {
      if (value == null) {
        userDefaults.removeObjectForKey(AUTH_SESSION_KEY)
      } else {
        userDefaults.setObject(ReferenceAuthSessionCodec.encode(value), forKey = AUTH_SESSION_KEY)
      }
    }
}
