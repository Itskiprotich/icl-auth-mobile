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

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val OhsPrimary = Color(0xFF236B3A)
private val OhsOnPrimary = Color.White
private val OhsPrimaryContainer = Color(0xFFB7F0C2)
private val OhsOnPrimaryContainer = Color(0xFF00210D)

private val OhsSecondary = Color(0xFF506352)
private val OhsOnSecondary = Color.White
private val OhsSecondaryContainer = Color(0xFFD3E8D3)
private val OhsOnSecondaryContainer = Color(0xFF0E1F12)

private val OhsTertiary = Color(0xFF2F6B54)
private val OhsOnTertiary = Color.White
private val OhsTertiaryContainer = Color(0xFFB2F1D1)
private val OhsOnTertiaryContainer = Color(0xFF002117)

private val OhsError = Color(0xFFB3261E)
private val OhsOnError = Color.White
private val OhsErrorContainer = Color(0xFFF9DEDC)
private val OhsOnErrorContainer = Color(0xFF601410)

private val OhsBackground = Color(0xFFF6FBF4)
private val OhsSurface = Color(0xFFF6FBF4)
private val OhsOnSurface = Color(0xFF171D18)
private val OhsSurfaceVariant = Color(0xFFDDE5DA)
private val OhsOnSurfaceVariant = Color(0xFF414941)
private val OhsOutline = Color(0xFF717970)

private val OhsLightColorScheme =
  lightColorScheme(
    primary = OhsPrimary,
    onPrimary = OhsOnPrimary,
    primaryContainer = OhsPrimaryContainer,
    onPrimaryContainer = OhsOnPrimaryContainer,
    secondary = OhsSecondary,
    onSecondary = OhsOnSecondary,
    secondaryContainer = OhsSecondaryContainer,
    onSecondaryContainer = OhsOnSecondaryContainer,
    tertiary = OhsTertiary,
    onTertiary = OhsOnTertiary,
    tertiaryContainer = OhsTertiaryContainer,
    onTertiaryContainer = OhsOnTertiaryContainer,
    error = OhsError,
    onError = OhsOnError,
    errorContainer = OhsErrorContainer,
    onErrorContainer = OhsOnErrorContainer,
    background = OhsBackground,
    onBackground = OhsOnSurface,
    surface = OhsSurface,
    onSurface = OhsOnSurface,
    surfaceVariant = OhsSurfaceVariant,
    onSurfaceVariant = OhsOnSurfaceVariant,
    outline = OhsOutline,
  )

private val OhsDarkColorScheme =
  darkColorScheme(
    primary = Color(0xFF9DD7A7),
    onPrimary = Color(0xFF073916),
    primaryContainer = Color(0xFF0E5123),
    onPrimaryContainer = Color(0xFFB7F0C2),
    secondary = Color(0xFFB7CCB7),
    onSecondary = Color(0xFF223426),
    secondaryContainer = Color(0xFF384B39),
    onSecondaryContainer = Color(0xFFD3E8D3),
    tertiary = Color(0xFF97D5B4),
    onTertiary = Color(0xFF003828),
    tertiaryContainer = Color(0xFF16513D),
    onTertiaryContainer = Color(0xFFB2F1D1),
    error = Color(0xFFF2B8B5),
    onError = Color(0xFF601410),
    errorContainer = Color(0xFF8C1D18),
    onErrorContainer = Color(0xFFF9DEDC),
    background = Color(0xFF0F1511),
    onBackground = Color(0xFFDCE5DC),
    surface = Color(0xFF0F1511),
    onSurface = Color(0xFFDCE5DC),
    surfaceVariant = Color(0xFF414941),
    onSurfaceVariant = Color(0xFFC1C9BE),
    outline = Color(0xFF8B938A),
  )

@Composable
fun OhsPlayerTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
  val colorScheme = if (darkTheme) OhsDarkColorScheme else OhsLightColorScheme
  MaterialTheme(colorScheme = colorScheme, content = content)
}
