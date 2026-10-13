package com.duoc.flotas_ms.dto;

import java.util.List;

public class RespuestaError {

    private int estado;
    private String mensaje;
    private List<String> detalles;

    public RespuestaError() {
    }

    public RespuestaError(int estado, String mensaje, List<String> detalles) {
        this.estado = estado;
        this.mensaje = mensaje;
        this.detalles = detalles;
    }

    public int getEstado() {
        return estado;
    }

    public void setEstado(int estado) {
        this.estado = estado;
    }

    public String getMensaje() {
        return mensaje;
    }

    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }

    public List<String> getDetalles() {
        return detalles;
    }

    public void setDetalles(List<String> detalles) {
        this.detalles = detalles;
    }
}
