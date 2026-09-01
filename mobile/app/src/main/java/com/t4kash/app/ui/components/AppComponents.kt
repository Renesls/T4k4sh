package com.t4kash.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddBox
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.t4kash.app.R
import com.t4kash.app.ui.navigation.Routes
import com.t4kash.app.ui.theme.T4Background
import com.t4kash.app.ui.theme.T4BrandDark
import com.t4kash.app.ui.theme.T4Border
import com.t4kash.app.ui.theme.T4Mint
import com.t4kash.app.ui.theme.T4MintDark
import com.t4kash.app.ui.theme.T4Primary
import com.t4kash.app.ui.theme.T4PrimaryContainer
import com.t4kash.app.ui.theme.T4Surface
import com.t4kash.app.ui.theme.T4SurfaceVariant
import com.t4kash.app.ui.theme.T4Text
import com.t4kash.app.ui.theme.T4TextMuted

data class BottomDestination(
    val route: String,
    val label: String,
    val icon: ImageVector
)

val T4BottomDestinations = listOf(
    BottomDestination(Routes.MARKETPLACE, "Inicio", Icons.Filled.Home),
    BottomDestination(Routes.NETWORK, "Network", Icons.Filled.Group),
    BottomDestination(Routes.POST, "Post", Icons.Filled.AddBox),
    BottomDestination(Routes.CHAT, "Chat", Icons.Filled.ChatBubble),
    BottomDestination(Routes.PROFILE, "Perfil", Icons.Filled.Person)
)

data class T4CategoryColors(
    val container: Color,
    val content: Color
)

private val T4CategoryPalette = listOf(
    T4CategoryColors(Color(0xFFE4E9FF), Color(0xFF263FA9)),
    T4CategoryColors(Color(0xFFDDF6F3), Color(0xFF006B62)),
    T4CategoryColors(Color(0xFFE5FFBE), Color(0xFF314600)),
    T4CategoryColors(Color(0xFFFFE8D1), Color(0xFF874500)),
    T4CategoryColors(Color(0xFFF0E7FF), Color(0xFF6531A8)),
    T4CategoryColors(Color(0xFFFFE5EA), Color(0xFF9F1239)),
    T4CategoryColors(Color(0xFFFFF4C2), Color(0xFF6F5B00)),
    T4CategoryColors(Color(0xFFE7EDF1), Color(0xFF344054)),
    T4CategoryColors(Color(0xFFE2F4D7), Color(0xFF315C1B)),
    T4CategoryColors(Color(0xFFDFF1FF), Color(0xFF1D4E89)),
    T4CategoryColors(Color(0xFFFBE8D9), Color(0xFF744018)),
    T4CategoryColors(Color(0xFFF8E2F1), Color(0xFF84285E))
)

fun t4CategoryColors(categoryId: Int): T4CategoryColors {
    val index = Math.floorMod(categoryId - 1, T4CategoryPalette.size)
    return T4CategoryPalette[index]
}

@Composable
fun T4BrandMark(
    modifier: Modifier = Modifier,
    showName: Boolean = true,
    tint: Color = T4Primary
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Surface(
            modifier = Modifier
                .size(38.dp),
            shape = RoundedCornerShape(10.dp),
            color = Color.White,
            shadowElevation = 2.dp,
            border = BorderStroke(1.dp, T4Border)
        ) {
            Image(
                painter = painterResource(R.drawable.t4kash_logo),
                contentDescription = "Logotipo de T4KASH",
                modifier = Modifier.fillMaxWidth(),
                contentScale = ContentScale.Crop
            )
        }
        if (showName) {
            Text(
                text = "T4KASH",
                color = tint,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun T4TopBar(
    title: String = "T4KASH",
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    TopAppBar(
        navigationIcon = {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Volver"
                    )
                }
            }
        },
        title = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(R.drawable.t4kash_logo),
                    contentDescription = "Isotipo de T4KASH",
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(6.dp)),
                    contentScale = ContentScale.Crop
                )
                Column {
                    Text(
                        text = title,
                        color = T4Primary,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (subtitle != null) {
                        Text(
                            text = subtitle,
                            color = T4TextMuted,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        },
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = T4Background,
            titleContentColor = T4Primary,
            navigationIconContentColor = T4Primary,
            actionIconContentColor = T4Primary
        )
    )
}

@Composable
fun T4BottomBar(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    onReselect: (String) -> Unit = {}
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
        color = T4Surface,
        shadowElevation = 10.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            T4BottomDestinations.forEach { destination ->
                val selected = currentRoute == destination.route
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            if (selected) {
                                onReselect(destination.route)
                            } else {
                                onNavigate(destination.route)
                            }
                        }
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .width(22.dp)
                            .height(3.dp)
                            .clip(CircleShape)
                            .background(if (selected) T4Primary else Color.Transparent)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Icon(
                        imageVector = destination.icon,
                        contentDescription = destination.label,
                        tint = if (selected) T4Primary else T4TextMuted,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = destination.label,
                        color = if (selected) T4Primary else T4TextMuted,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun T4LightPatternHeader(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp),
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .clip(shape)
            .background(T4Surface)
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            drawLightT4Pattern()
        }
        content()
    }
}

private fun DrawScope.drawLightT4Pattern() {
    val lavender = Color(0xFFD9D4FF)
    val navy = Color(0xFF10173D)

    drawRect(
        color = T4Primary,
        topLeft = Offset(0f, 0f),
        size = Size(size.width * 0.23f, size.height * 0.36f)
    )
    drawCircle(
        color = T4Mint,
        radius = size.minDimension * 0.095f,
        center = Offset(size.width * 0.11f, size.height * 0.37f)
    )
    drawRect(
        color = T4Mint,
        topLeft = Offset(0f, size.height * 0.52f),
        size = Size(size.width * 0.22f, size.height * 0.10f)
    )
    drawTriangle(
        color = navy,
        first = Offset(0f, size.height * 0.65f),
        second = Offset(size.width * 0.22f, size.height),
        third = Offset(0f, size.height)
    )
    drawCircle(
        color = lavender,
        radius = size.minDimension * 0.15f,
        center = Offset(size.width * 0.29f, size.height * 0.80f)
    )
    drawRoundRect(
        color = T4Mint,
        topLeft = Offset(size.width * 0.76f, size.height * 0.30f),
        size = Size(size.width * 0.16f, size.height * 0.43f),
        cornerRadius = CornerRadius(size.width * 0.08f)
    )
    drawRect(
        color = T4Primary,
        topLeft = Offset(size.width * 0.84f, size.height * 0.70f),
        size = Size(size.width * 0.16f, size.height * 0.22f)
    )
    drawTriangle(
        color = navy,
        first = Offset(size.width * 0.79f, 0f),
        second = Offset(size.width, 0f),
        third = Offset(size.width, size.height * 0.28f)
    )
    drawTriangle(
        color = lavender,
        first = Offset(size.width * 0.84f, 0f),
        second = Offset(size.width, 0f),
        third = Offset(size.width, size.height * 0.19f)
    )
    drawCircle(
        color = T4Mint,
        radius = size.minDimension * 0.055f,
        center = Offset(size.width * 0.95f, size.height * 0.69f)
    )
}

@Composable
fun T4PatternSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(20.dp),
    patternAlpha: Float = 0.72f,
    overlayAlpha: Float = 0.58f,
    animated: Boolean = false,
    content: @Composable BoxScope.() -> Unit
) {
    val motion = if (animated) {
        rememberInfiniteTransition(label = "T4PatternMotion")
            .animateFloat(
                initialValue = -1f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(
                        durationMillis = 4200,
                        easing = LinearEasing
                    ),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "T4PatternOffset"
            )
            .value
    } else {
        0f
    }
    val overlay = if (animated) {
        Brush.verticalGradient(
            listOf(
                T4BrandDark.copy(alpha = overlayAlpha),
                T4BrandDark.copy(alpha = (overlayAlpha - 0.10f).coerceAtLeast(0f)),
                T4BrandDark.copy(alpha = overlayAlpha)
            )
        )
    } else {
        Brush.horizontalGradient(
            listOf(
                T4BrandDark.copy(alpha = 0.92f),
                T4BrandDark.copy(alpha = overlayAlpha),
                T4BrandDark.copy(alpha = (overlayAlpha - 0.18f).coerceAtLeast(0f))
            )
        )
    }

    Box(
        modifier = modifier
            .clip(shape)
            .background(T4BrandDark)
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val horizontalMotion = motion * size.width * 0.035f
            val verticalMotion = motion * size.height * 0.025f
            if (animated) {
                drawExpandedT4Pattern(
                    horizontalMotion = horizontalMotion,
                    verticalMotion = verticalMotion,
                    alpha = patternAlpha
                )
            } else {
                drawCompactT4Pattern(alpha = patternAlpha)
            }
        }
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(overlay)
        )
        content()
    }
}

private fun DrawScope.drawCompactT4Pattern(alpha: Float) {
    val purple = T4Primary.copy(alpha = alpha)
    val mint = T4Mint.copy(alpha = alpha)
    val navy = Color(0xFF20228F).copy(alpha = alpha)

    drawRect(
        color = navy,
        topLeft = Offset(size.width * 0.58f, 0f),
        size = Size(size.width * 0.25f, size.height * 0.48f)
    )
    drawRoundRect(
        color = mint,
        topLeft = Offset(size.width * 0.76f, -size.height * 0.24f),
        size = Size(size.width * 0.18f, size.height * 0.92f),
        cornerRadius = CornerRadius(size.width * 0.09f)
    )
    drawTriangle(
        color = purple,
        first = Offset(size.width * 0.70f, size.height),
        second = Offset(size.width, size.height * 0.28f),
        third = Offset(size.width, size.height)
    )
    drawCircle(
        color = mint,
        radius = size.minDimension * 0.16f,
        center = Offset(size.width * 0.96f, size.height * 0.72f)
    )
    drawCircle(
        color = purple.copy(alpha = alpha * 0.45f),
        radius = size.minDimension * 0.25f,
        center = Offset(size.width * 0.12f, -size.height * 0.08f)
    )
}

private fun DrawScope.drawExpandedT4Pattern(
    horizontalMotion: Float,
    verticalMotion: Float,
    alpha: Float
) {
    val purple = T4Primary.copy(alpha = alpha)
    val mint = T4Mint.copy(alpha = alpha)
    val navy = Color(0xFF20228F).copy(alpha = alpha)

    drawRoundRect(
        color = purple,
        topLeft = Offset(
            -size.width * 0.12f + horizontalMotion,
            -size.height * 0.04f + verticalMotion
        ),
        size = Size(size.width * 0.42f, size.height * 0.20f),
        cornerRadius = CornerRadius(size.width * 0.10f)
    )
    drawRect(
        color = navy,
        topLeft = Offset(size.width * 0.26f - horizontalMotion, 0f),
        size = Size(size.width * 0.34f, size.height * 0.19f)
    )
    drawRoundRect(
        color = mint,
        topLeft = Offset(
            size.width * 0.61f + horizontalMotion,
            -size.height * 0.07f
        ),
        size = Size(size.width * 0.22f, size.height * 0.36f),
        cornerRadius = CornerRadius(size.width * 0.11f)
    )
    drawRoundRect(
        color = purple,
        topLeft = Offset(
            -size.width * 0.15f - horizontalMotion,
            size.height * 0.31f
        ),
        size = Size(size.width * 0.45f, size.height * 0.27f),
        cornerRadius = CornerRadius(size.width * 0.13f)
    )
    drawTriangle(
        color = mint,
        first = Offset(size.width * 0.42f, size.height * 0.35f + verticalMotion),
        second = Offset(size.width * 0.66f, size.height * 0.59f + verticalMotion),
        third = Offset(size.width * 0.42f, size.height * 0.59f + verticalMotion)
    )
    drawCircle(
        color = navy,
        radius = size.width * 0.16f,
        center = Offset(
            size.width * 0.34f + horizontalMotion,
            size.height * 0.74f
        )
    )
    drawTriangle(
        color = purple,
        first = Offset(size.width * 0.69f - horizontalMotion, size.height),
        second = Offset(size.width, size.height * 0.73f),
        third = Offset(size.width, size.height)
    )
    drawCircle(
        color = mint,
        radius = size.width * 0.08f,
        center = Offset(
            size.width * 0.95f - horizontalMotion,
            size.height * 0.45f + verticalMotion
        )
    )
    drawRect(
        color = mint,
        topLeft = Offset(
            size.width * 0.08f,
            size.height * 0.78f - verticalMotion
        ),
        size = Size(size.width * 0.13f, size.width * 0.13f)
    )
}

private fun DrawScope.drawTriangle(
    color: Color,
    first: Offset,
    second: Offset,
    third: Offset
) {
    val path = Path().apply {
        moveTo(first.x, first.y)
        lineTo(second.x, second.y)
        lineTo(third.x, third.y)
        close()
    }
    drawPath(path = path, color = color)
}

@Composable
fun StatusChip(
    text: String,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    containerColor: Color? = null,
    contentColor: Color? = null
) {
    val background = containerColor ?: if (selected) T4Mint else T4SurfaceVariant
    val content = contentColor ?: if (selected) T4MintDark else T4TextMuted

    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = background,
        contentColor = content
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun ConnectionErrorState(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = T4Surface),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, T4Border)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Filled.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(34.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "No se pudo cargar la informacion",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = T4Text
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = T4TextMuted
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onRetry) {
                Icon(
                    imageVector = Icons.Filled.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text("Reintentar")
            }
        }
    }
}

@Composable
fun EmptyState(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = T4Surface),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, T4Border)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Filled.ErrorOutline,
                contentDescription = null,
                tint = T4Primary,
                modifier = Modifier.size(34.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = T4Text
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = T4TextMuted
            )
            if (action != null) {
                Spacer(modifier = Modifier.height(16.dp))
                action()
            }
        }
    }
}

@Composable
fun ScreenPadding(content: @Composable (PaddingValues) -> Unit) {
    content(PaddingValues(horizontal = 16.dp, vertical = 16.dp))
}
