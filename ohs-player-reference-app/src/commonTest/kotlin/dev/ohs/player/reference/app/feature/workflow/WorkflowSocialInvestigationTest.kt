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

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class WorkflowSocialInvestigationTest {

  @Test
  fun socialInvestigationCategoryFor_mapsKnownQuestionnaireResourcesToCategories() {
    assertEquals(
      SOCIAL_CATEGORY_COUNTY_SUB_COUNTY,
      socialInvestigationCategoryFor(SOCIAL_COUNTY_SUB_COUNTY_QUESTIONNAIRE_RESOURCE),
    )
    assertEquals(
      SOCIAL_CATEGORY_COMMUNITY,
      socialInvestigationCategoryFor(SOCIAL_COMMUNITY_QUESTIONNAIRE_RESOURCE),
    )
  }

  @Test
  fun socialInvestigationCategoryFor_returnsNullForUnrelatedOrMissingResource() {
    assertNull(socialInvestigationCategoryFor(null))
    assertNull(socialInvestigationCategoryFor("questionnaires/measles-case.json"))
  }

  @Test
  fun registry_socialFormsSpecMatchesBothQuestionnaireResources() {
    val spec =
      WorkflowCasePresentationRegistry.specForRecordResource(
        SOCIAL_INVESTIGATION_COMBINED_RECORD_RESOURCE
      )

    assertEquals(
      setOf(
        SOCIAL_COUNTY_SUB_COUNTY_QUESTIONNAIRE_RESOURCE,
        SOCIAL_COMMUNITY_QUESTIONNAIRE_RESOURCE,
      ),
      spec.questionnaireResources,
    )

    assertTrue(
      WorkflowCasePresentationRegistry.matchesRecordResource(
        resource = SOCIAL_INVESTIGATION_COMBINED_RECORD_RESOURCE,
        questionnaireResource = SOCIAL_COUNTY_SUB_COUNTY_QUESTIONNAIRE_RESOURCE,
        questionnaireReference = SOCIAL_COUNTY_SUB_COUNTY_QUESTIONNAIRE_RESOURCE,
      )
    )
    assertTrue(
      WorkflowCasePresentationRegistry.matchesRecordResource(
        resource = SOCIAL_INVESTIGATION_COMBINED_RECORD_RESOURCE,
        questionnaireResource = SOCIAL_COMMUNITY_QUESTIONNAIRE_RESOURCE,
        questionnaireReference = SOCIAL_COMMUNITY_QUESTIONNAIRE_RESOURCE,
      )
    )
  }

  @Test
  fun registry_socialFormsSpecDoesNotMatchUnrelatedQuestionnaires() {
    assertFalse(
      WorkflowCasePresentationRegistry.matchesRecordResource(
        resource = SOCIAL_INVESTIGATION_COMBINED_RECORD_RESOURCE,
        questionnaireResource = "questionnaires/measles-case.json",
        questionnaireReference = "questionnaires/measles-case.json",
      )
    )
  }
}
