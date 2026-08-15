package com.t4kash.app.ui.screen

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.material3.Slider
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.t4kash.app.ui.GeoPoint
import com.t4kash.app.ui.components.T4TopBar
import com.t4kash.app.ui.distanceInKilometers
import com.t4kash.app.ui.formatDistance
import com.t4kash.app.ui.model.TaskDto
import com.t4kash.app.ui.theme.T4BrandDark
import com.t4kash.app.ui.theme.T4Mint
import com.t4kash.app.ui.theme.T4Primary
import com.t4kash.app.ui.theme.T4Surface
import com.t4kash.app.ui.theme.T4Text
import com.t4kash.app.ui.theme.T4TextMuted
import com.t4kash.app.ui.viewmodel.MarketplaceViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
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
import org.maplibre.compose.util.ClickResult
import org.maplibre.spatialk.geojson.Position
import kotlin.math.roundToInt

private const val DEFAULT_RADIUS_KM = 50f

@SuppressLint("MissingPermission")
@Composable
fun OpportunityMapScreen(
    viewModel: MarketplaceViewModel,
    onBack: () -> Unit,
    onTaskSelected: (Int) -> Unit,
    focusedTaskId: Int? = null,
    quickMode: Boolean = false,
    onQuickClaimed: (Int) -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val uiState = viewModel.uiState
    val locatedTasks = remember(uiState.tasks, uiState.quickTasks, quickMode) {
        val source = if (quickMode) {
            uiState.quickTasks.map { it.tarea }
        } else {
            uiState.tasks
        }
        source.filter {
            it.hasValidCoordinates() &&
                it.estadoTarea.equals("PUBLICADA", ignoreCase = true) &&
                !it.modalidad.equals("REMOTA", ignoreCase = true) &&
                (quickMode || !it.tipoOportunidad.equals("RAPIDA", ignoreCase = true))
        }
    }
    val focusedTask = remember(locatedTasks, focusedTaskId) {
        locatedTasks.firstOrNull { it.idTarea == focusedTaskId }
    }
    var reloadKey by rememberSaveable { mutableIntStateOf(0) }
    var isMapLoading by remember { mutableStateOf(true) }
    var mapErrorMessage by remember { mutableStateOf<String?>(null) }
    var hasCenteredOnUser by rememberSaveable { mutableStateOf(false) }
    var radiusKm by rememberSaveable(quickMode) {
        mutableFloatStateOf(if (quickMode) 1f else DEFAULT_RADIUS_KM)
    }
    var selectedTaskId by rememberSaveable(focusedTaskId) {
        mutableStateOf(focusedTaskId)
    }
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
        if (quickMode) {
            viewModel.clearQuickTaskFeedback()
        } else {
            viewModel.refresh()
        }
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
    val userPosition = locationState.location
        ?.position
        ?.value
        ?.takeIf { it.isUsableLocation() }
    val visibleTasks = remember(locatedTasks, userPosition, radiusKm, focusedTaskId) {
        if (userPosition == null) {
            locatedTasks
        } else {
            locatedTasks.filter { task ->
                task.idTarea == focusedTaskId ||
                    task.distanceTo(userPosition) <= radiusKm
            }
        }
    }
    val taskGeoJson = remember(visibleTasks) {
        visibleTasks.toGeoJson()
    }
    val selectedTask = remember(locatedTasks, selectedTaskId) {
        locatedTasks.firstOrNull { it.idTarea == selectedTaskId }
    }
    val selectedTaskGeoJson = remember(selectedTask) {
        selectedTask?.let { listOf(it).toGeoJson() }
    }
    val selectedTaskDistance = remember(selectedTask, userPosition) {
        val serverDistance = if (quickMode) {
            uiState.quickTasks.firstOrNull {
                it.tarea.idTarea == selectedTask?.idTarea
            }?.distanciaKm
        } else {
            null
        }
        val taskPoint = selectedTask?.geoPoint()
        val userPoint = userPosition?.toGeoPoint()
        if (serverDistance != null) {
            serverDistance
        } else if (taskPoint == null || userPoint == null) {
            null
        } else {
            distanceInKilometers(userPoint, taskPoint)
        }
    }

    LaunchedEffect(quickMode, userPosition, radiusKm) {
        if (!quickMode || userPosition == null) return@LaunchedEffect
        delay(350)
        viewModel.searchQuickTasks(
            latitude = userPosition.latitude,
            longitude = userPosition.longitude,
            radiusKm = radiusKm.toDouble()
        )
    }

    LaunchedEffect(uiState.claimedQuickJob?.idTrabajo) {
        uiState.claimedQuickJob?.idTrabajo?.let(onQuickClaimed)
    }

    val radarTransition = rememberInfiniteTransition(label = "radar")
    val radarProgress by radarTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800),
            repeatMode = RepeatMode.Restart
        ),
        label = "radar-pulse"
    )

    LaunchedEffect(focusedTask?.idTarea) {
        focusedTask?.let { task ->
            selectedTaskId = task.idTarea
            cameraState.animateTo(
                CameraPosition(
                    target = Position(
                        latitude = task.latitud ?: return@let,
                        longitude = task.longitud ?: return@let
                    ),
                    zoom = 15.0
                )
            )
            hasCenteredOnUser = true
        }
    }

    LaunchedEffect(locatedTasks, hasLocationPermission, focusedTaskId) {
        if (focusedTaskId == null && !hasLocationPermission && locatedTasks.isNotEmpty()) {
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
                title = if (quickMode) "Tareas rapidas" else "Mapa",
                subtitle = if (quickMode) {
                    "Encuentra una oportunidad urgente cerca de ti"
                } else {
                    "Oportunidades cerca de ti"
                },
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
                    baseStyle = BaseStyle.Uri(OPEN_FREE_MAP_STYLE_URI),
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
                        strokeWidth = const(3.dp),
                        onClick = { features ->
                            val taskId = features
                                .firstOrNull()
                                ?.properties
                                ?.get("idTarea")
                                ?.jsonPrimitive
                                ?.intOrNull
                            if (taskId != null) {
                                selectedTaskId = taskId
                                ClickResult.Consume
                            } else {
                                ClickResult.Pass
                            }
                        }
                    )

                    selectedTaskGeoJson?.let { geoJson ->
                        val selectedTaskSource = rememberGeoJsonSource(
                            data = GeoJsonData.JsonString(geoJson)
                        )
                        CircleLayer(
                            id = "t4kash-selected-task",
                            source = selectedTaskSource,
                            radius = const(12.dp),
                            color = const(T4Mint),
                            strokeColor = const(T4BrandDark),
                            strokeWidth = const(3.dp)
                        )
                    }

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
                            val trackedPosition = currentLocation.location
                                ?.position
                                ?.value
                                ?.takeIf { it.isUsableLocation() }
                            if (
                                trackedPosition != null &&
                                !hasCenteredOnUser &&
                                focusedTaskId == null
                            ) {
                                cameraState.animateTo(
                                    CameraPosition(
                                        target = trackedPosition,
                                        zoom = 15.0
                                    )
                                )
                                hasCenteredOnUser = true
                            }
                        }
                    }
                }
            }

            if (quickMode && userPosition != null && !isMapLoading) {
                Canvas(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(220.dp)
                ) {
                    listOf(0f, 0.33f, 0.66f).forEach { offset ->
                        val progress = (radarProgress + offset) % 1f
                        drawCircle(
                            color = T4Mint.copy(alpha = (1f - progress) * 0.55f),
                            radius = size.minDimension * 0.5f * progress,
                            style = Stroke(width = 2.dp.toPx())
                        )
                    }
                }
            }

            Card(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(14.dp)
                    .widthIn(max = 360.dp),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = T4Surface.copy(alpha = 0.96f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
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
                                text = when {
                                    quickMode && uiState.isLoadingQuickTasks ->
                                        "Rastreando tareas rapidas..."
                                    visibleTasks.isEmpty() ->
                                        if (quickMode) "Sin tareas rapidas en este radio"
                                        else "Sin oportunidades en este radio"
                                    visibleTasks.size == 1 ->
                                        if (quickMode) "1 tarea rapida encontrada"
                                        else "1 oportunidad en el mapa"
                                    else -> if (quickMode) {
                                        "${visibleTasks.size} tareas rapidas encontradas"
                                    } else {
                                        "${visibleTasks.size} oportunidades en el mapa"
                                    }
                                },
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = T4Text
                            )
                            Text(
                                text = when {
                                    quickMode && uiState.quickTasksError != null ->
                                        uiState.quickTasksError
                                    !quickMode && uiState.errorMessage != null ->
                                        uiState.errorMessage
                                    userPosition == null ->
                                        "Esperando una ubicación válida del teléfono."
                                    focusedTask != null ->
                                        "Mostrando la ubicación de ${focusedTask.titulo}."
                                    else -> if (quickMode) {
                                        "El radar se actualiza al mover el radio."
                                    } else {
                                        "Tareas presenciales o hibridas dentro del radio."
                                    }
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = T4TextMuted
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Radio de búsqueda",
                            style = MaterialTheme.typography.labelMedium,
                            color = T4Text
                        )
                        Text(
                            text = if (quickMode) {
                                String.format("%.2f km", radiusKm)
                            } else {
                                "${radiusKm.roundToInt()} km"
                            },
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = T4Primary
                        )
                    }
                    Slider(
                        value = radiusKm,
                        onValueChange = { radiusKm = it },
                        valueRange = if (quickMode) 0.25f..5f else 5f..50f,
                        steps = if (quickMode) 18 else 8
                    )
                }
            }

            SmallFloatingActionButton(
                onClick = {
                    if (!hasLocationPermission) {
                        locationPermissionLauncher.launch(LOCATION_PERMISSIONS)
                    } else {
                        userPosition?.let { validPosition ->
                            coroutineScope.launch {
                                cameraState.animateTo(
                                    CameraPosition(
                                        target = validPosition,
                                        zoom = 15.0
                                    )
                                )
                            }
                        }
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(
                        end = 18.dp,
                        bottom = if (selectedTask == null) 18.dp else 154.dp
                    ),
                containerColor = T4Mint,
                contentColor = T4BrandDark
            ) {
                Icon(
                    imageVector = Icons.Filled.MyLocation,
                    contentDescription = "Centrar en mi ubicación"
                )
            }

            selectedTask?.let { task ->
                Card(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(14.dp)
                        .widthIn(max = 360.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = T4Surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
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
                                    text = task.titulo,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = T4Text,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = task.direccionReferencia
                                        ?: task.modalidad
                                        ?: "Oportunidad presencial",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = T4TextMuted,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Text(
                                text = selectedTaskDistance?.let(::formatDistance)
                                    ?: "Sin distancia",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = T4Primary
                            )
                        }
                        Button(
                            onClick = {
                                if (quickMode) {
                                    viewModel.claimQuickTask(task.idTarea)
                                } else {
                                    onTaskSelected(task.idTarea)
                                }
                            },
                            enabled = uiState.claimingQuickTaskId == null,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (quickMode && uiState.claimingQuickTaskId == task.idTarea) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = Color.White
                                )
                            } else {
                                Text(if (quickMode) "Tomar tarea ahora" else "Ver oportunidad")
                            }
                        }
                    }
                }
            }

            if (isMapLoading) {
                Card(
                    modifier = Modifier.align(Alignment.Center),
                    shape = RoundedCornerShape(8.dp),
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
                    shape = RoundedCornerShape(8.dp),
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

private fun TaskDto.hasValidCoordinates(): Boolean {
    return geoPoint()?.isValid() == true
}

private fun Position.isUsableLocation(): Boolean {
    return toGeoPoint().isValid()
}

private fun TaskDto.distanceTo(position: Position): Double {
    val taskPoint = geoPoint() ?: return Double.POSITIVE_INFINITY
    return distanceInKilometers(position.toGeoPoint(), taskPoint)
}

private fun TaskDto.geoPoint(): GeoPoint? {
    val latitude = latitud ?: return null
    val longitude = longitud ?: return null
    return GeoPoint(latitude, longitude)
}

private fun Position.toGeoPoint(): GeoPoint {
    return GeoPoint(latitude, longitude)
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
