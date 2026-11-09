package com.gilnote.server.repository;

import com.gilnote.server.domain.PairCode;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface PairCodeRepository extends JpaRepository<PairCode, Long> {
    Optional<PairCode> findByCode(String code);
    boolean existsByCode(String code);
}