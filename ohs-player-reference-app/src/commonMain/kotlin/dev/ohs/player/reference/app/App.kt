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
package dev.ohs.player.reference.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
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
import dev.ohs.player.reference.app.auth.initializeReferenceAuthIfNeeded
import dev.ohs.player.reference.app.components.ReferenceBottomBar
import dev.ohs.player.reference.app.feature.group.list.GroupListScreen
import dev.ohs.player.reference.app.feature.group.profile.GroupProfileScreen
import dev.ohs.player.reference.app.feature.home.HomeScreen
import dev.ohs.player.reference.app.feature.home.HomeShellViewModel
import dev.ohs.player.reference.app.feature.home.ProfileScreen
import dev.ohs.player.reference.app.feature.patient.profile.PatientProfileScreen
import dev.ohs.player.reference.app.feature.workflow.DefaultWorkflowCatalog
import dev.ohs.player.reference.app.feature.workflow.WorkflowActionHostScreen
import dev.ohs.player.reference.app.feature.workflow.WorkflowCaseDetailsScreen
import dev.ohs.player.reference.app.feature.workflow.WorkflowCaseTabActionHostScreen
import dev.ohs.player.reference.app.feature.workflow.WorkflowCatalogStore
import dev.ohs.player.reference.app.feature.workflow.WorkflowModuleScreen
import dev.ohs.player.reference.app.feature.workflow.toCardSpec
import icl.ohs.libs.auth.IclAuth
import icl.ohs.libs.auth.models.SetNewPasswordScreenConfig
import icl.ohs.libs.auth.screens.NotificationScreen
import icl.ohs.libs.auth.screens.SetNewPasswordScreen

private const val HOME_ROUTE = "home"
private const val PROFILE_ROUTE = "profile"
private const val NOTIFICATIONS_ROUTE = "home/notifications"
private const val CHANGE_PASSWORD_ROUTE = "profile/change-password"
private const val CASE_MANAGEMENT_ROUTE = "workflow/case-management"
private const val WORKFLOW_MODULE_ROUTE = "workflow/module"
private const val WORKFLOW_NODE_ROUTE = "workflow/node"
private const val WORKFLOW_ACTION_ROUTE = "workflow/action"
private const val WORKFLOW_CASE_DETAILS_ROUTE = "workflow/case-details"
private const val WORKFLOW_CASE_TAB_ACTION_ROUTE = "workflow/case-tab-action"
private const val GROUP_PROFILE_ROUTE = "groupProfile"
private const val PATIENT_PROFILE_ROUTE = "patientProfile"
private const val GROUP_ID_ARG = "groupId"
private const val PATIENT_ID_ARG = "patientId"
private const val WORKFLOW_MODULE_ID_ARG = "workflowModuleId"
private const val WORKFLOW_NODE_ID_ARG = "workflowNodeId"
private const val WORKFLOW_ITEM_ID_ARG = "workflowItemId"
private const val WORKFLOW_RECORD_ID_ARG = "workflowRecordId"
private const val WORKFLOW_CASE_TAB_ID_ARG = "workflowCaseTabId"
private val CHANGE_PASSWORD_SCREEN_CONFIG = SetNewPasswordScreenConfig(showFooter = false)
private val bottomBarRoutes = setOf(HOME_ROUTE, PROFILE_ROUTE)

@Composable
fun App() {
  remember { initializeReferenceAuthIfNeeded() }
  val registry = remember { buildAppViewRegistry() }

  CompositionLocalProvider(LocalViewRegistry provides registry) {
    OhsPlayerTheme {
      var isLoggedIn by rememberSaveable { mutableStateOf(IclAuth.hasValidAccessToken()) }

      if (isLoggedIn) {
        ReferenceAppNavigation(
          onLogout = {
            IclAuth.clearSession()
            isLoggedIn = false
          }
        )
      } else {
        AuthNavigation(onAuthenticated = { isLoggedIn = true })
      }
    }
  }
}

@Composable
private fun ReferenceAppNavigation(onLogout: () -> Unit) {
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
  val showBottomBar = currentRoute in bottomBarRoutes
  val homeSelected = currentRoute == HOME_ROUTE
  val profileSelected = currentRoute == PROFILE_ROUTE

  Box(modifier = Modifier.fillMaxSize()) {
    NavHost(
      navController = navController,
      startDestination = HOME_ROUTE,
      modifier = Modifier.fillMaxSize(),
    ) {
      composable(HOME_ROUTE) {
        HomeScreen(
          uiState = uiState,
          workflows = workflowCards,
          onNotificationsClick = { navController.navigate(NOTIFICATIONS_ROUTE) },
          onWorkflowClick = { workflow ->
            if (workflow.key == "case-management") {
              navController.navigate(CASE_MANAGEMENT_ROUTE)
            } else {
              navController.navigate("$WORKFLOW_MODULE_ROUTE/${workflow.key}")
            }
          },
        )
      }

      composable(PROFILE_ROUTE) {
        ProfileScreen(
          uiState = uiState,
          onRefreshClick = shellViewModel::refreshProviderProfile,
          onChangePasswordClick = { navController.navigate(CHANGE_PASSWORD_ROUTE) },
          onLogoutClick = onLogout,
        )
      }

      composable(NOTIFICATIONS_ROUTE) {
        NotificationScreen(onBackClick = { navController.popBackStack() })
      }

      composable(CHANGE_PASSWORD_ROUTE) {
        SetNewPasswordScreen(
          config = CHANGE_PASSWORD_SCREEN_CONFIG,
          initialIdNumber = uiState.user?.idNumber.orEmpty(),
          onPasswordResetSuccess = { navController.popBackStack() },
        )
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
          onRecordClick = { record ->
            record.references?.questionnaireResponseId?.let { recordId ->
              navController.navigate("$WORKFLOW_CASE_DETAILS_ROUTE/$recordId")
            }
          },
        )
      }

      composable(
        route = "$WORKFLOW_CASE_DETAILS_ROUTE/{$WORKFLOW_RECORD_ID_ARG}",
        arguments = listOf(navArgument(WORKFLOW_RECORD_ID_ARG) { type = NavType.StringType }),
      ) { back ->
        val questionnaireResponseId =
          back.arguments?.read { getString(WORKFLOW_RECORD_ID_ARG) }.orEmpty()
        WorkflowCaseDetailsScreen(
          questionnaireResponseId = questionnaireResponseId,
          onBack = { navController.popBackStack() },
          onTabActionClick = { request ->
            navController.navigate(
              "$WORKFLOW_CASE_TAB_ACTION_ROUTE/${request.questionnaireResponseId}/${request.tabId}"
            )
          },
        )
      }

      composable(
        route =
          "$WORKFLOW_CASE_TAB_ACTION_ROUTE/{$WORKFLOW_RECORD_ID_ARG}/{$WORKFLOW_CASE_TAB_ID_ARG}",
        arguments =
          listOf(
            navArgument(WORKFLOW_RECORD_ID_ARG) { type = NavType.StringType },
            navArgument(WORKFLOW_CASE_TAB_ID_ARG) { type = NavType.StringType },
          ),
      ) { back ->
        val questionnaireResponseId =
          back.arguments?.read { getString(WORKFLOW_RECORD_ID_ARG) }.orEmpty()
        val tabId = back.arguments?.read { getString(WORKFLOW_CASE_TAB_ID_ARG) }.orEmpty()
        WorkflowCaseTabActionHostScreen(
          questionnaireResponseId = questionnaireResponseId,
          tabId = tabId,
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

    if (showBottomBar) {
      ReferenceBottomBar(
        homeSelected = homeSelected,
        profileSelected = profileSelected,
        modifier = Modifier.align(Alignment.BottomCenter),
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
  }
}
