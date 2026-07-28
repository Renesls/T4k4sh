package com.t4kash.api.identity;

import jakarta.persistence.*;
import java.util.List;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String fullName;

    // Guardamos la carrera como un simple texto
    private String careerName;

    // Guardamos los roles y habilidades en colecciones simples embebidas
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_roles_simple", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "role_name")
    private List<String> roles;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "user_skills_simple", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "skill_name")
    private List<String> skills;

    // IMPORTANTE: Generá los Getters, Setters y Constructores aquí
}