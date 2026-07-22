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
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import iclauth.ohs_player_reference_app.generated.resources.Res
import iclauth.ohs_player_reference_app.generated.resources.urbanist_bold
import iclauth.ohs_player_reference_app.generated.resources.urbanist_medium
import iclauth.ohs_player_reference_app.generated.resources.urbanist_regular
import iclauth.ohs_player_reference_app.generated.resources.urbanist_semibold
import org.jetbrains.compose.resources.Font

// --- Colour tokens ---

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

// --- Font ---

@Composable
fun urbanistFontFamily(): FontFamily =
  FontFamily(
    Font(resource = Res.font.urbanist_regular, weight = FontWeight.Normal),
    Font(resource = Res.font.urbanist_medium, weight = FontWeight.Medium),
    Font(resource = Res.font.urbanist_semibold, weight = FontWeight.SemiBold),
    Font(resource = Res.font.urbanist_bold, weight = FontWeight.Bold),
  )

// --- Typography ---
// All 15 Material 3 type roles are defined here so that every text in the app
// uses Urbanist and benefits from consistent clinical readability.
// Sizes follow the M3 type scale with minor upward tweaks for screen legibility.

@Composable
fun appTypography(): Typography {
  val u = urbanistFontFamily()

  fun base(size: Int, lineH: Int, weight: FontWeight, tracking: Float = 0f) =
    TextStyle(
      fontFamily = u,
      fontWeight = weight,
      fontSize = size.sp,
      lineHeight = lineH.sp,
      letterSpacing = tracking.sp,
    )

  return Typography(
    // Display — hero / splash text
    displayLarge = base(size = 57, lineH = 64, weight = FontWeight.Bold, tracking = -0.25f),
    displayMedium = base(size = 45, lineH = 52, weight = FontWeight.SemiBold),
    displaySmall = base(size = 36, lineH = 44, weight = FontWeight.SemiBold),

    // Headline — screen / section titles
    headlineLarge = base(size = 34, lineH = 42, weight = FontWeight.Bold),
    headlineMedium = base(size = 28, lineH = 36, weight = FontWeight.Bold),
    headlineSmall = base(size = 24, lineH = 32, weight = FontWeight.Bold),

    // Title — card headers, list titles
    titleLarge = base(size = 21, lineH = 28, weight = FontWeight.SemiBold),
    titleMedium = base(size = 17, lineH = 24, weight = FontWeight.SemiBold, tracking = 0.15f),
    titleSmall = base(size = 14, lineH = 20, weight = FontWeight.Medium, tracking = 0.10f),

    // Body — primary reading text; generous line-height for clinical clarity
    bodyLarge = base(size = 16, lineH = 25, weight = FontWeight.Normal, tracking = 0.50f),
    bodyMedium = base(size = 15, lineH = 22, weight = FontWeight.Normal, tracking = 0.25f),
    bodySmall = base(size = 13, lineH = 18, weight = FontWeight.Normal, tracking = 0.40f),

    // Label — chips, tags, captions, navigation items
    labelLarge = base(size = 14, lineH = 20, weight = FontWeight.SemiBold, tracking = 0.10f),
    labelMedium = base(size = 12, lineH = 16, weight = FontWeight.Medium, tracking = 0.50f),
    labelSmall = base(size = 11, lineH = 16, weight = FontWeight.Medium, tracking = 0.50f),
  )
}

// --- Theme entry point ---

@Composable
fun OhsPlayerTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
  val colorScheme = if (darkTheme) OhsDarkColorScheme else OhsLightColorScheme
  MaterialTheme(colorScheme = colorScheme, typography = appTypography(), content = content)
}
