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

import dev.ohs.fhir.model.r4.Questionnaire
import dev.ohs.fhir.model.r4.QuestionnaireResponse
import dev.ohs.fhir.model.r4.Resource
import dev.ohs.player.reference.app.data.encounterIdFromReference
import dev.ohs.player.reference.app.data.patientIdFromReference
import dev.ohs.player.reference.app.data.repository.resolveFhirRepository
import dev.ohs.player.reference.app.util.FhirJson
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

private const val DEFAULT_EMPTY_CASE_MESSAGE = "No locally saved cases are available yet."
private const val DEFAULT_SUMMARY_EMPTY_MESSAGE =
  "No captured responses are available in this section yet."

private val workflowCaseJson = FhirJson.instance

internal data class WorkflowCaseDetails(
  val patientName: String,
  val title: String,
  val subtitle: String,
  val caseLabel: String,
  val epidemicNumber: String?,
  val record: WorkflowRecord,
  val highlights: List<WorkflowCaseDetailAnswer>,
  val tabs: List<WorkflowCaseDetailTab>,
)

internal data class WorkflowCaseDetailTab(
  val id: String,
  val title: String,
  val sections: List<WorkflowCaseDetailSection>,
  val emptyMessage: String,
  val action: WorkflowCaseTabAction? = null,
)

internal data class WorkflowCaseDetailSection(
  val id: String,
  val title: String,
  val answers: List<WorkflowCaseDetailAnswer>,
)

internal data class WorkflowCaseDetailAnswer(val label: String, val value: String)

internal data class WorkflowCaseContext(
  val response: QuestionnaireResponse,
  val responseJson: JsonObject,
  val responseFields: List<WorkflowRecordField>,
  val answersByLinkId: Map<String, List<JsonObject>>,
  val patient: JsonObject?,
  val references: WorkflowRecordReferences,
  val observationIndex: ObservationIndex,
  val childObservationIndex: ObservationIndex,
  val latestLabResultsResponse: WorkflowLinkedQuestionnaireResponse? = null,
  val labResultsQuestionnaireSections: List<WorkflowCaseDetailSection> = emptyList(),
)

internal data class WorkflowLinkedQuestionnaireResponse(
  val responseJson: JsonObject,
  val answersByLinkId: Map<String, List<JsonObject>>,
  val authoredText: String?,
)

private data class WorkflowCaseFieldDefinition(
  val label: String,
  val aliases: List<String> = listOf(label),
  val observationKeys: List<String> = aliases,
  val answerLinkIds: List<String> = emptyList(),
)

internal data class WorkflowCaseSupplementalTab(
  val id: String,
  val title: String,
  val emptyMessage: String,
  val sectionBuilder: ((WorkflowCaseContext) -> List<WorkflowCaseDetailSection>)? = null,
  val actionBuilder: ((WorkflowCaseContext) -> WorkflowCaseTabAction?)? = null,
)

internal data class WorkflowCasePresentationSpec(
  val recordResource: String? = null,
  val identifierSystem: String? = null,
  val questionnaireResources: Set<String> = emptySet(),
  val questionnaireAliases: Set<String> = emptySet(),
  val questionnaireKeywords: Set<String> = emptySet(),
  val indicatorLinkIds: Set<String> = emptySet(),
  val emptyMessage: String = DEFAULT_EMPTY_CASE_MESSAGE,
  val defaultFieldValues: Map<String, String> = emptyMap(),
  val supplementalTabs: List<WorkflowCaseSupplementalTab> = emptyList(),
)

internal object WorkflowCasePresentationRegistry {
  private val specs =
    listOf(
      WorkflowCasePresentationSpec(
        recordResource = "records/measles-cases.json",
        identifierSystem = "measles-case-information",
        questionnaireResources = setOf("questionnaires/measles-case.json"),
        questionnaireAliases =
          setOf(
            "questionnaires/add-case.json",
            "questionnaires/measles-case-intake.json",
            "add-case",
            "measles-case-intake",
          ),
        questionnaireKeywords = setOf("measles-case", "add-case", "measles-case-intake"),
        indicatorLinkIds =
          setOf(
            "929966324957",
            "257830485990",
            "554231819382",
            "483042281962",
            "728034137219",
            "576528567552",
            "517772812375",
            "308128177300",
          ),
        emptyMessage = "No locally saved measles cases are available yet.",
        defaultFieldValues =
          linkedMapOf("Lab Results" to "Pending Results", "Final Classification" to "Pending"),
        supplementalTabs =
          listOf(
            WorkflowCaseSupplementalTab(
              id = "lab-results",
              title = "Lab Results",
              emptyMessage = "No lab results have been recorded for this case yet.",
              sectionBuilder = WorkflowCaseContext::buildLabResultsSections,
              actionBuilder = WorkflowCaseContext::buildLabResultsQuestionnaireAction,
            )
          ),
      ),
      WorkflowCasePresentationSpec(
        recordResource = "records/afp-cases.json",
        identifierSystem = "afp-case-information",
        questionnaireResources = setOf("questionnaires/afp-case.json"),
        questionnaireKeywords = setOf("afp-case"),
        indicatorLinkIds =
          setOf(
            "929966324957",
            "257830485990",
            "728034137219",
            "776980947995",
            "679475123276",
            "970455623029",
          ),
        emptyMessage = "No locally saved AFP cases are available yet.",
        defaultFieldValues = linkedMapOf("Outcome" to "Pending"),
      ),
      WorkflowCasePresentationSpec(
        questionnaireResources = setOf(MPOX_SUPERVISORY_CHECKLIST_RESOURCE),
        questionnaireKeywords = setOf("mpox", "supervisory", "checklist"),
        emptyMessage = "No locally saved supervisory checklists are available yet.",
      ),
      // Social Forms — both categories (County/Sub County and Community) are dependent
      // sub-forms of the same Social Investigation Form, not independent workflows, so there is
      // no separate per-category record list: both are matched by this single spec and
      // distinguished per-record via the "Category" field (see
      // WorkflowCaseContext.buildRecordFields / socialInvestigationCategoryFor).
      WorkflowCasePresentationSpec(
        recordResource = SOCIAL_INVESTIGATION_COMBINED_RECORD_RESOURCE,
        questionnaireResources =
          setOf(
            SOCIAL_COUNTY_SUB_COUNTY_QUESTIONNAIRE_RESOURCE,
            SOCIAL_COMMUNITY_QUESTIONNAIRE_RESOURCE,
          ),
        questionnaireKeywords =
          setOf("social-county-sub", "social-community", "social-investigation"),
        emptyMessage = "No locally saved social investigation forms are available yet.",
      ),
    )

  fun matchesRecordResource(
    resource: String,
    questionnaireResource: String?,
    questionnaireReference: String?,
  ): Boolean {
    val spec =
      if (resource.startsWith("records/")) {
        specForRecordResource(resource)
      } else {
        specForQuestionnaire(resource, resource)
      }

    return spec.matchesQuestionnaire(questionnaireResource, questionnaireReference)
  }

  internal fun specForRecordResource(resource: String): WorkflowCasePresentationSpec =
    specs.firstOrNull { it.recordResource == resource } ?: derivedSpec(recordResource = resource)

  internal fun specForQuestionnaire(
    questionnaireResource: String?,
    questionnaireReference: String?,
  ): WorkflowCasePresentationSpec =
    specs.firstOrNull { it.matchesQuestionnaire(questionnaireResource, questionnaireReference) }
      ?: derivedSpec(
        questionnaireResource = questionnaireResource,
        questionnaireReference = questionnaireReference,
      )

  internal fun specForResponse(
    questionnaireResource: String?,
    questionnaireReference: String?,
    answersByLinkId: Map<String, List<JsonObject>>,
  ): WorkflowCasePresentationSpec =
    specs.firstOrNull { it.matchesQuestionnaire(questionnaireResource, questionnaireReference) }
      ?: if (!questionnaireResource.isNullOrBlank() || !questionnaireReference.isNullOrBlank()) {
        derivedSpec(
          questionnaireResource = questionnaireResource,
          questionnaireReference = questionnaireReference,
        )
      } else {
        specs
          .maxByOrNull { spec -> spec.indicatorMatchCount(answersByLinkId) }
          ?.takeIf { spec -> spec.indicatorMatchCount(answersByLinkId) > 0 }
          ?: derivedSpec(
            questionnaireResource = questionnaireResource,
            questionnaireReference = questionnaireReference,
          )
      }

  internal fun matchesRecordResponse(
    resource: String,
    questionnaireResource: String?,
    questionnaireReference: String?,
    answeredLinkIds: Set<String>,
  ): Boolean {
    val spec = specForRecordResource(resource)
    return spec.matchesQuestionnaire(questionnaireResource, questionnaireReference) ||
      spec.indicatorMatchCount(answeredLinkIds) > 0
  }
}

private fun WorkflowCasePresentationSpec.matchesQuestionnaire(
  questionnaireResource: String?,
  questionnaireReference: String?,
): Boolean {
  val candidates =
    listOfNotNull(questionnaireResource, questionnaireReference).map { it.normalizeCaseKey() }

  if (questionnaireResources.isNotEmpty()) {
    val normalizedResources =
      (questionnaireResources + questionnaireAliases).map(String::normalizeCaseKey)
    return if (
      normalizedResources.any { expected ->
        candidates.any { candidate -> candidate == expected || candidate.endsWith(expected) }
      }
    ) {
      true
    } else {
      false
    }
  }

  return questionnaireKeywords.isNotEmpty() &&
    candidates.any { candidate ->
      questionnaireKeywords.any { keyword -> candidate.contains(keyword, ignoreCase = true) }
    }
}

private fun WorkflowCasePresentationSpec.indicatorMatchCount(
  answersByLinkId: Map<String, List<JsonObject>>
): Int = indicatorMatchCount(answersByLinkId.keys)

private fun WorkflowCasePresentationSpec.indicatorMatchCount(answeredLinkIds: Set<String>): Int =
  indicatorLinkIds.count { linkId -> linkId in answeredLinkIds }

private fun WorkflowCasePresentationSpec.resolveQuestionnaireResource(
  questionnaireResource: String?
): String? {
  if (questionnaireResources.isEmpty()) {
    return questionnaireResource
  }

  val normalizedCandidate = questionnaireResource?.normalizeCaseKey()
  val supportedResources = questionnaireResources + questionnaireAliases
  val matchesCurrentSpec =
    normalizedCandidate != null &&
      supportedResources.any { supported ->
        val normalizedSupported = supported.normalizeCaseKey()
        normalizedCandidate == normalizedSupported ||
          normalizedCandidate.endsWith(normalizedSupported)
      }

  return if (matchesCurrentSpec || questionnaireResource.isNullOrBlank()) {
    questionnaireResources.firstOrNull()
  } else {
    questionnaireResource
  }
}

internal suspend fun loadWorkflowCaseRecordCollection(
  resource: String,
  title: String,
  subtitle: String,
): WorkflowRecordCollection {
  val spec = WorkflowCasePresentationRegistry.specForRecordResource(resource)
  val records =
    loadWorkflowCaseContexts(recordResource = resource).map { it.toWorkflowRecord(spec) }

  return WorkflowRecordCollection(
    id = resource.collectionId(),
    title = title,
    subtitle = subtitle.ifBlank { "Locally saved case records" },
    emptyMessage = spec.emptyMessage,
    pageSize = 10,
    records = records,
  )
}

internal suspend fun loadWorkflowCaseDetails(
  questionnaireResponseId: String
): WorkflowCaseDetails? {
  val context =
    loadWorkflowCaseContexts(questionnaireResponseId = questionnaireResponseId).firstOrNull()
      ?: return null
  val contextWithLabResults =
    context.copy(
      labResultsQuestionnaireSections =
        context.latestLabResultsResponse
          ?.let { labResponse ->
            val questionnaire =
              WorkflowQuestionnaireStore.questionnaire(MEASLES_LAB_RESULTS_RESOURCE)
            val questionnaireJson =
              workflowCaseJson
                .encodeToJsonElement(Questionnaire.serializer(), questionnaire)
                .jsonObject
            buildLabResultsQuestionnaireSections(questionnaireJson, labResponse)
          }
          .orEmpty()
    )
  val spec =
    WorkflowCasePresentationRegistry.specForResponse(
      questionnaireResource = contextWithLabResults.references.questionnaireResource,
      questionnaireReference = contextWithLabResults.response.questionnaireReferenceValue(),
      answersByLinkId = contextWithLabResults.answersByLinkId,
    )
  val record = contextWithLabResults.toWorkflowRecord(spec)
  val questionnaireResource =
    spec.resolveQuestionnaireResource(contextWithLabResults.references.questionnaireResource)
      ?: spec.questionnaireResources.firstOrNull()
  val questionnaireDescriptor =
    questionnaireResource?.let { resource ->
      val questionnaire = WorkflowQuestionnaireStore.questionnaire(resource)
      val questionnaireJson =
        workflowCaseJson.encodeToJsonElement(Questionnaire.serializer(), questionnaire).jsonObject
      WorkflowQuestionnaireDescriptor(
        title = questionnaireJson["title"]?.jsonPrimitive?.contentOrNull,
        tabs = questionnaireJson.buildCaseDetailTabs(contextWithLabResults.answersByLinkId),
      )
    }
  val patientName = contextWithLabResults.mappedPatientName()
  val epidemicNumber =
    record.fields.valueForAliases("EPID No", "EPID No.", "EPID Number", "Case ID", "Case Number")
  val caseLabel = questionnaireDescriptor?.title?.toCaseHeaderLabel().orEmpty()

  return WorkflowCaseDetails(
    patientName = patientName.orEmpty(),
    title = record.title,
    subtitle = record.fields.summarySubtitle(),
    caseLabel = caseLabel,
    epidemicNumber = epidemicNumber,
    record = record,
    highlights = record.summaryHighlights(spec),
    tabs =
      questionnaireDescriptor?.tabs.orEmpty() +
        spec.supplementalTabs.map { it.toDetailTab(contextWithLabResults) },
  )
}

internal suspend fun loadWorkflowCaseTabAction(
  questionnaireResponseId: String,
  tabId: String,
): WorkflowCaseTabAction? =
  loadWorkflowCaseDetails(questionnaireResponseId)
    ?.tabs
    ?.firstOrNull { tab -> tab.id == tabId }
    ?.action

private suspend fun loadWorkflowCaseContexts(
  recordResource: String? = null,
  questionnaireResponseId: String? = null,
): List<WorkflowCaseContext> {
  val repository = resolveFhirRepository()
  val allResponses =
    repository.all("QuestionnaireResponse").filterIsInstance<QuestionnaireResponse>()
  val responses =
    when {
      !questionnaireResponseId.isNullOrBlank() ->
        allResponses.filter { it.id == questionnaireResponseId }

      !recordResource.isNullOrBlank() -> {
        val spec = WorkflowCasePresentationRegistry.specForRecordResource(recordResource)
        val identifierSystem = spec.identifierSystem

        if (!identifierSystem.isNullOrBlank()) {
          val patientIds = patientIdsForIdentifierSystem(identifierSystem)
          allResponses.filter { response ->
            val patientId = patientIdFromReference(response.subject?.reference?.value)
            patientId != null &&
              patientId in patientIds &&
              !response.isFollowUpQuestionnaireResponse()
          }
        } else {
          allResponses.filter { response ->
            val responseJson =
              workflowCaseJson
                .encodeToJsonElement(QuestionnaireResponse.serializer(), response)
                .jsonObject
            WorkflowCasePresentationRegistry.matchesRecordResponse(
              resource = recordResource,
              questionnaireResource =
                response.questionnaireReferenceValue()?.toQuestionnaireResource(),
              questionnaireReference = response.questionnaireReferenceValue(),
              answeredLinkIds = responseJson.answersByLinkId().keys,
            )
          }
        }
      }

      else -> allResponses
    }

  if (responses.isEmpty()) {
    return emptyList()
  }

  val patientsById =
    repository.all("Patient").associateNotNullBy({ it.id }, { resource -> resource.asJsonObject() })

  val observationsByEncounterId =
    repository
      .all("Observation")
      .map { resource -> resource.asJsonObject() to resource }
      .mapNotNull { (resourceJson, resource) ->
        encounterIdFromReference(resourceJson.referenceValue("encounter"))?.let { it to resource }
      }
      .groupBy(keySelector = { it.first }, valueTransform = { it.second })

  val observationsByPatientId =
    repository
      .all("Observation")
      .map { resource -> resource.asJsonObject() to resource }
      .mapNotNull { (resourceJson, resource) ->
        patientIdFromReference(resourceJson.referenceValue("subject"))?.let { it to resource }
      }
      .groupBy(keySelector = { it.first }, valueTransform = { it.second })

  val childEncounterIdsByParentEncounterId =
    repository
      .all("Encounter")
      .map { resource -> resource.asJsonObject() }
      .mapNotNull { encounterJson ->
        val encounterId = encounterJson["id"]?.jsonPrimitive?.contentOrNull
        val parentEncounterId = encounterIdFromReference(encounterJson.referenceValue("partOf"))
        if (encounterId.isNullOrBlank() || parentEncounterId.isNullOrBlank()) {
          null
        } else {
          parentEncounterId to encounterId
        }
      }
      .groupBy(keySelector = { it.first }, valueTransform = { it.second })

  return responses
    .map { response ->
      val responseJson =
        workflowCaseJson
          .encodeToJsonElement(QuestionnaireResponse.serializer(), response)
          .jsonObject
      val responseFields = responseJson.collectWorkflowFields()
      val answersByLinkId = responseJson.answersByLinkId()
      val patientId = patientIdFromReference(responseJson.referenceValue("subject"))
      val encounterId = encounterIdFromReference(responseJson.referenceValue("encounter"))
      val questionnaireResource = response.questionnaireReferenceValue()?.toQuestionnaireResource()
      val references =
        WorkflowRecordReferences(
          questionnaireResponseId = response.id,
          patientId = patientId,
          encounterId = encounterId,
          questionnaireResource = questionnaireResource,
          recordResource = recordResource,
        )
      val childEncounterIds = childEncounterIdsByParentEncounterId[encounterId].orEmpty()
      val childEncounterObservations =
        childEncounterIds.flatMap { childEncounterId ->
          observationsByEncounterId[childEncounterId].orEmpty()
        }
      val latestChildResponse =
        allResponses
          .filter { childResponse ->
            childResponse.questionnaireReferenceValue()?.toQuestionnaireResource() ==
              MEASLES_LAB_RESULTS_RESOURCE &&
              encounterIdFromReference(childResponse.encounter?.reference?.value) in
                childEncounterIds
          }
          .maxByOrNull { childResponse ->
            workflowCaseJson
              .encodeToJsonElement(QuestionnaireResponse.serializer(), childResponse)
              .jsonObject
              .authoredText()
              .orEmpty()
          }
      val latestLabResultsResponse =
        latestChildResponse?.let { childResponse ->
          val childResponseJson =
            workflowCaseJson
              .encodeToJsonElement(QuestionnaireResponse.serializer(), childResponse)
              .jsonObject
          WorkflowLinkedQuestionnaireResponse(
            responseJson = childResponseJson,
            answersByLinkId = childResponseJson.answersByLinkId(),
            authoredText = childResponseJson.authoredText(),
          )
        }
      val latestChildEncounterObservations =
        encounterIdFromReference(latestChildResponse?.encounter?.reference?.value)
          ?.let { observationsByEncounterId[it] }
          .orEmpty()
      val encounterLinkedObservations = buildList {
        encounterId?.let { addAll(observationsByEncounterId[it].orEmpty()) }
        addAll(childEncounterObservations)
      }
      val linkedObservations =
        encounterLinkedObservations.ifEmpty {
          patientId?.let { observationsByPatientId[it] }.orEmpty()
        }

      WorkflowCaseContext(
        response = response,
        responseJson = responseJson,
        responseFields = responseFields,
        answersByLinkId = answersByLinkId,
        patient = patientId?.let(patientsById::get),
        references = references,
        observationIndex = linkedObservations.toObservationIndex(),
        childObservationIndex = latestChildEncounterObservations.toObservationIndex(),
        latestLabResultsResponse = latestLabResultsResponse,
      )
    }
    .sortedByDescending { it.responseJson.authoredText() }
}

private fun WorkflowCaseContext.toWorkflowRecord(
  spec: WorkflowCasePresentationSpec
): WorkflowRecord {
  val title = resolveCaseTitle()
  val authored = responseJson.authoredText()
  val questionnaireReference = response.questionnaireReferenceValue()
  val statusRaw = responseJson["status"]?.jsonPrimitive?.contentOrNull.orEmpty()
  val fields = buildRecordFields(spec = spec, title = title)
  val identifier =
    fields.valueForAliases("EPID No", "Case ID", "Case Number", "Identifier", "Record ID")

  return WorkflowRecord(
    id = references.questionnaireResponseId ?: response.id ?: title.normalizeCaseKey(),
    title = title,
    subtitle =
      listOfNotNull(identifier?.takeUnless { it == "—" }, authored?.takeIf(String::isNotBlank))
        .joinToString(" • "),
    status = statusRaw.toCaseStatusLabel(),
    statusTone = statusRaw.toCaseStatusTone(),
    meta =
      listOfNotNull(
        authored?.takeIf(String::isNotBlank)?.let { "Authored: $it" },
        questionnaireReference?.substringAfterLast('/')?.takeIf(String::isNotBlank)?.let {
          "Questionnaire: $it"
        },
      ),
    fields = fields,
    references = references.copy(recordResource = references.recordResource ?: spec.recordResource),
  )
}

private fun WorkflowCaseContext.resolveCaseTitle(): String =
  resolveSocialInvestigationTitle()
    ?: patient.patientName()
    ?: answerValue(*NAME_FIELD_LINK_IDS.toTypedArray())
    ?: responseFields.valueForAliases("Patient Name", "Name", "Case Name", "Client Name")
    ?: response.questionnaireReferenceValue()?.substringAfterLast('/')?.toDisplayTitle()?.let {
      "$it Case"
    }
    ?: "Submitted Case"

/**
 * County/Sub County and Community questionnaires don't have a "patient name" — they're
 * facility/community assessments. Title them by location instead (Village for Community, Sub
 * County/County for County/Sub County), reusing the shared reporting-site linkIds so this works
 * even before/without the questionnaire's template-extract Observations are indexed.
 */
private fun WorkflowCaseContext.resolveSocialInvestigationTitle(): String? {
  val category = socialInvestigationCategoryFor(references.questionnaireResource) ?: return null
  val county =
    answerValue(
      "294367770999_national",
      "294367770999_county",
      "294367770999_sub_county",
      "294367770999",
    )
  val subCounty =
    answerValue(
      "819946803642_national",
      "819946803642_county",
      "819946803642_sub_county",
      "819946803642",
    )
  val village = answerValue("village")

  return when (category) {
    SOCIAL_CATEGORY_COMMUNITY ->
      (village ?: subCounty ?: county)?.let { "$it Community Investigation" }
        ?: "Community Investigation"
    else -> (subCounty ?: county)?.let { "$it Assessment" } ?: "County/Sub County Assessment"
  }
}

private fun WorkflowCaseContext.buildRecordFields(
  spec: WorkflowCasePresentationSpec,
  title: String,
): List<WorkflowRecordField> {
  val normalizedUsedLabels = mutableSetOf<String>()
  val fields = mutableListOf<WorkflowRecordField>()

  fields += WorkflowRecordField(label = "Name", value = title)
  normalizedUsedLabels += "Name".normalizeCaseKey()

  socialInvestigationCategoryFor(references.questionnaireResource)?.let { category ->
    fields += WorkflowRecordField(label = "Category", value = category)
    normalizedUsedLabels += "Category".normalizeCaseKey()
  }

  STANDARD_CASE_FIELDS.forEach { definition ->
    val value = resolveFieldValue(definition) ?: return@forEach
    val normalizedLabel = definition.label.normalizeCaseKey()
    if (normalizedLabel in normalizedUsedLabels) {
      return@forEach
    }
    fields += WorkflowRecordField(label = definition.label, value = value)
    normalizedUsedLabels += normalizedLabel
  }

  responseFields
    .filter { it.shouldExposeInRecordList() }
    .forEach { field ->
      val normalizedLabel = field.label.normalizeCaseKey()
      if (normalizedLabel in normalizedUsedLabels) {
        return@forEach
      }
      fields += field
      normalizedUsedLabels += normalizedLabel
    }

  spec.defaultFieldValues.forEach { (label, fallbackValue) ->
    val existingIndex = fields.indexOfFirst { field -> field.label.matchesAnyLabel(label) }
    if (existingIndex >= 0) {
      val existingField = fields[existingIndex]
      if (existingField.value.isBlank()) {
        fields[existingIndex] = existingField.copy(value = fallbackValue)
      }
    } else {
      fields += WorkflowRecordField(label = label, value = fallbackValue)
    }
  }

  return fields
}

private fun WorkflowCaseContext.resolveFieldValue(
  definition: WorkflowCaseFieldDefinition
): String? =
  resolveDerivedFieldValue(definition)
    ?: observationIndex.value(*definition.observationKeys.toTypedArray())
    ?: answerValue(*definition.answerLinkIds.toTypedArray())
    ?: responseFields.valueForAliases(*definition.aliases.toTypedArray())
    ?: if (definition.label.matchesAnyLabel("Lab Results", "Lab Result")) {
      responseFields.valueForAliases("Results", "Result")
    } else {
      null
    }

private fun WorkflowCaseContext.resolveDerivedFieldValue(
  definition: WorkflowCaseFieldDefinition
): String? =
  when {
    definition.label.matchesAnyLabel("Lab Results", "Lab Result") -> resolveChildLabResult()
    definition.label.matchesAnyLabel("Final Classification", "Classification") ->
      resolveChildFinalClassification()
    else -> null
  }

private fun WorkflowCaseContext.resolveChildLabResult(): String? =
  childObservationIndex.value(MEASLES_IGM_CODE, "Measles IgM results")

private fun WorkflowCaseContext.resolveChildFinalClassification(): String? =
  deriveLabFinalClassification(resolveChildLabResult())?.display
    ?: childObservationIndex.value(
      FINAL_CLASSIFICATION_CODE,
      "Final Classification. Case classification after investigation (e.g., confirmed, compatible, discarded)",
    )

private fun WorkflowCaseContext.mappedPatientName(): String? =
  patient.patientName()
    ?: answerValue(*NAME_FIELD_LINK_IDS.toTypedArray())
    ?: responseFields.valueForAliases("Patient Name", "Case Name", "Client Name")

private fun WorkflowCaseContext.buildLabResultsQuestionnaireAction(): WorkflowCaseTabAction? {
  val patientId = references.patientId ?: return null
  val parentEncounterId = references.encounterId ?: return null
  val initialValues = buildMap {
    put("patient_id", workflowStringInitialValue(patientId))
    put("encounter_id", workflowStringInitialValue(parentEncounterId))
    resolveSpecimenSentToLabDate()?.let { dateValue ->
      put("718251724172", workflowDateInitialValue(dateValue))
    }
  }

  return WorkflowCaseTabAction.Questionnaire(
    title = "Lab Results",
    subtitle = "Capture laboratory follow-up details for this case.",
    resource = "questionnaires/measles-lab-results.json",
    primaryActionLabel = "Save Lab Results",
    launchContext =
      WorkflowQuestionnaireLaunchContext(
        patientId = patientId,
        parentEncounterId = parentEncounterId,
        initialValues = initialValues,
      ),
  )
}

private fun WorkflowCaseContext.buildLabResultsSections(): List<WorkflowCaseDetailSection> {
  val summaryAnswers =
    LAB_RESULTS_SUMMARY_FIELDS.mapNotNull { definition ->
      resolveFieldValue(definition)?.takeIf(String::isNotBlank)?.let {
        WorkflowCaseDetailAnswer(label = definition.label, value = it)
      }
    }

  val sections = mutableListOf<WorkflowCaseDetailSection>()

  if (summaryAnswers.isNotEmpty()) {
    sections +=
      WorkflowCaseDetailSection(
        id = "lab-results-summary",
        title = "Captured laboratory details",
        answers = summaryAnswers,
      )
  }

  sections += labResultsQuestionnaireSections
  return sections
}

private fun buildLabResultsQuestionnaireSections(
  questionnaireJson: JsonObject,
  labResponse: WorkflowLinkedQuestionnaireResponse,
): List<WorkflowCaseDetailSection> =
  questionnaireJson.buildSections(
    answersByLinkId = labResponse.answersByLinkId,
    alwaysIncludeLinkIds = setOf("718251724172", FINAL_CLASSIFICATION_CODE),
    excludedLinkIds = setOf("patient_id", "encounter_id"),
  )

private fun WorkflowCaseContext.resolveSpecimenSentToLabDate(): String? =
  observationIndex.value("718251724172", "Date specimen sent to lab")
    ?: answerValue("718251724172")
    ?: responseFields.valueForAliases("Date specimen sent to lab")

private fun WorkflowRecord.summaryHighlights(
  spec: WorkflowCasePresentationSpec
): List<WorkflowCaseDetailAnswer> {
  val priorityGroups = buildList {
    add(listOf("Category"))
    spec.defaultFieldValues.keys.forEach { add(listOf(it)) }
    add(listOf("Outcome"))
    add(listOf("Status"))
    add(listOf("County", "District"))
    add(listOf("Sub County", "Sub-county", "Subcounty"))
    add(listOf("Reporting Facility", "Facility", "Health Facility"))
  }

  val highlights =
    priorityGroups
      .mapNotNull { aliases ->
        fields
          .firstOrNull { field -> aliases.any { alias -> field.label.matchesAnyLabel(alias) } }
          ?.takeIf { it.value.isNotBlank() }
      }
      .distinctBy { it.label.normalizeCaseKey() }
      .take(2)

  if (highlights.isNotEmpty()) {
    return highlights.map { field ->
      WorkflowCaseDetailAnswer(label = field.label.removeSuffix(":"), value = field.value)
    }
  }

  return fields
    .filter { it.shouldExposeInRecordList() }
    .take(2)
    .map { field ->
      WorkflowCaseDetailAnswer(label = field.label.removeSuffix(":"), value = field.value)
    }
}

private fun JsonObject.buildCaseDetailTabs(
  answersByLinkId: Map<String, List<JsonObject>>
): List<WorkflowCaseDetailTab> =
  itemArray().mapNotNull { element ->
    val item = element.jsonObject
    if (item.isHidden() || !item.isVisible(answersByLinkId)) {
      return@mapNotNull null
    }

    WorkflowCaseDetailTab(
      id = item.linkId(),
      title = item.displayTitle(),
      sections = item.buildSections(answersByLinkId),
      emptyMessage = DEFAULT_SUMMARY_EMPTY_MESSAGE,
    )
  }

private fun JsonObject.buildSections(
  answersByLinkId: Map<String, List<JsonObject>>,
  alwaysIncludeLinkIds: Set<String> = emptySet(),
  excludedLinkIds: Set<String> = emptySet(),
): List<WorkflowCaseDetailSection> {
  val sections = mutableListOf<WorkflowCaseDetailSection>()
  collectSections(
    answersByLinkId = answersByLinkId,
    target = sections,
    isTopLevel = true,
    alwaysIncludeLinkIds = alwaysIncludeLinkIds,
    excludedLinkIds = excludedLinkIds,
  )
  return sections
}

private fun JsonObject.collectSections(
  answersByLinkId: Map<String, List<JsonObject>>,
  target: MutableList<WorkflowCaseDetailSection>,
  isTopLevel: Boolean = false,
  alwaysIncludeLinkIds: Set<String> = emptySet(),
  excludedLinkIds: Set<String> = emptySet(),
) {
  if (
    shouldHide(alwaysIncludeLinkIds = alwaysIncludeLinkIds, excludedLinkIds = excludedLinkIds) ||
      !isVisible(answersByLinkId)
  ) {
    return
  }

  val directAnswers =
    itemArray()
      .map { it.jsonObject }
      .filter { child -> child.itemType() != "group" }
      .mapNotNull { child ->
        child.toDetailAnswer(
          answersByLinkId = answersByLinkId,
          alwaysIncludeLinkIds = alwaysIncludeLinkIds,
          excludedLinkIds = excludedLinkIds,
        )
      }

  if (directAnswers.isNotEmpty()) {
    target +=
      WorkflowCaseDetailSection(
        id = if (isTopLevel) "${linkId()}-summary" else linkId(),
        title = if (isTopLevel) "" else displayTitle().takeUnless(String::isBlank).orEmpty(),
        answers = directAnswers,
      )
  }

  itemArray()
    .map { it.jsonObject }
    .filter { child -> child.itemType() == "group" }
    .forEach { child ->
      child.collectSections(
        answersByLinkId = answersByLinkId,
        target = target,
        alwaysIncludeLinkIds = alwaysIncludeLinkIds,
        excludedLinkIds = excludedLinkIds,
      )
    }
}

private fun JsonObject.toDetailAnswer(
  answersByLinkId: Map<String, List<JsonObject>>,
  alwaysIncludeLinkIds: Set<String> = emptySet(),
  excludedLinkIds: Set<String> = emptySet(),
): WorkflowCaseDetailAnswer? {
  if (
    shouldHide(alwaysIncludeLinkIds = alwaysIncludeLinkIds, excludedLinkIds = excludedLinkIds) ||
      !isVisible(answersByLinkId) ||
      itemType() == "group"
  ) {
    return null
  }

  val answers =
    answersByLinkId[linkId()]
      .orEmpty()
      .mapNotNull(JsonObject::answerDisplayValue)
      .filter(String::isNotBlank)
      .distinct()

  if (answers.isEmpty()) {
    return null
  }

  return WorkflowCaseDetailAnswer(label = displayTitle(), value = answers.joinToString(", "))
}

private fun JsonObject.shouldHide(
  alwaysIncludeLinkIds: Set<String> = emptySet(),
  excludedLinkIds: Set<String> = emptySet(),
): Boolean {
  val linkId = this["linkId"]?.jsonPrimitive?.contentOrNull
  if (!linkId.isNullOrBlank() && linkId in excludedLinkIds) {
    return true
  }
  if (!linkId.isNullOrBlank() && linkId in alwaysIncludeLinkIds) {
    return false
  }
  return isHidden()
}

private fun JsonObject.isVisible(answersByLinkId: Map<String, List<JsonObject>>): Boolean {
  val conditions = this["enableWhen"]?.jsonArray.orEmpty()
  if (conditions.isEmpty()) {
    return true
  }

  val behavior = this["enableBehavior"]?.jsonPrimitive?.contentOrNull ?: "all"
  val results =
    conditions.map { condition -> condition.jsonObject.matchesCondition(answersByLinkId) }
  return if (behavior.equals("any", ignoreCase = true)) results.any { it } else results.all { it }
}

private fun JsonObject.matchesCondition(answersByLinkId: Map<String, List<JsonObject>>): Boolean {
  val targetLinkId =
    this["question"]?.jsonPrimitive?.contentOrNull
      ?: this["linkId"]?.jsonPrimitive?.contentOrNull
      ?: return false
  val operator = this["operator"]?.jsonPrimitive?.contentOrNull ?: "="
  val actualAnswers = answersByLinkId[targetLinkId].orEmpty()

  return when (operator) {
    "exists" -> {
      val expected = this["answerBoolean"]?.jsonPrimitive?.contentOrNull == "true"
      actualAnswers.isNotEmpty() == expected
    }

    "=" -> actualAnswers.any { answer -> answer.matchesExpectedValue(this) }

    "!=" ->
      actualAnswers.isNotEmpty() &&
        actualAnswers.none { answer -> answer.matchesExpectedValue(this) }

    else -> false
  }
}

private fun JsonObject.matchesExpectedValue(condition: JsonObject): Boolean {
  condition["answerCoding"]?.jsonObject?.let { expected ->
    val expectedCode = expected["code"]?.jsonPrimitive?.contentOrNull
    val expectedDisplay = expected["display"]?.jsonPrimitive?.contentOrNull
    val answerCoding = this["valueCoding"]?.jsonObject
    val answerCode = answerCoding?.get("code")?.jsonPrimitive?.contentOrNull
    val answerDisplay = answerCoding?.get("display")?.jsonPrimitive?.contentOrNull

    return expectedCode.equals(answerCode, ignoreCase = true) ||
      expectedDisplay.equals(answerDisplay, ignoreCase = true) ||
      expectedDisplay.equals(answerCode, ignoreCase = true) ||
      expectedCode.equals(answerDisplay, ignoreCase = true)
  }

  condition["answerBoolean"]?.jsonPrimitive?.contentOrNull?.let { expected ->
    return this["valueBoolean"]?.jsonPrimitive?.contentOrNull == expected
  }

  condition["answerString"]?.jsonPrimitive?.contentOrNull?.let { expected ->
    return scalarValue()?.equals(expected, ignoreCase = true) == true
  }

  return false
}

private fun JsonObject.isHidden(): Boolean =
  this["extension"]?.jsonArray.orEmpty().any { extension ->
    val extensionJson = extension.jsonObject
    extensionJson["url"]?.jsonPrimitive?.contentOrNull ==
      "http://hl7.org/fhir/StructureDefinition/questionnaire-hidden" &&
      extensionJson["valueBoolean"]?.jsonPrimitive?.contentOrNull == "true"
  }

private fun JsonObject.answerDisplayValue(): String? {
  this["valueCoding"]?.jsonObject?.let { coding ->
    return coding["display"]?.jsonPrimitive?.contentOrNull
      ?: coding["code"]?.jsonPrimitive?.contentOrNull
  }

  this["valueQuantity"]?.jsonObject?.let { quantity ->
    val amount = quantity["value"]?.jsonPrimitive?.contentOrNull
    val unit = quantity["unit"]?.jsonPrimitive?.contentOrNull
    return listOfNotNull(amount, unit).joinToString(" ").ifBlank { null }
  }

  this["valueReference"]?.jsonObject?.let { reference ->
    return reference["display"]?.jsonPrimitive?.contentOrNull
      ?: reference["reference"]?.jsonPrimitive?.contentOrNull
  }

  return scalarValue()?.let { value ->
    when (value) {
      "true" -> "Yes"
      "false" -> "No"
      else -> value
    }
  }
}

private fun JsonObject.scalarValue(): String? {
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
    val value = this[key]?.jsonPrimitive?.contentOrNull ?: continue
    return value
  }

  return null
}

private fun JsonObject.collectWorkflowFields(): List<WorkflowRecordField> {
  val fields = mutableListOf<WorkflowRecordField>()
  responseItems().collectWorkflowFields(fields)
  return fields
}

private fun JsonArray.collectWorkflowFields(target: MutableList<WorkflowRecordField>) {
  for (element in this) {
    val item = element.jsonObject
    val label =
      item["text"]?.jsonPrimitive?.contentOrNull
        ?: item["linkId"]?.jsonPrimitive?.contentOrNull
        ?: "Answer"
    val value = item["answer"]?.jsonArray?.firstOrNull()?.jsonObject?.answerDisplayValue()
    if (!value.isNullOrBlank()) {
      target += WorkflowRecordField(label = label, value = value)
    }
    item["item"]?.jsonArray.orEmpty().collectWorkflowFields(target)
  }
}

private fun JsonObject.answersByLinkId(): Map<String, List<JsonObject>> {
  val answers = mutableMapOf<String, MutableList<JsonObject>>()

  fun collect(items: JsonArray) {
    for (element in items) {
      val item = element.jsonObject
      val linkId = item["linkId"]?.jsonPrimitive?.contentOrNull
      val itemAnswers = item["answer"]?.jsonArray.orEmpty().map(JsonElement::jsonObject)
      if (!linkId.isNullOrBlank() && itemAnswers.isNotEmpty()) {
        answers.getOrPut(linkId) { mutableListOf() }.addAll(itemAnswers)
      }
      collect(item["item"]?.jsonArray.orEmpty())
    }
  }

  collect(responseItems())
  return answers
}

private fun List<Resource>.toObservationIndex(): ObservationIndex {
  val valuesByKey = linkedMapOf<String, String>()

  for (resource in this) {
    val resourceJson = resource.asJsonObject()
    val value = resourceJson.observationValue() ?: continue
    resourceJson.observationKeys().forEach { key ->
      if (key !in valuesByKey) {
        valuesByKey[key] = value
      }
    }
  }

  return ObservationIndex(valuesByKey)
}

internal class ObservationIndex(private val valuesByKey: Map<String, String>) {
  fun value(vararg keys: String): String? =
    keys.firstNotNullOfOrNull { key -> valuesByKey[key.normalizeCaseKey()] }
}

private fun JsonObject.observationKeys(): List<String> = buildList {
  codeObject()?.let { code ->
    addAll(
      code["coding"]?.jsonArray.orEmpty().mapNotNull { coding ->
        coding.jsonObject["code"]?.jsonPrimitive?.contentOrNull?.normalizeCaseKey()
      }
    )
    addAll(
      code["coding"]?.jsonArray.orEmpty().mapNotNull { coding ->
        coding.jsonObject["display"]?.jsonPrimitive?.contentOrNull?.normalizeCaseKey()
      }
    )
    code["text"]?.jsonPrimitive?.contentOrNull?.normalizeCaseKey()?.let(::add)
  }
}

private fun JsonObject.observationValue(): String? =
  this["valueCodeableConcept"]?.jsonObject?.let { concept ->
    concept["coding"]?.jsonArray.orEmpty().firstNotNullOfOrNull { coding ->
      coding.jsonObject["display"]?.jsonPrimitive?.contentOrNull
        ?: coding.jsonObject["code"]?.jsonPrimitive?.contentOrNull
    }
  }
    ?: this["valueReference"]?.jsonObject?.let { reference ->
      reference["display"]?.jsonPrimitive?.contentOrNull
        ?: reference["reference"]?.jsonPrimitive?.contentOrNull
    }
    ?: this["valueQuantity"]?.jsonObject?.let { quantity ->
      val amount = quantity["value"]?.jsonPrimitive?.contentOrNull
      val unit = quantity["unit"]?.jsonPrimitive?.contentOrNull
      listOfNotNull(amount, unit).joinToString(" ").ifBlank { null }
    }
    ?: this["valueString"]?.jsonPrimitive?.contentOrNull
    ?: this["valueDate"]?.jsonPrimitive?.contentOrNull
    ?: this["valueDateTime"]?.jsonPrimitive?.contentOrNull
    ?: this["valueBoolean"]?.jsonPrimitive?.contentOrNull?.let { value ->
      if (value == "true") "Yes" else "No"
    }

private fun JsonObject.responseItems(): JsonArray = this["item"]?.jsonArray.orEmpty()

private fun JsonObject.referenceValue(field: String): String? =
  this[field]?.jsonObject?.get("reference")?.jsonPrimitive?.contentOrNull

private fun JsonObject.codeObject(): JsonObject? = this["code"]?.jsonObject

private fun JsonObject.linkId(): String =
  this["linkId"]?.jsonPrimitive?.contentOrNull?.takeIf(String::isNotBlank)
    ?: displayTitle().normalizeCaseKey()

private fun JsonObject.itemType(): String = this["type"]?.jsonPrimitive?.contentOrNull.orEmpty()

private fun JsonObject.displayTitle(): String =
  this["text"]?.jsonPrimitive?.contentOrNull?.trim()?.takeUnless { it.isBlank() }
    ?: this["linkId"]?.jsonPrimitive?.contentOrNull?.toDisplayTitle()
    ?: "Section"

private fun JsonObject.authoredText(): String? = this["authored"]?.jsonPrimitive?.contentOrNull

private fun JsonObject.itemArray(): JsonArray = this["item"]?.jsonArray.orEmpty()

private fun QuestionnaireResponse.questionnaireReferenceValue(): String? =
  workflowCaseJson
    .encodeToJsonElement(QuestionnaireResponse.serializer(), this)
    .jsonObject["questionnaire"]
    ?.jsonPrimitive
    ?.contentOrNull

private fun QuestionnaireResponse.isFollowUpQuestionnaireResponse(): Boolean =
  questionnaireReferenceValue()?.toQuestionnaireResource() == MEASLES_LAB_RESULTS_RESOURCE

private fun Resource.asJsonObject(): JsonObject =
  workflowCaseJson.encodeToJsonElement(Resource.serializer(), this).jsonObject

internal suspend fun patientIdsForIdentifierSystem(identifierSystem: String): Set<String> =
  resolveFhirRepository()
    .all("Patient")
    .filter { it.asJsonObject().hasIdentifierWithSystem(identifierSystem) }
    .mapNotNull { it.id }
    .toSet()

private fun JsonObject.hasIdentifierWithSystem(system: String): Boolean =
  this["identifier"]?.jsonArray.orEmpty().any { identifier ->
    identifier.jsonObject["system"]?.jsonPrimitive?.contentOrNull == system
  }

private fun JsonObject?.patientName(): String? {
  val patientName = this?.get("name")?.jsonArray.orEmpty().firstOrNull()?.jsonObject ?: return null
  val given =
    patientName["given"]?.jsonArray.orEmpty().mapNotNull { value ->
      value.jsonPrimitive.contentOrNull?.trim()
    }
  val family = patientName["family"]?.jsonPrimitive?.contentOrNull?.trim()

  return (given + listOfNotNull(family)).joinToString(" ").trim().ifBlank { null }
}

private fun WorkflowCaseContext.answerValue(vararg linkIds: String): String? =
  linkIds.firstNotNullOfOrNull { linkId ->
    answersByLinkId[linkId]
      .orEmpty()
      .mapNotNull(JsonObject::answerDisplayValue)
      .filter(String::isNotBlank)
      .joinToString(" ")
      .ifBlank { null }
  }

private fun List<WorkflowRecordField>.valueForAliases(vararg labels: String): String? =
  firstOrNull { field -> labels.any { label -> field.label.matchesAnyLabel(label) } }
    ?.value
    ?.takeIf(String::isNotBlank)

private fun List<WorkflowRecordField>.summarySubtitle(): String =
  buildList {
      this@summarySubtitle.valueForAliases(
          "EPID No",
          "Case ID",
          "Case Number",
          "Identifier",
          "Record ID",
        )
        ?.let { add(it) }
      this@summarySubtitle.valueForAliases("County", "District")?.let { add(it) }
      this@summarySubtitle.valueForAliases("Sub County", "Sub-county", "Subcounty")?.let { add(it) }
      this@summarySubtitle.valueForAliases("Outcome")?.let { add(it) }
    }
    .take(3)
    .joinToString(" • ")

private fun WorkflowRecordField.shouldExposeInRecordList(): Boolean {
  val normalizedLabel = label.normalizeCaseKey()
  if (value.isBlank()) {
    return false
  }

  return normalizedLabel !in INTERNAL_RECORD_FIELD_LABELS &&
    normalizedLabel !in NAME_COMPONENT_FIELD_LABELS
}

private fun String.matchesAnyLabel(vararg labels: String): Boolean =
  labels.any { label ->
    trim().removeSuffix(":").equals(label.trim().removeSuffix(":"), ignoreCase = true)
  }

private fun WorkflowCaseSupplementalTab.toDetailTab(
  context: WorkflowCaseContext
): WorkflowCaseDetailTab =
  WorkflowCaseDetailTab(
    id = id,
    title = title,
    sections = sectionBuilder?.invoke(context).orEmpty(),
    emptyMessage = emptyMessage,
    action = actionBuilder?.invoke(context),
  )

private fun derivedSpec(
  recordResource: String? = null,
  questionnaireResource: String? = null,
  questionnaireReference: String? = null,
): WorkflowCasePresentationSpec {
  val label =
    recordResource?.toRecordResourceLabel()
      ?: questionnaireResource?.substringAfterLast('/')?.substringBeforeLast('.')?.toDisplayTitle()
      ?: "case records"

  return WorkflowCasePresentationSpec(
    recordResource = recordResource,
    questionnaireResources = listOfNotNull(questionnaireResource).toSet(),
    questionnaireKeywords =
      buildWorkflowKeywords(recordResource, questionnaireResource, questionnaireReference),
    emptyMessage = "No locally saved ${label.lowercase()} are available yet.",
  )
}

private fun buildWorkflowKeywords(vararg sources: String?): Set<String> =
  sources
    .filterNotNull()
    .flatMap { source ->
      source
        .substringAfterLast('/')
        .substringBefore('?')
        .substringBefore('#')
        .substringBeforeLast('.')
        .split(Regex("[^A-Za-z0-9]+"))
        .map(String::trim)
        .filter(String::isNotBlank)
    }
    .map(String::lowercase)
    .filterNot { token -> token in IGNORED_WORKFLOW_KEYWORDS || token.all(Char::isDigit) }
    .toSet()

private fun String.collectionId(): String =
  substringAfterLast('/').substringBeforeLast('.').ifBlank { "workflow-case-records" }

private fun String.toRecordResourceLabel(): String =
  substringAfterLast('/').substringBeforeLast('.').toDisplayTitle() ?: "case records"

private fun String?.toDisplayTitle(): String? =
  this?.replace('-', ' ')
    ?.replace('_', ' ')
    ?.split(' ')
    ?.filter(String::isNotBlank)
    ?.joinToString(" ") { token ->
      token.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }
    ?.ifBlank { null }

private fun String.normalizeCaseKey(): String = trim().lowercase()

private fun String?.toCaseStatusLabel(): String =
  this.orEmpty()
    .trim()
    .replace('-', ' ')
    .replace('_', ' ')
    .split(' ')
    .filter(String::isNotBlank)
    .joinToString(" ") { token ->
      token.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }
    .ifBlank { "Saved" }

private fun String?.toCaseStatusTone(): WorkflowRecordTone =
  when (this.orEmpty().trim().lowercase()) {
    "completed" -> WorkflowRecordTone.SUCCESS
    "in-progress",
    "draft" -> WorkflowRecordTone.INFO
    "stopped",
    "entered-in-error" -> WorkflowRecordTone.CRITICAL
    else -> WorkflowRecordTone.NEUTRAL
  }

private fun String.toCaseHeaderLabel(): String {
  val normalized = trim()
  if (normalized.isBlank()) {
    return "Case Information"
  }

  return when {
    normalized.contains("information", ignoreCase = true) -> normalized
    normalized.contains("report", ignoreCase = true) ->
      normalized.replace("report", "Information", ignoreCase = true)
    else -> "$normalized Information"
  }
}

private fun <K, V : Any> Iterable<V>.associateNotNullBy(
  keySelector: (V) -> K?,
  valueTransform: (V) -> JsonObject,
): Map<K, JsonObject> = buildMap {
  for (value in this@associateNotNullBy) {
    val key = keySelector(value) ?: continue
    put(key, valueTransform(value))
  }
}

private fun JsonArray?.orEmpty(): JsonArray = this ?: JsonArray(emptyList())

private val NAME_FIELD_LINK_IDS = listOf("873240407472", "246751846436", "486402457213")

private val STANDARD_CASE_FIELDS =
  listOf(
    WorkflowCaseFieldDefinition(
      label = "EPID No",
      aliases = listOf("EPID No", "EPID No.", "EPID Number"),
      observationKeys = listOf("EPID", "EPID Number"),
      answerLinkIds = listOf("992818778559"),
    ),
    WorkflowCaseFieldDefinition(
      label = "Case ID",
      aliases = listOf("Case ID", "Case Number", "Identifier", "Record ID"),
    ),
    WorkflowCaseFieldDefinition(
      label = "County",
      aliases = listOf("County", "Reporting County"),
      observationKeys = listOf("294367770999", "Reporting County"),
      answerLinkIds = listOf("294367770999", "294367770999_sub_county", "294367770999_county"),
    ),
    WorkflowCaseFieldDefinition(
      label = "Sub County",
      aliases = listOf("Sub County", "Sub-county", "Subcounty", "Reporting Sub County"),
      observationKeys = listOf("819946803642", "Reporting Sub County"),
      answerLinkIds = listOf("819946803642", "819946803642_sub_county", "819946803642_county"),
    ),
    WorkflowCaseFieldDefinition(
      label = "Onset of illness",
      aliases = listOf("Onset of illness", "Onset Date", "Date of onset of illness"),
      observationKeys = listOf("728034137219", "Date of onset of illness"),
      answerLinkIds = listOf("728034137219"),
    ),
    WorkflowCaseFieldDefinition(
      label = "Lab Results",
      aliases = listOf("Lab Results", "Lab Result"),
      observationKeys =
        listOf("measles-igm", "Measles IgM results", "rubella-igm", "Rubella IgM results"),
    ),
    WorkflowCaseFieldDefinition(
      label = "Final Classification",
      aliases = listOf("Final Classification", "Classification"),
      observationKeys =
        listOf(
          "final-classification",
          "Final Classification. Case classification after investigation (e.g., confirmed, compatible, discarded)",
        ),
    ),
    WorkflowCaseFieldDefinition(
      label = "Outcome",
      aliases = listOf("Outcome"),
      observationKeys = listOf("508745697175", "Outcome"),
    ),
    WorkflowCaseFieldDefinition(
      label = "Reporting Facility",
      aliases = listOf("Reporting Facility", "Health Facility", "Facility"),
      observationKeys = listOf("819946803677", "Reporting Facility"),
      answerLinkIds = listOf("819946803677", "819946803677_sub_county", "819946803677_county"),
    ),
  )

private val LAB_RESULTS_SECTION_FIELDS =
  listOf(
    WorkflowCaseFieldDefinition(
      label = "Lab ID No",
      aliases = listOf("Lab ID No"),
      observationKeys = listOf("8732404074721", "Lab ID No"),
      answerLinkIds = listOf("8732404074721"),
    ),
    WorkflowCaseFieldDefinition(
      label = "Date specimen sent to lab",
      aliases = listOf("Date specimen sent to lab"),
      observationKeys = listOf("718251724172", "Date specimen sent to lab"),
      answerLinkIds = listOf("718251724172"),
    ),
    WorkflowCaseFieldDefinition(
      label = "Date specimen received in lab",
      aliases = listOf("Date specimen received in lab"),
      observationKeys = listOf("date-specimen-received", "Date specimen received in lab"),
      answerLinkIds = listOf("date-specimen-received"),
    ),
    WorkflowCaseFieldDefinition(
      label = "Specimen Condition",
      aliases = listOf("Specimen Condition"),
      observationKeys = listOf("specimen-condition", "Specimen Condition"),
      answerLinkIds = listOf("specimen-condition"),
    ),
    WorkflowCaseFieldDefinition(
      label = "Measles IgM results",
      aliases = listOf("Measles IgM results"),
      observationKeys = listOf("measles-igm", "Measles IgM results"),
      answerLinkIds = listOf("measles-igm"),
    ),
    WorkflowCaseFieldDefinition(
      label = "Rubella IgM results",
      aliases = listOf("Rubella IgM results"),
      observationKeys = listOf("rubella-igm", "Rubella IgM results"),
      answerLinkIds = listOf("rubella-igm"),
    ),
    WorkflowCaseFieldDefinition(
      label = "Date lab sent out results to EPI/Surv Unit",
      aliases = listOf("Date lab sent out results to EPI/Surv Unit"),
      observationKeys =
        listOf("date-lab-sent-results", "Date lab sent out results to EPI/Surv Unit"),
      answerLinkIds = listOf("date-lab-sent-results"),
    ),
    WorkflowCaseFieldDefinition(
      label = "Was specimen sent to regional lab?",
      aliases = listOf("Was specimen sent to regional lab?"),
      observationKeys = listOf("139172422437", "Was specimen sent to regional lab?"),
      answerLinkIds = listOf("139172422437"),
    ),
    WorkflowCaseFieldDefinition(
      label = "Date Serum specimen was sent to regional lab for IgM confirmation",
      aliases = listOf("Date Serum specimen was sent to regional lab for IgM confirmation"),
      observationKeys =
        listOf("655865451155", "Date Serum specimen was sent to regional lab for IgM confirmation"),
      answerLinkIds = listOf("655865451155"),
    ),
    WorkflowCaseFieldDefinition(
      label = "Final Classification",
      aliases =
        listOf(
          "Final Classification",
          "Final Classification. Case classification after investigation (e.g., confirmed, compatible, discarded)",
        ),
      observationKeys =
        listOf(
          "final-classification",
          "Final Classification. Case classification after investigation (e.g., confirmed, compatible, discarded)",
        ),
      answerLinkIds = listOf("final-classification"),
    ),
    WorkflowCaseFieldDefinition(
      label = "Reasons",
      aliases = listOf("Reasons"),
      observationKeys = listOf("inadequate_specimen", "Reasons"),
      answerLinkIds = listOf("inadequate_specimen"),
    ),
  )

private val LAB_RESULTS_TRIGGER_FIELDS =
  LAB_RESULTS_SECTION_FIELDS.filterNot { definition ->
    definition.label.matchesAnyLabel("Date specimen sent to lab")
  }

private val LAB_RESULTS_SUMMARY_FIELDS =
  LAB_RESULTS_SECTION_FIELDS.filter { definition ->
    definition.label.matchesAnyLabel("Measles IgM results", "Final Classification")
  }

private val INTERNAL_RECORD_FIELD_LABELS =
  setOf("user role", "user facility", "user ward", "user sub county", "user county")

private val NAME_COMPONENT_FIELD_LABELS = setOf("first name", "middle name", "surname/family name")

private val IGNORED_WORKFLOW_KEYWORDS =
  setOf(
    "record",
    "records",
    "case",
    "cases",
    "list",
    "workflow",
    "questionnaire",
    "questionnaires",
    "json",
  )

private data class WorkflowQuestionnaireDescriptor(
  val title: String?,
  val tabs: List<WorkflowCaseDetailTab>,
)
