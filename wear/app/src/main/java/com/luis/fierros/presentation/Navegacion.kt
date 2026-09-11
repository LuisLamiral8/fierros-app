package com.luis.fierros.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavBackStackEntry
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import com.luis.fierros.R
import com.luis.fierros.data.CargaRutina
import com.luis.fierros.data.Mesociclo
import com.luis.fierros.data.ResultadoSync
import com.luis.fierros.presentation.theme.FierrosTheme

@Composable
fun WearApp(viewModel: RutinaViewModel) {
    FierrosTheme {
        AppScaffold {
            val titulo = stringResource(R.string.app_name)
            when (val c = viewModel.carga) {
                null -> PantallaLista(titulo, mensaje = stringResource(R.string.cargando))
                // Sin rutina (o ilegible): se puede sincronizar desde acá mismo.
                CargaRutina.SinRutina, is CargaRutina.Error -> {
                    val motivo = if (c is CargaRutina.Error) c.mensaje else stringResource(R.string.sin_rutina)
                    PantallaLista(
                        titulo = titulo,
                        mensaje = listOfNotNull(motivo, textoSincronizacion(viewModel.sincronizacion))
                            .joinToString("\n\n"),
                        opciones = listOf(Opcion(stringResource(R.string.sincronizar_datos))),
                        onClick = { viewModel.sincronizar() },
                    )
                }
                is CargaRutina.Ok -> Navegacion(c.mesociclo, viewModel)
            }
        }
    }
}

// Rutas: semanas → dias/{semana} → ejercicios/{semana}/{dia} → ejercicio/{semana}/{dia}/{indice}.
// Desde semanas, la tuerquita abre ajustes.
// Volver atrás es deslizar hacia la derecha (swipe to dismiss).
@Composable
private fun Navegacion(mesociclo: Mesociclo, viewModel: RutinaViewModel) {
    val navController = rememberSwipeDismissableNavController()
    // El grafo de rutas se arma una sola vez. Las pantallas leen siempre la rutina más reciente,
    // así que sincronizar no reinicia la navegación ni te saca de la pantalla en la que estás.
    val rutina by rememberUpdatedState(mesociclo)

    SwipeDismissableNavHost(navController = navController, startDestination = "semanas") {
        composable("semanas") {
            PantallaLista(
                titulo = stringResource(R.string.semanas),
                opciones = rutina.semanas.map { Opcion(stringResource(R.string.semana, it.numero)) },
                onClick = { i -> navController.navigate("dias/${rutina.semanas[i].numero}") },
                onAjustes = { navController.navigate("ajustes") },
            )
        }

        composable("ajustes") {
            LaunchedEffect(Unit) { viewModel.olvidarResultado() }
            PantallaLista(
                titulo = stringResource(R.string.ajustes),
                mensaje = textoSincronizacion(viewModel.sincronizacion),
                opciones = listOf(Opcion(stringResource(R.string.sincronizar_datos))),
                onClick = { viewModel.sincronizar() },
            )
        }

        composable("dias/{semana}") { entry ->
            val semana = rutina.semana(entry.int("semana"))
            if (semana == null) {
                NoEncontrado()
            } else {
                PantallaLista(
                    titulo = stringResource(R.string.semana, semana.numero),
                    opciones = semana.dias.map { dia ->
                        val numero = stringResource(R.string.dia, dia.numero)
                        if (dia.nombre == null) Opcion(numero) else Opcion(dia.nombre, etiqueta = numero)
                    },
                    anchoEtiqueta = 60.dp,
                    onClick = { i ->
                        navController.navigate("ejercicios/${semana.numero}/${semana.dias[i].numero}")
                    },
                )
            }
        }

        composable("ejercicios/{semana}/{dia}") { entry ->
            val semana = rutina.semana(entry.int("semana"))
            val dia = semana?.dia(entry.int("dia"))
            if (semana == null || dia == null) {
                NoEncontrado()
            } else {
                PantallaLista(
                    titulo = stringResource(R.string.titulo_dia, semana.numero, dia.numero),
                    opciones = dia.ejercicios.map { Opcion(it.nombre, etiqueta = it.letra) },
                    onClick = { i -> navController.navigate("ejercicio/${semana.numero}/${dia.numero}/$i") },
                )
            }
        }

        // Las flechas cambian de ejercicio dentro de esta misma pantalla, sin apilar rutas:
        // deslizar atrás siempre vuelve a la lista del día. Nunca pasan a otro día.
        composable("ejercicio/{semana}/{dia}/{indice}") { entry ->
            val ejercicios = rutina.semana(entry.int("semana"))?.dia(entry.int("dia"))?.ejercicios
            val inicial = entry.int("indice")
            if (ejercicios == null || inicial == null || inicial !in ejercicios.indices) {
                NoEncontrado()
            } else {
                var indice by rememberSaveable { mutableIntStateOf(inicial) }
                // key: al cambiar de ejercicio la pantalla arranca de cero (scroll arriba).
                key(indice) {
                    PantallaEjercicio(
                        ejercicio = ejercicios[indice],
                        onAnterior = if (indice > 0) ({ indice -= 1 }) else null,
                        onSiguiente = if (indice < ejercicios.lastIndex) ({ indice += 1 }) else null,
                    )
                }
            }
        }
    }
}

/** El mensaje que corresponde al estado de la sincronización, o null si no hay nada que decir. */
@Composable
private fun textoSincronizacion(estado: EstadoSincronizacion): String? =
    when (estado) {
        EstadoSincronizacion.Inactiva -> null
        EstadoSincronizacion.EnCurso -> stringResource(R.string.sincronizando)
        is EstadoSincronizacion.Terminada -> when (val r = estado.resultado) {
            is ResultadoSync.Ok -> stringResource(R.string.sync_ok)
            ResultadoSync.SinConexion -> stringResource(R.string.sync_sin_conexion)
            ResultadoSync.SinRutinaEnServidor -> stringResource(R.string.sync_sin_rutina)
            is ResultadoSync.ErrorServidor -> stringResource(R.string.sync_error_servidor, r.codigo)
            ResultadoSync.RutinaInvalida -> stringResource(R.string.sync_invalida)
            ResultadoSync.ErrorAlGuardar -> stringResource(R.string.sync_error_guardar)
        }
    }

@Composable
private fun NoEncontrado() {
    PantallaLista(stringResource(R.string.app_name), mensaje = stringResource(R.string.no_encontrado))
}

// Los parámetros de ruta llegan como String, igual que un @PathVariable sin conversión.
private fun NavBackStackEntry.int(nombre: String): Int? =
    arguments?.getString(nombre)?.toIntOrNull()
