package com.example.fotogram.utils

import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import com.example.fotogram.data.CommunicationController
import com.example.fotogram.data.SessionManager
import kotlinx.coroutines.launch

@Composable
fun TestingPage(navController: NavController) { // Lascia il NavController, può servire!
    val tag = "debugger_TestPage"
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Strumenti pronti all'uso
    val controller = remember { CommunicationController() }
    val sessionManager = remember { SessionManager(context) }

    // Variabili di stato rapide per debug
    var resultText by remember { mutableStateOf("Premi il tasto per testare...") }
    var isLoading by remember { mutableStateOf(false) }

    Scaffold { padding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {

                Text(text = "PAGINA DI TEST")

                if (isLoading) {
                    CircularProgressIndicator()
                } else {
                    Text(text = resultText)

                    Button(onClick = {
                        // QUI SCRIVI IL CODICE DI PROVA AL VOLO
                        scope.launch {
                            isLoading = true
                            val sid = sessionManager.getSid()
                            if (sid != null) {
                                // Esempio: Testo una chiamata API
                                // val res = controller.getQualcosa(sid)
                                // resultText = "Risultato: $res"
                                Log.d(tag, "Test eseguito con SID: $sid")
                            } else {
                                resultText = "Non loggato!"
                            }
                            isLoading = false
                        }
                    }) {
                        Text("TESTA ORA")
                    }
                }
            }
        }
    }
}