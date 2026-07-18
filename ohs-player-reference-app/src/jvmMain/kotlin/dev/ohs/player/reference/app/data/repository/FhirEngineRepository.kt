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

import dev.ohs.fhir.FhirEngine
import dev.ohs.fhir.db.ResourceNotFoundException
import dev.ohs.fhir.model.r4.Resource
import dev.ohs.fhir.search.Search

class FhirEngineRepository(fhirEngine: FhirEngine) :
  FhirRepository by EngineBackedFhirRepository(
    getResource = { type, id ->
      runCatching { fhirEngine.get(type, id) }
        .getOrElse { if (it is ResourceNotFoundException) null else throw it }
    },
    listResources = { type -> fhirEngine.search<Resource>(Search(type)).map { it.resource } },
    createResource = { resource -> fhirEngine.create(resource) },
    updateResource = { resource -> fhirEngine.update(resource) },
    withTransaction = { block -> fhirEngine.withTransaction { block() } },
  )
