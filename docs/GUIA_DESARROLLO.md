# Guía de desarrollo

## Organización del repositorio

```
URBANALERT/
├── backend/     API Spring Boot (Maven). Paquete raíz com.utp.urbanalert
├── frontend/    SPA Angular 22
├── docs/        Documentación técnica
├── .gitignore
└── README.md
```

Backend y frontend son independientes: cada uno tiene sus dependencias, sus pruebas y su README. Se comunican solo por la API REST.

## Convenciones de nombres

El código y el dominio están en español; los sufijos técnicos siguen las convenciones de cada framework.

**Backend (Java)**

| Elemento | Convención | Ejemplo existente |
|---|---|---|
| Entidad | Sustantivo singular, `@Table` en plural snake_case | `Incidencia` → `incidencias` |
| Enum | PascalCase, valores en MAYÚSCULAS | `EstadoIncidencia.EN_PROCESO` |
| DTO de entrada | `XxxRequest` (con Bean Validation) | `IncidenciaRequest`, `CambioEstadoRequest` |
| DTO de salida | `XxxResponse` | `IncidenciaResponse` |
| Servicio | Interfaz `XxxService` + `impl/XxxServiceImpl` | `IncidenciaService` |
| Repositorio | `XxxRepository` (+ `Custom`/`CustomImpl` para JPQL dinámico) | `IncidenciaRepositoryCustomImpl` |
| Controlador | `XxxController`, ruta `/api/<plural>` | `AreaController` → `/api/areas` |
| Excepciones | `XxxException`, mapeadas en `GlobalExceptionHandler` | `ConflictException` → 409 |

**Frontend (Angular)**

| Elemento | Convención | Ejemplo existente |
|---|---|---|
| Archivos | kebab-case sin sufijo `.component` (estilo Angular 20+) | `mis-incidencias.ts` |
| Clases | PascalCase sin sufijo `Component` | `MisIncidencias`, `Bandeja` |
| Selector | prefijo `ua-` | `ua-estado-badge` |
| Servicio HTTP | `XxxApi` en `core/api/apis.ts`, uno por controlador | `IncidenciasApi` |
| Modelos | Mismos nombres y campos que los DTO Java | `IncidenciaResponse` |
| Estado | `signal`/`computed`; datos remotos con `rxResource` | `lista = rxResource(...)` |

## Crear un endpoint nuevo (backend)

1. **DTO**: `dto/XxxRequest.java` con validaciones (`@NotBlank`, `@Size`, `@Pattern`, …) y `dto/XxxResponse.java`.
2. **Repositorio**: método derivado o `@Query` JPQL con parámetros nombrados (nunca concatenar valores).
3. **Servicio**: agregar el método a la interfaz e implementarlo en `impl/` con las reglas de negocio. Marcarlo `@Transactional` si escribe. Validar el alcance por dato si corresponde (ver `IncidenciaServiceImpl.verificarAcceso`).
4. **Controlador**: método delgado con `@PreAuthorize("hasAuthority('PERMISO')")`. Si hace falta un permiso nuevo, agregarlo al enum `Permiso` y asignarlo a los roles en `DataLoader`. Con MySQL, los roles ya creados no se actualizan solos: hay que insertar el permiso en `roles_permisos` o recrear la base.
5. **Errores**: lanzar `BusinessValidationException` (400), `ResourceNotFoundException` (404) o `ConflictException` (409). No devolver mapas armados a mano.
6. **Pruebas**: unitarias con Mockito en `src/test/.../service/` y de seguridad en `SeguridadIntegrationTest` (401/403 por rol).
7. **Documentar**: agregar la fila en [API_ENDPOINTS.md](API_ENDPOINTS.md).

## Crear un servicio y una pantalla nuevos (frontend)

1. **Modelo**: copiar el DTO a `core/models/api.models.ts` (mismos nombres de campos).
2. **Servicio**: agregar un método a la clase `XxxApi` en `core/api/apis.ts`, o una clase nueva si es otro controlador:
   ```ts
   @Injectable({ providedIn: 'root' })
   export class AlgoApi {
     private readonly http = inject(HttpClient);
     listar() {
       return this.http.get<AlgoResponse[]>(`${environment.apiUrl}/algo`);
     }
   }
   ```
   El token lo agrega `authInterceptor`; no hay que ponerlo a mano.
3. **Página**: componente standalone en `features/portal/` o `features/panel/`, con `ChangeDetectionStrategy.OnPush`. Para datos remotos usar `rxResource`. Para formularios, Reactive Forms con las mismas validaciones que el DTO. Para errores, `leerError()` y `aplicarErroresServidor()`.
4. **Ruta**: agregarla con `loadComponent` en `portal.routes.ts` o `panel.routes.ts`. Si es solo para ADMIN: `canActivate: [roleGuard('ADMIN')]`.
5. **Menú**: agregar el item en `app.routes.ts` (`PORTAL` o `PANEL`). `roles` limita su visibilidad.
6. **UI**: reutilizar `shared/components` (`ua-page-header`, `ua-paginator`, `ua-empty`, badges…) y las clases de `styles.scss`. No agregar librerías de UI sin acordarlo con el equipo.
7. **Pruebas**: `*.spec.ts` junto al código (Vitest) y `npm test`.

## Integrar una funcionalidad completa

Orden recomendado: modelo y DTO → servicio y endpoint con pruebas → probar con Postman o curl → modelo y servicio Angular → pantalla → actualizar la documentación (`docs/`). Antes de abrir un PR, ejecutar `mvn test` en `backend/` y `npm test` y `npm run build` en `frontend/`.

## Buenas prácticas de Git

- **Ramas**: `main` siempre estable. Trabajar en `feature/<tema>`, `fix/<tema>` o `docs/<tema>` y unir por Pull Request.
- **Commits**: pequeños y descriptivos, en imperativo. Ejemplo: `feat(incidencias): agregar filtro por prioridad`. Prefijos sugeridos: `feat`, `fix`, `docs`, `refactor`, `test`, `chore`.
- **Antes de subir**: `git status` y `git diff --staged` para revisar qué entra. Nunca subir `.env`, `target/`, `node_modules/`, `dist/` ni archivos del IDE (ya están en `.gitignore`).
- **Secretos**: si un secreto llega a un commit, no basta con borrarlo en el commit siguiente. Hay que cambiarlo (generar un `JWT_SECRET` nuevo, cambiar la contraseña) y limpiar el historial si ya se publicó.
- **Sincronizar**: `git pull --rebase origin main` antes de subir para evitar commits de merge innecesarios.
