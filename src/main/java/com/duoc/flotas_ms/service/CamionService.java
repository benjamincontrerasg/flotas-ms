package com.duoc.flotas_ms.service;

import com.duoc.flotas_ms.dto.CamionRequest;
import com.duoc.flotas_ms.dto.CamionResponse;
import com.duoc.flotas_ms.model.Camion;
import com.duoc.flotas_ms.repository.CamionRepository;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import com.duoc.flotas_ms.model.EstadoCamion;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.sql.SQLException;
import org.springframework.dao.DataIntegrityViolationException;

@Service
public class CamionService {

    private final CamionRepository repository;

    public CamionService(CamionRepository repository) {
        this.repository = repository;
    }

    public CamionResponse crear(CamionRequest datos) {
        validarDatos(datos);

        Camion camion = new Camion();
        camion.setPatente(datos.getPatente().trim().toUpperCase(Locale.ROOT));
        if (repository.existsByPatenteIgnoreCase(camion.getPatente())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La patente ya esta registrada");
        }
        camion.setCapacidadKg(datos.getCapacidadKg());
        if (datos.getEstado() != null) {
            camion.setEstado(datos.getEstado());
        }
        camion.setConductorId(datos.getConductorId());

        Camion guardado = guardarCamion(camion);
        return new CamionResponse(guardado.getId(), guardado.getPatente(),
                guardado.getCapacidadKg(), guardado.getEstado(), guardado.getConductorId());
    }

    public List<CamionResponse> listar() {
        return crearRespuestas(repository.findAll());
    }

    public List<CamionResponse> buscarPorEstado(EstadoCamion estado) {
        return crearRespuestas(repository.findByEstado(estado));
    }

    public List<CamionResponse> buscarPorConductor(Long conductorId) {
        if (conductorId == null || conductorId <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El ID del conductor debe ser mayor que cero");
        }
        return crearRespuestas(repository.findByConductorId(conductorId));
    }

    public CamionResponse buscarPorId(Long id) {
        Optional<Camion> resultado = repository.findById(id);
        if (resultado.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Camion no encontrado");
        }

        Camion camion = resultado.get();
        return new CamionResponse(camion.getId(), camion.getPatente(),
                camion.getCapacidadKg(), camion.getEstado(), camion.getConductorId());
    }

    public CamionResponse actualizar(Long id, CamionRequest datos) {
        validarDatos(datos);

        Optional<Camion> resultado = repository.findById(id);
        if (resultado.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Camion no encontrado");
        }

        Camion camion = resultado.get();
        String patente = datos.getPatente().trim().toUpperCase(Locale.ROOT);
        if (repository.existsByPatenteIgnoreCaseAndIdNot(patente, id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La patente ya esta registrada");
        }
        camion.setPatente(patente);
        camion.setCapacidadKg(datos.getCapacidadKg());
        if (datos.getEstado() != null) {
            camion.setEstado(datos.getEstado());
        }
        camion.setConductorId(datos.getConductorId());

        Camion guardado = guardarCamion(camion);
        return new CamionResponse(guardado.getId(), guardado.getPatente(),
                guardado.getCapacidadKg(), guardado.getEstado(), guardado.getConductorId());
    }

    public void eliminar(Long id) {
        Optional<Camion> resultado = repository.findById(id);
        if (resultado.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Camion no encontrado");
        }
        repository.delete(resultado.get());
    }

    private Camion guardarCamion(Camion camion) {
        try {
            return repository.save(camion);
        } catch (DataIntegrityViolationException error) {
            // PostgreSQL usa 23505 cuando se repite un valor unico.
            Throwable causa = error.getMostSpecificCause();
            if (causa instanceof SQLException) {
                SQLException errorSql = (SQLException) causa;
                if ("23505".equals(errorSql.getSQLState())) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT,
                            "La patente ya esta registrada");
                }
            }
            throw error;
        }
    }

    private void validarDatos(CamionRequest datos) {
        if (datos.getPatente() == null || datos.getPatente().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La patente es obligatoria");
        }
        if (datos.getCapacidadKg() == null || !Double.isFinite(datos.getCapacidadKg())
                || datos.getCapacidadKg() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La capacidad debe ser un numero mayor que cero");
        }
    }

    private List<CamionResponse> crearRespuestas(List<Camion> camiones) {
        List<CamionResponse> respuestas = new ArrayList<>();
        for (Camion camion : camiones) {
            respuestas.add(new CamionResponse(camion.getId(), camion.getPatente(),
                    camion.getCapacidadKg(), camion.getEstado(), camion.getConductorId()));
        }
        return respuestas;
    }
}
