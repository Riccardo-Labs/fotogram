package com.example.fotogram

import android.os.Bundle
import android.util.Base64
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import com.example.fotogram.data.CommunicationController
import com.example.fotogram.data.SessionManager
import com.example.fotogram.ui.EditProfileScreen
import com.example.fotogram.ui.FullScreenImageScreen
import com.example.fotogram.ui.HomeScreen
import com.example.fotogram.ui.MapScreen
import com.example.fotogram.ui.NewPostScreen
import com.example.fotogram.ui.OtherUserProfileScreen
import com.example.fotogram.ui.ProfileScreen
import com.example.fotogram.ui.RegisterScreen
import java.lang.Integer.parseInt

class MainActivity : ComponentActivity() {

    val tag = "debugger_Main-Activity"

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val context = LocalContext.current
            val navController = rememberNavController()

            // Inizializzo i manager
            val sessionManager = remember { SessionManager(context) }
            val controller = remember { CommunicationController() }

            // Stati per la UI
            var startDestination by remember { mutableStateOf<String?>(null) }
            var myProfilePicBase64 by remember { mutableStateOf<String?>(null) }

            // ------------------------------------------------------------------
            // 1. LOGICA DI AVVIO (Eseguita una volta sola all'apertura app)
            // ------------------------------------------------------------------
            LaunchedEffect(Unit) {
                Log.d(tag, "App avviata. Controllo sessione...")
                val sid = sessionManager.getSid()

                if (sid != null) {
                    Log.d(tag, "Sessione trovata ($sid). Vado alla Home.")
                    startDestination = "home"
                } else {
                    Log.d(tag, "Nessuna sessione. Vado alla Registrazione.")
                    startDestination = "register"
                }
            }

            // 2. LOGICA FOTO PROFILO REATTIVA (Self-Healing)
            // Osserviamo la navigazione: ogni volta che cambi pagina, questo blocco riparte.
            val navBackStackEntry by navController.currentBackStackEntryAsState()

            LaunchedEffect(navBackStackEntry) {
                // A. Provo a leggere dal DataStore locale (veloce)
                var pic = sessionManager.getProfilePic()

                // B. SELF-HEALING: Se non c'è in locale, ma sono loggato, la scarico dal server
                if (pic == null) {
                    val sid = sessionManager.getSid()
                    val uid = sessionManager.getUid()

                    // Se ho una sessione valida e un ID utente
                    if (sid != null && uid != -1) {
                        Log.d(tag, "Foto mancante in cache locale. Provo a scaricarla dal server...")
                        val user = controller.getUser(sid, uid)
                        if (user != null && user.profilePicture != null) {
                            pic = user.profilePicture
                            Log.d(tag, "Foto scaricata! La salvo in cache per la prossima volta.")
                            sessionManager.saveProfilePic(pic)
                        } else {
                            Log.d(tag, "L'utente non ha foto nemmeno sul server o errore download.")
                        }
                    }
                }

                // C. Aggiorno la UI se l'immagine è cambiata
                if (pic != myProfilePicBase64) {
                    Log.d(tag, "Aggiorno icona nella TopBar.")
                    myProfilePicBase64 = pic
                }
            }

            // 3. INTERFACCIA UTENTE

            if (startDestination == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                val currentRoute = navBackStackEntry?.destination?.route
                // Nascondiamo le barre se siamo in fase di registrazione
                val showBars = currentRoute != "register"

                Scaffold(
                    topBar = {
                        if (showBars) {
                            TopAppBar(
                                title = {
                                    val titleText = when (currentRoute) {
                                        "home" -> "Fotogram"
                                        "newpost" -> "Crea Post"
                                        "map" -> "Mappa"
                                        "profile" -> "Il Mio Profilo"
                                        "edit_profile" -> "Modifica Profilo"
                                        else -> if (currentRoute?.startsWith("user/") == true) "Utente" else "Fotogram"
                                    }
                                    Text(
                                        text = titleText,
                                        fontFamily = FontFamily.Cursive,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 30.sp
                                    )
                                },
                                colors = TopAppBarDefaults.topAppBarColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    titleContentColor = Color.White,
                                    actionIconContentColor = Color.White

                                ),
                                actions = {
                                    // Icona profilo cliccabile (porta al profilo)
                                    if (currentRoute != "profile") {
                                        IconButton(onClick = { navController.navigate("profile") }) {
                                            if (myProfilePicBase64 != null) {
                                                // Decodifica Base64 -> Bitmap
                                                val bitmap = remember(myProfilePicBase64) {
                                                    try {
                                                        val bytes = Base64.decode(myProfilePicBase64, Base64.DEFAULT)
                                                        bytes
                                                    } catch (e: Exception) {
                                                        Log.e(tag, "Errore decodifica Base64: ${e.message}")
                                                        null
                                                    }
                                                }
                                                AsyncImage(
                                                    model = bitmap,
                                                    contentDescription = "Profilo",
                                                    modifier = Modifier.size(32.dp).clip(CircleShape),
                                                    contentScale = ContentScale.Crop
                                                )
                                            } else {
                                                // Icona Default se non c'è foto
                                                Icon(Icons.Default.Person, contentDescription = "Profilo Default")
                                            }
                                        }
                                    }
                                }
                            )
                        }
                    },
                    bottomBar = {
                        if (showBars) {
                            NavigationBar {
                                NavigationBarItem(
                                    icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                                    label = { Text("Home") },
                                    selected = currentRoute == "home",
                                    onClick = {
                                        if (currentRoute != "home") {
                                            navController.navigate("home") {
                                                popUpTo("home") { inclusive = true }
                                            }
                                        }
                                    }
                                )
                                NavigationBarItem(
                                    icon = { Icon(Icons.Default.AddCircle, contentDescription = "Nuovo") },
                                    label = { Text("Nuovo") },
                                    selected = currentRoute == "newpost",
                                    onClick = { if (currentRoute != "newpost") navController.navigate("newpost") }
                                )
                                NavigationBarItem(
                                    icon = { Icon(Icons.Default.LocationOn, contentDescription = "Mappa") },
                                    label = { Text("Mappa") },
                                    selected = currentRoute == "map",
                                    onClick = { if (currentRoute != "map") navController.navigate("map") }
                                )
                                /*
                                NavigationBarItem(
                                    icon = { Icon(Icons.Default.Build, contentDescription = "TestingPage") },
                                    label = { Text("TestingPage") },
                                    selected = currentRoute == "TestingPage",
                                    onClick = { if (currentRoute != "TestingPage") navController.navigate("TestingPage") }
                                )

                                */
                            }
                        }
                    }
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = startDestination!!,
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable("register") { RegisterScreen(navController) }
                        composable("home") { HomeScreen(navController) }
                        composable("newpost") { NewPostScreen(navController) }
                        composable("profile") { ProfileScreen(navController) }
                        composable("edit_profile") { EditProfileScreen(navController) }
                        composable("fullscreen_image/{postId}") { backStackEntry ->
                            // Prendiamo l'ID come stringa e lo convertiamo in Int
                            val postIdString = backStackEntry.arguments?.getString("postId")
                            val postId = postIdString?.toIntOrNull()

                            if (postId != null) {
                                FullScreenImageScreen(navController, postId)
                            }
                        }
                        //composable("TestingPage") { TestingPage(navController) }
                        composable("map") { MapScreen() } // Aggiungere parametri se servono


                        composable("user/{userId}") { backStackEntry ->
                            val userId = backStackEntry.arguments?.getString("userId")?.toIntOrNull()
                            if (userId != null) {
                                OtherUserProfileScreen(navController, userId)
                            }
                        }
                    }
                }
            }
        }
    }
}
