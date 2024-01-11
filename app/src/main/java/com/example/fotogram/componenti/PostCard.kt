package com.example.fotogram.componenti

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Task
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.example.fotogram.ImageUtils
import com.example.fotogram.Utils
import com.example.fotogram.model.PostDetail
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@Composable
fun PostCard(
    post: PostDetail,
    onAuthorClick: (Int) -> Unit,
    onImageClick: (String) -> Unit,
    //onReactionClick: (String) -> Unit
) {

    val postBitmap = remember(post.contentPicture) { // Usiamo 'remember' per non decodificare l'immagine a ogni piccolo movimento (più veloce)
        ImageUtils.decodeBase64(post.contentPicture)
    }

    // Decodifica foto profilo (se c'è)
    val profileBitmap = remember(post.authorProfilePicture) {
        post.authorProfilePicture?.let { ImageUtils.decodeBase64(it) }
    }

    // se si segue l'autore (isFollowed = true) la card ottiene modifiche di stile
    val borderColor = if (post.isFollowed) Color(0xFFFFD700) else Color.Transparent
    val borderWidth = if (post.isFollowed) 2.dp else 0.dp

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        border = BorderStroke(borderWidth, borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {

            // --- HEADER (Foto Profilo + Nome + Stellina a destra) ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onAuthorClick(post.authorId) }
                    .padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {

                // 1. FOTO PROFILO
                if (profileBitmap != null) {
                    Image(
                        bitmap = profileBitmap,
                        contentDescription = "Avatar",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .border(1.dp, Color.Gray, CircleShape)
                    )
                } else {
                    // Placeholder se non ha foto
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Avatar nullo",
                        tint = Color.Gray,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .border(1.dp, Color.Gray, CircleShape)
                            .padding(4.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                // 2. NOME UTENTE
                Text(
                    text = "@${post.authorName ?: "Sconosciuto"}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                /*
                Spacer(modifier = Modifier.weight(0.2f))

                IconButton(onClick = { onReactionClick("SALVA") }) {
                    Icon(
                        imageVector = Icons.Default.Task,
                        contentDescription = "hearts",
                        tint = Color(0xFF009688),
                        modifier = Modifier.size(15.dp)
                    )
                }*/
                /*
                Spacer(modifier = Modifier.weight(0.2f))
                Text(
                    text = "${post.likes ?: 0}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                IconButton(onClick = { onReactionClick("LIKE") }) {
                    Icon(
                        imageVector = Icons.Default.ThumbUp,
                        contentDescription = "like",
                        tint = Color(0xFF2196F3),
                        modifier = Modifier.size(15.dp)
                    )
                }

                Spacer(modifier = Modifier.weight(0.2f))
                Text(
                    text = "${post.dislikes ?: 0}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                IconButton(onClick = { onReactionClick("DISLIKE") }) {
                    Icon(
                        imageVector = Icons.Default.ThumbDown,
                        contentDescription = "ThumbDown",
                        tint = Color(0xFF2196F3),
                        modifier = Modifier.size(15.dp)
                    )
                }
                */
                Spacer(modifier = Modifier.weight(1f))

                // 4. STELLINA (Solo se seguito)
                if (post.isFollowed) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Seguito",
                        tint = Color(0xFFFFD700), // Oro
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            // Data
            Text(
                text = Utils.formatTimeAgo(post.createdAt),
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Immagine Post
            if (postBitmap != null) {
                // --- Aggiungiamo il modifier cliccabile qui ---
                Image(
                    bitmap = postBitmap,
                    contentDescription = "Immagine del post",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .clip(MaterialTheme.shapes.medium) // Arrotonda un po' gli angoli della foto
                        .clickable {
                            // Codifichiamo la stringa Base64 per renderla sicura per un URL.
                            // Usiamo URL_SAFE per evitare problemi con caratteri speciali.
                            val encodedImage = URLEncoder.encode(post.contentPicture, StandardCharsets.UTF_8.toString())
                            // Eseguiamo la callback passando l'immagine codificata
                            onImageClick(encodedImage)
                        },
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Immagine non disponibile", color = Color.Gray)
                }
            }

            // Testo Post (se c'è)
            if (!post.contentText.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = post.contentText)
            }
        }
    }
}

