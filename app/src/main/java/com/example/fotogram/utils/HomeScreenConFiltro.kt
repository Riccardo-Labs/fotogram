package com.example.fotogram.utils

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
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
import com.example.fotogram.model.PostDetail
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlin.random.Random

@Composable
fun HomeScreenConFiltro(navController: NavController) {
    val tag = "debugger_Home"
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Controller e Database
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

    // Filtro "Solo Seguiti"
    var soloSeguiti by remember { mutableStateOf(false) }

    // Bottone Retry manuale
    var showRetryButton by remember { mutableStateOf(false) }

    // Seed per randomizzazione server
    var currentSeed by rememberSaveable { mutableIntStateOf(Random.nextInt(50, 800)) }

    // ----------------------------------------------------------------
    // FUNZIONI DI CARICAMENTO
    // ----------------------------------------------------------------

    // 1. CARICAMENTO INIZIALE (Reset)
    fun caricaBacheca() {
        if (isLoading) return
        scope.launch {
            isLoading = true
            errorMessage = null
            showRetryButton = false
            endReached = false

            // Pulisco la lista visiva (opzionale, se vuoi vedere lo spinner su bianco)
            // listaPost = emptyList()

            val sid = sessionManager.getSid()
            Log.d(tag, "REFRESH TOTALE. Seed Iniziale: $currentSeed")

            if (sid != null) {
                // Scarica i primi 10 post
                val wallString = controller.getFeed(sid, limit = 10, seed = currentSeed, maxPostId = null)

                if (wallString != null) {
                    try {
                        val idRicevuti = jsonHelper.decodeFromString<List<Int>>(wallString)
                        val nuoviPost = mutableListOf<PostDetail>()

                        for (id in idRicevuti) {
                            val postStr = controller.getPost(sid, id)
                            if (postStr != null) {
                                val p = jsonHelper.decodeFromString<PostDetail>(postStr)

                                // Arricchisco con dati utente (Nome, Foto, Follow)
                                val autore = controller.getUser(sid, p.authorId)
                                if (autore != null) {
                                    p.authorName = autore.username
                                    p.authorProfilePicture = autore.profilePicture
                                    p.isFollowed = autore.isYourFollowing == true
                                } else {
                                    p.authorName = "Sconosciuto"
                                }
                                nuoviPost.add(p)
                            }
                        }

                        if (nuoviPost.isNotEmpty()) {
                            // Salvo in DB (Cache) e aggiorno UI
                            dao.deleteAll()
                            dao.insertAll(nuoviPost)
                            listaPost = nuoviPost
                        } else {
                            // Se il server è vuoto, provo la cache
                            listaPost = dao.getAll()
                        }
                    } catch (e: Exception) {
                        errorMessage = "Errore nel parsing dati"
                    }
                } else {
                    errorMessage = "Problema di connessione"
                    val cache = dao.getAll()
                    if (cache.isNotEmpty()) listaPost = cache
                }
            } else {
                errorMessage = "Login richiesto"
            }
            isLoading = false
        }
    }

    // 2. CARICAMENTO PAGINE SUCCESSIVE (Infinite Scroll)
    fun loadMorePosts() {
        if (isLoading || endReached) return

        scope.launch {
            isLoading = true
            showRetryButton = false

            val sid = sessionManager.getSid()
            var postsTrovati = false
            var tentativi = 0
            val maxTentativi = 15

            // Algoritmo di retry con cambio seed per trovare post nuovi
            while (!postsTrovati && tentativi < maxTentativi) {
                tentativi++
                if (currentSeed > 500) currentSeed -= Random.nextInt(50, 200)
                else currentSeed += Random.nextInt(1, 200)

                Log.d(tag, "LoadMore - Tentativo $tentativi seed $currentSeed")

                if (sid != null) {
                    val wallString = controller.getFeed(sid, limit = 10, seed = currentSeed, maxPostId = null)
                    if (wallString != null) {
                        try {
                            val idRicevuti = jsonHelper.decodeFromString<List<Int>>(wallString)

                            if (idRicevuti.isEmpty()) {
                                endReached = true
                                break
                            } else {
                                val nuoviPost = mutableListOf<PostDetail>()
                                for (id in idRicevuti) {
                                    // Evito duplicati
                                    if (!listaPost.any { it.id == id }) {
                                        val postStr = controller.getPost(sid, id)
                                        if (postStr != null) {
                                            val p = jsonHelper.decodeFromString<PostDetail>(postStr)
                                            val autore = controller.getUser(sid, p.authorId)
                                            if (autore != null) {
                                                p.authorName = autore.username
                                                p.authorProfilePicture = autore.profilePicture
                                                p.isFollowed = autore.isYourFollowing == true
                                            }
                                            nuoviPost.add(p)
                                        }
                                    }
                                }

                                if (nuoviPost.isNotEmpty()) {
                                    listaPost = listaPost + nuoviPost
                                    dao.insertAll(nuoviPost)
                                    postsTrovati = true
                                }
                            }
                        } catch (e: Exception) { Log.e(tag, "Errore loadMore: ${e.message}") }
                    }
                }
            }

            if (!postsTrovati && !endReached) {
                showRetryButton = true
                Toast.makeText(context, "Nessun post nuovo. Riprova!", Toast.LENGTH_SHORT).show()
            }
            isLoading = false
        }
    }

    // Avvio automatico (Prima Cache, poi Rete)
    LaunchedEffect(Unit) {
        val cache = dao.getAll()
        if (cache.isNotEmpty()) listaPost = cache
        caricaBacheca()
    }

    // ----------------------------------------------------------------
    // INTERFACCIA GRAFICA (UI)
    // ----------------------------------------------------------------
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

        // CORREZIONE 1: Uso una Column per impilare Switch e Lista
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {

            // --- A. BARRA FILTRO (Sempre visibile in alto) ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text("Mostra solo seguiti")
                Spacer(modifier = Modifier.width(8.dp))
                Switch(
                    checked = soloSeguiti,
                    onCheckedChange = { soloSeguiti = it }
                )
            }

            // --- B. AREA LISTA + ERRORI ---
            Box(
                modifier = Modifier.weight(1f), // Occupa tutto lo spazio rimanente
                contentAlignment = Alignment.Center
            ) {

                // Caso 1: Errore o Lista Vuota (e non sta caricando)
                if (listaPost.isEmpty() && !isLoading) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(errorMessage ?: "Nessun post trovato.")
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = { caricaBacheca() }) { Text("Riprova") }
                    }
                }
                // Caso 2: Lista Piena -> Mostro i dati
                else {

                    // CORREZIONE 2: Calcolo la lista da passare alla LazyColumn
                    val listaDaMostrare = if (soloSeguiti) {
                        listaPost.filter { it.isFollowed == true }
                    } else {
                        listaPost
                    }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 80.dp) // Spazio per il FAB
                    ) {

                        // Iteriamo sulla lista corretta (Filtrata o Completa)
                        itemsIndexed(listaDaMostrare) { index, post ->

                            /*
                            // -----------------------------------------------------
                            // ZONA PUBBLICITÀ (Simulazione 3)
                            // Logica: Ogni 5 post, inserisci un annuncio
                            // -----------------------------------------------------
                            if ((index + 1) % 5 == 0) {
                                AdCardWrapper(
                                    controller = controller,
                                    sessionManager = sessionManager
                                )
                            }
                            // -----------------------------------------------------
                            */

                            // Post Normale
                            PostCard(
                                post = post,
                                onAuthorClick = { navController.navigate("user/${it}") },
                                onImageClick = {
                                    // Navigazione sicura con ID
                                    navController.navigate("fullscreen_image/${post.id}")
                                }
                            )
                        }

                        // Footer Infinite Scroll (Solo se non sto filtrando)
                        if (!soloSeguiti) {
                            item {
                                if (!endReached && listaPost.isNotEmpty()) {
                                    Box(
                                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isLoading) {
                                            CircularProgressIndicator()
                                        } else if (showRetryButton) {
                                            Button(onClick = { loadMorePosts() }) { Text("Carica altri") }
                                        } else {
                                            LaunchedEffect(Unit) { loadMorePosts() }
                                        }
                                    }
                                } else if (endReached) {
                                    Text("Hai visto tutto!", modifier = Modifier.padding(16.dp), color = Color.Gray)
                                }
                            }
                        }
                    }
                }

                // Caso 3: Caricamento Iniziale (Spinner al centro)
                if (isLoading && listaPost.isEmpty()) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}