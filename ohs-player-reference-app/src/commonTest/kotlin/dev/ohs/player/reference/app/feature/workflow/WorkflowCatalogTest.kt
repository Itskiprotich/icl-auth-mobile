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
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json

class WorkflowCatalogTest {

  private val json = Json { ignoreUnknownKeys = true }

  @Test
  fun resolveNode_usesStartNodeWhenRouteNodeMissing() {
    val catalog =
      json.decodeFromString(
        WorkflowCatalog.serializer(),
        """
        {
          "modules": [
            {
              "id": "notifiable-diseases",
              "title": "Notifiable Diseases",
              "description": "Capture immediate, weekly and monthly alerts.",
              "icon": "warning",
              "startNodeId": "root",
              "nodes": [
                {
                  "id": "root",
                  "title": "Notifiable Diseases",
                  "subtitle": "Select a form to proceed.",
                  "sectionTitle": "Available Forms",
                  "layout": "grid",
                  "items": [
                    {
                      "id": "immediate",
                      "title": "Immediate Reportable Diseases",
                      "destinationNodeId": "immediate"
                    }
                  ]
                },
                {
                  "id": "immediate",
                  "title": "Immediate Reportable Diseases",
                  "subtitle": "Select reported disease",
                  "sectionTitle": "Available Forms",
                  "layout": "grid",
                  "items": []
                }
              ]
            }
          ]
        }
        """
          .trimIndent(),
      )

    val module = assertNotNull(catalog.findModule("notifiable-diseases"))

    assertEquals("root", module.resolveNode(null)?.id)
    assertEquals("immediate", module.resolveNode("immediate")?.id)
  }

  @Test
  fun toCardSpec_preservesConfiguredModuleMetadata() {
    val module =
      WorkflowModule(
        id = "rcce-tools",
        title = "RCCE Tools",
        description = "Record rumors and social investigation data.",
        icon = WorkflowIconKey.PERSON,
        startNodeId = "rcce-home",
      )

    val card = module.toCardSpec()

    assertEquals("rcce-tools", card.key)
    assertEquals("RCCE Tools", card.title)
    assertEquals("Record rumors and social investigation data.", card.description)
  }

  @Test
  fun workflowAction_deserializesQuestionnaireAndRecordListDestinations() {
    val catalog =
      json.decodeFromString(
        WorkflowCatalog.serializer(),
        """
        {
          "modules": [
            {
              "id": "notifiable-diseases",
              "title": "Notifiable Diseases",
              "description": "Capture immediate, weekly and monthly alerts.",
              "icon": "warning",
              "startNodeId": "measles",
              "nodes": [
                {
                  "id": "measles",
                  "title": "Measles",
                  "layout": "actions",
                  "items": [
                    {
                      "id": "add-case",
                      "title": "Add New Measles Case",
                      "action": {
                        "type": "questionnaire",
                        "resource": "questionnaires/measles-case-intake.json",
                        "title": "New Measles Case",
                        "subtitle": "Capture patient details.",
                        "primaryActionLabel": "Submit Case"
                      }
                    },
                    {
                      "id": "case-list",
                      "title": "Measles Case List",
                      "action": {
                        "type": "record_list",
                        "resource": "records/measles-cases.json",
                        "title": "Measles Case List"
                      },
                      "trailingLabel": "Cases"
                    }
                  ]
                }
              ]
            }
          ]
        }
        """
          .trimIndent(),
      )

    val module = assertNotNull(catalog.findModule("notifiable-diseases"))
    val addCase = assertNotNull(module.findItem(nodeId = "measles", itemId = "add-case"))
    val caseList = assertNotNull(module.findItem(nodeId = "measles", itemId = "case-list"))

    assertEquals(WorkflowActionType.QUESTIONNAIRE, addCase.action?.type)
    assertEquals("questionnaires/measles-case-intake.json", addCase.action?.resource)
    assertEquals("Submit Case", addCase.action?.primaryActionLabel)
    assertEquals(WorkflowActionType.RECORD_LIST, caseList.action?.type)
    assertEquals("records/measles-cases.json", caseList.action?.resource)
    assertEquals("Cases", caseList.trailingLabel)
    assertTrue(caseList.action != null)
  }

  @Test
  fun workflowNode_defaultsToScreenPresentationWhenUnspecified() {
    val catalog =
      json.decodeFromString(
        WorkflowCatalog.serializer(),
        """
        {
          "modules": [
            {
              "id": "rcce-tools",
              "title": "RCCE Tools",
              "description": "Record rumors and social investigation data.",
              "icon": "person",
              "startNodeId": "root",
              "nodes": [
                {
                  "id": "root",
                  "title": "RCCE Tools",
                  "layout": "grid",
                  "items": []
                }
              ]
            }
          ]
        }
        """
          .trimIndent(),
      )

    val node = assertNotNull(catalog.findModule("rcce-tools")?.findNode("root"))

    assertEquals(WorkflowNodePresentation.SCREEN, node.presentation)
    assertEquals(null, node.formCategory)
  }

  @Test
  fun workflowNode_parsesBottomSheetPresentationAndFormCategory() {
    val catalog =
      json.decodeFromString(
        WorkflowCatalog.serializer(),
        """
        {
          "modules": [
            {
              "id": "rcce-tools",
              "title": "RCCE Tools",
              "description": "Record rumors and social investigation data.",
              "icon": "person",
              "startNodeId": "social-investigation-add",
              "nodes": [
                {
                  "id": "social-investigation-add",
                  "title": "Add",
                  "layout": "grid",
                  "items": [
                    {
                      "id": "county-sub-county-questionnaire",
                      "title": "County/Sub County Questionnaire",
                      "destinationNodeId": "county-sub-county-questionnaire"
                    }
                  ]
                },
                {
                  "id": "county-sub-county-questionnaire",
                  "title": "County/Sub County Questionnaire",
                  "layout": "actions",
                  "presentation": "bottom_sheet",
                  "formCategory": "social",
                  "items": [
                    {
                      "id": "add-county-sub-county-questionnaire",
                      "title": "Add",
                      "action": {
                        "type": "questionnaire",
                        "resource": "questionnaires/county-sub-county-questionnaire.json"
                      }
                    }
                  ]
                }
              ]
            }
          ]
        }
        """
          .trimIndent(),
      )

    val module = assertNotNull(catalog.findModule("rcce-tools"))
    val destinationId =
      assertNotNull(module.findItem("social-investigation-add", "county-sub-county-questionnaire"))
        .destinationNodeId
    val destinationNode = assertNotNull(destinationId?.let(module::findNode))

    assertEquals(WorkflowNodePresentation.BOTTOM_SHEET, destinationNode.presentation)
    assertEquals("social", destinationNode.formCategory)
    assertEquals(1, destinationNode.items.size)
  }
}
