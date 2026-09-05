package com.t4kash.app.ui.screen

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.t4kash.app.ui.components.ConnectionErrorState
import com.t4kash.app.ui.components.EmptyState
import com.t4kash.app.ui.components.StatusChip
import com.t4kash.app.ui.components.T4TopBar
import com.t4kash.app.ui.formatNioCurrency
import com.t4kash.app.ui.model.PaymentDto
import com.t4kash.app.ui.model.WalletMovementDto
import com.t4kash.app.ui.theme.T4Background
import com.t4kash.app.ui.theme.T4Border
import com.t4kash.app.ui.theme.T4Mint
import com.t4kash.app.ui.theme.T4MintDark
import com.t4kash.app.ui.theme.T4Primary
import com.t4kash.app.ui.theme.T4Surface
import com.t4kash.app.ui.theme.T4Text
import com.t4kash.app.ui.theme.T4TextMuted
import com.t4kash.app.ui.viewmodel.MarketplaceViewModel

@Composable
fun WalletScreen(
    viewModel: MarketplaceViewModel,
    onBack: () -> Unit
) {
    val state = viewModel.uiState
    val wallet = state.wallet
    val uriHandler = LocalUriHandler.current

    LaunchedEffect(Unit) {
        viewModel.loadWallet()
    }
    LaunchedEffect(state.checkoutUrl) {
        state.checkoutUrl?.let { url ->
            runCatching { uriHandler.openUri(url) }
            viewModel.clearCheckoutUrl()
        }
    }

    Scaffold(
        containerColor = T4Background,
        topBar = {
            T4TopBar(
                title = "Wallet",
                subtitle = "Pagos protegidos y actividad",
                onBack = onBack
            )
        }
    ) { innerPadding ->
        when {
            state.isLoadingWallet && wallet == null -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Cargando Wallet...")
                }
            }

            state.walletError != null && wallet == null -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    ConnectionErrorState(
                        message = state.walletError,
                        onRetry = viewModel::loadWallet
                    )
                }
            }

            wallet != null -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .background(T4Background),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        BalanceSummary(
                            available = wallet.balanceDisponible,
                            held = wallet.fondosRetenidos,
                            earned = wallet.totalGanado
                        )
                    }

                    state.walletError?.let { error ->
                        item {
                            Text(
                                text = error,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                    state.paymentMessage?.let { message ->
                        item {
                            StatusChip(
                                text = message,
                                containerColor = T4Mint,
                                contentColor = T4MintDark
                            )
                        }
                    }

                    item {
                        SectionTitle("Pagos", "Acuerdos asociados a tus trabajos")
                    }
                    if (wallet.pagos.isEmpty()) {
                        item {
                            EmptyState(
                                title = "Sin pagos registrados",
                                message = "Al aceptar una postulacion, el acuerdo aparecera aqui."
                            )
                        }
                    } else {
                        items(wallet.pagos, key = { it.idPago }) { payment ->
                            PaymentCard(
                                payment = payment,
                                taskTitle = state.tasks.firstOrNull {
                                    it.idTarea == state.jobs.firstOrNull { job ->
                                        job.idTrabajo == payment.idTrabajo
                                    }?.idTarea
                                }?.titulo,
                                processing = state.processingPaymentId == payment.idPago,
                                onPay = {
                                    viewModel.openPaymentCheckout(
                                        payment.idTrabajo,
                                        payment.idPago
                                    )
                                },
                                onRefresh = { viewModel.refreshPayment(payment.idPago) }
                            )
                        }
                    }

                    item {
                        SectionTitle("Movimientos", "Historial financiero verificable")
                    }
                    if (wallet.movimientos.isEmpty()) {
                        item {
                            EmptyState(
                                title = "Sin movimientos",
                                message = "Las confirmaciones y liberaciones se mostraran aqui."
                            )
                        }
                    } else {
                        items(wallet.movimientos, key = { it.idTransaccion }) { movement ->
                            MovementRow(movement)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BalanceSummary(available: Double, held: Double, earned: Double) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = T4Primary)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.AccountBalanceWallet,
                    contentDescription = null,
                    tint = T4Mint
                )
                Spacer(modifier = Modifier.size(10.dp))
                Column {
                    Text(
                        text = formatNioCurrency(available),
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Balance disponible",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.78f)
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                BalanceMetric("Retenido", held, Icons.Filled.Lock)
                BalanceMetric("Total ganado", earned, Icons.Filled.Payments)
            }
        }
    }
}

@Composable
private fun BalanceMetric(
    label: String,
    amount: Double,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = T4Mint,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.size(7.dp))
        Column {
            Text(
                text = formatNioCurrency(amount),
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun PaymentCard(
    payment: PaymentDto,
    taskTitle: String?,
    processing: Boolean,
    onPay: () -> Unit,
    onRefresh: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = T4Surface),
        border = BorderStroke(1.dp, T4Border.copy(alpha = 0.7f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = taskTitle ?: "Trabajo #${payment.idTrabajo}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = T4Text
                    )
                    Text(
                        text = if (payment.metodoPago == "PAGADITO") {
                            "Trabajo #${payment.idTrabajo} · Pagadito ${payment.entornoPago.lowercase()}"
                        } else {
                            "Pago presencial en efectivo"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = T4TextMuted
                    )
                }
                StatusChip(text = payment.estadoPago.toPaymentLabel())
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                AmountColumn("Recibe estudiante", payment.montoEstudiante)
                AmountColumn("Total cliente", payment.montoTotalCliente)
            }
            // El wallet es del estudiante: primero lo que se le retiene a el, y solo
            // despues el total que cobra la plataforma entre las dos partes.
            if (payment.comisionEstudiante > 0.0) {
                Text(
                    text = "Se te retiene: " + formatNioCurrency(payment.comisionEstudiante),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = T4Text
                )
            }
            Text(
                text = if (payment.comisionCliente > 0.0 || payment.comisionEstudiante > 0.0) {
                    "Comision T4KASH " + formatNioCurrency(payment.comisionPlataforma) +
                        ": " + formatNioCurrency(payment.comisionCliente) + " del cliente + " +
                        formatNioCurrency(payment.comisionEstudiante) + " tuyo"
                } else {
                    "Sin comision: pago presencial en efectivo"
                },
                style = MaterialTheme.typography.bodySmall,
                color = T4TextMuted
            )

            if (processing) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.size(8.dp))
                    Text("Procesando...", style = MaterialTheme.typography.bodySmall)
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (payment.puedePagar) {
                        Button(onClick = onPay, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Filled.OpenInNew, contentDescription = null)
                            Spacer(modifier = Modifier.size(6.dp))
                            Text("Pagar")
                        }
                    }
                    if (payment.metodoPago == "PAGADITO" && !payment.estadoPago.isFinalPayment()) {
                        OutlinedButton(onClick = onRefresh, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Filled.Refresh, contentDescription = null)
                            Spacer(modifier = Modifier.size(6.dp))
                            Text("Actualizar")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AmountColumn(label: String, amount: Double) {
    Column {
        Text(label, style = MaterialTheme.typography.bodySmall, color = T4TextMuted)
        Text(
            text = formatNioCurrency(amount),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = T4Text
        )
    }
}

@Composable
private fun MovementRow(movement: WalletMovementDto) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(T4Surface, RoundedCornerShape(8.dp))
            .padding(14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.ReceiptLong,
                contentDescription = null,
                tint = T4Primary
            )
            Spacer(modifier = Modifier.size(10.dp))
            Column {
                Text(
                    text = movement.tipoMovimiento.toPaymentLabel(),
                    fontWeight = FontWeight.SemiBold,
                    color = T4Text
                )
                Text(
                    text = movement.descripcion ?: "Movimiento de pago",
                    style = MaterialTheme.typography.bodySmall,
                    color = T4TextMuted
                )
            }
        }
        Text(
            text = formatNioCurrency(movement.monto),
            fontWeight = FontWeight.Bold,
            color = T4Text
        )
    }
}

@Composable
private fun SectionTitle(title: String, subtitle: String) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = T4Text
        )
        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = T4TextMuted)
    }
}

private fun String.toPaymentLabel(): String = lowercase()
    .replace('_', ' ')
    .replaceFirstChar { it.uppercase() }

private fun String.isFinalPayment(): Boolean = this in setOf(
    "FONDOS_RETENIDOS",
    "PAGO_LIBERADO",
    "PAGO_EXTERNO_CONFIRMADO",
    "PAGO_REVOCADO"
)
