package com.gilnote.server.api;

import com.gilnote.server.domain.Car;
import com.gilnote.server.domain.Member;
import com.gilnote.server.domain.PairCode;
import com.gilnote.server.repository.MemberRepository;
import com.gilnote.server.repository.PairCodeRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@RestController
public class JoinController {

    private final PairCodeRepository pairCodeRepository;
    private final MemberRepository memberRepository;

    public JoinController(PairCodeRepository pairCodeRepository, MemberRepository memberRepository) {
        this.pairCodeRepository = pairCodeRepository;
        this.memberRepository = memberRepository;
    }

    public record JoinRequest(String code, String name) {}

    // API 3. 가족 등록
    @PostMapping("/api/join")
    @Transactional
    public ResponseEntity<?> join(@RequestBody JoinRequest req) {
        if (req == null || req.code() == null || req.name() == null || req.name().isBlank()) {
            return ResponseEntity.status(400).body(Map.of(
                    "error", "BAD_REQUEST", "message", "코드와 이름을 모두 입력해 주세요"));
        }

        PairCode pairCode = pairCodeRepository.findByCode(req.code().trim()).orElse(null);
        if (pairCode == null) {
            return ResponseEntity.status(404).body(Map.of(
                    "error", "CODE_NOT_FOUND", "message", "없는 연결 코드예요"));
        }
        if (pairCode.isUsed() || pairCode.getExpiresAt().isBefore(Instant.now())) {
            return ResponseEntity.status(410).body(Map.of(
                    "error", "CODE_EXPIRED", "message", "연결 코드가 만료됐어요. 차에서 새 코드를 받아 주세요"));
        }

        pairCode.markUsed();   // 한 번 쓰면 끝
        Car car = pairCode.getCar();
        String memberToken = "mt_" + UUID.randomUUID().toString().replace("-", "");
        Member member = memberRepository.save(new Member(car, req.name().trim(), memberToken));

        return ResponseEntity.ok(Map.of(
                "memberId", "m_" + member.getId(),
                "memberToken", memberToken,
                "name", member.getName(),
                "carName", car.getName()));
    }
}