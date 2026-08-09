package com.t4kash.api.finance.controller;

import com.t4kash.api.finance.dto.CheckoutResponse;
import com.t4kash.api.finance.dto.PaymentResponse;
import com.t4kash.api.finance.dto.WalletResponse;
import com.t4kash.api.finance.service.PagaditoWebhookVerifier;
import com.t4kash.api.finance.service.PaymentService;
import com.t4kash.api.identity.dto.AuthenticatedUserResponse;
import com.t4kash.api.identity.web.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@Tag(name = "Wallet y pagos", description = "Balance, pagos protegidos y Pagadito Sandbox")
public class PaymentController {
    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @GetMapping("/wallet")
    @Operation(summary = "Consultar billetera e historial")
    @SecurityRequirement(name = "bearerAuth")
    public WalletResponse getWallet(@CurrentUser AuthenticatedUserResponse user) {
        return paymentService.getWallet(user.idUsuario());
    }

    @GetMapping("/jobs/{idTrabajo}/payment")
    @Operation(summary = "Consultar el pago de un trabajo")
    @SecurityRequirement(name = "bearerAuth")
    public PaymentResponse getJobPayment(
            @CurrentUser AuthenticatedUserResponse user,
            @PathVariable Integer idTrabajo
    ) {
        return paymentService.findByJob(user.idUsuario(), idTrabajo);
    }

    @PostMapping("/jobs/{idTrabajo}/payment/checkout")
    @Operation(summary = "Crear checkout en Pagadito Sandbox")
    @SecurityRequirement(name = "bearerAuth")
    public CheckoutResponse createCheckout(
            @CurrentUser(role = "CLIENTE") AuthenticatedUserResponse user,
            @PathVariable Integer idTrabajo
    ) {
        return paymentService.createCheckout(user.idUsuario(), idTrabajo);
    }

    @PostMapping("/jobs/{idTrabajo}/payment/cash/confirm-receipt")
    @Operation(summary = "Confirmar recepcion de un pago en efectivo")
    @SecurityRequirement(name = "bearerAuth")
    public PaymentResponse confirmCashReceipt(
            @CurrentUser(role = "ESTUDIANTE") AuthenticatedUserResponse user,
            @PathVariable Integer idTrabajo
    ) {
        return paymentService.confirmCashReceipt(user.idUsuario(), idTrabajo);
    }

    @PostMapping("/payments/{idPago}/refresh")
    @Operation(summary = "Actualizar estado consultando Pagadito")
    @SecurityRequirement(name = "bearerAuth")
    public PaymentResponse refreshPayment(
            @CurrentUser AuthenticatedUserResponse user,
            @PathVariable Integer idPago
    ) {
        return paymentService.refreshStatus(user.idUsuario(), idPago);
    }

    @PostMapping("/payments/pagadito/webhook")
    @Operation(summary = "Recibir evento firmado de Pagadito")
    public void pagaditoWebhook(
            @RequestBody String rawBody,
            @RequestHeader("PAGADITO-NOTIFICATION-ID") String notificationId,
            @RequestHeader("PAGADITO-NOTIFICATION-TIMESTAMP") String timestamp,
            @RequestHeader("PAGADITO-AUTH-ALGO") String algorithm,
            @RequestHeader("PAGADITO-CERT-URL") String certificateUrl,
            @RequestHeader("PAGADITO-SIGNATURE") String signature
    ) {
        paymentService.processWebhook(
                rawBody,
                new PagaditoWebhookVerifier.Headers(
                        notificationId, timestamp, algorithm, certificateUrl, signature
                )
        );
    }

    @GetMapping(value = "/payments/pagadito/return", produces = MediaType.TEXT_HTML_VALUE)
    @Operation(summary = "Confirmar retorno del checkout de Pagadito")
    public String pagaditoReturn(
            @RequestParam(required = false) String token,
            @RequestParam(name = "token_trans", required = false) String transactionToken
    ) {
        String resolvedToken = token == null || token.isBlank() ? transactionToken : token;
        if (resolvedToken == null || resolvedToken.isBlank()) {
            throw new IllegalArgumentException("Pagadito no devolvio el token de la transaccion.");
        }
        String status = paymentService.processReturn(resolvedToken);
        return """
                <!doctype html><html lang="es"><head><meta charset="utf-8">
                <meta name="viewport" content="width=device-width,initial-scale=1">
                <title>T4KASH - Resultado del pago</title></head>
                <body style="font-family:sans-serif;padding:32px;background:#f7f7f4;color:#191919">
                <h1>Pago recibido por T4KASH</h1><p>Estado: <strong>%s</strong></p>
                <p>Ya puedes volver a la aplicacion y actualizar tu Wallet.</p></body></html>
                """.formatted(status);
    }
}
