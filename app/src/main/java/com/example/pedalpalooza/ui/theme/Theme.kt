package com.example.pedalpalooza.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = BikeGreen80,
    onPrimary = OnBikeGreen80,
    primaryContainer = BikeGreenContainer80,
    onPrimaryContainer = OnBikeGreenContainer80,
    secondary = PnwTeal80,
    onSecondary = OnPnwTeal80,
    secondaryContainer = PnwTealContainer80,
    onSecondaryContainer = OnPnwTealContainer80,
    tertiary = SunsetOrange80,
    onTertiary = OnSunsetOrange80,
    tertiaryContainer = SunsetOrangeContainer80,
    onTertiaryContainer = OnSunsetOrangeContainer80
)

private val LightColorScheme = lightColorScheme(
    primary = BikeGreen40,
    onPrimary = OnBikeGreen40,
    primaryContainer = BikeGreenContainer40,
    onPrimaryContainer = OnBikeGreenContainer40,
    secondary = PnwTeal40,
    onSecondary = OnPnwTeal40,
    secondaryContainer = PnwTealContainer40,
    onSecondaryContainer = OnPnwTealContainer40,
    tertiary = SunsetOrange40,
    onTertiary = OnSunsetOrange40,
    tertiaryContainer = SunsetOrangeContainer40,
    onTertiaryContainer = OnSunsetOrangeContainer40
)

@Composable
fun PedalPaloozaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
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