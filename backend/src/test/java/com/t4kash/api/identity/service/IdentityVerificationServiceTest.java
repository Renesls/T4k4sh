package com.t4kash.api.identity.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.t4kash.api.exception.IdentityProviderException;
import com.t4kash.api.identity.dto.IdentityVerificationStatusResponse;
import com.t4kash.api.identity.dto.IdentityWebhookResponse;
import com.t4kash.api.identity.entity.EventoWebhookIdentidad;
import com.t4kash.api.identity.entity.VerificacionIdentidad;
import com.t4kash.api.identity.repository.EventoWebhookIdentidadRepository;
import com.t4kash.api.identity.repository.VerificacionIdentidadRepository;
import com.t4kash.api.marketplace.entity.Usuario;
import com.t4kash.api.marketplace.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IdentityVerificationServiceTest {
    private static final UUID SESSION_ID =
            UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");
    private static final UUID WORKFLOW_ID =
            UUID.fromString("11111111-2222-3333-4444-555555555555");
    private static final UUID USER_UUID =
            UUID.fromString("99999999-8888-4777-8666-555555555555");

    @Mock
    private VerificacionIdentidadRepository verificationRepository;
    @Mock
    private EventoWebhookIdentidadRepository webhookRepository;
    @Mock
    private UsuarioRepository userRepository;
    @Mock
    private DiditClient diditClient;
    @Mock
    private DiditWebhookVerifier webhookVerifier;
    @Mock
    private IdentityDocumentHasher documentHasher;
    @Mock
    private IdentityVerificationPolicyService policyService;

    private IdentityVerificationService service;
    private VerificacionIdentidad verification;

    @BeforeEach
    void setUp() {
        service = new IdentityVerificationService(
                verificationRepository,
                webhookRepository,
                userRepository,
                diditClient,
                webhookVerifier,
                documentHasher,
                policyService,
                new ObjectMapper()
        );
        verification = verification();
    }

    @Test
    void approvedDecisionStoresOnlyDocumentFingerprint() {
        stubRefreshContext();
        when(diditClient.getDecision(SESSION_ID)).thenReturn(decision("Approved"));
        when(documentHasher.hash("NIC", "001-010190-0001A"))
                .thenReturn("a".repeat(64));
        when(verificationRepository.findFirstByHuellaDocumentoAndEstadoVerificacion(
                "a".repeat(64),
                "APROBADA"
        )).thenReturn(Optional.empty());
        when(policyService.isApprovedNow(verification)).thenReturn(true);

        IdentityVerificationStatusResponse response = service.refresh(1);

        assertEquals("APROBADA", verification.getEstadoVerificacion());
        assertEquals("a".repeat(64), verification.getHuellaDocumento());
        assertEquals(true, response.verificada());
    }

    @ParameterizedTest
    @CsvSource({
            "Declined,RECHAZADA",
            "Abandoned,ABANDONADA",
            "Expired,EXPIRADA",
            "In Review,EN_REVISION"
    })
    void mapsProviderStatusesWithoutActivatingIdentity(
            String providerStatus,
            String expectedStatus
    ) {
        stubRefreshContext();
        when(diditClient.getDecision(SESSION_ID))
                .thenReturn(decision(providerStatus));

        IdentityVerificationStatusResponse response = service.refresh(1);

        assertEquals(expectedStatus, verification.getEstadoVerificacion());
        assertEquals(false, response.verificada());
        verify(documentHasher, never()).hash(anyString(), anyString());
    }

    @Test
    void duplicateProcessedWebhookDoesNotQueryProviderAgain() throws Exception {
        EventoWebhookIdentidad event = new EventoWebhookIdentidad();
        event.setHashContenido(sha256(webhookBody()));
        event.setEstadoProcesamiento("PROCESADO");
        when(verificationRepository.findByIdSesionProveedor(SESSION_ID))
                .thenReturn(Optional.of(verification));
        when(webhookRepository.findByClaveIdempotencia(anyString()))
                .thenReturn(Optional.of(event));

        IdentityWebhookResponse response = service.processWebhook(
                webhookBody(),
                webhookHeaders()
        );

        assertEquals(true, response.duplicado());
        verify(diditClient, never()).getDecision(any());
    }

    @Test
    void providerFailureMarksWebhookForRetry() {
        when(verificationRepository.findByIdSesionProveedor(SESSION_ID))
                .thenReturn(Optional.of(verification));
        when(webhookRepository.findByClaveIdempotencia(anyString()))
                .thenReturn(Optional.empty());
        when(webhookRepository.save(any(EventoWebhookIdentidad.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(diditClient.getDecision(SESSION_ID))
                .thenThrow(new IdentityProviderException("Didit temporalmente no disponible."));

        assertThrows(
                IdentityProviderException.class,
                () -> service.processWebhook(webhookBody(), webhookHeaders())
        );

        ArgumentCaptor<EventoWebhookIdentidad> captor =
                ArgumentCaptor.forClass(EventoWebhookIdentidad.class);
        verify(webhookRepository, org.mockito.Mockito.atLeast(2)).save(captor.capture());
        EventoWebhookIdentidad saved = captor.getAllValues()
                .get(captor.getAllValues().size() - 1);
        assertEquals("ERROR", saved.getEstadoProcesamiento());
    }

    private VerificacionIdentidad verification() {
        VerificacionIdentidad item = new VerificacionIdentidad();
        item.setIdVerificacionIdentidad(10);
        item.setIdUsuario(1);
        item.setProveedor("DIDIT");
        item.setIdSesionProveedor(SESSION_ID);
        item.setIdFlujoProveedor(WORKFLOW_ID);
        item.setEstadoVerificacion("EN_PROCESO");
        item.setEstadoProveedor("In Progress");
        item.setOrigenSolicitud("PERFIL");
        item.setFechaInicio(LocalDateTime.now().minusMinutes(5));
        item.setFechaActualizacion(LocalDateTime.now().minusMinutes(1));
        return item;
    }

    private void stubRefreshContext() {
        when(verificationRepository.findFirstByIdUsuarioOrderByFechaInicioDesc(1))
                .thenReturn(Optional.of(verification));
        when(userRepository.findById(1)).thenReturn(Optional.of(user()));
        when(verificationRepository.save(any(VerificacionIdentidad.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    private Usuario user() {
        Usuario user = new Usuario();
        user.setIdUsuario(1);
        user.setUuidPublico(USER_UUID);
        return user;
    }

    private DiditClient.Decision decision(String status) {
        return new DiditClient.Decision(
                SESSION_ID,
                WORKFLOW_ID,
                1,
                DiditClient.vendorData(USER_UUID),
                status,
                "001-010190-0001A",
                "NIC",
                LocalDateTime.now().plusYears(1)
        );
    }

    private String webhookBody() {
        return """
                {"event_id":"event-1","webhook_type":"status.updated","timestamp":1774970000,"session_id":"aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee","status":"Approved"}
                """.trim();
    }

    private DiditWebhookVerifier.Headers webhookHeaders() {
        return new DiditWebhookVerifier.Headers("signature", null, null, "1774970000");
    }

    private String sha256(String value) throws Exception {
        return java.util.HexFormat.of().formatHex(
                java.security.MessageDigest.getInstance("SHA-256")
                        .digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8))
        );
    }
}
