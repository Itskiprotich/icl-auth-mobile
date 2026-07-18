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

actual object WorkflowFhirStore {
  actual val isPersistenceAvailable: Boolean = false

  actual fun initialize(platformContext: Any) = Unit

  actual suspend fun saveQuestionnaireResponse(response: QuestionnaireResponse): String? = null

  actual suspend fun listQuestionnaireResponses(): List<QuestionnaireResponse> = emptyList()

  actual suspend fun saveBundle(bundle: Bundle): List<String?> = emptyList()
}
