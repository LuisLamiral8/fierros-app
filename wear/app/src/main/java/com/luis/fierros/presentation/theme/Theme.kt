package com.luis.fierros.presentation.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.wear.compose.material3.ColorScheme
import androidx.wear.compose.material3.MaterialTheme

// Paleta de las maquetas de la iteración 3 (docs/planificaciones/planificacion-it-3.md).
// Son los mismos valores que usa la web de Fierros, así que el reloj y la web se ven de la
// misma familia. Hasta acá la app usaba el tema por defecto de Material 3, que da parecido
// pero no igual: el fondo era negro puro y no este gris muy oscuro.
private val Lavanda = Color(0xFFD0BCFF)
private val SobreLavanda = Color(0xFF21005D)
private val Fondo = Color(0xFF0F0F12)
private val Superficie = Color(0xFF1C1B1F)
private val SuperficieAlta = Color(0xFF2B2930)
private val Borde = Color(0xFF3A3740)
private val Texto = Color(0xFFE6E1E5)
private val TextoSuave = Color(0xFFA8A2AE)
private val Rojo = Color(0xFFF2B8B5)

private val ColoresFierros = ColorScheme(
    primary = Lavanda,
    onPrimary = SobreLavanda,
    primaryContainer = SuperficieAlta,
    onPrimaryContainer = Texto,
    background = Fondo,
    onBackground = Texto,
    // Los botones de lista salen de acá: gris oscuro, como en las maquetas.
    surfaceContainerLow = Superficie,
    surfaceContainer = Superficie,
    surfaceContainerHigh = SuperficieAlta,
    onSurface = Texto,
    onSurfaceVariant = TextoSuave,
    outline = Borde,
    outlineVariant = Borde,
    error = Rojo,
)

@Composable
fun FierrosTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = ColoresFierros, content = content)
}
