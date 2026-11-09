package com.gilnote.server.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
public class Car {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Column(unique = true, nullable = false)
    private String carToken;

    private Instant createdAt = Instant.now();

    protected Car() {}

    public Car(String name, String carToken) {
        this.name = name;
        this.carToken = carToken;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getCarToken() { return carToken; }
}