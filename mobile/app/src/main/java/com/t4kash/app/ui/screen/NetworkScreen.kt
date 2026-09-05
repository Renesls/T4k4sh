package com.t4kash.app.ui.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Interests
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import com.t4kash.app.ui.components.T4BottomBar
import com.t4kash.app.ui.components.T4LightPatternHeader
import com.t4kash.app.ui.components.T4TopBar
import com.t4kash.app.ui.components.keepVisibleAboveKeyboard
import com.t4kash.app.ui.model.NetworkCommentDto
import com.t4kash.app.ui.model.NetworkFeedScope
import com.t4kash.app.ui.model.NetworkPostDto
import com.t4kash.app.ui.model.PublicIdentityDto
import com.t4kash.app.ui.navigation.Routes
import com.t4kash.app.ui.parseApiDateTime
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
import com.t4kash.app.ui.viewmodel.NetworkViewModel
import java.util.Date
import java.util.concurrent.TimeUnit

private data class PostKind(
    val value: String,
    val label: String
)

private val networkPostKinds = listOf(
    PostKind("TEXTO", "Publicacion"),
    PostKind("PREGUNTA", "Pregunta"),
    PostKind("PROYECTO", "Proyecto"),
    PostKind("LOGRO", "Logro"),
    PostKind("RECURSO", "Recurso"),
    PostKind("EVENTO", "Evento")
)

private val networkVisibilities = listOf(
    "PUBLICA" to "Publica",
    "CONEXIONES" to "Conexiones",
    "UNIVERSIDAD" to "Universidad"
)

private val networkReactions = listOf(
    "ME_GUSTA" to "Me gusta",
    "APOYO" to "Apoyo",
    "CELEBRAR" to "Celebrar",
    "INTERESA" to "Me interesa"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetworkScreen(
    viewModel: NetworkViewModel,
    user: SessionUser,
    onNavigate: (String) -> Unit,
    onOpenProfile: (String) -> Unit
) {
    val state = viewModel.uiState
    val snackbarHostState = remember { SnackbarHostState() }
    var showComposer by remember { mutableStateOf(false) }
    var editingPost by remember { mutableStateOf<NetworkPostDto?>(null) }
    var deletingPost by remember { mutableStateOf<NetworkPostDto?>(null) }

    LaunchedEffect(Unit) {
        if (state.posts.isEmpty()) viewModel.refresh()
    }
    LaunchedEffect(state.infoMessage) {
        state.infoMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearFeedback()
        }
    }
    LaunchedEffect(state.errorMessage) {
        if (state.posts.isNotEmpty()) {
            state.errorMessage?.let {
                snackbarHostState.showSnackbar(it)
                viewModel.clearFeedback()
            }
        }
    }

    if (showComposer || editingPost != null) {
        NetworkComposerSheet(
            post = editingPost,
            isSubmitting = state.isSubmitting,
            onDismiss = {
                if (!state.isSubmitting) {
                    showComposer = false
                    editingPost = null
                }
            },
            onSubmit = { content, type, visibility, commentsEnabled ->
                viewModel.submitPost(
                    editingPostId = editingPost?.idPublicacion,
                    content = content,
                    type = type,
                    visibility = visibility,
                    commentsEnabled = commentsEnabled
                ) {
                    showComposer = false
                    editingPost = null
                }
            }
        )
    }

    state.activeCommentsPost?.let { post ->
        CommentsSheet(
            post = post,
            comments = state.comments,
            isLoading = state.isLoadingComments,
            isSubmitting = state.isSubmittingComment,
            errorMessage = state.commentsError,
            onDismiss = viewModel::closeComments,
            onRetry = { viewModel.openComments(post) },
            onSubmit = viewModel::submitComment,
            onUpdate = viewModel::updateComment,
            onDelete = viewModel::deleteComment,
            onOpenProfile = onOpenProfile
        )
    }

    deletingPost?.let { post ->
        AlertDialog(
            onDismissRequest = { deletingPost = null },
            title = { Text("Eliminar publicacion") },
            text = { Text("Esta publicacion dejara de aparecer en Network.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deletePost(post.idPublicacion)
                        deletingPost = null
                    }
                ) {
                    Text("Eliminar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingPost = null }) {
                    Text("Cancelar")
                }
            }
        )
    }

    Scaffold(
        containerColor = T4Background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            T4TopBar(
                title = "Network",
                subtitle = if (state.showingSaved) {
                    "Publicaciones guardadas"
                } else {
                    "Comunidad estudiantil"
                },
                actions = {
                    IconButton(onClick = viewModel::showSavedPosts) {
                        Icon(
                            imageVector = if (state.showingSaved) {
                                Icons.Filled.Bookmark
                            } else {
                                Icons.Filled.BookmarkBorder
                            },
                            contentDescription = "Publicaciones guardadas"
                        )
                    }
                    IconButton(
                        onClick = { viewModel.refresh(showLoading = false) },
                        enabled = !state.isLoading && !state.isRefreshing
                    ) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Actualizar")
                    }
                }
            )
        },
        bottomBar = {
            T4BottomBar(
                currentRoute = Routes.NETWORK,
                onNavigate = onNavigate,
                onReselect = { viewModel.refresh(showLoading = false) }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(bottom = 18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (state.isRefreshing) {
                item { LinearProgressIndicator(modifier = Modifier.fillMaxWidth()) }
            }

            item {
                NetworkHeader(
                    user = user,
                    onCreate = {
                        editingPost = null
                        showComposer = true
                    }
                )
            }

            if (!state.showingSaved) {
                item {
                    ScopeSelector(
                        selected = state.selectedScope,
                        onSelect = viewModel::selectScope
                    )
                }
            }

            when {
                state.isLoading -> item {
                    LoadingNetwork()
                }

                state.errorMessage != null && state.posts.isEmpty() -> item {
                    NetworkError(
                        message = state.errorMessage,
                        onRetry = { viewModel.refresh() }
                    )
                }

                state.posts.isEmpty() -> item {
                    EmptyNetwork(
                        saved = state.showingSaved,
                        onCreate = { showComposer = true }
                    )
                }

                else -> items(
                    items = state.posts,
                    key = { it.idPublicacion }
                ) { post ->
                    NetworkPostCard(
                        post = post,
                        originalPost = post.idPublicacionOrigen?.let { sourceId ->
                            state.posts.firstOrNull {
                                it.idPublicacion == sourceId
                            }
                        },
                        isBusy = post.idPublicacion in state.busyPostIds,
                        onOpenProfile = onOpenProfile,
                        onReact = { reaction -> viewModel.react(post, reaction) },
                        onComments = { viewModel.openComments(post) },
                        onShare = { viewModel.sharePost(post) },
                        onSave = { viewModel.toggleSaved(post) },
                        onEdit = { editingPost = post },
                        onDelete = { deletingPost = post }
                    )
                }
            }
        }
    }
}

@Composable
private fun NetworkHeader(
    user: SessionUser,
    onCreate: () -> Unit
) {
    T4LightPatternHeader(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp),
        shape = RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.Bottom
        ) {
            Text(
                text = "Hola, ${user.firstName}",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black,
                color = T4Text
            )
            Text(
                text = "Comparte ideas, avances y oportunidades con tu comunidad.",
                style = MaterialTheme.typography.bodyMedium,
                color = T4TextMuted
            )
            Spacer(modifier = Modifier.height(10.dp))
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onCreate),
                color = Color.White,
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, T4Border),
                shadowElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    NetworkAvatar(user.initials, compact = true)
                    Text(
                        text = "Comparte algo con el Network...",
                        modifier = Modifier.weight(1f),
                        color = T4TextMuted,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = null,
                        tint = T4Primary
                    )
                }
            }
        }
    }
}

@Composable
private fun ScopeSelector(
    selected: NetworkFeedScope,
    onSelect: (NetworkFeedScope) -> Unit
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(NetworkFeedScope.entries) { scope ->
            val active = selected == scope
            Surface(
                modifier = Modifier.clickable { onSelect(scope) },
                shape = RoundedCornerShape(8.dp),
                color = if (active) T4Text else T4Surface,
                border = BorderStroke(1.dp, if (active) T4Text else T4Border)
            ) {
                Text(
                    text = scope.label,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp),
                    color = if (active) T4Mint else T4TextMuted,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun NetworkPostCard(
    post: NetworkPostDto,
    originalPost: NetworkPostDto?,
    isBusy: Boolean,
    onOpenProfile: (String) -> Unit,
    onReact: (String) -> Unit,
    onComments: () -> Unit,
    onShare: () -> Unit,
    onSave: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    var showReactions by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = T4Surface),
        border = BorderStroke(1.dp, T4Border),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .background(postAccent(post.tipoPublicacion))
        )
        Column(modifier = Modifier.padding(top = 14.dp)) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp),
                verticalAlignment = Alignment.Top
            ) {
                NetworkAvatar(initials(post.autor), compact = false)
                Spacer(modifier = Modifier.width(10.dp))
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onOpenProfile(post.autor.nombreUsuario) }
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = post.autor.nombreCompleto,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = T4Text,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (post.autor.estudianteVerificado) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Filled.Verified,
                                contentDescription = "Estudiante verificado",
                                tint = T4Primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    Text(
                        text = "@${post.autor.nombreUsuario} · ${relativeTime(post.fechaPublicacion)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = T4TextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    post.autor.nombreUniversidad?.let { university ->
                        Text(
                            text = listOfNotNull(
                                university,
                                post.autor.nombreCarrera
                            ).joinToString(" · "),
                            style = MaterialTheme.typography.labelSmall,
                            color = T4TextMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                if (post.propia) {
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Filled.MoreVert, contentDescription = "Opciones")
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Editar") },
                                leadingIcon = { Icon(Icons.Filled.Edit, null) },
                                onClick = {
                                    showMenu = false
                                    onEdit()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Eliminar") },
                                leadingIcon = { Icon(Icons.Filled.DeleteOutline, null) },
                                onClick = {
                                    showMenu = false
                                    onDelete()
                                }
                            )
                        }
                    }
                }
            }

            Column(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    PostTypeChip(post.tipoPublicacion)
                    VisibilityChip(post.visibilidad)
                    if (post.fechaEdicion != null) {
                        Text(
                            text = "Editada",
                            style = MaterialTheme.typography.labelSmall,
                            color = T4TextMuted,
                            modifier = Modifier.align(Alignment.CenterVertically)
                        )
                    }
                }
                if (post.tipoPublicacion == "COMPARTIDA") {
                    Text(
                        text = "Compartio una publicacion de la comunidad",
                        style = MaterialTheme.typography.labelLarge,
                        color = T4Primary
                    )
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        color = T4SurfaceVariant,
                        border = BorderStroke(1.dp, T4Border)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = originalPost?.autor?.nombreCompleto
                                    ?: "Publicacion original",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = T4Text
                            )
                            Text(
                                text = originalPost?.contenido
                                    ?: "Abre el feed original para consultar el contenido.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = T4TextMuted,
                                maxLines = 4,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
                post.contenido?.takeIf { it.isNotBlank() }?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyLarge,
                        color = T4Text
                    )
                }
                if (post.totalReacciones + post.totalComentarios + post.totalCompartidas > 0) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${post.totalReacciones} reacciones",
                            style = MaterialTheme.typography.labelSmall,
                            color = T4TextMuted
                        )
                        Text(
                            text = "${post.totalComentarios} comentarios · ${post.totalCompartidas} compartidas",
                            style = MaterialTheme.typography.labelSmall,
                            color = T4TextMuted
                        )
                    }
                }
            }

            HorizontalDivider(color = T4Border)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box {
                    NetworkAction(
                        icon = if (post.miReaccion != null) {
                            Icons.Filled.Favorite
                        } else {
                            Icons.Filled.FavoriteBorder
                        },
                        label = reactionLabel(post.miReaccion),
                        selected = post.miReaccion != null,
                        enabled = !isBusy,
                        onClick = {
                            showReactions = true
                        }
                    )
                    DropdownMenu(
                        expanded = showReactions,
                        onDismissRequest = { showReactions = false }
                    ) {
                        networkReactions.forEach { (value, label) ->
                            DropdownMenuItem(
                                text = { Text(label) },
                                leadingIcon = {
                                    Icon(reactionIcon(value), contentDescription = null)
                                },
                                onClick = {
                                    showReactions = false
                                    onReact(value)
                                }
                            )
                        }
                    }
                }
                NetworkAction(
                    icon = Icons.Filled.ChatBubbleOutline,
                    label = "Comentar",
                    enabled = post.permiteComentarios,
                    onClick = onComments
                )
                NetworkAction(
                    icon = Icons.Filled.Share,
                    label = "Compartir",
                    enabled = !isBusy,
                    onClick = onShare
                )
                IconButton(onClick = onSave, enabled = !isBusy) {
                    Icon(
                        imageVector = if (post.guardada) {
                            Icons.Filled.Bookmark
                        } else {
                            Icons.Filled.BookmarkBorder
                        },
                        contentDescription = if (post.guardada) {
                            "Quitar de guardadas"
                        } else {
                            "Guardar"
                        },
                        tint = if (post.guardada) T4Primary else T4TextMuted
                    )
                }
            }
            if (isBusy) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun NetworkAction(
    icon: ImageVector,
    label: String,
    selected: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 7.dp, vertical = 5.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (selected) T4Primary else T4TextMuted,
            modifier = Modifier.size(19.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) T4Primary else T4TextMuted,
            maxLines = 1
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun NetworkComposerSheet(
    post: NetworkPostDto?,
    isSubmitting: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (String, String, String, Boolean) -> Unit
) {
    var content by remember(post?.idPublicacion) {
        mutableStateOf(post?.contenido.orEmpty())
    }
    var type by remember(post?.idPublicacion) {
        mutableStateOf(
            post?.tipoPublicacion?.takeUnless { it == "COMPARTIDA" } ?: "TEXTO"
        )
    }
    var visibility by remember(post?.idPublicacion) {
        mutableStateOf(post?.visibilidad ?: "PUBLICA")
    }
    var commentsEnabled by remember(post?.idPublicacion) {
        mutableStateOf(post?.permiteComentarios ?: true)
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = if (post == null) "Crear publicacion" else "Editar publicacion",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black,
                color = T4Text
            )
            Text(
                text = "Comparte algo util, una pregunta o un avance con otros estudiantes.",
                style = MaterialTheme.typography.bodyMedium,
                color = T4TextMuted
            )
            OutlinedTextField(
                value = content,
                onValueChange = { content = it.take(5000) },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 140.dp)
                    .keepVisibleAboveKeyboard(),
                label = { Text("¿Que quieres compartir?") },
                supportingText = { Text("${content.length}/5000") },
                minLines = 5,
                maxLines = 10
            )
            Text(
                text = "Tipo de publicacion",
                style = MaterialTheme.typography.labelLarge,
                color = T4Text
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                networkPostKinds.forEach { kind ->
                    SelectablePill(
                        label = kind.label,
                        selected = type == kind.value,
                        onClick = { type = kind.value }
                    )
                }
            }
            Text(
                text = "Quien puede verla",
                style = MaterialTheme.typography.labelLarge,
                color = T4Text
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                networkVisibilities.forEach { (value, label) ->
                    SelectablePill(
                        label = label,
                        selected = visibility == value,
                        onClick = { visibility = value }
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Permitir comentarios",
                        style = MaterialTheme.typography.labelLarge,
                        color = T4Text
                    )
                    Text(
                        text = "La comunidad podra responder a esta publicacion.",
                        style = MaterialTheme.typography.bodySmall,
                        color = T4TextMuted
                    )
                }
                Switch(
                    checked = commentsEnabled,
                    onCheckedChange = { commentsEnabled = it }
                )
            }
            Button(
                onClick = {
                    onSubmit(content, type, visibility, commentsEnabled)
                },
                enabled = content.isNotBlank() && !isSubmitting,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (post == null) "Publicar" else "Guardar cambios")
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun SelectablePill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = if (selected) T4Mint else T4SurfaceVariant,
        border = BorderStroke(
            1.dp,
            if (selected) T4MintDark else T4Border
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            if (selected) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = T4MintDark,
                    modifier = Modifier.size(16.dp)
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (selected) T4MintDark else T4TextMuted
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CommentsSheet(
    post: NetworkPostDto,
    comments: List<NetworkCommentDto>,
    isLoading: Boolean,
    isSubmitting: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onRetry: () -> Unit,
    onSubmit: (String, Int?, () -> Unit) -> Unit,
    onUpdate: (Int, String, () -> Unit) -> Unit,
    onDelete: (NetworkCommentDto) -> Unit,
    onOpenProfile: (String) -> Unit
) {
    var draft by remember(post.idPublicacion) { mutableStateOf("") }
    var replyingTo by remember(post.idPublicacion) {
        mutableStateOf<NetworkCommentDto?>(null)
    }
    var editing by remember(post.idPublicacion) {
        mutableStateOf<NetworkCommentDto?>(null)
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
                .navigationBarsPadding()
                .imePadding()
        ) {
            Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp)) {
                Text(
                    text = "Comentarios",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black,
                    color = T4Text
                )
                Text(
                    text = "${post.totalComentarios} respuestas en esta conversacion",
                    style = MaterialTheme.typography.bodySmall,
                    color = T4TextMuted
                )
            }
            HorizontalDivider(color = T4Border)

            when {
                isLoading -> Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }

                errorMessage != null && comments.isEmpty() -> Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(errorMessage, color = MaterialTheme.colorScheme.error)
                        TextButton(onClick = onRetry) { Text("Reintentar") }
                    }
                }

                comments.isEmpty() -> Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Filled.ChatBubbleOutline,
                            contentDescription = null,
                            tint = T4Primary,
                            modifier = Modifier.size(32.dp)
                        )
                        Text(
                            text = "Inicia la conversacion",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Se la primera persona en comentar.",
                            style = MaterialTheme.typography.bodySmall,
                            color = T4TextMuted
                        )
                    }
                }

                else -> LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(comments, key = { it.idComentario }) { comment ->
                        CommentRow(
                            comment = comment,
                            onReply = {
                                replyingTo = comment
                                editing = null
                                draft = ""
                            },
                            onEdit = {
                                editing = comment
                                replyingTo = null
                                draft = comment.contenido
                            },
                            onDelete = { onDelete(comment) },
                            onOpenProfile = onOpenProfile
                        )
                    }
                }
            }

            errorMessage?.takeIf { comments.isNotEmpty() }?.let {
                Text(
                    text = it,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
            (editing ?: replyingTo)?.let { selectedComment ->
                Surface(color = T4PrimaryContainer) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (editing != null) {
                                "Editando tu comentario"
                            } else {
                                "Respondiendo a @${selectedComment.autor.nombreUsuario}"
                            },
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.labelMedium,
                            color = T4Primary
                        )
                        TextButton(
                            onClick = {
                                editing = null
                                replyingTo = null
                                draft = ""
                            }
                        ) {
                            Text("Cancelar")
                        }
                    }
                }
            }
            Surface(color = T4Surface, shadowElevation = 6.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = draft,
                        onValueChange = { draft = it.take(2000) },
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(max = 130.dp)
                            .keepVisibleAboveKeyboard(),
                        placeholder = { Text("Escribe un comentario...") },
                        minLines = 1,
                        maxLines = 4
                    )
                    IconButton(
                        onClick = {
                            val editingComment = editing
                            if (editingComment != null) {
                                onUpdate(editingComment.idComentario, draft) {
                                    draft = ""
                                    editing = null
                                }
                            } else {
                                onSubmit(draft, replyingTo?.idComentario) {
                                    draft = ""
                                    replyingTo = null
                                }
                            }
                        },
                        enabled = draft.isNotBlank() && !isSubmitting,
                        modifier = Modifier
                            .size(48.dp)
                            .background(T4Primary, CircleShape)
                    ) {
                        if (isSubmitting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Enviar comentario",
                                tint = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CommentRow(
    comment: NetworkCommentDto,
    onReply: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onOpenProfile: (String) -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = if (comment.idComentarioPadre != null) 28.dp else 0.dp),
        verticalAlignment = Alignment.Top
    ) {
        NetworkAvatar(initials(comment.autor), compact = true)
        Spacer(modifier = Modifier.width(9.dp))
        Column(modifier = Modifier.weight(1f)) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = T4SurfaceVariant
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = comment.autor.nombreCompleto,
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    onOpenProfile(comment.autor.nombreUsuario)
                                },
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = T4Text
                        )
                        if (comment.propio) {
                            Box {
                                IconButton(
                                    onClick = { showMenu = true },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        Icons.Filled.MoreVert,
                                        contentDescription = "Opciones del comentario",
                                        modifier = Modifier.size(17.dp)
                                    )
                                }
                                DropdownMenu(
                                    expanded = showMenu,
                                    onDismissRequest = { showMenu = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Editar") },
                                        onClick = {
                                            showMenu = false
                                            onEdit()
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Eliminar") },
                                        onClick = {
                                            showMenu = false
                                            onDelete()
                                        }
                                    )
                                }
                            }
                        }
                    }
                    Text(
                        text = comment.contenido,
                        style = MaterialTheme.typography.bodyMedium,
                        color = T4Text
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = relativeTime(comment.fechaComentario),
                    style = MaterialTheme.typography.labelSmall,
                    color = T4TextMuted
                )
                TextButton(onClick = onReply) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Reply,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("Responder", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
private fun NetworkAvatar(initials: String, compact: Boolean) {
    val size = if (compact) 38.dp else 48.dp
    Box(
        modifier = Modifier
            .size(size)
            .background(T4Primary, RoundedCornerShape(if (compact) 10.dp else 13.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initials,
            color = Color.White,
            fontWeight = FontWeight.Black,
            style = if (compact) {
                MaterialTheme.typography.labelLarge
            } else {
                MaterialTheme.typography.titleMedium
            }
        )
    }
}

@Composable
private fun PostTypeChip(type: String) {
    val color = postAccent(type)
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = color.copy(alpha = 0.14f)
    ) {
        Text(
            text = networkPostKinds.firstOrNull { it.value == type }?.label
                ?: if (type == "COMPARTIDA") "Compartida" else type.lowercase()
                    .replaceFirstChar { it.uppercase() },
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@Composable
private fun VisibilityChip(visibility: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Icon(
            imageVector = if (visibility == "UNIVERSIDAD") {
                Icons.Filled.School
            } else {
                Icons.Filled.Public
            },
            contentDescription = null,
            tint = T4TextMuted,
            modifier = Modifier.size(14.dp)
        )
        Text(
            text = networkVisibilities.firstOrNull { it.first == visibility }?.second
                ?: visibility,
            style = MaterialTheme.typography.labelSmall,
            color = T4TextMuted
        )
    }
}

@Composable
private fun LoadingNetwork() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 54.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        CircularProgressIndicator()
        Text("Cargando publicaciones...", color = T4TextMuted)
    }
}

@Composable
private fun NetworkError(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 44.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "No pudimos abrir Network",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = T4TextMuted
        )
        OutlinedButton(onClick = onRetry) {
            Icon(Icons.Filled.Refresh, contentDescription = null)
            Spacer(modifier = Modifier.width(6.dp))
            Text("Reintentar")
        }
    }
}

@Composable
private fun EmptyNetwork(saved: Boolean, onCreate: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 42.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            imageVector = if (saved) Icons.Filled.BookmarkBorder else Icons.Filled.Interests,
            contentDescription = null,
            tint = T4Primary,
            modifier = Modifier.size(38.dp)
        )
        Text(
            text = if (saved) "Aun no guardas publicaciones" else "Todavia no hay publicaciones",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = if (saved) {
                "Usa el marcador de una publicacion para encontrarla aqui."
            } else {
                "Inicia la conversacion con tu comunidad estudiantil."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = T4TextMuted
        )
        if (!saved) {
            Button(onClick = onCreate, shape = RoundedCornerShape(8.dp)) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Crear publicacion")
            }
        }
    }
}

private fun initials(identity: PublicIdentityDto): String {
    return identity.nombreCompleto
        .split(" ")
        .mapNotNull { it.firstOrNull()?.uppercase() }
        .take(2)
        .joinToString("")
        .ifBlank { "TK" }
}

private fun postAccent(type: String): Color = when (type) {
    "PREGUNTA" -> Color(0xFF1669B2)
    "PROYECTO" -> T4MintDark
    "LOGRO" -> Color(0xFF9A5A00)
    "RECURSO" -> Color(0xFF7B3FA0)
    "EVENTO" -> Color(0xFFB33B63)
    "COMPARTIDA" -> Color(0xFF00796B)
    else -> T4Primary
}

private fun reactionIcon(reaction: String): ImageVector = when (reaction) {
    "CELEBRAR" -> Icons.Filled.Celebration
    "INTERESA" -> Icons.Filled.Interests
    "APOYO" -> Icons.Filled.Verified
    else -> Icons.Filled.Favorite
}

private fun reactionLabel(reaction: String?): String {
    return networkReactions.firstOrNull { it.first == reaction }?.second ?: "Reaccionar"
}

private fun relativeTime(value: String): String {
    val date = parseApiDateTime(value) ?: return "Ahora"
    val elapsed = (Date().time - date.time).coerceAtLeast(0)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(elapsed)
    val hours = TimeUnit.MILLISECONDS.toHours(elapsed)
    val days = TimeUnit.MILLISECONDS.toDays(elapsed)
    return when {
        minutes < 1 -> "Ahora"
        minutes < 60 -> "Hace ${minutes} min"
        hours < 24 -> "Hace ${hours} h"
        days < 7 -> "Hace ${days} d"
        else -> com.t4kash.app.ui.formatApiDateTime(value)
    }
}
