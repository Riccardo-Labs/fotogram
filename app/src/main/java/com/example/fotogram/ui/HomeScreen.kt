package com.example.fotogram.ui

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.fotogram.componenti.PostCard
import com.example.fotogram.data.AppDatabase
import com.example.fotogram.data.CommunicationController
import com.example.fotogram.data.SessionManager
//import com.example.fotogram.model.Post
import com.example.fotogram.model.PostDetail
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlin.random.Random

@Composable
fun HomeScreen(navController: NavController) {
    val tag = "debugger_Home"
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val controller = remember { CommunicationController() }
    val database = remember { AppDatabase.getDatabase(context) }
    val sessionManager = remember { SessionManager(context) }
    val dao = database.postDao()
    val jsonHelper = Json { ignoreUnknownKeys = true; coerceInputValues = true; isLenient = true }

    // --- VARIABILI DI STATO ---
    var listaPost by remember { mutableStateOf<List<PostDetail>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var endReached by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Variabile per mostrare il tasto "Carica altri" se l'automatico fallisce
    var showRetryButton by remember { mutableStateOf(false) }

    // SEED INIZIALE
    var currentSeed by rememberSaveable { mutableIntStateOf(Random.nextInt(50, 800)) }

    // --- 1. CARICAMENTO BACHECA INIZIALE E CON TOGGLE floatingActionButton---
    fun caricaBacheca() {
        if (isLoading) return
        scope.launch {
            isLoading = true
            listaPost = emptyList() // Schermo bianco pulito
            endReached = false
            showRetryButton = false
            errorMessage = null

            val sid = sessionManager.getSid()
            Log.d(tag, "REFRESH TOTALE. Seed Iniziale: $currentSeed")

            if (sid != null) {
                // Prendo i primi 10
                val wallString = controller.getFeed(sid, limit = 10, seed = currentSeed, maxPostId = null) //[34, 24, 54, ecc]

                if (wallString != null) {
                    try {
                        val idRicevuti = jsonHelper.decodeFromString<List<Int>>(wallString)
                        val nuoviPost = mutableListOf<PostDetail>()
                        //var h = 0
                        //var l = 0
                        //var u = 0

                        for (id in idRicevuti) {
                            val postStr = controller.getPost(sid, id)
                            /*
                            val reaction = controller.getReaction(sid, id)
                            if(reaction != null){
                                try {
                                    val r = jsonHelper.decodeFromString<Reaction>(reaction)
                                    Log.d(tag, "Simulazione esame: postID: $id numero di cuori: ${r.hearts}, numero di pollici su ${r.likes}, numero di pollici giù ${r.dislikes}.")
                                    h = r.hearts
                                    l = r.likes
                                    u = r.dislikes
                                } catch (e: Exception) { errorMessage = "Errore dati" }
                            }
                            */

                            if (postStr != null) {
                                val p = jsonHelper.decodeFromString<PostDetail>(postStr)
                                val autore = controller.getUser(sid, p.authorId)
                                if (autore != null) {
                                    p.authorName = autore.username
                                    p.authorProfilePicture = autore.profilePicture
                                    p.isFollowed = autore.isYourFollowing == true
                                    //p.hearts = h
                                    //p.likes = l
                                    //p.dislikes = u
                                } else { p.authorName = "Sconosciuto" }
                                nuoviPost.add(p)
                            }
                        }

                        if (nuoviPost.isNotEmpty()) {
                            dao.deleteAll()
                            dao.insertAll(nuoviPost)
                            listaPost = nuoviPost
                        } else {
                            listaPost = dao.getAll()
                        }
                    } catch (e: Exception) { errorMessage = "Errore dati" }
                } else {
                    errorMessage = "Problema di connessione"
                    val cache = dao.getAll()
                    if (cache.isNotEmpty()) listaPost = cache
                }
            } else { errorMessage = "Login richiesto" }
            isLoading = false
        }
    }

    // --- 2. CARICAMENTO NUOVI POST SOTTO
    fun loadMorePosts() {
        if (isLoading || endReached) return

        scope.launch {
            isLoading = true
            showRetryButton = false // Nascondo il bottone carica altri post mentre provo

            val sid = sessionManager.getSid()
            var postsTrovati = false
            var tentativi = 0
            val maxTentativi = 15 //prova n volta a cercare nuovi post

            // CICLO WHILE: Continua a cambiare seed finché non trova qualcosa di nuovo
            // in pratica si simula la paginazione cambiando il seed (il numero casuale) finché non esce una combinazione di post che contiene ID che non sono ancora usciti
            while (!postsTrovati && tentativi < maxTentativi) {
                tentativi++

                // cambio seed dinamico
                if (currentSeed > 500) {
                    currentSeed -= Random.nextInt(50, 200)
                } else {
                    currentSeed += Random.nextInt(1, 200)
                }

                Log.d(tag, "Tentativo $tentativi: Nuovo Seed: $currentSeed")

                if (sid != null) {
                    val wallString = controller.getFeed(sid, limit = 10, seed = currentSeed, maxPostId = null)

                    if (wallString != null) {
                        try {
                            val idRicevuti = jsonHelper.decodeFromString<List<Int>>(wallString)

                            if (idRicevuti.isEmpty()) {
                                endReached = true

                                break // Esco dal while, non c'è più nulla

                            } else {
                                val nuoviPost = mutableListOf<PostDetail>()

                                for (id in idRicevuti) {
                                    // CONTROLLO DUPLICATI
                                    val giaPresente = listaPost.any { it.id == id }
                                    if (!giaPresente) {
                                        val postStr = controller.getPost(sid, id)
                                        if (postStr != null) {
                                            val p = jsonHelper.decodeFromString<PostDetail>(postStr)
                                            val autore = controller.getUser(sid, p.authorId)
                                            if (autore != null) {
                                                p.authorName = autore.username
                                                p.authorProfilePicture = autore.profilePicture
                                                p.isFollowed = autore.isYourFollowing == true
                                            } else { p.authorName = "Sconosciuto" }
                                            nuoviPost.add(p)
                                        }
                                    }
                                }

                                if (nuoviPost.isNotEmpty()) {
                                    listaPost = listaPost + nuoviPost
                                    dao.insertAll(nuoviPost)

                                    //post trovati esco dal while
                                    postsTrovati = true
                                }
                            }
                        } catch (e: Exception) {
                            Log.e(tag, "Errore tentativo $tentativi: ${e.message}")
                        }
                    }
                }
            } // Fine While

            // Se dopo n tentativi ancora niente, mostro il bottone manuale
            if (!postsTrovati) {
                if (!endReached) {
                    Log.w(tag, "Solo duplicati dopo $maxTentativi tentativi.")
                    showRetryButton = true

                    // Toast per fare capire all'utente che il click ha funzionato, ma non c'erano nuovi post
                    android.widget.Toast.makeText(
                        context,
                        "Nessun post nuovo trovato. Riprova!",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
            }

            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        // se i post sono presenti in cache li carichiamo subito poi parte caricaBacheca() per prendere nuovi post dal server
        val cache = dao.getAll()
        if (cache.isNotEmpty()) listaPost = cache
        caricaBacheca()
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    currentSeed = Random.nextInt(0, 10000)
                    caricaBacheca()
                },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Refresh, contentDescription = "Ricarica")
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {

            // A. SCHERMATA ERRORE O VUOTA
            if (listaPost.isEmpty() && !isLoading) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(errorMessage ?: "Nessun post.")
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = { caricaBacheca() }) { Text("Riprova") }
                }
            }
            // B. LISTA POST
            else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 35.dp)
                ) {
                    // USA itemsIndexed PER AVERE IL NUMERO DI RIGA
                    itemsIndexed(listaPost) { index, post ->

                        /*
                        // Se siamo a un multiplo di 5 (5, 10, 15...) disegna PRIMA la pubblicità
                        //ogni cinque post mostrati nella bacheca, adCardWrapper recuperare un annuncio pubblicitario
                        if ((index + 1) % 5 == 0) {
                            AdCardWrapper(
                                controller = controller,
                                sessionManager = sessionManager
                            )
                        }*/

                        // --- IL POST NORMALE ---
                        // Questo viene disegnato SEMPRE (subito dopo l'eventuale pubblicità)
                        PostCard(
                            post = post,
                            onAuthorClick = { navController.navigate("user/${it}") },
                            onImageClick = { encoded ->
                                navController.navigate("fullscreen_image/${post.id}")
                            }
                        )
                    }

                    // C. FOOTER per Infinite Scroll quando l'utente scorre verso il basso e arriva all'ultimo post, questo item entra nello schermo.
                    //Appena entra nello schermo, Compose esegue il codice al suo interno.
                    item {
                        if (!endReached && listaPost.isNotEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isLoading) {
                                    CircularProgressIndicator()
                                } else if (showRetryButton) {
                                    Button(onClick = { loadMorePosts() }) {
                                        Text("Carica altri post")
                                    }
                                } else {
                                    LaunchedEffect(Unit) { loadMorePosts() }
                                }
                            }
                        } else if (endReached) {
                            Text("Hai visto tutto!", modifier = Modifier.padding(16.dp), color = Color.Gray) //finiti i post nel database
                        }
                    }
                }
            }

            // D. SPINNER CENTRALE (Solo se lista vuota all'inizio o nel caricamento tramite floatingActionButton)
            if (isLoading && listaPost.isEmpty()) {
                CircularProgressIndicator()
            }
        }
    }
}