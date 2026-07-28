package com.t4kash.api.identity.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "rols")
public class Rol {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

}