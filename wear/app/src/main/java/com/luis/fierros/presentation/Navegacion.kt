package com.luis.fierros.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavBackStackEntry
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import com.luis.fierros.R
import com.luis.fierros.data.CargaRutina
import com.luis.fierros.data.Mesociclo
import com.luis.fierros.data.RutinaRepository
import com.luis.fierros.presentation.theme.FierrosTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun WearApp() {
    val context = LocalContext.current
    // null mientras se lee el archivo; la lectura corre en un hilo de I/O.
    val carga by produceState<CargaRutina?>(initialValue = null) {
        value = withContext(Dispatchers.IO) { RutinaRepository.cargar(context) }
    }

    FierrosTheme {
        AppScaffold {
            val titulo = stringResource(R.string.app_name)
            when (val c = carga) {
                null -> PantallaLista(titulo, mensaje = stringResource(R.string.cargando))
                CargaRutina.SinRutina -> PantallaLista(titulo, mensaje = stringResource(R.string.sin_rutina))
                is CargaRutina.Error -> PantallaLista(titulo, mensaje = c.mensaje)
                is CargaRutina.Ok -> Navegacion(c.mesociclo)
            }
        }
    }
}

// Rutas: semanas → dias/{semana} → ejercicios/{semana}/{dia} → ejercicio/{semana}/{dia}/{indice}.
// Desde semanas, la tuerquita abre ajustes.
// Volver atrás es deslizar hacia la derecha (swipe to dismiss).
@Composable
private fun Navegacion(mesociclo: Mesociclo) {
    val navController = rememberSwipeDismissableNavController()

    SwipeDismissableNavHost(navController = navController, startDestination = "semanas") {
        composable("semanas") {
            PantallaLista(
                titulo = mesociclo.nombre,
                opciones = mesociclo.semanas.map { stringResource(R.string.semana, it.numero) },
                onClick = { i -> navController.navigate("dias/${mesociclo.semanas[i].numero}") },
                onAjustes = { navController.navigate("ajustes") },
            )
        }

        composable("ajustes") {
            PantallaLista(
                titulo = stringResource(R.string.ajustes),
                opciones = listOf(stringResource(R.string.sincronizar_datos)),
                onClick = { /* Sync: GET a la API del homelab, próximo paso */ },
            )
        }

        composable("dias/{semana}") { entry ->
            val semana = mesociclo.semana(entry.int("semana"))
            if (semana == null) {
                NoEncontrado()
            } else {
                PantallaLista(
                    titulo = stringResource(R.string.semana, semana.numero),
                    opciones = semana.dias.map { stringResource(R.string.dia, it.numero) },
                    onClick = { i ->
                        navController.navigate("ejercicios/${semana.numero}/${semana.dias[i].numero}")
                    },
                )
            }
        }

        composable("ejercicios/{semana}/{dia}") { entry ->
            val semana = mesociclo.semana(entry.int("semana"))
            val dia = semana?.dia(entry.int("dia"))
            if (semana == null || dia == null) {
                NoEncontrado()
            } else {
                PantallaLista(
                    titulo = stringResource(R.string.titulo_dia, semana.numero, dia.numero),
                    opciones = dia.ejercicios.map { "${it.letra}  ${it.nombre}" },
                    onClick = { i -> navController.navigate("ejercicio/${semana.numero}/${dia.numero}/$i") },
                )
            }
        }

        composable("ejercicio/{semana}/{dia}/{indice}") { entry ->
            val ejercicio = mesociclo.semana(entry.int("semana"))
                ?.dia(entry.int("dia"))
                ?.ejercicios?.getOrNull(entry.int("indice") ?: -1)
            if (ejercicio == null) NoEncontrado() else PantallaEjercicio(ejercicio)
        }
    }
}

@Composable
private fun NoEncontrado() {
    PantallaLista(stringResource(R.string.app_name), mensaje = stringResource(R.string.no_encontrado))
}

// Los parámetros de ruta llegan como String, igual que un @PathVariable sin conversión.
private fun NavBackStackEntry.int(nombre: String): Int? =
    arguments?.getString(nombre)?.toIntOrNull()
