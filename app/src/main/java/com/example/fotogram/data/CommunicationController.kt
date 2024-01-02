package com.example.fotogram.data

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import com.example.fotogram.model.LoginResponse
import com.example.fotogram.model.RegisterRequest
import com.example.fotogram.model.User
import com.example.fotogram.model.UserUpdateRequest
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.json.JSONObject
import java.io.ByteArrayOutputStream

class CommunicationController {

    val tag = "debugger_C-Controller"
    private val BASE_URL = "https://develop.ewlab.di.unimi.it/mc/2526/"

    private val jsonHelper = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        isLenient = true
    }

    private val client = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(jsonHelper)
        }
    }

    // --- REGISTRAZIONE ---
    suspend fun register(username: String, email: String, pass: String, bio: String): LoginResponse? {
        Log.d(tag, "REGISTER Request -> User: $username, Email: $email")
        try {
            val registerBody = RegisterRequest(username, email, pass)

            val response = client.post("${BASE_URL}user") {
                contentType(ContentType.Application.Json)
                setBody(registerBody)
            }

            Log.d(tag, "REGISTER Status: ${response.status}")

            if (response.status.value in 200..299) {
                val bodyString = response.bodyAsText()
                val loginData = jsonHelper.decodeFromString<LoginResponse>(bodyString)

                Log.d(tag, "Aggiorno profilo con Bio: '$bio'")
                updateUser(loginData.sessionId, username, bio) // se bio è vuota passa stringa vuota e aggiorna comunque il nome

                return loginData
            } else {
                Log.e(tag, "REGISTER Failed: ${response.bodyAsText()}")
                return null
            }
            Log.d(tag, "UPDATE_USER -> User: $username")
        } catch (e: Exception) {
            Log.e(tag, "REGISTER Exception: ${e.message}")
            return null
        }
    }

    // --- MODIFICA PROFILO ---
    suspend fun updateUser(sessionId: String, username: String, bio: String, dateOfBirth: String? = null): User? {
        return try {
            val requestBody = UserUpdateRequest(username, bio, dateOfBirth)
            val response = client.put("${BASE_URL}user") {
                headers { append("x-session-id", sessionId) }
                contentType(ContentType.Application.Json)
                setBody(requestBody)
            }
            if (response.status.value in 200..299) {
                jsonHelper.decodeFromString<User>(response.bodyAsText())
            } else {
                Log.e(tag, "UPDATE_USER Failed: ${response.bodyAsText()}")
                null
            }
        } catch (e: Exception) { null }
    }


    // --- AGGIORNA FOTO PROFILO ---
    suspend fun updateProfilePicture(sessionId: String, imageBytes: ByteArray): Boolean {
        Log.d(tag, "UPDATE_Foto Request -> Bytes: ${imageBytes.size}")
        return try {
            val compressedBytes = compressImage(imageBytes, 50)
            val imageBase64 = Base64.encodeToString(compressedBytes, Base64.NO_WRAP)

            val jsonBody = JSONObject().put("base64", imageBase64).toString()

            val response = client.put("${BASE_URL}user/image") {
                headers { append("x-session-id", sessionId) }
                contentType(ContentType.Application.Json)
                setBody(jsonBody)
            }
            Log.d(tag, "UPDATE_PIC Response: ${response.status}")
            response.status.value in 200..299
        } catch (e: Exception) {
            Log.e(tag, "UPDATE_Foto Exception: ${e.message}")
            false
        }
    }


    // --- CREA POST ---
    suspend fun createPost(sessionId: String, imageBytes: ByteArray, text: String, lat: Double? = null, lng: Double? = null): Boolean {
        Log.d(tag, "CREATE_POST Request -> Txt: $text, Loc: $lat,$lng")
        return try {
            val compressedBytes = compressImage(imageBytes)
            val imageBase64 = Base64.encodeToString(compressedBytes, Base64.NO_WRAP)

            val jsonObject = JSONObject()
            jsonObject.put("contentPicture", imageBase64)
            if (text.isNotEmpty()) jsonObject.put("contentText", text)

            if (lat != null && lng != null) {
                val locationObj = JSONObject()
                locationObj.put("latitude", lat)
                locationObj.put("longitude", lng)
                jsonObject.put("location", locationObj)
            }

            val response = client.post("${BASE_URL}post") {
                headers { append("x-session-id", sessionId) }
                contentType(ContentType.Application.Json)
                setBody(jsonObject.toString())
            }

            Log.d(tag, "CREATE_POST Response: ${response.status}")
            response.status.value in 200..299
        } catch (e: Exception) {
            Log.e(tag, "CREATE_POST Error: ${e.message}")
            false
        }
    }


    // --- DATA FETCHING (BACHECA) ---
    suspend fun getFeed(sessionId: String, limit: Int?, seed: Int? = null, maxPostId: Int? = null): String? {
        Log.d(tag, "GET_Feed Request -> sessionId: $sessionId, maxPostId=$maxPostId, limit=$limit, seed=$seed")
        return try {
            val response = client.get("${BASE_URL}feed") {
                headers { append("x-session-id", sessionId) }
                url {
                    if (maxPostId != null) parameters.append("maxPostId", maxPostId.toString())
                    parameters.append("limit", limit.toString())
                    if (seed != null) parameters.append("seed", seed.toString())
                }
            }
            if (response.status.value in 200..299) {
                val body = response.bodyAsText()
                Log.d(tag, "GET_Feed Success. Id ricevuti dal server $body")
                body
            } else {
                Log.e(tag, "GET_Feed Failed: ${response.status}")
                null
            }
        } catch (e: Exception) {
            Log.e(tag, "GET_Feed Error: ${e.message}")
            null
        }
    }

    suspend fun getPost(sessionId: String, postId: Int): String? {
        return try {
            val response = client.get("${BASE_URL}post/$postId") {
                headers { append("x-session-id", sessionId) }
            }
            val s = response.bodyAsText()
            //Log.d(tag, "GET_Post Response: $s") //stampa post uno per uno
            if (response.status.value in 200..299) s else null
        } catch (e: Exception) { null }
    }

    /* SIMULAZIONE 1
    // --- GET ALLE REAZIONE AL POST ---
    suspend fun getReaction(sessionId: String, postId: Int): String? {
        return try {
            val response = client.get("${BASE_URL}mockexam/reaction/$postId") {
                headers { append("x-session-id", sessionId) }
            }
            if (response.status.value in 200..299) response.bodyAsText() else null
        } catch (e: Exception) { null }
    }
    */

    /*
    // --- UPDATE REAZIONE AL POST ---
    suspend fun updateReaction(sessionId: String, postId: Int, reaction: String): Boolean {
        return try {
            val jsonBody = JSONObject().put("reaction", reaction).toString()
            Log.d(tag, "JSON_BODY: $jsonBody")
            val response = client.put("${BASE_URL}mockexam/reaction/$postId") {
                headers { append("x-session-id", sessionId) }
                contentType(ContentType.Application.Json)
                setBody(jsonBody)
            }
            Log.d(tag, "UPDATE_Reaction Response: ${response.status}")
            response.status.value in 200..299
        } catch (e: Exception) {
            Log.e(tag, "UPDATE_Reaction Exception: ${e.message}")
            false
        }
    }
    */
    /* SIMULAZIONE 2
    suspend fun savePost(sessionId: String, post: Int): Boolean {
        return try {
            val jsonBody = JSONObject().put("post", post).toString()
            Log.d(tag, "JSON_BODY: $jsonBody")
            val response = client.put("${BASE_URL}exercise_1/saved") {
                headers { append("x-session-id", sessionId) }
                contentType(ContentType.Application.Json)
                setBody(jsonBody)
            }
            Log.d(tag, "UPDATE_post nei salvati Response: ${response.status}")
            response.status.value in 200..299
        } catch (e: Exception) {
            Log.e(tag, "UPDATE_post nei salvati Exception: ${e.message}")
            false
        }
    }
    */

    /*
    suspend fun getSavedPost(sessionId: String): String? {
        return try {
            val response = client.get("${BASE_URL}exercise_1/saved") {
                headers { append("x-session-id", sessionId) }
            }
            if (response.status.value in 200..299) response.bodyAsText() else null
        } catch (e: Exception) { null }
    }
    */

//    SIMULAZIONE 3
/*
    suspend fun getAdv(sessionId: String): Adv? {
        return try {
            val response = client.get("${BASE_URL}exercise_2/adv") {
                headers { append("x-session-id", sessionId) }
            }
            Log.d(tag, "GET_Adv Response: ${response.bodyAsText()}") //stampa adv a log
            if (response.status.value in 200..299) {
                jsonHelper.decodeFromString<Adv>(response.bodyAsText())
            } else null
        } catch (e: Exception) { null }
    }
*/


    //---------------------------------------------

    suspend fun getUser(sessionId: String, userId: Int): User? {
        return try {
            val response = client.get("${BASE_URL}user/$userId") {
                headers { append("x-session-id", sessionId) }
            }
            //Log.d(tag, "GET_User ${response.bodyAsText()}") //stampa utenti uno per uno
            if (response.status.value in 200..299) {
                jsonHelper.decodeFromString<User>(response.bodyAsText())
            } else null

        } catch (e: Exception) { null }
    }


    suspend fun getUserPosts(sessionId: String, userId: Int): String? {
        return try {
            val response = client.get("${BASE_URL}post/list/$userId") {
                headers { append("x-session-id", sessionId) }
            }
            val s = response.bodyAsText()
            Log.d(tag, "GET_USER_POSTS UserID: $userId -> $s")

            if (response.status.value in 200..299) s else null
        } catch (e: Exception) { null }
    }


    // --- FOLLOW / UNFOLLOW ---
    suspend fun followUser(sessionId: String, userId: Int): Boolean {
        Log.d(tag, "FOLLOW User $userId")
        return try {
            val response = client.put("${BASE_URL}follow/$userId") {
                headers { append("x-session-id", sessionId) }
            }
            Log.d(tag, "FOLLOW Resp: ${response.status}")
            response.status.value in 200..299
        } catch (e: Exception) { false }
    }

    suspend fun unfollowUser(sessionId: String, userId: Int): Boolean {
        Log.d(tag, "UNFOLLOW User $userId")
        return try {
            val response = client.delete("${BASE_URL}follow/$userId") {
                headers { append("x-session-id", sessionId) }
            }
            Log.d(tag, "UNFOLLOW Resp: ${response.status}")
            response.status.value in 200..299
        } catch (e: Exception) { false }
    }

    // --- UTILITY ---
    private fun compressImage(originalBytes: ByteArray, quality: Int = 70): ByteArray {
        val originalBitmap = BitmapFactory.decodeByteArray(originalBytes, 0, originalBytes.size)
        val maxDimension = 800
        var width = originalBitmap.width
        var height = originalBitmap.height
        val ratio = width.toFloat() / height.toFloat()

        if (width > height && width > maxDimension) {
            width = maxDimension
            height = (width / ratio).toInt()
        } else if (height > maxDimension) {
            height = maxDimension
            width = (height * ratio).toInt()
        }

        val scaledBitmap = Bitmap.createScaledBitmap(originalBitmap, width, height, true)
        val outputStream = ByteArrayOutputStream()
        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
        return outputStream.toByteArray()
    }
}