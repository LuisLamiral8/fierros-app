package com.luis.fierros.data

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

/**
 * Un peso levantado en un ejercicio, uno por ejercicio y no por serie.
 *
 * El formato es el que ya espera la API y lee la web (fierros-web/src/tipos.ts): el registro
 * queda pegado a la posición en la rutina (semana, día y letra) más la fecha en que se anotó.
 * [subido] es el único campo local: dice si ya viajó al servidor, y no se manda en el POST.
 */
@Serializable
data class Registro(
    val semana: Int,
    val dia: Int,
    val letra: String,
    val peso: String,
    val fecha: String,
    val subido: Boolean = false,
)

/** Lo que viaja al servidor: el mismo formato, sin el campo local [Registro.subido]. */
@Serializable
private data class RegistroApi(
    val semana: Int,
    val dia: Int,
    val letra: String,
    val peso: String,
    val fecha: String,
)

/** 32.5 -> "32,5 kg"; 33.0 -> "33 kg". Con coma decimal, como se escribe acá. */
fun formatearPeso(kilos: Double): String {
    val redondeado = (kilos * 100).roundToInt() / 100.0
    val texto =
        if (redondeado % 1.0 == 0.0) {
            redondeado.toInt().toString()
        } else {
            redondeado.toString().trimEnd('0').trimEnd('.').replace('.', ',')
        }
    return "$texto kg"
}

/** "32,5 kg" -> 32.5. Devuelve null si no es un número (un "peso corporal" viejo, por ejemplo). */
fun kilosDe(peso: String): Double? =
    peso.removeSuffix("kg").trim().replace(',', '.').toDoubleOrNull()

/**
 * Los registros viven en un archivo propio de la app (filesDir/registros.json), aparte de la
 * rutina: sincronizar la rutina no los toca. Se guardan al instante y sin red, porque se anotan
 * en el gimnasio.
 */
object RegistroRepository {
    private const val ARCHIVO = "registros.json"
    private const val TEMPORAL = "registros.json.tmp"

    private val json = Json { ignoreUnknownKeys = true }

    /** Lee los registros guardados. Hace I/O: llamar fuera del hilo principal. */
    fun cargar(context: Context): List<Registro> {
        val archivo = File(context.filesDir, ARCHIVO)
        if (!archivo.exists()) return emptyList()
        return try {
            json.decodeFromString<List<Registro>>(archivo.readText())
        } catch (e: SerializationException) {
            emptyList() // un archivo roto no puede dejar la app sin arrancar
        } catch (e: IOException) {
            emptyList()
        }
    }

    /** Reemplazo atómico, igual que la rutina: nunca queda un archivo a medio escribir. */
    fun guardar(context: Context, registros: List<Registro>): Boolean =
        try {
            val actual = File(context.filesDir, ARCHIVO)
            val temporal = File(context.filesDir, TEMPORAL)
            temporal.writeText(json.encodeToString(registros))
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

    /**
     * Anota [kilos] en un ejercicio. Si ya había un registro para esa misma posición lo
     * reemplaza —corregir no agrega uno nuevo— y vuelve a marcarlo como pendiente de subir.
     * Devuelve la lista nueva, o null si no se pudo escribir.
     */
    fun registrar(
        context: Context,
        semana: Int,
        dia: Int,
        letra: String,
        kilos: Double,
    ): List<Registro>? {
        val nuevo = Registro(
            semana = semana,
            dia = dia,
            letra = letra,
            peso = formatearPeso(kilos),
            fecha = OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
        )
        val lista = cargar(context)
            .filterNot { it.semana == semana && it.dia == dia && it.letra == letra }
            .plus(nuevo)
        return if (guardar(context, lista)) lista else null
    }

    /** El registro de esta misma posición, si ya lo anoté. */
    fun deEsta(registros: List<Registro>, semana: Int, dia: Int, letra: String): Registro? =
        registros.firstOrNull { it.semana == semana && it.dia == dia && it.letra == letra }

    /**
     * Lo que levanté la vez pasada en este ejercicio: el registro más nuevo del mismo día y
     * letra en otra semana. Es el dato que se mira antes de decidir con cuánto arrancar.
     *
     * Ojo: se empareja por posición (día + letra), no por nombre, igual que en la API. Si
     * cambia la rutina, la "B" del día 2 puede pasar a ser otro ejercicio.
     */
    fun ultimo(registros: List<Registro>, semana: Int, dia: Int, letra: String): Registro? =
        registros
            .filter { it.dia == dia && it.letra == letra && it.semana != semana }
            .maxByOrNull { it.fecha }

    /** Cuántos faltan subir al servidor. */
    fun pendientes(registros: List<Registro>): Int = registros.count { !it.subido }

    /** Los que todavia no viajaron al servidor. */
    fun pendientesDe(registros: List<Registro>): List<Registro> = registros.filter { !it.subido }

    /** El cuerpo del POST: sin "subido", que es un campo local y al servidor no le importa. */
    fun aJsonParaSubir(registros: List<Registro>): String =
        json.encodeToString(registros.map { RegistroApi(it.semana, it.dia, it.letra, it.peso, it.fecha) })

    /**
     * Marca como subidos exactamente los que se enviaron (posicion + fecha). Si mientras subia
     * se anoto algo nuevo, ese queda pendiente para la proxima.
     */
    fun marcarSubidos(context: Context, enviados: List<Registro>): List<Registro>? {
        val enviadas = enviados.map(::huella).toSet()
        val lista = cargar(context).map { if (huella(it) in enviadas) it.copy(subido = true) else it }
        return if (guardar(context, lista)) lista else null
    }

    private fun huella(r: Registro) = "${r.semana}|${r.dia}|${r.letra}|${r.fecha}"
}
