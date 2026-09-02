package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val LightColorScheme = lightColorScheme(
  primary = DoorPrimary,
  onPrimary = DoorOnPrimary,
  primaryContainer = DoorPrimaryContainer,
  onPrimaryContainer = DoorOnPrimaryContainer,
  secondary = DoorSecondary,
  onSecondary = DoorOnSecondary,
  secondaryContainer = DoorSecondaryContainer,
  onSecondaryContainer = DoorOnSecondaryContainer,
  tertiary = DoorTertiary,
  onTertiary = DoorOnTertiary,
  tertiaryContainer = DoorTertiaryContainer,
  onTertiaryContainer = DoorOnTertiaryContainer,
  background = BackgroundLight,
  onBackground = OnSurfaceLight,
  surface = SurfaceLight,
  onSurface = OnSurfaceLight,
  surfaceVariant = SurfaceVariantLight,
  onSurfaceVariant = OnSurfaceVariantLight,
  outline = OutlineLight,
  error = DoorError,
  onError = Color.White
)

private val DarkColorScheme = darkColorScheme(
  primary = Color(0xFF38BDF8),
  onPrimary = Color(0xFF082F49),
  primaryContainer = Color(0xFF0369A1),
  onPrimaryContainer = Color(0xFFE0F2FE),
  secondary = Color(0xFFFBBF24),
  onSecondary = Color(0xFF451A03),
  secondaryContainer = Color(0xFF78350F),
  onSecondaryContainer = Color(0xFFFEF3C7),
  tertiary = Color(0xFF2DD4BF),
  onTertiary = Color(0xFF042F2E),
  tertiaryContainer = Color(0xFF0F766E),
  onTertiaryContainer = Color(0xFFCCFBF1),
  background = BackgroundDark,
  onBackground = OnSurfaceDark,
  surface = SurfaceDark,
  onSurface = OnSurfaceDark,
  surfaceVariant = SurfaceVariantDark,
  onSurfaceVariant = OnSurfaceVariantDark,
  outline = OutlineDark,
  error = Color(0xFFF87171),
  onError = Color(0xFF450A0A)
)

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false, // Use our handcrafted branding by default for high contrast & professionalism
  content: @Composable () -> Unit,
) {
  val colorScheme = when {
    dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
      val context = LocalContext.current
      if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    }
    darkTheme -> DarkColorScheme
    else -> LightColorScheme
  }

  MaterialTheme(
    colorScheme = colorScheme,
    typography = Typography,
    content = content
  )
}
