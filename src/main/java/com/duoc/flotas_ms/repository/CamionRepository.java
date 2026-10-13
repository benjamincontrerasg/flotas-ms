package com.duoc.flotas_ms.repository;

import com.duoc.flotas_ms.model.Camion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CamionRepository extends JpaRepository<Camion, Long> {
}
