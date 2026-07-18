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
import dev.ohs.player.reference.app.data.datasource.loadSampleResourcesBundle
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class SeededFhirRepository(private val delegate: FhirRepository) : FhirRepository {
  private val initializationMutex = Mutex()
  private var initialized = false

  override val revision: StateFlow<Long> = delegate.revision

  override suspend fun upsert(resource: Resource) {
    ensureInitialized()
    delegate.upsert(resource)
  }

  override suspend fun upsert(bundle: Bundle): Int {
    ensureInitialized()
    return delegate.upsert(bundle)
  }

  override suspend fun get(resourceType: String, id: String): Resource? {
    ensureInitialized()
    return delegate.get(resourceType, id)
  }

  override suspend fun all(resourceType: String): List<Resource> {
    ensureInitialized()
    return delegate.all(resourceType)
  }

  private suspend fun ensureInitialized() {
    if (initialized) return

    initializationMutex.withLock {
      if (initialized) return

      val hasPatients = delegate.all("Patient").isNotEmpty()
      val hasGroups = delegate.all("Group").isNotEmpty()
      if (!hasPatients && !hasGroups) {
        delegate.upsert(loadSampleResourcesBundle())
      }

      initialized = true
    }
  }
}
