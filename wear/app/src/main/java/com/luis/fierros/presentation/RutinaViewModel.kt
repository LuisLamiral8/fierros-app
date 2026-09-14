package com.luis.fierros.presentation

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.luis.fierros.BuildConfig
import com.luis.fierros.data.ApiFierros
import com.luis.fierros.data.CargaRutina
import com.luis.fierros.data.Mesociclo
import com.luis.fierros.data.Registro
import com.luis.fierros.data.RespuestaApi
import com.luis.fierros.data.RegistroRepository
import com.luis.fierros.data.ResultadoSync
import com.luis.fierros.data.RutinaRepository
import com.luis.fierros.data.ServidorRepository
import com.luis.fierros.data.kilosDe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Con cuántos kilos abre la rueda la primera vez, cuando el ejercicio todavía no tiene ningún
 * registro del cual partir.
 */
private const val KILOS_POR_DEFECTO = 20.0

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

    /** Lo levantado. Se guarda en el reloj al instante y sube al servidor al sincronizar. */
    var registros by mutableStateOf<List<Registro>>(emptyList())
        private set

    /** Cuántos registros faltan subir. Ajustes lo muestra para que no se junten sin que me entere. */
    val pendientes: Int
        get() = RegistroRepository.pendientes(registros)

    /** Lo que levanté la vez pasada en este ejercicio ("32,5 kg"), o null si es la primera. */
    fun ultimoDe(semana: Int, dia: Int, letra: String): String? =
        RegistroRepository.ultimo(registros, semana, dia, letra)?.peso

    /** Lo que ya anoté en este ejercicio de esta semana, o null si todavía no lo hice. */
    fun registradoEn(semana: Int, dia: Int, letra: String): String? =
        RegistroRepository.deEsta(registros, semana, dia, letra)?.peso

    /**
     * Con cuántos kilos abre la rueda: lo de hoy si ya anoté, si no lo de la vez pasada, y si no
     * hay nada, [KILOS_POR_DEFECTO]. Así lo más común —repetir o subir un escalón— es un toque.
     */
    fun kilosIniciales(semana: Int, dia: Int, letra: String): Double {
        val referencia = registradoEn(semana, dia, letra) ?: ultimoDe(semana, dia, letra)
        return referencia?.let { kilosDe(it) } ?: KILOS_POR_DEFECTO
    }

    /** Anota el peso y lo guarda en el acto, sin red. Corregir reemplaza, no agrega. */
    fun registrar(semana: Int, dia: Int, letra: String, kilos: Double) {
        viewModelScope.launch {
            val nueva = withContext(Dispatchers.IO) {
                RegistroRepository.registrar(app, semana, dia, letra, kilos)
            }
            if (nueva != null) registros = nueva
        }
    }

    var sincronizacion by mutableStateOf<EstadoSincronizacion>(EstadoSincronizacion.Inactiva)
        private set

    /** La ultima subida que fallo, para que la pantalla de conflicto sepa que paso. */
    var fallaSubida by mutableStateOf<ResultadoSync.SubidaFallida?>(null)
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
            registros = withContext(Dispatchers.IO) { RegistroRepository.cargar(app) }
        }
        viewModelScope.launch {
            ServidorRepository.urlGuardada(app).collect { guardada ->
                urlServidor = guardada ?: BuildConfig.API_URL_POR_DEFECTO
            }
        }
    }

    /**
     * Sincroniza en un orden que importa: **primero sube y despues baja**. Un registro apunta a
     * una posicion (semana, dia, letra), asi que si se pisara la rutina con una nueva antes de
     * subir, los registros viejos pasarian a describir otros ejercicios. Si la subida falla, no
     * se baja nada y no se pierde nada.
     *
     * La URL es la de Ajustes -> Servidor, la misma que usa la bajada.
     */
    fun sincronizar() {
        // Un segundo toque mientras hay una en curso no hace nada.
        if (sincronizacion == EstadoSincronizacion.EnCurso) return
        sincronizacion = EstadoSincronizacion.EnCurso

        viewModelScope.launch {
            val url = withContext(Dispatchers.IO) { ServidorRepository.urlActual(app) }

            val pendientes = RegistroRepository.pendientesDe(registros)
            if (pendientes.isNotEmpty()) {
                val subida = withContext(Dispatchers.IO) {
                    ApiFierros.subirRegistros(url, RegistroRepository.aJsonParaSubir(pendientes))
                }
                if (subida !is RespuestaApi.Ok) {
                    // Si contesto con un codigo, el servidor esta vivo y rechazo algo; si no,
                    // directamente no se llego hasta el.
                    val falla = ResultadoSync.SubidaFallida(
                        pendientes = pendientes.size,
                        codigo = (subida as? RespuestaApi.Error)?.codigo,
                    )
                    fallaSubida = falla
                    sincronizacion = EstadoSincronizacion.Terminada(falla)
                    return@launch
                }
                fallaSubida = null
                withContext(Dispatchers.IO) { RegistroRepository.marcarSubidos(app, pendientes) }
                    ?.let { registros = it }
            }

            val resultado = withContext(Dispatchers.IO) { RutinaRepository.sincronizar(app, url) }
            if (resultado is ResultadoSync.Ok) carga = CargaRutina.Ok(resultado.mesociclo)
            sincronizacion = EstadoSincronizacion.Terminada(resultado)
        }
    }


    /**
     * Salida manual del conflicto: baja la rutina sin subir. Los registros quedan pendientes y
     * pueden terminar apuntando a otros ejercicios, por eso no es el camino por defecto.
     */
    fun bajarIgual() {
        if (sincronizacion == EstadoSincronizacion.EnCurso) return
        sincronizacion = EstadoSincronizacion.EnCurso
        viewModelScope.launch {
            val url = withContext(Dispatchers.IO) { ServidorRepository.urlActual(app) }
            val resultado = withContext(Dispatchers.IO) { RutinaRepository.sincronizar(app, url) }
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
