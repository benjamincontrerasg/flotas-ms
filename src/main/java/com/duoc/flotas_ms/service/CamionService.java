package com.duoc.flotas_ms.service;

import com.duoc.flotas_ms.dto.CamionRequest;
import com.duoc.flotas_ms.dto.CamionResponse;
import com.duoc.flotas_ms.model.Camion;
import com.duoc.flotas_ms.repository.CamionRepository;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CamionService {

    private final CamionRepository repository;

    public CamionService(CamionRepository repository) {
        this.repository = repository;
    }

    public CamionResponse crear(CamionRequest datos) {
        if (datos.getPatente() == null || datos.getPatente().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La patente es obligatoria");
        }
        if (datos.getCapacidadKg() == null || !Double.isFinite(datos.getCapacidadKg())
                || datos.getCapacidadKg() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La capacidad debe ser un numero mayor que cero");
        }

        Camion camion = new Camion();
        camion.setPatente(datos.getPatente().trim().toUpperCase(Locale.ROOT));
        camion.setCapacidadKg(datos.getCapacidadKg());
        if (datos.getEstado() != null) {
            camion.setEstado(datos.getEstado());
        }
        camion.setConductorId(datos.getConductorId());

        Camion guardado = repository.save(camion);
        return new CamionResponse(guardado.getId(), guardado.getPatente(),
                guardado.getCapacidadKg(), guardado.getEstado(), guardado.getConductorId());
    }
}
