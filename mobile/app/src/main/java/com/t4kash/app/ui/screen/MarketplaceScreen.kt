package com.t4kash.app.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.t4kash.app.ui.components.ConnectionErrorState
import com.t4kash.app.ui.components.EmptyState
import com.t4kash.app.ui.components.StatusChip
import com.t4kash.app.ui.components.T4BottomBar
import com.t4kash.app.ui.components.T4TopBar
import com.t4kash.app.ui.model.CategoryDto
import com.t4kash.app.ui.model.TaskDto
import com.t4kash.app.ui.navigation.Routes
import com.t4kash.app.ui.theme.T4Amber
import com.t4kash.app.ui.theme.T4Background
import com.t4kash.app.ui.theme.T4Primary
import com.t4kash.app.ui.theme.T4Surface
import com.t4kash.app.ui.theme.T4Text
import com.t4kash.app.ui.theme.T4TextMuted
import com.t4kash.app.ui.viewmodel.MarketplaceViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarketplaceScreen(
    viewModel: MarketplaceViewModel = viewModel(),
    currentRoute: String = Routes.MARKETPLACE,
    onNavigate: (String) -> Unit = {},
    onTaskSelected: (TaskDto) -> Unit = {},
    onCreateTask: () -> Unit = {}
) {
    val state = viewModel.uiState
    var query by remember { mutableStateOf("") }
    var selectedCategoryId by remember { mutableIntStateOf(0) }

    val filteredTasks = remember(state.tasks, query, selectedCategoryId) {
        state.tasks.filter { task ->
            val matchesQuery = query.isBlank() ||
                task.titulo.contains(query, ignoreCase = true) ||
                task.descripcion.contains(query, ignoreCase = true)
            val matchesCategory = selectedCategoryId == 0 || task.idCategoria == selectedCategoryId
            matchesQuery && matchesCategory
        }
    }

    Scaffold(
        containerColor = T4Background,
        topBar = {
            T4TopBar(
                title = "T4KASH",
                subtitle = "Oportunidades universitarias",
                actions = {
                    IconButton(onClick = {}) {
                        Icon(
                            imageVector = Icons.Filled.Notifications,
                            contentDescription = "Notificaciones"
                        )
                    }
                }
            )
        },
        bottomBar = {
            T4BottomBar(
                currentRoute = currentRoute,
                onNavigate = onNavigate
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onCreateTask,
                icon = {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = null
                    )
                },
                text = { Text("Post") },
                containerColor = T4Primary,
                contentColor = androidx.compose.ui.graphics.Color.White
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(T4Background),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Search tasks or students...") },
                    singleLine = true,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = null
                        )
                    }
                )
            }

            if (state.categories.isNotEmpty()) {
                item {
                    CategoryChips(
                        categories = state.categories,
                        selectedCategoryId = selectedCategoryId,
                        onSelected = { selectedCategoryId = it }
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
                            text = "Explorar",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = T4Text
                        )
                        Text(
                            text = "${filteredTasks.size} oportunidades disponibles",
                            style = MaterialTheme.typography.bodyMedium,
                            color = T4TextMuted
                        )
                    }
                }
            }

            when {
                state.isLoading -> {
                    item {
                        LoadingState()
                    }
                }

                state.errorMessage != null -> {
                    item {
                        ConnectionErrorState(
                            message = state.errorMessage,
                            onRetry = viewModel::refresh
                        )
                    }
                }

                filteredTasks.isEmpty() -> {
                    item {
                        EmptyState(
                            title = "No encontramos oportunidades",
                            message = "Prueba con otro filtro o crea una nueva tarea desde la seccion Post."
                        )
                    }
                }

                else -> {
                    items(filteredTasks, key = { it.idTarea }) { task ->
                        TaskCard(
                            task = task,
                            onClick = { onTaskSelected(task) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryChips(
    categories: List<CategoryDto>,
    selectedCategoryId: Int,
    onSelected: (Int) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            StatusChip(
                text = "All",
                selected = selectedCategoryId == 0,
                modifier = Modifier.clickable { onSelected(0) }
            )
        }
        items(categories, key = { it.idCategoria }) { category ->
            StatusChip(
                text = category.nombreCategoria,
                selected = selectedCategoryId == category.idCategoria,
                modifier = Modifier.clickable { onSelected(category.idCategoria) }
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
    onClick: () -> Unit
) {
    val accentCard = task.tipoOportunidad.equals("tech", ignoreCase = true) ||
        task.modalidad.equals("remoto", ignoreCase = true)

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (accentCard) T4Primary else T4Surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                StatusChip(
                    text = task.tipoOportunidad.ifBlank { "Task" },
                    selected = true,
                    modifier = if (accentCard) {
                        Modifier.background(T4Primary)
                    } else {
                        Modifier
                    }
                )
                Text(
                    text = "$${"%.2f".format(task.presupuesto)}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (accentCard) androidx.compose.ui.graphics.Color.White else T4Amber
                )
            }

            Text(
                text = task.titulo,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = if (accentCard) androidx.compose.ui.graphics.Color.White else T4Text
            )
            Text(
                text = task.descripcion,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMedium,
                color = if (accentCard) {
                    androidx.compose.ui.graphics.Color.White.copy(alpha = 0.82f)
                } else {
                    T4TextMuted
                }
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.School,
                        contentDescription = null,
                        tint = if (accentCard) androidx.compose.ui.graphics.Color.White else T4Primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = task.modalidad ?: "Campus",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (accentCard) androidx.compose.ui.graphics.Color.White else T4TextMuted
                    )
                }
                Text(
                    text = task.estadoTarea,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (accentCard) {
                        androidx.compose.ui.graphics.Color.White.copy(alpha = 0.72f)
                    } else {
                        T4TextMuted
                    }
                )
            }
        }
    }
}
