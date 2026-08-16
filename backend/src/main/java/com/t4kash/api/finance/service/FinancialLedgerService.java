package com.t4kash.api.finance.service;

import com.t4kash.api.exception.ResourceConflictException;
import com.t4kash.api.finance.entity.DesembolsoPago;
import com.t4kash.api.finance.entity.DisputaPago;
import com.t4kash.api.finance.entity.Pago;
import com.t4kash.api.finance.entity.ReembolsoPago;
import com.t4kash.api.finance.entity.TransaccionPago;
import com.t4kash.api.finance.repository.DesembolsoPagoRepository;
import com.t4kash.api.finance.repository.ReembolsoPagoRepository;
import com.t4kash.api.finance.repository.TransaccionPagoRepository;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class FinancialLedgerService {
    private final TransaccionPagoRepository movementRepository;
    private final DesembolsoPagoRepository payoutRepository;
    private final ReembolsoPagoRepository refundRepository;

    public FinancialLedgerService(
            TransaccionPagoRepository movementRepository,
            DesembolsoPagoRepository payoutRepository,
            ReembolsoPagoRepository refundRepository
    ) {
        this.movementRepository = movementRepository;
        this.payoutRepository = payoutRepository;
        this.refundRepository = refundRepository;
    }

    public DesembolsoPago registerSandboxPayout(Pago payment) {
        requireSandbox(payment);
        String key = "PAYOUT:" + payment.getUuidPago();
        return payoutRepository.findByClaveIdempotencia(key).orElseGet(() -> {
            LocalDateTime now = LocalDateTime.now();
            DesembolsoPago payout = new DesembolsoPago();
            payout.setIdPago(payment.getIdPago());
            payout.setIdEstudiante(payment.getIdEstudiante());
            payout.setMontoDesembolso(money(payment.getMontoEstudiante()));
            payout.setMoneda(payment.getMonedaCobro());
            payout.setProveedorDesembolso("SANDBOX");
            payout.setEstadoDesembolso("CONFIRMADO");
            payout.setClaveIdempotencia(key);
            payout.setReferenciaDestino("WALLET_T4KASH_SANDBOX");
            payout.setReferenciaProveedor("SANDBOX-" + payment.getUuidPago());
            payout.setFechaCreacion(now);
            payout.setFechaProcesamiento(now);
            payout.setFechaConfirmacion(now);
            return payoutRepository.save(payout);
        });
    }

    public ReembolsoPago registerSandboxRefund(
            Pago payment,
            DisputaPago dispute,
            Integer adminId,
            String reason
    ) {
        requireSandbox(payment);
        String key = "REFUND:DISPUTE:" + dispute.getIdDisputa();
        return refundRepository.findByClaveIdempotencia(key).orElseGet(() -> {
            LocalDateTime now = LocalDateTime.now();
            ReembolsoPago refund = new ReembolsoPago();
            refund.setIdPago(payment.getIdPago());
            refund.setIdDisputa(dispute.getIdDisputa());
            refund.setIdUsuarioSolicita(adminId);
            refund.setMontoReembolso(money(payment.getMontoTotalCliente()));
            refund.setMoneda(payment.getMonedaCobro());
            refund.setMotivo(reason);
            refund.setEstadoReembolso("CONFIRMADO");
            refund.setClaveIdempotencia(key);
            refund.setReferenciaProveedor("SANDBOX-" + payment.getUuidPago());
            refund.setFechaSolicitud(now);
            refund.setFechaProcesamiento(now);
            refund.setFechaConfirmacion(now);
            return refundRepository.save(refund);
        });
    }

    public void recordMovement(
            Pago payment,
            Integer userId,
            String type,
            String affectedBalance,
            BigDecimal amount,
            String description,
            String idempotencyKey
    ) {
        if (movementRepository.existsByClaveIdempotencia(idempotencyKey)) {
            return;
        }
        TransaccionPago movement = new TransaccionPago();
        movement.setIdPago(payment.getIdPago());
        movement.setIdUsuario(userId);
        movement.setTipoMovimiento(type);
        movement.setSaldoAfectado(affectedBalance);
        movement.setMonto(money(amount));
        movement.setMoneda(payment.getMonedaCobro());
        movement.setEstadoMovimiento("REGISTRADO");
        movement.setProveedorPago(payment.getProveedorPago());
        movement.setClaveIdempotencia(idempotencyKey);
        movement.setReferenciaProveedor(payment.getReferenciaProveedor());
        movement.setFechaEventoProveedor(LocalDateTime.now());
        movement.setFechaRegistro(LocalDateTime.now());
        movement.setDescripcion(description);
        movement.setMetadatos(Map.of("idTrabajo", payment.getIdTrabajo()));
        movementRepository.save(movement);
    }

    public List<DesembolsoPago> findPayouts(Integer userId, Pageable pageable) {
        return payoutRepository.findByIdEstudianteOrderByFechaCreacionDesc(userId, pageable);
    }

    public List<ReembolsoPago> findRefunds(Integer userId, Pageable pageable) {
        return refundRepository.findVisibleToUser(userId, pageable);
    }

    private void requireSandbox(Pago payment) {
        if (!"SANDBOX".equals(payment.getEntornoPago())) {
            throw new ResourceConflictException(
                    "Los desembolsos y reembolsos automaticos solo estan habilitados en Sandbox."
            );
        }
    }

    private BigDecimal money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }
}
