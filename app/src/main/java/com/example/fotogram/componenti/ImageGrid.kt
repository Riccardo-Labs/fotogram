package com.example.fotogram.componenti

import android.util.Base64
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.example.fotogram.model.PostDetail

@Composable
fun ImageGrid (
    listaPostUtente: List<PostDetail>,
    caricamentoInCorso: Boolean,
    onImageClick: (Int) -> Unit
) {
    //CARICAMENTO
    if (caricamentoInCorso) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }

    //LISTA POST VUOTA
    } else if (listaPostUtente.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Nessun post.", color = Color.Gray) }

    //LISTA POST PIENA >= 1
    } else {
        LazyVerticalGrid(columns = GridCells.Fixed(3)) { //disegna solo le immagini che sono visibili in quel momento e divide lo schermo in 3 colonne fisse

            items(listaPostUtente) { post ->
                val imageModel = remember(post.contentPicture) {
                    //le immagini possono arrivare in due modi: se è un link (http...) la usa così com'è se è in base64 la converte in ByteArray
                    if (post.contentPicture.startsWith("http")) post.contentPicture
                    else try { Base64.decode(post.contentPicture, Base64.DEFAULT) } catch (e: Exception) { null }
                }

                AsyncImage(
                    model = imageModel,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .aspectRatio(1f) //forzata in un quadrato per mantenere la griglia ordinata
                        .background(Color.LightGray)
                        .clickable {
                            onImageClick(post.id) //passiamo una stringa al padre (callback)
                        },
                )
            }
        }
    }
}