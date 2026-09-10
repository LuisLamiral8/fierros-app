package com.luis.fierros.presentation

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
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
 * Lista con título y un botón por opción. La usan semanas, días, ejercicios y ajustes.
 * Si se pasa [onAjustes], abajo de todo aparece la tuerquita.
 */
@Composable
fun PantallaLista(
    titulo: String,
    opciones: List<Opcion> = emptyList(),
    anchoEtiqueta: Dp = 36.dp,
    mensaje: String? = null,
    onClick: (indice: Int) -> Unit = {},
    onAjustes: (() -> Unit)? = null,
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
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            items(opciones.size) { indice ->
                Button(
                    onClick = { onClick(indice) },
                    modifier = Modifier.fillMaxWidth()
                        .transformedHeight(this, transformationSpec),
                    transformation = SurfaceTransformation(transformationSpec),
                ) {
                    val opcion = opciones[indice]
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

    if (onAjustes == null) {
        ScreenScaffold(scrollState = listState, content = contenido)
    } else {
        ScreenScaffold(
            scrollState = listState,
            edgeButton = {
                EdgeButton(onClick = onAjustes, buttonSize = EdgeButtonSize.Small) {
                    Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.ajustes))
                }
            },
            content = contenido,
        )
    }
}

/** Una pantalla = un ejercicio: letra, nombre grande, series × reps, peso y nota. */
@Composable
fun PantallaEjercicio(ejercicio: Ejercicio) {
    val listState = rememberTransformingLazyColumnState()
    ScreenScaffold(scrollState = listState) { contentPadding ->
        TransformingLazyColumn(contentPadding = contentPadding, state = listState) {
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
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
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
        PantallaEjercicio(Ejercicio("A2", "Puente de glúteos a una pierna", 3, "20", "0 kg", "por pierna"))
    }
}
