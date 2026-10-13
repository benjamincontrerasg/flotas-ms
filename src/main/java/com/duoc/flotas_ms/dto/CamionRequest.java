package com.duoc.flotas_ms.dto;

import com.duoc.flotas_ms.model.EstadoCamion;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.Locale;

public class CamionRequest {

    @NotBlank(message = "La patente es obligatoria")
    @Size(max = 10, message = "La patente no puede superar 10 caracteres")
    private String patente;
    @NotNull(message = "La capacidad es obligatoria")
    @Positive(message = "La capacidad debe ser mayor que cero")
    private Double capacidadKg;
    private EstadoCamion estado;
    @Positive(message = "El ID del conductor debe ser mayor que cero")
    private Long conductorId;

    public CamionRequest() {
    }

    public String getPatente() {
        return patente;
    }

    public void setPatente(String patente) {
        this.patente = patente == null ? null : patente.trim().toUpperCase(Locale.ROOT);
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
