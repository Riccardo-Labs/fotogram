package com.example.fotogram.ui

import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.fotogram.data.CommunicationController
import com.example.fotogram.data.SessionManager
import com.example.fotogram.model.PostDetail
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

@Composable
fun FullScreenImageScreen(navController: NavController, postId: Int) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val controller = remember { CommunicationController() }
    val sessionManager = remember { SessionManager(context) }
    val jsonHelper = Json { ignoreUnknownKeys = true; coerceInputValues = true; isLenient = true }

    // Stato del post scaricato
    var currentPost by remember { mutableStateOf<PostDetail?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    // 1. SCARICAMENTO DATI
    LaunchedEffect(postId) {
        scope.launch {
            val sid = sessionManager.getSid()
            if (sid != null) {
                try {
                    val postStr = controller.getPost(sid, postId)
                    if (postStr != null) {
                        val p = jsonHelper.decodeFromString<PostDetail>(postStr)

                        // Scarichiamo anche l'autore per avere il nome aggiornato
                        val autore = controller.getUser(sid, p.authorId)
                        if (autore != null) {
                            p.authorName = autore.username
                        }
                        currentPost = p
                    }
                } catch (e: Exception) {
                    Log.e("FullScreen", "Errore download post: ${e.message}")
                }
            }
            isLoading = false
        }
    }

    // 2. INTERFACCIA
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }
        else if (currentPost != null) {
            val post = currentPost!!

            // DECODIFICA IMMAGINE
            val imageBitmap = remember(post.contentPicture) {
                try {
                    val bytes = Base64.decode(post.contentPicture, Base64.DEFAULT)
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                } catch (e: Exception) { null }
            }

            // A. IMMAGINE
            if (imageBitmap != null) {
                AsyncImage(
                    model = imageBitmap,
                    contentDescription = "Full Screen",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit // Adatta l'immagine senza tagliarla
                )
            }

            // B. OMBRA SFUMATA IN BASSO (per leggere meglio il testo)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .height(200.dp)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))
                        )
                    )
            )

            // C. TESTI (Nome e Descrizione)
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp)
                    .padding(bottom = 24.dp) // Spazio per la barra di navigazione
            ) {
                // Nome Autore
                Text(
                    text = "@${post.authorName ?: "Utente"}",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Testo del Post
                if (!post.contentText.isNullOrBlank()) {
                    Text(
                        text = post.contentText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White
                    )
                }
            }
        }

        // D. PULSANTE INDIETRO (Sempre visibile in alto a sinistra)
        IconButton(
            onClick = { navController.popBackStack() },
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.5f))
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Indietro",
                tint = Color.White
            )
        }
    }
}