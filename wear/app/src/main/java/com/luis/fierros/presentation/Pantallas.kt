package com.luis.fierros.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.EdgeButton
import androidx.wear.compose.material3.EdgeButtonSize
import androidx.wear.compose.material3.FilledTonalButton
import androidx.wear.compose.material3.IconButton
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import androidx.wear.compose.ui.tooling.preview.WearPreviewDevices
import com.luis.fierros.R
import com.luis.fierros.data.Ejercicio
import com.luis.fierros.presentation.theme.FierrosTheme

// Todas las medidas de este archivo son unidades de la maqueta (lienzo de 384); las convierte
// du()/duSp() según el diámetro real de la pantalla. Ver Medidas.kt.

/**
 * Un botón de la lista. La [etiqueta] (el número del día, la letra del ejercicio) va en su
 * propia columna. [grupo] marca los ejercicios de una misma superserie: los que comparten
 * grupo se dibujan pegados, con una barra de acento al costado. [principal] lo pinta lavanda,
 * para la acción principal de la pantalla; el resto va gris oscuro.
 */
data class Opcion(
    val texto: String,
    val etiqueta: String? = null,
    val grupo: String? = null,
    val principal: Boolean = false,
)

/**
 * Lista con título y un botón por opción. La usan semanas, días, ejercicios, ajustes y servidor.
 * [botonInferior] va abajo de todo, con la forma del borde de la pantalla (ver [BotonAjustes]).
 *
 * Los tamaños por defecto son los de `semanas.html`; días y ejercicios ajustan lo suyo.
 */
@Composable
fun PantallaLista(
    titulo: String,
    opciones: List<Opcion> = emptyList(),
    anchoEtiqueta: Float = 44f,
    colorEtiqueta: Color? = null,
    tamTitulo: Float = 17f,
    tamTexto: Float = 19f,
    altoBoton: Float = 52f,
    padLateral: Float = 30f,
    padBoton: Float = 18f,
    mensaje: String? = null,
    onClick: (indice: Int) -> Unit = {},
    botonInferior: (@Composable BoxScope.() -> Unit)? = null,
) {
    val listState = rememberTransformingLazyColumnState()
    val transformationSpec = rememberTransformationSpec()
    val contenido: @Composable BoxScope.(PaddingValues) -> Unit = { contentPadding ->
        TransformingLazyColumn(
            contentPadding = PaddingValues(
                start = du(padLateral),
                end = du(padLateral),
                top = contentPadding.calculateTopPadding(),
                bottom = contentPadding.calculateBottomPadding(),
            ),
            verticalArrangement = Arrangement.spacedBy(du(8f)),
            state = listState,
        ) {
            item {
                Text(
                    text = titulo,
                    textAlign = TextAlign.Center,
                    fontSize = duSp(tamTitulo),
                    lineHeight = duSp(tamTitulo * 1.15f),
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = du(22f), bottom = du(4f))
                        .transformedHeight(this, transformationSpec),
                )
            }
            if (mensaje != null) {
                item {
                    Text(
                        text = mensaje,
                        textAlign = TextAlign.Center,
                        fontSize = duSp(17f),
                        lineHeight = duSp(23f),
                        modifier = Modifier.fillMaxWidth().padding(bottom = du(14f)),
                    )
                }
            }
            items(opciones.size) { indice ->
                val opcion = opciones[indice]
                val enGrupo = opcion.grupo != null
                val primero = opcion.grupo != opciones.getOrNull(indice - 1)?.grupo
                val ultimo = opcion.grupo != opciones.getOrNull(indice + 1)?.grupo
                val acento = MaterialTheme.colorScheme.primary
                val sobra = du(8f)
                val recorte = du(3f)
                val grosor = du(2.5f)

                // La barra de la superserie se pinta por ítem, no como un contenedor que abarque
                // los dos: así no pelea con la animación de la lista al scrollear. Para que no se
                // vea partida, en la punta de adentro se dibuja más allá del borde del ítem y
                // tapa el hueco que la lista deja entre uno y otro.
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .transformedHeight(this, transformationSpec)
                        .then(
                            if (!enGrupo) Modifier else Modifier.drawBehind {
                                val arriba = if (primero) recorte.toPx() else -sobra.toPx()
                                val abajo =
                                    if (ultimo) size.height - recorte.toPx() else size.height + sobra.toPx()
                                drawRoundRect(
                                    color = acento,
                                    topLeft = Offset(0f, arriba),
                                    size = Size(grosor.toPx(), abajo - arriba),
                                    cornerRadius = CornerRadius(grosor.toPx() / 2, grosor.toPx() / 2),
                                )
                            },
                        )
                        .padding(start = if (enGrupo) du(8f) else 0.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val contenidoBoton: @Composable RowScope.() -> Unit = {
                        if (opcion.etiqueta != null) {
                            Text(
                                text = opcion.etiqueta,
                                fontSize = duSp(16f),
                                fontWeight = FontWeight.Medium,
                                color = colorEtiqueta ?: MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.width(du(anchoEtiqueta)),
                            )
                            Text(
                                text = opcion.texto,
                                fontSize = duSp(tamTexto),
                                lineHeight = duSp(tamTexto * 1.15f),
                                modifier = Modifier.weight(1f),
                            )
                        } else {
                            // Sin etiqueta el texto va centrado (la lista de semanas).
                            Text(
                                text = opcion.texto,
                                fontSize = duSp(tamTexto),
                                lineHeight = duSp(tamTexto * 1.15f),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                    val forma =
                        if (enGrupo) {
                            RoundedCornerShape(
                                topStart = if (primero) du(22f) else du(6f),
                                topEnd = if (primero) du(22f) else du(6f),
                                bottomStart = if (ultimo) du(22f) else du(6f),
                                bottomEnd = if (ultimo) du(22f) else du(6f),
                            )
                        } else {
                            RoundedCornerShape(du(26f))
                        }
                    val modBoton = Modifier.fillMaxWidth().heightIn(min = du(altoBoton))
                    val padContenido = PaddingValues(horizontal = du(padBoton), vertical = du(4f))
                    if (opcion.principal) {
                        Button(
                            onClick = { onClick(indice) },
                            modifier = modBoton,
                            shape = forma,
                            contentPadding = padContenido,
                            transformation = SurfaceTransformation(transformationSpec),
                            content = contenidoBoton,
                        )
                    } else {
                        FilledTonalButton(
                            onClick = { onClick(indice) },
                            modifier = modBoton,
                            shape = forma,
                            contentPadding = padContenido,
                            transformation = SurfaceTransformation(transformationSpec),
                            content = contenidoBoton,
                        )
                    }
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

/**
 * La tuerquita de abajo de todo, con la forma del borde de la pantalla.
 *
 * Ojo con esto al comparar contra las maquetas: el botón de borde de Wear se **colapsa**
 * mientras la lista no llegó al final, y ahí se ve una pastilla chata sin ícono. Al scrollear
 * hasta abajo se despliega y aparece el engranaje. No es un bug: la maqueta lo dibuja siempre
 * desplegado porque es una imagen fija.
 */
@Composable
fun BotonAjustes(onClick: () -> Unit) {
    EdgeButton(onClick = onClick, buttonSize = EdgeButtonSize.Small) {
        Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.ajustes))
    }
}

/**
 * La pastilla con el último peso levantado: "último  32,5 kg". Es el dato que más importa
 * mirar antes de una serie, así que va a la vista y sin tocar nada.
 */
@Composable
fun PastillaUltimo(peso: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(50))
            .padding(horizontal = du(14f), vertical = du(6f)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.ultimo),
            fontSize = duSp(15f),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.width(du(8f)))
        Text(text = peso, fontSize = duSp(16f), fontWeight = FontWeight.Medium)
    }
}

/**
 * Una pantalla = un ejercicio: letra, nombre grande, series × reps, lo último que levanté y la
 * nota. Flechas a los costados para anterior/siguiente; si el callback es null, no aparece.
 *
 * El contenido va centrado, como en la maqueta; si no entra (reps largas con nota y pastilla),
 * scrollea. El peso del plan no se muestra: viene siempre en "0 kg" y ese renglón es el espacio
 * que necesita el registro (ver planificacion-it-3.md).
 */
@Composable
fun PantallaEjercicio(
    ejercicio: Ejercicio,
    ultimo: String? = null,
    registradoHoy: String? = null,
    onRegistrar: (() -> Unit)? = null,
    onAnterior: (() -> Unit)? = null,
    onSiguiente: (() -> Unit)? = null,
) {
    val reps = formatearReps(ejercicio.series, ejercicio.reps)
    val largo = reps is Reps.EnDos
    ScreenScaffold {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val alto = maxHeight
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .heightIn(min = alto)
                    // Márgenes laterales propios para que el texto no quede bajo las flechas.
                    .padding(horizontal = du(if (largo || registradoHoy != null) 58f else 62f)),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(du(if (largo) 7f else 10f), Alignment.CenterVertically),
            ) {
                Text(
                    text = ejercicio.letra,
                    fontSize = duSp(16f),
                    fontWeight = FontWeight.Medium,
                    letterSpacing = duSp(1.6f),
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = ejercicio.nombre,
                    fontSize = duSp(if (largo) 17f else 18f),
                    lineHeight = duSp(if (largo) 20f else 22f),
                    textAlign = TextAlign.Center,
                )
                when (reps) {
                    is Reps.EnUnRenglon ->
                        Text(
                            text = reps.texto,
                            fontSize = duSp(42f),
                            lineHeight = duSp(44f),
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center,
                        )
                    // Reps largas: dos renglones parejos y el "N SERIES" como etiqueta abajo.
                    is Reps.EnDos -> {
                        Text(
                            text = reps.primero,
                            fontSize = duSp(36f),
                            lineHeight = duSp(38f),
                            fontWeight = FontWeight.Medium,
                        )
                        Text(
                            text = reps.segundo,
                            fontSize = duSp(36f),
                            lineHeight = duSp(38f),
                            fontWeight = FontWeight.Medium,
                        )
                        Text(
                            text = stringResource(R.string.series_cantidad, reps.series),
                            fontSize = duSp(15f),
                            fontWeight = FontWeight.Medium,
                            letterSpacing = duSp(1.2f),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                ejercicio.nota?.let { nota ->
                    Text(
                        text = nota,
                        fontSize = duSp(16f),
                        lineHeight = duSp(19f),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
                if (registradoHoy != null) {
                    // Ya registrado. El valor de hoy se distingue por el tilde y el borde, no
                    // solo por el color; tocarlo vuelve a la rueda para corregirlo.
                    Row(
                        modifier = Modifier
                            .background(
                                MaterialTheme.colorScheme.surfaceContainerHigh,
                                RoundedCornerShape(50),
                            )
                            .border(du(1f), MaterialTheme.colorScheme.primary, RoundedCornerShape(50))
                            .then(
                                if (onRegistrar == null) Modifier
                                else Modifier.clickable(onClick = onRegistrar),
                            )
                            .padding(horizontal = du(16f), vertical = du(8f)),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Filled.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(du(16f)),
                        )
                        Spacer(Modifier.width(du(9f)))
                        Text(registradoHoy, fontSize = duSp(20f), fontWeight = FontWeight.Medium)
                        Spacer(Modifier.width(du(9f)))
                        Text(
                            text = stringResource(R.string.hoy),
                            fontSize = duSp(15f),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (ultimo != null) {
                        Text(
                            text = stringResource(R.string.ultimo_corregir, ultimo),
                            fontSize = duSp(15f),
                            lineHeight = duSp(18f),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }
                } else {
                    if (ultimo != null) PastillaUltimo(ultimo)
                    if (onRegistrar != null) {
                        Button(
                            onClick = onRegistrar,
                            modifier = Modifier.heightIn(min = du(48f)),
                            shape = RoundedCornerShape(50),
                            contentPadding = PaddingValues(horizontal = du(28f), vertical = du(4f)),
                        ) {
                            Text(
                                text = stringResource(R.string.registrar),
                                fontSize = duSp(17f),
                                fontWeight = FontWeight.Medium,
                            )
                        }
                    }
                }
            }

            onAnterior?.let {
                IconButton(
                    onClick = it,
                    modifier = Modifier.align(Alignment.CenterStart).size(du(44f)),
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = stringResource(R.string.anterior),
                        modifier = Modifier.size(du(26f)),
                    )
                }
            }
            onSiguiente?.let {
                IconButton(
                    onClick = it,
                    modifier = Modifier.align(Alignment.CenterEnd).size(du(44f)),
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = stringResource(R.string.siguiente),
                        modifier = Modifier.size(du(26f)),
                    )
                }
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
fun AvisoInferior(
    texto: String,
    visible: Boolean,
    onCerrar: () -> Unit,
    modifier: Modifier = Modifier,
    esError: Boolean = false,
) {
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
                .heightIn(min = du(44f))
                .background(
                    MaterialTheme.colorScheme.surfaceContainerHigh,
                    RoundedCornerShape(topStart = du(24f), topEnd = du(24f)),
                )
                .clickable(onClick = onCerrar)
                .padding(top = du(14f), bottom = du(30f)),
        ) {
            // Hasta dos renglones angostos: si el texto es largo, la franja crece hacia arriba,
            // donde la U es más ancha, en vez de cortarse contra la curva.
            Text(
                text = texto,
                fontSize = duSp(18f),
                lineHeight = duSp(22f),
                fontWeight = FontWeight.Medium,
                color = if (esError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                maxLines = 2,
                modifier = Modifier.widthIn(max = du(290f)),
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
@Composable
fun PantallaListaPreview() {
    FierrosTheme {
        PantallaLista(
            titulo = "Semana 1 · Día 2",
            opciones = listOf(
                Opcion("Band pull apart", etiqueta = "A1", grupo = "A"),
                Opcion("Puente de glúteos a una pierna", etiqueta = "A2", grupo = "A"),
                Opcion("Press militar con barra", etiqueta = "B"),
            ),
            anchoEtiqueta = 28f,
            tamTitulo = 16f,
            tamTexto = 16f,
            altoBoton = 48f,
            padLateral = 24f,
        )
    }
}

@WearPreviewDevices
@Composable
fun PantallaEjercicioPreview() {
    FierrosTheme {
        PantallaEjercicio(
            Ejercicio("A2", "Puente de glúteos a una pierna", 3, "20", "0 kg", "por pierna"),
            ultimo = "32,5 kg",
            onAnterior = {},
            onSiguiente = {},
        )
    }
}

@WearPreviewDevices
@Composable
fun PantallaEjercicioLargoPreview() {
    FierrosTheme {
        PantallaEjercicio(
            Ejercicio("B", "Db floor press", 5, "20-12-10-8-6", "0 kg", "serie descendente"),
            ultimo = "18 kg c/mano",
        )
    }
}
