package com.gilnote.server.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
public class Note {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private Car car;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private Member sender;

    @Enumerated(EnumType.STRING)
    private TriggerType triggerType;

    private String placeLabel;
    private Double lat;
    private Double lng;
    private Integer radiusM;
    private String seat;          // 동승자 쪽지일 때: "REAR_LEFT" 등

    private String audioPath;     // 서버 디스크의 음성 파일 경로

    @Enumerated(EnumType.STRING)
    private NoteStatus status = NoteStatus.PENDING;

    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime playedAt;

    protected Note() {}

    public Long getId() { return id; }
    public NoteStatus getStatus() { return status; }
}