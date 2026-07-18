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
import dev.ohs.player.reference.app.data.encounterIdFromReference
import dev.ohs.player.reference.app.data.patientIdFromReference
import dev.ohs.player.reference.app.data.repository.FhirRepository
import dev.ohs.player.reference.app.data.repository.resolveFhirRepository
import dev.ohs.player.reference.app.generateUuid
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

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
  val references: WorkflowRecordReferences? = null,
)

@Serializable data class WorkflowRecordField(val label: String, val value: String)

@Serializable
data class WorkflowRecordReferences(
  val questionnaireResponseId: String? = null,
  val patientId: String? = null,
  val encounterId: String? = null,
  val questionnaireResource: String? = null,
  val recordResource: String? = null,
)

@Serializable
enum class WorkflowRecordTone {
  @SerialName("neutral") NEUTRAL,
  @SerialName("success") SUCCESS,
  @SerialName("warning") WARNING,
  @SerialName("critical") CRITICAL,
  @SerialName("info") INFO,
}

private val workflowResponseJson = Json {
  encodeDefaults = false
  explicitNulls = false
}

internal suspend fun saveWorkflowQuestionnaireResponse(response: QuestionnaireResponse): String? {
  val storedResponse = response.withIdIfMissing()
  workflowRepository().upsert(storedResponse)
  return storedResponse.id
}

internal suspend fun saveWorkflowBundle(bundle: Bundle): List<String?> {
  workflowRepository().upsert(bundle)
  return bundle.entry.map { it.resource?.id }
}

internal suspend fun loadWorkflowRecordCollection(
  resource: String,
  title: String,
  subtitle: String,
): WorkflowRecordCollection =
  if (resource.startsWith("records/")) {
    loadWorkflowCaseRecordCollection(resource = resource, title = title, subtitle = subtitle)
  } else {
    buildStoredResponseCollection(
      resource = resource,
      title = title,
      subtitle = subtitle,
      responses = listWorkflowQuestionnaireResponses(resource),
    )
  }

internal suspend fun loadWorkflowRecordCount(resource: String): Int =
  listWorkflowQuestionnaireResponses(resource).size

internal fun buildStoredResponseCollection(
  resource: String? = null,
  title: String,
  subtitle: String,
  responses: List<QuestionnaireResponse>,
): WorkflowRecordCollection =
  WorkflowRecordCollection(
    id = "stored-questionnaire-responses",
    title = title,
    subtitle = subtitle.ifBlank { "Locally saved case records" },
    emptyMessage = "No locally saved cases are available yet.",
    pageSize = 10,
    records =
      responses.sortedByDescending { it.authoredText() }.map { it.toWorkflowRecord(resource) },
  )

internal fun QuestionnaireResponse.toWorkflowRecord(
  recordResource: String? = null
): WorkflowRecord {
  val resource =
    workflowResponseJson.encodeToJsonElement(QuestionnaireResponse.serializer(), this).jsonObject
  val fields = buildList { resource.responseItems().collectWorkflowFields(this) }
  val authored = resource.authoredText()
  val questionnaireLabel = resource.questionnaireLabel()
  val title =
    preferredFieldValue(fields, "name")
      ?: preferredFieldValue(fields, "patient")
      ?: questionnaireLabel?.let { "$it Case" }
      ?: "Submitted Case"
  val subtitleParts =
    listOfNotNull(
      questionnaireLabel?.takeIf(String::isNotBlank),
      authored?.takeIf(String::isNotBlank),
    )
  val statusRaw = resource["status"]?.jsonPrimitive?.contentOrNull.orEmpty()

  return WorkflowRecord(
    id = resource["id"]?.jsonPrimitive?.contentOrNull ?: title.lowercase().replace(' ', '-'),
    title = title,
    subtitle = subtitleParts.joinToString(" • "),
    status = statusRaw.toWorkflowStatusLabel(),
    statusTone = statusRaw.toWorkflowStatusTone(),
    meta =
      listOfNotNull(
        authored?.takeIf(String::isNotBlank)?.let { "Authored: $it" },
        questionnaireLabel?.takeIf(String::isNotBlank)?.let { "Questionnaire: $it" },
      ),
    fields = fields,
    references =
      WorkflowRecordReferences(
        questionnaireResponseId = id,
        patientId = patientIdFromReference(subject?.reference?.value),
        encounterId = encounterIdFromReference(encounter?.reference?.value),
        questionnaireResource = questionnaireReferenceValue()?.toQuestionnaireResource(),
        recordResource = recordResource,
      ),
  )
}

private suspend fun listWorkflowQuestionnaireResponses(
  resource: String
): List<QuestionnaireResponse> {
  val responses =
    workflowRepository().all("QuestionnaireResponse").filterIsInstance<QuestionnaireResponse>()

  if (resource.startsWith("records/")) {
    return responses.filter { it.matchesCaseRecordResource(resource) }
  }

  return responses.filter { it.matchesRecordResource(resource) }
}

private fun workflowRepository(): FhirRepository = resolveFhirRepository()

private fun QuestionnaireResponse.withIdIfMissing(): QuestionnaireResponse =
  if (id.isNullOrBlank()) copy(id = generateUuid()) else this

private fun QuestionnaireResponse.matchesRecordResource(resource: String): Boolean =
  WorkflowCasePresentationRegistry.matchesRecordResource(
    resource = resource,
    questionnaireResource = questionnaireReferenceValue()?.toQuestionnaireResource(),
    questionnaireReference = questionnaireReferenceValue(),
  )

private fun QuestionnaireResponse.matchesCaseRecordResource(resource: String): Boolean {
  val responseJson =
    workflowResponseJson.encodeToJsonElement(QuestionnaireResponse.serializer(), this).jsonObject
  return WorkflowCasePresentationRegistry.matchesRecordResponse(
    resource = resource,
    questionnaireResource = questionnaireReferenceValue()?.toQuestionnaireResource(),
    questionnaireReference = questionnaireReferenceValue(),
    answeredLinkIds = responseJson.answeredLinkIds(),
  )
}

private fun QuestionnaireResponse.authoredText(): String? =
  workflowResponseJson
    .encodeToJsonElement(QuestionnaireResponse.serializer(), this)
    .jsonObject
    .authoredText()

private fun QuestionnaireResponse.questionnaireReferenceValue(): String? =
  workflowResponseJson
    .encodeToJsonElement(QuestionnaireResponse.serializer(), this)
    .jsonObject["questionnaire"]
    ?.jsonPrimitive
    ?.contentOrNull

private fun JsonObject.responseItems(): JsonArray =
  this["item"]?.jsonArray ?: JsonArray(emptyList())

private fun JsonObject.authoredText(): String? = this["authored"]?.jsonPrimitive?.contentOrNull

private fun JsonObject.answeredLinkIds(): Set<String> {
  val linkIds = linkedSetOf<String>()

  fun collect(items: JsonArray) {
    for (element in items) {
      val item = element.jsonObject
      val linkId = item["linkId"]?.jsonPrimitive?.contentOrNull
      if (!linkId.isNullOrBlank() && item["answer"]?.jsonArray?.isNotEmpty() == true) {
        linkIds += linkId
      }
      collect(item["item"]?.jsonArray ?: JsonArray(emptyList()))
    }
  }

  collect(responseItems())
  return linkIds
}

private fun JsonObject.questionnaireLabel(): String? =
  this["questionnaire"]?.jsonPrimitive?.contentOrNull?.substringAfterLast('/')?.toWorkflowTitle()

private fun JsonArray.collectWorkflowFields(target: MutableList<WorkflowRecordField>) {
  for (element in this) {
    val item = element.jsonObject
    val label =
      item["text"]?.jsonPrimitive?.contentOrNull
        ?: item["linkId"]?.jsonPrimitive?.contentOrNull
        ?: "Answer"
    val value = item["answer"]?.jsonArray?.firstOrNull()?.toAnswerValue()
    if (!value.isNullOrBlank()) {
      target += WorkflowRecordField(label = label, value = value)
    }

    item["item"]?.jsonArray?.collectWorkflowFields(target)
  }
}

private fun JsonElement.toAnswerValue(): String? {
  val answer = jsonObject

  answer["valueCoding"]?.jsonObject?.let { coding ->
    return coding["display"]?.jsonPrimitive?.contentOrNull
      ?: coding["code"]?.jsonPrimitive?.contentOrNull
  }

  answer["valueQuantity"]?.jsonObject?.let { quantity ->
    val value = quantity["value"]?.jsonPrimitive?.contentOrNull
    val unit = quantity["unit"]?.jsonPrimitive?.contentOrNull
    return listOfNotNull(value, unit).joinToString(" ").ifBlank { null }
  }

  answer["valueReference"]?.jsonObject?.let { reference ->
    return reference["display"]?.jsonPrimitive?.contentOrNull
      ?: reference["reference"]?.jsonPrimitive?.contentOrNull
  }

  val scalarKeys =
    listOf(
      "valueString",
      "valueInteger",
      "valueDecimal",
      "valueDate",
      "valueDateTime",
      "valueTime",
      "valueUri",
      "valueUrl",
      "valueBoolean",
    )

  for (key in scalarKeys) {
    val value = answer[key]?.jsonPrimitive?.contentOrNull ?: continue
    return when (key) {
      "valueBoolean" -> if (value == "true") "Yes" else "No"
      else -> value
    }
  }

  return null
}

private fun preferredFieldValue(fields: List<WorkflowRecordField>, query: String): String? =
  fields
    .firstOrNull { it.label.contains(query, ignoreCase = true) }
    ?.value
    ?.takeIf(String::isNotBlank)

private fun String.toWorkflowStatusLabel(): String =
  trim()
    .replace('-', ' ')
    .replace('_', ' ')
    .split(' ')
    .filter(String::isNotBlank)
    .joinToString(" ") { token ->
      token.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }
    .ifBlank { "Saved" }

private fun String.toWorkflowStatusTone(): WorkflowRecordTone =
  when (trim().lowercase()) {
    "completed" -> WorkflowRecordTone.SUCCESS
    "in-progress",
    "draft" -> WorkflowRecordTone.INFO
    "stopped",
    "entered-in-error" -> WorkflowRecordTone.CRITICAL
    else -> WorkflowRecordTone.NEUTRAL
  }

private fun String.toWorkflowTitle(): String =
  replace('-', ' ').replace('_', ' ').split(' ').filter(String::isNotBlank).joinToString(" ") {
    token ->
    token.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
  }

internal fun String.toQuestionnaireResource(): String? =
  trim()
    .takeIf(String::isNotBlank)
    ?.substringBefore('?')
    ?.substringBefore('#')
    ?.substringAfterLast('/')
    ?.takeIf(String::isNotBlank)
    ?.let { fileName ->
      val normalizedFileName = fileName.toCanonicalQuestionnaireFileName()
      if (normalizedFileName.endsWith(".json", ignoreCase = true)) {
        "questionnaires/$normalizedFileName"
      } else {
        "questionnaires/$normalizedFileName.json"
      }
    }

private fun String.toCanonicalQuestionnaireFileName(): String =
  when (trim().lowercase().removeSuffix(".json")) {
    "add-case",
    "measles-case-intake" -> "measles-case.json"
    else -> trim()
  }
