package com.duoc.flotas_ms.dto;

import com.duoc.flotas_ms.model.EstadoCamion;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CamionRequestTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void prepararValidador() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void cerrarValidador() {
        factory.close();
    }

    @Test
    void aceptaCapacidadDecimalPositivaConEstadoYConductorOpcionales() {
        assertTrue(validator.validate(datos("ABCD12", 0.5, null)).isEmpty());
    }

    @Test
    void aceptaPatenteDeDiezCaracteresDespuesDeNormalizar() {
        assertTrue(validator.validate(datos(" abcdef1234 ", 2000.0, null)).isEmpty());
    }

    @Test
    void rechazaPatentesNulasVaciasOConSoloEspacios() {
        String[] patentes = {null, "", " "};
        for (String patente : patentes) {
            assertTrue(tieneError(datos(patente, 2000.0, null), "patente"));
        }
    }

    @Test
    void rechazaPatenteQueSuperaLaLongitudDeLaTabla() {
        assertTrue(tieneError(datos("ABCDEFGHIJK", 2000.0, null), "patente"));
    }

    @Test
    void compruebaLaLongitudDespuesDeConvertirAMayusculas() {
        assertTrue(tieneError(datos("\u00df".repeat(6), 2000.0, null), "patente"));
    }

    @Test
    void rechazaCapacidadAusenteCeroONegativa() {
        Double[] capacidades = {null, 0.0, -1.0};
        for (Double capacidad : capacidades) {
            assertTrue(tieneError(datos("ABCD12", capacidad, null), "capacidadKg"));
        }
    }

    @Test
    void rechazaConductorCeroONegativo() {
        Long[] conductores = {0L, -1L};
        for (Long conductor : conductores) {
            assertTrue(tieneError(datos("ABCD12", 2000.0, conductor), "conductorId"));
        }
    }

    @Test
    void aceptaLosTresEstadosConUnConductorPositivo() {
        for (EstadoCamion estado : EstadoCamion.values()) {
            CamionRequest datos = datos("ABCD12", 2000.0, 3L);
            datos.setEstado(estado);
            assertTrue(validator.validate(datos).isEmpty());
        }
    }

    private boolean tieneError(CamionRequest datos, String campo) {
        for (ConstraintViolation<CamionRequest> error : validator.validate(datos)) {
            if (error.getPropertyPath().toString().equals(campo)) {
                return true;
            }
        }
        return false;
    }

    private CamionRequest datos(String patente, Double capacidad, Long conductor) {
        CamionRequest datos = new CamionRequest();
        datos.setPatente(patente);
        datos.setCapacidadKg(capacidad);
        datos.setConductorId(conductor);
        return datos;
    }
}
