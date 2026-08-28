package com.t4kash.api.finance.service;

import com.t4kash.api.exception.ResourceConflictException;
import com.t4kash.api.exception.AccountNotVerifiedException;
import com.t4kash.api.finance.entity.DesembolsoPago;
import com.t4kash.api.finance.entity.Pago;
import com.t4kash.api.finance.repository.DesembolsoPagoRepository;
import com.t4kash.api.finance.repository.ReembolsoPagoRepository;
import com.t4kash.api.finance.repository.TransaccionPagoRepository;
import com.t4kash.api.identity.service.IdentityVerificationPolicyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FinancialLedgerServiceTest {
    private DesembolsoPagoRepository payoutRepository;
    private IdentityVerificationPolicyService identityVerificationPolicy;
    private FinancialLedgerService service;

    @BeforeEach
    void setUp() {
        payoutRepository = mock(DesembolsoPagoRepository.class);
        identityVerificationPolicy = mock(IdentityVerificationPolicyService.class);
        service = new FinancialLedgerService(
                mock(TransaccionPagoRepository.class),
                payoutRepository,
                mock(ReembolsoPagoRepository.class),
                identityVerificationPolicy
        );
        when(payoutRepository.save(any(DesembolsoPago.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void registersConfirmedSandboxPayout() {
        Pago payment = payment("SANDBOX");

        DesembolsoPago payout = service.registerSandboxPayout(payment);

        assertEquals("CONFIRMADO", payout.getEstadoDesembolso());
        assertEquals("SANDBOX", payout.getProveedorDesembolso());
        assertEquals(new BigDecimal("100.00"), payout.getMontoDesembolso());
        verify(payoutRepository).save(payout);
    }

    @Test
    void returnsExistingPayoutForRepeatedRequest() {
        Pago payment = payment("SANDBOX");
        DesembolsoPago existing = new DesembolsoPago();
        when(payoutRepository.findByClaveIdempotencia("PAYOUT:" + payment.getUuidPago()))
                .thenReturn(Optional.of(existing));

        assertSame(existing, service.registerSandboxPayout(payment));
        verify(payoutRepository, never()).save(any());
    }

    @Test
    void rejectsAutomaticPayoutOutsideSandbox() {
        assertThrows(
                ResourceConflictException.class,
                () -> service.registerSandboxPayout(payment("PRODUCCION"))
        );
        verify(payoutRepository, never()).save(any());
    }

    @Test
    void rejectsNewPayoutWhenStudentIdentityIsNotApproved() {
        Pago payment = payment("SANDBOX");
        doThrow(new AccountNotVerifiedException("Verifica tu identidad desde Perfil."))
                .when(identityVerificationPolicy)
                .requireApproved(20, "recibir un pago");

        assertThrows(
                AccountNotVerifiedException.class,
                () -> service.registerSandboxPayout(payment)
        );
        verify(payoutRepository, never()).save(any());
    }

    private Pago payment(String environment) {
        Pago payment = new Pago();
        payment.setIdPago(8);
        payment.setIdTrabajo(5);
        payment.setIdEstudiante(20);
        payment.setUuidPago(UUID.randomUUID());
        payment.setEntornoPago(environment);
        payment.setMonedaCobro("NIO");
        payment.setMontoEstudiante(new BigDecimal("100.00"));
        return payment;
    }
}
