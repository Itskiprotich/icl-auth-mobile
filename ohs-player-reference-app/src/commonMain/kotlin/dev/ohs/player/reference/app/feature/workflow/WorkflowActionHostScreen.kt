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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.ohs.player.reference.app.components.ReferenceAppLoader

@Composable
fun WorkflowActionHostScreen(
  moduleId: String,
  nodeId: String,
  itemId: String,
  onBack: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val screenState by
    produceState<WorkflowActionState>(
      initialValue = WorkflowActionState.Loading,
      moduleId,
      nodeId,
      itemId,
    ) {
      value =
        runCatching {
            val module = WorkflowCatalogStore.module(moduleId)
            val item = module?.findItem(nodeId, itemId)
            val action = item?.action
            if (module == null || item == null || action == null) {
              WorkflowActionState.Missing("This workflow action is not configured yet.")
            } else {
              WorkflowActionState.Ready(
                title = action.title ?: item.title,
                subtitle = action.subtitle.ifBlank { item.description },
                action = action,
              )
            }
          }
          .getOrElse { error ->
            WorkflowActionState.Missing(
              error.message ?: "This workflow action could not be resolved."
            )
          }
    }

  when (val state = screenState) {
    WorkflowActionState.Loading ->
      ReferenceAppLoader(
        message = "Preparing workflow action",
        subtitle = "Fetching the next screen for this workflow.",
        modifier = modifier,
      )
    is WorkflowActionState.Missing ->
      Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Card(
          modifier = Modifier.padding(24.dp),
          shape = RoundedCornerShape(24.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
          Column(modifier = Modifier.padding(22.dp)) {
            Text(
              text = "Workflow action unavailable",
              style = MaterialTheme.typography.headlineSmall,
              color = MaterialTheme.colorScheme.onSurface,
              fontWeight = FontWeight.Bold,
            )
            Text(
              text = state.message,
              modifier = Modifier.padding(top = 8.dp),
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }
    is WorkflowActionState.Ready ->
      when (state.action.type) {
        WorkflowActionType.QUESTIONNAIRE ->
          QuestionnaireHostScreen(
            title = state.title,
            subtitle = state.subtitle,
            resource = state.action.resource,
            onBack = onBack,
            modifier = modifier,
            primaryActionLabel = state.action.primaryActionLabel ?: "Submit Case",
          )
        WorkflowActionType.RECORD_LIST ->
          WorkflowRecordListScreen(
            title = state.title,
            subtitle = state.subtitle,
            resource = state.action.resource,
            onBack = onBack,
            modifier = modifier,
          )
      }
  }
}

private sealed interface WorkflowActionState {
  data object Loading : WorkflowActionState

  data class Ready(val title: String, val subtitle: String, val action: WorkflowAction) :
    WorkflowActionState

  data class Missing(val message: String) : WorkflowActionState
}
