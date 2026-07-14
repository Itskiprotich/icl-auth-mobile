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

import dev.ohs.fhir.model.r4.Questionnaire
import iclohsmobileclient.icl_ohs_starter_app.generated.resources.Res
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json

private val questionnaireJson = Json { ignoreUnknownKeys = true }

object WorkflowQuestionnaireStore {
  private val mutex = Mutex()
  private val cachedQuestionnaires = mutableMapOf<String, Questionnaire>()

  suspend fun questionnaire(resource: String): Questionnaire =
    mutex.withLock {
      cachedQuestionnaires[resource]
        ?: loadQuestionnaire(resource).also { cachedQuestionnaires[resource] = it }
    }

  private suspend fun loadQuestionnaire(resource: String): Questionnaire {
    val json = Res.readBytes("files/$resource").decodeToString()
    return questionnaireJson.decodeFromString(Questionnaire.serializer(), json)
  }
}
