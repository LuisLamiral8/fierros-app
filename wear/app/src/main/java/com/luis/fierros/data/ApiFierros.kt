package com.luis.fierros.data

import android.util.Log
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.CancellationException

/** Lo que devolvió el GET /fierros/rutina, sin interpretar. */
sealed interface RespuestaApi {
    data class Ok(val texto: String) : RespuestaApi
    data class Error(val codigo: Int) : RespuestaApi
    data object SinConexion : RespuestaApi
}

private const val TAG = "Fierros"

/** Cliente de la API del homelab (el equivalente a un RestClient de Spring). */
object ApiFierros {
    private val cliente = HttpClient(OkHttp) {
        install(HttpTimeout) {
            connectTimeoutMillis = 5_000
            requestTimeoutMillis = 10_000
        }
    }

    suspend fun descargarRutina(urlBase: String): RespuestaApi =
        try {
            val respuesta = cliente.get("${urlBase.trimEnd('/')}/fierros/rutina")
            if (respuesta.status.value == 200) {
                RespuestaApi.Ok(respuesta.bodyAsText())
            } else {
                RespuestaApi.Error(respuesta.status.value)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Sin red, timeout, IP que no responde o URL mal escrita: para el usuario es lo mismo,
            // pero el motivo real queda en el log (adb logcat -s Fierros).
            Log.w(TAG, "No se pudo bajar la rutina de $urlBase", e)
            RespuestaApi.SinConexion
        }
}
