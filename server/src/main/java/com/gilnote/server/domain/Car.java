package com.gilnote.server.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
public class Car {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String carToken;

    private LocalDateTime createdAt = LocalDateTime.now();

    protected Car() {}

    public Car(String carToken) { this.carToken = carToken; }

    public Long getId() { return id; }
    public String getCarToken() { return carToken; }
}