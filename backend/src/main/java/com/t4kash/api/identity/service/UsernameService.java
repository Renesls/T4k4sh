package com.t4kash.api.identity.service;

import com.t4kash.api.marketplace.repository.UsuarioRepository;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.Locale;

@Service
public class UsernameService {
    private static final int MAX_LENGTH = 30;
    private final UsuarioRepository usuarioRepository;

    public UsernameService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    public String generate(String firstName, String lastName) {
        String base = normalize(firstName + "." + lastName);
        if (base.length() < 3) {
            base = "usuario";
        }
        base = limit(base, MAX_LENGTH);

        String candidate = base;
        int suffix = 2;
        while (usuarioRepository.existsByNombreUsuarioIgnoreCase(candidate)) {
            String suffixText = Integer.toString(suffix++);
            candidate = limit(base, MAX_LENGTH - suffixText.length()) + suffixText;
        }
        return candidate;
    }

    private String normalize(String value) {
        String withoutAccents = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return withoutAccents.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", ".")
                .replaceAll("^\\.+|\\.+$", "");
    }

    private String limit(String value, int maxLength) {
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
