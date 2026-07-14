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

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class WorkflowCatalog(
    val modules: List<WorkflowModule> = emptyList(),
)

@Serializable
data class WorkflowModule(
    val id: String,
    val title: String,
    val description: String,
    val icon: WorkflowIconKey = WorkflowIconKey.INFO,
    val startNodeId: String,
    val nodes: List<WorkflowNode> = emptyList(),
)

@Serializable
data class WorkflowNode(
    val id: String,
    val title: String,
    val subtitle: String = "",
    val sectionTitle: String = "",
    val layout: WorkflowNodeLayout = WorkflowNodeLayout.GRID,
    val items: List<WorkflowNodeItem> = emptyList(),
)

@Serializable
data class WorkflowNodeItem(
    val id: String,
    val title: String,
    val description: String = "",
    val icon: WorkflowIconKey? = null,
    val destinationNodeId: String? = null,
    val action: WorkflowAction? = null,
    val trailingValue: String? = null,
    val trailingLabel: String? = null,
)

@Serializable
data class WorkflowAction(
    val type: WorkflowActionType,
    val resource: String,
    val title: String? = null,
    val subtitle: String = "",
    val primaryActionLabel: String? = null,
)

@Serializable
enum class WorkflowNodeLayout {
    @SerialName("grid")
    GRID,
    @SerialName("actions")
    ACTIONS,
}

@Serializable
enum class WorkflowActionType {
    @SerialName("questionnaire")
    QUESTIONNAIRE,
    @SerialName("record_list")
    RECORD_LIST,
}

@Serializable
enum class WorkflowIconKey {
    @SerialName("warning")
    WARNING,
    @SerialName("favorite")
    FAVORITE,
    @SerialName("info")
    INFO,
    @SerialName("person")
    PERSON,
    @SerialName("check_circle")
    CHECK_CIRCLE,
    @SerialName("add")
    ADD,
}

@Immutable
data class WorkflowCardSpec(
    val key: String,
    val title: String,
    val description: String,
    val icon: ImageVector,
    val color: Color? = null,
)

object DefaultWorkflowCatalog {
    val cards: List<WorkflowCardSpec> =
        listOf(
            WorkflowCardSpec(
                key = "notifiable-diseases",
                title = "Notifiable Diseases",
                description = "Capture immediate, weekly and monthly alerts.",
                icon = WorkflowIconKey.WARNING.toImageVector(),
            ),
            WorkflowCardSpec(
                key = "mass-immunization",
                title = "Mass Immunization",
                description = "Track vaccination and campaign activities.",
                icon = WorkflowIconKey.FAVORITE.toImageVector(),
            ),
            WorkflowCardSpec(
                key = "case-management",
                title = "Case Management",
                description = "Manage follow-up and case outcomes.",
                icon = WorkflowIconKey.INFO.toImageVector(),
            ),
            WorkflowCardSpec(
                key = "rcce-tools",
                title = "RCCE Tools",
                description = "Record rumors and social investigation data.",
                icon = WorkflowIconKey.PERSON.toImageVector(),
            ),
            WorkflowCardSpec(
                key = "assessments-surveys",
                title = "Assessments/Surveys",
                description = "Run assessments and supervision checklists.",
                icon = WorkflowIconKey.CHECK_CIRCLE.toImageVector(),
            ),
        )
}

fun WorkflowCatalog.findModule(moduleId: String?): WorkflowModule? =
    modules.firstOrNull { it.id == moduleId }

fun WorkflowModule.resolveNode(nodeId: String?): WorkflowNode? =
    if (nodeId == null) findNode(startNodeId) else findNode(nodeId)

fun WorkflowModule.findNode(nodeId: String): WorkflowNode? = nodes.firstOrNull { it.id == nodeId }

fun WorkflowModule.findItem(nodeId: String, itemId: String): WorkflowNodeItem? =
    findNode(nodeId)?.findItem(itemId)

fun WorkflowNode.findItem(itemId: String): WorkflowNodeItem? = items.firstOrNull { it.id == itemId }

fun WorkflowModule.toCardSpec(): WorkflowCardSpec =
    WorkflowCardSpec(
        key = id,
        title = title,
        description = description,
        icon = icon.toImageVector(),
    )

fun WorkflowIconKey.toImageVector(): ImageVector =
    when (this) {
        WorkflowIconKey.WARNING -> Icons.Default.Warning
        WorkflowIconKey.FAVORITE -> Icons.Default.Favorite
        WorkflowIconKey.INFO -> Icons.Default.Info
        WorkflowIconKey.PERSON -> Icons.Default.Person
        WorkflowIconKey.CHECK_CIRCLE -> Icons.Default.CheckCircle
        WorkflowIconKey.ADD -> Icons.Default.Add
    }
