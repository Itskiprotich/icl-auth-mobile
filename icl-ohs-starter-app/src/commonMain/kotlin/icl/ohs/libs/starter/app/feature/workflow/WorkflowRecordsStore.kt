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
package icl.ohs.libs.starter.app.feature.workflow

import iclohsmobileclient.icl_ohs_starter_app.generated.resources.Res
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class WorkflowRecordCollection(
  val id: String,
  val title: String,
  val subtitle: String = "",
  val emptyMessage: String = "No records found.",
  val pageSize: Int = 10,
  val records: List<WorkflowRecord> = emptyList(),
)

@Serializable
data class WorkflowRecord(
  val id: String,
  val title: String,
  val subtitle: String = "",
  val status: String = "",
  val statusTone: WorkflowRecordTone = WorkflowRecordTone.INFO,
  val meta: List<String> = emptyList(),
  val fields: List<WorkflowRecordField> = emptyList(),
)

@Serializable
data class WorkflowRecordField(
  val label: String,
  val value: String,
)

@Serializable
enum class WorkflowRecordTone {
  @SerialName("neutral") NEUTRAL,
  @SerialName("success") SUCCESS,
  @SerialName("warning") WARNING,
  @SerialName("critical") CRITICAL,
  @SerialName("info") INFO,
}

private val workflowRecordsJson = Json { ignoreUnknownKeys = true }

object WorkflowRecordsStore {
  private val mutex = Mutex()
  private val cachedCollections = mutableMapOf<String, WorkflowRecordCollection>()

  suspend fun records(resource: String): WorkflowRecordCollection =
    mutex.withLock {
      cachedCollections[resource] ?: loadRecords(resource).also { cachedCollections[resource] = it }
    }

  private suspend fun loadRecords(resource: String): WorkflowRecordCollection {
    val json = Res.readBytes("files/$resource").decodeToString()
    return workflowRecordsJson.decodeFromString(WorkflowRecordCollection.serializer(), json)
  }
}
