package com.example.fotogram.ui

import android.util.Base64
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.fotogram.componenti.ImageGrid
import com.example.fotogram.data.CommunicationController
import com.example.fotogram.data.SessionManager
import com.example.fotogram.model.PostDetail
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OtherUserProfileScreen(navController: NavController, userId: Int) {
    val tag = "debugger_OtherUserProfile-Screen"

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val controller = remember { CommunicationController() }
    val sessionManager = remember { SessionManager(context) }
    val jsonHelper = Json { ignoreUnknownKeys = true; coerceInputValues = true; isLenient = true }

    // --- STATI UI ---
    var listaPostUtente by remember { mutableStateOf<List<PostDetail>>(emptyList()) }
    var username by remember { mutableStateOf("Caricamento...") }
    var bio by remember { mutableStateOf("") }
    var dateOfBirth by remember { mutableStateOf("") }
    var userImageBase64 by remember { mutableStateOf<String?>(null) }
    var numPosts by remember { mutableIntStateOf(0) }
    var numFollowers by remember { mutableIntStateOf(0) }
    var numFollowing by remember { mutableIntStateOf(0) }

    var isFollowing by remember { mutableStateOf(false) }
    var caricamentoInCorso by remember { mutableStateOf(true) }
    var isLoading by remember { mutableStateOf(true) } // Nota: questa variabile sembra ridondante con caricamentoInCorso, ma la lascio come da tuo codice

    // Aggiorna solo i dati utente (contatori e stato follow) ---
    suspend fun refreshUserStats(sid: String) {
        val userFetch = controller.getUser(sid, userId)
        if (userFetch != null) {

            // Aggiorno i dati visuali che potrebbero cambiare con il button follow/unfollow
            numFollowers = userFetch.followersCount
            numFollowing = userFetch.followingCount
            numPosts = userFetch.postsCount
            isFollowing = userFetch.isYourFollowing == true
        }
    }

    // --- CARICAMENTO DATI ---
    LaunchedEffect(userId) {
        val sid = sessionManager.getSid()
        if (sid != null) {
            Log.d(tag, "Apro profilo Utente ID: $userId")

            // 1. Scarica Info Utente
            val user = controller.getUser(sid, userId)
            if (user != null) {
                username = user.username ?: "Utente"
                bio = user.bio ?: ""
                dateOfBirth = user.dateOfBirth ?: ""
                userImageBase64 = user.profilePicture
                isFollowing = user.isYourFollowing == true
                numPosts = user.postsCount
                numFollowers = user.followersCount
                numFollowing = user.followingCount

                Log.d(tag, "Dati utente scaricati: $username, Seguito: $isFollowing")
            } else {
                Log.e(tag, "Impossibile scaricare info utente ID: $userId")
            }

            // 2. Scarica i post
            val listString = controller.getUserPosts(sid, userId)
            if (listString != null) {
                try {
                    val ids = jsonHelper.decodeFromString<List<Int>>(listString)
                    Log.d(tag, "Trovati ${ids.size} post. Scarico dettagli...")

                    val posts = mutableListOf<PostDetail>()
                    for (id in ids) {
                        val pStr = controller.getPost(sid, id)
                        if (pStr != null) {
                            posts.add(jsonHelper.decodeFromString(pStr))
                        }
                    }
                    listaPostUtente = posts.reversed()
                    Log.d(tag, "Caricamento post completato.")
                } catch (e: Exception) {
                    Log.e(tag, "Errore parsing post utente: ${e.message}")
                }
            }
            caricamentoInCorso = false
        }
    }


    Column(modifier = Modifier.fillMaxSize().background(Color.White)) {

        Box() {
            IconButton(onClick = { navController.popBackStack()}) {
                Icon(Icons.Default.ArrowBack, contentDescription = "schermata_precedente", tint = MaterialTheme.colorScheme.primary)
            }
        }


        // IMPORTO COMPONENTE HEADER
        UserProfileHeader(
            username = username,
            bio = bio,
            dateOfBirth = dateOfBirth,
            imageBase64 = userImageBase64,
            postsCount = numPosts,
            followersCount = numFollowers,
            followingCount = numFollowing,
            isMyProfile = false, // NON è il mio profilo
            onEditProfileClick = { /* Non fa nulla */ }
        )
        Spacer(modifier = Modifier.height(8.dp))
        Divider()


        Spacer(modifier = Modifier.height(8.dp))

        // BOTTONE FOLLOW / UNFOLLOW
        Button(
            onClick = {
                scope.launch {

                    val sid = sessionManager.getSid() ?: return@launch

                    var operazioneRiuscita = false // Flag per sapere se dobbiamo aggiornare

                    if (isFollowing) {
                        Log.d(tag, "Tentativo UNFOLLOW su $userId...")
                        if (controller.unfollowUser(sid, userId)) {
                            // Non cambio isFollowing qui, lo faccio fare al refreshUserStats per essere sicuro
                            // Ma per reattività immediata nel log lo lasciamo
                            Log.d(tag, "Unfollow successo.")
                            operazioneRiuscita = true
                        } else {
                            Log.e(tag, "Unfollow fallito.")
                        }
                    } else {
                        Log.d(tag, "Tentativo FOLLOW su $userId...")
                        if (controller.followUser(sid, userId)) {
                            Log.d(tag, "Follow successo.")
                            operazioneRiuscita = true
                        } else {
                            Log.e(tag, "Follow fallito.")
                        }
                    }

                    if (operazioneRiuscita) {
                        refreshUserStats(sid)
                    }
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = if (isFollowing) Color.Gray else MaterialTheme.colorScheme.primary)
        ) {
            Text(if (isFollowing) "Smetti di seguire" else "Segui")
        }


        Divider()

        //IMPORTO COMPONENTE IMAGEGRID
        ImageGrid(
            listaPostUtente,
            caricamentoInCorso,
            onImageClick = { postId ->
                navController.navigate("fullscreen_image/$postId")
            }
        )

    }
}