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
import dev.ohs.fhir.model.r4.Condition
import dev.ohs.fhir.model.r4.Encounter
import dev.ohs.fhir.model.r4.Observation
import dev.ohs.fhir.model.r4.Patient
import dev.ohs.fhir.model.r4.Questionnaire
import dev.ohs.fhir.model.r4.Reference
import dev.ohs.fhir.model.r4.Specimen
import dev.ohs.fhir.model.r4.String as FhirString
import dev.ohs.player.reference.app.components.ReferenceAppLoader
import dev.ohs.player.reference.app.generateUuid
import dev.ohs.player.reference.app.util.FhirJson
import kotlin.time.Clock
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

@Composable
fun QuestionnaireHostScreen(
  title: String,
  subtitle: String,
  resource: String,
  onBack: () -> Unit,
  modifier: Modifier = Modifier,
  primaryActionLabel: String = "Submit Case",
) {
  val fhirJson = FhirJson.instance
  fun String?.orGeneratedId(): String = takeUnless { it.isNullOrBlank() } ?: generateUuid()

  val screenState by
    produceState<QuestionnaireScreenState>(
      initialValue = QuestionnaireScreenState.Loading,
      resource,
    ) {
      value =
        runCatching { WorkflowQuestionnaireStore.questionnaireJson(resource) }
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
  Box(modifier = modifier.fillMaxSize()) {
    Scaffold(
      modifier = Modifier.fillMaxSize(),
      snackbarHost = { SnackbarHost(snackbarHostState) },
      topBar = {
        QuestionnaireTopBar(
          title = title,
          onBack = {
            if (!isSubmitting && submissionSuccessMessage == null) {
              onBack()
            }
          },
        )
      },
    ) { innerPadding ->
      Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
        when (val state = screenState) {
          QuestionnaireScreenState.Loading ->
            ReferenceAppLoader(
              message = "Loading questionnaire",
              subtitle = "Preparing the form and pre-filling any available context.",
            )

          is QuestionnaireScreenState.Error ->
            WorkflowCenteredMessage(title = "Questionnaire unavailable", message = state.message)

          is QuestionnaireScreenState.Ready ->
            Column(modifier = Modifier.fillMaxSize()) {
              Box(modifier = Modifier.weight(1f)) {
                DataCaptureQuestionnaire(
                  questionnaireJson = resolveCqfCalculatedToday(state.questionnaireJson),
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
                        val patientId = generateUuid()
                        val encounterId = generateUuid()
                        val patientReference = FhirString(value = "Patient/$patientId")
                        val patientRef = Reference(reference = patientReference)

                        val encounterReference = FhirString(value = "Encounter/$encounterId")
                        val encounterRef = Reference(reference = encounterReference)

                        val response = getResponse()
                        saveWorkflowQuestionnaireResponse(
                          response.copy(subject = patientRef, encounter = encounterRef)
                        )
                        val questionnaire =
                          fhirJson.decodeFromString(
                            Questionnaire.serializer(),
                            state.questionnaireJson,
                          )
                        val extractedBundle =
                          TemplateExtractionEngine.extract(questionnaire, response)

                        val bundle =
                          extractedBundle.copy(
                            entry =
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
                                        id = resource.id.orGeneratedId(),
                                        subject = patientRef,
                                      )

                                    is Specimen ->
                                      resource.copy(
                                        id = resource.id.orGeneratedId(),
                                        subject = patientRef,
                                      )

                                    is Patient -> resource.copy(id = resource.id.orGeneratedId())

                                    else -> resource
                                  }

                                entry.copy(resource = updatedResource)
                              }
                          )
                        saveWorkflowBundle(bundle)
                        submissionSuccessMessage =
                          "Resources were extracted successfully and the questionnaire response is ready for review."
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
                  onCancel = {
                    if (!isSubmitting && submissionSuccessMessage == null) {
                      onBack()
                    }
                  },
                )
              }
            }
        }
      }
    }

    if (isSubmitting) {
      ReferenceAppLoader(
        message = "Submitting questionnaire response",
        subtitle = "Extracting and saving the generated resources.",
        showScrim = true,
      )
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
private fun WorkflowCenteredMessage(title: String, message: String) {
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
