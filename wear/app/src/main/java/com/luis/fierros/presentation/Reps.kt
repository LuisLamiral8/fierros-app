package com.luis.fierros.presentation

/**
 * Cómo mostrar "5 series de 20-12-10-8-6" en una pantalla de 40 mm.
 *
 * El problema: "5 × 20-12-10-8-6" no entra en un renglón y el corte automático dejaba
 * "20-12-10-8" arriba y un "-6" huérfano abajo. Achicar la letra ya se probó y se descartó.
 *
 * La solución de las maquetas: cuando las reps son largas, el "5 ×" se va a una etiqueta
 * aparte ("5 SERIES") y las reps quedan solas, cortadas a propósito en dos renglones parejos.
 */
sealed interface Reps {
    /** Entra todo junto: "5 × 12-10". */
    data class EnUnRenglon(val texto: String) : Reps

    /** Reps solas en dos renglones ("20-12-10" / "-8-6") y las series como etiqueta aparte. */
    data class EnDos(val primero: String, val segundo: String, val series: Int) : Reps
}

// Abajo de esto entra cómodo en un renglón junto con el "N × ".
private const val LARGO_COMODO = 8

// Con menos bloques que esto, partir deja renglones de un solo número.
private const val MINIMO_BLOQUES = 4

/**
 * Decide cómo mostrar [series] × [reps].
 *
 * Parte solamente si las reps son un esquema largo separado por guiones ("20-12-10-8-6").
 * El corte va por cantidad de bloques, no por caracteres: con 5 bloques quedan 3 arriba y 2
 * abajo, así el segundo renglón nunca es un número solo. El guion arranca el segundo renglón,
 * para que se lea como continuación.
 */
fun formatearReps(series: Int, reps: String): Reps {
    val bloques = reps.split("-")
    if (reps.length <= LARGO_COMODO || bloques.size < MINIMO_BLOQUES || bloques.any { it.isBlank() }) {
        return Reps.EnUnRenglon("$series × $reps")
    }
    val corte = (bloques.size + 1) / 2 // 5 bloques -> 3 arriba
    return Reps.EnDos(
        primero = bloques.take(corte).joinToString("-"),
        segundo = "-" + bloques.drop(corte).joinToString("-"),
        series = series,
    )
}
