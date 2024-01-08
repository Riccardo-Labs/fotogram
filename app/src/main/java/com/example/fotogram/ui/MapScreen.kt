package com.example.fotogram.ui

import android.Manifest
import android.preference.PreferenceManager
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.fotogram.data.CommunicationController
import com.example.fotogram.data.SessionManager
import com.example.fotogram.model.PostDetail
import kotlinx.serialization.json.Json
import org.osmdroid.api.IMapController
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

@Composable
fun MapScreen() {
    val tag = "debugger_mappa"
    val context = LocalContext.current
    val sessionManager = remember { SessionManager(context) }
    val controller = remember { CommunicationController() }
    val jsonHelper = Json { ignoreUnknownKeys = true; coerceInputValues = true; isLenient = true }

    Configuration.getInstance().load(context, PreferenceManager.getDefaultSharedPreferences(context))
    Configuration.getInstance().userAgentValue = "FotogramApp/1.0"

    var listaPostGeolocalizzati by remember { mutableStateOf<List<PostDetail>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var hasZoomedToPosts by remember { mutableStateOf(false) }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { }

    // 1. SCARICAMENTO
    LaunchedEffect(Unit) {
        locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        val sid = sessionManager.getSid()

        Log.d(tag, ">>> INIZIO RICERCA POST CON GPS <<<")

        if (sid != null) {
            val wallString = controller.getFeed(sid, limit = 20)
            if (wallString != null) {
                try {
                    val ids = jsonHelper.decodeFromString<List<Int>>(wallString)
                    Log.d(tag, "Scaricati ${ids.size} ID post totali.")

                    val postsValidi = mutableListOf<PostDetail>()

                    for (id in ids) {
                        val pStr = controller.getPost(sid, id)
                        if (pStr != null) {
                            val p = jsonHelper.decodeFromString<PostDetail>(pStr)

                            val lat = p.location?.latitude ?: 0.0
                            val lon = p.location?.longitude ?: 0.0

                            if (lat != 0.0 || lon != 0.0) {
                                //Log.d(tag, "✅ Post $id HA GPS: $lat, $lon") // LOG DETTAGLIATO PER OGNI POST
                                val autore = controller.getUser(sid, p.authorId)
                                if (autore != null) p.authorName = autore.username
                                postsValidi.add(p)
                            } else {
                                //Log.d(tag, "❌ Post $id scartato (no gps)")
                            }
                        }
                    }
                    listaPostGeolocalizzati = postsValidi
                    Log.d(tag, ">>> FINE: Trovati ${postsValidi.size} post validi per la mappa <<<")

                } catch (e: Exception) { Log.e(tag, "ERRORE: ${e.message}") }
            }
        }
        isLoading = false
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                MapView(ctx).apply {
                    setTileSource(TileSourceFactory.MAPNIK)
                    setMultiTouchControls(true)
                    // Usa 'this.controller' per non confondersi
                    this.controller.setZoom(4.0)
                }
            },
            update = { mapView ->
                val mapController = mapView.controller as IMapController

                // Overlay Posizione Utente
                val locationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(context), mapView)
                locationOverlay.enableMyLocation()
                if (!mapView.overlays.contains(locationOverlay)) {
                    mapView.overlays.add(locationOverlay)
                }

                val markersToRemove = mapView.overlays.filterIsInstance<Marker>()
                mapView.overlays.removeAll(markersToRemove)

                // 2. DISEGNO MARKER E LOG VISIVO
                Log.d(tag, "--- INIZIO DISEGNO MAPPA (${listaPostGeolocalizzati.size} elementi) ---")

                val puntiDaInquadrare = mutableListOf<GeoPoint>()
                val myLoc = locationOverlay.myLocation
                if (myLoc != null) puntiDaInquadrare.add(myLoc)

                for (post in listaPostGeolocalizzati) {
                    // Prendiamo la location (sicuri che esista perché filtrata prima)
                    val loc = post.location!!

                    // Trasformiamo i Double? in Double usando '?: 0.0'
                    val lat = loc.latitude ?: 0.0
                    val lon = loc.longitude ?: 0.0

                    val point = GeoPoint(lat, lon)
                    puntiDaInquadrare.add(point)

                    val marker = Marker(mapView)
                    marker.position = point
                    marker.title = "@${post.authorName ?: "User"}"
                    marker.snippet = post.contentText ?: ""
                    marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)

                    mapView.overlays.add(marker)

                    Log.d(tag, "📍 DISEGNATO MARKER a $lat, $lon")
                }

                mapView.invalidate() // Forza ridisegno

                // Zoom Automatico
                if (puntiDaInquadrare.isNotEmpty() && !hasZoomedToPosts) {
                    Log.d(tag, "Provo a fare zoom automatico su ${puntiDaInquadrare.size} punti")
                    if (puntiDaInquadrare.size == 1) {
                        mapController.setCenter(puntiDaInquadrare[0])
                        mapController.setZoom(14.0)
                    } else {
                        try {
                            val box = BoundingBox.fromGeoPoints(puntiDaInquadrare)
                            mapView.zoomToBoundingBox(box, true, 100)
                        } catch (e: Exception) {
                            mapController.setZoom(10.0)
                        }
                    }
                    hasZoomedToPosts = true
                }
            }
        )

        FloatingActionButton(
            onClick = { hasZoomedToPosts = false },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
        ) {
            Icon(Icons.Default.CenterFocusStrong, contentDescription = "Zoom")
        }

        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }
    }
}
