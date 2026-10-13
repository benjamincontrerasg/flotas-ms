package com.duoc.flotas_ms.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "camiones")
public class Camion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 10)
    private String patente;

    @Column(name = "capacidad_kg", nullable = false)
    private Double capacidadKg;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoCamion estado = EstadoCamion.DISPONIBLE;

    @Column(name = "conductor_id")
    private Long conductorId;

    public Camion() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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
