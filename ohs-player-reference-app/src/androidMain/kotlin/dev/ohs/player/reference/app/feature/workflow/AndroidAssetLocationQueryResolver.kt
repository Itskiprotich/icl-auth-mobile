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
package dev.ohs.player.reference.app.feature.workflow

import android.content.Context
import android.net.Uri
import dev.ohs.fhir.datacapture.XFhirQueryResolver
import dev.ohs.fhir.model.r4.Location
import dev.ohs.fhir.model.r4.Reference
import dev.ohs.fhir.model.r4.Resource
import dev.ohs.fhir.model.r4.String as FhirString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

private const val LOCATION_ASSET_PATH = "locations.json"

internal class AndroidAssetLocationQueryResolver(
  private val context: Context,
) : XFhirQueryResolver {
  override suspend fun resolve(xFhirQuery: String): List<Resource> {
    val query = LocationAssetQuery.parse(xFhirQuery) ?: return emptyList()
    return AndroidLocationAssetStore.locations(context)
      .asSequence()
      .filter(query::matches)
      .sortedBy { it.name?.value?.uppercase().orEmpty() }
      .toList()
  }
}

private object AndroidLocationAssetStore {
  private val json = Json { ignoreUnknownKeys = true }
  private val mutex = Mutex()
  private var cachedLocations: List<Location>? = null

  suspend fun locations(context: Context): List<Location> {
    cachedLocations?.let { return it }

    return mutex.withLock {
      cachedLocations ?: loadLocations(context).also { cachedLocations = it }
    }
  }

  private suspend fun loadLocations(context: Context): List<Location> =
    withContext(Dispatchers.IO) {
      context.assets.open(LOCATION_ASSET_PATH).bufferedReader().use { reader ->
        json.decodeFromString<List<LocationAssetEntry>>(reader.readText())
      }
    }
    .mapNotNull { it.resource?.toLocation() }
}

private data class LocationAssetQuery(
  val ids: Set<String>,
  val partOfReference: String?,
) {
  fun matches(location: Location): Boolean {
    if (ids.isNotEmpty() && location.id.normalizeId() !in ids) {
      return false
    }

    if (partOfReference != null &&
      location.partOf?.reference?.value.normalizeReference() != partOfReference) {
      return false
    }

    return true
  }

  companion object {
    fun parse(rawQuery: String): LocationAssetQuery? {
      val trimmed = rawQuery.trim()
      if (!trimmed.startsWith("Location?")) {
        return null
      }

      val uri = Uri.parse("https://resolver.invalid/$trimmed")
      val ids =
        uri.getQueryParameter("_id")
          ?.split(',')
          .orEmpty()
          .mapNotNull { it.normalizeId() }
          .toSet()
      val partOfReference =
        uri.getQueryParameter("partof")?.normalizeReference()
          ?: uri.getQueryParameter("partOf")?.normalizeReference()

      return LocationAssetQuery(ids = ids, partOfReference = partOfReference)
    }
  }
}

@Serializable
private data class LocationAssetEntry(
  val resource: LocationAssetResource? = null,
)

@Serializable
private data class LocationAssetResource(
  val id: String? = null,
  val name: String? = null,
  val partOf: LocationAssetReference? = null,
) {
  fun toLocation(): Location? {
    val locationId = id?.trim()?.takeIf(String::isNotBlank) ?: return null
    return Location(
      id = locationId,
      name = name?.trim()?.takeIf(String::isNotBlank)?.let { FhirString.of(it, null) },
      partOf = partOf?.toReference(),
    )
  }
}

@Serializable
private data class LocationAssetReference(
  val reference: String? = null,
  val display: String? = null,
) {
  fun toReference(): Reference =
    Reference(
      reference = reference?.trim()?.takeIf(String::isNotBlank)?.let { FhirString.of(it, null) },
      display = display?.trim()?.takeIf(String::isNotBlank)?.let { FhirString.of(it, null) },
    )
}

private fun String?.normalizeId(): String? =
  this?.trim()?.substringAfterLast('/')?.takeIf(String::isNotBlank)

private fun String?.normalizeReference(): String? =
  normalizeId()?.let { "Location/$it" }
