package com.t4kash.app.ui.screen

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.t4kash.app.ui.GeoPoint
import com.t4kash.app.ui.components.T4TopBar
import com.t4kash.app.ui.theme.T4BrandDark
import com.t4kash.app.ui.theme.T4Mint
import com.t4kash.app.ui.theme.T4Primary
import com.t4kash.app.ui.theme.T4Surface
import com.t4kash.app.ui.theme.T4Text
import com.t4kash.app.ui.theme.T4TextMuted
import java.util.Locale
import kotlinx.coroutines.launch
import org.maplibre.compose.camera.CameraPosition
import org.maplibre.compose.camera.rememberCameraState
import org.maplibre.compose.location.LocationPuck
import org.maplibre.compose.location.LocationPuckColors
import org.maplibre.compose.location.rememberDefaultLocationProvider
import org.maplibre.compose.location.rememberNullLocationProvider
import org.maplibre.compose.location.rememberUserLocationState
import org.maplibre.compose.map.MaplibreMap
import org.maplibre.compose.style.BaseStyle
import org.maplibre.compose.util.ClickResult
import org.maplibre.spatialk.geojson.Position

@SuppressLint("MissingPermission")
@Composable
fun TaskLocationPickerDialog(
    initialLatitude: Double?,
    initialLongitude: Double?,
    onDismiss: () -> Unit,
    onLocationSelected: (Double, Double) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val initialPosition = remember(initialLatitude, initialLongitude) {
        initialLatitude
            ?.let { latitude ->
                initialLongitude?.let { longitude ->
                    GeoPoint(latitude, longitude)
                        .takeIf(GeoPoint::isValid)
                        ?.let {
                            Position(
                                latitude = it.latitude,
                                longitude = it.longitude
                            )
                        }
                }
            }
    }
    val cameraState = rememberCameraState(
        firstPosition = CameraPosition(
            target = initialPosition ?: DEFAULT_MAP_POSITION,
            zoom = if (initialPosition == null) 12.0 else 16.0
        )
    )
    var selectedPosition by remember(initialPosition) {
        mutableStateOf(initialPosition)
    }
    var isMapLoading by remember { mutableStateOf(true) }
    var mapErrorMessage by remember { mutableStateOf<String?>(null) }
    var reloadKey by rememberSaveable { mutableIntStateOf(0) }
    var hasCenteredOnUser by rememberSaveable {
        mutableStateOf(initialPosition != null)
    }
    var hasLocationPermission by remember {
        mutableStateOf(context.hasPickerLocationPermission())
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasLocationPermission =
            permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
    }

    LaunchedEffect(Unit) {
        if (!hasLocationPermission) {
            permissionLauncher.launch(PICKER_LOCATION_PERMISSIONS)
        }
    }

    val locationProvider = if (hasLocationPermission) {
        rememberDefaultLocationProvider()
    } else {
        rememberNullLocationProvider()
    }
    val locationState = rememberUserLocationState(locationProvider)
    val userPosition = locationState.location
        ?.position
        ?.value
        ?.takeIf {
            GeoPoint(it.latitude, it.longitude).isValid()
        }

    LaunchedEffect(userPosition, hasCenteredOnUser) {
        if (userPosition != null && !hasCenteredOnUser) {
            selectedPosition = userPosition
            cameraState.animateTo(
                CameraPosition(target = userPosition, zoom = 16.0)
            )
            hasCenteredOnUser = true
        }
    }

    LaunchedEffect(cameraState.isCameraMoving) {
        if (!cameraState.isCameraMoving && !isMapLoading) {
            val target = cameraState.position.target
            if (GeoPoint(target.latitude, target.longitude).isValid()) {
                selectedPosition = target
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Scaffold(
            topBar = {
                T4TopBar(
                    title = "Elegir ubicación",
                    subtitle = "Punto de la oportunidad",
                    onBack = onDismiss
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
                        baseStyle = BaseStyle.Uri(OPEN_FREE_MAP_STYLE_URI),
                        cameraState = cameraState,
                        onMapClick = { position, _ ->
                            selectedPosition = position
                            coroutineScope.launch {
                                cameraState.animateTo(
                                    cameraState.position.copy(target = position)
                                )
                            }
                            ClickResult.Consume
                        },
                        onMapLoadFinished = {
                            isMapLoading = false
                            mapErrorMessage = null
                        },
                        onMapLoadFailed = { reason ->
                            isMapLoading = false
                            mapErrorMessage = reason?.takeIf(String::isNotBlank)
                                ?: "No se pudo cargar el mapa."
                        }
                    ) {
                        if (hasLocationPermission) {
                            LocationPuck(
                                idPrefix = "t4kash-picker-user",
                                location = locationState.location,
                                cameraState = cameraState,
                                colors = LocationPuckColors(
                                    dotFillColorCurrentLocation = T4Mint,
                                    dotFillColorOldLocation = T4TextMuted,
                                    dotStrokeColor = T4BrandDark,
                                    shadowColor = T4BrandDark,
                                    accuracyStrokeColor = T4Mint,
                                    accuracyFillColor = T4Mint.copy(alpha = 0.18f),
                                    bearingColor = T4Primary
                                )
                            )
                        }
                    }
                }

                Icon(
                    imageVector = Icons.Filled.LocationOn,
                    contentDescription = "Punto seleccionado",
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(bottom = 38.dp)
                        .size(48.dp),
                    tint = if (selectedPosition == null) {
                        T4TextMuted
                    } else {
                        T4Primary
                    }
                )

                SmallFloatingActionButton(
                    onClick = {
                        if (!hasLocationPermission) {
                            permissionLauncher.launch(PICKER_LOCATION_PERMISSIONS)
                        } else {
                            userPosition?.let { position ->
                                selectedPosition = position
                                coroutineScope.launch {
                                    cameraState.animateTo(
                                        CameraPosition(
                                            target = position,
                                            zoom = 16.0
                                        )
                                    )
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 18.dp, bottom = 150.dp),
                    containerColor = T4Mint,
                    contentColor = T4BrandDark
                ) {
                    Icon(
                        imageVector = Icons.Filled.MyLocation,
                        contentDescription = "Usar mi ubicación"
                    )
                }

                Card(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(14.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = T4Surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.LocationOn,
                                contentDescription = null,
                                tint = T4Primary
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (selectedPosition == null) {
                                        "Ubicación pendiente"
                                    } else {
                                        "Ubicación seleccionada"
                                    },
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = T4Text
                                )
                                Text(
                                    text = selectedPosition?.let {
                                        String.format(
                                            Locale.US,
                                            "%.6f, %.6f",
                                            it.latitude,
                                            it.longitude
                                        )
                                    } ?: "Ubicación pendiente",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = T4TextMuted
                                )
                            }
                        }
                        Button(
                            onClick = {
                                selectedPosition?.let {
                                    onLocationSelected(
                                        it.latitude,
                                        it.longitude
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = selectedPosition != null
                        ) {
                            Text("Confirmar ubicación")
                        }
                    }
                }

                if (isMapLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = T4Primary
                    )
                }

                mapErrorMessage?.let { message ->
                    Card(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(24.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = T4Surface
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Map,
                                contentDescription = null,
                                tint = T4Primary
                            )
                            Text(
                                text = message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = T4Text
                            )
                            Button(
                                onClick = {
                                    isMapLoading = true
                                    mapErrorMessage = null
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
}

private val PICKER_LOCATION_PERMISSIONS = arrayOf(
    Manifest.permission.ACCESS_FINE_LOCATION,
    Manifest.permission.ACCESS_COARSE_LOCATION
)

private fun Context.hasPickerLocationPermission(): Boolean {
    return ContextCompat.checkSelfPermission(
        this,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
}
