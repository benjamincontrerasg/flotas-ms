package com.duoc.flotas_ms.dto;

import com.duoc.flotas_ms.model.EstadoCamion;

public class CamionRequest {

    private String patente;
    private Double capacidadKg;
    private EstadoCamion estado;
    private Long conductorId;

    public CamionRequest() {
    }

    public String getPatente() {
        return patente;
    }

    public void setPatente(String patente) {
        this.patente = patente;
    }

    public Double getCapacidadKg() {
        return capacidadKg;
    }

    public void setCapacidadKg(Double capacidadKg) {
        this.capacidadKg = capacidadKg;
    }

    public EstadoCamion getEstado() {
        return estado;
    }

    public void setEstado(EstadoCamion estado) {
        this.estado = estado;
    }

    public Long getConductorId() {
        return conductorId;
    }

    public void setConductorId(Long conductorId) {
        this.conductorId = conductorId;
    }
}
