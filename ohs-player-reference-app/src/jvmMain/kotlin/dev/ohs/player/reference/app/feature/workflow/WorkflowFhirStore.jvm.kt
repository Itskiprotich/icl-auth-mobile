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

import dev.ohs.fhir.FhirEngine
import dev.ohs.fhir.FhirEngineConfiguration
import dev.ohs.fhir.FhirEngineProvider
import dev.ohs.fhir.model.r4.QuestionnaireResponse
import dev.ohs.fhir.model.r4.terminologies.ResourceType
import dev.ohs.fhir.search.Search
import java.io.File

actual object WorkflowFhirStore {
  private var platformContext: Any = Unit
  private var fhirEngine: FhirEngine? = null

  private val storageDirectory: String
    get() = File(System.getProperty("user.home"), ".icl-auth-reference").absolutePath

  actual val isPersistenceAvailable: Boolean = true

  actual fun initialize(platformContext: Any) {
    this.platformContext = platformContext
    if (FhirEngineProvider.isNotInitialized()) {
      FhirEngineProvider.init(
        FhirEngineConfiguration(storageDirectory = storageDirectory),
        platformContext,
      )
    }
    if (fhirEngine == null) {
      fhirEngine = FhirEngineProvider.getInstance(platformContext)
    }
  }

  actual suspend fun saveQuestionnaireResponse(response: QuestionnaireResponse): String? {
    return engine().create(response).firstOrNull()
  }

  actual suspend fun listQuestionnaireResponses(): List<QuestionnaireResponse> {
    return engine().search<QuestionnaireResponse>(Search(ResourceType.QuestionnaireResponse)).map { it.resource }
  }

  private fun engine(): FhirEngine {
    initialize(platformContext)
    return checkNotNull(fhirEngine)
  }
}
