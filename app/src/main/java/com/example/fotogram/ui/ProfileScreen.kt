package com.example.fotogram.ui

import android.util.Base64
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

@Composable
fun ProfileScreen(navController: NavController) {
    val tag = "debugger_Profile-Screen"
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val controller = remember { CommunicationController() }
    val sessionManager = remember { SessionManager(context) }
    val jsonHelper = Json { ignoreUnknownKeys = true; coerceInputValues = true; isLenient = true }

    // --- STATO ---
    var listaPostUtente by remember { mutableStateOf<List<PostDetail>>(emptyList()) }
    var nomeUtente by remember { mutableStateOf("Caricamento...") }
    var bioUtente by remember { mutableStateOf("") }
    var dateOfBirth by remember { mutableStateOf("") }
    var immagineProfiloBase64 by remember { mutableStateOf<String?>(null) }
    var numPosts by remember { mutableIntStateOf(0) }
    var numFollowers by remember { mutableIntStateOf(0) }
    var numFollowing by remember { mutableIntStateOf(0) }

    var caricamentoInCorso by remember { mutableStateOf(true) }

    // --- CARICAMENTO ---
    LaunchedEffect(Unit) {
        val sid = sessionManager.getSid()
        val myId = sessionManager.getUid()

        if (sid != null && myId != -1) {
            try {
                // 1. INFO UTENTE
                val user = controller.getUser(sid, myId)
                if (user != null) {
                    nomeUtente = user.username ?: "Senza Nome"
                    bioUtente = user.bio ?: ""
                    dateOfBirth = user.dateOfBirth ?: ""
                    numPosts = user.postsCount
                    numFollowers = user.followersCount
                    numFollowing = user.followingCount
                    Log.d(tag, "Dati utente scaricati: $nomeUtente, $bioUtente, $dateOfBirth, $numPosts, $numFollowers, $numFollowing")

                    immagineProfiloBase64 = user.profilePicture
                    if (user.profilePicture != null) {
                        sessionManager.saveProfilePic(user.profilePicture)
                    }
                }

                // 2. LISTA POST
                val userPostsString = controller.getUserPosts(sid, myId)
                if (userPostsString != null) {
                    val listaIdPost = jsonHelper.decodeFromString<List<Int>>(userPostsString)
                    val listaDettagli = mutableListOf<PostDetail>()
                    for (id in listaIdPost) {
                        val postString = controller.getPost(sid, id)
                        if (postString != null) listaDettagli.add(jsonHelper.decodeFromString(postString))
                    }
                    listaPostUtente = listaDettagli.reversed()
                }
            } catch (e: Exception) {
                Log.e(tag, "Errore: ${e.message}")
            } finally {
                caricamentoInCorso = false
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(Color.White)) {

        Row() {
            IconButton(onClick = { navController.popBackStack()}) {
                Icon(Icons.Default.ArrowBack, contentDescription = "schermata_precedente", tint = MaterialTheme.colorScheme.primary)
            }

            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                IconButton(onClick = {
                    scope.launch {
                        sessionManager.clearSession()
                        navController.navigate("register") { popUpTo(0) { inclusive = true } }
                    }
                }) {
                    Icon(Icons.Default.Logout, contentDescription = "Esci", tint = MaterialTheme.colorScheme.error)
                }

            }
        }


        // IMPORTO COMPONENTE HEADER
        UserProfileHeader(
            username = nomeUtente,
            bio = bioUtente,
            dateOfBirth = dateOfBirth,
            imageBase64 = immagineProfiloBase64,
            postsCount = numPosts,
            followersCount = numFollowers,
            followingCount = numFollowing,
            isMyProfile = true, //è il mio profilo
            onEditProfileClick = {
                navController.navigate("edit_profile")
            }
        )

        Spacer(modifier = Modifier.height(8.dp))
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