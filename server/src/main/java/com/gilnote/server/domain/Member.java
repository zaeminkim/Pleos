package com.gilnote.server.domain;

import jakarta.persistence.*;

@Entity
public class Member {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private Car car;

    private String name;

    @Column(unique = true, nullable = false)
    private String memberToken;

    protected Member() {}

    public Member(Car car, String name, String memberToken) {
        this.car = car;
        this.name = name;
        this.memberToken = memberToken;
    }

    public Long getId() { return id; }
    public Car getCar() { return car; }
    public String getName() { return name; }
    public String getMemberToken() { return memberToken; }
}