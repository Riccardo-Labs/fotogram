package com.example.fotogram.ui

import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.fotogram.model.User
import com.example.fotogram.data.CommunicationController
import com.example.fotogram.data.SessionManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(navController: NavController) {
    // TAG per filtrare i messaggi nel Logcat
    val tag = "debugger_EditProfile"

    val context = LocalContext.current
    // Inizializzazione dei controller e gestori
    val controller = remember { CommunicationController() }
    val sessionManager = remember { SessionManager(context) }
    val coroutineScope = rememberCoroutineScope() // Necessario per operazioni asincrone (Salvataggio)

    // --- STATI UI (Variabili che modificano l'interfaccia) ---
    var user by remember { mutableStateOf<User?>(null) } // L'oggetto utente completo

    // Variabili per i campi di testo modificabili
    var editableNome by remember { mutableStateOf("") }
    var editableBio by remember { mutableStateOf("") }
    var editableDate by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }

    // Launcher Foto
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedImageUri = uri
        Log.d(tag, "📸 Foto selezionata: $uri")
    }
    // Stati di controllo per il caricamento
    var isLoading by remember { mutableStateOf(true) } // Rotellina iniziale
    var isSaving by remember { mutableStateOf(false) } // Rotellina sul bottone salva

    // --- CARICAMENTO DATI INIZIALE ---
    // LaunchedEffect(Unit) parte una sola volta quando la pagina viene creata
    LaunchedEffect(Unit) {
        val sid = sessionManager.getSid()
        // --- MODIFICA 1: Recupero l'ID utente ---
        val myId = sessionManager.getUid() // Questo restituisce un Int (es. -1 se non trovato)

        Log.d(tag, "--- Apertura EditProfileScreen ---")
        Log.d(tag, "ID Utente recuperato: $myId")

        // --- MODIFICA 2: Controllo corretto su SID e ID Utente ---
        // Controlliamo che il SID esista e che l'ID utente sia valido (diverso da -1)
        if (sid != null && myId != -1) {
            // Chiamata al server per ottenere i dati attuali
            // La funzione getUser si aspetta un Int, quindi passiamo myId direttamente
            user = controller.getUser(sid, myId)

            // Se l'utente è stato scaricato, riempiamo i campi di testo
            user?.let {
                editableNome = it.username ?: ""
                editableBio = it.bio ?: ""
                editableDate = it.dateOfBirth ?: ""
                Log.d(tag, "Dati utente caricati con successo. Nome: $editableNome, Data: $editableDate")
            } ?: run {
                Log.e(tag, "Errore: Oggetto User è null dopo il caricamento.")
            }
        } else {
            Log.e(tag, "Errore: SessionID ($sid) o UserID ($myId) non validi.")
        }
        // Finito il caricamento, nascondiamo la rotellina a schermo intero
        isLoading = false
    }

    // --- STRUTTURA DELLA PAGINA (Scaffold) ---
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Modifica Profilo") },
                navigationIcon = {
                    IconButton(onClick = {
                        Log.d(tag, "Tasto indietro premuto.")
                        navController.popBackStack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Indietro")
                    }
                }
            )
        }
    ) { paddingValues ->

        // Se sta ancora caricando i dati iniziali, mostra solo la rotellina al centro
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            // Se i dati ci sono, mostra il modulo
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp)
            ) {
                // --- SELEZIONE FOTO ---
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .background(if (selectedImageUri == null) Color.LightGray else Color.Transparent)
                        .clickable { galleryLauncher.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    if (selectedImageUri != null) {
                        Image(
                            painter = rememberAsyncImagePainter(selectedImageUri),
                            contentDescription = "Avatar",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.AddAPhoto, contentDescription = null, tint = Color.DarkGray)
                            Text("Foto", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
                // CAMPO NOME
                OutlinedTextField(
                    value = editableNome,
                    onValueChange = { editableNome = it },
                    label = { Text("Nome Utente") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))

                // CAMPO BIO
                OutlinedTextField(
                    value = editableBio,
                    onValueChange = { editableBio = it },
                    label = { Text("Bio") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))

                // CAMPO DATA DI NASCITA
                OutlinedTextField(
                    value = editableDate,
                    onValueChange = { editableDate = it },
                    label = { Text("Data di Nascita (GG/MM/AAAA)") },
                    placeholder = { Text("Es. 1990-05-25") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(32.dp))

                // BOTTONE SALVA
                Button(
                    onClick = {
                        // Quando clicco salva...
                        isSaving = true // Attiva la rotellina sul bottone
                        coroutineScope.launch {
                            val sid = sessionManager.getSid()


                            Log.d(tag, "Inizio procedura di salvataggio...")
                            Log.d(tag, "Dati da inviare -> Nome: $editableNome, Bio: $editableBio, Data: $editableDate")

                            if (sid != null) {
                                //UPDATE UTENTE
                                val updatedUser = controller.updateUser(sid, editableNome, editableBio, editableDate)
                                Log.d(tag, "Update dati utente: $updatedUser")


                                //UPLOAD FOTO
                                if(selectedImageUri != null){
                                    try {
                                        val inputStream = context.contentResolver.openInputStream(selectedImageUri!!)
                                        val bytes = inputStream?.readBytes()
                                        if (bytes != null) {
                                            val fotoOk = controller.updateProfilePicture(sid, bytes)
                                            if (fotoOk) Log.d(tag, "Foto caricata correttamente.")
                                            else Log.e(tag, "Upload foto fallito lato server")
                                        }
                                    } catch (e: Exception) {
                                        Log.e(tag, "Errore lettura file foto: ${e.message}")
                                    }
                                }
                                if (updatedUser != null) {
                                    Log.d(tag, "Salvataggio riuscito! Utente aggiornato: ${updatedUser}")

                                    navController.popBackStack() // Torna indietro alla schermata del profilo
                                } else {
                                    Log.e(tag, "Errore: updateUser ha restituito null (salvataggio fallito).")
                                    Toast.makeText(context, "Errore durante il salvataggio", Toast.LENGTH_SHORT)
                                }
                            } else {
                                Log.e(tag, "Errore: SessionID nullo durante il salvataggio.")
                            }
                            isSaving = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isSaving // Disabilita il tasto se sta già salvando
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                    } else {
                        Text("Salva Modifiche")
                    }
                }
            }
        }
    }
}
