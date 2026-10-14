package com.duoc.flotas_ms.integration;

import com.duoc.flotas_ms.model.Camion;
import com.duoc.flotas_ms.model.EstadoCamion;
import com.duoc.flotas_ms.repository.CamionRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("integration")
class CamionIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private CamionRepository repository;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void prepararDatos() {
        comprobarBaseDePruebas();
        repository.deleteAll();
    }

    @AfterEach
    void limpiarDatos() {
        comprobarBaseDePruebas();
        repository.deleteAll();
    }

    @Test
    void crearNormalizaPatenteGeneraIdYGuardaDatosEnPostgreSQL() throws Exception {
        mvc.perform(post("/camiones").contentType(MediaType.APPLICATION_JSON).content("""
                {"id":987654321,"patente":" abcd12 ","capacidadKg":2750.5}
                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.patente").value("ABCD12"))
                .andExpect(jsonPath("$.capacidadKg").value(2750.5))
                .andExpect(jsonPath("$.estado").value("DISPONIBLE"))
                .andExpect(jsonPath("$.conductorId").value(nullValue()));

        assertEquals(1, repository.count());
        Camion guardado = repository.findAll().get(0);
        assertNotEquals(987654321L, guardado.getId());
        assertEquals("ABCD12", jdbc.queryForObject(
                "SELECT patente FROM camiones WHERE id = ?", String.class, guardado.getId()));
        assertEquals(2750.5, guardado.getCapacidadKg());
        assertEquals(EstadoCamion.DISPONIBLE, guardado.getEstado());
        assertNull(guardado.getConductorId());
    }

    @Test
    void crearConEstadoYConductorIndicadosLosGuarda() throws Exception {
        mvc.perform(post("/camiones").contentType(MediaType.APPLICATION_JSON).content("""
                {"patente":"ABCD12","capacidadKg":2000,"estado":"EN_RUTA","conductorId":3}
                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("EN_RUTA"))
                .andExpect(jsonPath("$.conductorId").value(3));

        Camion guardado = repository.findAll().get(0);
        assertEquals(EstadoCamion.EN_RUTA, guardado.getEstado());
        assertEquals(3L, guardado.getConductorId());
    }

    @Test
    void listarSinRegistrosDevuelveListaVacia() throws Exception {
        mvc.perform(get("/camiones"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void listarRecuperaLosRegistrosSinDependerDelOrden() throws Exception {
        guardar("ABCD12", EstadoCamion.DISPONIBLE, null);
        guardar("EFGH34", EstadoCamion.EN_RUTA, 3L);

        mvc.perform(get("/camiones"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].patente", containsInAnyOrder("ABCD12", "EFGH34")));
    }

    @Test
    void filtrarPorEstadoDevuelveSoloLosCamionesDeEseEstado() throws Exception {
        guardar("ABCD12", EstadoCamion.DISPONIBLE, null);
        guardar("EFGH34", EstadoCamion.EN_RUTA, 3L);
        guardar("IJKL56", EstadoCamion.MANTENCION, null);

        mvc.perform(get("/camiones/estado/DISPONIBLE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].patente").value("ABCD12"));
    }

    @Test
    void filtrarPorEstadoSinCoincidenciasDevuelveListaVacia() throws Exception {
        guardar("ABCD12", EstadoCamion.EN_RUTA, 3L);
        mvc.perform(get("/camiones/estado/MANTENCION"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void filtrarPorConductorRecuperaSoloSusCamiones() throws Exception {
        guardar("ABCD12", EstadoCamion.EN_RUTA, 3L);
        guardar("EFGH34", EstadoCamion.DISPONIBLE, 3L);
        guardar("IJKL56", EstadoCamion.DISPONIBLE, 4L);

        mvc.perform(get("/camiones/conductor/3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].patente", containsInAnyOrder("ABCD12", "EFGH34")));
    }

    @Test
    void filtrarPorConductorSinCoincidenciasDevuelveListaVacia() throws Exception {
        guardar("ABCD12", EstadoCamion.DISPONIBLE, null);
        mvc.perform(get("/camiones/conductor/99"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void consultarPorIdDevuelveLosDatosGuardados() throws Exception {
        Camion camion = guardar("ABCD12", EstadoCamion.MANTENCION, 3L);
        mvc.perform(get("/camiones/{id}", camion.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(camion.getId()))
                .andExpect(jsonPath("$.patente").value("ABCD12"))
                .andExpect(jsonPath("$.capacidadKg").value(2000.0))
                .andExpect(jsonPath("$.estado").value("MANTENCION"))
                .andExpect(jsonPath("$.conductorId").value(3));
    }

    @Test
    void consultarIdInexistenteDevuelve404ConFormatoUniforme() throws Exception {
        mvc.perform(get("/camiones/{id}", Long.MAX_VALUE))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.estado").value(404))
                .andExpect(jsonPath("$.mensaje").value("Camion no encontrado"))
                .andExpect(jsonPath("$.detalles", hasSize(0)));
    }

    @Test
    void actualizarGuardaCambiosYConservaElId() throws Exception {
        Camion camion = guardar("ABCD12", EstadoCamion.EN_RUTA, 3L);
        mvc.perform(put("/camiones/{id}", camion.getId())
                .contentType(MediaType.APPLICATION_JSON).content("""
                {"id":987654321,"patente":" efgh34 ","capacidadKg":3500.5,"estado":"MANTENCION","conductorId":4}
                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(camion.getId()))
                .andExpect(jsonPath("$.patente").value("EFGH34"));

        Camion actualizado = repository.findById(camion.getId()).orElseThrow();
        assertEquals("EFGH34", actualizado.getPatente());
        assertEquals(3500.5, jdbc.queryForObject(
                "SELECT capacidad_kg FROM camiones WHERE id = ?", Double.class, camion.getId()));
        assertEquals(EstadoCamion.MANTENCION, actualizado.getEstado());
        assertEquals(4L, actualizado.getConductorId());
        assertEquals(1, repository.count());
    }

    @Test
    void actualizarPermitePatentePropiaConservaEstadoYQuitaConductorOmitido() throws Exception {
        Camion camion = guardar("ABCD12", EstadoCamion.EN_RUTA, 3L);
        mvc.perform(put("/camiones/{id}", camion.getId())
                .contentType(MediaType.APPLICATION_JSON).content("""
                {"patente":" abcd12 ","capacidadKg":3000}
                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("EN_RUTA"))
                .andExpect(jsonPath("$.conductorId").value(nullValue()));

        Camion actualizado = repository.findById(camion.getId()).orElseThrow();
        assertEquals(EstadoCamion.EN_RUTA, actualizado.getEstado());
        assertNull(actualizado.getConductorId());
        assertEquals(3000.0, actualizado.getCapacidadKg());
    }

    @Test
    void actualizarIdInexistenteDevuelve404SinCrearRegistros() throws Exception {
        mvc.perform(put("/camiones/{id}", Long.MAX_VALUE)
                .contentType(MediaType.APPLICATION_JSON).content("""
                {"patente":"ABCD12","capacidadKg":2000}
                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.estado").value(404));
        assertEquals(0, repository.count());
    }

    @Test
    void eliminarRetiraSoloElRegistroIndicadoYDevuelve204() throws Exception {
        Camion camion = guardar("ABCD12", EstadoCamion.DISPONIBLE, null);
        Camion otro = guardar("EFGH34", EstadoCamion.DISPONIBLE, null);
        mvc.perform(delete("/camiones/{id}", camion.getId()))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        assertEquals(0, jdbc.queryForObject(
                "SELECT count(*) FROM camiones WHERE id = ?", Integer.class, camion.getId()));
        assertTrue(repository.existsById(otro.getId()));
        mvc.perform(get("/camiones/{id}", camion.getId())).andExpect(status().isNotFound());
    }

    @Test
    void eliminarIdInexistenteDevuelve404() throws Exception {
        mvc.perform(delete("/camiones/{id}", Long.MAX_VALUE))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensaje").value("Camion no encontrado"));
        assertEquals(0, repository.count());
    }

    @Test
    void crearConDatosInvalidosDevuelveDetallesYNoInserta() throws Exception {
        mvc.perform(post("/camiones").contentType(MediaType.APPLICATION_JSON).content("""
                {"patente":" ","capacidadKg":0,"conductorId":-1}
                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("Datos de entrada invalidos"))
                .andExpect(jsonPath("$.detalles", hasItems("patente: La patente es obligatoria",
                        "capacidadKg: La capacidad debe ser mayor que cero",
                        "conductorId: El ID del conductor debe ser mayor que cero")));
        assertEquals(0, repository.count());
    }

    @Test
    void actualizarConDatosInvalidosNoModificaElRegistro() throws Exception {
        Camion camion = guardar("ABCD12", EstadoCamion.EN_RUTA, 3L);
        mvc.perform(put("/camiones/{id}", camion.getId())
                .contentType(MediaType.APPLICATION_JSON).content("""
                {"patente":"ABCDEFGHIJK","capacidadKg":9000,"conductorId":0}
                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalles", hasItem("patente: La patente no puede superar 10 caracteres")));

        Camion sinCambios = repository.findById(camion.getId()).orElseThrow();
        assertEquals("ABCD12", sinCambios.getPatente());
        assertEquals(2000.0, sinCambios.getCapacidadKg());
        assertEquals(EstadoCamion.EN_RUTA, sinCambios.getEstado());
        assertEquals(3L, sinCambios.getConductorId());
    }

    @Test
    void crearPatenteDuplicadaIgnoraMayusculasYNoInsertaOtraFila() throws Exception {
        guardar("abcd12", EstadoCamion.DISPONIBLE, null);
        mvc.perform(post("/camiones").contentType(MediaType.APPLICATION_JSON).content("""
                {"patente":" ABCD12 ","capacidadKg":2000}
                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.estado").value(409))
                .andExpect(jsonPath("$.mensaje").value("La patente ya esta registrada"));
        assertEquals(1, repository.count());
    }

    @Test
    void actualizarPatenteAjenaDevuelve409SinCambiarDatos() throws Exception {
        guardar("ABCD12", EstadoCamion.DISPONIBLE, null);
        Camion camion = guardar("EFGH34", EstadoCamion.EN_RUTA, 3L);
        mvc.perform(put("/camiones/{id}", camion.getId())
                .contentType(MediaType.APPLICATION_JSON).content("""
                {"patente":" abcd12 ","capacidadKg":9000,"estado":"MANTENCION"}
                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value("La patente ya esta registrada"));

        Camion sinCambios = repository.findById(camion.getId()).orElseThrow();
        assertEquals("EFGH34", sinCambios.getPatente());
        assertEquals(2000.0, sinCambios.getCapacidadKg());
        assertEquals(EstadoCamion.EN_RUTA, sinCambios.getEstado());
        assertEquals(3L, sinCambios.getConductorId());
        assertEquals(2, repository.count());
    }

    @Test
    void jsonIncorrectoEIdInvalidoDevuelven400Uniforme() throws Exception {
        mvc.perform(post("/camiones").contentType(MediaType.APPLICATION_JSON).content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.estado").value(400))
                .andExpect(jsonPath("$.mensaje").value("El cuerpo debe contener un JSON valido con los campos esperados"));
        mvc.perform(get("/camiones/no-numero"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("El parametro de la solicitud tiene un formato invalido"));
        assertEquals(0, repository.count());
    }

    @Test
    void estadosDesconocidosYConductoresInvalidosDevuelven400() throws Exception {
        mvc.perform(post("/camiones").contentType(MediaType.APPLICATION_JSON).content("""
                {"patente":"ABCD12","capacidadKg":2000,"estado":"INVALIDO"}
                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.estado").value(400));
        mvc.perform(get("/camiones/estado/INVALIDO")).andExpect(status().isBadRequest());
        mvc.perform(get("/camiones/conductor/no-numero")).andExpect(status().isBadRequest());
        mvc.perform(get("/camiones/conductor/0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("El ID del conductor debe ser mayor que cero"));
        assertEquals(0, repository.count());
    }

    @Test
    void capacidadesNoFinitasDevuelven400SinGuardar() throws Exception {
        for (String capacidad : new String[]{"Infinity", "NaN"}) {
            mvc.perform(post("/camiones").contentType(MediaType.APPLICATION_JSON)
                    .content("{\"patente\":\"ABCD12\",\"capacidadKg\":\"" + capacidad + "\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.estado").value(400));
        }
        assertEquals(0, repository.count());
    }

    @Test
    void erroresDeMetodoContenidoYRecursoConservanSusEstados() throws Exception {
        Camion camion = guardar("ABCD12", EstadoCamion.DISPONIBLE, null);
        mvc.perform(patch("/camiones/{id}", camion.getId()))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(header().string("Allow", allOf(containsString("GET"), containsString("PUT"), containsString("DELETE"))))
                .andExpect(jsonPath("$.estado").value(405));
        mvc.perform(post("/camiones").contentType(MediaType.TEXT_PLAIN).content("texto"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.estado").value(415));
        mvc.perform(get("/recurso-inexistente"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensaje").value("Recurso no encontrado"));
        assertEquals(1, repository.count());
    }

    @Test
    void falloDePostgreSQLDevuelve500SinExponerLaExcepcion() throws Exception {
        // PostgreSQL rechaza NUL dentro de texto, sin alterar el esquema.
        mvc.perform(post("/camiones").contentType(MediaType.APPLICATION_JSON)
                .content("{\"patente\":\"A\\u0000BCD\",\"capacidadKg\":1000}"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.estado").value(500))
                .andExpect(jsonPath("$.mensaje").value("Error interno del servidor"))
                .andExpect(jsonPath("$.detalles", hasSize(0)))
                .andExpect(content().string(not(containsString("SQLException"))));
        assertEquals(0, repository.count());
    }

    @Test
    void postgresImpideDuplicadosGuardadosDirectamente() {
        guardar("ABCD12", EstadoCamion.DISPONIBLE, null);
        Camion duplicado = new Camion();
        duplicado.setPatente("ABCD12");
        duplicado.setCapacidadKg(2000.0);

        assertThrows(DataIntegrityViolationException.class, () -> repository.saveAndFlush(duplicado));
        assertEquals(1, repository.count());
    }

    private void comprobarBaseDePruebas() {
        assertEquals("flotas_test_db", jdbc.queryForObject("SELECT current_database()", String.class));
        assertEquals("flotas_test_user", jdbc.queryForObject("SELECT current_user", String.class));
    }

    private Camion guardar(String patente, EstadoCamion estado, Long conductorId) {
        Camion camion = new Camion();
        camion.setPatente(patente);
        camion.setCapacidadKg(2000.0);
        camion.setEstado(estado);
        camion.setConductorId(conductorId);
        return repository.saveAndFlush(camion);
    }
}
