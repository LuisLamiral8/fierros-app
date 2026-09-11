package com.luis.fierros.presentation

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.luis.fierros.BuildConfig
import com.luis.fierros.data.CargaRutina
import com.luis.fierros.data.ResultadoSync
import com.luis.fierros.data.RutinaRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface EstadoSincronizacion {
    data object Inactiva : EstadoSincronizacion
    data object EnCurso : EstadoSincronizacion
    data class Terminada(val resultado: ResultadoSync) : EstadoSincronizacion
}

/**
 * Estado de la app: la rutina y la sincronización. Sobrevive a que el sistema recree la
 * pantalla, y las pantallas se redibujan solas cuando cambia (lo más parecido a un @Service
 * con estado).
 */
class RutinaViewModel(application: Application) : AndroidViewModel(application) {

    /** null mientras se lee el archivo al arrancar. */
    var carga by mutableStateOf<CargaRutina?>(null)
        private set

    var sincronizacion by mutableStateOf<EstadoSincronizacion>(EstadoSincronizacion.Inactiva)
        private set

    init {
        viewModelScope.launch {
            carga = withContext(Dispatchers.IO) { RutinaRepository.cargar(getApplication<Application>()) }
        }
    }

    fun sincronizar() {
        // Un segundo toque mientras hay una en curso no hace nada.
        if (sincronizacion == EstadoSincronizacion.EnCurso) return
        sincronizacion = EstadoSincronizacion.EnCurso

        viewModelScope.launch {
            val resultado = withContext(Dispatchers.IO) {
                // Hasta que exista Ajustes -> Servidor, siempre la URL por defecto.
                RutinaRepository.sincronizar(getApplication<Application>(), BuildConfig.API_URL_POR_DEFECTO)
            }
            if (resultado is ResultadoSync.Ok) carga = CargaRutina.Ok(resultado.mesociclo)
            sincronizacion = EstadoSincronizacion.Terminada(resultado)
        }
    }

    /** Borra el mensaje de la última sincronización, para no mostrar uno viejo al volver a Ajustes. */
    fun olvidarResultado() {
        if (sincronizacion is EstadoSincronizacion.Terminada) sincronizacion = EstadoSincronizacion.Inactiva
    }
}
