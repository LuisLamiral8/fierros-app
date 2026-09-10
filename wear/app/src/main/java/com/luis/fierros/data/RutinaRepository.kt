package com.luis.fierros.data

import android.content.Context
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.io.File
import java.io.IOException

sealed interface CargaRutina {
    data object SinRutina : CargaRutina
    data class Error(val mensaje: String) : CargaRutina
    data class Ok(val mesociclo: Mesociclo) : CargaRutina
}

object RutinaRepository {
    private const val ARCHIVO = "rutina.json"

    private val json = Json { ignoreUnknownKeys = true }

    /** Lee rutina.json de filesDir. Hace I/O: llamar fuera del hilo principal. */
    fun cargar(context: Context): CargaRutina {
        val archivo = File(context.filesDir, ARCHIVO)
        if (!archivo.exists()) return CargaRutina.SinRutina

        return try {
            CargaRutina.Ok(json.decodeFromString<Mesociclo>(archivo.readText()))
        } catch (e: SerializationException) {
            CargaRutina.Error("JSON inválido: ${e.message}")
        } catch (e: IOException) {
            CargaRutina.Error("No se pudo leer el archivo: ${e.message}")
        }
    }
}
