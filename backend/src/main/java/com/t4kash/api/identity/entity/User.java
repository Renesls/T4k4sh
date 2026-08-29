package com.t4kash.api.identity.entity;

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

    @Column(name = "fcm_token", length = 255)
    private String fcmToken;



    private String careerName;



    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_roles_simple", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "role_name")
    private List<String> roles;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "user_skills_simple", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "skill_name")


    private List<String> skills;

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Object getPassword() {
        return password;
    }

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getFullName() {
        return fullName;
    }

    public String getCareerName() {return careerName;}

    public void setCareerName(String careerName) {this.careerName = careerName;}

    public String getFcmToken() {return fcmToken;}

    public void setFcmToken(String fcmToken) {this.fcmToken = fcmToken;}

}