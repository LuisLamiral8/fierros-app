package com.luis.fierros.presentation

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.luis.fierros.BuildConfig
import com.luis.fierros.data.CargaRutina
import com.luis.fierros.data.Mesociclo
import com.luis.fierros.data.ResultadoSync
import com.luis.fierros.data.RutinaRepository
import com.luis.fierros.data.ServidorRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface EstadoSincronizacion {
    data object Inactiva : EstadoSincronizacion
    data object EnCurso : EstadoSincronizacion
    data class Terminada(val resultado: ResultadoSync) : EstadoSincronizacion
}

/**
 * Estado de la app: la rutina, la sincronización y el servidor. Sobrevive a que el sistema recree
 * la pantalla, y las pantallas se redibujan solas cuando cambia (lo más parecido a un @Service
 * con estado).
 */
class RutinaViewModel(application: Application) : AndroidViewModel(application) {

    /** null mientras se lee el archivo al arrancar. */
    var carga by mutableStateOf<CargaRutina?>(null)
        private set

    /** La rutina cargada, o null si todavía no hay. */
    val rutina: Mesociclo?
        get() = (carga as? CargaRutina.Ok)?.mesociclo

    var sincronizacion by mutableStateOf<EstadoSincronizacion>(EstadoSincronizacion.Inactiva)
        private set

    /** La URL con la que se sincroniza: la elegida en Ajustes o la de por defecto. */
    var urlServidor by mutableStateOf(BuildConfig.API_URL_POR_DEFECTO)
        private set

    /** true si lo último que se escribió como URL no era válido (no se guardó); el aviso lo apaga. */
    var urlInvalida by mutableStateOf(false)
        private set

    private val app: Application
        get() = getApplication()

    init {
        viewModelScope.launch {
            carga = withContext(Dispatchers.IO) { RutinaRepository.cargar(app) }
        }
        viewModelScope.launch {
            ServidorRepository.urlGuardada(app).collect { guardada ->
                urlServidor = guardada ?: BuildConfig.API_URL_POR_DEFECTO
            }
        }
    }

    fun sincronizar() {
        // Un segundo toque mientras hay una en curso no hace nada.
        if (sincronizacion == EstadoSincronizacion.EnCurso) return
        sincronizacion = EstadoSincronizacion.EnCurso

        viewModelScope.launch {
            val resultado = withContext(Dispatchers.IO) {
                RutinaRepository.sincronizar(app, ServidorRepository.urlActual(app))
            }
            if (resultado is ResultadoSync.Ok) carga = CargaRutina.Ok(resultado.mesociclo)
            sincronizacion = EstadoSincronizacion.Terminada(resultado)
        }
    }

    /** Borra el mensaje de la última sincronización, para no mostrar uno viejo al volver a Ajustes. */
    fun olvidarResultado() {
        if (sincronizacion is EstadoSincronizacion.Terminada) sincronizacion = EstadoSincronizacion.Inactiva
    }

    /** Guarda la URL escrita en Ajustes -> Servidor. Si no es válida, no guarda nada y lo marca. */
    fun cambiarServidor(texto: String) {
        val url = ServidorRepository.normalizar(texto)
        urlInvalida = url == null
        when (url) {
            null -> Unit
            // Escribir la de por defecto es lo mismo que restablecer.
            BuildConfig.API_URL_POR_DEFECTO -> viewModelScope.launch { ServidorRepository.restablecer(app) }
            else -> viewModelScope.launch { ServidorRepository.guardar(app, url) }
        }
    }

    /** Vuelve a la URL de local.properties. */
    fun restablecerServidor() {
        urlInvalida = false
        viewModelScope.launch { ServidorRepository.restablecer(app) }
    }

    fun olvidarUrlInvalida() {
        urlInvalida = false
    }
}
