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
import iclauth.ohs_player_reference_app.generated.resources.Res
import icl.ohs.libs.auth.IclAuth
import icl.ohs.libs.auth.ProviderLocationInfo
import icl.ohs.libs.auth.ProviderUser
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

private val workflowQuestionnaireJson = Json { ignoreUnknownKeys = true }

object WorkflowQuestionnaireStore {
  private val mutex = Mutex()
  private val cachedRawQuestionnaireJson = mutableMapOf<String, String>()
  private val cachedQuestionnaires = mutableMapOf<String, Questionnaire>()

  suspend fun questionnaireJson(resource: String): String =
    personalizeQuestionnaireJson(
      questionnaireJson = rawQuestionnaireJson(resource),
      providerUser = IclAuth.currentProviderUser(),
    )

  suspend fun questionnaire(resource: String): Questionnaire =
    mutex.withLock {
      cachedQuestionnaires[resource]
        ?: loadQuestionnaireLocked(resource).also { cachedQuestionnaires[resource] = it }
    }

  private suspend fun rawQuestionnaireJson(resource: String): String =
    mutex.withLock {
      cachedRawQuestionnaireJson[resource]
        ?: loadQuestionnaireJson(resource).also { cachedRawQuestionnaireJson[resource] = it }
    }

  private suspend fun loadQuestionnaireJson(resource: String): String =
    Res.readBytes("files/$resource").decodeToString()

  private suspend fun loadQuestionnaireLocked(resource: String): Questionnaire {
    val json =
      cachedRawQuestionnaireJson[resource]
        ?: loadQuestionnaireJson(resource).also { cachedRawQuestionnaireJson[resource] = it }
    return workflowQuestionnaireJson.decodeFromString(Questionnaire.serializer(), json)
  }
}

internal fun personalizeQuestionnaireJson(
  questionnaireJson: String,
  providerUser: ProviderUser?,
): String {
  val initialValues = providerUser.toHiddenQuestionnaireInitialValues()
  if (initialValues.isEmpty()) {
    return questionnaireJson
  }

  val source = workflowQuestionnaireJson.parseToJsonElement(questionnaireJson)
  val personalized = source.injectQuestionnaireInitialValues(initialValues)

  return if (personalized == source) {
    questionnaireJson
  } else {
    workflowQuestionnaireJson.encodeToString(JsonElement.serializer(), personalized)
  }
}

internal fun ProviderUser?.toHiddenQuestionnaireInitialValues(): Map<String, String> {
  if (this == null) {
    return emptyMap()
  }

  return buildMap {
    putIfNotBlank("user_role", role ?: practitionerRole)
    locationInfo.putIfNotBlank(this, "user_facility", ProviderLocationInfo::facility)
    locationInfo.putIfNotBlank(this, "user_ward", ProviderLocationInfo::ward)
    locationInfo.putIfNotBlank(this, "user_sub_county", ProviderLocationInfo::subCounty)
    locationInfo.putIfNotBlank(this, "user_county", ProviderLocationInfo::county)
  }
}

private fun JsonElement.injectQuestionnaireInitialValues(
  initialValues: Map<String, String>,
): JsonElement =
  when (this) {
    is JsonArray -> JsonArray(map { it.injectQuestionnaireInitialValues(initialValues) })
    is JsonObject -> {
      val updatedEntries =
        entries.associate { (key, value) ->
          key to
            if (key == "item") {
              value.injectQuestionnaireInitialValues(initialValues)
            } else {
              value
            }
        }
      val linkId = this["linkId"]?.jsonPrimitive?.contentOrNull
      val initialValue = linkId?.let(initialValues::get)

      if (initialValue == null) {
        JsonObject(updatedEntries)
      } else {
        JsonObject(
          updatedEntries +
            (
              "initial" to
                buildJsonArray {
                  add(buildJsonObject { put("valueString", JsonPrimitive(initialValue)) })
                }
              ),
        )
      }
    }
    else -> this
  }

private fun MutableMap<String, String>.putIfNotBlank(linkId: String, value: String?) {
  value?.trim()?.takeIf(String::isNotBlank)?.let { put(linkId, it) }
}

private fun ProviderLocationInfo?.putIfNotBlank(
  target: MutableMap<String, String>,
  linkId: String,
  selector: ProviderLocationInfo.() -> String?,
) {
  this?.selector()?.trim()?.takeIf(String::isNotBlank)?.let { target[linkId] = it }
}
