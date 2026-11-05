package com.gilnote.server.repository;

import com.gilnote.server.domain.Car;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface CarRepository extends JpaRepository<Car, Long> {
    Optional<Car> findByCarToken(String carToken);
}