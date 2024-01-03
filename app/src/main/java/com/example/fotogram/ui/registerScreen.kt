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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.fotogram.data.CommunicationController
import com.example.fotogram.data.SessionManager
import kotlinx.coroutines.launch
import java.util.UUID

@Composable
fun RegisterScreen(navController: NavController) {
    val tag = "debugger_REGISTER"
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val controller = remember { CommunicationController() }
    val sessionManager = remember { SessionManager(context) }

    // STATO UI
    var username by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }

    var isLoading by remember { mutableStateOf(false) }

    // Launcher Foto
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedImageUri = uri
        Log.d(tag, "📸 Foto selezionata: $uri")
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Benvenuto su Fotogram", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
        Text("Crea il tuo profilo", style = MaterialTheme.typography.bodyLarge, color = Color.Gray)

        Spacer(modifier = Modifier.height(32.dp))

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

        Spacer(modifier = Modifier.height(24.dp))

        // 1. NOME UTENTE (Max 15 char)
        OutlinedTextField(
            value = username,
            onValueChange = { if (it.length <= 15) username = it }, // Blocco input > 15
            label = { Text("Nome Utente (max 15)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            supportingText = { Text("${username.length}/15") }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 2. BIO (Max 100 char)
        OutlinedTextField(
            value = bio,
            onValueChange = { if (it.length <= 100) bio = it }, // Blocco input > 100
            label = { Text("Bio (max 100)") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2,
            maxLines = 4,
            supportingText = { Text("${bio.length}/100") }
        )

        Spacer(modifier = Modifier.height(32.dp))

        // BOTTONE REGISTRAZIONE IMPLICITA
        Button(
            onClick = {
                // --- CONTROLLI PRELIMINARI (VALIDAZIONE) ---
                if (username.isBlank()) {
                    Toast.makeText(context, "Inserisci il nome", Toast.LENGTH_SHORT).show()
                    return@Button
                }

                // La specifica dice "fornire un nome E un'immagine", quindi l'immagine è obbligatoria.
                if (selectedImageUri == null) {
                    Toast.makeText(context, "Seleziona una foto profilo", Toast.LENGTH_SHORT).show()
                    return@Button
                }

                // Doppia sicurezza sui limiti (anche se l'UI li blocca già)
                if (username.length > 15) {
                    Toast.makeText(context, "Nome troppo lungo!", Toast.LENGTH_SHORT).show()
                    return@Button
                }
                if (bio.length > 100) {
                    Toast.makeText(context, "Bio troppo lunga!", Toast.LENGTH_SHORT).show()
                    return@Button
                }

                scope.launch {
                    isLoading = true

                    // Generazione credenziali implicite
                    val cleanName = username.trim()
                    val uniqueId = UUID.randomUUID().toString().substring(0, 8)
                    val fakeEmail = "${cleanName.replace(" ", "")}_$uniqueId@fotogram.app"
                    val fakePass = UUID.randomUUID().toString()

                    Log.d(tag, "🤖 Registrazione Implicita: User=$cleanName")

                    // 1. CREAZIONE UTENTE
                    val loginData = controller.register(cleanName, fakeEmail, fakePass, bio)

                    if (loginData != null) {
                        Log.d(tag, "✅ Utente creato. SID: ${loginData.sessionId}")

                        // 2. UPLOAD FOTO (Ora siamo sicuri che c'è!)
                        try {
                            val inputStream = context.contentResolver.openInputStream(selectedImageUri!!)
                            val bytes = inputStream?.readBytes()
                            if (bytes != null) {
                                val fotoOk = controller.updateProfilePicture(loginData.sessionId, bytes)
                                if (fotoOk) Log.d(tag, "Foto caricata correttamente.")
                                else Log.e(tag, "⚠Upload foto fallito lato server")
                            }
                        } catch (e: Exception) {
                            Log.e(tag, "Errore lettura file foto: ${e.message}")
                        }

                        // 3. SALVA E VAI
                        sessionManager.saveSession(loginData.sessionId, loginData.userId)
                        Toast.makeText(context, "Benvenuto $cleanName!", Toast.LENGTH_SHORT).show()

                        navController.navigate("home") {
                            popUpTo("register") { inclusive = true }
                        }
                    } else {
                        Log.e(tag, "Errore Server in fase di registrazione")
                        Toast.makeText(context, "Errore di connessione", Toast.LENGTH_SHORT).show()
                    }
                    isLoading = false
                }
            },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            enabled = !isLoading
        ) {
            if (isLoading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
            else Text("INIZIA A USARE FOTOGRAM")
        }
    }
}