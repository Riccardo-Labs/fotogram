package com.example.fotogram

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap


object ImageUtils {

    fun decodeBase64(base64String: String): ImageBitmap? {
        //OVERVIEW: Classe di utility per la gestione delle immagini nell'app.
        // prende in input base64 -> da in output ImageBitmap il formato richiesto da Jetpack Compose per mostrare le immagini

        return try {
            // Toglie intestazioni tipo "data:image/png;base64," se presenti
            val cleanString = if (base64String.contains(",")) {
                base64String.substringAfter(",")
            } else {
                base64String
            }

            // Decodifica i byte
            val decodedBytes = Base64.decode(cleanString, Base64.DEFAULT)
            // Crea la Bitmap
            val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
            // Converte per Compose
            bitmap?.asImageBitmap()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}