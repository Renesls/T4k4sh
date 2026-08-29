package com.t4kash.api.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FirebaseConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger(FirebaseConfig.class);

    private final String credentialsJson;

    public FirebaseConfig(
            @Value("${app.firebase.credentials-json:}") String credentialsJson
    ) {
        this.credentialsJson = credentialsJson;
    }

    @PostConstruct
    public void init() {
        if (credentialsJson == null || credentialsJson.isBlank()) {
            LOGGER.warn(
                    "FIREBASE_CREDENTIALS_JSON no esta configurado; Firebase Admin no se inicializara "
                            + "y las notificaciones push quedaran deshabilitadas."
            );
            return;
        }
        try (InputStream serviceAccount = new ByteArrayInputStream(
                credentialsJson.getBytes(StandardCharsets.UTF_8)
        )) {
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build();

            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options);
                LOGGER.info("Firebase Admin inicializado correctamente.");
            }
        } catch (IOException e) {
            LOGGER.error("No se pudo inicializar Firebase Admin desde FIREBASE_CREDENTIALS_JSON.", e);
        }
    }
}
