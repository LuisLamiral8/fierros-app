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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import kotlin.math.roundToInt

// El peso se elige de a 0,25 kg: es el salto más chico que existe en discos y mancuernas, así
// que con ese paso entran todos los pesos posibles y no hace falta teclado.
private const val PASO = 0.25
private const val MAX_KILOS = 250.0
private const val OPCIONES = (MAX_KILOS / PASO).toInt() + 1

private fun kilosDeIndice(indice: Int): Double = indice * PASO

private fun indiceDeKilos(kilos: Double): Int =
    (kilos / PASO).roundToInt().coerceIn(0, OPCIONES - 1)

// Los dos grises de los vecinos salen de la maqueta: el de al lado se lee, el de más afuera
// apenas se insinúa, para que quede claro hacia dónde se mueve la rueda.
private val VECINO_CERCA = Color(0xFF6F6A78)
private val VECINO_LEJOS = Color(0xFF4E4A57)

/**
 * Cargar el peso levantado, con una rueda. Abre centrada en el último registro: si hice lo
 * mismo que la vez pasada, alcanza con Guardar.
 *
 * Se mueve con el dedo (el Watch8 40 mm no tiene corona ni bisel giratorio); el gesto se toma
 * en toda la pantalla, no solo sobre la rueda, así el dedo nunca tapa el número.
 */
@Composable
fun PantallaRegistrar(
    titulo: String,
    kilosIniciales: Double,
    ultimo: String? = null,
    plan: String? = null,
    onGuardar: (Double) -> Unit,
) {
    val estado = rememberPickerState(
        initialNumberOfOptions = OPCIONES,
        initiallySelectedIndex = indiceDeKilos(kilosIniciales),
        // Los kilos no dan la vuelta: después de 250 no vuelve a empezar en 0.
        shouldRepeatOptions = false,
    )
    val kilos = kilosDeIndice(estado.selectedOptionIndex)
    val moviendo = estado.isScrollInProgress
    val diferencia = ultimo?.let { kilosDe(it) }?.let { kilos - it }

    ScreenScaffold {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .scrollable(state = estado, orientation = Orientation.Vertical, reverseDirection = true),
        ) {
            Column(
                // El hueco de abajo es para la franja de Guardar, que va por encima.
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = du(30f), end = du(30f), bottom = du(44f)),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(du(2f), Alignment.CenterVertically),
            ) {
                Text(
                    text = titulo,
                    fontSize = duSp(16f),
                    lineHeight = duSp(19f),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    modifier = Modifier.padding(bottom = du(6f)),
                )

                Picker(
                    state = estado,
                    contentDescription = { formatearPeso(kilos) },
                    // Alto justo para cinco valores: el elegido y dos a cada lado, como la maqueta.
                    modifier = Modifier.fillMaxWidth().height(du(150f)),
                    // La maqueta no difumina los extremos: los apaga con color.
                    gradientRatio = 0f,
                ) { indice ->
                    val distancia = abs(indice - estado.selectedOptionIndex)
                    val texto = formatearPeso(kilosDeIndice(indice)).removeSuffix(" kg")
                    when (distancia) {
                        0 -> Row(
                            modifier = Modifier
                                .background(
                                    if (moviendo) MaterialTheme.colorScheme.surfaceContainerHigh
                                    else MaterialTheme.colorScheme.surfaceContainer,
                                    RoundedCornerShape(50),
                                )
                                .then(
                                    if (!moviendo) Modifier
                                    else Modifier.border(
                                        du(1f),
                                        MaterialTheme.colorScheme.primary,
                                        RoundedCornerShape(50),
                                    )
                                )
                                .padding(horizontal = du(22f), vertical = du(4f)),
                            verticalAlignment = Alignment.Bottom,
                        ) {
                            Text(
                                text = texto,
                                fontSize = duSp(46f),
                                lineHeight = duSp(48f),
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Text(
                                text = stringResource(R.string.kilos),
                                fontSize = duSp(16f),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(start = du(6f), bottom = du(6f)),
                            )
                        }
                        1 -> Text(text = texto, fontSize = duSp(27f), lineHeight = duSp(34f), color = VECINO_CERCA)
                        else -> Text(text = texto, fontSize = duSp(22f), lineHeight = duSp(29f), color = VECINO_LEJOS)
                    }
                }

                // Mientras se mueve, lo útil es cuánto cambió respecto de la vez pasada; en
                // reposo, contra qué estoy comparando.
                val pie = when {
                    moviendo && diferencia != null && diferencia != 0.0 -> {
                        val signo = if (diferencia > 0) "+" else "−"
                        stringResource(R.string.vs_ultimo, signo + formatearPeso(abs(diferencia)))
                    }
                    ultimo != null && plan != null ->
                        stringResource(R.string.ultimo_y_plan, ultimo.removeSuffix(" kg"), plan.removeSuffix(" kg"))
                    ultimo != null -> stringResource(R.string.ultimo_suelto, ultimo.removeSuffix(" kg"))
                    else -> null
                }
                if (pie != null) {
                    Text(
                        text = pie,
                        fontSize = duSp(15f),
                        fontWeight = if (moviendo) FontWeight.Medium else FontWeight.Normal,
                        color =
                            if (moviendo && diferencia != null && diferencia != 0.0) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = du(8f)),
                    )
                }
            }

            // La marca del costado dice que la rueda se mueve; se agranda mientras gira.
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = du(if (moviendo) 4f else 6f))
                    .size(
                        width = du(if (moviendo) 6f else 4f),
                        height = du(if (moviendo) 104f else 70f),
                    )
                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(50)),
            )

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
                    .clickable { onGuardar(kilos) }
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
