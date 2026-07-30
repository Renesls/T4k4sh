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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.t4kash.app.ui.components.ConnectionErrorState
import com.t4kash.app.ui.components.EmptyState
import com.t4kash.app.ui.components.T4BottomBar
import com.t4kash.app.ui.components.T4PatternSurface
import com.t4kash.app.ui.components.T4TopBar
import com.t4kash.app.ui.components.isSoftwareKeyboardVisible
import com.t4kash.app.ui.components.keepVisibleAboveKeyboard
import com.t4kash.app.ui.formatApiDateTime
import com.t4kash.app.ui.formatDaySeparator
import com.t4kash.app.ui.isSameApiDay
import com.t4kash.app.ui.model.ConversationDto
import com.t4kash.app.ui.model.MessageDto
import com.t4kash.app.ui.model.NotificationDto
import com.t4kash.app.ui.navigation.Routes
import com.t4kash.app.ui.theme.T4Background
import com.t4kash.app.ui.theme.T4Border
import com.t4kash.app.ui.theme.T4BrandDark
import com.t4kash.app.ui.theme.T4Mint
import com.t4kash.app.ui.theme.T4MintDark
import com.t4kash.app.ui.theme.T4Primary
import com.t4kash.app.ui.theme.T4PrimaryContainer
import com.t4kash.app.ui.theme.T4Surface
import com.t4kash.app.ui.theme.T4SurfaceVariant
import com.t4kash.app.ui.theme.T4Text
import com.t4kash.app.ui.theme.T4TextMuted
import com.t4kash.app.ui.viewmodel.CommunicationViewModel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@Composable
fun ChatScreen(
    viewModel: CommunicationViewModel = viewModel(),
    onNavigate: (String) -> Unit,
    onOpenConversation: (Int) -> Unit,
    onOpenNotifications: () -> Unit
) {
    val state = viewModel.uiState
    val keyboardVisible = isSoftwareKeyboardVisible()
    var query by remember { mutableStateOf("") }
    val conversations = remember(state.conversations, query) {
        state.conversations.filter {
            query.isBlank() ||
                it.nombreParticipante.contains(query, ignoreCase = true) ||
                it.tituloTarea.contains(query, ignoreCase = true)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.refreshOverview()
    }

    Scaffold(
        containerColor = T4Background,
        topBar = {
            T4TopBar(
                title = "Mensajes",
                subtitle = "Conversaciones de trabajo",
                actions = {
                    NotificationButton(
                        unreadCount = state.unreadNotifications,
                        onClick = onOpenNotifications
                    )
                }
            )
        },
        bottomBar = {
            if (!keyboardVisible) {
                T4BottomBar(
                    currentRoute = Routes.CHAT,
                    onNavigate = onNavigate,
                    onReselect = { viewModel.refreshOverview() }
                )
            }
        }
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = state.isLoadingOverview,
            onRefresh = viewModel::refreshOverview,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    horizontal = 16.dp,
                    vertical = 16.dp
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    T4PatternSurface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(128.dp),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(18.dp),
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "Tus trabajos, en una conversacion.",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (state.unreadMessages > 0) {
                                    "${state.unreadMessages} mensajes esperan tu respuesta"
                                } else {
                                    "Todo al dia"
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = T4PrimaryContainer
                            )
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .keepVisibleAboveKeyboard(),
                        singleLine = true,
                        label = { Text("Buscar conversacion") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Filled.Search,
                                contentDescription = null
                            )
                        }
                    )
                }

                if (
                    state.overviewError != null &&
                    state.conversations.isEmpty()
                ) {
                    item {
                        ConnectionErrorState(
                            message = state.overviewError,
                            onRetry = viewModel::refreshOverview
                        )
                    }
                } else if (
                    state.isLoadingOverview &&
                    state.conversations.isEmpty()
                ) {
                    item {
                        LoadingBlock()
                    }
                } else if (conversations.isEmpty()) {
                    item {
                        EmptyState(
                            title = if (query.isBlank()) {
                                "Aun no hay conversaciones"
                            } else {
                                "Sin resultados"
                            },
                            message = if (query.isBlank()) {
                                "El chat se habilita al aceptar una postulacion."
                            } else {
                                "Prueba con el nombre o la oportunidad."
                            }
                        )
                    }
                } else {
                    items(
                        items = conversations,
                        key = { it.idConversacion }
                    ) { conversation ->
                        ConversationCard(
                            conversation = conversation,
                            onClick = {
                                onOpenConversation(
                                    conversation.idConversacion
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ConversationScreen(
    conversationId: Int,
    viewModel: CommunicationViewModel = viewModel(),
    onBack: () -> Unit
) {
    val state = viewModel.uiState
    val conversation = state.conversations.firstOrNull {
        it.idConversacion == conversationId
    }
    val messages = state.messages.filter {
        it.idConversacion == conversationId
    }
    val chatItems = remember(messages) { buildChatItems(messages) }
    val listState = rememberLazyListState()
    var draft by remember(conversationId) { mutableStateOf("") }
    var stickToBottom by remember(conversationId) { mutableStateOf(true) }

    LaunchedEffect(conversationId) {
        viewModel.loadMessages(conversationId)
        while (currentCoroutineContext().isActive) {
            delay(5_000)
            viewModel.loadMessages(conversationId, silent = true)
        }
    }
    LaunchedEffect(listState) {
        snapshotFlow {
            listState.isScrollInProgress to isNearBottom(listState)
        }.collect { (scrolling, nearBottom) ->
            if (scrolling) {
                stickToBottom = nearBottom
            }
        }
    }
    LaunchedEffect(chatItems.size) {
        if (chatItems.isNotEmpty() && stickToBottom) {
            listState.animateScrollToItem(chatItems.lastIndex)
        }
    }
    LaunchedEffect(state.sentMessageId) {
        if (state.sentMessageId != null) {
            draft = ""
            stickToBottom = true
            if (chatItems.isNotEmpty()) {
                listState.animateScrollToItem(chatItems.lastIndex)
            }
            viewModel.clearSendFeedback()
        }
    }

    Scaffold(
        modifier = Modifier.imePadding(),
        containerColor = T4Background,
        topBar = {
            T4TopBar(
                title = conversation?.nombreParticipante ?: "Conversacion",
                subtitle = conversation?.tituloTarea,
                onBack = onBack
            )
        },
        bottomBar = {
            MessageComposer(
                value = draft,
                onValueChange = {
                    if (it.length <= 2000) draft = it
                },
                isSending = state.isSending,
                onSend = {
                    viewModel.sendMessage(conversationId, draft)
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(
                horizontal = 14.dp,
                vertical = 16.dp
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (state.isLoadingMessages && messages.isEmpty()) {
                item { LoadingBlock() }
            } else if (state.messageError != null && messages.isEmpty()) {
                item {
                    ConnectionErrorState(
                        message = state.messageError,
                        onRetry = {
                            viewModel.loadMessages(conversationId)
                        }
                    )
                }
            } else if (messages.isEmpty()) {
                item {
                    EmptyState(
                        title = "Inicia la conversacion",
                        message = "Escribe el primer mensaje sobre este trabajo."
                    )
                }
            } else {
                itemsIndexed(
                    items = chatItems,
                    key = { index, chatItem ->
                        when (chatItem) {
                            is ChatListItem.DateHeader -> "date-header-$index"
                            is ChatListItem.MessageItem -> chatItem.message.idMensaje
                        }
                    }
                ) { _, chatItem ->
                    when (chatItem) {
                        is ChatListItem.DateHeader -> DateSeparator(chatItem.label)
                        is ChatListItem.MessageItem -> MessageBubble(message = chatItem.message)
                    }
                }
            }

            if (state.messageError != null && messages.isNotEmpty()) {
                item {
                    Text(
                        text = state.messageError,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun NotificationsScreen(
    viewModel: CommunicationViewModel = viewModel(),
    onBack: () -> Unit
) {
    val state = viewModel.uiState

    LaunchedEffect(Unit) {
        viewModel.refreshOverview()
    }

    Scaffold(
        containerColor = T4Background,
        topBar = {
            T4TopBar(
                title = "Notificaciones",
                subtitle = "${state.unreadNotifications} sin leer",
                onBack = onBack,
                actions = {
                    if (state.unreadNotifications > 0) {
                        TextButton(
                            onClick = viewModel::markAllNotificationsRead
                        ) {
                            Text("Leer todas")
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = state.isLoadingOverview,
            onRefresh = viewModel::refreshOverview,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    horizontal = 16.dp,
                    vertical = 16.dp
                ),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (
                    state.overviewError != null &&
                    state.notifications.isEmpty()
                ) {
                    item {
                        ConnectionErrorState(
                            message = state.overviewError,
                            onRetry = viewModel::refreshOverview
                        )
                    }
                } else if (
                    state.isLoadingOverview &&
                    state.notifications.isEmpty()
                ) {
                    item { LoadingBlock() }
                } else if (state.notifications.isEmpty()) {
                    item {
                        EmptyState(
                            title = "Sin notificaciones",
                            message = "Tu actividad importante aparecera aqui."
                        )
                    }
                } else {
                    items(
                        items = state.notifications,
                        key = { it.idNotificacion }
                    ) { notification ->
                        NotificationCard(
                            notification = notification,
                            onClick = {
                                viewModel.markNotificationRead(
                                    notification.idNotificacion
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

private sealed interface ChatListItem {
    data class DateHeader(val label: String) : ChatListItem
    data class MessageItem(val message: MessageDto) : ChatListItem
}

private fun buildChatItems(messages: List<MessageDto>): List<ChatListItem> {
    val items = mutableListOf<ChatListItem>()
    var previous: MessageDto? = null
    for (message in messages) {
        if (previous == null || !isSameApiDay(previous.fechaEnvio, message.fechaEnvio)) {
            items += ChatListItem.DateHeader(formatDaySeparator(message.fechaEnvio))
        }
        items += ChatListItem.MessageItem(message)
        previous = message
    }
    return items
}

private const val NEAR_BOTTOM_SLOP_PX = 24

private fun isNearBottom(listState: LazyListState): Boolean {
    val info = listState.layoutInfo
    val last = info.visibleItemsInfo.lastOrNull() ?: return true
    return last.index >= info.totalItemsCount - 1 &&
        last.offset + last.size <= info.viewportEndOffset + NEAR_BOTTOM_SLOP_PX
}

@Composable
private fun DateSeparator(label: String) {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = T4SurfaceVariant
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = T4TextMuted,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
private fun LoadingBlock() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(color = T4Primary)
    }
}

@Composable
private fun ConversationCard(
    conversation: ConversationDto,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = T4Surface),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (conversation.mensajesNoLeidos > 0) 3.dp else 1.dp
        ),
        border = BorderStroke(
            1.dp,
            if (conversation.mensajesNoLeidos > 0) T4Mint else T4Border
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(46.dp),
                shape = CircleShape,
                color = if (conversation.mensajesNoLeidos > 0) {
                    T4Mint
                } else {
                    T4SurfaceVariant
                }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = initials(conversation.nombreParticipante),
                        fontWeight = FontWeight.Bold,
                        color = T4BrandDark
                    )
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = conversation.nombreParticipante,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = T4Text,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = formatApiDateTime(
                            conversation.fechaUltimoMensaje,
                            "dd/MM HH:mm"
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = T4TextMuted
                    )
                }
                Text(
                    text = conversation.tituloTarea,
                    style = MaterialTheme.typography.labelMedium,
                    color = T4Primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = conversation.ultimoMensaje
                            ?: "Conversacion disponible",
                        style = MaterialTheme.typography.bodySmall,
                        color = T4TextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    if (conversation.mensajesNoLeidos > 0) {
                        Badge(
                            containerColor = T4Primary,
                            contentColor = Color.White
                        ) {
                            Text(
                                conversation.mensajesNoLeidos
                                    .coerceAtMost(99)
                                    .toString()
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MessageBubble(message: MessageDto) {
    val maxBubbleWidth = (LocalConfiguration.current.screenWidthDp * 0.78f).dp
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.propio) {
            Arrangement.End
        } else {
            Arrangement.Start
        }
    ) {
        Surface(
            modifier = Modifier.widthIn(max = maxBubbleWidth),
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (message.propio) 16.dp else 4.dp,
                bottomEnd = if (message.propio) 4.dp else 16.dp
            ),
            color = if (message.propio) T4Primary else T4Surface,
            shadowElevation = 1.dp,
            border = if (message.propio) {
                null
            } else {
                BorderStroke(1.dp, T4Border)
            }
        ) {
            Column(
                modifier = Modifier.padding(
                    horizontal = 13.dp,
                    vertical = 9.dp
                )
            ) {
                Text(
                    text = message.contenido,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (message.propio) Color.White else T4Text
                )
                Row(
                    modifier = Modifier.align(Alignment.End),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formatApiDateTime(
                            message.fechaEnvio,
                            "HH:mm"
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (message.propio) {
                            Color.White.copy(alpha = 0.74f)
                        } else {
                            T4TextMuted
                        }
                    )
                    if (message.propio) {
                        Icon(
                            imageVector = Icons.Filled.DoneAll,
                            contentDescription = if (message.leido) {
                                "Leido"
                            } else {
                                "Enviado"
                            },
                            modifier = Modifier.size(14.dp),
                            tint = if (message.leido) {
                                T4Mint
                            } else {
                                Color.White.copy(alpha = 0.68f)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MessageComposer(
    value: String,
    onValueChange: (String) -> Unit,
    isSending: Boolean,
    onSend: () -> Unit
) {
    val canSend = value.isNotBlank() && !isSending
    Surface(
        color = T4Surface,
        shadowElevation = 10.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier
                    .weight(1f)
                    .keepVisibleAboveKeyboard(),
                placeholder = { Text("Escribe un mensaje") },
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = Color.Transparent,
                    focusedBorderColor = T4Primary,
                    unfocusedContainerColor = T4SurfaceVariant,
                    focusedContainerColor = T4SurfaceVariant
                ),
                minLines = 1,
                maxLines = 4
            )
            Surface(
                modifier = Modifier
                    .size(46.dp)
                    .clickable(enabled = canSend, onClick = onSend),
                shape = CircleShape,
                color = if (canSend) T4Primary else T4SurfaceVariant
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (isSending) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = Color.White
                        )
                    } else {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Enviar mensaje",
                            tint = if (canSend) Color.White else T4TextMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationCard(
    notification: NotificationDto,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (notification.leida) {
                T4Surface
            } else {
                T4Mint.copy(alpha = 0.22f)
            }
        ),
        border = BorderStroke(
            1.dp,
            if (notification.leida) T4Border else T4Mint
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = CircleShape,
                color = if (notification.leida) {
                    T4SurfaceVariant
                } else {
                    T4Mint
                }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Filled.Notifications,
                        contentDescription = null,
                        tint = T4MintDark,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = notification.titulo,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = if (notification.leida) {
                            FontWeight.Medium
                        } else {
                            FontWeight.Bold
                        },
                        color = T4Text,
                        modifier = Modifier.weight(1f)
                    )
                    if (!notification.leida) {
                        Box(
                            modifier = Modifier
                                .padding(start = 8.dp, top = 5.dp)
                                .size(8.dp)
                                .background(T4Primary, CircleShape)
                        )
                    }
                }
                Text(
                    text = notification.mensaje,
                    style = MaterialTheme.typography.bodySmall,
                    color = T4TextMuted
                )
                HorizontalDivider(color = T4Border)
                Text(
                    text = formatApiDateTime(
                        notification.fechaCreacion,
                        "dd/MM/yyyy HH:mm"
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = T4TextMuted
                )
            }
        }
    }
}

@Composable
private fun NotificationButton(
    unreadCount: Int,
    onClick: () -> Unit
) {
    IconButton(onClick = onClick) {
        BadgedBox(
            badge = {
                if (unreadCount > 0) {
                    Badge {
                        Text(unreadCount.coerceAtMost(99).toString())
                    }
                }
            }
        ) {
            Icon(
                imageVector = Icons.Filled.Notifications,
                contentDescription = "Notificaciones"
            )
        }
    }
}

private fun initials(name: String): String {
    return name.trim()
        .split(" ")
        .mapNotNull { it.firstOrNull()?.uppercase() }
        .take(2)
        .joinToString("")
        .ifBlank { "TK" }
}
