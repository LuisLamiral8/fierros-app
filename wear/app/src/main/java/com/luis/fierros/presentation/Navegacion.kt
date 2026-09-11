package com.luis.fierros.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
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
                titulo = stringResource(R.string.semanas),
                opciones = mesociclo.semanas.map { Opcion(stringResource(R.string.semana, it.numero)) },
                onClick = { i -> navController.navigate("dias/${mesociclo.semanas[i].numero}") },
                onAjustes = { navController.navigate("ajustes") },
            )
        }

        composable("ajustes") {
            PantallaLista(
                titulo = stringResource(R.string.ajustes),
                opciones = listOf(Opcion(stringResource(R.string.sincronizar_datos))),
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
            val semana = mesociclo.semana(entry.int("semana"))
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
            val ejercicios = mesociclo.semana(entry.int("semana"))?.dia(entry.int("dia"))?.ejercicios
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

@Composable
private fun NoEncontrado() {
    PantallaLista(stringResource(R.string.app_name), mensaje = stringResource(R.string.no_encontrado))
}

// Los parámetros de ruta llegan como String, igual que un @PathVariable sin conversión.
private fun NavBackStackEntry.int(nombre: String): Int? =
    arguments?.getString(nombre)?.toIntOrNull()
