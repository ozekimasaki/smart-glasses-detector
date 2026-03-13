package jp.smartglasses.detector.ui.theme

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
    primary            = BrandOrange,
    onPrimary          = NeutralWhite,
    primaryContainer   = BrandOrangeLight,
    onPrimaryContainer = Color(0xFF4A1800),

    background         = NeutralGray50,
    onBackground       = TextPrimary,

    surface            = NeutralWhite,
    onSurface          = TextPrimary,
    surfaceVariant     = NeutralGray100,
    onSurfaceVariant   = TextSecondary,

    outline            = NeutralGray200,
    outlineVariant     = NeutralGray200,
)

private val DarkColorScheme = darkColorScheme(
    primary            = Color(0xFFFFC27A),
    onPrimary          = Color(0xFF4B2500),
    primaryContainer   = Color(0xFF7A3A00),
    onPrimaryContainer = Color(0xFFFFE3C1),

    background         = DarkBackground,
    onBackground       = DarkOnSurface,

    surface            = DarkSurface,
    onSurface          = DarkOnSurface,
    surfaceVariant     = DarkSurface2,
    onSurfaceVariant   = DarkOnSurface2,

    outline            = DarkOutline,
    outlineVariant     = DarkOutlineMuted,
)

@Composable
fun スマートグラス検出Theme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
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
