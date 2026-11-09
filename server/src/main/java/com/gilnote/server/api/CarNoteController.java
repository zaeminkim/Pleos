package com.gilnote.server.api;

import com.gilnote.server.domain.Car;
import com.gilnote.server.domain.Note;
import com.gilnote.server.domain.NoteStatus;
import com.gilnote.server.repository.CarRepository;
import com.gilnote.server.repository.NoteRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// 차 앱이 부르는 쪽지 API (API 6, 7, 8)
@RestController
@RequestMapping("/api/cars/notes")
public class CarNoteController {

    private final CarRepository carRepository;
    private final NoteRepository noteRepository;
    private final Path audioDir;

    public CarNoteController(CarRepository carRepository,
                             NoteRepository noteRepository,
                             @Value("${gilnote.audio-dir:data/audio}") String audioDir) {
        this.carRepository = carRepository;
        this.noteRepository = noteRepository;
        this.audioDir = Paths.get(audioDir).toAbsolutePath();
    }

    // API 6. 차의 쪽지 목록 (20초마다 호출)
    // 아직 안 들은 쪽지(PENDING, DELIVERED)를 주고, PENDING은 DELIVERED("차에 도착")로 바꾼다
    @GetMapping
    @Transactional
    public ResponseEntity<?> list(
            @RequestHeader(value = "X-Car-Token", required = false) String carToken) {

        Car car = findCar(carToken);
        if (car == null) return invalidCar();

        List<Note> notes = noteRepository.findByCarAndStatusInOrderByCreatedAtAsc(
                car, List.of(NoteStatus.PENDING, NoteStatus.DELIVERED));

        List<Map<String, Object>> body = notes.stream().map(note -> {
            note.markDelivered();                 // 메서드가 끝날 때 DB에 자동 반영
            return toCarView(note);
        }).toList();

        return ResponseEntity.ok(body);
    }

    // API 7. 음성 파일 받기 (차가 미리 내려받아 저장)
    @GetMapping("/{noteId}/audio")
    @Transactional(readOnly = true)
    public ResponseEntity<?> audio(
            @RequestHeader(value = "X-Car-Token", required = false) String carToken,
            @PathVariable Long noteId) throws IOException {

        Car car = findCar(carToken);
        if (car == null) return invalidCar();

        Note note = findMyNote(car, noteId);
        if (note == null) return notFound();

        Path file = audioDir.resolve(note.getAudioPath());
        if (!Files.exists(file)) return notFound();

        String type = (note.getAudioContentType() != null) ? note.getAudioContentType() : "application/octet-stream";
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(type))
                .contentLength(Files.size(file))
                .body(new FileSystemResource(file));
    }

    public record PlayedRequest(String playedAt) {}

    // API 8. 들었어요 (재생이 끝나면 차가 호출)
    @PostMapping("/{noteId}/played")
    @Transactional
    public ResponseEntity<?> played(
            @RequestHeader(value = "X-Car-Token", required = false) String carToken,
            @PathVariable Long noteId,
            @RequestBody(required = false) PlayedRequest req) {

        Car car = findCar(carToken);
        if (car == null) return invalidCar();

        Note note = findMyNote(car, noteId);
        if (note == null) return notFound();

        // 차가 보낸 재생 시각이 있으면 그걸 쓰고, 없거나 형식이 틀리면 지금 시각
        Instant when = null;
        if (req != null && req.playedAt() != null) {
            try { when = Instant.parse(req.playedAt()); } catch (Exception ignored) {}
        }
        note.markPlayed(when);

        return ResponseEntity.ok(Map.of("noteId", note.getId(), "status", note.getStatus().name()));
    }

    // ---- 도우미 ----

    // 헤더의 토큰으로 차 찾기 (없으면 null)
    private Car findCar(String carToken) {
        return (carToken == null) ? null : carRepository.findByCarToken(carToken).orElse(null);
    }

    // 이 차의 쪽지만 (다른 차의 쪽지면 null → 404, 남의 쪽지를 못 보게)
    private Note findMyNote(Car car, Long noteId) {
        Note note = noteRepository.findById(noteId).orElse(null);
        if (note == null || !note.getCar().getId().equals(car.getId())) return null;
        return note;
    }

    // 차 앱에 보낼 쪽지 모양
    private Map<String, Object> toCarView(Note note) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("noteId", note.getId());
        m.put("from", note.getSender().getName());
        m.put("triggerType", note.getTriggerType().name());
        m.put("lat", note.getLat());
        m.put("lng", note.getLng());
        m.put("radiusM", note.getRadiusM());
        m.put("placeLabel", note.getPlaceLabel());
        m.put("seat", note.getSeat());
        m.put("audioUrl", "/api/cars/notes/" + note.getId() + "/audio");
        m.put("createdAt", note.getCreatedAt().toString());
        return m;
    }

    private ResponseEntity<?> invalidCar() {
        return ResponseEntity.status(401).body(Map.of(
                "error", "INVALID_CAR_TOKEN", "message", "차 토큰이 없거나 잘못됐어요"));
    }

    private ResponseEntity<?> notFound() {
        return ResponseEntity.status(404).body(Map.of(
                "error", "NOTE_NOT_FOUND", "message", "이 차의 쪽지가 아니거나 없는 쪽지예요"));
    }
}
