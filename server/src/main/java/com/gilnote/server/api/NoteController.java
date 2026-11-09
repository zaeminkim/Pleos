package com.gilnote.server.api;

import com.gilnote.server.domain.Member;
import com.gilnote.server.domain.Note;
import com.gilnote.server.domain.TriggerType;
import com.gilnote.server.repository.MemberRepository;
import com.gilnote.server.repository.NoteRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/notes")
public class NoteController {

    private static final long MAX_AUDIO_BYTES = 10L * 1024 * 1024;   // 10MB

    private final MemberRepository memberRepository;
    private final NoteRepository noteRepository;
    private final Path audioDir;   // 녹음 파일을 저장할 폴더

    public NoteController(MemberRepository memberRepository,
                          NoteRepository noteRepository,
                          @Value("${gilnote.audio-dir:data/audio}") String audioDir) {
        this.memberRepository = memberRepository;
        this.noteRepository = noteRepository;
        this.audioDir = Paths.get(audioDir).toAbsolutePath();
    }

    // API 4. 쪽지 올리기 (가족 폰 -> 서버)
    @PostMapping
    @Transactional
    public ResponseEntity<?> upload(
            @RequestHeader(value = "X-Member-Token", required = false) String memberToken,
            @RequestParam(value = "audio", required = false) MultipartFile audio,
            @RequestParam(value = "triggerType", required = false) String triggerType,
            @RequestParam(value = "lat", required = false) Double lat,
            @RequestParam(value = "lng", required = false) Double lng,
            @RequestParam(value = "radiusM", required = false) Integer radiusM,
            @RequestParam(value = "placeLabel", required = false) String placeLabel,
            @RequestParam(value = "seat", required = false) String seat) throws IOException {

        // ① 누가 보냈나 (토큰 -> 가족)
        Member member = (memberToken == null) ? null
                : memberRepository.findByMemberToken(memberToken).orElse(null);
        if (member == null) {
            return error(401, "INVALID_MEMBER_TOKEN", "연결되지 않은 폰이에요. 차의 QR로 다시 연결해 주세요");
        }

        // ② 녹음 파일 검사
        if (audio == null || audio.isEmpty()) {
            return error(400, "AUDIO_REQUIRED", "녹음 파일이 없어요");
        }
        if (audio.getSize() > MAX_AUDIO_BYTES) {
            return error(400, "AUDIO_TOO_LARGE", "녹음 파일은 10MB 이하만 보낼 수 있어요");
        }
        String contentType = audio.getContentType();
        if (contentType == null || !(contentType.startsWith("audio/") || contentType.startsWith("video/"))) {
            return error(400, "AUDIO_TYPE", "음성 파일만 보낼 수 있어요");
        }

        // ③ 언제 들려줄지 검사
        TriggerType type;
        try {
            type = TriggerType.valueOf(triggerType);
        } catch (Exception e) {
            return error(400, "BAD_TRIGGER_TYPE", "triggerType은 PLACE 또는 SEAT여야 해요");
        }
        if (type == TriggerType.PLACE) {
            if (lat == null || lng == null) {
                return error(400, "PLACE_REQUIRED", "장소 쪽지는 위도(lat)와 경도(lng)가 필요해요");
            }
            if (radiusM == null) radiusM = 200;   // 기본 반경 200m
            seat = null;
        } else {
            if (seat == null || seat.isBlank()) {
                return error(400, "SEAT_REQUIRED", "동승자 쪽지는 좌석(seat)이 필요해요");
            }
            lat = null;
            lng = null;
            radiusM = null;
        }

        // ④ 녹음 파일을 서버 폴더에 저장
        Files.createDirectories(audioDir);
        String fileName = UUID.randomUUID() + "." + extensionOf(contentType);
        audio.transferTo(audioDir.resolve(fileName));

        // ⑤ 쪽지 정보를 DB에 저장 (받는 차 = 이 가족이 연결된 차)
        Note note = noteRepository.save(new Note(
                member.getCar(), member, type,
                placeLabel, lat, lng, radiusM, seat,
                fileName, contentType));

        return ResponseEntity.ok(Map.of(
                "noteId", note.getId(),
                "status", note.getStatus().name(),
                "createdAt", note.getCreatedAt().toString()));
    }

    // API 5. 내가 보낸 쪽지 목록 (가족 폰 -> 서버), 최신순
    @GetMapping
    public ResponseEntity<?> myNotes(
            @RequestHeader(value = "X-Member-Token", required = false) String memberToken) {

        // ① 누가 물어봤나 (토큰 -> 가족)
        Member member = (memberToken == null) ? null
                : memberRepository.findByMemberToken(memberToken).orElse(null);
        if (member == null) {
            return error(401, "INVALID_MEMBER_TOKEN", "연결되지 않은 폰이에요. 차의 QR로 다시 연결해 주세요");
        }

        // ② 이 가족이 보낸 쪽지만 최신순으로
        List<Note> notes = noteRepository.findBySenderOrderByCreatedAtDesc(member);

        // ③ 폰 화면에 필요한 값만 골라서 응답
        List<Map<String, Object>> body = notes.stream().map(this::toSummary).toList();
        return ResponseEntity.ok(body);
    }

    // 쪽지 하나를 응답 모양으로 바꾸기 (값이 비어 있으면 null 그대로)
    private Map<String, Object> toSummary(Note note) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("noteId", note.getId());
        m.put("triggerType", note.getTriggerType().name());
        m.put("placeLabel", note.getPlaceLabel());
        m.put("seat", note.getSeat());
        m.put("status", note.getStatus().name());
        m.put("createdAt", note.getCreatedAt().toString());
        m.put("playedAt", note.getPlayedAt() == null ? null : note.getPlayedAt().toString());
        return m;
    }

    // 파일 형식에 맞는 확장자
    private String extensionOf(String contentType) {
        if (contentType.contains("webm")) return "webm";
        if (contentType.contains("mp4") || contentType.contains("m4a") || contentType.contains("aac")) return "m4a";
        if (contentType.contains("ogg")) return "ogg";
        if (contentType.contains("wav")) return "wav";
        if (contentType.contains("mpeg")) return "mp3";
        return "bin";
    }

    // 에러 응답: { "error": 코드, "message": 설명 }
    private ResponseEntity<Map<String, String>> error(int status, String code, String message) {
        return ResponseEntity.status(status).body(Map.of("error", code, "message", message));
    }
}
