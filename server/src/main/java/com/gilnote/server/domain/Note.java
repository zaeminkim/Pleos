package com.gilnote.server.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
public class Note {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private Car car;              // 받는 차

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private Member sender;        // 보낸 가족

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TriggerType triggerType;   // PLACE(장소) 또는 SEAT(좌석)

    private String placeLabel;    // 장소 쪽지: 장소 이름
    private Double lat;           // 장소 쪽지: 위도
    private Double lng;           // 장소 쪽지: 경도
    private Integer radiusM;      // 장소 쪽지: 반경(m)
    private String seat;          // 동승자 쪽지: 좌석

    @Column(nullable = false)
    private String audioPath;         // 서버 폴더(data/audio) 안의 파일 이름
    private String audioContentType;  // audio/webm, audio/mp4 등 (API 7에서 차에 파일을 줄 때 사용)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NoteStatus status = NoteStatus.PENDING;

    private Instant createdAt = Instant.now();
    private Instant playedAt;

    protected Note() {}   // JPA용

    public Note(Car car, Member sender, TriggerType triggerType,
                String placeLabel, Double lat, Double lng, Integer radiusM, String seat,
                String audioPath, String audioContentType) {
        this.car = car;
        this.sender = sender;
        this.triggerType = triggerType;
        this.placeLabel = placeLabel;
        this.lat = lat;
        this.lng = lng;
        this.radiusM = radiusM;
        this.seat = seat;
        this.audioPath = audioPath;
        this.audioContentType = audioContentType;
    }

    // 차가 받아 갔을 때 (API 6): PENDING -> DELIVERED
    public void markDelivered() {
        if (status == NoteStatus.PENDING) status = NoteStatus.DELIVERED;
    }

    // 차에서 재생됐을 때 (API 8): -> PLAYED
    public void markPlayed(Instant when) {
        status = NoteStatus.PLAYED;
        playedAt = (when != null) ? when : Instant.now();
    }

    public Long getId() { return id; }
    public Car getCar() { return car; }
    public Member getSender() { return sender; }
    public TriggerType getTriggerType() { return triggerType; }
    public String getPlaceLabel() { return placeLabel; }
    public Double getLat() { return lat; }
    public Double getLng() { return lng; }
    public Integer getRadiusM() { return radiusM; }
    public String getSeat() { return seat; }
    public String getAudioPath() { return audioPath; }
    public String getAudioContentType() { return audioContentType; }
    public NoteStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getPlayedAt() { return playedAt; }
}
