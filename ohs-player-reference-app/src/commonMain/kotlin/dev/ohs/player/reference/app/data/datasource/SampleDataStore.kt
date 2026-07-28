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
package dev.ohs.player.reference.app.data.datasource

import dev.ohs.fhir.model.r4.AllergyIntolerance
import dev.ohs.fhir.model.r4.Condition
import dev.ohs.fhir.model.r4.Immunization
import dev.ohs.fhir.model.r4.MedicationRequest
import dev.ohs.fhir.model.r4.Patient
import dev.ohs.fhir.model.r4.Resource
import dev.ohs.player.library.model.SearchResult
import dev.ohs.player.reference.app.data.patientIdFromReference
import dev.ohs.player.reference.app.data.repository.FhirRepository

/** Returns all patient IDs — used by the patient list screen. */
suspend fun allPatientIds(repository: FhirRepository): List<String> =
  repository.all("Patient").mapNotNull { (it as? Patient)?.id }

/**
 * Patient list: root = Patient only. No clinical resources needed — the list card shows summary
 * fields available directly on the Patient resource.
 */
suspend fun patientSummarySearchResult(
  patientId: String,
  repository: FhirRepository,
): SearchResult<Resource>? {
  val patient = repository.get("Patient", patientId) as? Patient ?: return null
  return SearchResult(resource = patient)
}

/**
 * Patient profile: root = Patient, all clinical resources in revIncluded. Mirrors a real `GET
 * /Patient/{id}/$everything` response. All section extractors run against this single result.
 */
suspend fun patientProfileSearchResult(
  patientId: String,
  repository: FhirRepository,
): SearchResult<Resource>? {
  val patient = repository.get("Patient", patientId) as? Patient ?: return null
  val revIncluded = buildMap {
    repository
      .all("AllergyIntolerance")
      .filterIsInstance<AllergyIntolerance>()
      .filter { patientId(it.patient.reference?.value) == patientId }
      .takeIf { it.isNotEmpty() }
      ?.let { put("AllergyIntolerance" to "patient", it) }
    repository
      .all("MedicationRequest")
      .filterIsInstance<MedicationRequest>()
      .filter { patientId(it.subject.reference?.value) == patientId }
      .takeIf { it.isNotEmpty() }
      ?.let { put("MedicationRequest" to "subject", it) }
    repository
      .all("Condition")
      .filterIsInstance<Condition>()
      .filter { patientId(it.subject.reference?.value) == patientId }
      .takeIf { it.isNotEmpty() }
      ?.let { put("Condition" to "subject", it) }
    repository
      .all("Immunization")
      .filterIsInstance<Immunization>()
      .filter { patientId(it.patient.reference?.value) == patientId }
      .takeIf { it.isNotEmpty() }
      ?.let { put("Immunization" to "patient", it) }
  }
  return SearchResult(
    resource = patient,
    included = mapOf("patient" to listOf(patient)),
    revIncluded = revIncluded.ifEmpty { null },
  )
}

private fun patientId(reference: String?): String? = patientIdFromReference(reference)
