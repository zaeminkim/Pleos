package com.gilnote.server.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
public class PairCode {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private Car car;

    @Column(unique = true, nullable = false)
    private String code;

    private LocalDateTime expiresAt;
    private boolean used = false;

    protected PairCode() {}

    public PairCode(Car car, String code, LocalDateTime expiresAt) {
        this.car = car;
        this.code = code;
        this.expiresAt = expiresAt;
    }

    public Long getId() { return id; }
    public Car getCar() { return car; }
    public String getCode() { return code; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public boolean isUsed() { return used; }
    public void markUsed() { this.used = true; }
}