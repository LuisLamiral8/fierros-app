package com.luis.fierros.data

import kotlinx.serialization.Serializable

// Espejo del formato de rutina.json (ver docs/planificaciones/planificacion-it-1.md).

@Serializable
data class Mesociclo(
    val nombre: String,
    val semanas: List<Semana>,
) {
    fun semana(numero: Int?): Semana? = semanas.firstOrNull { it.numero == numero }
}

@Serializable
data class Semana(
    val numero: Int,
    val dias: List<Dia>,
) {
    fun dia(numero: Int?): Dia? = dias.firstOrNull { it.numero == numero }
}

@Serializable
data class Dia(
    val numero: Int,
    val ejercicios: List<Ejercicio>,
)

@Serializable
data class Ejercicio(
    val letra: String,
    val nombre: String,
    val series: Int,
    val reps: String,
    val peso: String,
    val nota: String? = null,
)
