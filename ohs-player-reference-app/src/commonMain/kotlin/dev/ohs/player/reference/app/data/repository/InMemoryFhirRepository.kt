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
package dev.ohs.player.reference.app.data.repository

import dev.ohs.fhir.model.r4.Bundle
import dev.ohs.fhir.model.r4.Resource
import dev.ohs.player.reference.app.util.FhirJson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class InMemoryFhirRepository : FhirRepository {
  private val json = FhirJson.instance
  private val resourcesByType = mutableMapOf<String, MutableMap<String, Resource>>()
  private val _revision = MutableStateFlow(0L)

  override val revision: StateFlow<Long> = _revision

  override suspend fun upsert(resource: Resource) {
    store(resource)
    _revision.value += 1
  }

  override suspend fun upsert(bundle: Bundle): Int {
    val resources = bundle.entry.mapNotNull { it.resource }
    resources.forEach(::store)
    if (resources.isNotEmpty()) _revision.value += 1
    return resources.size
  }

  override suspend fun get(resourceType: String, id: String): Resource? {
    return resourcesByType[resourceType]?.get(id)
  }

  override suspend fun all(resourceType: String): List<Resource> {
    return resourcesByType[resourceType]?.values?.toList().orEmpty()
  }

  private fun store(resource: Resource) {
    val id = resource.id ?: return
    val resourceType = resourceTypeOf(resource)
    resourcesByType.getOrPut(resourceType) { mutableMapOf() }[id] = resource
  }

  private fun resourceTypeOf(resource: Resource): String =
    json
      .encodeToJsonElement(Resource.serializer(), resource)
      .jsonObject["resourceType"]
      ?.jsonPrimitive
      ?.content ?: error("FHIR resource is missing resourceType")
}
