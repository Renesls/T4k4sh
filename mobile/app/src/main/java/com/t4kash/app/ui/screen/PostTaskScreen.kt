package com.t4kash.app.ui.screen

import android.Manifest
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.pm.PackageManager
import android.text.format.DateFormat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.TipsAndUpdates
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.t4kash.app.ui.components.StatusChip
import com.t4kash.app.ui.components.SearchableSelectionDialog
import com.t4kash.app.ui.components.SelectionOption
import com.t4kash.app.ui.components.T4BottomBar
import com.t4kash.app.ui.components.T4PatternSurface
import com.t4kash.app.ui.components.T4TopBar
import com.t4kash.app.ui.components.keepVisibleAboveKeyboard
import com.t4kash.app.ui.model.CreateTaskRequest
import com.t4kash.app.ui.model.PendingAttachment
import com.t4kash.app.ui.navigation.Routes
import com.t4kash.app.ui.parseApiDateTime
import com.t4kash.app.ui.theme.T4Background
import com.t4kash.app.ui.theme.T4Border
import com.t4kash.app.ui.theme.T4Mint
import com.t4kash.app.ui.theme.T4MintDark
import com.t4kash.app.ui.theme.T4Primary
import com.t4kash.app.ui.theme.T4Surface
import com.t4kash.app.ui.theme.T4Text
import com.t4kash.app.ui.theme.T4TextMuted
import com.t4kash.app.ui.viewmodel.MarketplaceViewModel
import org.maplibre.compose.location.rememberDefaultLocationProvider
import org.maplibre.compose.location.rememberNullLocationProvider
import org.maplibre.compose.location.rememberUserLocationState
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

private const val MODALIDAD_REMOTA = "REMOTA"
private const val MODALIDAD_PRESENCIAL = "PRESENCIAL"
private const val TIPO_OPORTUNIDAD = "TAREA"
private const val TIPO_TAREA_RAPIDA = "RAPIDA"
private const val PAGO_MAXIMO_TAREA_RAPIDA = 1000.0
private const val UNA_HORA_MILLIS = 60 * 60 * 1000L
private val MODALIDADES = listOf("REMOTA", "PRESENCIAL", "HIBRIDA")

@Composable
fun PostTaskScreen(
    currentRoute: String = Routes.POST,
    viewModel: MarketplaceViewModel,
    onNavigate: (String) -> Unit,
    onTaskPublished: () -> Unit,
    editTaskId: Int? = null,
    onBack: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val uiState = viewModel.uiState
    val editingTask = editTaskId?.let { id ->
        uiState.tasks.firstOrNull { it.idTarea == id }
    }
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var budget by remember { mutableStateOf("") }
    var opportunityType by remember { mutableStateOf(TIPO_OPORTUNIDAD) }
    var modality by remember { mutableStateOf(MODALIDAD_REMOTA) }
    var addressReference by remember { mutableStateOf("") }
    var latitude by remember { mutableStateOf<Double?>(null) }
    var longitude by remember { mutableStateOf<Double?>(null) }
    var applicationDeadlineMillis by rememberSaveable {
        mutableStateOf<Long?>(null)
    }
    var taskDeadlineMillis by rememberSaveable {
        mutableStateOf<Long?>(null)
    }
    var selectedCategoryId by remember { mutableStateOf<Int?>(null) }
    var showCategoryDialog by rememberSaveable { mutableStateOf(false) }
    var validationError by remember { mutableStateOf<String?>(null) }
    var pendingAttachments by remember { mutableStateOf<List<PendingAttachment>>(emptyList()) }
    var captureLocationRequested by remember { mutableStateOf(false) }
    var showLocationPicker by rememberSaveable { mutableStateOf(false) }
    var hasLocationPermission by remember {
        mutableStateOf(context.hasTaskLocationPermission())
    }
    var editInitialized by rememberSaveable(editTaskId) {
        mutableStateOf(false)
    }

    LaunchedEffect(editingTask?.idTarea) {
        val task = editingTask ?: return@LaunchedEffect
        if (editInitialized) return@LaunchedEffect
        title = task.titulo
        description = task.descripcion
        budget = task.presupuesto.toString()
        opportunityType = task.tipoOportunidad
        modality = task.modalidad ?: MODALIDAD_REMOTA
        addressReference = task.direccionReferencia.orEmpty()
        latitude = task.latitud
        longitude = task.longitud
        applicationDeadlineMillis = task.fechaLimitePostulacion.toEpochMillis()
        taskDeadlineMillis = task.fechaLimite.toEpochMillis()
        selectedCategoryId = task.idCategoria
        editInitialized = true
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

    val hasRuntimeLocationPermission =
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
    val locationProvider = if (
        hasLocationPermission && hasRuntimeLocationPermission
    ) {
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

    fun finishPublication() {
        viewModel.clearAttachmentFeedback()
        viewModel.clearPublishFeedback()
        onTaskPublished()
    }

    LaunchedEffect(
        uiState.publishedTask?.idTarea,
        uiState.updatedTask?.idTarea,
        uiState.isUploadingAttachments,
        uiState.attachmentsUploadedTaskId,
        uiState.attachmentsError
    ) {
        val savedTask = if (editTaskId == null) {
            uiState.publishedTask
        } else {
            uiState.updatedTask
        } ?: return@LaunchedEffect
        when {
            pendingAttachments.isEmpty() -> finishPublication()
            uiState.attachmentsUploadedTaskId == savedTask.idTarea ->
                finishPublication()
            !uiState.isUploadingAttachments && uiState.attachmentsError == null ->
                viewModel.uploadTaskAttachments(
                    savedTask.idTarea,
                    pendingAttachments
                )
        }
    }

    LaunchedEffect(Unit) {
        viewModel.clearAttachmentFeedback()
        viewModel.clearPublishFeedback()
    }

    if (showLocationPicker) {
        TaskLocationPickerDialog(
            initialLatitude = latitude,
            initialLongitude = longitude,
            onDismiss = { showLocationPicker = false },
            onLocationSelected = { selectedLatitude, selectedLongitude ->
                latitude = selectedLatitude
                longitude = selectedLongitude
                captureLocationRequested = false
                validationError = null
                showLocationPicker = false
            }
        )
    }

    fun publish() {
        val numericBudget = budget.toDoubleOrNull()
        val now = System.currentTimeMillis()
        val isQuickTask = opportunityType == TIPO_TAREA_RAPIDA
        val applicationDeadline = if (isQuickTask) {
            now + (24 * UNA_HORA_MILLIS)
        } else {
            applicationDeadlineMillis
        }
        val taskDeadline = if (isQuickTask) {
            now + (27 * UNA_HORA_MILLIS)
        } else {
            taskDeadlineMillis
        }
        validationError = when {
            title.isBlank() -> "Escribe un título para la oportunidad."
            description.trim().length < 20 ->
                "Agrega una descripción de al menos 20 caracteres."
            numericBudget == null || numericBudget < 0 ->
                "Ingresa un presupuesto numérico válido."
            opportunityType == TIPO_TAREA_RAPIDA && numericBudget <= 0 ->
                "La tarea rápida requiere un pago mayor que cero."
            opportunityType == TIPO_TAREA_RAPIDA &&
                numericBudget > PAGO_MAXIMO_TAREA_RAPIDA ->
                "La tarea rápida no puede superar C$1,000."
            selectedCategoryId == null -> "Selecciona una categoría."
            !isQuickTask && applicationDeadline == null ->
                "Selecciona el cierre de postulaciones."
            !isQuickTask && applicationDeadline != null && applicationDeadline <= now ->
                "El cierre de postulaciones debe ser futuro."
            !isQuickTask && taskDeadline == null ->
                "Selecciona la fecha limite del trabajo."
            !isQuickTask && taskDeadline != null && applicationDeadline != null &&
                taskDeadline <= applicationDeadline ->
                "La fecha limite debe ser posterior al cierre de postulaciones."
            modality != MODALIDAD_REMOTA && (latitude == null || longitude == null) ->
                "Captura la ubicación para esta modalidad."
            else -> null
        }

        if (validationError != null || numericBudget == null) {
            return
        }

        focusManager.clearFocus()
        val request = CreateTaskRequest(
                titulo = title.trim(),
                descripcion = description.trim(),
                presupuesto = numericBudget,
                fechaLimitePostulacion = applicationDeadline?.toApiDateTime(),
                fechaLimite = taskDeadline?.toApiDateTime(),
                idCategoria = selectedCategoryId ?: return,
                tipoOportunidad = opportunityType,
                modalidad = modality,
                direccionReferencia = addressReference.trim().takeIf {
                    modality != MODALIDAD_REMOTA && it.isNotEmpty()
                },
                latitud = latitude.takeIf { modality != MODALIDAD_REMOTA },
                longitud = longitude.takeIf { modality != MODALIDAD_REMOTA }
            )
        if (editTaskId == null) {
            viewModel.publishTask(request)
        } else {
            viewModel.updateTask(editTaskId, request)
        }
    }

    val selectedCategory = uiState.categories.firstOrNull {
        it.idCategoria == selectedCategoryId
    }

    if (showCategoryDialog) {
        SearchableSelectionDialog(
            title = "Seleccionar categoria",
            options = uiState.categories.map {
                SelectionOption(it.idCategoria, it.nombreCategoria)
            },
            selectedId = selectedCategoryId,
            onDismiss = { showCategoryDialog = false },
            onSelected = {
                selectedCategoryId = it
                validationError = null
            }
        )
    }

    Scaffold(
        containerColor = T4Background,
        topBar = {
            T4TopBar(
                title = if (editTaskId == null) "Publicar" else "Editar publicacion",
                subtitle = if (editTaskId == null) {
                    "Crea una nueva oportunidad"
                } else {
                    "Actualiza la informacion"
                },
                onBack = onBack
            )
        },
        bottomBar = {
            if (editTaskId == null) {
                T4BottomBar(
                    currentRoute = currentRoute,
                    onNavigate = onNavigate
                )
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(T4Background, Color(0xFFF2F2ED))
                    )
                ),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                T4PatternSurface(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            StatusChip(
                                text = if (opportunityType == TIPO_TAREA_RAPIDA) {
                                    "Tarea rápida"
                                } else {
                                    "Publicación"
                                },
                                selected = true,
                                containerColor = Color.White.copy(alpha = 0.18f),
                                contentColor = Color.White
                            )
                            StatusChip(
                                text = if (opportunityType == TIPO_TAREA_RAPIDA) {
                                    "Cerca de ti"
                                } else {
                                    "Marketplace"
                                },
                                selected = true,
                                containerColor = Color.White.copy(alpha = 0.14f),
                                contentColor = Color.White
                            )
                        }
                        Text(
                            text = if (opportunityType == TIPO_TAREA_RAPIDA) {
                                "Resuelve algo urgente cerca de ti."
                            } else {
                                "Publica una oportunidad clara y atractiva."
                            },
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = if (opportunityType == TIPO_TAREA_RAPIDA) {
                                "Aparecerá en el radar y se asignará al primer estudiante que la tome."
                            } else {
                                "Al publicarla quedará disponible en el marketplace y, si tiene ubicación, también en el mapa."
                            },
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
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, T4Border.copy(alpha = 0.60f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
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

                        Text(
                            text = "Tipo de publicación",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = T4Text
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = opportunityType == TIPO_OPORTUNIDAD,
                                onClick = { opportunityType = TIPO_OPORTUNIDAD },
                                label = { Text("Oportunidad") }
                            )
                            FilterChip(
                                selected = opportunityType == TIPO_TAREA_RAPIDA,
                                onClick = {
                                    opportunityType = TIPO_TAREA_RAPIDA
                                    modality = MODALIDAD_PRESENCIAL
                                    val now = System.currentTimeMillis()
                                    applicationDeadlineMillis = now + (24 * UNA_HORA_MILLIS)
                                    taskDeadlineMillis = now + (27 * UNA_HORA_MILLIS)
                                    validationError = null
                                },
                                label = { Text("Tarea rápida") }
                            )
                        }

                        OutlinedTextField(
                            value = title,
                            onValueChange = {
                                title = it
                                validationError = null
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .keepVisibleAboveKeyboard(),
                            label = { Text("Título") },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            keyboardOptions = KeyboardOptions(
                                imeAction = ImeAction.Next
                            ),
                            keyboardActions = KeyboardActions(
                                onNext = {
                                    focusManager.moveFocus(FocusDirection.Down)
                                }
                            )
                        )
                        OutlinedTextField(
                            value = description,
                            onValueChange = {
                                description = it
                                validationError = null
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .keepVisibleAboveKeyboard(),
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
                            modifier = Modifier
                                .fillMaxWidth()
                                .keepVisibleAboveKeyboard(),
                            label = { Text("Presupuesto") },
                            prefix = { Text("C\$") },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Decimal,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = { focusManager.clearFocus() }
                            )
                        )

                        if (opportunityType == TIPO_TAREA_RAPIDA) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = T4Mint.copy(alpha = 0.16f)
                                ),
                                border = BorderStroke(1.dp, T4MintDark.copy(alpha = 0.28f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Schedule,
                                        contentDescription = null,
                                        tint = T4MintDark
                                    )
                                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Text(
                                            text = "Disponible durante 24 horas",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = T4Text
                                        )
                                        Text(
                                            text = "Al tomarla, inicia de inmediato y hay 3 horas para completarla.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = T4TextMuted
                                        )
                                    }
                                }
                            }
                        } else {
                            Text(
                                text = "Fechas",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = T4Text
                            )
                            DateTimeSelector(
                                label = "Cierre de postulaciones",
                                selectedMillis = applicationDeadlineMillis,
                                onClick = {
                                    val minimumDeadline =
                                        System.currentTimeMillis() + ONE_MINUTE_MILLIS
                                    showDateTimePicker(
                                        context = context,
                                        initialMillis = applicationDeadlineMillis,
                                        minimumMillis = minimumDeadline
                                    ) { selectedMillis ->
                                        if (selectedMillis < minimumDeadline) {
                                            validationError =
                                                "El cierre de postulaciones debe ser futuro."
                                        } else {
                                            applicationDeadlineMillis = selectedMillis
                                            val currentTaskDeadline = taskDeadlineMillis
                                            if (
                                                currentTaskDeadline != null &&
                                                currentTaskDeadline <= selectedMillis
                                            ) {
                                                taskDeadlineMillis = null
                                            }
                                            validationError = null
                                        }
                                    }
                                }
                            )
                            DateTimeSelector(
                                label = "Fecha limite del trabajo",
                                selectedMillis = taskDeadlineMillis,
                                onClick = {
                                    val minimumTaskDeadline = (
                                        applicationDeadlineMillis
                                            ?: System.currentTimeMillis()
                                        ) + ONE_MINUTE_MILLIS
                                    showDateTimePicker(
                                        context = context,
                                        initialMillis = taskDeadlineMillis,
                                        minimumMillis = minimumTaskDeadline
                                    ) { selectedMillis ->
                                        if (selectedMillis < minimumTaskDeadline) {
                                            validationError =
                                                "La entrega debe ocurrir después del cierre."
                                        } else {
                                            taskDeadlineMillis = selectedMillis
                                            validationError = null
                                        }
                                    }
                                }
                            )
                        }

                        Text(
                            text = "Categoría",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = T4Text
                        )
                        OutlinedButton(
                            onClick = { showCategoryDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = selectedCategory?.nombreCategoria
                                    ?: "Seleccionar categoria",
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                imageVector = Icons.Filled.ArrowDropDown,
                                contentDescription = null
                            )
                        }

                        Text(
                            text = "Modalidad",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = T4Text
                        )
                        if (opportunityType == TIPO_TAREA_RAPIDA) {
                            StatusChip(
                                text = "Presencial y pago en efectivo",
                                selected = true,
                                containerColor = T4Mint,
                                contentColor = T4MintDark
                            )
                        } else {
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
                        }

                        if (modality != MODALIDAD_REMOTA) {
                            OutlinedTextField(
                                value = addressReference,
                                onValueChange = { addressReference = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .keepVisibleAboveKeyboard(),
                                label = { Text("Referencia del lugar") },
                                singleLine = true,
                                shape = RoundedCornerShape(16.dp),
                                keyboardOptions = KeyboardOptions(
                                    imeAction = ImeAction.Done
                                ),
                                keyboardActions = KeyboardActions(
                                    onDone = { focusManager.clearFocus() }
                                ),
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

                            OutlinedButton(
                                onClick = {
                                    focusManager.clearFocus()
                                    showLocationPicker = true
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Map,
                                    contentDescription = null
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    if (latitude == null || longitude == null) {
                                        "Elegir ubicación en el mapa"
                                    } else {
                                        "Ajustar ubicación en el mapa"
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
                        (uiState.publishError ?: uiState.taskMutationError)?.let { message ->
                            Text(
                                text = message,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                    }
                }
            }

            item {
                AttachmentPickerSection(
                    attachments = pendingAttachments,
                    onAttachmentsChange = {
                        pendingAttachments = it
                        validationError = null
                    },
                    onError = { validationError = it },
                    enabled = !uiState.isPublishing &&
                        !uiState.isUpdatingTask &&
                        !uiState.isUploadingAttachments &&
                        uiState.publishedTask == null &&
                        uiState.updatedTask == null
                )
            }

            uiState.attachmentsError?.let { error ->
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = T4Surface),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.error.copy(alpha = 0.35f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = error,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = {
                                        (uiState.publishedTask ?: uiState.updatedTask)?.let { task ->
                                            viewModel.clearAttachmentFeedback()
                                            viewModel.uploadTaskAttachments(
                                                task.idTarea,
                                                pendingAttachments
                                            )
                                        }
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Reintentar")
                                }
                                Button(
                                    onClick = ::finishPublication,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Continuar")
                                }
                            }
                        }
                    }
                }
            }

            item {
                Button(
                    onClick = ::publish,
                    enabled = !uiState.isPublishing &&
                        !uiState.isUpdatingTask &&
                        !uiState.isUploadingAttachments &&
                        uiState.publishedTask == null &&
                        uiState.updatedTask == null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    if (
                        uiState.isPublishing ||
                        uiState.isUpdatingTask ||
                        uiState.isUploadingAttachments
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.height(22.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            if (uiState.isUploadingAttachments) {
                                "Subiendo archivos..."
                            } else if (uiState.isUpdatingTask) {
                                "Guardando cambios..."
                            } else {
                                "Publicando..."
                            }
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            if (editTaskId == null) {
                                if (opportunityType == TIPO_TAREA_RAPIDA) {
                                    "Publicar tarea rápida"
                                } else {
                                    "Publicar oportunidad"
                                }
                            } else {
                                "Guardar cambios"
                            }
                        )
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

@Composable
private fun DateTimeSelector(
    label: String,
    selectedMillis: Long?,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Icon(
            imageVector = Icons.Filled.CalendarMonth,
            contentDescription = null
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = T4TextMuted
            )
            Text(
                text = selectedMillis.toDisplayDateTime(),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = T4Text
            )
        }
        Icon(
            imageVector = Icons.Filled.Schedule,
            contentDescription = "Seleccionar fecha y hora"
        )
    }
}

private fun showDateTimePicker(
    context: Context,
    initialMillis: Long?,
    minimumMillis: Long,
    onSelected: (Long) -> Unit
) {
    val initialCalendar = Calendar.getInstance().apply {
        timeInMillis = initialMillis?.takeIf { it >= minimumMillis }
            ?: minimumMillis + ONE_HOUR_MILLIS
    }
    val datePicker = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            val selectedCalendar = Calendar.getInstance().apply {
                set(
                    year,
                    month,
                    dayOfMonth,
                    initialCalendar.get(Calendar.HOUR_OF_DAY),
                    initialCalendar.get(Calendar.MINUTE),
                    0
                )
                set(Calendar.MILLISECOND, 0)
            }
            TimePickerDialog(
                context,
                { _, hourOfDay, minute ->
                    selectedCalendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                    selectedCalendar.set(Calendar.MINUTE, minute)
                    onSelected(selectedCalendar.timeInMillis)
                },
                initialCalendar.get(Calendar.HOUR_OF_DAY),
                initialCalendar.get(Calendar.MINUTE),
                DateFormat.is24HourFormat(context)
            ).show()
        },
        initialCalendar.get(Calendar.YEAR),
        initialCalendar.get(Calendar.MONTH),
        initialCalendar.get(Calendar.DAY_OF_MONTH)
    )
    datePicker.datePicker.minDate = minimumMillis
    datePicker.show()
}

private fun Long?.toDisplayDateTime(): String {
    if (this == null) {
        return "Seleccionar fecha y hora"
    }
    return SimpleDateFormat(
        "dd/MM/yyyy · HH:mm",
        Locale.getDefault()
    ).format(this)
}

private fun Long?.toApiDateTime(): String? {
    if (this == null) {
        return null
    }
    return SimpleDateFormat(
        "yyyy-MM-dd'T'HH:mm:ss",
        Locale.US
    ).format(this)
}

private fun String?.toEpochMillis(): Long? {
    return parseApiDateTime(this)?.time
}

private const val ONE_MINUTE_MILLIS = 60_000L
private const val ONE_HOUR_MILLIS = 3_600_000L

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
