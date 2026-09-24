package com.mayle.ortopedia.ui.theme

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

private val MayLeLightColorScheme = lightColorScheme(

    // Colores principales de MayLe
    primary = Color(0xFF123B5D),
    onPrimary = Color.White,

    secondary = Color(0xFF456B86),
    onSecondary = Color.White,

    // Fondo general
    background = Color.White,
    onBackground = Color(0xFF111111),

    // Superficies y campos
    surface = Color.White,
    onSurface = Color(0xFF111111),

    surfaceVariant = Color(0xFFF5F5F5),
    onSurfaceVariant = Color(0xFF222222),

    // Bordes
    outline = Color(0xFF555555),
    outlineVariant = Color(0xFF999999),

    // Errores
    error = Color(0xFFB3261E),
    onError = Color.White
)

private val MayLeDarkColorScheme = darkColorScheme(

    primary = Color(0xFF9CCBEE),
    onPrimary = Color(0xFF00344F),

    secondary = Color(0xFFB4CBDD),
    onSecondary = Color(0xFF1D3445),

    background = Color(0xFF121212),
    onBackground = Color.White,

    surface = Color(0xFF121212),
    onSurface = Color.White,

    surfaceVariant = Color(0xFF303030),
    onSurfaceVariant = Color(0xFFF5F5F5),

    outline = Color(0xFFCCCCCC),
    outlineVariant = Color(0xFF888888),

    error = Color(0xFFF2B8B5),
    onError = Color(0xFF601410)
)

@Composable
fun MayLeOrtopediaTheme(
    darkTheme: Boolean = false,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {

    val colorScheme = when {

        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {

            val context = LocalContext.current

            if (darkTheme) {
                dynamicDarkColorScheme(context)
            } else {
                dynamicLightColorScheme(context)
            }
        }

        darkTheme -> MayLeDarkColorScheme

        else -> MayLeLightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}