# UrbanAlert — Frontend (Angular 22)

Aplicación web de UrbanAlert con dos experiencias:

- **Portal ciudadano** (`/portal`, rol CIUDADANO): registrarse, reportar incidencias, ver su estado e historial, y editar el perfil.
- **Panel municipal** (`/panel`, roles ADMIN y OPERADOR): dashboard con indicadores, bandeja con filtros, gestión de incidencias (estado, área, prioridad), registro presencial, ciudadanos, áreas, usuarios y reportes.

Todos los datos vienen de la API Spring Boot de `../backend`.

## Versiones

| | Versión |
|---|---|
| Angular (core, router, forms, common) | **22.2.4** (CLI y `@angular/build` 22.2.5) |
| TypeScript | 6.0.3 (modo `strict` + `strictTemplates`) |
| RxJS | 7.8.2 |
| Node.js compatible | `^22.22.3`, `^24.15.0` o `>=26` (probado con 24.21.0) |
| npm | 11.x (`packageManager: npm@11.19.0`) |
| Pruebas | Vitest 5.0.5 + jsdom |

**Sin librerías de UI, iconos ni gráficos externas.** Los estilos son SCSS propios (`src/styles.scss`), los iconos son SVG en línea (`shared/components/icon.ts`, trazos de Lucide, licencia ISC) y los gráficos de barras son componentes CSS propios. La única dependencia externa en tiempo de ejecución es la fuente Inter de Google Fonts (`src/index.html`).

Características de Angular que se usan: componentes standalone, aplicación *zoneless*, signals y `rxResource`, Reactive Forms, carga diferida por página e interceptores HTTP funcionales.

## Instalación

```bash
cd frontend
node -v          # debe ser 22.22+ o 24.15+
npm install
```

Con nvm: `nvm install 24 && nvm use 24`.

## Configuración: URL del backend

Angular no usa `.env`. La URL de la API está en `src/environments/`:

```ts
// src/environments/environment.development.ts  (npm start)
export const environment = { production: false, apiUrl: 'http://localhost:8080/api' };

// src/environments/environment.ts  (npm run build)
export const environment = { production: true, apiUrl: 'http://localhost:8080/api' };
```

- Para otro puerto u host del backend, cambie `apiUrl` (debe terminar en `/api`).
- Antes de desplegar, ponga en `environment.ts` la URL pública de la API.
- El backend debe permitir el origen del frontend. Por defecto acepta `http://localhost:4200`; para otro origen, defina `CORS_ALLOWED_ORIGINS` en `backend/.env`.

No ponga secretos en estos archivos: todo lo que se compila en el frontend lo puede leer cualquier usuario.

## Comandos (`package.json`)

| Comando | Qué hace |
|---|---|
| `npm start` | `ng serve`: servidor de desarrollo en `http://localhost:4200` |
| `npm run build` | `ng build`: build de producción en `dist/urbanalert-web/browser/` |
| `npm run watch` | Build de desarrollo que se recompila al guardar |
| `npm test` | `ng test`: pruebas unitarias con Vitest (modo watch) |
| `npm test -- --watch=false` | Ejecuta las pruebas una sola vez (CI) |

## Ejecución local

1. Inicie el backend: `cd ../backend && mvn spring-boot:run`. Ver [backend/README.md](../backend/README.md).
2. En otra terminal:
   ```bash
   cd frontend
   npm start
   ```
3. Abra `http://localhost:4200` y entre con un usuario de prueba, por ejemplo `admin` / `Admin123*` o `maria` / `Ciudadano123*`.

## Build de producción

```bash
npm run build
```

Genera archivos estáticos en `dist/urbanalert-web/browser/`, que se pueden servir con cualquier servidor web. Como la aplicación usa rutas de Angular, el servidor debe devolver `index.html` para cualquier ruta desconocida.

## Pruebas

```bash
npm test -- --watch=false
```

`src/app/core/core.spec.ts` cubre la sesión, los permisos por rol, los guards, los interceptores (Bearer y sesión expirada), la lectura de errores del backend y la búsqueda y paginación de tablas.

## Estructura

```
src/app
├── core/       auth/ (sesión con signals, permisos), guards/, interceptors/, api/ (un servicio por controlador),
│               models/ (DTO del backend + catálogos), services/ (errores, toasts, confirmación)
├── shared/     components/ (icon, badges, ui, historial, ficha y campos de incidencia), pipes/, utils/
├── layouts/    public-layout/ (login, registro) y shell/ (sidebar + topbar)
└── features/
    ├── auth/   login, registro
    ├── portal/ inicio, reportar, mis incidencias, detalle, perfil
    └── panel/  inicio (dashboard), bandeja, gestión, registro presencial, ciudadanos, áreas, usuarios, reportes
```

Para agregar pantallas o servicios, vea [docs/GUIA_DESARROLLO.md](../docs/GUIA_DESARROLLO.md).

## Problemas frecuentes

| Síntoma | Solución |
|---|---|
| `Node.js version v18... is not supported by Angular CLI` | Usar Node 22.22+ o 24.15+ (`nvm use 24`) |
| "No se pudo conectar con el servidor" al iniciar sesión | El backend no está corriendo o `apiUrl` no es correcta. Probar `curl http://localhost:8080/api/publico/resumen` |
| Error CORS en la consola del navegador | Agregar el origen del frontend a `CORS_ALLOWED_ORIGINS` del backend y reiniciarlo |
| `Port 4200 is already in use` | `npm start -- --port 4300`, y agregar `http://localhost:4300` a `CORS_ALLOWED_ORIGINS` |
| La sesión se cierra sola | El token venció (2 h por defecto, `JWT_EXPIRATION_MS`) o el backend se reinició con otro `JWT_SECRET` |
| `npm install` falla con errores de peer dependencies | Verificar la versión de Node; borrar `node_modules` y volver a ejecutar `npm install` (no usar `--force`) |
