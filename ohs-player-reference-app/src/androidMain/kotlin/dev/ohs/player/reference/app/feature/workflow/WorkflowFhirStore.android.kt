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

import dev.ohs.fhir.model.r4.Bundle
import dev.ohs.fhir.model.r4.QuestionnaireResponse
import dev.ohs.player.reference.app.data.repository.FhirRepository
import dev.ohs.player.reference.app.data.repository.resolveFhirRepository
import dev.ohs.player.reference.app.generateUuid

actual object WorkflowFhirStore {
  actual val isPersistenceAvailable: Boolean = true

  actual fun initialize(platformContext: Any) = Unit

  actual suspend fun saveQuestionnaireResponse(response: QuestionnaireResponse): String? {
    val storedResponse = response.withIdIfMissing()
    repository().upsert(storedResponse)
    return storedResponse.id
  }

  actual suspend fun listQuestionnaireResponses(): List<QuestionnaireResponse> {
    return repository().all("QuestionnaireResponse").filterIsInstance<QuestionnaireResponse>()
  }

  actual suspend fun saveBundle(bundle: Bundle): List<String?> {
    repository().upsert(bundle)
    return bundle.entry.map { it.resource?.id }
  }

  private fun repository(): FhirRepository = resolveFhirRepository()

  private fun QuestionnaireResponse.withIdIfMissing(): QuestionnaireResponse =
    if (id.isNullOrBlank()) copy(id = generateUuid()) else this
}
