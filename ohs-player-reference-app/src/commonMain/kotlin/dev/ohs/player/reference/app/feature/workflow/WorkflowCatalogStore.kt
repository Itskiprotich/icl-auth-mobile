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

import iclauth.ohs_player_reference_app.generated.resources.Res
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json

private val workflowJson = Json { ignoreUnknownKeys = true }

object WorkflowCatalogStore {
  private const val ASSET_PATH = "files/workflows/workflow-catalog.json"

  private val mutex = Mutex()
  private var cachedCatalog: WorkflowCatalog? = null

  suspend fun catalog(): WorkflowCatalog {
    cachedCatalog?.let { return it }

    return mutex.withLock {
      cachedCatalog ?: loadCatalog().also { cachedCatalog = it }
    }
  }

  suspend fun module(moduleId: String): WorkflowModule? = catalog().findModule(moduleId)

  private suspend fun loadCatalog(): WorkflowCatalog {
    val json = Res.readBytes(ASSET_PATH).decodeToString()
    return workflowJson.decodeFromString(WorkflowCatalog.serializer(), json)
  }
}
