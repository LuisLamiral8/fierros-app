package com.luis.fierros.data

import android.content.Context
import android.net.Uri
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.luis.fierros.BuildConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

// Preferencias de la app guardadas en el reloj (DataStore: el reemplazo moderno de SharedPreferences).
private val Context.ajustes: DataStore<Preferences> by preferencesDataStore(name = "ajustes")

/** La URL de la API: la que se eligió en Ajustes -> Servidor o, si no hay, la de local.properties. */
object ServidorRepository {
    private val CLAVE_URL = stringPreferencesKey("url_servidor")

    /** La URL guardada desde Ajustes, o null si se usa la de por defecto. Avisa cada vez que cambia. */
    fun urlGuardada(context: Context): Flow<String?> = context.ajustes.data.map { it[CLAVE_URL] }

    suspend fun urlActual(context: Context): String =
        urlGuardada(context).first() ?: BuildConfig.API_URL_POR_DEFECTO

    suspend fun guardar(context: Context, url: String) {
        context.ajustes.edit { it[CLAVE_URL] = url }
    }

    suspend fun restablecer(context: Context) {
        context.ajustes.edit { it.remove(CLAVE_URL) }
    }

    /**
     * Limpia lo que se escribió o dictó: "192.168.1.57 : 3000" -> "http://192.168.1.57:3000".
     * Devuelve null si no parece una dirección http(s) con un host de verdad.
     */
    fun normalizar(texto: String): String? {
        val limpio = texto.filterNot { it.isWhitespace() }.trimEnd('/')
        if (limpio.isEmpty()) return null
        val conEsquema = if ("://" in limpio) limpio else "http://$limpio"
        val uri = Uri.parse(conEsquema)
        val esHttp = uri.scheme == "http" || uri.scheme == "https"
        return if (esHttp && hostValido(uri.host)) conEsquema else null
    }

    // Si parece una IP (solo números y puntos), tiene que ser una IPv4 completa: así "22338877"
    // o "1.2" no se aceptan. Si es un nombre, tiene que tener un punto (api.luis) o ser localhost.
    private fun hostValido(host: String?): Boolean = when {
        host.isNullOrBlank() -> false
        host.all { it.isDigit() || it == '.' } -> esIpv4(host)
        else -> host == "localhost" || '.' in host
    }

    private val IPV4 = Regex("""(\d{1,3})\.(\d{1,3})\.(\d{1,3})\.(\d{1,3})""")

    private fun esIpv4(host: String): Boolean =
        IPV4.matchEntire(host)?.groupValues?.drop(1)?.all { it.toInt() in 0..255 } == true
}
