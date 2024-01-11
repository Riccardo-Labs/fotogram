package com.example.fotogram.utils


import androidx.compose.runtime.Composable
import androidx.navigation.NavController

@Composable
fun postSalvatiScreen(navController: NavController) {
   /* val tag = "debugger_PostSalvati"
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

    fun caricaPostSalvati() {
        if (isLoading) return
        scope.launch {
            isLoading = true
            listaPost = emptyList() // Schermo bianco pulito
            endReached = false
            showRetryButton = false
            errorMessage = null

            val sid = sessionManager.getSid()

            if (sid != null) {
                // Prendo i primi 10
                val postSalvati = controller.getSavedPost(sid)
                if (postSalvati != null) {
                    Log.d(tag, "Post salvati ricevuti: $postSalvati")
                    try {
                        val idRicevuti = jsonHelper.decodeFromString<List<Int>>(postSalvati)
                        val nuoviPost = mutableListOf<PostDetail>()
                        for (id in idRicevuti) {
                            val postStr = controller.getPost(sid, id)

                            if (postStr != null) {
                                val p = jsonHelper.decodeFromString<PostDetail>(postStr)
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
                            dao.deleteAll()
                            dao.insertAll(nuoviPost)
                            listaPost = nuoviPost
                        } else {
                            listaPost = dao.getAll()
                        }
                    } catch (e: Exception) { errorMessage = "Errore dati"}

                }
            } else {
                errorMessage = "Login richiesto"
            }
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {

        caricaPostSalvati()
    }

    Scaffold(
    ) { padding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentAlignment = Alignment.Center
        ) {

            // A. SCHERMATA ERRORE O VUOTA
            if (listaPost.isEmpty() && !isLoading) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(errorMessage ?: "Nessun post.")
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = { caricaPostSalvati() }) { Text("Riprova") }
                }
            }
            // B. LISTA POST
            else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 35.dp)
                ) {
                    items(listaPost) { post ->
                        PostCard(
                            post = post,
                            onAuthorClick = { navController.navigate("user/${it}") },
                            onImageClick = { encodedImageBase64 ->
                                val safe = java.net.URLEncoder.encode(encodedImageBase64, "UTF-8")
                                navController.navigate("fullscreen_image/$safe")
                            },

                            /*onReactionClick = { salva ->

                            }*/

                        )
                    }

                }
            }

            // D. SPINNER CENTRALE (Solo se lista vuota all'inizio o nel caricamento tramite floatingActionButton)
            if (isLoading && listaPost.isEmpty()) {
                CircularProgressIndicator()
            }
        }
    }*/
}
