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
package dev.ohs.player.reference.app.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import icl.ohs.libs.auth.IclAuth
import icl.ohs.libs.auth.models.ProviderProfile
import icl.ohs.libs.auth.models.ProviderUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeShellUiState(
  val isRefreshingProfile: Boolean = false,
  val providerProfile: ProviderProfile? = IclAuth.currentProviderProfile(),
) {
  val user: ProviderUser?
    get() = providerProfile?.user

  val displayName: String
    get() {
      val fullName = user?.fullNames?.trim().orEmpty()
      if (fullName.isNotBlank()) {
        return fullName
      }

      return listOfNotNull(user?.firstName, user?.lastName)
        .map(String::trim)
        .filter(String::isNotBlank)
        .joinToString(" ")
        .ifBlank { "Welcome" }
    }

  val roleLabel: String
    get() = user?.practitionerRole.formatRoleLabel()

  val locationLabel: String
    get() =
      listOfNotNull(
          user?.locationInfo?.subCountyName?.takeIf(String::isNotBlank),
          user?.locationInfo?.countyName?.takeIf(String::isNotBlank),
        )
        .joinToString(", ")
        .ifBlank { "Kenya" }
}

class HomeShellViewModel : ViewModel() {
  private val _uiState = MutableStateFlow(HomeShellUiState())
  val uiState: StateFlow<HomeShellUiState> = _uiState.asStateFlow()

  init {
    refreshProviderProfile()
  }

  fun refreshProviderProfile() {
    viewModelScope.launch {
      _uiState.update { it.copy(isRefreshingProfile = true) }
      val refreshedProfile = IclAuth.refreshProviderProfile() ?: IclAuth.currentProviderProfile()
      _uiState.value =
        HomeShellUiState(isRefreshingProfile = false, providerProfile = refreshedProfile)
    }
  }
}

internal fun String?.formatRoleLabel(): String =
  this?.trim()
    ?.takeIf(String::isNotBlank)
    ?.split('_')
    ?.joinToString(" ") { token ->
      token.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }
    .orEmpty()
