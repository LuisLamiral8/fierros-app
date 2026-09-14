package com.luis.fierros.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Picker
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.rememberPickerState
import androidx.wear.compose.ui.tooling.preview.WearPreviewDevices
import com.luis.fierros.R
import com.luis.fierros.data.formatearPeso
import com.luis.fierros.data.kilosDe
import com.luis.fierros.presentation.theme.FierrosTheme
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.roundToInt

// El peso se arma con dos ruedas: el entero de 1 en 1 y el decimal de 0,25 en 0,25. Con una
// sola rueda de 0,25, ir de 32,5 a 40 eran 30 arrastres; así son 8 y el decimal ni se toca.
private const val MAX_ENTERO = 250
private val DECIMALES = listOf("00", "25", "50", "75")

// Colores de la maqueta v2 (assets/diseño-wearos-fierros-v2.html).
private val PISTA = Color(0xFF141317)
private val SELECCION = Color(0xFF26242B)
private val SELECCION_ACTIVA = Color(0xFF2B2930)
private val VECINO_CERCA = Color(0xFF928C9B)
private val VECINO_LEJOS = Color(0xFF6F6A78)

/**
 * Cargar el peso levantado, con dos ruedas. Abre centrada en el último registro: si hice lo
 * mismo que la vez pasada, alcanza con Guardar.
 *
 * Las dos pistas van desfasadas media fila y con la coma fija en el medio, a propósito: si las
 * filas quedaran alineadas, los vecinos se leerían como números completos ("31 ,25") que no son
 * valores a los que se pueda llegar moviendo una sola rueda.
 *
 * Se mueve con el dedo (el Watch8 40 mm no tiene corona ni bisel giratorio).
 */
@Composable
fun PantallaRegistrar(
    titulo: String,
    kilosIniciales: Double,
    ultimo: String? = null,
    plan: String? = null,
    onGuardar: (Double) -> Unit,
) {
    val enteroInicial = floor(kilosIniciales).toInt().coerceIn(0, MAX_ENTERO)
    val decimalInicial = ((kilosIniciales - enteroInicial) / 0.25).roundToInt().coerceIn(0, 3)

    val estadoEntero = rememberPickerState(
        initialNumberOfOptions = MAX_ENTERO + 1,
        initiallySelectedIndex = enteroInicial,
        // Los kilos no dan la vuelta: después de 250 no vuelve a empezar en 0.
        shouldRepeatOptions = false,
    )
    val estadoDecimal = rememberPickerState(
        initialNumberOfOptions = DECIMALES.size,
        initiallySelectedIndex = decimalInicial,
        // Son cuatro y dan la vuelta: de ,75 se pasa a ,00 en un solo paso.
        shouldRepeatOptions = true,
    )
    // 0 = la rueda del entero, 1 = la del decimal. Arranca en el entero, que es lo que cambia.
    var rueda by remember { mutableIntStateOf(0) }
    val enMando = if (rueda == 0) estadoEntero else estadoDecimal

    // Al dar la vuelta con los decimales se acarrea al entero: de ,75 a ,00 suma 1 kg.
    var decimalPrevio by remember { mutableIntStateOf(decimalInicial) }
    LaunchedEffect(estadoDecimal.selectedOptionIndex) {
        val actual = estadoDecimal.selectedOptionIndex
        val previo = decimalPrevio
        val entero = estadoEntero.selectedOptionIndex
        if (previo == DECIMALES.lastIndex && actual == 0 && entero < MAX_ENTERO) {
            estadoEntero.scrollToOption(entero + 1)
        } else if (previo == 0 && actual == DECIMALES.lastIndex && entero > 0) {
            estadoEntero.scrollToOption(entero - 1)
        }
        decimalPrevio = actual
    }

    val kilos = estadoEntero.selectedOptionIndex + estadoDecimal.selectedOptionIndex * 0.25
    val moviendo = estadoEntero.isScrollInProgress || estadoDecimal.isScrollInProgress
    val diferencia = ultimo?.let { kilosDe(it) }?.let { kilos - it }
    val haptica = LocalHapticFeedback.current

    ScreenScaffold {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .scrollable(state = enMando, orientation = Orientation.Vertical, reverseDirection = true),
        ) {
            Column(
                // El hueco de abajo es para la franja de Guardar, que va por encima.
                modifier = Modifier.fillMaxSize().padding(bottom = du(40f)),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(du(4f), Alignment.CenterVertically),
            ) {
                Text(
                    text = titulo,
                    fontSize = duSp(16f),
                    lineHeight = duSp(19f),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    modifier = Modifier.padding(horizontal = du(30f)),
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(du(10f)),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Pista(ancho = 92f, activa = rueda == 0, desfase = 0f) {
                        Picker(
                            state = estadoEntero,
                            contentDescription = { "Kilos" },
                            modifier = Modifier.fillMaxWidth().height(du(172f)).clickable { rueda = 0 },
                            userScrollEnabled = rueda == 0,
                            verticalSpacing = du(20f),
                            // El degradé difumina las filas que asoman en los bordes: sin esto quedan cortadas al medio.
                            gradientRatio = 0.3f,
                            gradientColor = PISTA,
                        ) { indice ->
                            Valor(
                                texto = indice.toString(),
                                distancia = abs(indice - estadoEntero.selectedOptionIndex),
                                activa = rueda == 0,
                            )
                        }
                    }

                    // La coma no se mueve: es el ancla que separa las dos pistas.
                    Text(
                        text = ",",
                        fontSize = duSp(38f),
                        fontWeight = FontWeight.Medium,
                        color = if (rueda == 1) VECINO_CERCA else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(top = du(18f)),
                    )

                    // Media fila más abajo que la otra pista, para que las filas no se lean en
                    // horizontal como si fueran un número completo.
                    Pista(ancho = 104f, activa = rueda == 1, desfase = 26f) {
                        Picker(
                            state = estadoDecimal,
                            contentDescription = { "Decimal" },
                            modifier = Modifier.fillMaxWidth().height(du(172f)).clickable { rueda = 1 },
                            userScrollEnabled = rueda == 1,
                            verticalSpacing = du(20f),
                            // El degradé difumina las filas que asoman en los bordes: sin esto quedan cortadas al medio.
                            gradientRatio = 0.3f,
                            gradientColor = PISTA,
                        ) { indice ->
                            Valor(
                                texto = DECIMALES[indice],
                                distancia = distanciaCircular(
                                    indice,
                                    estadoDecimal.selectedOptionIndex,
                                    DECIMALES.size,
                                ),
                                activa = rueda == 1,
                            )
                        }
                    }
                }

                // En reposo, contra qué estoy comparando. Moviendo, cuánto vale y cuánto cambió.
                val pie = when {
                    moviendo && diferencia != null -> {
                        val signo = if (diferencia >= 0) "+" else "−"
                        stringResource(
                            R.string.pie_editando,
                            formatearPeso(kilos),
                            signo + formatearPeso(abs(diferencia)).removeSuffix(" kg"),
                        )
                    }
                    moviendo -> formatearPeso(kilos)
                    ultimo != null && plan != null ->
                        stringResource(
                            R.string.pie_con_plan,
                            ultimo.removeSuffix(" kg"),
                            plan.removeSuffix(" kg"),
                        )
                    ultimo != null -> stringResource(R.string.pie_sin_plan, ultimo.removeSuffix(" kg"))
                    else -> stringResource(R.string.kilos)
                }
                Text(
                    text = pie,
                    fontSize = duSp(if (moviendo) 16f else 15f),
                    fontWeight = if (moviendo) FontWeight.Medium else FontWeight.Normal,
                    color =
                        if (moviendo) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = du(24f)),
                )
            }

            // "Guardar" no es un botón de borde: la maqueta lo dibuja como la misma franja en U
            // que los avisos, pegada abajo y recortada por la curva de la pantalla.
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.surfaceContainerHigh,
                        RoundedCornerShape(topStart = du(24f), topEnd = du(24f)),
                    )
                    .clickable {
                        haptica.performHapticFeedback(HapticFeedbackType.LongPress)
                        onGuardar(kilos)
                    }
                    .padding(top = du(10f), bottom = du(26f)),
                contentAlignment = Alignment.TopCenter,
            ) {
                Text(
                    text = stringResource(R.string.guardar),
                    fontSize = duSp(17f),
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}

/** La pista de una rueda: el fondo redondeado. La que manda toma el borde de acento. */
@Composable
private fun Pista(
    ancho: Float,
    activa: Boolean,
    desfase: Float,
    contenido: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier
            .padding(top = du(desfase))
            .width(du(ancho))
            .alpha(if (activa) 1f else 0.5f)
            .background(PISTA, RoundedCornerShape(du(22f)))
            .then(
                if (!activa) {
                    Modifier
                } else {
                    Modifier.border(du(1f), MaterialTheme.colorScheme.primary, RoundedCornerShape(du(22f)))
                },
            )
            .padding(vertical = du(6f)),
    ) {
        contenido()
    }
}

/** Un valor de la rueda: el elegido en su pastilla, los vecinos apagándose por distancia. */
@Composable
private fun Valor(texto: String, distancia: Int, activa: Boolean) {
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        when (distancia) {
            0 -> Text(
                text = texto,
                fontSize = duSp(42f),
                lineHeight = duSp(46f),
                fontWeight = FontWeight.Medium,
                color = if (activa) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .background(
                        if (activa) SELECCION_ACTIVA else SELECCION,
                        RoundedCornerShape(du(12f)),
                    )
                    .padding(horizontal = du(14f), vertical = du(3f)),
            )
            1 -> Text(texto, fontSize = duSp(27f), lineHeight = duSp(32f), color = VECINO_CERCA)
            else -> Text(texto, fontSize = duSp(23f), lineHeight = duSp(29f), color = VECINO_LEJOS)
        }
    }
}

/** Distancia en una rueda que da la vuelta: entre ,75 y ,00 hay un paso, no tres. */
private fun distanciaCircular(indice: Int, seleccionado: Int, total: Int): Int {
    val bruta = abs(indice - seleccionado) % total
    return minOf(bruta, total - bruta)
}

@WearPreviewDevices
@Composable
fun PantallaRegistrarPreview() {
    FierrosTheme {
        PantallaRegistrar(
            titulo = "B · Press militar",
            kilosIniciales = 32.5,
            ultimo = "32,5 kg",
            plan = "20 kg",
            onGuardar = {},
        )
    }
}
