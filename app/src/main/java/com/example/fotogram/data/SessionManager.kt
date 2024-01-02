package com.example.fotogram.data

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

// L'istanza DataStore (Singleton per tutta l'app)
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "fotogram_settings")

class SessionManager(private val context: Context) {

    val tag = "debugger_Session-Manager"

    companion object {
        private val SID_KEY = stringPreferencesKey("session_id")
        private val UID_KEY = intPreferencesKey("user_id")
        private val PROFILE_PIC_KEY = stringPreferencesKey("profile_pic")
    }

    suspend fun saveSession(sid: String, uid: Int) {
        Log.d(tag, "Scrivo Sessione su Disco -> SID: $sid, UID: $uid")
        context.dataStore.edit { p ->
            p[SID_KEY] = sid
            p[UID_KEY] = uid
        }
    }

    suspend fun getSid(): String? {
        val sid = context.dataStore.data.map { it[SID_KEY] }.first()
        //Log.d(tag, "Lettura SID: ${sid ?: "NULL (Utente non loggato)"}") // Siamo loggati all'avvio?
        return sid
    }

    suspend fun getUid(): Int {
        val uid = context.dataStore.data.map { it[UID_KEY] ?: -1 }.first()
        // Log.v(tag, "Lettura UID: $uid")
        return uid
    }

    suspend fun saveProfilePic(base64: String?) {
        val size = base64?.length ?: 0
        Log.d(tag, "Salvataggio Foto Profilo Locale (Size: $size chars)")
        context.dataStore.edit { p ->
            if (base64 == null) {
                p.remove(PROFILE_PIC_KEY)
                Log.d(tag, "Foto rimossa dal DataStore")
            } else {
                p[PROFILE_PIC_KEY] = base64
            }
        }
    }

    suspend fun getProfilePic(): String? {
        val pic = context.dataStore.data.map { it[PROFILE_PIC_KEY] }.first()
        Log.d(tag, "Lettura Foto Profilo: ${if (pic != null) "Ok" else "NULL"}")
        return pic
    }

    suspend fun clearSession() {
        Log.w(tag, "CLEAR SESSION: Cancellazione totale dati utente (Logout)")
        context.dataStore.edit { it.clear() }
    }
}