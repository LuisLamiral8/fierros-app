package com.luis.fierros.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.EdgeButton
import androidx.wear.compose.material3.EdgeButtonSize
import androidx.wear.compose.material3.IconButton
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import androidx.wear.compose.ui.tooling.preview.WearPreviewDevices
import androidx.wear.compose.ui.tooling.preview.WearPreviewFontScales
import com.luis.fierros.R
import com.luis.fierros.data.Ejercicio
import com.luis.fierros.presentation.theme.FierrosTheme

/** Un botón de la lista. La [etiqueta] (la letra del ejercicio) va en su propia columna. */
data class Opcion(val texto: String, val etiqueta: String? = null)

/**
 * Lista con título y un botón por opción. La usan semanas, días, ejercicios, ajustes y servidor.
 * [botonInferior] va abajo de todo, con la forma del borde de la pantalla (ver [BotonAjustes]).
 */
@Composable
fun PantallaLista(
    titulo: String,
    opciones: List<Opcion> = emptyList(),
    anchoEtiqueta: Dp = 36.dp,
    mensaje: String? = null,
    onClick: (indice: Int) -> Unit = {},
    botonInferior: (@Composable BoxScope.() -> Unit)? = null,
) {
    val listState = rememberTransformingLazyColumnState()
    val transformationSpec = rememberTransformationSpec()
    val contenido: @Composable BoxScope.(PaddingValues) -> Unit = { contentPadding ->
        TransformingLazyColumn(contentPadding = contentPadding, state = listState) {
            item {
                ListHeader(
                    modifier =
                        Modifier.fillMaxWidth().transformedHeight(this, transformationSpec),
                    transformation = SurfaceTransformation(transformationSpec),
                ) {
                    Text(text = titulo, textAlign = TextAlign.Center)
                }
            }
            if (mensaje != null) {
                item {
                    Text(
                        text = mensaje,
                        textAlign = TextAlign.Center,
                        // Aire entre el mensaje y los botones que vienen debajo.
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    )
                }
            }
            items(opciones.size) { indice ->
                val opcion = opciones[indice]
                Button(
                    onClick = { onClick(indice) },
                    modifier = Modifier.fillMaxWidth()
                        .transformedHeight(this, transformationSpec),
                    transformation = SurfaceTransformation(transformationSpec),
                ) {
                    if (opcion.etiqueta != null) {
                        Text(
                            text = opcion.etiqueta,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.width(anchoEtiqueta),
                        )
                    }
                    Text(opcion.texto, modifier = Modifier.weight(1f))
                }
            }
        }
    }

    if (botonInferior == null) {
        ScreenScaffold(scrollState = listState, content = contenido)
    } else {
        ScreenScaffold(scrollState = listState, edgeButton = botonInferior, content = contenido)
    }
}

/** La tuerquita de abajo de todo, con la forma del borde de la pantalla. */
@Composable
fun BotonAjustes(onClick: () -> Unit) {
    EdgeButton(onClick = onClick, buttonSize = EdgeButtonSize.Small) {
        Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.ajustes))
    }
}


/**
 * Una pantalla = un ejercicio: letra, nombre grande, series × reps, peso y nota.
 * Flechas a los costados para anterior/siguiente; si el callback es null, la flecha no aparece.
 */
@Composable
fun PantallaEjercicio(
    ejercicio: Ejercicio,
    onAnterior: (() -> Unit)? = null,
    onSiguiente: (() -> Unit)? = null,
) {
    val listState = rememberTransformingLazyColumnState()
    ScreenScaffold(scrollState = listState) { contentPadding ->
        TransformingLazyColumn(
            // Márgenes laterales propios para que el texto no quede debajo de las flechas.
            contentPadding = PaddingValues(
                start = 36.dp,
                end = 36.dp,
                top = contentPadding.calculateTopPadding(),
                bottom = contentPadding.calculateBottomPadding(),
            ),
            state = listState,
        ) {
            item {
                Text(
                    text = ejercicio.letra,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            item {
                Text(
                    text = ejercicio.nombre,
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            item {
                Text(
                    text = stringResource(R.string.series_por_reps, ejercicio.series, ejercicio.reps),
                    style = MaterialTheme.typography.displaySmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
            }
            item {
                Text(text = ejercicio.peso, style = MaterialTheme.typography.bodyLarge)
            }
            ejercicio.nota?.let { nota ->
                item {
                    Text(
                        text = nota,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        onAnterior?.let {
            IconButton(onClick = it, modifier = Modifier.align(Alignment.CenterStart)) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = stringResource(R.string.anterior))
            }
        }
        onSiguiente?.let {
            IconButton(onClick = it, modifier = Modifier.align(Alignment.CenterEnd)) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = stringResource(R.string.siguiente))
            }
        }
    }
}

/**
 * Aviso abajo de todo, como los avisos del celular: una franja pegada al borde inferior con las
 * esquinas de arriba redondeadas. La parte de abajo la curva la propia pantalla redonda, así que
 * queda con forma de U. El texto va arriba, donde hay más ancho. Entra deslizando desde abajo y
 * sale igual; cuánto tiempo se ve lo decide quien lo muestra, y tocarlo lo cierra antes.
 */
@Composable
fun AvisoInferior(texto: String, visible: Boolean, onCerrar: () -> Unit, modifier: Modifier = Modifier) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = slideInVertically { it } + fadeIn(),
        exit = slideOutVertically { it } + fadeOut(),
    ) {
        Box(
            contentAlignment = Alignment.TopCenter,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 44.dp)
                .background(
                    MaterialTheme.colorScheme.surfaceContainerHigh,
                    RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp),
                )
                .clickable(onClick = onCerrar)
                .padding(top = 10.dp, bottom = 18.dp),
        ) {
            // Hasta dos renglones angostos: si el texto es largo, la franja crece hacia arriba,
            // donde la U es más ancha, en vez de cortarse contra la curva.
            Text(
                text = texto,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                maxLines = 2,
                modifier = Modifier.widthIn(max = 130.dp),
            )
        }
    }
}

@WearPreviewDevices
@Composable
fun AvisoInferiorPreview() {
    FierrosTheme {
        Box(Modifier.fillMaxSize()) {
            AvisoInferior(
                "¡Actualizado!",
                visible = true,
                onCerrar = {},
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}

@WearPreviewDevices
@WearPreviewFontScales
@Composable
fun PantallaListaPreview() {
    FierrosTheme {
        PantallaLista(
            titulo = "Semana 1 · Día 2",
            opciones = listOf(
                Opcion("Band pull apart", etiqueta = "A1"),
                Opcion("Puente de glúteos a una pierna", etiqueta = "A2"),
                Opcion("Press militar con barra", etiqueta = "B"),
            ),
        )
    }
}

@WearPreviewDevices
@Composable
fun PantallaEjercicioPreview() {
    FierrosTheme {
        PantallaEjercicio(
            Ejercicio("A2", "Puente de glúteos a una pierna", 3, "20", "0 kg", "por pierna"),
            onAnterior = {},
            onSiguiente = {},
        )
    }
}
