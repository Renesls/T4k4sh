package com.t4kash.api.identity.dto;

public class UserProfileResponse {
    private Long id;
    private String email;
    private String fullName;

    public UserProfileResponse(Long id, String email, String fullName) {
        this.id = id;
        this.email = email;
        this.fullName = fullName;
    }

    // Getters
    public Long getId() { return id; }
    public String getEmail() { return email; }
    public String getFullName() { return fullName; }
}