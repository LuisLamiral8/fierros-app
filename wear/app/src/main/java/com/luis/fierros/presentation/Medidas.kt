package com.luis.fierros.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Las maquetas del diseñador (assets/diseño-wearos-fierros.html) están dibujadas dentro de un
 * círculo de 384 unidades de diámetro. Acá esas unidades se llevan a la pantalla que haya:
 * el emulador "small round" mide 192 dp y el Watch8 40 mm un poco más, así que en vez de clavar
 * dp fijos se escala todo por el diámetro real y las proporciones de la maqueta se mantienen.
 */
private const val LIENZO = 384f

@Composable
@ReadOnlyComposable
private fun factor(): Float = LocalConfiguration.current.screenWidthDp / LIENZO

/** Una medida de la maqueta (px del lienzo de 384), llevada a la pantalla real. */
@Composable
@ReadOnlyComposable
fun du(valor: Float): Dp = (valor * factor()).dp

/** Un tamaño de letra de la maqueta, llevado a la pantalla real. */
@Composable
@ReadOnlyComposable
fun duSp(valor: Float): TextUnit = (valor * factor()).sp
