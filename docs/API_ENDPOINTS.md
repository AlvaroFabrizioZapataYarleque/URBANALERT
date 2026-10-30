# API REST — endpoints

Base: `http://localhost:8080/api`. Inventario tomado de `backend/src/main/java/com/utp/urbanalert/controller/`.
No hay documentación Swagger/OpenAPI. Hay una colección Postman en `backend/postman/UrbanAlert-AP2.postman_collection.json`.

- **Públicos**: no requieren token.
- **Protegidos**: requieren `Authorization: Bearer <token>`. Sin token o con un token vencido responden `401`. Sin el permiso necesario responden `403`.

## Endpoints públicos

| Módulo | Método | Endpoint | Descripción | Rol |
|---|---|---|---|---|
| Auth | POST | `/api/auth/register` | Registro de ciudadano (perfil + cuenta con rol CIUDADANO) | Público |
| Auth | POST | `/api/auth/login` | Inicio de sesión; devuelve el JWT | Público |
| Público | GET | `/api/publico/resumen` | Totales por estado y categoría (sin datos personales) | Público |

## Endpoints protegidos

| Módulo | Método | Endpoint | Descripción | Rol (permiso) |
|---|---|---|---|---|
| Incidencias | GET | `/api/incidencias` | Listado del alcance del usuario, con filtros | ADMIN, OPERADOR (su área), CIUDADANO (las suyas) · `INCIDENCIA_VER` |
| Incidencias | GET | `/api/incidencias/{id}` | Detalle | Igual que el listado |
| Incidencias | GET | `/api/incidencias/{id}/historial` | Historial de seguimiento | Igual que el listado |
| Incidencias | POST | `/api/incidencias` | Registrar | ADMIN, OPERADOR, CIUDADANO · `INCIDENCIA_CREAR` |
| Incidencias | PUT | `/api/incidencias/{id}` | Editar título, descripción, categoría, prioridad y dirección | ADMIN; CIUDADANO solo si está REGISTRADA · `INCIDENCIA_EDITAR` |
| Incidencias | PUT | `/api/incidencias/{id}/estado` | Cambiar estado | ADMIN; OPERADOR (su área, no puede cerrar) · `INCIDENCIA_CAMBIAR_ESTADO` |
| Incidencias | PUT | `/api/incidencias/{id}/area` | Asignar o derivar área | ADMIN · `INCIDENCIA_ASIGNAR_AREA` |
| Incidencias | DELETE | `/api/incidencias/{id}` | Eliminar junto con su historial | ADMIN; CIUDADANO solo si está REGISTRADA · `INCIDENCIA_ELIMINAR` |
| Ciudadanos | GET | `/api/ciudadanos` | Listado | ADMIN, OPERADOR · `CIUDADANO_GESTIONAR` |
| Ciudadanos | GET | `/api/ciudadanos/{id}` | Detalle | ADMIN, OPERADOR · `CIUDADANO_GESTIONAR` |
| Ciudadanos | POST | `/api/ciudadanos` | Alta sin cuenta (atención presencial) | ADMIN, OPERADOR · `CIUDADANO_GESTIONAR` |
| Perfil | GET | `/api/ciudadanos/me` | Mi perfil | CIUDADANO |
| Perfil | PUT | `/api/ciudadanos/me` | Editar nombre, email y teléfono | CIUDADANO |
| Áreas | GET | `/api/areas` | Listado | Cualquier usuario autenticado |
| Áreas | GET | `/api/areas/{id}` | Detalle | Cualquier usuario autenticado |
| Áreas | POST | `/api/areas` | Crear | ADMIN · `AREA_GESTIONAR` |
| Áreas | PUT | `/api/areas/{id}` | Editar | ADMIN · `AREA_GESTIONAR` |
| Áreas | DELETE | `/api/areas/{id}` | Eliminar (409 si tiene incidencias o personal) | ADMIN · `AREA_GESTIONAR` |
| Usuarios | GET | `/api/usuarios` | Listado de cuentas | ADMIN (`@Secured("ROLE_ADMIN")`) |
| Usuarios | POST | `/api/usuarios` | Alta de personal (rol ADMIN u OPERADOR; OPERADOR requiere `areaId`) | ADMIN |
| Usuarios | PUT | `/api/usuarios/{id}/habilitado` | Habilitar o deshabilitar una cuenta | ADMIN |
| Reportes | GET | `/api/reportes/indicadores?dias=7` | Totales, vencidas y tiempos de resolución | ADMIN · `REPORTE_VER` |
| Reportes | GET | `/api/reportes/pendientes?dias=7` | Incidencias sin resolver con más de N días | ADMIN · `REPORTE_VER` |

### Filtros de `GET /api/incidencias`

Parámetros opcionales: `estado`, `categoria`, `prioridad`, `areaId`, `ciudadanoId`, `desde`, `hasta` (fechas `yyyy-MM-dd`, ambas inclusivas). El orden es siempre `fechaReporte DESC`. **No hay paginación.** El servidor fija el alcance según el rol, así que un operador o un ciudadano no pueden ampliarlo con `areaId` o `ciudadanoId`.

### Valores de los enums

- `estado`: `REGISTRADA`, `EN_PROCESO`, `RESUELTA`, `CERRADA`
- `categoria`: `ALUMBRADO_PUBLICO`, `PISTAS`, `VEREDAS`, `SEMAFOROS`, `RESIDUOS`, `SENALIZACION`, `INFRAESTRUCTURA_PUBLICA`
- `prioridad`: `BAJA`, `MEDIA`, `ALTA`, `CRITICA`

## Ejemplos

```bash
# Login
curl -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"Admin123*"}'
# → {"token":"eyJ...","tipo":"Bearer","expiraEnSegundos":7200,"username":"admin","roles":["ADMIN"]}

# Llamada protegida
curl http://localhost:8080/api/incidencias?estado=REGISTRADA \
  -H "Authorization: Bearer <token>"

# Registrar incidencia (como ciudadano)
curl -X POST http://localhost:8080/api/incidencias \
  -H "Authorization: Bearer <token>" -H 'Content-Type: application/json' \
  -d '{"titulo":"Poste apagado","descripcion":"Lleva 3 días sin luz","categoria":"ALUMBRADO_PUBLICO","prioridad":"MEDIA","direccionReferencial":"Av. Grau 450"}'

# Cambiar estado
curl -X PUT http://localhost:8080/api/incidencias/5/estado \
  -H "Authorization: Bearer <token>" -H 'Content-Type: application/json' \
  -d '{"estado":"EN_PROCESO"}'
```

## Respuestas de error

| Código | Cuándo | Cuerpo |
|---|---|---|
| 400 | Validación de un DTO | `{ "timestamp", "status": 400, "errores": { "campo": "mensaje" } }` |
| 400 | Regla de negocio, JSON mal formado o parámetro inválido | `{ "timestamp", "status": 400, "mensaje" }` |
| 401 | Credenciales incorrectas, o token ausente, inválido o vencido | `{ "timestamp", "status": 401, "mensaje" }` |
| 403 | Sin permiso, o el recurso no está en el alcance del usuario | `{ "timestamp", "status": 403, "mensaje" }` |
| 404 | Recurso inexistente | `{ "timestamp", "status": 404, "mensaje" }` |
| 409 | Duplicado (usuario, DNI, área) o recurso en uso | `{ "timestamp", "status": 409, "mensaje" }` |
