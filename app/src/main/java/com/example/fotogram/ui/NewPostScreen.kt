package com.example.fotogram.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.ModifierLocalBeyondBoundsLayout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.fotogram.data.CommunicationController
import com.example.fotogram.data.SessionManager
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.launch

@Composable
fun NewPostScreen(navController: NavController) {
    // TAG UNIVOCO PER DEBUG
    val tag = "debugger_NewPost-Screen"

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val controller = remember { CommunicationController() }
    val sessionManager = remember { SessionManager(context) }

    // Client per il GPS (FusedLocationProvider)
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    // --- STATI UI ---
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var description by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    // --- STATI POSIZIONE ---
    var addLocation by remember { mutableStateOf(false) }
    var currentLat by remember { mutableStateOf<Double?>(null) }
    var currentLng by remember { mutableStateOf<Double?>(null) }

    // --- LAUNCHER 1: GALLERIA ---
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            Log.d(tag, "Foto selezionata dalla galleria: $uri")
            selectedImageUri = uri
        } else {
            Log.d(tag, "Selezione foto annullata dall'utente.")
        }
    }

    // --- LAUNCHER 2: PERMESSO GPS ---
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            Log.d(tag, "Permesso GPS concesso dall'utente.")
        } else {
            Log.w(tag, "Permesso GPS negato.")
            addLocation = false
            Toast.makeText(context, "Permesso GPS negato", Toast.LENGTH_SHORT).show()
        }
    }

    // --- FUNZIONE HELPER: OTTIENI POSIZIONE ---
    @SuppressLint("MissingPermission")
    fun getLocation() {
        Log.d(tag, "Tentativo recupero posizione GPS...")

        // Controllo se abbiamo il permesso
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    currentLat = location.latitude
                    currentLng = location.longitude
                    Log.d(tag, "Posizione trovata: Lat=${currentLat}, Lng=${currentLng}")
                    Toast.makeText(context, "Posizione trovata! 📍", Toast.LENGTH_SHORT).show()
                } else {
                    Log.w(tag, "GPS attivo ma 'lastLocation' è null (emulatore o segnale assente).")
                    Toast.makeText(context, "Impossibile trovare posizione (attiva GPS)", Toast.LENGTH_SHORT).show()
                }
            }.addOnFailureListener {
                Log.e(tag, "Errore FusedLocation: ${it.message}")
            }

        } else {
            Log.d(tag, "Permesso mancante. Richiedo permesso all'utente...")
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    // --- UI ---
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        //pulsante torna indietro
        IconButton(onClick = { navController.popBackStack()}) {
            Icon(Icons.Default.ArrowBack, contentDescription = "schermata_precedente", tint = MaterialTheme.colorScheme.primary)
        }

        Spacer(modifier = Modifier.height(24.dp))

        // AREA SELEZIONE FOTO
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .background(Color.LightGray, shape = RectangleShape)
                .clickable { galleryLauncher.launch("image/*") },
            contentAlignment = Alignment.Center
        ) {
            if (selectedImageUri != null) {
                Image(
                    painter = rememberAsyncImagePainter(selectedImageUri),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.AddAPhoto, "Aggiungi", tint = Color.DarkGray, modifier = Modifier.size(48.dp))
                    Text("Tocca per scegliere una foto", color = Color.DarkGray)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // CAMPO TESTO
        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Scrivi qualcosa...") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2
        )

        Spacer(modifier = Modifier.height(16.dp))

        // --- CHECKBOX POSIZIONE ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = addLocation,
                onCheckedChange = { isChecked ->
                    addLocation = isChecked
                    if (isChecked) {
                        getLocation() // Avvia logica GPS
                    } else {
                        Log.d(tag, "Posizione rimossa dall'utente.")
                        currentLat = null
                        currentLng = null
                    }
                }
            )
            Icon(Icons.Default.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text("Aggiungi posizione")
                if (currentLat != null) {
                    Text(
                        text = "Lat: ${String.format("%.4f", currentLat)}, Lng: ${String.format("%.4f", currentLng)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // BOTTONE PUBBLICA
        Button(
            onClick = {
                scope.launch {
                    if (selectedImageUri != null) {
                        isLoading = true
                        Log.d(tag, "Inizio procedura pubblicazione...")

                        try {
                            // 1. Converto URI -> ByteArray
                            val inputStream = context.contentResolver.openInputStream(selectedImageUri!!)
                            val bytes = inputStream?.readBytes()

                            if (bytes != null) {
                                Log.d(tag, "Immagine letta correttamente (${bytes.size} bytes).")
                                val sid = sessionManager.getSid()

                                if (sid != null) {
                                    // 2. Chiamata API
                                    Log.d(tag, "Invio dati al server... (Lat: $currentLat, Lng: $currentLng)")
                                    val success = controller.createPost(
                                        sid,
                                        bytes,
                                        description,
                                        if (addLocation) currentLat else null,
                                        if (addLocation) currentLng else null
                                    )

                                    if (success) {
                                        Log.d(tag, "PUBBLICAZIONE RIUSCITA!")
                                        Toast.makeText(context, "Post Pubblicato! 🎉", Toast.LENGTH_SHORT).show()
                                        navController.popBackStack() // Torna alla home
                                    } else {
                                        Log.e(tag, "Errore API durante la creazione del post.")
                                        Toast.makeText(context, "Errore pubblicazione", Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    Log.e(tag, "SessionID mancante.")
                                }
                            } else {
                                Log.e(tag, "Impossibile leggere i bytes dell'immagine.")
                            }
                        } catch (e: Exception) {
                            Log.e(tag, "Eccezione durante upload: ${e.message}")
                            e.printStackTrace()
                        } finally {
                            isLoading = false
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            enabled = !isLoading && selectedImageUri != null
        ) {
            if (isLoading) CircularProgressIndicator(color = Color.White)
            else Text("PUBBLICA")
        }
    }
}