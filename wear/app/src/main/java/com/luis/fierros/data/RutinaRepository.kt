package com.luis.fierros.data

import android.content.Context
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

sealed interface CargaRutina {
    data object SinRutina : CargaRutina
    data class Error(val mensaje: String) : CargaRutina
    data class Ok(val mesociclo: Mesociclo) : CargaRutina
}

sealed interface ResultadoSync {
    /** No se pudieron subir los registros, asi que no se bajo la rutina nueva. */
    data class SubidaFallida(val pendientes: Int) : ResultadoSync

    data class Ok(val mesociclo: Mesociclo) : ResultadoSync
    data object SinConexion : ResultadoSync
    data object SinRutinaEnServidor : ResultadoSync
    data class ErrorServidor(val codigo: Int) : ResultadoSync
    data object RutinaInvalida : ResultadoSync
    data object ErrorAlGuardar : ResultadoSync
}

object RutinaRepository {
    private const val ARCHIVO = "rutina.json"
    private const val DIR_HISTORIAL = "historial"
    private val FORMATO_HISTORIAL = DateTimeFormatter.ofPattern("yyyy-MM-dd-HHmmss")

    private val json = Json { ignoreUnknownKeys = true }

    /** Convierte el texto en rutina. Tira SerializationException si no tiene el formato esperado. */
    private fun parsear(texto: String): Mesociclo = json.decodeFromString<Mesociclo>(texto)

    /** Lee rutina.json de filesDir. Hace I/O: llamar fuera del hilo principal. */
    fun cargar(context: Context): CargaRutina {
        val archivo = File(context.filesDir, ARCHIVO)
        if (!archivo.exists()) return CargaRutina.SinRutina

        return try {
            CargaRutina.Ok(parsear(archivo.readText()))
        } catch (e: SerializationException) {
            CargaRutina.Error("JSON inválido: ${e.message}")
        } catch (e: IOException) {
            CargaRutina.Error("No se pudo leer el archivo: ${e.message}")
        }
    }

    /**
     * Baja la rutina de la API y, solo si es válida, reemplaza la guardada.
     * Hace red e I/O: llamar fuera del hilo principal.
     */
    suspend fun sincronizar(context: Context, urlBase: String): ResultadoSync =
        when (val respuesta = ApiFierros.descargarRutina(urlBase)) {
            RespuestaApi.SinConexion -> ResultadoSync.SinConexion
            is RespuestaApi.Error ->
                if (respuesta.codigo == 404) ResultadoSync.SinRutinaEnServidor
                else ResultadoSync.ErrorServidor(respuesta.codigo)
            is RespuestaApi.Ok -> {
                val mesociclo = try {
                    parsear(respuesta.texto)
                } catch (e: SerializationException) {
                    null
                }
                when {
                    mesociclo == null || mesociclo.semanas.isEmpty() -> ResultadoSync.RutinaInvalida
                    !guardar(context, respuesta.texto) -> ResultadoSync.ErrorAlGuardar
                    else -> ResultadoSync.Ok(mesociclo)
                }
            }
        }

    /**
     * Reemplaza rutina.json por [texto], que ya tiene que estar validado.
     * Antes copia la actual a historial/, y el reemplazo es atómico: el reloj nunca queda sin rutina.
     */
    private fun guardar(context: Context, texto: String): Boolean =
        try {
            val actual = File(context.filesDir, ARCHIVO)
            val temporal = File(context.filesDir, "$ARCHIVO.tmp")
            temporal.writeText(texto)
            if (actual.exists()) {
                val historial = File(context.filesDir, DIR_HISTORIAL).apply { mkdirs() }
                val nombre = "rutina-${LocalDateTime.now().format(FORMATO_HISTORIAL)}.json"
                actual.copyTo(File(historial, nombre), overwrite = true)
            }
            Files.move(
                temporal.toPath(),
                actual.toPath(),
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE,
            )
            true
        } catch (e: IOException) {
            false
        }
}
