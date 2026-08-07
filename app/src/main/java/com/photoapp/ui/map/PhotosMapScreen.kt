package com.photoapp.ui.map

import android.content.Context
import android.graphics.Bitmap
import android.location.Geocoder
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.photoapp.data.local.entities.PhotoEntity
import com.photoapp.data.repository.PhotoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import javax.inject.Inject

data class MapMarker(
    val latitude: Double,
    val longitude: Double,
    val photos: List<PhotoEntity>
)

@HiltViewModel
class PhotosMapViewModel @Inject constructor(
    private val repository: PhotoRepository
) : ViewModel() {
    val photos: StateFlow<List<PhotoEntity>> = repository.getAllPhotos()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotosMapScreen(
    onBack: () -> Unit,
    onPhotoClick: (Long) -> Unit,
    viewModel: PhotosMapViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val photos by viewModel.photos.collectAsState()

    // Filter photos that have valid coordinates
    val geotaggedPhotos = remember(photos) {
        photos.filter { it.latitude != null && it.longitude != null && (it.latitude != 0.0 || it.longitude != 0.0) }
    }

    // Cluster photos dynamically within a 0.1 degree grid
    val markers = remember(geotaggedPhotos) {
        geotaggedPhotos
            .groupBy { photo ->
                val roundLat = Math.round(photo.latitude!! * 10.0) / 10.0
                val roundLng = Math.round(photo.longitude!! * 10.0) / 10.0
                Pair(roundLat, roundLng)
            }
            .map { (_, photosList) ->
                MapMarker(
                    latitude = photosList.first().latitude!!,
                    longitude = photosList.first().longitude!!,
                    photos = photosList
                )
            }
    }

    var selectedMarker by remember { mutableStateOf<MapMarker?>(null) }
    var locationName by remember { mutableStateOf("") }

    LaunchedEffect(selectedMarker) {
        val marker = selectedMarker
        if (marker != null) {
            locationName = "Fetching location..."
            withContext(Dispatchers.IO) {
                try {
                    val geocoder = Geocoder(context, Locale.getDefault())
                    val addresses = geocoder.getFromLocation(marker.latitude, marker.longitude, 1)
                    if (!addresses.isNullOrEmpty()) {
                        val address = addresses[0]
                        val city = address.locality ?: address.subAdminArea ?: ""
                        val state = address.adminArea ?: ""
                        val components = listOfNotNull(city.takeIf { it.isNotEmpty() }, state.takeIf { it.isNotEmpty() })
                        locationName = if (components.isNotEmpty()) components.joinToString(", ") else "Geotagged Location"
                    } else {
                        locationName = String.format(Locale.US, "%.4f, %.4f", marker.latitude, marker.longitude)
                    }
                } catch (e: Exception) {
                    locationName = String.format(Locale.US, "%.4f, %.4f", marker.latitude, marker.longitude)
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Photos Map",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${geotaggedPhotos.size} Geotagged Photos",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (geotaggedPhotos.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Map,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No Geotagged Photos",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Take photos with GPS/location enabled on your phone to see them plotted here on the India map.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            } else {

                // Render real interactive zoomable Map
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    AndroidView(
                        factory = { ctx ->
                            WebView(ctx).apply {
                                layoutParams = android.view.ViewGroup.LayoutParams(
                                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                                    android.view.ViewGroup.LayoutParams.MATCH_PARENT
                                )
                                webViewClient = WebViewClient()
                                settings.javaScriptEnabled = true
                                settings.domStorageEnabled = true
                                
                                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                                    settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                                }
                                
                                val defaultUserAgent = settings.userAgentString
                                if (defaultUserAgent != null) {
                                    settings.userAgentString = defaultUserAgent
                                        .replace("; wv", "")
                                        .replace("Version/4.0 ", "")
                                }
                                
                                // JavaScript bridge interface for pin clicks
                                addJavascriptInterface(object {
                                    @android.webkit.JavascriptInterface
                                    fun onMarkerClick(index: Int) {
                                        android.os.Handler(android.os.Looper.getMainLooper()).post {
                                            selectedMarker = markers.getOrNull(index)
                                        }
                                    }
                                }, "AndroidBridge")
                                
                                setBackgroundColor(0)
                            }
                        },
                        update = { webView ->
                            // Only load html if the list of markers changes
                            if (webView.tag != markers) {
                                webView.tag = markers
                                
                                // Serialize markers list to JSON array
                                val markersJson = markers.mapIndexed { index, marker ->
                                    """{"lat": ${marker.latitude}, "lng": ${marker.longitude}, "count": ${marker.photos.size}}"""
                                }.joinToString(separator = ",", prefix = "[", postfix = "]")

                                val htmlContent = """
                                    <!DOCTYPE html>
                                    <html>
                                    <head>
                                        <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
                                        <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" />
                                        <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
                                        <style>
                                            html, body, #map {
                                                width: 100%;
                                                height: 100%;
                                                margin: 0;
                                                padding: 0;
                                                background-color: #121212;
                                            }
                                            .leaflet-container {
                                                background-color: #121212 !important;
                                            }
                                            .leaflet-tile-pane {
                                                filter: invert(90%) hue-rotate(180deg) brightness(95%) contrast(90%);
                                            }
                                            .dark-tooltip {
                                                background-color: #1e1e1e !important;
                                                color: #ffffff !important;
                                                border: 1px solid #444 !important;
                                                border-radius: 4px !important;
                                                font-weight: bold !important;
                                                font-size: 11px !important;
                                                padding: 2px 6px !important;
                                                box-shadow: 0 1px 3px rgba(0,0,0,0.4) !important;
                                            }
                                            .dark-tooltip:before {
                                                border-top-color: #1e1e1e !important;
                                            }
                                        </style>
                                    </head>
                                    <body>
                                        <div id="map"></div>
                                        <script>
                                            // Initialize centered on India
                                            var map = L.map('map', {
                                                zoomControl: false,
                                                attributionControl: false
                                            }).setView([20.5937, 78.9629], 5);

                                            L.control.zoom({
                                                position: 'topright'
                                            }).addTo(map);

                                            L.tileLayer('https://mt1.google.com/vt/lyrs=m&x={x}&y={y}&z={z}', {
                                                maxZoom: 20
                                            }).addTo(map);

                                            function markerClicked(index) {
                                                if (window.AndroidBridge) {
                                                    window.AndroidBridge.onMarkerClick(index);
                                                }
                                            }

                                            var markerData = $markersJson;
                                            
                                            // Keep track of marker objects to fit bounds
                                            var leafletMarkers = [];
                                            
                                            markerData.forEach(function(marker, index) {
                                                var m = L.marker([marker.lat, marker.lng]).addTo(map);
                                                leafletMarkers.push(m);
                                                m.on('click', function() {
                                                    markerClicked(index);
                                                });
                                                if (marker.count > 1) {
                                                    m.bindTooltip(marker.count + " photos", { 
                                                        permanent: true, 
                                                        direction: 'top', 
                                                        className: 'dark-tooltip' 
                                                    });
                                                }
                                            });
                                            
                                            // Fit map bounds to show all markers if there are markers
                                            if (leafletMarkers.length > 0) {
                                                var group = new L.featureGroup(leafletMarkers);
                                                map.fitBounds(group.getBounds().pad(0.15));
                                            }
                                        </script>
                                    </body>
                                    </html>
                                """.trimIndent()
                                webView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            // Bottom popup showing thumbnails for selected marker
            selectedMarker?.let { marker ->
                Card(
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 16.dp),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant,
                            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                        )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = locationName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${marker.photos.size} photo${if (marker.photos.size > 1) "s" else ""}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(
                                onClick = { selectedMarker = null },
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 2.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(marker.photos) { photo ->
                                Box(
                                    modifier = Modifier
                                        .size(80.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                        .clickable { onPhotoClick(photo.id) }
                                ) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(LocalContext.current)
                                            .data(photo.contentUri)
                                            .bitmapConfig(Bitmap.Config.RGB_565)
                                            .crossfade(false)
                                            .size(200)
                                            .build(),
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}
