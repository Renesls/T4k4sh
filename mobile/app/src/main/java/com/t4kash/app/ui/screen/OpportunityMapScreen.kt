package com.t4kash.app.ui.screen

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.t4kash.app.ui.components.T4TopBar
import com.t4kash.app.ui.theme.T4BrandDark
import com.t4kash.app.ui.theme.T4Mint
import com.t4kash.app.ui.theme.T4Primary
import com.t4kash.app.ui.theme.T4Surface
import com.t4kash.app.ui.theme.T4Text
import com.t4kash.app.ui.theme.T4TextMuted
import kotlinx.coroutines.launch
import org.maplibre.compose.camera.CameraPosition
import org.maplibre.compose.camera.rememberCameraState
import org.maplibre.compose.location.LocationPuck
import org.maplibre.compose.location.LocationPuckColors
import org.maplibre.compose.location.LocationTrackingEffect
import org.maplibre.compose.location.rememberDefaultLocationProvider
import org.maplibre.compose.location.rememberNullLocationProvider
import org.maplibre.compose.location.rememberUserLocationState
import org.maplibre.compose.map.MaplibreMap
import org.maplibre.compose.style.BaseStyle
import org.maplibre.spatialk.geojson.Position

private const val OPEN_FREE_MAP_STYLE =
    "https://tiles.openfreemap.org/styles/liberty"

@Composable
fun OpportunityMapScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var reloadKey by rememberSaveable { mutableIntStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var hasCenteredOnUser by rememberSaveable { mutableStateOf(false) }
    var hasLocationPermission by remember {
        mutableStateOf(context.hasLocationPermission())
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasLocationPermission =
            permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
    }

    LaunchedEffect(Unit) {
        if (!hasLocationPermission) {
            locationPermissionLauncher.launch(LOCATION_PERMISSIONS)
        }
    }

    val cameraState = rememberCameraState(
        firstPosition = CameraPosition(
            target = Position(
                latitude = 12.11499,
                longitude = -86.23617
            ),
            zoom = 11.5
        )
    )
    val locationProvider = if (hasLocationPermission) {
        rememberDefaultLocationProvider()
    } else {
        rememberNullLocationProvider()
    }
    val locationState = rememberUserLocationState(locationProvider)

    Scaffold(
        topBar = {
            T4TopBar(
                title = "Mapa",
                subtitle = "Explora oportunidades por ubicación",
                onBack = onBack
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            key(reloadKey) {
                MaplibreMap(
                    modifier = Modifier.fillMaxSize(),
                    baseStyle = BaseStyle.Uri(OPEN_FREE_MAP_STYLE),
                    cameraState = cameraState,
                    onMapLoadFinished = {
                        isLoading = false
                        errorMessage = null
                    },
                    onMapLoadFailed = { reason ->
                        isLoading = false
                        errorMessage = reason?.takeIf { it.isNotBlank() }
                            ?: "No se pudo cargar el mapa."
                    }
                ) {
                    if (hasLocationPermission) {
                        LocationPuck(
                            idPrefix = "t4kash-user",
                            location = locationState.location,
                            cameraState = cameraState,
                            colors = LocationPuckColors(
                                dotFillColorCurrentLocation = T4Primary,
                                dotFillColorOldLocation = T4TextMuted,
                                dotStrokeColor = Color.White,
                                shadowColor = T4BrandDark,
                                accuracyStrokeColor = T4Primary,
                                accuracyFillColor = T4Primary.copy(alpha = 0.18f),
                                bearingColor = T4Mint
                            )
                        )
                        LocationTrackingEffect(
                            locationState = locationState,
                            trackBearing = false
                        ) {
                            val userPosition = currentLocation.location?.position?.value
                            if (userPosition != null && !hasCenteredOnUser) {
                                cameraState.animateTo(
                                    CameraPosition(
                                        target = userPosition,
                                        zoom = 15.0
                                    )
                                )
                                hasCenteredOnUser = true
                            }
                        }
                    }
                }
            }

            SmallFloatingActionButton(
                onClick = {
                    if (!hasLocationPermission) {
                        locationPermissionLauncher.launch(LOCATION_PERMISSIONS)
                    } else {
                        locationState.location?.position?.value?.let { userPosition ->
                            coroutineScope.launch {
                                cameraState.animateTo(
                                    CameraPosition(
                                        target = userPosition,
                                        zoom = 15.0
                                    )
                                )
                            }
                        }
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(18.dp),
                containerColor = T4Mint,
                contentColor = T4BrandDark
            ) {
                Icon(
                    imageVector = Icons.Filled.MyLocation,
                    contentDescription = "Centrar en mi ubicación"
                )
            }

            if (isLoading) {
                Card(
                    modifier = Modifier.align(Alignment.Center),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = T4Surface)
                ) {
                    Column(
                        modifier = Modifier.padding(22.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator(color = T4Primary)
                        Text(
                            text = "Cargando mapa...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = T4TextMuted
                        )
                    }
                }
            }

            errorMessage?.let { message ->
                Card(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp)
                        .widthIn(max = 340.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = T4Surface)
                ) {
                    Column(
                        modifier = Modifier.padding(22.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Map,
                            contentDescription = null,
                            tint = T4Primary
                        )
                        Text(
                            text = "Mapa no disponible",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = T4Text
                        )
                        Text(
                            text = message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = T4TextMuted
                        )
                        Button(
                            onClick = {
                                isLoading = true
                                errorMessage = null
                                reloadKey += 1
                            }
                        ) {
                            Text("Reintentar")
                        }
                    }
                }
            }
        }
    }
}

private val LOCATION_PERMISSIONS = arrayOf(
    Manifest.permission.ACCESS_FINE_LOCATION,
    Manifest.permission.ACCESS_COARSE_LOCATION
)

private fun Context.hasLocationPermission(): Boolean {
    return ContextCompat.checkSelfPermission(
        this,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
}
