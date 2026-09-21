package com.pulsepass.domain;

import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "artists")
public class Artist {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "stage_name", nullable = false, unique = true)
    private String stageName;

    @Column
    private String country;

    @Column
    private String genre;

    @Column(nullable = false)
    private boolean active = true;

    @ManyToMany(mappedBy = "artists")
    private Set<Event> events = new HashSet<>();

    protected Artist() {
    }

    public Artist(String stageName, String country, String genre) {
        this.stageName = stageName;
        this.country = country;
        this.genre = genre;
    }

    public Long getId() {
        return id;
    }

    public String getStageName() {
        return stageName;
    }

    public String getCountry() {
        return country;
    }

    public String getGenre() {
        return genre;
    }

    public boolean isActive() {
        return active;
    }

    public Set<Event> getEvents() {
        return events;
    }
}