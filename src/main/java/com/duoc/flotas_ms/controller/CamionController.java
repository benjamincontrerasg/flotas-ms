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
}
