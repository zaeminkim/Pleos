package com.gilnote.server.api;

import com.gilnote.server.domain.Car;
import com.gilnote.server.domain.PairCode;
import com.gilnote.server.repository.CarRepository;
import com.gilnote.server.repository.PairCodeRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/cars")
public class CarController {

    private final CarRepository carRepository;
    private final PairCodeRepository pairCodeRepository;
    private final SecureRandom random = new SecureRandom();

    //DI
    public CarController(CarRepository carRepository, PairCodeRepository pairCodeRepository) {
        this.carRepository = carRepository;
        this.pairCodeRepository = pairCodeRepository;
    }

    public record RegisterRequest(String name) {}

    // API 1. 차 등록-> 차에 Id, 토큰을 배정
    @PostMapping("/register")
    public Map<String, String> register(@RequestBody(required = false) RegisterRequest req) {
        String name = (req == null || req.name() == null || req.name().isBlank())
                ? "우리 가족 차" : req.name();
        String carToken = "ct_" + UUID.randomUUID().toString().replace("-", "");
        Car car = carRepository.save(new Car(name, carToken));
        return Map.of("carId", "c_" + car.getId(), "carToken", carToken);
    }

    // API 2. 차의 토큰을 바탕으로 pair-code를 서버로부터 받음
    @PostMapping("/pair-code")
    public ResponseEntity<?> pairCode(
            @RequestHeader(value = "X-Car-Token", required = false) String carToken) {

        Car car = (carToken == null) ? null
                : carRepository.findByCarToken(carToken).orElse(null);
        if (car == null) {
            return ResponseEntity.status(401).body(Map.of(
                    "error", "INVALID_CAR_TOKEN",
                    "message", "차 토큰이 없거나 잘못됐어요"));
        }

        String code;
        do {
            code = String.format("%06d", random.nextInt(1_000_000));
        } while (pairCodeRepository.existsByCode(code));

        Instant expiresAt = Instant.now().plus(3, ChronoUnit.MINUTES);
        pairCodeRepository.save(new PairCode(car, code, expiresAt));

        return ResponseEntity.ok(Map.of("code", code, "expiresAt", expiresAt.toString()));
    }
}