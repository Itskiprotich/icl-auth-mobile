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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.ohs.fhir.datacapture.Questionnaire as DataCaptureQuestionnaire
import dev.ohs.fhir.datacapture.QuestionnaireConfig
import dev.ohs.fhir.datacapture.extraction.template.TemplateExtractionEngine
import dev.ohs.fhir.model.r4.Bundle
import dev.ohs.fhir.model.r4.Canonical
import dev.ohs.fhir.model.r4.Code
import dev.ohs.fhir.model.r4.CodeableConcept
import dev.ohs.fhir.model.r4.Coding
import dev.ohs.fhir.model.r4.Condition
import dev.ohs.fhir.model.r4.Encounter
import dev.ohs.fhir.model.r4.Observation
import dev.ohs.fhir.model.r4.Patient
import dev.ohs.fhir.model.r4.Questionnaire
import dev.ohs.fhir.model.r4.Reference
import dev.ohs.fhir.model.r4.Specimen
import dev.ohs.fhir.model.r4.String as FhirString
import dev.ohs.fhir.model.r4.Uri
import dev.ohs.player.reference.app.generateUuid
import dev.ohs.player.reference.app.util.FhirJson
import kotlin.time.Clock
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.todayIn
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

private const val MPOX_SUMMARY_SHEET_MEASURE_REFERENCE = "Measure/mpox-summary-sheet"
private const val MPOX_SUMMARY_SHEET_OBSERVATION_CODE_SYSTEM =
  "https://open-health-stack.org/fhir/CodeSystem/mpox-summary-sheet-observation"
private const val MPOX_SUMMARY_SHEET_SECTION_CODE_SYSTEM =
  "https://open-health-stack.org/fhir/CodeSystem/mpox-summary-sheet-section"
private const val SYSTEM_CREATION_IDENTIFIER_SYSTEM = "system-creation"

// A single, professional, non-technical confirmation shown regardless of what happens
// internally (raw response save, template extraction, etc.) — the user shouldn't see
// implementation details like "resources were extracted."
private const val SUBMISSION_SUCCESS_MESSAGE = "Your form has been saved and submitted."

private val MPOX_SUMMARY_SHEET_OBSERVATION_SECTION_LINK_IDS =
  setOf("target_population", "utilization_aefi_mpox_surveilance")

@Composable
internal fun QuestionnaireHostScreen(
  title: String,
  subtitle: String,
  resource: String,
  onBack: () -> Unit,
  modifier: Modifier = Modifier,
  primaryActionLabel: String = "Submit Case",
  launchContext: WorkflowQuestionnaireLaunchContext? = null,
) {
  val questionnaireResourcePath = resource
  val fhirJson = FhirJson.instance
  fun String?.orGeneratedId(): String = takeUnless { it.isNullOrBlank() } ?: generateUuid()
  fun String?.takeIfNotBlank(): String? = this?.trim()?.takeIf(String::isNotBlank)

  val screenState by
    produceState<QuestionnaireScreenState>(
      initialValue = QuestionnaireScreenState.Loading,
      resource,
      launchContext,
    ) {
      value =
        runCatching {
            WorkflowQuestionnaireStore.questionnaireJson(
              resource = resource,
              initialValues = launchContext?.initialValues.orEmpty(),
            )
          }
          .fold(
            onSuccess = QuestionnaireScreenState::Ready,
            onFailure = {
              QuestionnaireScreenState.Error(it.message ?: "The questionnaire could not be loaded.")
            },
          )
    }
  val snackbarHostState = remember { SnackbarHostState() }
  val scope = rememberCoroutineScope()
  var isSubmitting by remember(resource) { mutableStateOf(false) }
  var submissionSuccessMessage by remember(resource) { mutableStateOf<String?>(null) }
  var showDiscardConfirmation by remember(resource) { mutableStateOf(false) }
  fun requestBack() {
    if (isSubmitting || submissionSuccessMessage != null) {
      return
    }
    showDiscardConfirmation = true
  }
  fun resolveCqfCalculatedToday(questionnaireJson: String): String {
    val today = Clock.System.todayIn(TimeZone.currentSystemDefault()).toString() // "2026-07-14"
    val root = Json.parseToJsonElement(questionnaireJson)

    fun transform(element: JsonElement): JsonElement =
      when (element) {
        is JsonObject -> {
          val map = element.toMutableMap()
          val underscoreValueDate = map["_valueDate"] as? JsonObject
          val extensions = underscoreValueDate?.get("extension") as? JsonArray
          val hasTodayExpr =
            extensions?.any { ext ->
              val extObj = ext as? JsonObject
              extObj?.get("url")?.jsonPrimitive?.content ==
                "http://hl7.org/fhir/StructureDefinition/cqf-calculatedValue" &&
                extObj["valueExpression"]?.jsonObject?.get("expression")?.jsonPrimitive?.content in
                  setOf("today()", "now()")
            } == true

          if (hasTodayExpr) {
            map.remove("_valueDate")
            map["valueDate"] = JsonPrimitive(today)
          }
          JsonObject(map.mapValues { (_, v) -> transform(v) })
        }

        is JsonArray -> JsonArray(element.map { transform(it) })
        else -> element
      }

    return transform(root).toString()
  }

  // Intercept the hardware/gesture back press the same way as the toolbar back arrow, so
  // in-progress answers can't be lost by backing out of the screen without confirming.
  BackHandler { requestBack() }

  Box(modifier = modifier.fillMaxSize()) {
    Scaffold(
      modifier = Modifier.fillMaxSize(),
      snackbarHost = { SnackbarHost(snackbarHostState) },
      topBar = { QuestionnaireTopBar(title = title, onBack = ::requestBack) },
    ) { innerPadding ->
      Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
        when (val state = screenState) {
          QuestionnaireScreenState.Loading ->
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
              CircularProgressIndicator(
                strokeWidth = 4.dp,
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.42f),
              )
            }

          is QuestionnaireScreenState.Error ->
            WorkflowCenteredMessage(title = "Questionnaire unavailable", message = state.message)

          is QuestionnaireScreenState.Ready -> {
            val preparedQuestionnaireJson =
              remember(resource, state.questionnaireJson) {
                resolveCqfCalculatedToday(
                  augmentQuestionnaireJsonForWorkflow(
                    resource = resource,
                    questionnaireJson = state.questionnaireJson,
                  )
                )
              }
            Column(modifier = Modifier.fillMaxSize()) {
              Box(modifier = Modifier.weight(1f)) {
                DataCaptureQuestionnaire(
                  questionnaireJson = preparedQuestionnaireJson,
                  config =
                    QuestionnaireConfig(
                      showSubmitButton = true,
                      showCancelButton = false,
                      showReviewPage = true,
                      submitButtonText = primaryActionLabel,
                    ),
                  onSubmit = { getResponse ->
                    scope.launch {
                      isSubmitting = true
                      try {
                        val patientId = launchContext?.patientId.takeIfNotBlank() ?: generateUuid()
                        val encounterId = generateUuid()
                        val patientReference = FhirString(value = "Patient/$patientId")
                        val patientRef = Reference(reference = patientReference)

                        val encounterReference = FhirString(value = "Encounter/$encounterId")
                        val encounterRef = Reference(reference = encounterReference)
                        val parentEncounterRef =
                          launchContext?.parentEncounterId.takeIfNotBlank()?.let { encounterId ->
                            Reference(reference = FhirString(value = "Encounter/$encounterId"))
                          }

                        val response = getResponse()
                        if (resource == MPOX_SUPERVISORY_CHECKLIST_RESOURCE) {
                          saveWorkflowQuestionnaireResponse(
                            prepareSupervisorChecklistResponse(
                              response = response,
                              resource = resource,
                            )
                          )
                          submissionSuccessMessage = SUBMISSION_SUCCESS_MESSAGE
                          return@launch
                        }

                        saveWorkflowQuestionnaireResponse(
                          response.copy(
                            questionnaire = Canonical(value = resource),
                            subject = patientRef,
                            encounter = encounterRef,
                          )
                        )
                        val questionnaire =
                          fhirJson.decodeFromString(
                            Questionnaire.serializer(),
                            preparedQuestionnaireJson,
                          )
                        val extractedBundle =
                          TemplateExtractionEngine.extract(questionnaire, response)

                        val bundle =
                          postProcessExtractedBundle(
                            questionnaireResourcePath = questionnaireResourcePath,
                            extractedBundle = extractedBundle,
                            patientId = patientId,
                            encounterId = encounterId,
                            patientRef = patientRef,
                            encounterRef = encounterRef,
                            parentEncounterRef = parentEncounterRef,
                          )
                        saveWorkflowBundle(bundle)
                        submissionSuccessMessage = SUBMISSION_SUCCESS_MESSAGE
                      } catch (_: CancellationException) {
                        // Validation feedback is already shown by the data capture library.
                      } catch (error: Throwable) {
                        snackbarHostState.showSnackbar(
                          error.message ?: "Unable to save the case locally."
                        )
                      } finally {
                        isSubmitting = false
                      }
                    }
                  },
                  onCancel = ::requestBack,
                )
              }
            }
          }
        }
      }
    }

    if (isSubmitting) {
      Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(
          strokeWidth = 4.dp,
          color = MaterialTheme.colorScheme.primary,
          trackColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.42f),
        )
      }
    }

    submissionSuccessMessage?.let { message ->
      AlertDialog(
        onDismissRequest = {},
        title = { Text(text = "Submission complete") },
        text = { Text(text = message) },
        confirmButton = {
          TextButton(
            onClick = {
              submissionSuccessMessage = null
              onBack()
            }
          ) {
            Text(text = "OK")
          }
        },
      )
    }

    if (showDiscardConfirmation) {
      AlertDialog(
        onDismissRequest = { showDiscardConfirmation = false },
        title = { Text(text = "Discard this form?") },
        text = {
          Text(text = "Your answers haven't been saved. If you leave now, they will be lost.")
        },
        confirmButton = {
          TextButton(
            onClick = {
              showDiscardConfirmation = false
              onBack()
            }
          ) {
            Text(text = "Discard")
          }
        },
        dismissButton = {
          TextButton(onClick = { showDiscardConfirmation = false }) { Text(text = "Keep Editing") }
        },
      )
    }
  }
}

private sealed interface QuestionnaireScreenState {
  data object Loading : QuestionnaireScreenState

  data class Ready(val questionnaireJson: String) : QuestionnaireScreenState

  data class Error(val message: String) : QuestionnaireScreenState
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuestionnaireTopBar(title: String, onBack: () -> Unit) {
  TopAppBar(
    title = { Text(text = title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
    navigationIcon = {
      IconButton(onClick = onBack) {
        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
      }
    },
    colors =
      TopAppBarDefaults.topAppBarColors(
        containerColor = MaterialTheme.colorScheme.primary,
        titleContentColor = MaterialTheme.colorScheme.onPrimary,
        navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
      ),
  )
}

@Composable
private fun QuestionnaireIntroCard(subtitle: String, showPersistenceNotice: Boolean) {
  Card(
    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp),
    shape = RoundedCornerShape(20.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      if (subtitle.isNotBlank()) {
        Text(
          text = subtitle,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }

      if (showPersistenceNotice) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Icon(
            imageVector = Icons.Default.Info,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
          )
          Text(
            text =
              "Responses can be filled on this platform, but local FHIR persistence is only enabled on Android, desktop JVM, and iOS in this app.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
    }
  }
}

@Composable
internal fun WorkflowCenteredMessage(title: String, message: String) {
  Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
    Card(
      modifier = Modifier.padding(24.dp),
      shape = RoundedCornerShape(24.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
      Column(
        modifier = Modifier.fillMaxWidth().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Text(
          text = title,
          style = MaterialTheme.typography.headlineSmall,
          color = MaterialTheme.colorScheme.onSurface,
          fontWeight = FontWeight.Bold,
        )
        Text(
          text = message,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
  }
}

private fun postProcessExtractedBundle(
  questionnaireResourcePath: String,
  extractedBundle: Bundle,
  patientId: String,
  encounterId: String,
  patientRef: Reference,
  encounterRef: Reference,
  parentEncounterRef: Reference?,
): Bundle {
  val workflowTrackingSystem =
    extractedBundle.workflowTrackingIdentifierSystem(questionnaireResourcePath)
  val rewrittenEntries =
    extractedBundle.entry.map { entry ->
      val updatedResource =
        when (val resource = entry.resource) {
          is Observation ->
            resource.copy(
              id = resource.id.orGeneratedId(),
              subject = patientRef,
              encounter = encounterRef,
            )

          is Condition ->
            resource.copy(
              id = resource.id.orGeneratedId(),
              subject = patientRef,
              encounter = encounterRef,
            )

          is Encounter ->
            resource.copy(
              id = encounterId,
              subject = patientRef,
              partOf = parentEncounterRef ?: resource.partOf,
            )

          is Specimen -> resource.copy(id = resource.id.orGeneratedId(), subject = patientRef)

          is Patient ->
            resource.withWorkflowTrackingIdentifiers(
              patientId = patientId,
              encounterId = encounterId,
              workflowTrackingSystem = workflowTrackingSystem,
            )

          else -> resource
        }

      entry.copy(resource = updatedResource)
    }

  if (questionnaireResourcePath != MEASLES_LAB_RESULTS_RESOURCE) {
    return extractedBundle.copy(entry = rewrittenEntries)
  }

  val measlesIgmResult =
    rewrittenEntries
      .mapNotNull { entry -> entry.resource as? Observation }
      .firstOrNull { observation -> observation.primaryCode() == MEASLES_IGM_CODE }
      ?.codedValue()

  val derivedClassification = deriveLabFinalClassification(measlesIgmResult)

  val updatedEntries =
    rewrittenEntries.map { entry ->
      val observation = entry.resource as? Observation ?: return@map entry
      if (observation.primaryCode() != FINAL_CLASSIFICATION_CODE || derivedClassification == null) {
        entry
      } else {
        entry.copy(
          resource =
            observation.withCodeableValue(
              codeValue = derivedClassification.code,
              displayValue = derivedClassification.display,
            )
        )
      }
    }

  return extractedBundle.copy(entry = updatedEntries)
}

private fun String?.orGeneratedId(): String = this?.takeIf(String::isNotBlank) ?: generateUuid()

private fun prepareSupervisorChecklistResponse(
  response: dev.ohs.fhir.model.r4.QuestionnaireResponse,
  resource: String,
): dev.ohs.fhir.model.r4.QuestionnaireResponse {
  val fhirJson = FhirJson.instance
  val timestamp = Clock.System.now().toString()
  val responseJson =
    fhirJson
      .encodeToJsonElement(dev.ohs.fhir.model.r4.QuestionnaireResponse.serializer(), response)
      .jsonObject
  val updatedResponse = responseJson.toMutableMap()
  val existingExtensions = responseJson["extension"]?.jsonArray?.toMutableList() ?: mutableListOf()

  existingExtensions.add(
    buildJsonObject {
      put(
        "url",
        JsonPrimitive("http://github.com/google-android/questionnaire-lastLaunched-timestamp"),
      )
      put("valueDateTime", JsonPrimitive(timestamp))
    }
  )
  existingExtensions.add(
    buildJsonObject {
      put("url", JsonPrimitive("supervisor_checklist"))
      put("valueIdentifier", supervisorChecklistIdentifierJson())
    }
  )

  updatedResponse["questionnaire"] = JsonPrimitive(resource)
  updatedResponse["status"] = JsonPrimitive("in-progress")
  updatedResponse["identifier"] = supervisorChecklistGeoLocationIdentifierJson()
  updatedResponse["extension"] = JsonArray(existingExtensions)

  if (responseJson["authored"] == null) {
    updatedResponse["authored"] = JsonPrimitive(timestamp)
  }

  return fhirJson.decodeFromJsonElement(
    dev.ohs.fhir.model.r4.QuestionnaireResponse.serializer(),
    JsonObject(updatedResponse),
  )
}

private fun supervisorChecklistIdentifierJson(): JsonObject = buildJsonObject {
  put("use", JsonPrimitive("official"))
  put(
    "type",
    buildJsonObject {
      put(
        "coding",
        JsonArray(
          listOf(
            buildJsonObject {
              put("system", JsonPrimitive("supervisor_checklist"))
              put("code", JsonPrimitive("supervisor_checklist"))
              put("display", JsonPrimitive("Supervisor Checklist"))
            }
          )
        ),
      )
      put("text", JsonPrimitive("Supervisor Checklist"))
    },
  )
  put("system", JsonPrimitive("supervisor_checklist"))
  put("value", JsonPrimitive("supervisor_checklist"))
}

private fun supervisorChecklistGeoLocationIdentifierJson(): JsonObject = buildJsonObject {
  put("use", JsonPrimitive("official"))
  put(
    "type",
    buildJsonObject {
      put(
        "coding",
        JsonArray(
          listOf(
            buildJsonObject {
              put("system", JsonPrimitive("geo-location-details"))
              put("code", JsonPrimitive("geo-location"))
              put("display", JsonPrimitive("Latitude: -4.0206585, Longitude: 39.6799808"))
            }
          )
        ),
      )
      put("text", JsonPrimitive("Latitude: -4.0206585, Longitude: 39.6799808"))
    },
  )
  put("system", JsonPrimitive("geo-location-details"))
  put("value", JsonPrimitive("lat:-4.0206585,lon:39.6799808"))
}

private fun augmentQuestionnaireJsonForWorkflow(
  resource: String,
  questionnaireJson: String,
): String =
  when (resource) {
    MPOX_SUMMARY_SHEET_RESOURCE -> questionnaireJson.withMpoxSummarySheetTemplateExtractBundle()
    else -> questionnaireJson
  }

private fun String.withMpoxSummarySheetTemplateExtractBundle(): String {
  val root = Json.parseToJsonElement(this).jsonObject
  val extensions = root["extension"]?.jsonArray.orEmpty()
  val hasTemplateExtractBundle =
    extensions.any { extension ->
      extension.jsonObject["url"]?.jsonPrimitive?.contentOrNull ==
        "http://hl7.org/fhir/uv/sdc/StructureDefinition/sdc-questionnaire-templateExtractBundle"
    }

  if (hasTemplateExtractBundle) {
    return this
  }

  val updatedRoot = root.toMutableMap()
  updatedRoot["subjectType"] = JsonArray(listOf(JsonPrimitive("Patient")))
  updatedRoot["extension"] =
    JsonArray(
      listOf(
        buildJsonObject {
          put(
            "url",
            JsonPrimitive(
              "http://hl7.org/fhir/uv/sdc/StructureDefinition/sdc-questionnaire-templateExtractBundle"
            ),
          )
          put("valueReference", buildJsonObject { put("reference", JsonPrimitive("#bunExtract")) })
        }
      ) + extensions
    )
  updatedRoot["contained"] =
    JsonArray(
      (root["contained"]?.jsonArray.orEmpty()) + listOf(buildMpoxSummarySheetTemplateBundle(root))
    )

  return JsonObject(updatedRoot).toString()
}

private fun buildMpoxSummarySheetTemplateBundle(questionnaireJson: JsonObject): JsonObject {
  val patientFullUrl = "urn:uuid:mpox-summary-sheet-patient"
  val encounterFullUrl = "urn:uuid:mpox-summary-sheet-encounter"
  val measureReportFullUrl = "urn:uuid:mpox-summary-sheet-measure-report"
  val entries =
    mutableListOf(
      buildJsonObject {
        put("fullUrl", JsonPrimitive(patientFullUrl))
        put(
          "resource",
          buildJsonObject {
            put("resourceType", JsonPrimitive("Patient"))
            put("active", JsonPrimitive(true))
          },
        )
        put("request", questionnaireTemplateRequestJson("Patient"))
      },
      buildJsonObject {
        put("fullUrl", JsonPrimitive(encounterFullUrl))
        put(
          "resource",
          buildJsonObject {
            put("resourceType", JsonPrimitive("Encounter"))
            put("status", JsonPrimitive("finished"))
            put(
              "class",
              buildJsonObject {
                put("system", JsonPrimitive("http://terminology.hl7.org/CodeSystem/v3-ActCode"))
                put("code", JsonPrimitive("AMB"))
                put("display", JsonPrimitive("ambulatory"))
              },
            )
            put("subject", referenceJson(patientFullUrl))
            put(
              "reasonCode",
              JsonArray(
                listOf(
                  buildJsonObject {
                    put(
                      "coding",
                      JsonArray(
                        listOf(
                          buildJsonObject {
                            put(
                              "system",
                              JsonPrimitive("http://example.org/CodeSystem/case-reason"),
                            )
                            put(
                              "code",
                              JsonPrimitive(
                                workflowTrackingIdentifierSystem(MPOX_SUMMARY_SHEET_RESOURCE)
                              ),
                            )
                            put("display", JsonPrimitive("Mpox Summary Sheet"))
                          }
                        )
                      ),
                    )
                  }
                )
              ),
            )
          },
        )
        put("request", questionnaireTemplateRequestJson("Encounter"))
      },
      buildJsonObject {
        put("fullUrl", JsonPrimitive(measureReportFullUrl))
        put(
          "resource",
          buildJsonObject {
            put("resourceType", JsonPrimitive("MeasureReport"))
            put("status", JsonPrimitive("complete"))
            put("type", JsonPrimitive("summary"))
            put("measure", JsonPrimitive(MPOX_SUMMARY_SHEET_MEASURE_REFERENCE))
            put("subject", referenceJson(patientFullUrl))
          },
        )
        put("request", questionnaireTemplateRequestJson("MeasureReport"))
      },
    )

  questionnaireJson.responseItems().forEach { itemElement ->
    val item = itemElement.jsonObject
    val linkId = item["linkId"]?.jsonPrimitive?.contentOrNull ?: return@forEach
    val title = item["text"]?.jsonPrimitive?.contentOrNull.orEmpty()
    if (linkId in MPOX_SUMMARY_SHEET_OBSERVATION_SECTION_LINK_IDS) {
      entries +=
        item.buildMpoxSummarySheetObservationTemplateEntries(
          sectionLinkId = linkId,
          sectionTitle = title,
          patientFullUrl = patientFullUrl,
          encounterFullUrl = encounterFullUrl,
        )
    }
  }

  return buildJsonObject {
    put("resourceType", JsonPrimitive("Bundle"))
    put("id", JsonPrimitive("bunExtract"))
    put("type", JsonPrimitive("transaction"))
    put("entry", JsonArray(entries))
  }
}

private fun JsonObject.buildMpoxSummarySheetObservationTemplateEntries(
  sectionLinkId: String,
  sectionTitle: String,
  patientFullUrl: String,
  encounterFullUrl: String,
): List<JsonObject> {
  val entries = mutableListOf<JsonObject>()

  fun collect(items: JsonArray) {
    for (element in items) {
      val item = element.jsonObject
      val type = item["type"]?.jsonPrimitive?.contentOrNull.orEmpty()
      val linkId = item["linkId"]?.jsonPrimitive?.contentOrNull.orEmpty()
      val text =
        item["text"]?.jsonPrimitive?.contentOrNull?.trim()?.takeIf(String::isNotBlank) ?: linkId

      questionnaireObservationValueTemplate(type)?.let { valueTemplate ->
        entries += buildJsonObject {
          put(
            "extension",
            JsonArray(
              listOf(
                buildJsonObject {
                  put(
                    "url",
                    JsonPrimitive(
                      "http://hl7.org/fhir/uv/sdc/StructureDefinition/sdc-questionnaire-templateExtractContext"
                    ),
                  )
                  put(
                    "valueString",
                    JsonPrimitive("%resource.descendants().where(linkId = '$linkId')"),
                  )
                }
              )
            ),
          )
          put("fullUrl", JsonPrimitive("urn:uuid:mpox-summary-sheet-observation-$linkId"))
          put(
            "resource",
            buildJsonObject {
              put("resourceType", JsonPrimitive("Observation"))
              put("status", JsonPrimitive("final"))
              put("subject", referenceJson(patientFullUrl))
              put("encounter", referenceJson(encounterFullUrl))
              put(
                "category",
                JsonArray(
                  listOf(
                    buildJsonObject {
                      put(
                        "coding",
                        JsonArray(
                          listOf(
                            buildJsonObject {
                              put("system", JsonPrimitive(MPOX_SUMMARY_SHEET_SECTION_CODE_SYSTEM))
                              put("code", JsonPrimitive(sectionLinkId))
                              put("display", JsonPrimitive(sectionTitle))
                            }
                          )
                        ),
                      )
                    }
                  )
                ),
              )
              put(
                "code",
                buildJsonObject {
                  put(
                    "coding",
                    JsonArray(
                      listOf(
                        buildJsonObject {
                          put("system", JsonPrimitive(MPOX_SUMMARY_SHEET_OBSERVATION_CODE_SYSTEM))
                          put("code", JsonPrimitive(linkId))
                          put("display", JsonPrimitive(text))
                        }
                      )
                    ),
                  )
                  put("text", JsonPrimitive(text))
                },
              )
              valueTemplate(this, text)
            },
          )
          put("request", questionnaireTemplateRequestJson("Observation"))
        }
      }
      collect(item.responseItems())
    }
  }

  collect(responseItems())
  return entries
}

private fun questionnaireObservationValueTemplate(
  type: String
): ((kotlinx.serialization.json.JsonObjectBuilder, String) -> Unit)? =
  when (type) {
    "integer" -> { builder, _ ->
        builder.put(
          "_valueInteger",
          questionnaireTemplateExtractPrimitiveValue(extensionValue = "answer.value"),
        )
      }

    "string" -> { builder, _ ->
        builder.put(
          "_valueString",
          questionnaireTemplateExtractPrimitiveValue(extensionValue = "answer.value"),
        )
      }

    "choice" -> { builder, _ ->
        builder.put(
          "valueCodeableConcept",
          buildJsonObject {
            put(
              "coding",
              JsonArray(
                listOf(
                  buildJsonObject {
                    put("system", JsonPrimitive(MPOX_SUMMARY_SHEET_OBSERVATION_CODE_SYSTEM))
                    put(
                      "_code",
                      questionnaireTemplateExtractPrimitiveValue(
                        extensionValue = "answer.value.code"
                      ),
                    )
                    put(
                      "_display",
                      questionnaireTemplateExtractPrimitiveValue(
                        extensionValue = "answer.value.display"
                      ),
                    )
                  }
                )
              ),
            )
          },
        )
      }

    else -> null
  }

private fun questionnaireTemplateExtractPrimitiveValue(extensionValue: String): JsonObject =
  buildJsonObject {
    put(
      "extension",
      JsonArray(
        listOf(
          buildJsonObject {
            put(
              "url",
              JsonPrimitive(
                "http://hl7.org/fhir/uv/sdc/StructureDefinition/sdc-questionnaire-templateExtractValue"
              ),
            )
            put("valueString", JsonPrimitive(extensionValue))
          }
        )
      ),
    )
  }

private fun questionnaireTemplateRequestJson(resourceType: String): JsonObject = buildJsonObject {
  put("method", JsonPrimitive("POST"))
  put("url", JsonPrimitive(resourceType))
}

private fun bundleEntryJson(resource: JsonObject): JsonObject = buildJsonObject {
  put("resource", resource)
}

private fun referenceJson(reference: String): JsonObject = buildJsonObject {
  put("reference", JsonPrimitive(reference))
}

private fun JsonObject.responseItems(): JsonArray =
  this["item"]?.jsonArray ?: JsonArray(emptyList())

private fun Patient.withWorkflowTrackingIdentifiers(
  patientId: String,
  encounterId: String,
  workflowTrackingSystem: String,
): Patient {
  val fhirJson = FhirJson.instance
  val patientJson =
    fhirJson.encodeToJsonElement(Patient.serializer(), this).jsonObject.toMutableMap()
  patientJson["id"] = JsonPrimitive(patientId)
  patientJson["identifier"] =
    workflowPatientIdentifiersJson(
      existingIdentifiers = patientJson["identifier"]?.jsonArray,
      workflowTrackingSystem = workflowTrackingSystem,
      encounterId = encounterId,
      systemCreationTimestamp = currentWorkflowIdentifierTimestamp(),
    )
  return fhirJson.decodeFromJsonElement(Patient.serializer(), JsonObject(patientJson))
}

private fun workflowPatientIdentifiersJson(
  existingIdentifiers: JsonArray?,
  workflowTrackingSystem: String,
  encounterId: String,
  systemCreationTimestamp: String,
): JsonArray {
  val retainedIdentifiers =
    existingIdentifiers
      ?.filterNot { identifier ->
        val identifierSystem = identifier.jsonObject["system"]?.jsonPrimitive?.contentOrNull
        identifierSystem == SYSTEM_CREATION_IDENTIFIER_SYSTEM ||
          identifierSystem == workflowTrackingSystem
      }
      .orEmpty()

  return JsonArray(
    retainedIdentifiers +
      listOf(
        workflowSystemCreationIdentifierJson(systemCreationTimestamp),
        workflowRegistrationIdentifierJson(
          workflowTrackingSystem = workflowTrackingSystem,
          encounterId = encounterId,
        ),
      )
  )
}

private fun workflowSystemCreationIdentifierJson(timestamp: String): JsonObject = buildJsonObject {
  put(
    "type",
    buildJsonObject {
      put(
        "coding",
        JsonArray(
          listOf(
            buildJsonObject {
              put("system", JsonPrimitive(SYSTEM_CREATION_IDENTIFIER_SYSTEM))
              put("code", JsonPrimitive("system_creation"))
              put("display", JsonPrimitive("System Creation"))
            }
          )
        ),
      )
      put("text", JsonPrimitive(timestamp))
    },
  )
  put("system", JsonPrimitive(SYSTEM_CREATION_IDENTIFIER_SYSTEM))
  put("value", JsonPrimitive(timestamp))
}

private fun workflowRegistrationIdentifierJson(
  workflowTrackingSystem: String,
  encounterId: String,
): JsonObject = buildJsonObject {
  put(
    "type",
    buildJsonObject {
      put(
        "coding",
        JsonArray(
          listOf(
            buildJsonObject {
              put("system", JsonPrimitive(workflowTrackingSystem))
              put("code", JsonPrimitive(workflowTrackingSystem))
              put("display", JsonPrimitive(workflowTrackingSystem))
            }
          )
        ),
      )
      put("text", JsonPrimitive(encounterId))
    },
  )
  put("system", JsonPrimitive(workflowTrackingSystem))
  put("value", JsonPrimitive(encounterId))
}

private fun Bundle.workflowTrackingIdentifierSystem(questionnaireResourcePath: String): String =
  entry
    .mapNotNull { entry -> entry.resource as? Encounter }
    .firstNotNullOfOrNull { encounter ->
      encounter.reasonCode.firstOrNull()?.coding?.firstOrNull()?.code?.value
    } ?: workflowTrackingIdentifierSystem(questionnaireResourcePath)

private fun workflowTrackingIdentifierSystem(resource: String): String =
  when (resource) {
    MPOX_SUPERVISORY_CHECKLIST_RESOURCE -> "supervisor_checklist"
    else -> resource.substringAfterLast('/').substringBeforeLast('.')
  }

private fun currentWorkflowIdentifierTimestamp(): String {
  val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
  fun Int.twoDigits(): String = toString().padStart(2, '0')

  return buildString {
    append(now.date.year)
    append('-')
    append(now.date.monthNumber.twoDigits())
    append('-')
    append(now.date.dayOfMonth.twoDigits())
    append(' ')
    append(now.hour.twoDigits())
    append(':')
    append(now.minute.twoDigits())
    append(':')
    append(now.second.twoDigits())
  }
}

private fun Observation.primaryCode(): String? =
  code.coding.firstOrNull()?.code?.value ?: code.text?.value

private fun Observation.codedValue(): String? =
  `value`?.asCodeableConcept()?.value?.let { codeableConcept ->
    codeableConcept.coding.firstOrNull()?.display?.value
      ?: codeableConcept.coding.firstOrNull()?.code?.value
      ?: codeableConcept.text?.value
  } ?: `value`?.asString()?.value?.value

private fun Observation.withCodeableValue(codeValue: String, displayValue: String): Observation =
  copy(
    `value` =
      Observation.Value.CodeableConcept(
        CodeableConcept(
          coding =
            listOf(
              Coding(
                system = Uri(value = LAB_RESULTS_CODE_SYSTEM),
                code = Code(value = codeValue),
                display = FhirString(value = displayValue),
              )
            ),
          text = FhirString(value = displayValue),
        )
      )
  )
