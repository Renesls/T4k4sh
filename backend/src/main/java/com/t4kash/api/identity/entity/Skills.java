package com.t4kash.api.identity;

import jakarta.persistence.*;
import jakarta.persistence.GenerationType;

public class Skills {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;
}
