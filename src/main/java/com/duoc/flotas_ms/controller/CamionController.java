package com.duoc.flotas_ms.controller;

import com.duoc.flotas_ms.dto.CamionRequest;
import com.duoc.flotas_ms.dto.CamionResponse;
import com.duoc.flotas_ms.service.CamionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.duoc.flotas_ms.model.EstadoCamion;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@RestController
@RequestMapping("/camiones")
public class CamionController {

    private final CamionService service;

    public CamionController(CamionService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<CamionResponse> crear(@RequestBody CamionRequest datos) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crear(datos));
    }

    @GetMapping
    public ResponseEntity<List<CamionResponse>> listar() {
        return ResponseEntity.ok(service.listar());
    }

    @GetMapping("/estado/{estado}")
    public ResponseEntity<List<CamionResponse>> buscarPorEstado(
            @PathVariable("estado") EstadoCamion estado) {
        return ResponseEntity.ok(service.buscarPorEstado(estado));
    }

    @GetMapping("/conductor/{conductorId}")
    public ResponseEntity<List<CamionResponse>> buscarPorConductor(
            @PathVariable("conductorId") Long conductorId) {
        return ResponseEntity.ok(service.buscarPorConductor(conductorId));
    }
}
