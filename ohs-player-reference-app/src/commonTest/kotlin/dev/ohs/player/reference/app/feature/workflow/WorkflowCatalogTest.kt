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
        """.trimIndent(),
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
}
