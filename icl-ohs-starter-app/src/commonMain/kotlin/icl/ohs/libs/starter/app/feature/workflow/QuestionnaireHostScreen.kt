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
package icl.ohs.libs.starter.app.feature.workflow

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.ohs.fhir.model.r4.Questionnaire
import dev.ohs.fhir.model.r4.Questionnaire.QuestionnaireItemType

@Composable
fun QuestionnaireHostScreen(
  title: String,
  subtitle: String,
  resource: String,
  onBack: () -> Unit,
  modifier: Modifier = Modifier,
  primaryActionLabel: String = "Submit Case",
) {
  val screenState by
    produceState<QuestionnaireScreenState>(
      initialValue = QuestionnaireScreenState.Loading,
      resource,
    ) {
      value =
        runCatching { WorkflowQuestionnaireStore.questionnaire(resource) }
          .fold(
            onSuccess = QuestionnaireScreenState::Ready,
            onFailure = {
              QuestionnaireScreenState.Error(
                it.message ?: "The questionnaire could not be loaded.",
              )
            },
          )
    }

  Scaffold(
    modifier = modifier,
    containerColor = Color.Transparent,
    topBar = {
      QuestionnaireTopBar(
        title = title,
        onBack = onBack,
      )
    },
  ) { innerPadding ->
    Box(
      modifier =
        Modifier.fillMaxSize()
          .background(
            Brush.verticalGradient(
              listOf(
                MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                MaterialTheme.colorScheme.background,
                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f),
              )
            )
          )
          .padding(innerPadding),
    ) {
      when (val state = screenState) {
        QuestionnaireScreenState.Loading ->
          Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
          }
        is QuestionnaireScreenState.Error ->
          WorkflowCenteredMessage(
            title = "Questionnaire unavailable",
            message = state.message,
          )
        is QuestionnaireScreenState.Ready ->
          QuestionnaireContent(
            questionnaire = state.questionnaire,
            title = title,
            subtitle = subtitle,
            primaryActionLabel = primaryActionLabel,
            onBack = onBack,
          )
      }
    }
  }
}

private sealed interface QuestionnaireScreenState {
  data object Loading : QuestionnaireScreenState

  data class Ready(val questionnaire: Questionnaire) : QuestionnaireScreenState

  data class Error(val message: String) : QuestionnaireScreenState
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuestionnaireTopBar(
  title: String,
  onBack: () -> Unit,
) {
  TopAppBar(
    title = {
      Text(
        text = title,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    },
    navigationIcon = {
      IconButton(onClick = onBack) {
        Icon(
          imageVector = Icons.AutoMirrored.Filled.ArrowBack,
          contentDescription = "Back",
        )
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
private fun QuestionnaireContent(
  questionnaire: Questionnaire,
  title: String,
  subtitle: String,
  primaryActionLabel: String,
  onBack: () -> Unit,
) {
  val answers = remember(questionnaire.id) { mutableStateMapOf<String, String>() }
  var validationMessage by rememberSaveable(questionnaire.id) { mutableStateOf<String?>(null) }
  var successMessage by rememberSaveable(questionnaire.id) { mutableStateOf<String?>(null) }
  val answerableItems = remember(questionnaire) { questionnaire.answerableItems() }
  val requiredItems = remember(answerableItems) { answerableItems.filter { it.isRequired() } }
  val answeredCount =
    answerableItems.count { item -> answers[item.linkId.value.orEmpty()].isAnswerPresent() }
  val totalQuestions = answerableItems.size.coerceAtLeast(1)

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(start = 24.dp, top = 20.dp, end = 24.dp, bottom = 120.dp),
    verticalArrangement = Arrangement.spacedBy(18.dp),
  ) {
    item {
      QuestionnaireHeroCard(
        title = title,
        subtitle = subtitle.ifBlank {
          questionnaire.description?.value ?: "Capture structured case information."
        },
        answeredCount = answeredCount,
        totalQuestions = totalQuestions,
        requiredCount = requiredItems.size,
      )
    }

    validationMessage?.let { message ->
      item {
        InlineMessageCard(
          title = "Missing required answers",
          message = message,
          containerColor = MaterialTheme.colorScheme.errorContainer,
          contentColor = MaterialTheme.colorScheme.onErrorContainer,
        )
      }
    }

    successMessage?.let { message ->
      item {
        InlineMessageCard(
          title = "Questionnaire ready",
          message = message,
          containerColor = MaterialTheme.colorScheme.tertiaryContainer,
          contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        )
      }
    }

    item {
      QuestionnaireInstructionCard(
        description =
          questionnaire.description?.value
            ?: "Complete the questions below to prepare a case record for submission.",
      )
    }

    item {
      QuestionnaireItemGroup(
        items = questionnaire.item,
        answers = answers,
      )
    }

    item {
      Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedButton(
          modifier = Modifier.weight(1f),
          onClick = {
            validationMessage = null
            successMessage = "Draft saved locally for this session."
          },
        ) {
          Text("Save Draft")
        }
        Button(
          modifier = Modifier.weight(1f),
          onClick = {
            val missingQuestions =
              requiredItems.filterNot { item ->
                answers[item.linkId.value.orEmpty()].isAnswerPresent()
              }
            if (missingQuestions.isNotEmpty()) {
              validationMessage =
                missingQuestions.joinToString(separator = "\n") {
                  "• ${it.text?.value.orEmpty().ifBlank { "A required question" }}"
                }
              successMessage = null
            } else {
              validationMessage = null
              successMessage =
                "${questionnaire.title?.value ?: title} is ready. $answeredCount answers were captured locally."
            }
          },
        ) {
          Text(primaryActionLabel)
        }
      }
    }

    item {
      TextButton(
        modifier = Modifier.fillMaxWidth(),
        onClick = onBack,
      ) {
        Text("Return To Workflow")
      }
    }
  }
}

@Composable
private fun QuestionnaireHeroCard(
  title: String,
  subtitle: String,
  answeredCount: Int,
  totalQuestions: Int,
  requiredCount: Int,
) {
  Card(
    shape = RoundedCornerShape(30.dp),
    colors =
      CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surface,
      ),
    border =
      BorderStroke(
        1.dp,
        MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
      ),
    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
  ) {
    Column(
      modifier =
        Modifier.fillMaxWidth()
          .background(
            Brush.verticalGradient(
              listOf(
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                MaterialTheme.colorScheme.surface,
              )
            )
          )
          .padding(22.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        WorkflowHeaderPill(label = "Questionnaire Host")
        Text(
          text = title,
          style = MaterialTheme.typography.headlineMedium,
          color = MaterialTheme.colorScheme.onSurface,
          fontWeight = FontWeight.Bold,
        )
        Text(
          text = subtitle,
          style = MaterialTheme.typography.bodyLarge,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }

      Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        QuestionnaireMetricCard(
          label = "Answered",
          value = "$answeredCount/$totalQuestions",
        )
        QuestionnaireMetricCard(
          label = "Required",
          value = requiredCount.toString(),
        )
      }
    }
  }
}

@Composable
private fun QuestionnaireMetricCard(
  label: String,
  value: String,
) {
  Surface(
    shape = RoundedCornerShape(20.dp),
    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
  ) {
    Column(
      modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
      verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
      Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
      )
      Text(
        text = value,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
        fontWeight = FontWeight.Bold,
      )
    }
  }
}

@Composable
private fun QuestionnaireInstructionCard(
  description: String,
) {
  Card(
    shape = RoundedCornerShape(24.dp),
    colors =
      CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surface,
      ),
    border =
      BorderStroke(
        1.dp,
        MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
      ),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 16.dp),
      horizontalArrangement = Arrangement.spacedBy(12.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Icon(
        imageVector = Icons.Default.Info,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
      )
      Text(
        text = description,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}

@Composable
private fun QuestionnaireItemGroup(
  items: List<Questionnaire.Item>,
  answers: MutableMap<String, String>,
  level: Int = 0,
) {
  Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
    items.forEach { item ->
      when (item.type.value) {
        QuestionnaireItemType.Group -> QuestionnaireSectionCard(item = item, answers = answers, level = level)
        QuestionnaireItemType.Display -> QuestionnaireDisplayCard(item = item)
        null -> Unit
        else -> QuestionnaireQuestionCard(item = item, answers = answers, level = level)
      }
    }
  }
}

@Composable
private fun QuestionnaireSectionCard(
  item: Questionnaire.Item,
  answers: MutableMap<String, String>,
  level: Int,
) {
  Card(
    shape = RoundedCornerShape(26.dp),
    colors =
      CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surface,
      ),
    border =
      BorderStroke(
        1.dp,
        MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
      ),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 18.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
          text = item.text?.value.orEmpty().ifBlank { "Question Group" },
          style = MaterialTheme.typography.titleLarge,
          color = MaterialTheme.colorScheme.onSurface,
          fontWeight = FontWeight.Bold,
        )
        item.prefix?.value?.takeIf(String::isNotBlank)?.let { prefix ->
          Text(
            text = prefix,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
          )
        }
      }

      QuestionnaireItemGroup(
        items = item.item,
        answers = answers,
        level = level + 1,
      )
    }
  }
}

@Composable
private fun QuestionnaireDisplayCard(item: Questionnaire.Item) {
  Surface(
    shape = RoundedCornerShape(22.dp),
    color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.55f),
  ) {
    Text(
      text = item.text?.value.orEmpty(),
      modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 16.dp),
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onTertiaryContainer,
    )
  }
}

@Composable
private fun QuestionnaireQuestionCard(
  item: Questionnaire.Item,
  answers: MutableMap<String, String>,
  level: Int,
) {
  val answerKey = item.linkId.value.orEmpty()
  val answer = answers[answerKey].orEmpty()
  val isRequired = item.isRequired()
  val questionType = item.type.value ?: QuestionnaireItemType.String
  val cardPadding = if (level == 0) 18.dp else 16.dp

  Card(
    shape = RoundedCornerShape(24.dp),
    colors =
      CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surface,
      ),
    border =
      BorderStroke(
        1.dp,
        if (isRequired) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
        else MaterialTheme.colorScheme.outline.copy(alpha = 0.12f),
      ),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(cardPadding),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
          Text(
            text = item.text?.value.orEmpty().ifBlank { "Question" },
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f),
          )
          if (isRequired) {
            WorkflowHeaderPill(
              label = "Required",
              containerColor = MaterialTheme.colorScheme.primaryContainer,
              contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            )
          }
        }
        Text(
          text = questionTypeLabel(questionType),
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }

      when (questionType) {
        QuestionnaireItemType.String,
        QuestionnaireItemType.Text,
        QuestionnaireItemType.Url,
        QuestionnaireItemType.Date,
        QuestionnaireItemType.DateTime,
        QuestionnaireItemType.Time,
        QuestionnaireItemType.Integer,
        QuestionnaireItemType.Decimal ->
          AnswerTextField(
            questionType = questionType,
            value = answer,
            onValueChange = { newValue ->
              answers[answerKey] =
                when (questionType) {
                  QuestionnaireItemType.Integer -> newValue.filter(Char::isDigit)
                  QuestionnaireItemType.Decimal ->
                    buildString {
                      var dotSeen = false
                      newValue.forEach { char ->
                        when {
                          char.isDigit() -> append(char)
                          char == '.' && !dotSeen -> {
                            dotSeen = true
                            append(char)
                          }
                        }
                      }
                    }
                  else -> newValue
                }
            },
          )
        QuestionnaireItemType.Boolean ->
          BooleanAnswerSelector(
            selected = answer,
            onSelected = { selected ->
              answers[answerKey] = selected
            },
          )
        QuestionnaireItemType.Choice,
        QuestionnaireItemType.Open_Choice ->
          ChoiceAnswerSelector(
            item = item,
            selected = answer,
            onSelected = { selected ->
              answers[answerKey] = selected
            },
          )
        else ->
          Surface(
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
          ) {
            Text(
              text = "This question type is not yet rendered in the sample host.",
              modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
      }
    }
  }
}

@Composable
private fun AnswerTextField(
  questionType: QuestionnaireItemType,
  value: String,
  onValueChange: (String) -> Unit,
) {
  val placeholder =
    when (questionType) {
      QuestionnaireItemType.Date -> "YYYY-MM-DD"
      QuestionnaireItemType.DateTime -> "YYYY-MM-DDThh:mm:ss"
      QuestionnaireItemType.Time -> "hh:mm:ss"
      QuestionnaireItemType.Integer -> "Enter a whole number"
      QuestionnaireItemType.Decimal -> "Enter a decimal number"
      QuestionnaireItemType.Text -> "Type detailed notes"
      QuestionnaireItemType.Url -> "https://"
      else -> "Enter response"
    }

  OutlinedTextField(
    value = value,
    onValueChange = onValueChange,
    modifier = Modifier.fillMaxWidth(),
    minLines = if (questionType == QuestionnaireItemType.Text) 4 else 1,
    maxLines = if (questionType == QuestionnaireItemType.Text) 6 else 1,
    label = { Text("Response") },
    placeholder = { Text(placeholder) },
    keyboardOptions =
      androidx.compose.foundation.text.KeyboardOptions(
        keyboardType =
          when (questionType) {
            QuestionnaireItemType.Integer -> KeyboardType.Number
            QuestionnaireItemType.Decimal -> KeyboardType.Decimal
            QuestionnaireItemType.Url -> KeyboardType.Uri
            else -> KeyboardType.Text
          }
      ),
    shape = RoundedCornerShape(18.dp),
  )
}

@Composable
private fun BooleanAnswerSelector(
  selected: String,
  onSelected: (String) -> Unit,
) {
  Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
    BooleanOptionCard(
      modifier = Modifier.weight(1f),
      title = "Yes",
      selected = selected == "true",
      onClick = { onSelected("true") },
    )
    BooleanOptionCard(
      modifier = Modifier.weight(1f),
      title = "No",
      selected = selected == "false",
      onClick = { onSelected("false") },
    )
  }
}

@Composable
private fun BooleanOptionCard(
  title: String,
  selected: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
      Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
    color =
      if (selected) MaterialTheme.colorScheme.primaryContainer
      else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.32f),
    border =
      BorderStroke(
        1.dp,
        if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
        else MaterialTheme.colorScheme.outline.copy(alpha = 0.12f),
      ),
  ) {
      Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 14.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color =
          if (selected) MaterialTheme.colorScheme.onPrimaryContainer
          else MaterialTheme.colorScheme.onSurface,
        fontWeight = FontWeight.SemiBold,
      )
      Surface(
        shape = CircleShape,
        color =
          if (selected) MaterialTheme.colorScheme.primary
          else MaterialTheme.colorScheme.surface,
        border =
          BorderStroke(
            2.dp,
            if (selected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
          ),
      ) {
        Icon(
          imageVector = Icons.Default.CheckCircle,
          contentDescription = null,
          tint =
            if (selected) MaterialTheme.colorScheme.onPrimary
            else Color.Transparent,
          modifier = Modifier.padding(6.dp),
        )
      }
    }
  }
}

@Composable
private fun ChoiceAnswerSelector(
  item: Questionnaire.Item,
  selected: String,
  onSelected: (String) -> Unit,
) {
  Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
    item.answerOption.forEach { option ->
      val optionLabel = option.label()
      if (optionLabel.isNotBlank()) {
        Surface(
          modifier = Modifier.fillMaxWidth().clickable { onSelected(optionLabel) },
          shape = RoundedCornerShape(18.dp),
          color =
            if (selected == optionLabel) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
          border =
            BorderStroke(
              1.dp,
              if (selected == optionLabel) MaterialTheme.colorScheme.primary.copy(alpha = 0.24f)
              else MaterialTheme.colorScheme.outline.copy(alpha = 0.12f),
            ),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            RadioButton(
              selected = selected == optionLabel,
              onClick = { onSelected(optionLabel) },
            )
            Text(
              text = optionLabel,
              style = MaterialTheme.typography.bodyLarge,
              color =
                if (selected == optionLabel) MaterialTheme.colorScheme.onPrimaryContainer
                else MaterialTheme.colorScheme.onSurface,
            )
          }
        }
      }
    }
  }
}

@Composable
private fun InlineMessageCard(
  title: String,
  message: String,
  containerColor: Color,
  contentColor: Color,
) {
  Surface(
    color = containerColor,
    shape = RoundedCornerShape(22.dp),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 16.dp),
      verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = contentColor,
        fontWeight = FontWeight.Bold,
      )
      Text(
        text = message,
        style = MaterialTheme.typography.bodyMedium,
        color = contentColor,
      )
    }
  }
}

@Composable
private fun WorkflowCenteredMessage(
  title: String,
  message: String,
) {
  Box(
    modifier = Modifier.fillMaxSize(),
    contentAlignment = Alignment.Center,
  ) {
    Card(
      modifier = Modifier.padding(24.dp),
      shape = RoundedCornerShape(24.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
      Column(
        modifier = Modifier.padding(22.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
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

@Composable
private fun WorkflowHeaderPill(
  label: String,
  containerColor: Color = MaterialTheme.colorScheme.secondaryContainer,
  contentColor: Color = MaterialTheme.colorScheme.onSecondaryContainer,
) {
  Surface(
    shape = RoundedCornerShape(999.dp),
    color = containerColor,
  ) {
    Text(
      text = label,
      modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
      style = MaterialTheme.typography.labelLarge,
      color = contentColor,
      fontWeight = FontWeight.Bold,
    )
  }
}

private fun Questionnaire.answerableItems(): List<Questionnaire.Item> = item.flatMap { it.answerableItems() }

private fun Questionnaire.Item.answerableItems(): List<Questionnaire.Item> =
  when (type.value) {
    QuestionnaireItemType.Group -> item.flatMap { it.answerableItems() }
    QuestionnaireItemType.Display -> emptyList()
    else -> listOf(this)
  }

private fun Questionnaire.Item.isRequired(): Boolean = required?.value == true

private fun Questionnaire.Item.AnswerOption.label(): String =
  value.asCoding()?.value?.display?.value
    ?: value.asCoding()?.value?.code?.value
    ?: value.asString()?.value?.value
    ?: value.asInteger()?.value?.value?.toString()
    ?: value.asDate()?.value?.value?.toString()
    ?: value.asTime()?.value?.value?.toString()
    ?: value.asReference()?.value?.display?.value
    ?: ""

private fun String?.isAnswerPresent(): Boolean = !this.isNullOrBlank()

private fun questionTypeLabel(type: QuestionnaireItemType): String =
  when (type) {
    QuestionnaireItemType.String -> "Short answer"
    QuestionnaireItemType.Text -> "Detailed notes"
    QuestionnaireItemType.Integer -> "Whole number"
    QuestionnaireItemType.Decimal -> "Decimal number"
    QuestionnaireItemType.Boolean -> "Yes / No"
    QuestionnaireItemType.Date -> "Date"
    QuestionnaireItemType.DateTime -> "Date and time"
    QuestionnaireItemType.Time -> "Time"
    QuestionnaireItemType.Url -> "URL"
    QuestionnaireItemType.Choice -> "Single choice"
    QuestionnaireItemType.Open_Choice -> "Choice with custom answer"
    QuestionnaireItemType.Display -> "Display"
    QuestionnaireItemType.Group -> "Group"
    QuestionnaireItemType.Attachment -> "Attachment"
    QuestionnaireItemType.Reference -> "Reference"
    QuestionnaireItemType.Quantity -> "Quantity"
  }
