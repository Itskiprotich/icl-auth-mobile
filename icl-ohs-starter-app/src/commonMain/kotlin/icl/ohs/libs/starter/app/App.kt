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
package icl.ohs.libs.starter.app

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.savedstate.read
import dev.ohs.player.library.registry.LocalViewRegistry
import icl.ohs.libs.starter.app.feature.group.list.GroupListScreen
import icl.ohs.libs.starter.app.feature.group.profile.GroupProfileScreen
import icl.ohs.libs.starter.app.feature.home.HomeScreen
import icl.ohs.libs.starter.app.feature.home.HomeShellViewModel
import icl.ohs.libs.starter.app.feature.home.ProfileScreen
import icl.ohs.libs.starter.app.feature.patient.profile.PatientProfileScreen
import icl.ohs.libs.starter.app.feature.workflow.DefaultWorkflowCatalog
import icl.ohs.libs.starter.app.feature.workflow.WorkflowActionHostScreen
import icl.ohs.libs.starter.app.feature.workflow.WorkflowCatalogStore
import icl.ohs.libs.starter.app.feature.workflow.WorkflowModuleScreen
import icl.ohs.libs.starter.app.feature.workflow.toCardSpec
import icl.ohs.libs.auth.IclAuth
import icl.ohs.libs.auth.IclAuthConfig

private const val HOME_ROUTE = "home"
private const val PROFILE_ROUTE = "profile"
private const val CASE_MANAGEMENT_ROUTE = "workflow/case-management"
private const val WORKFLOW_MODULE_ROUTE = "workflow/module"
private const val WORKFLOW_NODE_ROUTE = "workflow/node"
private const val WORKFLOW_ACTION_ROUTE = "workflow/action"
private const val GROUP_PROFILE_ROUTE = "groupProfile"
private const val PATIENT_PROFILE_ROUTE = "patientProfile"
private const val GROUP_ID_ARG = "groupId"
private const val PATIENT_ID_ARG = "patientId"
private const val WORKFLOW_MODULE_ID_ARG = "workflowModuleId"
private const val WORKFLOW_NODE_ID_ARG = "workflowNodeId"
private const val WORKFLOW_ITEM_ID_ARG = "workflowItemId"
private val AUTH_CONFIG =
  IclAuthConfig(baseAuthUrl = "https://dsrkeycloak.intellisoftkenya.com/auth")

@Composable
fun App() {
  remember(AUTH_CONFIG) { IclAuth.initialize(AUTH_CONFIG) }
  val registry = remember { buildAppViewRegistry() }

  CompositionLocalProvider(LocalViewRegistry provides registry) {
    OhsPlayerTheme {
      var isLoggedIn by rememberSaveable { mutableStateOf(IclAuth.hasValidAccessToken()) }

      if (isLoggedIn) {
        ReferenceAppNavigation()
      } else {
        AuthNavigation(onAuthenticated = { isLoggedIn = true })
      }
    }
  }
}

@Composable
private fun ReferenceAppNavigation() {
  val navController = rememberNavController()
  val shellViewModel: HomeShellViewModel = viewModel { HomeShellViewModel() }
  val uiState by shellViewModel.uiState.collectAsStateWithLifecycle()
  val workflowCards by
    produceState(initialValue = DefaultWorkflowCatalog.cards) {
      value =
        runCatching { WorkflowCatalogStore.catalog().modules.map { it.toCardSpec() } }
          .getOrElse { DefaultWorkflowCatalog.cards }
    }
  val backStackEntry by navController.currentBackStackEntryAsState()
  val currentRoute = backStackEntry?.destination?.route
  val showBottomBar =
    currentRoute == HOME_ROUTE ||
      currentRoute == PROFILE_ROUTE ||
      currentRoute?.startsWith("workflow/") == true
  val homeSelected = currentRoute != PROFILE_ROUTE

  Scaffold(
    containerColor = MaterialTheme.colorScheme.background,
    bottomBar = {
      if (showBottomBar) {
        ReferenceBottomBar(
          homeSelected = homeSelected,
          profileSelected = currentRoute == PROFILE_ROUTE,
          onHomeClick = {
            navController.navigate(HOME_ROUTE) {
              popUpTo(navController.graph.startDestinationId) { saveState = true }
              launchSingleTop = true
              restoreState = true
            }
          },
          onProfileClick = {
            navController.navigate(PROFILE_ROUTE) {
              popUpTo(navController.graph.startDestinationId) { saveState = true }
              launchSingleTop = true
              restoreState = true
            }
          },
        )
      }
    },
  ) { innerPadding ->
    NavHost(
      navController = navController,
      startDestination = HOME_ROUTE,
      modifier = Modifier.padding(innerPadding),
    ) {
      composable(HOME_ROUTE) {
        HomeScreen(
          uiState = uiState,
          workflows = workflowCards,
          onWorkflowClick = { workflow ->
            if (workflow.key == "case-management") {
              navController.navigate(CASE_MANAGEMENT_ROUTE)
            } else {
              navController.navigate("$WORKFLOW_MODULE_ROUTE/${workflow.key}")
            }
          },
          onNotificationsClick = shellViewModel::refreshProviderProfile,
        )
      }

      composable(PROFILE_ROUTE) {
        ProfileScreen(uiState = uiState, onRefreshClick = shellViewModel::refreshProviderProfile)
      }

      composable(CASE_MANAGEMENT_ROUTE) {
        GroupListScreen(
          onGroupClick = { id -> navController.navigate("$GROUP_PROFILE_ROUTE/$id") },
          onBack = { navController.popBackStack() },
        )
      }

      composable(
        route = "$WORKFLOW_MODULE_ROUTE/{$WORKFLOW_MODULE_ID_ARG}",
        arguments = listOf(navArgument(WORKFLOW_MODULE_ID_ARG) { type = NavType.StringType }),
      ) { back ->
        val moduleId = back.arguments?.read { getString(WORKFLOW_MODULE_ID_ARG) }.orEmpty()
        WorkflowModuleScreen(
          moduleId = moduleId,
          onBack = { navController.popBackStack() },
          onNodeSelected = { nextNodeId ->
            navController.navigate("$WORKFLOW_NODE_ROUTE/$moduleId/$nextNodeId")
          },
          onActionClick = { nodeId, itemId ->
            navController.navigate("$WORKFLOW_ACTION_ROUTE/$moduleId/$nodeId/$itemId")
          },
        )
      }

      composable(
        route = "$WORKFLOW_NODE_ROUTE/{$WORKFLOW_MODULE_ID_ARG}/{$WORKFLOW_NODE_ID_ARG}",
        arguments =
          listOf(
            navArgument(WORKFLOW_MODULE_ID_ARG) { type = NavType.StringType },
            navArgument(WORKFLOW_NODE_ID_ARG) { type = NavType.StringType },
          ),
      ) { back ->
        val moduleId = back.arguments?.read { getString(WORKFLOW_MODULE_ID_ARG) }.orEmpty()
        val nodeId = back.arguments?.read { getString(WORKFLOW_NODE_ID_ARG) }.orEmpty()
        WorkflowModuleScreen(
          moduleId = moduleId,
          nodeId = nodeId,
          onBack = { navController.popBackStack() },
          onNodeSelected = { nextNodeId ->
            navController.navigate("$WORKFLOW_NODE_ROUTE/$moduleId/$nextNodeId")
          },
          onActionClick = { actionNodeId, itemId ->
            navController.navigate("$WORKFLOW_ACTION_ROUTE/$moduleId/$actionNodeId/$itemId")
          },
        )
      }

      composable(
        route =
          "$WORKFLOW_ACTION_ROUTE/{$WORKFLOW_MODULE_ID_ARG}/{$WORKFLOW_NODE_ID_ARG}/{$WORKFLOW_ITEM_ID_ARG}",
        arguments =
          listOf(
            navArgument(WORKFLOW_MODULE_ID_ARG) { type = NavType.StringType },
            navArgument(WORKFLOW_NODE_ID_ARG) { type = NavType.StringType },
            navArgument(WORKFLOW_ITEM_ID_ARG) { type = NavType.StringType },
          ),
      ) { back ->
        val moduleId = back.arguments?.read { getString(WORKFLOW_MODULE_ID_ARG) }.orEmpty()
        val nodeId = back.arguments?.read { getString(WORKFLOW_NODE_ID_ARG) }.orEmpty()
        val itemId = back.arguments?.read { getString(WORKFLOW_ITEM_ID_ARG) }.orEmpty()
        WorkflowActionHostScreen(
          moduleId = moduleId,
          nodeId = nodeId,
          itemId = itemId,
          onBack = { navController.popBackStack() },
        )
      }

      composable(
        route = "$GROUP_PROFILE_ROUTE/{$GROUP_ID_ARG}",
        arguments = listOf(navArgument(GROUP_ID_ARG) { type = NavType.StringType }),
      ) { back ->
        val groupId = back.arguments?.read { getString(GROUP_ID_ARG) }.orEmpty()
        GroupProfileScreen(
          groupId = groupId,
          onBack = { navController.popBackStack() },
          onMemberClick = { id -> navController.navigate("$PATIENT_PROFILE_ROUTE/$id") },
        )
      }

      composable(
        route = "$PATIENT_PROFILE_ROUTE/{$PATIENT_ID_ARG}",
        arguments = listOf(navArgument(PATIENT_ID_ARG) { type = NavType.StringType }),
      ) { back ->
        val patientId = back.arguments?.read { getString(PATIENT_ID_ARG) }.orEmpty()
        PatientProfileScreen(patientId = patientId, onBack = { navController.popBackStack() })
      }
    }
  }
}

@Composable
private fun ReferenceBottomBar(
  homeSelected: Boolean,
  profileSelected: Boolean,
  onHomeClick: () -> Unit,
  onProfileClick: () -> Unit,
) {
  Surface(
    color = MaterialTheme.colorScheme.surface,
    shadowElevation = 22.dp,
    tonalElevation = 10.dp,
    shape = RoundedCornerShape(topStart = 34.dp, topEnd = 34.dp),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 14.dp),
      horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      BottomNavItem(
        selected = homeSelected,
        label = "Home",
        icon = Icons.Default.Home,
        modifier = Modifier.weight(1f),
        onClick = onHomeClick,
      )
      BottomNavItem(
        selected = profileSelected,
        label = "Profile",
        icon = Icons.Default.Person,
        modifier = Modifier.weight(1f),
        onClick = onProfileClick,
      )
    }
  }
}

@Composable
private fun BottomNavItem(
  selected: Boolean,
  label: String,
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val selectedBrush =
    Brush.horizontalGradient(
      listOf(
        MaterialTheme.colorScheme.primaryContainer,
        MaterialTheme.colorScheme.tertiaryContainer,
      )
    )

  Surface(
    modifier = modifier,
    onClick = onClick,
    shape = RoundedCornerShape(26.dp),
    color = if (selected) Color.Transparent else MaterialTheme.colorScheme.surface,
    border =
      BorderStroke(
        1.dp,
        if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
        else MaterialTheme.colorScheme.outline.copy(alpha = 0.08f),
      ),
  ) {
    Box(
      modifier =
        Modifier.fillMaxWidth()
          .background(
            if (selected) selectedBrush
            else Brush.horizontalGradient(
              listOf(
                MaterialTheme.colorScheme.surface,
                MaterialTheme.colorScheme.surface,
              )
            )
          )
          .padding(horizontal = 18.dp, vertical = 14.dp),
    ) {
      Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Icon(
          imageVector = icon,
          contentDescription = label,
          tint =
            if (selected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
          text = label,
          style = MaterialTheme.typography.labelLarge,
          color =
            if (selected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant,
          fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
        )
      }
    }
  }
}
