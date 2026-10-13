package com.duoc.flotas_ms.service;

import com.duoc.flotas_ms.dto.CamionRequest;
import com.duoc.flotas_ms.dto.CamionResponse;
import com.duoc.flotas_ms.model.Camion;
import com.duoc.flotas_ms.model.EstadoCamion;
import com.duoc.flotas_ms.repository.CamionRepository;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CamionServiceTest {

    @Mock
    private CamionRepository repository;

    @InjectMocks
    private CamionService service;

    @Test
    void crearNormalizaPatenteYUsaDisponiblePorDefecto() {
        Camion guardado = camion(7L, "ABCD12", EstadoCamion.DISPONIBLE, null);
        when(repository.save(any(Camion.class))).thenReturn(guardado);

        CamionResponse respuesta = service.crear(datos(" abcd12 ", 2000.0, null, null));

        ArgumentCaptor<Camion> captura = ArgumentCaptor.forClass(Camion.class);
        verify(repository).save(captura.capture());
        Camion enviado = captura.getValue();
        assertNull(enviado.getId());
        assertEquals("ABCD12", enviado.getPatente());
        assertEquals(2000.0, enviado.getCapacidadKg());
        assertEquals(EstadoCamion.DISPONIBLE, enviado.getEstado());
        assertNull(enviado.getConductorId());
        assertEquals(7L, respuesta.getId());
        verify(repository).existsByPatenteIgnoreCase("ABCD12");
    }

    @Test
    void crearConservaElEstadoYConductorIndicados() {
        Camion guardado = camion(8L, "ABCD12", EstadoCamion.EN_RUTA, 3L);
        when(repository.save(any(Camion.class))).thenReturn(guardado);

        CamionResponse respuesta = service.crear(datos("ABCD12", 2000.0, EstadoCamion.EN_RUTA, 3L));

        ArgumentCaptor<Camion> captura = ArgumentCaptor.forClass(Camion.class);
        verify(repository).save(captura.capture());
        assertEquals(EstadoCamion.EN_RUTA, captura.getValue().getEstado());
        assertEquals(3L, captura.getValue().getConductorId());
        assertEquals(EstadoCamion.EN_RUTA, respuesta.getEstado());
        assertEquals(3L, respuesta.getConductorId());
    }

    @Test
    void crearRechazaDuplicadosSinGuardar() {
        when(repository.existsByPatenteIgnoreCase("ABCD12")).thenReturn(true);

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.crear(datos(" abcd12 ", 2000.0, null, null)));

        assertEquals(409, error.getStatusCode().value());
        verify(repository, never()).save(any(Camion.class));
    }

    @Test
    void crearYActualizarRechazanDatosObligatoriosAntesDeConsultarLaBase() {
        CamionRequest[] casos = {
                datos(null, 1000.0, null, null),
                datos(" ", 1000.0, null, null),
                datos("ABCD12", null, null, null),
                datos("ABCD12", 0.0, null, null),
                datos("ABCD12", -1.0, null, null),
                datos("ABCD12", Double.NaN, null, null),
                datos("ABCD12", Double.POSITIVE_INFINITY, null, null),
                datos("ABCD12", Double.NEGATIVE_INFINITY, null, null)
        };
        for (CamionRequest caso : casos) {
            ResponseStatusException crear = assertThrows(ResponseStatusException.class,
                    () -> service.crear(caso));
            ResponseStatusException actualizar = assertThrows(ResponseStatusException.class,
                    () -> service.actualizar(7L, caso));
            assertEquals(400, crear.getStatusCode().value());
            assertEquals(400, actualizar.getStatusCode().value());
        }
        verifyNoInteractions(repository);
    }

    @Test
    void listarDevuelveLosDatosDeLosCamiones() {
        when(repository.findAll()).thenReturn(List.of(
                camion(7L, "ABCD12", EstadoCamion.DISPONIBLE, null),
                camion(8L, "EFGH34", EstadoCamion.EN_RUTA, 3L)));

        List<CamionResponse> respuesta = service.listar();

        assertEquals(2, respuesta.size());
        assertEquals(7L, respuesta.get(0).getId());
        assertEquals("ABCD12", respuesta.get(0).getPatente());
        assertEquals(2000.0, respuesta.get(0).getCapacidadKg());
        assertNull(respuesta.get(0).getConductorId());
        assertEquals(EstadoCamion.EN_RUTA, respuesta.get(1).getEstado());
        assertEquals(3L, respuesta.get(1).getConductorId());
    }

    @Test
    void listarSinRegistrosDevuelveListaVacia() {
        when(repository.findAll()).thenReturn(List.of());
        assertTrue(service.listar().isEmpty());
    }

    @Test
    void filtrarPorEstadoConsultaElEstadoIndicado() {
        when(repository.findByEstado(EstadoCamion.MANTENCION)).thenReturn(
                List.of(camion(9L, "IJKL56", EstadoCamion.MANTENCION, null)));

        List<CamionResponse> respuesta = service.buscarPorEstado(EstadoCamion.MANTENCION);

        assertEquals(1, respuesta.size());
        assertEquals(9L, respuesta.get(0).getId());
        assertEquals(EstadoCamion.MANTENCION, respuesta.get(0).getEstado());
        verify(repository).findByEstado(EstadoCamion.MANTENCION);
    }

    @Test
    void filtrarPorConductorConsultaElIdIndicado() {
        when(repository.findByConductorId(3L)).thenReturn(
                List.of(camion(8L, "EFGH34", EstadoCamion.EN_RUTA, 3L)));

        List<CamionResponse> respuesta = service.buscarPorConductor(3L);

        assertEquals(1, respuesta.size());
        assertEquals(8L, respuesta.get(0).getId());
        assertEquals(3L, respuesta.get(0).getConductorId());
        verify(repository).findByConductorId(3L);
    }

    @Test
    void filtrarRechazaConductoresInvalidosAntesDeConsultar() {
        Long[] ids = {null, 0L, -1L};
        for (Long id : ids) {
            ResponseStatusException error = assertThrows(ResponseStatusException.class,
                    () -> service.buscarPorConductor(id));
            assertEquals(400, error.getStatusCode().value());
        }
        verifyNoInteractions(repository);
    }

    @Test
    void consultarPorIdDevuelveElRegistroEncontrado() {
        when(repository.findById(7L)).thenReturn(
                Optional.of(camion(7L, "ABCD12", EstadoCamion.MANTENCION, 3L)));

        CamionResponse respuesta = service.buscarPorId(7L);

        assertEquals(7L, respuesta.getId());
        assertEquals("ABCD12", respuesta.getPatente());
        assertEquals(2000.0, respuesta.getCapacidadKg());
        assertEquals(EstadoCamion.MANTENCION, respuesta.getEstado());
        assertEquals(3L, respuesta.getConductorId());
    }

    @Test
    void consultarIdInexistenteDevuelve404() {
        when(repository.findById(99L)).thenReturn(Optional.empty());
        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.buscarPorId(99L));
        assertEquals(404, error.getStatusCode().value());
    }

    @Test
    void actualizarGuardaCambiosYConservaId() {
        Camion existente = camion(7L, "ABCD12", EstadoCamion.DISPONIBLE, null);
        when(repository.findById(7L)).thenReturn(Optional.of(existente));
        when(repository.save(any(Camion.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        CamionResponse respuesta = service.actualizar(7L,
                datos(" efgh34 ", 3500.5, EstadoCamion.MANTENCION, 4L));

        assertEquals(7L, respuesta.getId());
        assertEquals("EFGH34", respuesta.getPatente());
        assertEquals(3500.5, respuesta.getCapacidadKg());
        assertEquals(EstadoCamion.MANTENCION, respuesta.getEstado());
        assertEquals(4L, respuesta.getConductorId());
        verify(repository).save(existente);
    }

    @Test
    void actualizarPermitePatentePropiaConservaEstadoYQuitaConductorOmitido() {
        Camion existente = camion(7L, "ABCD12", EstadoCamion.EN_RUTA, 3L);
        when(repository.findById(7L)).thenReturn(Optional.of(existente));
        when(repository.save(any(Camion.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        CamionResponse respuesta = service.actualizar(7L, datos(" abcd12 ", 3000.0, null, null));

        assertEquals(7L, respuesta.getId());
        assertEquals("ABCD12", respuesta.getPatente());
        assertEquals(EstadoCamion.EN_RUTA, respuesta.getEstado());
        assertNull(respuesta.getConductorId());
        verify(repository).existsByPatenteIgnoreCaseAndIdNot("ABCD12", 7L);
    }

    @Test
    void actualizarPatenteAjenaDevuelve409SinCambiarElCamion() {
        Camion existente = camion(7L, "ABCD12", EstadoCamion.EN_RUTA, 3L);
        when(repository.findById(7L)).thenReturn(Optional.of(existente));
        when(repository.existsByPatenteIgnoreCaseAndIdNot("EFGH34", 7L)).thenReturn(true);

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.actualizar(7L, datos(" efgh34 ", 9999.0, EstadoCamion.MANTENCION, null)));

        assertEquals(409, error.getStatusCode().value());
        assertEquals("ABCD12", existente.getPatente());
        assertEquals(2000.0, existente.getCapacidadKg());
        assertEquals(EstadoCamion.EN_RUTA, existente.getEstado());
        assertEquals(3L, existente.getConductorId());
        verify(repository, never()).save(any(Camion.class));
    }

    @Test
    void actualizarIdInexistenteDevuelve404SinCrearUnCamion() {
        when(repository.findById(99L)).thenReturn(Optional.empty());
        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.actualizar(99L, datos("ABCD12", 2000.0, null, null)));
        assertEquals(404, error.getStatusCode().value());
        verify(repository, never()).save(any(Camion.class));
    }

    @Test
    void eliminarBorraElRegistroEncontrado() {
        Camion existente = camion(7L, "ABCD12", EstadoCamion.DISPONIBLE, null);
        when(repository.findById(7L)).thenReturn(Optional.of(existente));

        service.eliminar(7L);

        verify(repository).delete(existente);
    }

    @Test
    void eliminarIdInexistenteDevuelve404SinBorrar() {
        when(repository.findById(99L)).thenReturn(Optional.empty());
        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.eliminar(99L));
        assertEquals(404, error.getStatusCode().value());
        verify(repository, never()).delete(any(Camion.class));
    }

    @Test
    void conflictoUnicoDePostgreSQLDevuelve409AunquePaseLaConsultaPrevia() {
        DataIntegrityViolationException conflicto = new DataIntegrityViolationException(
                "Conflicto al guardar", new SQLException("Valor duplicado", "23505"));
        when(repository.save(any(Camion.class))).thenThrow(conflicto);

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.crear(datos("ABCD12", 2000.0, null, null)));

        assertEquals(409, error.getStatusCode().value());
        assertEquals("La patente ya esta registrada", error.getReason());
    }

    @Test
    void otrosErroresDePersistenciaSePropaganSinConvertirlosEnDuplicados() {
        DataIntegrityViolationException[] errores = {
                new DataIntegrityViolationException("Dato obligatorio", new SQLException("Nulo", "23502")),
                new DataIntegrityViolationException("Dato incorrecto", new SQLException("Codificacion", "22021")),
                new DataIntegrityViolationException("Error sin causa SQL")
        };
        for (DataIntegrityViolationException error : errores) {
            when(repository.save(any(Camion.class))).thenThrow(error);
            DataIntegrityViolationException recibido = assertThrows(DataIntegrityViolationException.class,
                    () -> service.crear(datos("ABCD12", 2000.0, null, null)));
            assertSame(error, recibido);
        }
    }

    private CamionRequest datos(String patente, Double capacidad, EstadoCamion estado, Long conductor) {
        CamionRequest datos = new CamionRequest();
        datos.setPatente(patente);
        datos.setCapacidadKg(capacidad);
        datos.setEstado(estado);
        datos.setConductorId(conductor);
        return datos;
    }

    private Camion camion(Long id, String patente, EstadoCamion estado, Long conductor) {
        Camion camion = new Camion();
        camion.setId(id);
        camion.setPatente(patente);
        camion.setCapacidadKg(2000.0);
        camion.setEstado(estado);
        camion.setConductorId(conductor);
        return camion;
    }
}
