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

internal data class DerivedClassification(val code: String, val display: String)

internal fun deriveLabFinalClassification(measlesIgmResult: String?): DerivedClassification? =
  when (measlesIgmResult.normalizedWorkflowValue()) {
    "positive" -> DerivedClassification(code = "confirmed-by-lab", display = "Confirmed by Lab")
    "negative" -> DerivedClassification(code = "discarded", display = "Discarded")
    else -> null
  }

internal fun String?.normalizedWorkflowValue(): String? =
  this?.trim()
    ?.lowercase()
    ?.replace('_', ' ')
    ?.replace('-', ' ')
    ?.replace(Regex("\\s+"), " ")
    ?.ifBlank { null }

internal const val MEASLES_LAB_RESULTS_RESOURCE = "questionnaires/measles-lab-results.json"
internal const val MEASLES_IGM_CODE = "measles-igm"
internal const val FINAL_CLASSIFICATION_CODE = "final-classification"
internal const val LAB_RESULTS_CODE_SYSTEM = "http://example.org/CodeSystem/measles-classification"
