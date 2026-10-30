package com.utp.urbanalert;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Prueba de integracion de extremo a extremo: levanta la aplicacion completa
 * (con H2 y los datos del DataLoader) y ejercita la cadena real de seguridad:
 * login -> token JWT -> filtro -> autorizacion por rol/permiso -> servicio -> BD.
 */
@SpringBootTest
@AutoConfigureMockMvc
class SeguridadIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    // ------------------------------------------------------------ utilidades

    private String login(String username, String password) throws Exception {
        String cuerpo = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(cuerpo, "$.token");
    }

    private String tokenAdmin() throws Exception {
        return login("admin", "Admin123*");
    }

    private String tokenCiudadano() throws Exception {
        return login("maria", "Ciudadano123*");
    }

    private String tokenOperadorAlumbrado() throws Exception {
        return login("op.alumbrado", "Operador123*");
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private ResultActions getCon(String token, String url) throws Exception {
        return mockMvc.perform(get(url).header(HttpHeaders.AUTHORIZATION, bearer(token)));
    }

    private ResultActions enviar(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder metodo,
                                 String token, String json) throws Exception {
        return mockMvc.perform(metodo
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json));
    }

    private int idPrimeraIncidenciaDe(String ciudadanoNombre) throws Exception {
        String lista = getCon(tokenAdmin(), "/api/incidencias").andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        List<Integer> ids = JsonPath.read(lista, "$[?(@.ciudadanoNombre == '" + ciudadanoNombre + "')].id");
        return ids.get(0);
    }

    // ------------------------------------------------------------ autenticacion

    @Test
    void sinToken_unEndpointProtegido_responde401() throws Exception {
        mockMvc.perform(get("/api/incidencias"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void conTokenInvalido_responde401() throws Exception {
        getCon("esto.no.es.un.jwt", "/api/incidencias").andExpect(status().isUnauthorized());
    }

    @Test
    void loginCorrecto_devuelveTokenTipoYRoles() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"Admin123*\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.tipo").value("Bearer"))
                .andExpect(jsonPath("$.roles[0]").value("ADMIN"))
                .andExpect(jsonPath("$.expiraEnSegundos").isNumber());
    }

    @Test
    void loginConContrasenaIncorrecta_responde401ConMensajeGenerico() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"incorrecta1\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensaje").value("Usuario o contrasena incorrectos"));
    }

    @Test
    void loginConUsuarioInexistente_respondeLoMismoQueConContrasenaIncorrecta() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"fantasma\",\"password\":\"Clave1234\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensaje").value("Usuario o contrasena incorrectos"));
    }

    @Test
    void registro_creaCuentaCiudadano_noExponeLaContrasena_yPermiteIniciarSesion() throws Exception {
        String cuerpo = "{\"username\":\"pedro.gomez\",\"password\":\"Clave1234\","
                + "\"nombre\":\"Pedro Gomez\",\"dni\":\"70123456\","
                + "\"email\":\"pedro@mail.com\",\"telefono\":\"955123456\"}";

        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("pedro.gomez"))
                .andExpect(jsonPath("$.roles[0]").value("CIUDADANO"))
                .andExpect(jsonPath("$.password").doesNotExist());

        // Registrar de nuevo el mismo usuario es un conflicto.
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isConflict());

        // La cuenta recien creada ya puede iniciar sesion.
        login("pedro.gomez", "Clave1234");
    }

    @Test
    void registro_conContrasenaDebil_responde400ConElCampoInvalido() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"nuevo.usuario\",\"password\":\"corta\","
                                + "\"nombre\":\"Nuevo\",\"dni\":\"70999999\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.password").exists());
    }

    @Test
    void endpointPublico_resumen_funcionaSinToken() throws Exception {
        mockMvc.perform(get("/api/publico/resumen"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").isNumber())
                .andExpect(jsonPath("$.porEstado.REGISTRADA").exists())
                .andExpect(jsonPath("$.porCategoria.ALUMBRADO_PUBLICO").exists());
    }

    // ------------------------------------------------------------ autorizacion por rol/permiso

    @Test
    void ciudadano_noPuedeListarUsuarios_responde403() throws Exception {
        getCon(tokenCiudadano(), "/api/usuarios").andExpect(status().isForbidden());
    }

    @Test
    void admin_listaUsuarios_sinExponerContrasenas() throws Exception {
        getCon(tokenAdmin(), "/api/usuarios")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.username == 'admin')]").isNotEmpty())
                .andExpect(jsonPath("$[0].password").doesNotExist());
    }

    @Test
    void ciudadano_noPuedeVerLosReportesDelDashboard_responde403() throws Exception {
        getCon(tokenCiudadano(), "/api/reportes/indicadores").andExpect(status().isForbidden());
    }

    @Test
    void operador_noPuedeEliminarIncidencias_responde403() throws Exception {
        mockMvc.perform(delete("/api/incidencias/1")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenOperadorAlumbrado())))
                .andExpect(status().isForbidden());
    }

    @Test
    void ciudadano_soloVeSusPropiasIncidencias() throws Exception {
        getCon(tokenCiudadano(), "/api/incidencias")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.ciudadanoNombre == 'Maria Fernandez')]").isNotEmpty())
                .andExpect(jsonPath("$[?(@.ciudadanoNombre != 'Maria Fernandez')]").isEmpty());
    }

    @Test
    void ciudadano_noPuedeVerLaIncidenciaDeOtroCiudadano_responde403() throws Exception {
        int idDeJose = idPrimeraIncidenciaDe("Jose Torres");

        getCon(tokenCiudadano(), "/api/incidencias/" + idDeJose).andExpect(status().isForbidden());
    }

    @Test
    void operador_soloVeLasIncidenciasDeSuArea() throws Exception {
        getCon(tokenOperadorAlumbrado(), "/api/incidencias")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.areaResponsableNombre == 'Alumbrado Publico')]").isNotEmpty())
                .andExpect(jsonPath("$[?(@.areaResponsableNombre != 'Alumbrado Publico')]").isEmpty());
    }

    // ------------------------------------------------------------ validaciones y errores

    @Test
    void crearIncidencia_sinTitulo_responde400() throws Exception {
        enviar(post("/api/incidencias"), tokenCiudadano(),
                "{\"descripcion\":\"Sin titulo\",\"categoria\":\"PISTAS\",\"prioridad\":\"BAJA\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.titulo").exists());
    }

    @Test
    void crearIncidencia_conCategoriaInexistente_responde400() throws Exception {
        enviar(post("/api/incidencias"), tokenCiudadano(),
                "{\"titulo\":\"X\",\"descripcion\":\"Y\",\"categoria\":\"XYZ\",\"prioridad\":\"BAJA\"}")
                .andExpect(status().isBadRequest());
    }

    @Test
    void filtro_conEstadoInexistente_responde400() throws Exception {
        getCon(tokenAdmin(), "/api/incidencias?estado=XYZ").andExpect(status().isBadRequest());
    }

    // ------------------------------------------------------------ consultas JPQL (filtros y reportes)

    @Test
    void filtroPorEstado_devuelveSoloIncidenciasEnEseEstado() throws Exception {
        getCon(tokenAdmin(), "/api/incidencias?estado=EN_PROCESO")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.estado == 'EN_PROCESO')]").isNotEmpty())
                .andExpect(jsonPath("$[?(@.estado != 'EN_PROCESO')]").isEmpty());
    }

    @Test
    void filtroPorCategoriaYPrioridad_combinaLosCriterios() throws Exception {
        getCon(tokenAdmin(), "/api/incidencias?categoria=ALUMBRADO_PUBLICO&prioridad=MEDIA")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.categoria == 'ALUMBRADO_PUBLICO')]").isNotEmpty())
                .andExpect(jsonPath("$[?(@.categoria != 'ALUMBRADO_PUBLICO')]").isEmpty())
                .andExpect(jsonPath("$[?(@.prioridad != 'MEDIA')]").isEmpty());
    }

    @Test
    void filtroPorRangoDeFechas_fueraDeRango_devuelveListaVacia() throws Exception {
        getCon(tokenAdmin(), "/api/incidencias?desde=2099-01-01&hasta=2099-12-31")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void filtroPorRangoDeFechas_amplio_incluyeLasIncidenciasExistentes() throws Exception {
        getCon(tokenAdmin(), "/api/incidencias?desde=2020-01-01&hasta=2099-12-31")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isNotEmpty());
    }

    @Test
    void reportePendientesAntiguas_listaLasNoResueltasConMasDeNDias() throws Exception {
        getCon(tokenAdmin(), "/api/reportes/pendientes?dias=7")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[?(@.estado == 'RESUELTA' || @.estado == 'CERRADA')]").isEmpty());
    }

    @Test
    void reporteIndicadores_devuelveTotalesYTiemposDeResolucion() throws Exception {
        getCon(tokenAdmin(), "/api/reportes/indicadores?dias=7")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").isNumber())
                .andExpect(jsonPath("$.porEstado.EN_PROCESO").isNumber())
                .andExpect(jsonPath("$.pendientesAntiguas").isNumber())
                .andExpect(jsonPath("$.tiempoPromedioResolucionHoras").isNumber());
    }

    @Test
    void reporteIndicadores_conDiasInvalidos_responde400() throws Exception {
        getCon(tokenAdmin(), "/api/reportes/indicadores?dias=0").andExpect(status().isBadRequest());
    }

    // ------------------------------------------------------------ flujo completo de una incidencia

    @Test
    void flujoCompleto_registroDerivacionAtencionYCierre() throws Exception {
        String ciudadano = tokenCiudadano();
        String operador = tokenOperadorAlumbrado();
        String admin = tokenAdmin();

        // 1. La ciudadana registra una incidencia: queda REGISTRADA y sin area.
        String creada = enviar(post("/api/incidencias"), ciudadano,
                "{\"titulo\":\"Luminaria sin luz\",\"descripcion\":\"No enciende desde el lunes\","
                        + "\"categoria\":\"ALUMBRADO_PUBLICO\",\"prioridad\":\"ALTA\","
                        + "\"direccionReferencial\":\"Av. Grau 100\"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("REGISTRADA"))
                .andExpect(jsonPath("$.areaResponsableNombre").doesNotExist())
                .andReturn().getResponse().getContentAsString();
        int id = JsonPath.read(creada, "$.id");

        // 2. El operador aun no puede verla: no pertenece a su area.
        getCon(operador, "/api/incidencias/" + id).andExpect(status().isForbidden());

        // 3. El administrador la deriva al area de Alumbrado Publico.
        String areas = getCon(admin, "/api/areas").andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        List<Integer> idsAlumbrado = JsonPath.read(areas, "$[?(@.nombre == 'Alumbrado Publico')].id");
        enviar(put("/api/incidencias/" + id + "/area"), admin, "{\"areaId\":" + idsAlumbrado.get(0) + "}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.areaResponsableNombre").value("Alumbrado Publico"));

        // 4. Ahora el operador del area la atiende: REGISTRADA -> EN_PROCESO.
        enviar(put("/api/incidencias/" + id + "/estado"), operador, "{\"estado\":\"EN_PROCESO\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("EN_PROCESO"));

        // 5. Saltarse el flujo (EN_PROCESO -> CERRADA) no esta permitido.
        enviar(put("/api/incidencias/" + id + "/estado"), operador, "{\"estado\":\"CERRADA\"}")
                .andExpect(status().isBadRequest());

        // 6. EN_PROCESO -> RESUELTA fija la fecha de resolucion.
        enviar(put("/api/incidencias/" + id + "/estado"), operador, "{\"estado\":\"RESUELTA\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fechaResolucion").isNotEmpty());

        // 7. Un operador no puede cerrar; solo el administrador.
        enviar(put("/api/incidencias/" + id + "/estado"), operador, "{\"estado\":\"CERRADA\"}")
                .andExpect(status().isForbidden());
        enviar(put("/api/incidencias/" + id + "/estado"), admin, "{\"estado\":\"CERRADA\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CERRADA"));

        // 8. El historial dejo constancia de cada paso: alta, derivacion, 3 cambios de estado.
        getCon(admin, "/api/incidencias/" + id + "/historial")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(5))
                .andExpect(jsonPath("$[0].estadoNuevo").value("REGISTRADA"))
                .andExpect(jsonPath("$[4].estadoNuevo").value("CERRADA"));

        // 9. Una incidencia CERRADA ya no se puede editar.
        enviar(put("/api/incidencias/" + id), ciudadano,
                "{\"titulo\":\"Otro titulo\",\"descripcion\":\"Otra descripcion\","
                        + "\"categoria\":\"ALUMBRADO_PUBLICO\",\"prioridad\":\"BAJA\"}")
                .andExpect(status().isBadRequest());
    }
}
