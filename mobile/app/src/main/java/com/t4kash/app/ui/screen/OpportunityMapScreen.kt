package com.t4kash.app.ui.screen

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
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
import androidx.core.content.ContextCompat
import com.t4kash.app.ui.components.T4TopBar
import com.t4kash.app.ui.model.TaskDto
import com.t4kash.app.ui.theme.T4BrandDark
import com.t4kash.app.ui.theme.T4Mint
import com.t4kash.app.ui.theme.T4Primary
import com.t4kash.app.ui.theme.T4Surface
import com.t4kash.app.ui.theme.T4Text
import com.t4kash.app.ui.theme.T4TextMuted
import com.t4kash.app.ui.viewmodel.MarketplaceViewModel
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import org.maplibre.compose.camera.CameraPosition
import org.maplibre.compose.camera.rememberCameraState
import org.maplibre.compose.expressions.dsl.const
import org.maplibre.compose.layers.CircleLayer
import org.maplibre.compose.location.LocationPuck
import org.maplibre.compose.location.LocationPuckColors
import org.maplibre.compose.location.LocationTrackingEffect
import org.maplibre.compose.location.rememberDefaultLocationProvider
import org.maplibre.compose.location.rememberNullLocationProvider
import org.maplibre.compose.location.rememberUserLocationState
import org.maplibre.compose.map.MaplibreMap
import org.maplibre.compose.sources.GeoJsonData
import org.maplibre.compose.sources.rememberGeoJsonSource
import org.maplibre.compose.style.BaseStyle
import org.maplibre.spatialk.geojson.Position

private const val OPEN_FREE_MAP_STYLE =
    "https://tiles.openfreemap.org/styles/liberty"

@Composable
fun OpportunityMapScreen(
    viewModel: MarketplaceViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val uiState = viewModel.uiState
    val locatedTasks = remember(uiState.tasks) {
        uiState.tasks.filter {
            it.latitud != null &&
                it.longitud != null &&
                !it.modalidad.equals("REMOTA", ignoreCase = true)
        }
    }
    val taskGeoJson = remember(locatedTasks) {
        locatedTasks.toGeoJson()
    }
    var reloadKey by rememberSaveable { mutableIntStateOf(0) }
    var isMapLoading by remember { mutableStateOf(true) }
    var mapErrorMessage by remember { mutableStateOf<String?>(null) }
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
        viewModel.refresh()
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

    LaunchedEffect(locatedTasks, hasLocationPermission) {
        if (!hasLocationPermission && locatedTasks.isNotEmpty()) {
            val firstTask = locatedTasks.first()
            cameraState.animateTo(
                CameraPosition(
                    target = Position(
                        latitude = firstTask.latitud ?: return@LaunchedEffect,
                        longitude = firstTask.longitud ?: return@LaunchedEffect
                    ),
                    zoom = 14.0
                )
            )
        }
    }

    Scaffold(
        topBar = {
            T4TopBar(
                title = "Mapa",
                subtitle = "Oportunidades cerca de ti",
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
                        isMapLoading = false
                        mapErrorMessage = null
                    },
                    onMapLoadFailed = { reason ->
                        isMapLoading = false
                        mapErrorMessage = reason?.takeIf { it.isNotBlank() }
                            ?: "No se pudo cargar el mapa."
                    }
                ) {
                    val taskSource = rememberGeoJsonSource(
                        data = GeoJsonData.JsonString(taskGeoJson)
                    )
                    CircleLayer(
                        id = "t4kash-task-halo",
                        source = taskSource,
                        radius = const(15.dp),
                        color = const(T4Primary.copy(alpha = 0.18f)),
                        strokeColor = const(Color.Transparent)
                    )
                    CircleLayer(
                        id = "t4kash-tasks",
                        source = taskSource,
                        radius = const(9.dp),
                        color = const(T4Primary),
                        strokeColor = const(Color.White),
                        strokeWidth = const(3.dp)
                    )

                    if (hasLocationPermission) {
                        LocationPuck(
                            idPrefix = "t4kash-user",
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

            Card(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(14.dp)
                    .widthIn(max = 360.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = T4Surface.copy(alpha = 0.96f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.LocationOn,
                        contentDescription = null,
                        tint = T4Primary
                    )
                    Column {
                        Text(
                            text = when {
                                uiState.isLoading -> "Buscando oportunidades..."
                                locatedTasks.isEmpty() -> "Sin tareas ubicadas"
                                locatedTasks.size == 1 -> "1 oportunidad en el mapa"
                                else -> "${locatedTasks.size} oportunidades en el mapa"
                            },
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = T4Text
                        )
                        Text(
                            text = uiState.errorMessage
                                ?: "Los puntos morados representan tareas presenciales o híbridas.",
                            style = MaterialTheme.typography.bodySmall,
                            color = T4TextMuted
                        )
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

            if (isMapLoading) {
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

            mapErrorMessage?.let { message ->
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

private fun List<TaskDto>.toGeoJson(): String {
    val features = JSONArray()
    forEach { task ->
        val latitude = task.latitud ?: return@forEach
        val longitude = task.longitud ?: return@forEach
        val coordinates = JSONArray()
            .put(longitude)
            .put(latitude)
        val geometry = JSONObject()
            .put("type", "Point")
            .put("coordinates", coordinates)
        val properties = JSONObject()
            .put("idTarea", task.idTarea)
            .put("titulo", task.titulo)
        features.put(
            JSONObject()
                .put("type", "Feature")
                .put("geometry", geometry)
                .put("properties", properties)
        )
    }
    return JSONObject()
        .put("type", "FeatureCollection")
        .put("features", features)
        .toString()
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
