package com.luis.fierros.presentation

import android.app.RemoteInput
import android.content.Intent
import android.view.inputmethod.EditorInfo
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.wear.input.RemoteInputIntentHelper
import androidx.wear.input.wearableExtender
import com.luis.fierros.R

private const val CLAVE_URL = "url_servidor"

/**
 * Ajustes -> Servidor: la URL que usa la app para sincronizar y, debajo, dos botones:
 * "Cambiar" abre el teclado del reloj (el mismo de los mensajes, con dictado) y "Restablecer"
 * vuelve a la de local.properties.
 */
@Composable
fun PantallaServidor(viewModel: RutinaViewModel) {
    val etiquetaTeclado = stringResource(R.string.url_del_servidor)
    val teclado = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { resultado ->
        val texto = resultado.data?.let { RemoteInput.getResultsFromIntent(it) }?.getCharSequence(CLAVE_URL)
        if (texto != null) viewModel.cambiarServidor(texto.toString())
    }

    PantallaLista(
        titulo = stringResource(R.string.servidor),
        // Si lo escrito no es válido, lo avisa el cartel de abajo (ver WearApp).
        mensaje = viewModel.urlServidor.removePrefix("http://"),
        opciones = listOf(
            Opcion(stringResource(R.string.cambiar)),
            Opcion(stringResource(R.string.restablecer)),
        ),
        onClick = { i ->
            if (i == 0) teclado.launch(intentTeclado(etiquetaTeclado)) else viewModel.restablecerServidor()
        },
    )
}

/** Intent del teclado del sistema de Wear OS para escribir (o dictar) un texto. */
private fun intentTeclado(etiqueta: String): Intent {
    val entrada = RemoteInput.Builder(CLAVE_URL)
        .setLabel(etiqueta)
        // Para una URL no tiene sentido dibujar emojis; el botón de confirmar dice "Listo".
        .wearableExtender {
            setEmojisAllowed(false)
            setInputActionType(EditorInfo.IME_ACTION_DONE)
        }
        .build()
    return RemoteInputIntentHelper.createActionRemoteInputIntent().also {
        RemoteInputIntentHelper.putRemoteInputsExtra(it, listOf(entrada))
    }
}
