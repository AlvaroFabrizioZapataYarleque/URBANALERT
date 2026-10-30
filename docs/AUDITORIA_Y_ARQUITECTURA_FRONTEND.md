# UrbanAlert — Auditoría del backend y arquitectura del frontend

Documento de las Fases 1 y 2. Todo lo descrito aquí sale de la lectura del código
de `backend/src/main/java/com/utp/urbanalert` (Spring Boot 3.2.5, Java 17).

---

## 1. Resumen del backend

| Aspecto | Hallazgo |
|---|---|
| Estructura | `config`, `controller`, `service` (+`impl`), `repository`, `model`, `dto`, `mapper`, `security`, `exception`. Capas bien separadas. |
| Persistencia | JPA/Hibernate. H2 en memoria por defecto; perfil `mysql` opcional. |
| Autenticación | JWT HS256 *stateless*. `POST /api/auth/login` devuelve `{ token, tipo: "Bearer", expiraEnSegundos, username, roles[] }`. Vigencia por defecto: 2 h. No hay *refresh token*. |
| Autorización | `@PreAuthorize` por **permiso** (`hasAuthority`) y `@Secured("ROLE_ADMIN")` en usuarios. Además, el servicio aplica **alcance por dato** (ciudadano → sus incidencias; operador → las de su área). |
| Roles reales | `ADMIN`, `OPERADOR`, `CIUDADANO` (enum `NombreRol`). El login los devuelve sin prefijo `ROLE_`. |
| CORS | Solo `/api/**`, origen `http://localhost:4200` (variable `CORS_ALLOWED_ORIGINS`), métodos GET/POST/PUT/DELETE/OPTIONS, cabeceras `Authorization` y `Content-Type`. |
| Errores | JSON `{ timestamp, status, mensaje }`; en validación `{ timestamp, status, errores: { campo: mensaje } }`. 401 / 403 / 404 / 409 / 400. |
| Paginación | **No existe.** Los listados devuelven arreglos completos. |
| Ordenamiento | Fijo en servidor: `fechaReporte DESC`. |
| Filtros | `GET /api/incidencias`: `estado`, `categoria`, `prioridad`, `areaId`, `ciudadanoId`, `desde`, `hasta` (`yyyy-MM-dd`, inclusivas). |
| Swagger/OpenAPI | **No existe.** Hay una colección Postman en `backend/postman/`. |

### Permisos por rol (sembrados en `DataLoader`)

| Permiso | ADMIN | OPERADOR | CIUDADANO |
|---|:-:|:-:|:-:|
| INCIDENCIA_CREAR | ✔ | ✔ | ✔ |
| INCIDENCIA_VER | ✔ todas | ✔ su área | ✔ las suyas |
| INCIDENCIA_EDITAR | ✔ | | ✔ solo REGISTRADA |
| INCIDENCIA_CAMBIAR_ESTADO | ✔ | ✔ su área, no cierra | |
| INCIDENCIA_ASIGNAR_AREA | ✔ | | |
| INCIDENCIA_ELIMINAR | ✔ | | ✔ solo REGISTRADA |
| CIUDADANO_GESTIONAR | ✔ | ✔ | |
| AREA_GESTIONAR | ✔ | | |
| REPORTE_VER | ✔ | | |

### Modelo de datos

- **Incidencia**: `id, titulo(150), descripcion(500), categoria, prioridad, estado, direccionReferencial(200), fechaReporte, fechaActualizacion, fechaResolucion` · N:1 `Ciudadano` (obligatorio) · N:1 `AreaResponsable` (opcional) · 1:N `HistorialIncidencia`.
- **HistorialIncidencia**: `estadoAnterior, estadoNuevo, detalle(300), realizadoPor, fecha`. Lo genera el servidor en cada alta, edición, cambio de estado y derivación.
- **Ciudadano**: `nombre, dni (8 dígitos, único), email, telefono (9 dígitos)`.
- **Usuario**: `username, password(BCrypt), enabled, fechaCreacion` · N:M `Rol` · 1:1 `Ciudadano` · N:1 `AreaResponsable`.
- **Enums**: `CategoriaIncidencia` (ALUMBRADO_PUBLICO, PISTAS, VEREDAS, SEMAFOROS, RESIDUOS, SENALIZACION, INFRAESTRUCTURA_PUBLICA) · `PrioridadIncidencia` (BAJA, MEDIA, ALTA, CRITICA) · `EstadoIncidencia` (REGISTRADA → EN_PROCESO → RESUELTA → CERRADA; RESUELTA puede volver a EN_PROCESO).

### Reglas de negocio relevantes para la UI

1. No se puede pasar a `EN_PROCESO` sin área asignada.
2. Solo ADMIN cierra (`CERRADA`). `CERRADA` es final.
3. El ciudadano solo edita o elimina mientras su incidencia está `REGISTRADA`.
4. El ciudadano **siempre** reporta a su nombre; el servidor ignora `ciudadanoId` y `areaId` que envíe.
5. Personal municipal (ADMIN/OPERADOR) **debe** indicar `ciudadanoId` al registrar.
6. Un operador requiere `areaId`; no se pueden crear ciudadanos desde `/api/usuarios`.
7. Un área con incidencias o personal no puede eliminarse (409).
8. Un admin no puede deshabilitarse a sí mismo.

---

## 2. Inventario de endpoints

| Módulo | Método | Endpoint | Descripción | Autenticación | Rol permitido |
|---|---|---|---|---|---|
| Auth | POST | `/api/auth/register` | Registro de ciudadano (perfil + cuenta) | No | Público |
| Auth | POST | `/api/auth/login` | Inicio de sesión → JWT | No | Público |
| Público | GET | `/api/publico/resumen` | Totales por estado y categoría | No | Público |
| Incidencias | GET | `/api/incidencias` | Listado con filtros (alcance por rol) | JWT | ADMIN, OPERADOR, CIUDADANO |
| Incidencias | GET | `/api/incidencias/{id}` | Detalle | JWT | ADMIN, OPERADOR (su área), CIUDADANO (propia) |
| Incidencias | GET | `/api/incidencias/{id}/historial` | Historial de seguimiento | JWT | Igual que detalle |
| Incidencias | POST | `/api/incidencias` | Registrar | JWT | ADMIN, OPERADOR, CIUDADANO |
| Incidencias | PUT | `/api/incidencias/{id}` | Editar datos (incl. prioridad y categoría) | JWT | ADMIN, CIUDADANO (REGISTRADA) |
| Incidencias | PUT | `/api/incidencias/{id}/estado` | Cambiar estado | JWT | ADMIN, OPERADOR |
| Incidencias | PUT | `/api/incidencias/{id}/area` | Asignar / derivar área | JWT | ADMIN |
| Incidencias | DELETE | `/api/incidencias/{id}` | Eliminar con su historial | JWT | ADMIN, CIUDADANO (REGISTRADA) |
| Ciudadanos | GET | `/api/ciudadanos` | Listado | JWT | ADMIN, OPERADOR |
| Ciudadanos | GET | `/api/ciudadanos/{id}` | Detalle | JWT | ADMIN, OPERADOR |
| Ciudadanos | POST | `/api/ciudadanos` | Alta sin cuenta (atención presencial) | JWT | ADMIN, OPERADOR |
| Perfil | GET | `/api/ciudadanos/me` | Mi perfil | JWT | CIUDADANO |
| Perfil | PUT | `/api/ciudadanos/me` | Editar mi perfil | JWT | CIUDADANO |
| Áreas | GET | `/api/areas` | Listado | JWT | Cualquier autenticado |
| Áreas | GET | `/api/areas/{id}` | Detalle | JWT | Cualquier autenticado |
| Áreas | POST | `/api/areas` | Crear | JWT | ADMIN |
| Áreas | PUT | `/api/areas/{id}` | Editar | JWT | ADMIN |
| Áreas | DELETE | `/api/areas/{id}` | Eliminar | JWT | ADMIN |
| Usuarios | GET | `/api/usuarios` | Listado de cuentas | JWT | ADMIN |
| Usuarios | POST | `/api/usuarios` | Alta de personal (ADMIN/OPERADOR) | JWT | ADMIN |
| Usuarios | PUT | `/api/usuarios/{id}/habilitado` | Habilitar / deshabilitar | JWT | ADMIN |
| Reportes | GET | `/api/reportes/indicadores?dias=N` | Totales, pendientes antiguas, tiempos de resolución | JWT | ADMIN |
| Reportes | GET | `/api/reportes/pendientes?dias=N` | Incidencias sin resolver con más de N días | JWT | ADMIN |

### Funcionalidades solicitadas vs. soporte real

| Funcionalidad | Soporte en backend | Decisión en el frontend |
|---|---|---|
| Inicio de sesión | ✔ por **username** (no correo) | Implementado |
| Registro de ciudadanos | ✔ username, password, nombre (completo, un solo campo), DNI, email, teléfono | Implementado con esos campos exactos |
| Consulta de perfil | Parcial: solo `/api/ciudadanos/me` (CIUDADANO). Para ADMIN/OPERADOR solo username+roles del login | Perfil editable para ciudadano; cabecera muestra username/rol para el personal |
| Roles y permisos | ✔ roles en login; los permisos **no** se exponen | La UI deriva sus controles visuales del rol (espejo de la tabla de permisos). La autorización real sigue en el servidor |
| Registro de incidencias | ✔ | Implementado (ciudadano y personal a nombre de un ciudadano) |
| Listado / detalle | ✔ | Implementado |
| Actualización | ✔ | Implementado según rol y estado |
| Categorías | Enum fijo, **sin endpoint ni CRUD** | Catálogo tipado en el frontend igual al enum. Sin módulo de categorías |
| Geolocalización / coordenadas / mapa | **No existe** (solo `direccionReferencial`) | Solo dirección o referencia. Sin mapa |
| Asignación de áreas | ✔ | Implementado (ADMIN) |
| Cambio de estados | ✔ | Implementado mostrando solo transiciones válidas |
| Prioridades | ✔ dentro de `PUT /api/incidencias/{id}` | ADMIN la modifica desde el detalle |
| Evidencias fotográficas | **No existe** | No implementado. Se informa en la UI |
| Historial | ✔ (generado por el servidor) | Línea de tiempo |
| Observaciones / respuesta del área | **No existe** (el cambio de estado no acepta comentario) | No implementado |
| Dashboard e indicadores | ✔ solo ADMIN (`/api/reportes/*`) | Dashboard admin con indicadores reales; operador y ciudadano cuentan sobre su propio listado real |
| Incidencias por prioridad / por área | No en `indicadores` | Se agregan en el cliente a partir de `GET /api/incidencias` (datos reales, filtrables por fecha/área) |
| Incidencias vencidas | ✔ `pendientes?dias=N` | Módulo Reportes |
| Notificaciones | **No existe** | No implementado |
| Paginación y ordenamiento | **No existe** | Búsqueda, orden y paginación en el cliente sobre el listado real |

---

## 3. Observaciones de arquitectura del backend

Calidad general buena: controladores delgados, lógica en servicios, DTOs con Bean
Validation, transacciones y consultas JPQL parametrizadas con `JOIN FETCH`.
Observaciones y mejoras propuestas (no se modificó el backend):

1. **Sin paginación**: `GET /api/incidencias` devuelve todo. Con volumen real conviene `Pageable` (`page,size,sort`) y `Page<IncidenciaResponse>`.
2. **Respuestas de error con dos formas** (`mensaje` vs `errores`). Funciona, pero un único `ErrorResponse { status, mensaje, errores? }` simplifica clientes.
3. **No hay endpoint de "mi cuenta"** (`GET /api/auth/me`) para cualquier rol; el operador no conoce su `areaId`/`areaNombre` desde el login. Propuesta: incluir `areaNombre` y `ciudadanoId` en `AuthResponse` o exponer `/api/auth/me`.
4. **Permisos no expuestos**: el frontend debe duplicar la tabla rol→permiso para la UI. Propuesta: devolver `permisos[]` en el login.
5. **Cambio de estado sin observación**: la trazabilidad pierde el "por qué". Propuesta: `CambioEstadoRequest { estado, observacion }` guardada en `HistorialIncidencia.detalle`.
6. **El ciudadano fija la prioridad** al registrar (campo obligatorio). En el negocio la prioriza el municipio; propuesta: hacerla opcional (default `MEDIA`) para CIUDADANO.
7. **Faltan evidencias, geolocalización y notificaciones**, requeridos por el enunciado del proyecto.
8. **Seguridad**: ~~secreto JWT por defecto en `application.properties`~~ (resuelto: ahora es obligatorio vía `JWT_SECRET`, ver CONFIGURACION_ENTORNO.md); `spring.jpa.show-sql=true` y consola H2 abiertas (solo dev). Sin límite de intentos de login.
9. **`asignarArea` no llama `verificarAcceso`**: hoy es correcto porque solo ADMIN tiene el permiso, pero si se concede a otro rol quedaría sin control por dato.
10. **Sin OpenAPI**: agregar `springdoc-openapi` facilitaría generar modelos del cliente.

---

## 4. Arquitectura Angular 22

Proyecto en `frontend/` (Angular 22, standalone, zoneless, signals, Reactive Forms,
lazy loading, `HttpClient` con interceptores funcionales). Sin librerías de UI ni de
gráficos: estilos SCSS propios e íconos SVG en línea para no sumar dependencias.

```
frontend/src/app
├── core
│   ├── auth/            session.service (signals), auth.api, auth.models
│   ├── guards/          auth.guard, guest.guard, role.guard
│   ├── interceptors/    auth.interceptor (Bearer), error.interceptor (401 → sesión expirada)
│   ├── models/          DTOs tipados + catálogos (estados, categorías, prioridades)
│   └── services/        incidencias.api, areas.api, ciudadanos.api, usuarios.api,
│                        reportes.api, publico.api, toast.service, api-error
├── shared
│   ├── components/      icon, badge (estado/prioridad), page-header, empty-state,
│   │                    stat-card, bar-chart, paginator, field-error, toasts, confirm-dialog
│   ├── layouts/         shell (sidebar + topbar) reutilizado por portal y panel
│   └── pipes/           labels de enums, fecha
├── layouts
│   ├── public-layout/   login y registro
│   ├── portal-layout/   ciudadano
│   └── panel-layout/    ADMIN y OPERADOR
└── features
    ├── auth/            login, registro
    ├── portal/          inicio, mis-incidencias, nueva/editar, detalle, perfil
    └── panel/           inicio (dashboard), bandeja, detalle-gestion, nueva,
                         ciudadanos, areas, usuarios, reportes
```

### Mapeo pantalla ↔ endpoint

| Pantalla | Ruta | Rol | Endpoints |
|---|---|---|---|
| Login | `/login` | Público | `POST /api/auth/login`, `GET /api/publico/resumen` |
| Registro | `/registro` | Público | `POST /api/auth/register` |
| Inicio ciudadano | `/portal` | CIUDADANO | `GET /api/ciudadanos/me`, `GET /api/incidencias` |
| Mis incidencias | `/portal/incidencias` | CIUDADANO | `GET /api/incidencias?filtros` |
| Reportar / editar | `/portal/reportar`, `/portal/incidencias/:id/editar` | CIUDADANO | `POST`, `PUT /api/incidencias/{id}` |
| Detalle y seguimiento | `/portal/incidencias/:id` | CIUDADANO | `GET /{id}`, `GET /{id}/historial`, `DELETE /{id}` |
| Mi perfil | `/portal/perfil` | CIUDADANO | `GET/PUT /api/ciudadanos/me` |
| Dashboard | `/panel` | ADMIN (indicadores) / OPERADOR (su área) | `GET /api/reportes/indicadores`, `GET /api/incidencias`, `GET /api/areas` |
| Bandeja | `/panel/incidencias` | ADMIN, OPERADOR | `GET /api/incidencias?filtros`, `GET /api/areas` |
| Gestión de incidencia | `/panel/incidencias/:id` | ADMIN, OPERADOR | `GET /{id}`, `/historial`, `PUT /estado`, `PUT /area`, `PUT /{id}`, `DELETE` |
| Registro presencial | `/panel/registro` | ADMIN, OPERADOR | `GET /api/ciudadanos`, `POST /api/ciudadanos`, `POST /api/incidencias` |
| Ciudadanos | `/panel/ciudadanos` | ADMIN, OPERADOR | `GET/POST /api/ciudadanos` |
| Áreas | `/panel/areas` | ADMIN | CRUD `/api/areas` |
| Usuarios | `/panel/usuarios` | ADMIN | `GET/POST /api/usuarios`, `PUT /{id}/habilitado` |
| Reportes | `/panel/reportes` | ADMIN | `GET /api/reportes/indicadores`, `GET /api/reportes/pendientes` |

### Sesión

- Token, username, roles y fecha de expiración en `localStorage` (clave `ua.session`).
- Al expirar (`expiraEnSegundos`) se cierra la sesión automáticamente.
- Cualquier `401` en una petición autenticada cierra la sesión y redirige a `/login?motivo=expirada`.
- `403` muestra el mensaje del servidor sin cerrar sesión.
- Redirección por rol: CIUDADANO → `/portal`; ADMIN/OPERADOR → `/panel`.

---

## 5. Verificación realizada (Fase 7)

Backend levantado con `mvn spring-boot:run` (desde `backend/`) (H2 + datos de `DataLoader`) y frontend con `ng serve`.

| Prueba | Resultado |
|---|---|
| `ng build` (producción) | OK, sin errores ni advertencias. Inicial ≈ 98 KB transferidos; cada página es un chunk diferido |
| `ng test` (Vitest) | 14/14: sesión, permisos, guards, interceptores, lectura de errores, tabla |
| Login correcto / incorrecto | Redirección por rol; muestra "Usuario o contrasena incorrectos" del backend |
| Registro de ciudadano | Validaciones locales; DNI duplicado muestra el 409 del backend; luego login OK |
| Ciudadano: reportar, ver detalle e historial | OK (incidencia creada y visible con su historial) |
| ADMIN: cambio de estado, reclasificación | OK; el historial se actualiza al momento |
| ADMIN: alta de operador, usuario duplicado, área obligatoria | OK |
| ADMIN: eliminar área con incidencias | Muestra el 409 del backend |
| Registro presencial (personal a nombre de un vecino) | OK, con área asignada desde el alta |
| OPERADOR | Solo ve su área, solo "Cambiar estado", no puede cerrar, sin acceso a Reportes/Áreas/Usuarios |
| Guard por rol (ciudadano entra a `/panel`) | Redirige a `/portal` |
| Token inválido / vencido | Cierra sesión y muestra "Su sesión expiró" |
| Responsive (375 px) | Sin scroll horizontal; menú lateral desplegable |
| Consola del navegador | Sin errores de JavaScript (solo registros de red de los 401/409 provocados) |

Nota: el enunciado menciona una imagen de referencia visual que no llegó adjunta; el diseño sigue la
descripción escrita (sidebar, fondo gris claro, tarjetas blancas, acento azul).
