package com.utp.urbanalert.service;

import com.utp.urbanalert.dto.HistorialResponse;
import com.utp.urbanalert.dto.IncidenciaActualizacionRequest;
import com.utp.urbanalert.dto.IncidenciaFiltro;
import com.utp.urbanalert.dto.IncidenciaRequest;
import com.utp.urbanalert.dto.IncidenciaResponse;
import com.utp.urbanalert.exception.BusinessValidationException;
import com.utp.urbanalert.exception.ResourceNotFoundException;
import com.utp.urbanalert.mapper.IncidenciaMapper;
import com.utp.urbanalert.model.AreaResponsable;
import com.utp.urbanalert.model.CategoriaIncidencia;
import com.utp.urbanalert.model.Ciudadano;
import com.utp.urbanalert.model.EstadoIncidencia;
import com.utp.urbanalert.model.HistorialIncidencia;
import com.utp.urbanalert.model.Incidencia;
import com.utp.urbanalert.model.NombreRol;
import com.utp.urbanalert.model.PrioridadIncidencia;
import com.utp.urbanalert.repository.AreaResponsableRepository;
import com.utp.urbanalert.repository.CiudadanoRepository;
import com.utp.urbanalert.repository.HistorialIncidenciaRepository;
import com.utp.urbanalert.repository.IncidenciaRepository;
import com.utp.urbanalert.security.UsuarioPrincipal;
import com.utp.urbanalert.service.impl.IncidenciaServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pruebas unitarias de IncidenciaServiceImpl. Los repositorios se reemplazan
 * por mocks de Mockito (posible gracias a la inyeccion de dependencias por
 * constructor), de modo que solo se prueba la logica de negocio:
 * alcance por rol, flujo de estados y registro de historial.
 */
@ExtendWith(MockitoExtension.class)
class IncidenciaServiceTest {

    @Mock
    private IncidenciaRepository incidenciaRepository;
    @Mock
    private CiudadanoRepository ciudadanoRepository;
    @Mock
    private AreaResponsableRepository areaResponsableRepository;
    @Mock
    private HistorialIncidenciaRepository historialRepository;
    @Spy
    private IncidenciaMapper mapper = new IncidenciaMapper();

    @InjectMocks
    private IncidenciaServiceImpl service;

    private Ciudadano maria;
    private AreaResponsable alumbrado;
    private AreaResponsable pistas;

    @BeforeEach
    void setUp() {
        maria = new Ciudadano("Maria Fernandez", "45678912", "maria@mail.com", "987654321");
        maria.setId(10L);
        alumbrado = new AreaResponsable("Alumbrado Publico", "Postes y luminarias");
        alumbrado.setId(1L);
        pistas = new AreaResponsable("Pistas y Veredas", "Bacheo y veredas");
        pistas.setId(2L);
    }

    // ------------------------------------------------------------ utilidades

    private UsuarioPrincipal admin() {
        return new UsuarioPrincipal(1L, "admin", "x", true, null, null, Set.of(NombreRol.ADMIN), Set.of());
    }

    private UsuarioPrincipal operador(Long areaId) {
        return new UsuarioPrincipal(2L, "op.alumbrado", "x", true, null, areaId, Set.of(NombreRol.OPERADOR), Set.of());
    }

    private UsuarioPrincipal ciudadano(Long ciudadanoId) {
        return new UsuarioPrincipal(3L, "maria", "x", true, ciudadanoId, null, Set.of(NombreRol.CIUDADANO), Set.of());
    }

    private Incidencia incidencia(Long id, AreaResponsable area, EstadoIncidencia estado) {
        Incidencia i = new Incidencia("Poste apagado", "Sin luz hace una semana",
                CategoriaIncidencia.ALUMBRADO_PUBLICO, PrioridadIncidencia.MEDIA, "Jr. Las Flores 123", maria);
        i.setId(id);
        i.setAreaResponsable(area);
        i.setEstado(estado);
        return i;
    }

    private IncidenciaRequest requestValido() {
        IncidenciaRequest request = new IncidenciaRequest();
        request.setTitulo("  Poste de luz apagado  ");
        request.setDescripcion("El poste de la cuadra 3 lleva una semana sin funcionar");
        request.setCategoria(CategoriaIncidencia.ALUMBRADO_PUBLICO);
        request.setPrioridad(PrioridadIncidencia.MEDIA);
        request.setDireccionReferencial("Jr. Las Flores 123");
        return request;
    }

    private HistorialIncidencia historialGuardado() {
        ArgumentCaptor<HistorialIncidencia> captor = ArgumentCaptor.forClass(HistorialIncidencia.class);
        verify(historialRepository).save(captor.capture());
        return captor.getValue();
    }

    private void guardarDevuelveLoRecibido() {
        when(incidenciaRepository.save(any(Incidencia.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    // ------------------------------------------------------------ registrar

    @Test
    void registrar_comoCiudadano_usaSuPerfilEIgnoraCiudadanoIdYAreaDelRequest() {
        IncidenciaRequest request = requestValido();
        request.setCiudadanoId(99L); // un ciudadano no puede reportar a nombre de otro
        request.setAreaId(2L);       // ni derivar a un area
        when(ciudadanoRepository.findById(10L)).thenReturn(Optional.of(maria));
        when(incidenciaRepository.save(any(Incidencia.class))).thenAnswer(inv -> {
            Incidencia i = inv.getArgument(0);
            i.setId(1L);
            return i;
        });

        IncidenciaResponse respuesta = service.registrar(request, ciudadano(10L));

        assertThat(respuesta.getCiudadanoId()).isEqualTo(10L);
        assertThat(respuesta.getAreaResponsableId()).isNull();
        assertThat(respuesta.getEstado()).isEqualTo(EstadoIncidencia.REGISTRADA);
        assertThat(respuesta.getTitulo()).isEqualTo("Poste de luz apagado"); // sin espacios sobrantes
        verify(areaResponsableRepository, never()).findById(any());

        HistorialIncidencia historial = historialGuardado();
        assertThat(historial.getEstadoAnterior()).isNull();
        assertThat(historial.getEstadoNuevo()).isEqualTo(EstadoIncidencia.REGISTRADA);
        assertThat(historial.getRealizadoPor()).isEqualTo("maria");
    }

    @Test
    void registrar_comoOperadorSinCiudadanoId_lanzaBusinessValidationException() {
        assertThatThrownBy(() -> service.registrar(requestValido(), operador(1L)))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessageContaining("ciudadano");

        verify(incidenciaRepository, never()).save(any());
    }

    @Test
    void registrar_comoOperadorConCiudadanoInexistente_lanzaResourceNotFoundException() {
        IncidenciaRequest request = requestValido();
        request.setCiudadanoId(77L);
        when(ciudadanoRepository.findById(77L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.registrar(request, operador(1L)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("ciudadano");

        verify(incidenciaRepository, never()).save(any());
    }

    @Test
    void registrar_comoOperadorConCiudadanoYArea_asignaElArea() {
        IncidenciaRequest request = requestValido();
        request.setCiudadanoId(10L);
        request.setAreaId(1L);
        when(ciudadanoRepository.findById(10L)).thenReturn(Optional.of(maria));
        when(areaResponsableRepository.findById(1L)).thenReturn(Optional.of(alumbrado));
        guardarDevuelveLoRecibido();

        IncidenciaResponse respuesta = service.registrar(request, operador(1L));

        assertThat(respuesta.getAreaResponsableNombre()).isEqualTo("Alumbrado Publico");
        assertThat(respuesta.getCiudadanoNombre()).isEqualTo("Maria Fernandez");
    }

    // ------------------------------------------------------------ listar (alcance por rol)

    @Test
    void listar_comoCiudadano_fuerzaElFiltroPorSuPropioPerfil() {
        IncidenciaFiltro filtro = new IncidenciaFiltro();
        filtro.setCiudadanoId(99L); // intento de ver las de otro ciudadano
        when(incidenciaRepository.buscar(any(IncidenciaFiltro.class))).thenReturn(List.of());

        service.listar(filtro, ciudadano(10L));

        ArgumentCaptor<IncidenciaFiltro> captor = ArgumentCaptor.forClass(IncidenciaFiltro.class);
        verify(incidenciaRepository).buscar(captor.capture());
        assertThat(captor.getValue().getCiudadanoId()).isEqualTo(10L);
    }

    @Test
    void listar_comoOperador_fuerzaElFiltroPorSuArea() {
        IncidenciaFiltro filtro = new IncidenciaFiltro();
        filtro.setAreaId(2L); // intento de ver otra area
        when(incidenciaRepository.buscar(any(IncidenciaFiltro.class))).thenReturn(List.of());

        service.listar(filtro, operador(1L));

        ArgumentCaptor<IncidenciaFiltro> captor = ArgumentCaptor.forClass(IncidenciaFiltro.class);
        verify(incidenciaRepository).buscar(captor.capture());
        assertThat(captor.getValue().getAreaId()).isEqualTo(1L);
    }

    @Test
    void listar_comoAdmin_respetaLosFiltrosSinRestriccionAdicional() {
        IncidenciaFiltro filtro = new IncidenciaFiltro();
        filtro.setAreaId(2L);
        filtro.setEstado(EstadoIncidencia.EN_PROCESO);
        when(incidenciaRepository.buscar(any(IncidenciaFiltro.class))).thenReturn(List.of());

        service.listar(filtro, admin());

        ArgumentCaptor<IncidenciaFiltro> captor = ArgumentCaptor.forClass(IncidenciaFiltro.class);
        verify(incidenciaRepository).buscar(captor.capture());
        assertThat(captor.getValue().getAreaId()).isEqualTo(2L);
        assertThat(captor.getValue().getCiudadanoId()).isNull();
        assertThat(captor.getValue().getEstado()).isEqualTo(EstadoIncidencia.EN_PROCESO);
    }

    @Test
    void listar_comoOperadorSinArea_devuelveListaVaciaSinConsultar() {
        List<IncidenciaResponse> resultado = service.listar(new IncidenciaFiltro(), operador(null));

        assertThat(resultado).isEmpty();
        verify(incidenciaRepository, never()).buscar(any());
    }

    @Test
    void listar_conRangoDeFechasInvertido_lanzaBusinessValidationException() {
        IncidenciaFiltro filtro = new IncidenciaFiltro();
        filtro.setDesde(LocalDate.of(2026, 10, 1));
        filtro.setHasta(LocalDate.of(2026, 9, 1));

        assertThatThrownBy(() -> service.listar(filtro, admin()))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessageContaining("desde");
    }

    // ------------------------------------------------------------ obtenerPorId (autorizacion a nivel de dato)

    @Test
    void obtenerPorId_ciudadanoConIncidenciaAjena_lanzaAccessDenied() {
        when(incidenciaRepository.findById(5L)).thenReturn(Optional.of(incidencia(5L, alumbrado, EstadoIncidencia.REGISTRADA)));

        assertThatThrownBy(() -> service.obtenerPorId(5L, ciudadano(11L)))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void obtenerPorId_operadorDeOtraArea_lanzaAccessDenied() {
        when(incidenciaRepository.findById(5L)).thenReturn(Optional.of(incidencia(5L, alumbrado, EstadoIncidencia.REGISTRADA)));

        assertThatThrownBy(() -> service.obtenerPorId(5L, operador(2L)))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void obtenerPorId_operadorSobreIncidenciaSinArea_lanzaAccessDenied() {
        when(incidenciaRepository.findById(5L)).thenReturn(Optional.of(incidencia(5L, null, EstadoIncidencia.REGISTRADA)));

        assertThatThrownBy(() -> service.obtenerPorId(5L, operador(1L)))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void obtenerPorId_operadorDeSuArea_devuelveLaIncidencia() {
        when(incidenciaRepository.findById(5L)).thenReturn(Optional.of(incidencia(5L, alumbrado, EstadoIncidencia.REGISTRADA)));

        IncidenciaResponse respuesta = service.obtenerPorId(5L, operador(1L));

        assertThat(respuesta.getId()).isEqualTo(5L);
    }

    @Test
    void obtenerPorId_inexistente_lanzaResourceNotFoundException() {
        when(incidenciaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.obtenerPorId(99L, admin()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ------------------------------------------------------------ cambiarEstado (flujo de estados)

    @Test
    void cambiarEstado_deRegistradaAEnProcesoConArea_actualizaYRegistraHistorial() {
        when(incidenciaRepository.findById(5L)).thenReturn(Optional.of(incidencia(5L, alumbrado, EstadoIncidencia.REGISTRADA)));
        guardarDevuelveLoRecibido();

        IncidenciaResponse respuesta = service.cambiarEstado(5L, EstadoIncidencia.EN_PROCESO, operador(1L));

        assertThat(respuesta.getEstado()).isEqualTo(EstadoIncidencia.EN_PROCESO);
        assertThat(respuesta.getFechaActualizacion()).isNotNull();
        HistorialIncidencia historial = historialGuardado();
        assertThat(historial.getEstadoAnterior()).isEqualTo(EstadoIncidencia.REGISTRADA);
        assertThat(historial.getEstadoNuevo()).isEqualTo(EstadoIncidencia.EN_PROCESO);
        assertThat(historial.getRealizadoPor()).isEqualTo("op.alumbrado");
    }

    @Test
    void cambiarEstado_aResuelta_fijaLaFechaDeResolucion() {
        when(incidenciaRepository.findById(5L)).thenReturn(Optional.of(incidencia(5L, alumbrado, EstadoIncidencia.EN_PROCESO)));
        guardarDevuelveLoRecibido();

        IncidenciaResponse respuesta = service.cambiarEstado(5L, EstadoIncidencia.RESUELTA, operador(1L));

        assertThat(respuesta.getEstado()).isEqualTo(EstadoIncidencia.RESUELTA);
        assertThat(respuesta.getFechaResolucion()).isNotNull();
    }

    @Test
    void cambiarEstado_reabrirUnaResuelta_limpiaLaFechaDeResolucion() {
        Incidencia resuelta = incidencia(5L, alumbrado, EstadoIncidencia.RESUELTA);
        resuelta.setFechaResolucion(LocalDateTime.now().minusHours(3));
        when(incidenciaRepository.findById(5L)).thenReturn(Optional.of(resuelta));
        guardarDevuelveLoRecibido();

        IncidenciaResponse respuesta = service.cambiarEstado(5L, EstadoIncidencia.EN_PROCESO, operador(1L));

        assertThat(respuesta.getEstado()).isEqualTo(EstadoIncidencia.EN_PROCESO);
        assertThat(respuesta.getFechaResolucion()).isNull();
    }

    @Test
    void cambiarEstado_aEnProcesoSinAreaAsignada_lanzaBusinessValidationException() {
        when(incidenciaRepository.findById(5L)).thenReturn(Optional.of(incidencia(5L, null, EstadoIncidencia.REGISTRADA)));

        assertThatThrownBy(() -> service.cambiarEstado(5L, EstadoIncidencia.EN_PROCESO, admin()))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessageContaining("area");

        verify(incidenciaRepository, never()).save(any());
    }

    @Test
    void cambiarEstado_conTransicionInvalida_lanzaBusinessValidationException() {
        when(incidenciaRepository.findById(5L)).thenReturn(Optional.of(incidencia(5L, alumbrado, EstadoIncidencia.REGISTRADA)));

        assertThatThrownBy(() -> service.cambiarEstado(5L, EstadoIncidencia.RESUELTA, operador(1L)))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessageContaining("Transicion");

        verify(incidenciaRepository, never()).save(any());
    }

    @Test
    void cambiarEstado_operadorIntentaCerrar_lanzaAccessDenied() {
        when(incidenciaRepository.findById(5L)).thenReturn(Optional.of(incidencia(5L, alumbrado, EstadoIncidencia.RESUELTA)));

        assertThatThrownBy(() -> service.cambiarEstado(5L, EstadoIncidencia.CERRADA, operador(1L)))
                .isInstanceOf(AccessDeniedException.class);

        verify(incidenciaRepository, never()).save(any());
    }

    @Test
    void cambiarEstado_adminCierraUnaResuelta_actualizaElEstado() {
        when(incidenciaRepository.findById(5L)).thenReturn(Optional.of(incidencia(5L, alumbrado, EstadoIncidencia.RESUELTA)));
        guardarDevuelveLoRecibido();

        IncidenciaResponse respuesta = service.cambiarEstado(5L, EstadoIncidencia.CERRADA, admin());

        assertThat(respuesta.getEstado()).isEqualTo(EstadoIncidencia.CERRADA);
    }

    @Test
    void cambiarEstado_deUnaIncidenciaCerrada_noPermiteNingunCambio() {
        when(incidenciaRepository.findById(5L)).thenReturn(Optional.of(incidencia(5L, alumbrado, EstadoIncidencia.CERRADA)));

        assertThatThrownBy(() -> service.cambiarEstado(5L, EstadoIncidencia.EN_PROCESO, admin()))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessageContaining("CERRADA");
    }

    // ------------------------------------------------------------ asignarArea

    @Test
    void asignarArea_valida_derivaYRegistraElCambioEnElHistorial() {
        when(incidenciaRepository.findById(5L)).thenReturn(Optional.of(incidencia(5L, null, EstadoIncidencia.REGISTRADA)));
        when(areaResponsableRepository.findById(2L)).thenReturn(Optional.of(pistas));
        guardarDevuelveLoRecibido();

        IncidenciaResponse respuesta = service.asignarArea(5L, 2L, admin());

        assertThat(respuesta.getAreaResponsableNombre()).isEqualTo("Pistas y Veredas");
        assertThat(historialGuardado().getDetalle()).contains("sin asignar").contains("Pistas y Veredas");
    }

    @Test
    void asignarArea_conAreaInexistente_lanzaResourceNotFoundException() {
        when(incidenciaRepository.findById(5L)).thenReturn(Optional.of(incidencia(5L, null, EstadoIncidencia.REGISTRADA)));
        when(areaResponsableRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.asignarArea(5L, 99L, admin()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("area");

        verify(incidenciaRepository, never()).save(any());
    }

    @Test
    void asignarArea_aUnaIncidenciaCerrada_lanzaBusinessValidationException() {
        when(incidenciaRepository.findById(5L)).thenReturn(Optional.of(incidencia(5L, alumbrado, EstadoIncidencia.CERRADA)));

        assertThatThrownBy(() -> service.asignarArea(5L, 2L, admin()))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessageContaining("CERRADA");
    }

    // ------------------------------------------------------------ actualizar

    private IncidenciaActualizacionRequest actualizacion() {
        IncidenciaActualizacionRequest request = new IncidenciaActualizacionRequest();
        request.setTitulo("Poste apagado en la cuadra 3");
        request.setDescripcion("Ahora tambien parpadea el poste vecino");
        request.setCategoria(CategoriaIncidencia.ALUMBRADO_PUBLICO);
        request.setPrioridad(PrioridadIncidencia.ALTA);
        request.setDireccionReferencial("Jr. Las Flores 125");
        return request;
    }

    @Test
    void actualizar_ciudadanoMientrasEstaRegistrada_modificaLosDatos() {
        when(incidenciaRepository.findById(5L)).thenReturn(Optional.of(incidencia(5L, null, EstadoIncidencia.REGISTRADA)));
        guardarDevuelveLoRecibido();

        IncidenciaResponse respuesta = service.actualizar(5L, actualizacion(), ciudadano(10L));

        assertThat(respuesta.getTitulo()).isEqualTo("Poste apagado en la cuadra 3");
        assertThat(respuesta.getPrioridad()).isEqualTo(PrioridadIncidencia.ALTA);
        assertThat(respuesta.getEstado()).isEqualTo(EstadoIncidencia.REGISTRADA);
    }

    @Test
    void actualizar_ciudadanoCuandoYaEstaEnProceso_lanzaBusinessValidationException() {
        when(incidenciaRepository.findById(5L)).thenReturn(Optional.of(incidencia(5L, alumbrado, EstadoIncidencia.EN_PROCESO)));

        assertThatThrownBy(() -> service.actualizar(5L, actualizacion(), ciudadano(10L)))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessageContaining("REGISTRADA");

        verify(incidenciaRepository, never()).save(any());
    }

    @Test
    void actualizar_incidenciaCerrada_lanzaBusinessValidationException() {
        when(incidenciaRepository.findById(5L)).thenReturn(Optional.of(incidencia(5L, alumbrado, EstadoIncidencia.CERRADA)));

        assertThatThrownBy(() -> service.actualizar(5L, actualizacion(), admin()))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessageContaining("CERRADA");
    }

    // ------------------------------------------------------------ eliminar

    @Test
    void eliminar_ciudadanoConSuIncidenciaRegistrada_laElimina() {
        Incidencia registrada = incidencia(5L, null, EstadoIncidencia.REGISTRADA);
        when(incidenciaRepository.findById(5L)).thenReturn(Optional.of(registrada));

        service.eliminar(5L, ciudadano(10L));

        verify(incidenciaRepository).delete(registrada);
    }

    @Test
    void eliminar_ciudadanoConIncidenciaEnProceso_lanzaBusinessValidationException() {
        when(incidenciaRepository.findById(5L)).thenReturn(Optional.of(incidencia(5L, alumbrado, EstadoIncidencia.EN_PROCESO)));

        assertThatThrownBy(() -> service.eliminar(5L, ciudadano(10L)))
                .isInstanceOf(BusinessValidationException.class);

        verify(incidenciaRepository, never()).delete(any(Incidencia.class));
    }

    @Test
    void eliminar_ciudadanoConIncidenciaAjena_lanzaAccessDenied() {
        when(incidenciaRepository.findById(5L)).thenReturn(Optional.of(incidencia(5L, null, EstadoIncidencia.REGISTRADA)));

        assertThatThrownBy(() -> service.eliminar(5L, ciudadano(11L)))
                .isInstanceOf(AccessDeniedException.class);

        verify(incidenciaRepository, never()).delete(any(Incidencia.class));
    }

    @Test
    void eliminar_adminPuedeEliminarEnCualquierEstado() {
        Incidencia cerrada = incidencia(5L, alumbrado, EstadoIncidencia.CERRADA);
        when(incidenciaRepository.findById(5L)).thenReturn(Optional.of(cerrada));

        service.eliminar(5L, admin());

        verify(incidenciaRepository).delete(cerrada);
    }

    // ------------------------------------------------------------ historial

    @Test
    void obtenerHistorial_devuelveLasEntradasDeLaIncidencia() {
        Incidencia incidencia = incidencia(5L, alumbrado, EstadoIncidencia.EN_PROCESO);
        HistorialIncidencia alta = new HistorialIncidencia(incidencia, null, EstadoIncidencia.REGISTRADA, "Incidencia registrada", "maria");
        HistorialIncidencia atencion = new HistorialIncidencia(incidencia, EstadoIncidencia.REGISTRADA, EstadoIncidencia.EN_PROCESO, "Atencion iniciada", "op.alumbrado");
        when(incidenciaRepository.findById(5L)).thenReturn(Optional.of(incidencia));
        when(historialRepository.findByIncidenciaIdOrderByFechaAsc(5L)).thenReturn(List.of(alta, atencion));

        List<HistorialResponse> historial = service.obtenerHistorial(5L, admin());

        assertThat(historial).hasSize(2);
        assertThat(historial.get(0).getEstadoNuevo()).isEqualTo(EstadoIncidencia.REGISTRADA);
        assertThat(historial.get(1).getRealizadoPor()).isEqualTo("op.alumbrado");
    }
}
