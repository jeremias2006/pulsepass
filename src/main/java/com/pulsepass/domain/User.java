package com.pulsepass.domain;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private boolean active = true;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private UserProfile profile;

    @OneToMany(mappedBy = "user")
    private List<Ticket> tickets = new ArrayList<>();

    protected User() {
    }

    public User(String username, String email) {
        this.username = username;
        this.email = email;
    }

    public void assignProfile(UserProfile profile) {
        this.profile = profile;
        profile.setUser(this);
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public boolean isActive() {
        return active;
    }

    public UserProfile getProfile() {
        return profile;
    }

    public List<Ticket> getTickets() {
        return tickets;
    }
}