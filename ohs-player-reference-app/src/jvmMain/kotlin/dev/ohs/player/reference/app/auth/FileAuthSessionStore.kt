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
import java.io.File

private const val AUTH_SESSION_FILE_NAME = "auth-session.json"

class FileAuthSessionStore(storageDirectory: File = defaultReferenceStorageDirectory()) :
  AuthSessionStore {
  private val sessionFile = File(storageDirectory, AUTH_SESSION_FILE_NAME)

  override var session: AuthSession?
    get() {
      val serializedSession =
        sessionFile
          .takeIf(File::isFile)
          ?.let { file -> runCatching(file::readText).getOrNull() }
          ?.trim()
          ?.takeIf(String::isNotBlank) ?: return null

      return ReferenceAuthSessionCodec.decode(serializedSession)
        ?: run {
          sessionFile.delete()
          null
        }
    }
    set(value) {
      if (value == null) {
        sessionFile.delete()
        return
      }

      sessionFile.parentFile?.mkdirs()
      sessionFile.writeText(ReferenceAuthSessionCodec.encode(value))
    }
}

internal fun defaultReferenceStorageDirectory(): File {
  val userHome = System.getProperty("user.home").orEmpty().ifBlank { "." }
  return File(userHome, ".icl-auth-reference")
}
