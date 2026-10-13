package com.duoc.flotas_ms.repository;

import com.duoc.flotas_ms.model.Camion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.duoc.flotas_ms.model.EstadoCamion;
import java.util.List;

@Repository
public interface CamionRepository extends JpaRepository<Camion, Long> {

    List<Camion> findByEstado(EstadoCamion estado);

    List<Camion> findByConductorId(Long conductorId);
}
