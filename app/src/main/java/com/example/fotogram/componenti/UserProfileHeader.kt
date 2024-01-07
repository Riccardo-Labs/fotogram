package com.example.fotogram.ui // O .componenti

import android.util.Base64
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cake
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

@Composable
fun UserProfileHeader(
    username: String,
    bio: String,
    dateOfBirth: String,
    imageBase64: String?,
    postsCount: Int,
    followersCount: Int,
    followingCount: Int,
    isMyProfile: Boolean,
    onEditProfileClick: () -> Unit
) {
    //se c'è una foto provo a usarla se l'utente non ha una foto uso ui-avatars.com che restituisce un immagine colorata con le iniziali dell'utente
    val avatarModel = remember(imageBase64, username) {
        if (!imageBase64.isNullOrEmpty()) {
            try { Base64.decode(imageBase64, Base64.DEFAULT) } catch (e: Exception) { null }
        } else {
            "https://ui-avatars.com/api/?name=$username&background=random"
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .background(Color.White)
    ) {

        Row(verticalAlignment = Alignment.CenterVertically) {

            //FOTO PROFILO
            AsyncImage(
                model = avatarModel,
                contentDescription = "Avatar",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(Color.LightGray)
            )
            Spacer(modifier = Modifier.width(16.dp))


            Column {
                //USERNAME
                Text(text = "@$username", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

                //BIO
                if (bio.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = bio, style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                }

                Spacer(modifier = Modifier.height(4.dp))

                //DATA DI NASCITA
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Cake,
                        contentDescription = "Data di nascita",
                        modifier = Modifier.size(16.dp),
                        tint = Color.Gray
                    )
                    Spacer(modifier = Modifier.width(4.dp))

                    val displayText = if (dateOfBirth.isNotEmpty()) {
                        dateOfBirth
                    } else {
                        "Data di nascita non impostata"
                    }

                    Text(
                        text = displayText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                }
            }
        }

        //RIGA STATISTICHE
        Spacer(modifier = Modifier.height(24.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            StatItem("Post", postsCount)
            StatItem("Follower", followersCount)
            StatItem("Seguiti", followingCount)
        }

        //MODIFICA PROFILO
        if (isMyProfile) {
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onEditProfileClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Modifica Profilo")
            }
        }
    }
}


//FUNZIONE COMPOSABLE PER GESTIRE I NUMERETTI DELLE STATISTICHE
@Composable
private fun StatItem(label: String, count: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = count.toString(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
    }
}
