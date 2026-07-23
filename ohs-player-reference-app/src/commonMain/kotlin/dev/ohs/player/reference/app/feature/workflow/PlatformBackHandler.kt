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

import androidx.compose.runtime.Composable

/**
 * Cross-platform hook for the system/hardware back gesture.
 *
 * On Android this intercepts the hardware/gesture back button via the platform's
 * OnBackPressedDispatcher (see the `androidMain` actual). Platforms without an equivalent
 * system-level back affordance (desktop, iOS, web) provide a no-op actual, since those screens are
 * only ever reachable through in-app navigation controls (e.g. the toolbar back arrow).
 */
@Composable expect fun BackHandler(enabled: Boolean = true, onBack: () -> Unit)
