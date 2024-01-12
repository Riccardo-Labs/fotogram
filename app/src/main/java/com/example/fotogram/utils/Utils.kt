package com.example.fotogram

import android.location.Location
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.abs

object Utils {

    // 1. FORMATTAZIONE DATA (es. "5 min fa", "2 ore fa")
    // Il server manda date tipo "2023-10-25T14:30:00.000Z".
    // Questa funzione le trasforma in qualcosa più user-friendly (SE LA DATA è PIù VECCHIA DI 31 GIORNI MOSTRA QUELLA STANDARD)
    fun formatTimeAgo(isoString: String?): String {
        if (isoString.isNullOrBlank()) return "Sconosciuto"

        // ELENCO FORMATI (Il primo è quello che ti serve ora!)
        val possiblePatterns = listOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", // ESATTO per: 2025-11-29T15:11:27.364Z
            "yyyy-MM-dd'T'HH:mm:ss'Z'",     // Senza millisecondi
            "yyyy-MM-dd'T'HH:mm:ss.SSS",    // Senza Z finale
            "yyyy-MM-dd'T'HH:mm:ss",        // Semplice
            "yyyy-MM-dd HH:mm:ss"           // SQL Standard
        )

        for (pattern in possiblePatterns) {
            try {
                // Locale.US è fondamentale per evitare bug su telefoni con impostazioni strane
                val format = SimpleDateFormat(pattern, Locale.US)
                format.timeZone = TimeZone.getTimeZone("UTC") // Impostiamo UTC perché c'è la Z

                val date = format.parse(isoString)

                if (date != null) {
                    val now = Date().time
                    val time = date.time
                    // Usa abs() perché se il post è nel "futuro" (2025) non deve dare numeri negativi
                    val diff = abs(now - time)

                    val seconds = diff / 1000
                    val minutes = seconds / 60
                    val hours = minutes / 60
                    val days = hours / 24

                    return when {
                        seconds < 60 -> "proprio ora"
                        minutes < 60 -> "$minutes min fa"
                        hours < 24 -> "$hours ore fa"
                        days < 31 -> "$days gg fa"
                        //se è superiore a 31 giorni mostra formato standard
                        else -> SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(date)
                    }
                }
            } catch (e: Exception) {
                // Continua col prossimo pattern
            }
        }

        // Se fallisce tutto, restituisce la stringa originale
        return isoString
    }


    // 2. CALCOLO DISTANZA (es. "Quanto dista questo post da me?")
    // "Ordina i post per vicinanza" o "Mostra distanza".
    // locationA = la tua posizione (es. dalla Mappa), locationB = posizione del post
    /*// Supponiamo una posizione fissa o dal GPS
         val myLat = 45.48
         myLon = 9.05
    // Ordina la lista prima di passarla alla LazyColumn
    val listaOrdinata = listaPost.sortedBy { post ->
        val results = FloatArray(1)
        // Se il post non ha location, mettilo lontano (Max Value)
        if (post.location?.latitude != null) {
            Location.distanceBetween(myLat, myLon, post.location.latitude, post.location.longitude, results)
            results[0] // Restituisce la distanza in metri
        } else {
            Float.MAX_VALUE
        }
    }*/
    fun getDistanceString(lat1: Double?, lon1: Double?, lat2: Double?, lon2: Double?): String {
        if (lat1 == null || lon1 == null || lat2 == null || lon2 == null) return ""

        val results = FloatArray(1)
        Location.distanceBetween(lat1, lon1, lat2, lon2, results)
        val distanceMeters = results[0]

        return when {
            distanceMeters < 1000 -> "${distanceMeters.toInt()} m"
            else -> String.format("%.1f km", distanceMeters / 1000)
        }
    }

    // 3. VALIDATORE EMAIL SEMPLICE (mai usato nell'app)
    fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }
}
