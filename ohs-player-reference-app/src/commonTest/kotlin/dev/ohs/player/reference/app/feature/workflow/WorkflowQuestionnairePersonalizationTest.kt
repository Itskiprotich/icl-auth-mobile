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

import icl.ohs.libs.auth.ProviderLocationInfo
import icl.ohs.libs.auth.ProviderUser
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class WorkflowQuestionnairePersonalizationTest {
  private val json = Json { ignoreUnknownKeys = true }

  @Test
  fun personalizeQuestionnaireJson_injectsHiddenUserInitialValues() {
    val questionnaireJson =
      """
      {
        "resourceType": "Questionnaire",
        "item": [
          {
            "item": [
              { "linkId": "user_role", "type": "string" },
              { "linkId": "user_county", "type": "string" },
              { "linkId": "visible-field", "type": "string" }
            ]
          }
        ]
      }
      """.trimIndent()

    val personalized =
      personalizeQuestionnaireJson(
        questionnaireJson = questionnaireJson,
        providerUser =
          ProviderUser(
            role = "SUBCOUNTY_DISEASE_SURVEILLANCE_OFFICER",
            locationInfo = ProviderLocationInfo(county = "37"),
          ),
      )

    val groupItems =
      json.parseToJsonElement(personalized).jsonObject["item"]!!.jsonArray.first().jsonObject["item"]!!.jsonArray

    assertEquals(
      "SUBCOUNTY_DISEASE_SURVEILLANCE_OFFICER",
      groupItems[0].jsonObject["initial"]!!.jsonArray.first().jsonObject["valueString"]!!.jsonPrimitive.content,
    )
    assertEquals(
      "37",
      groupItems[1].jsonObject["initial"]!!.jsonArray.first().jsonObject["valueString"]!!.jsonPrimitive.content,
    )
    assertNull(groupItems[2].jsonObject["initial"])
  }

  @Test
  fun personalizeQuestionnaireJson_returnsOriginalJsonWhenNoInitialValuesExist() {
    val questionnaireJson =
      """
      {
        "resourceType": "Questionnaire",
        "item": [
          {
            "item": [
              { "linkId": "visible-field", "type": "string" }
            ]
          }
        ]
      }
      """.trimIndent()

    val personalized =
      personalizeQuestionnaireJson(
        questionnaireJson = questionnaireJson,
        providerUser = ProviderUser(),
      )

    assertEquals(questionnaireJson, personalized)
  }
}
