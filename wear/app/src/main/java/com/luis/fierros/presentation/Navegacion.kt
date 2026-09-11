package com.luis.fierros.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavBackStackEntry
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import com.luis.fierros.R
import com.luis.fierros.data.CargaRutina
import com.luis.fierros.data.ResultadoSync
import com.luis.fierros.presentation.theme.FierrosTheme
import kotlinx.coroutines.delay

@Composable
fun WearApp(viewModel: RutinaViewModel) {
    var aviso by remember { mutableStateOf("") }
    var avisoVisible by remember { mutableStateOf(false) }
    var avisoDuracionMs by remember { mutableLongStateOf(2_000L) }

    // El resultado de cada sincronización se avisa con el cartel de abajo, que desaparece solo:
    // 2 segundos si salió bien, 3 si hubo un error (para que dé tiempo a leerlo).
    val terminada = viewModel.sincronizacion as? EstadoSincronizacion.Terminada
    val textoTerminada = terminada?.let { textoResultado(it.resultado) }
    LaunchedEffect(terminada) {
        if (terminada != null && textoTerminada != null) {
            aviso = textoTerminada
            avisoDuracionMs = if (terminada.resultado is ResultadoSync.Ok) 2_000L else 3_000L
            avisoVisible = true
            viewModel.olvidarResultado()
        }
    }
    // Una URL inválida en Ajustes -> Servidor usa el mismo cartel (es un error: 3 segundos).
    val textoUrlInvalida = stringResource(R.string.url_invalida)
    LaunchedEffect(viewModel.urlInvalida) {
        if (viewModel.urlInvalida) {
            aviso = textoUrlInvalida
            avisoDuracionMs = 3_000L
            avisoVisible = true
            viewModel.olvidarUrlInvalida()
        }
    }
    LaunchedEffect(avisoVisible, aviso) {
        if (avisoVisible) {
            delay(avisoDuracionMs)
            avisoVisible = false
        }
    }

    FierrosTheme {
        AppScaffold {
            Box(Modifier.fillMaxSize()) {
                Navegacion(viewModel)

                // Por encima de cualquier pantalla.
                AvisoInferior(
                    aviso,
                    visible = avisoVisible,
                    onCerrar = { avisoVisible = false },
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            }
        }
    }
}

// Rutas: semanas → dias/{semana} → ejercicios/{semana}/{dia} → ejercicio/{semana}/{dia}/{indice}.
// Desde semanas (haya rutina o no), la tuerquita abre ajustes → servidor.
// Volver atrás es deslizar hacia la derecha (swipe to dismiss).
//
// El grafo de rutas se arma una sola vez y cada pantalla lee la rutina del ViewModel: al
// sincronizar se actualizan solas, sin reiniciar la navegación.
@Composable
private fun Navegacion(viewModel: RutinaViewModel) {
    val navController = rememberSwipeDismissableNavController()

    SwipeDismissableNavHost(navController = navController, startDestination = "semanas") {
        composable("semanas") {
            val titulo = stringResource(R.string.app_name)
            when (val c = viewModel.carga) {
                null -> PantallaLista(titulo, mensaje = stringResource(R.string.cargando))
                // Sin rutina (o ilegible): se puede sincronizar desde acá, y Ajustes queda a mano
                // por si hay que cambiar el servidor.
                CargaRutina.SinRutina, is CargaRutina.Error -> {
                    val motivo = if (c is CargaRutina.Error) c.mensaje else stringResource(R.string.sin_rutina)
                    PantallaLista(
                        titulo = titulo,
                        mensaje = motivo,
                        opciones = listOf(Opcion(textoBotonSincronizar(viewModel.sincronizacion))),
                        onClick = { viewModel.sincronizar() },
                        botonInferior = { BotonAjustes { navController.navigate("ajustes") } },
                    )
                }
                is CargaRutina.Ok -> PantallaLista(
                    titulo = stringResource(R.string.semanas),
                    opciones = c.mesociclo.semanas.map { Opcion(stringResource(R.string.semana, it.numero)) },
                    onClick = { i -> navController.navigate("dias/${c.mesociclo.semanas[i].numero}") },
                    botonInferior = { BotonAjustes { navController.navigate("ajustes") } },
                )
            }
        }

        composable("ajustes") {
            PantallaLista(
                titulo = stringResource(R.string.ajustes),
                opciones = listOf(
                    Opcion(textoBotonSincronizar(viewModel.sincronizacion)),
                    Opcion(stringResource(R.string.servidor)),
                ),
                onClick = { i -> if (i == 0) viewModel.sincronizar() else navController.navigate("servidor") },
            )
        }

        composable("servidor") {
            PantallaServidor(viewModel)
        }

        composable("dias/{semana}") { entry ->
            val semana = viewModel.rutina?.semana(entry.int("semana"))
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
            val semana = viewModel.rutina?.semana(entry.int("semana"))
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
            val ejercicios = viewModel.rutina?.semana(entry.int("semana"))?.dia(entry.int("dia"))?.ejercicios
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

/** Mientras sincroniza, el propio botón lo dice. */
@Composable
private fun textoBotonSincronizar(estado: EstadoSincronizacion): String =
    if (estado == EstadoSincronizacion.EnCurso) {
        stringResource(R.string.sincronizando)
    } else {
        stringResource(R.string.sincronizar_datos)
    }

/** Lo que dice el cartel de abajo cuando termina una sincronización. */
@Composable
private fun textoResultado(resultado: ResultadoSync): String =
    when (resultado) {
        is ResultadoSync.Ok -> stringResource(R.string.sync_ok)
        ResultadoSync.SinConexion -> stringResource(R.string.sync_sin_conexion)
        ResultadoSync.SinRutinaEnServidor -> stringResource(R.string.sync_sin_rutina)
        is ResultadoSync.ErrorServidor -> stringResource(R.string.sync_error_servidor, resultado.codigo)
        ResultadoSync.RutinaInvalida -> stringResource(R.string.sync_invalida)
        ResultadoSync.ErrorAlGuardar -> stringResource(R.string.sync_error_guardar)
    }

@Composable
private fun NoEncontrado() {
    PantallaLista(stringResource(R.string.app_name), mensaje = stringResource(R.string.no_encontrado))
}

// Los parámetros de ruta llegan como String, igual que un @PathVariable sin conversión.
private fun NavBackStackEntry.int(nombre: String): Int? =
    arguments?.getString(nombre)?.toIntOrNull()
