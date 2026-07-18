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
package dev.ohs.player.reference.app.data

internal fun patientIdFromReference(reference: String?): String? {
  return resourceIdFromReference(reference, resourceType = "Patient")
}

internal fun encounterIdFromReference(reference: String?): String? {
  return resourceIdFromReference(reference, resourceType = "Encounter")
}

internal fun resourceIdFromReference(reference: String?, resourceType: String): String? {
  val value = reference?.trim().orEmpty()
  if (value.isBlank()) return null

  val patientSegment =
    if ("$resourceType/" in value) {
      value.substringAfter("$resourceType/")
    } else {
      value.substringAfterLast('/')
    }

  return patientSegment.substringBefore('?').substringBefore('#').substringBefore('/').ifBlank {
    null
  }
}
