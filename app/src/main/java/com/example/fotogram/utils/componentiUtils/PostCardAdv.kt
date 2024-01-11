package com.example.fotogram.utils.componentiUtils

import androidx.compose.runtime.Composable

//import com.example.fotogram.model.Adv

@Composable
fun PostCardAdv(/*
    adv: Adv,
    //onAuthorClick: (Int) -> Unit,
    //onImageClick: (String) -> Unit,
    //onReactionClick: (String) -> Unit  //aggiungo dopo per reazione alla adv */
) {
    /*
    // Usiamo 'remember' per non decodificare l'immagine a ogni piccolo movimento (più veloce)
    val postBitmap = remember(adv.image) {
        ImageUtils.decodeBase64(adv.image)
    }


    // LOGICA COLORE BORDO SCHEDA
    val borderColor = Color(0xFF673AB7)
    val borderWidth = 2.dp

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        border = BorderStroke(borderWidth, borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {



                Spacer(modifier = Modifier.width(10.dp))

                // 2. NOME UTENTE
                Text(
                    text = "SPONSORIZZATA",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.weight(1f))

            }


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
                        .clip(MaterialTheme.shapes.medium) // Arrotonda un po' gli angoli della foto ,
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

        }
    }*/
}
