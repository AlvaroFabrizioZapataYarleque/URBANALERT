package com.utp.urbanalert.config;

import com.utp.urbanalert.model.AreaResponsable;
import com.utp.urbanalert.model.CategoriaIncidencia;
import com.utp.urbanalert.model.Ciudadano;
import com.utp.urbanalert.model.EstadoIncidencia;
import com.utp.urbanalert.model.HistorialIncidencia;
import com.utp.urbanalert.model.Incidencia;
import com.utp.urbanalert.model.NombreRol;
import com.utp.urbanalert.model.Permiso;
import com.utp.urbanalert.model.PrioridadIncidencia;
import com.utp.urbanalert.model.Rol;
import com.utp.urbanalert.model.Usuario;
import com.utp.urbanalert.repository.AreaResponsableRepository;
import com.utp.urbanalert.repository.CiudadanoRepository;
import com.utp.urbanalert.repository.HistorialIncidenciaRepository;
import com.utp.urbanalert.repository.IncidenciaRepository;
import com.utp.urbanalert.repository.RolRepository;
import com.utp.urbanalert.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.Set;

/**
 * Carga los datos iniciales al arrancar la aplicacion:
 *  - Roles y permisos (datos de referencia indispensables).
 *  - Datos de ejemplo para probar en Postman (areas, usuarios, ciudadanos e
 *    incidencias con distintas fechas y estados).
 *
 * Es idempotente: si los roles ya existen (por ejemplo con MySQL en un segundo
 * arranque) no vuelve a insertar nada.
 *
 * Credenciales de prueba (solo desarrollo):
 *   admin            / Admin123*       (ADMIN)
 *   op.alumbrado     / Operador123*    (OPERADOR del area Alumbrado Publico)
 *   op.pistas        / Operador123*    (OPERADOR del area Pistas y Veredas)
 *   op.semaforos     / Operador123*    (OPERADOR del area Semaforizacion)
 *   maria, jose, lucia / Ciudadano123* (CIUDADANO)
 */
@Component
@RequiredArgsConstructor
public class DataLoader implements CommandLineRunner {

    private static final String CLAVE_ADMIN = "Admin123*";
    private static final String CLAVE_OPERADOR = "Operador123*";
    private static final String CLAVE_CIUDADANO = "Ciudadano123*";

    private final RolRepository rolRepository;
    private final UsuarioRepository usuarioRepository;
    private final CiudadanoRepository ciudadanoRepository;
    private final AreaResponsableRepository areaRepository;
    private final IncidenciaRepository incidenciaRepository;
    private final HistorialIncidenciaRepository historialRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (rolRepository.count() > 0) {
            return;
        }

        // ---------------- roles y permisos ----------------
        Rol admin = rolRepository.save(new Rol(NombreRol.ADMIN, EnumSet.allOf(Permiso.class)));
        Rol operador = rolRepository.save(new Rol(NombreRol.OPERADOR, Set.of(
                Permiso.INCIDENCIA_CREAR, Permiso.INCIDENCIA_VER, Permiso.INCIDENCIA_CAMBIAR_ESTADO,
                Permiso.CIUDADANO_GESTIONAR)));
        Rol ciudadanoRol = rolRepository.save(new Rol(NombreRol.CIUDADANO, Set.of(
                Permiso.INCIDENCIA_CREAR, Permiso.INCIDENCIA_VER, Permiso.INCIDENCIA_EDITAR,
                Permiso.INCIDENCIA_ELIMINAR)));

        // ---------------- areas ----------------
        AreaResponsable alumbrado = areaRepository.save(
                new AreaResponsable("Alumbrado Publico", "Atiende postes y luminarias"));
        AreaResponsable pistas = areaRepository.save(
                new AreaResponsable("Pistas y Veredas", "Atiende bacheo y veredas danadas"));
        AreaResponsable semaforos = areaRepository.save(
                new AreaResponsable("Semaforizacion", "Atiende semaforos malogrados"));
        AreaResponsable limpieza = areaRepository.save(
                new AreaResponsable("Limpieza Publica", "Atiende acumulacion de residuos"));
        AreaResponsable senalizacion = areaRepository.save(
                new AreaResponsable("Senalizacion Vial", "Atiende senales y letreros danados"));

        // ---------------- personal municipal ----------------
        crearUsuario("admin", CLAVE_ADMIN, admin, null, null);
        crearUsuario("op.alumbrado", CLAVE_OPERADOR, operador, null, alumbrado);
        crearUsuario("op.pistas", CLAVE_OPERADOR, operador, null, pistas);
        crearUsuario("op.semaforos", CLAVE_OPERADOR, operador, null, semaforos);

        // ---------------- ciudadanos (Carlos no tiene cuenta: lo registro personal municipal) ----------------
        Ciudadano maria = ciudadanoRepository.save(
                new Ciudadano("Maria Fernandez", "45678912", "maria.fernandez@mail.com", "987654321"));
        Ciudadano jose = ciudadanoRepository.save(
                new Ciudadano("Jose Torres", "41234567", "jose.torres@mail.com", "912345678"));
        Ciudadano lucia = ciudadanoRepository.save(
                new Ciudadano("Lucia Ramirez", "47891234", "lucia.ramirez@mail.com", "934567890"));
        Ciudadano carlos = ciudadanoRepository.save(
                new Ciudadano("Carlos Vega", "40123456", "carlos.vega@mail.com", "945678901"));

        crearUsuario("maria", CLAVE_CIUDADANO, ciudadanoRol, maria, null);
        crearUsuario("jose", CLAVE_CIUDADANO, ciudadanoRol, jose, null);
        crearUsuario("lucia", CLAVE_CIUDADANO, ciudadanoRol, lucia, null);

        // ---------------- incidencias (dias atras, horas hasta resolver) ----------------
        crearIncidencia(maria, "Poste de luz apagado", "El poste de la cuadra 3 lleva semanas sin funcionar",
                CategoriaIncidencia.ALUMBRADO_PUBLICO, PrioridadIncidencia.MEDIA, "Jr. Las Flores 123",
                alumbrado, EstadoIncidencia.EN_PROCESO, 12, null, "op.alumbrado");

        crearIncidencia(jose, "Vereda rota", "Vereda con un hueco grande frente al parque",
                CategoriaIncidencia.VEREDAS, PrioridadIncidencia.ALTA, "Av. Sol 456",
                null, EstadoIncidencia.REGISTRADA, 2, null, null);

        crearIncidencia(lucia, "Semaforo malogrado", "El semaforo del cruce principal no enciende ninguna luz",
                CategoriaIncidencia.SEMAFOROS, PrioridadIncidencia.CRITICA, "Cruce Av. Sol con Jr. Piura",
                semaforos, EstadoIncidencia.EN_PROCESO, 9, null, "op.semaforos");

        crearIncidencia(carlos, "Basura acumulada", "Contenedor lleno hace mas de una semana",
                CategoriaIncidencia.RESIDUOS, PrioridadIncidencia.MEDIA, "Parque Central",
                limpieza, EstadoIncidencia.RESUELTA, 6, 48, "admin");

        crearIncidencia(maria, "Senal de transito caida", "El letrero de pare esta tirado sobre la pista",
                CategoriaIncidencia.SENALIZACION, PrioridadIncidencia.BAJA, "Jr. Las Flores 500",
                senalizacion, EstadoIncidencia.REGISTRADA, 1, null, null);

        crearIncidencia(jose, "Pista con hueco", "Hueco grande que ya fue reparado por la comuna",
                CategoriaIncidencia.PISTAS, PrioridadIncidencia.ALTA, "Av. Grau 300",
                pistas, EstadoIncidencia.CERRADA, 20, 72, "admin");

        crearIncidencia(lucia, "Banca del parque destruida", "La banca fue vandalizada y es peligrosa",
                CategoriaIncidencia.INFRAESTRUCTURA_PUBLICA, PrioridadIncidencia.BAJA, "Parque Los Alamos",
                null, EstadoIncidencia.REGISTRADA, 15, null, null);

        crearIncidencia(jose, "Luminaria parpadeante", "La luminaria enciende y apaga intermitentemente",
                CategoriaIncidencia.ALUMBRADO_PUBLICO, PrioridadIncidencia.MEDIA, "Calle Los Pinos 88",
                alumbrado, EstadoIncidencia.RESUELTA, 5, 24, "op.alumbrado");
    }

    private void crearUsuario(String username, String clave, Rol rol, Ciudadano ciudadano, AreaResponsable area) {
        Usuario usuario = new Usuario();
        usuario.setUsername(username);
        usuario.setPassword(passwordEncoder.encode(clave));
        usuario.getRoles().add(rol);
        usuario.setCiudadano(ciudadano);
        usuario.setArea(area);
        usuarioRepository.save(usuario);
    }

    /**
     * Crea una incidencia ya "envejecida": fecha de reporte en el pasado y, si esta
     * resuelta/cerrada, fecha de resolucion. Deja su historial coherente con el estado.
     */
    private void crearIncidencia(Ciudadano ciudadano, String titulo, String descripcion,
                                 CategoriaIncidencia categoria, PrioridadIncidencia prioridad, String direccion,
                                 AreaResponsable area, EstadoIncidencia estado,
                                 int diasAtras, Integer horasHastaResolver, String atendidoPor) {
        LocalDateTime reporte = LocalDateTime.now().minusDays(diasAtras);

        Incidencia incidencia = new Incidencia(titulo, descripcion, categoria, prioridad, direccion, ciudadano);
        incidencia.setFechaReporte(reporte);
        incidencia.setAreaResponsable(area);
        incidencia.setEstado(estado);
        if (horasHastaResolver != null) {
            incidencia.setFechaResolucion(reporte.plusHours(horasHastaResolver));
        }
        if (estado != EstadoIncidencia.REGISTRADA) {
            incidencia.setFechaActualizacion(
                    horasHastaResolver != null ? reporte.plusHours(horasHastaResolver) : reporte.plusHours(2));
        }
        incidenciaRepository.save(incidencia);

        guardarHistorial(incidencia, null, EstadoIncidencia.REGISTRADA, "Incidencia registrada",
                ciudadano.getNombre(), reporte);
        if (estado != EstadoIncidencia.REGISTRADA) {
            guardarHistorial(incidencia, EstadoIncidencia.REGISTRADA, EstadoIncidencia.EN_PROCESO,
                    "Atencion iniciada", atendidoPor, reporte.plusHours(2));
        }
        if (estado == EstadoIncidencia.RESUELTA || estado == EstadoIncidencia.CERRADA) {
            guardarHistorial(incidencia, EstadoIncidencia.EN_PROCESO, EstadoIncidencia.RESUELTA,
                    "Incidencia resuelta", atendidoPor, reporte.plusHours(horasHastaResolver));
        }
        if (estado == EstadoIncidencia.CERRADA) {
            guardarHistorial(incidencia, EstadoIncidencia.RESUELTA, EstadoIncidencia.CERRADA,
                    "Incidencia cerrada", "admin", reporte.plusHours(horasHastaResolver + 4));
        }
    }

    private void guardarHistorial(Incidencia incidencia, EstadoIncidencia anterior, EstadoIncidencia nuevo,
                                  String detalle, String realizadoPor, LocalDateTime fecha) {
        HistorialIncidencia registro = new HistorialIncidencia(incidencia, anterior, nuevo, detalle, realizadoPor);
        registro.setFecha(fecha);
        historialRepository.save(registro);
    }
}
