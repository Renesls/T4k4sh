package com.t4kash.app.ui.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.WorkOutline
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.t4kash.app.ui.components.ConnectionErrorState
import com.t4kash.app.ui.components.EmptyState
import com.t4kash.app.ui.components.SearchableSelectionDialog
import com.t4kash.app.ui.components.SelectionOption
import com.t4kash.app.ui.components.StatusChip
import com.t4kash.app.ui.components.T4BottomBar
import com.t4kash.app.ui.components.T4BrandMark
import com.t4kash.app.ui.components.keepVisibleAboveKeyboard
import com.t4kash.app.ui.components.t4CategoryColors
import com.t4kash.app.ui.formatNioCurrency
import com.t4kash.app.ui.model.CategoryDto
import com.t4kash.app.ui.model.TaskDto
import com.t4kash.app.ui.navigation.Routes
import com.t4kash.app.ui.session.SessionUser
import com.t4kash.app.ui.theme.T4Background
import com.t4kash.app.ui.theme.T4Border
import com.t4kash.app.ui.theme.T4Mint
import com.t4kash.app.ui.theme.T4MintDark
import com.t4kash.app.ui.theme.T4Primary
import com.t4kash.app.ui.theme.T4PrimaryContainer
import com.t4kash.app.ui.theme.T4Surface
import com.t4kash.app.ui.theme.T4SurfaceVariant
import com.t4kash.app.ui.theme.T4Text
import com.t4kash.app.ui.theme.T4TextMuted
import com.t4kash.app.ui.viewmodel.MarketplaceViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarketplaceScreen(
    viewModel: MarketplaceViewModel = viewModel(),
    user: SessionUser? = null,
    currentRoute: String = Routes.MARKETPLACE,
    onNavigate: (String) -> Unit = {},
    onTaskSelected: (TaskDto) -> Unit = {},
    onOpenMap: () -> Unit = {},
    onOpenQuickTasks: () -> Unit = {},
    onOpenProfile: () -> Unit = {},
    onOpenNotifications: () -> Unit = {},
    unreadNotifications: Int = 0
) {
    val state = viewModel.uiState
    var query by remember { mutableStateOf("") }
    var selectedCategoryId by remember { mutableIntStateOf(0) }
    var showCategoryDialog by remember { mutableStateOf(false) }

    val availableTasks = remember(state.tasks) {
        state.tasks.filter {
            it.estadoTarea.equals("PUBLICADA", ignoreCase = true) &&
                !it.tipoOportunidad.equals("RAPIDA", ignoreCase = true)
        }
    }
    val filteredTasks = remember(availableTasks, query, selectedCategoryId) {
        availableTasks.filter { task ->
            val matchesQuery = query.isBlank() ||
                task.titulo.contains(query, ignoreCase = true) ||
                task.descripcion.contains(query, ignoreCase = true)
            val matchesCategory = selectedCategoryId == 0 || task.idCategoria == selectedCategoryId
            matchesQuery && matchesCategory
        }
    }
    val categoriesById = remember(state.categories) {
        state.categories.associateBy { it.idCategoria }
    }
    val mappedTasks = remember(availableTasks) {
        availableTasks.count { it.latitud != null && it.longitud != null }
    }

    if (showCategoryDialog) {
        SearchableSelectionDialog(
            title = "Filtrar por categoria",
            options = state.categories.map {
                SelectionOption(it.idCategoria, it.nombreCategoria)
            },
            selectedId = selectedCategoryId.takeIf { it != 0 },
            onDismiss = { showCategoryDialog = false },
            onSelected = { selectedCategoryId = it }
        )
    }

    Scaffold(
        containerColor = T4Background,
        topBar = {
            HomeTopBar(
                user = user,
                unreadNotifications = unreadNotifications,
                onOpenNotifications = onOpenNotifications,
                onOpenProfile = onOpenProfile
            )
        },
        bottomBar = {
            T4BottomBar(
                currentRoute = currentRoute,
                onNavigate = onNavigate,
                onReselect = { route ->
                    if (route == Routes.MARKETPLACE && !state.isLoading) {
                        viewModel.refresh(force = true)
                    }
                }
            )
        }
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = state.isLoading && state.tasks.isNotEmpty(),
            onRefresh = { viewModel.refresh(force = true) },
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(T4Background),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    HomeMetrics(
                        available = availableTasks.size,
                        categories = state.categories.size,
                        mapped = mappedTasks,
                        onOpenMap = onOpenMap
                    )
                }

                item {
                    QuickTaskBanner(onClick = onOpenQuickTasks)
                }

                item {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .keepVisibleAboveKeyboard(),
                        placeholder = { Text("Buscar oportunidades...") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = T4Surface,
                            unfocusedContainerColor = T4Surface,
                            focusedBorderColor = T4Primary,
                            unfocusedBorderColor = T4Border
                        ),
                        leadingIcon = {
                            Icon(Icons.Filled.Search, contentDescription = null)
                        }
                    )
                }

                if (state.categories.isNotEmpty()) {
                    item {
                        CategoryChips(
                            categories = state.categories,
                            selectedCategoryId = selectedCategoryId,
                            onSelected = { selectedCategoryId = it },
                            onShowAll = { showCategoryDialog = true }
                        )
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Oportunidades",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = T4Text
                            )
                            Text(
                                text = "${filteredTasks.size} disponibles para explorar",
                                style = MaterialTheme.typography.bodySmall,
                                color = T4TextMuted
                            )
                        }
                        TextButton(onClick = onOpenMap) {
                            Icon(
                                imageVector = Icons.Filled.Map,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.size(6.dp))
                            Text("Ver mapa")
                        }
                    }
                }

                when {
                    state.isLoading && state.tasks.isEmpty() -> item { LoadingState() }
                    state.errorMessage != null && state.tasks.isEmpty() -> {
                        item {
                            ConnectionErrorState(
                                message = state.errorMessage,
                                onRetry = { viewModel.refresh(force = true) }
                            )
                        }
                    }
                    filteredTasks.isEmpty() -> {
                        item {
                            EmptyState(
                                title = "No encontramos oportunidades",
                                message = "Prueba con otro filtro o crea una nueva tarea desde Post."
                            )
                        }
                    }
                    else -> {
                        items(filteredTasks, key = { it.idTarea }) { task ->
                            TaskCard(
                                task = task,
                                categoryLabel = categoriesById[task.idCategoria]?.nombreCategoria,
                                onClick = { onTaskSelected(task) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickTaskBanner(onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = T4Mint),
        border = BorderStroke(1.dp, T4MintDark.copy(alpha = 0.25f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = T4Text
            ) {
                Icon(
                    imageVector = Icons.Filled.Bolt,
                    contentDescription = null,
                    tint = T4Mint,
                    modifier = Modifier.padding(9.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Tareas rapidas",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = T4Text
                )
                Text(
                    text = "Activa el radar y encuentra trabajos urgentes cerca de ti.",
                    style = MaterialTheme.typography.bodySmall,
                    color = T4MintDark
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "Abrir tareas rapidas",
                tint = T4Text
            )
        }
    }
}

@Composable
private fun HomeTopBar(
    user: SessionUser?,
    unreadNotifications: Int,
    onOpenNotifications: () -> Unit,
    onOpenProfile: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(T4Surface)
            .statusBarsPadding()
            .padding(horizontal = 18.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            T4BrandMark(showName = false)
            Column {
                Text(
                    text = "Hola,",
                    style = MaterialTheme.typography.labelMedium,
                    color = T4TextMuted
                )
                Text(
                    text = user?.firstName?.ifBlank { "T4KASH" } ?: "T4KASH",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = T4Text
                )
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = T4SurfaceVariant
            ) {
                IconButton(onClick = onOpenNotifications) {
                    BadgedBox(
                        badge = {
                            if (unreadNotifications > 0) {
                                Badge {
                                    Text(unreadNotifications.coerceAtMost(99).toString())
                                }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Notifications,
                            contentDescription = "Notificaciones",
                            tint = T4TextMuted
                        )
                    }
                }
            }
            Surface(
                modifier = Modifier
                    .size(44.dp)
                    .clickable(onClick = onOpenProfile),
                shape = RoundedCornerShape(12.dp),
                color = T4Primary
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = user?.initials ?: "TK",
                        color = Color.White,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeMetrics(
    available: Int,
    categories: Int,
    mapped: Int,
    onOpenMap: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        HomeMetricCard(
            value = available.toString(),
            label = "Disponibles",
            icon = Icons.Filled.WorkOutline,
            tint = T4Primary,
            modifier = Modifier.weight(1f)
        )
        HomeMetricCard(
            value = categories.toString(),
            label = "Categorias",
            icon = Icons.Filled.Category,
            tint = T4MintDark,
            modifier = Modifier.weight(1f)
        )
        HomeMetricCard(
            value = mapped.toString(),
            label = "En el mapa",
            icon = Icons.Filled.LocationOn,
            tint = T4Primary,
            modifier = Modifier
                .weight(1f)
                .clickable(onClick = onOpenMap)
        )
    }
}

@Composable
private fun HomeMetricCard(
    value: String,
    label: String,
    icon: ImageVector,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = T4Surface),
        border = BorderStroke(1.dp, T4Border),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = tint
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = T4TextMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun CategoryChips(
    categories: List<CategoryDto>,
    selectedCategoryId: Int,
    onSelected: (Int) -> Unit,
    onShowAll: () -> Unit
) {
    val selectedCategory = categories.firstOrNull {
        it.idCategoria == selectedCategoryId
    }
    val visibleCategories = listOfNotNull(selectedCategory) + categories
        .filterNot { it.idCategoria == selectedCategoryId }
        .take(if (selectedCategory == null) 6 else 5)

    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            StatusChip(
                text = "Todas",
                selected = selectedCategoryId == 0,
                containerColor = if (selectedCategoryId == 0) T4Primary else T4Surface,
                contentColor = if (selectedCategoryId == 0) Color.White else T4TextMuted,
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable { onSelected(0) }
            )
        }
        items(visibleCategories, key = { it.idCategoria }) { category ->
            val selected = selectedCategoryId == category.idCategoria
            val categoryColors = t4CategoryColors(category.idCategoria)
            StatusChip(
                text = category.nombreCategoria,
                selected = selected,
                containerColor = if (selected) T4Mint else categoryColors.container,
                contentColor = if (selected) T4MintDark else categoryColors.content,
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable { onSelected(category.idCategoria) }
            )
        }
        item {
            StatusChip(
                text = "Mas categorias",
                selected = false,
                containerColor = T4SurfaceVariant,
                contentColor = T4Primary,
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable(onClick = onShowAll)
            )
        }
    }
}

@Composable
private fun LoadingState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 64.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator()
        Spacer(modifier = Modifier.height(12.dp))
        Text("Cargando oportunidades...")
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TaskCard(
    task: TaskDto,
    categoryLabel: String?,
    onClick: () -> Unit
) {
    val tag = categoryLabel?.takeIf { it.isNotBlank() }
        ?: task.tipoOportunidad.ifBlank { "Tarea" }
    val categoryColors = t4CategoryColors(task.idCategoria)

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = T4Surface),
        border = BorderStroke(1.dp, T4Border),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .background(categoryColors.content)
        )
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(11.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                Surface(
                    modifier = Modifier.size(42.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = categoryColors.container
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.WorkOutline,
                            contentDescription = null,
                            tint = categoryColors.content,
                            modifier = Modifier.size(21.dp)
                        )
                    }
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    StatusChip(
                        text = tag,
                        containerColor = categoryColors.container,
                        contentColor = categoryColors.content
                    )
                    Text(
                        text = task.titulo,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = T4Text,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = formatNioCurrency(task.presupuesto),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = T4MintDark
                )
            }

            Text(
                text = task.descripcion,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMedium,
                color = T4TextMuted
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (task.latitud != null) {
                            Icons.Filled.LocationOn
                        } else {
                            Icons.Filled.WorkOutline
                        },
                        contentDescription = null,
                        tint = T4TextMuted,
                        modifier = Modifier.size(17.dp)
                    )
                    Text(
                        text = task.modalidad ?: "Remota",
                        style = MaterialTheme.typography.labelMedium,
                        color = T4TextMuted
                    )
                }
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = T4PrimaryContainer
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Ver detalle",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = T4Primary
                        )
                        Spacer(modifier = Modifier.size(4.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = T4Primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}
