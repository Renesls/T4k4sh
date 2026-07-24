package com.t4kash.app.ui.screen

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.TipsAndUpdates
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.t4kash.app.ui.components.StatusChip
import com.t4kash.app.ui.components.T4BottomBar
import com.t4kash.app.ui.components.T4TopBar
import com.t4kash.app.ui.model.CreateTaskRequest
import com.t4kash.app.ui.navigation.Routes
import com.t4kash.app.ui.theme.T4Background
import com.t4kash.app.ui.theme.T4Border
import com.t4kash.app.ui.theme.T4MintDark
import com.t4kash.app.ui.theme.T4Primary
import com.t4kash.app.ui.theme.T4PrimarySoft
import com.t4kash.app.ui.theme.T4Surface
import com.t4kash.app.ui.theme.T4Text
import com.t4kash.app.ui.theme.T4TextMuted
import com.t4kash.app.ui.viewmodel.MarketplaceViewModel
import org.maplibre.compose.location.rememberDefaultLocationProvider
import org.maplibre.compose.location.rememberNullLocationProvider
import org.maplibre.compose.location.rememberUserLocationState
import java.util.Locale

private const val MODALIDAD_REMOTA = "REMOTA"
private val MODALIDADES = listOf("REMOTA", "PRESENCIAL", "HIBRIDA")

@Composable
fun PostTaskScreen(
    currentRoute: String = Routes.POST,
    viewModel: MarketplaceViewModel,
    onNavigate: (String) -> Unit,
    onTaskPublished: () -> Unit
) {
    val context = LocalContext.current
    val uiState = viewModel.uiState
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var budget by remember { mutableStateOf("") }
    var modality by remember { mutableStateOf(MODALIDAD_REMOTA) }
    var addressReference by remember { mutableStateOf("") }
    var latitude by remember { mutableStateOf<Double?>(null) }
    var longitude by remember { mutableStateOf<Double?>(null) }
    var selectedCategoryId by remember { mutableStateOf<Int?>(null) }
    var validationError by remember { mutableStateOf<String?>(null) }
    var captureLocationRequested by remember { mutableStateOf(false) }
    var hasLocationPermission by remember {
        mutableStateOf(context.hasTaskLocationPermission())
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasLocationPermission =
            permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (!hasLocationPermission) {
            captureLocationRequested = false
            validationError =
                "Activa el permiso de ubicación para publicar esta modalidad."
        }
    }

    val locationProvider = if (hasLocationPermission) {
        rememberDefaultLocationProvider()
    } else {
        rememberNullLocationProvider()
    }
    val locationState = rememberUserLocationState(locationProvider)

    LaunchedEffect(uiState.categories) {
        if (selectedCategoryId == null) {
            selectedCategoryId = uiState.categories.firstOrNull()?.idCategoria
        }
    }

    LaunchedEffect(
        captureLocationRequested,
        hasLocationPermission,
        locationState.location
    ) {
        if (captureLocationRequested && hasLocationPermission) {
            locationState.location?.position?.value?.let { position ->
                latitude = position.latitude
                longitude = position.longitude
                captureLocationRequested = false
                validationError = null
            }
        }
    }

    LaunchedEffect(uiState.publishedTask?.idTarea) {
        if (uiState.publishedTask != null) {
            onTaskPublished()
            viewModel.clearPublishFeedback()
        }
    }

    fun publish() {
        val numericBudget = budget.toDoubleOrNull()
        validationError = when {
            title.isBlank() -> "Escribe un título para la oportunidad."
            description.trim().length < 20 ->
                "Agrega una descripción de al menos 20 caracteres."
            numericBudget == null || numericBudget < 0 ->
                "Ingresa un presupuesto numérico válido."
            selectedCategoryId == null -> "Selecciona una categoría."
            modality != MODALIDAD_REMOTA && (latitude == null || longitude == null) ->
                "Captura la ubicación para esta modalidad."
            else -> null
        }

        if (validationError != null || numericBudget == null) {
            return
        }

        viewModel.publishTask(
            CreateTaskRequest(
                titulo = title.trim(),
                descripcion = description.trim(),
                presupuesto = numericBudget,
                idCategoria = selectedCategoryId ?: return,
                idCliente = 1,
                modalidad = modality,
                direccionReferencia = addressReference.trim().takeIf {
                    modality != MODALIDAD_REMOTA && it.isNotEmpty()
                },
                latitud = latitude.takeIf { modality != MODALIDAD_REMOTA },
                longitud = longitude.takeIf { modality != MODALIDAD_REMOTA }
            )
        )
    }

    Scaffold(
        containerColor = T4Background,
        topBar = {
            T4TopBar(
                title = "Publicar",
                subtitle = "Crea una nueva oportunidad"
            )
        },
        bottomBar = {
            T4BottomBar(
                currentRoute = currentRoute,
                onNavigate = onNavigate
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(T4Background, Color(0xFFF2F2ED))
                    )
                ),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(T4PrimarySoft, T4Primary)
                            ),
                            RoundedCornerShape(24.dp)
                        )
                        .padding(20.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            StatusChip(
                                text = "Publicación",
                                selected = true,
                                containerColor = Color.White.copy(alpha = 0.18f),
                                contentColor = Color.White
                            )
                            StatusChip(
                                text = "En línea",
                                selected = true,
                                containerColor = Color.White.copy(alpha = 0.14f),
                                contentColor = Color.White
                            )
                        }
                        Text(
                            text = "Publica una oportunidad clara y atractiva.",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Al publicarla quedará disponible en el marketplace y, si tiene ubicación, también en el mapa.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.84f)
                        )
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = T4Surface),
                    shape = RoundedCornerShape(22.dp),
                    border = BorderStroke(1.dp, T4Border.copy(alpha = 0.60f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "Datos de la oportunidad",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = T4Text
                        )

                        OutlinedTextField(
                            value = title,
                            onValueChange = {
                                title = it
                                validationError = null
                            },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Título") },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp)
                        )
                        OutlinedTextField(
                            value = description,
                            onValueChange = {
                                description = it
                                validationError = null
                            },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Descripción") },
                            minLines = 4,
                            shape = RoundedCornerShape(16.dp)
                        )
                        OutlinedTextField(
                            value = budget,
                            onValueChange = {
                                budget = it
                                validationError = null
                            },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Presupuesto") },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Decimal
                            )
                        )

                        Text(
                            text = "Categoría",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = T4Text
                        )
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(
                                items = uiState.categories,
                                key = { it.idCategoria }
                            ) { category ->
                                FilterChip(
                                    selected = selectedCategoryId == category.idCategoria,
                                    onClick = {
                                        selectedCategoryId = category.idCategoria
                                        validationError = null
                                    },
                                    label = { Text(category.nombreCategoria) }
                                )
                            }
                        }

                        Text(
                            text = "Modalidad",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = T4Text
                        )
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(MODALIDADES) { option ->
                                FilterChip(
                                    selected = modality == option,
                                    onClick = {
                                        modality = option
                                        validationError = null
                                        if (option == MODALIDAD_REMOTA) {
                                            addressReference = ""
                                            latitude = null
                                            longitude = null
                                            captureLocationRequested = false
                                        }
                                    },
                                    label = {
                                        Text(
                                            option.lowercase()
                                                .replaceFirstChar { it.uppercase() }
                                        )
                                    }
                                )
                            }
                        }

                        if (modality != MODALIDAD_REMOTA) {
                            OutlinedTextField(
                                value = addressReference,
                                onValueChange = { addressReference = it },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Referencia del lugar") },
                                singleLine = true,
                                shape = RoundedCornerShape(16.dp),
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Filled.LocationOn,
                                        contentDescription = null
                                    )
                                }
                            )

                            Button(
                                onClick = {
                                    captureLocationRequested = true
                                    validationError = null
                                    if (!hasLocationPermission) {
                                        permissionLauncher.launch(TASK_LOCATION_PERMISSIONS)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.MyLocation,
                                    contentDescription = null
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    when {
                                        latitude != null && longitude != null ->
                                            "Actualizar mi ubicación"
                                        captureLocationRequested -> "Buscando ubicación..."
                                        else -> "Usar mi ubicación actual"
                                    }
                                )
                            }

                            if (latitude != null && longitude != null) {
                                Text(
                                    text = String.format(
                                        Locale.US,
                                        "Ubicación lista: %.6f, %.6f",
                                        latitude,
                                        longitude
                                    ),
                                    color = T4MintDark,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }

                        validationError?.let { message ->
                            Text(
                                text = message,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        uiState.publishError?.let { message ->
                            Text(
                                text = message,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        Button(
                            onClick = ::publish,
                            enabled = !uiState.isPublishing,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                        ) {
                            if (uiState.isPublishing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.height(22.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Filled.CheckCircle,
                                    contentDescription = null
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Publicar oportunidad")
                            }
                        }
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = T4Surface),
                    shape = RoundedCornerShape(22.dp),
                    border = BorderStroke(1.dp, T4Border.copy(alpha = 0.50f))
                ) {
                    Row(
                        modifier = Modifier.padding(18.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.TipsAndUpdates,
                            contentDescription = null,
                            tint = T4Primary
                        )
                        Text(
                            text = "Las tareas remotas no publican coordenadas. Las presenciales e híbridas aparecerán en el mapa.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = T4TextMuted
                        )
                    }
                }
            }
        }
    }
}

private val TASK_LOCATION_PERMISSIONS = arrayOf(
    Manifest.permission.ACCESS_FINE_LOCATION,
    Manifest.permission.ACCESS_COARSE_LOCATION
)

private fun Context.hasTaskLocationPermission(): Boolean {
    return ContextCompat.checkSelfPermission(
        this,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
}
