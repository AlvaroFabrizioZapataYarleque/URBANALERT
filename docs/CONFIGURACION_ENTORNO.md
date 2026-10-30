# Configuración del entorno

## Herramientas

| Herramienta | Versión | Origen |
|---|---|---|
| JDK | 17 | `pom.xml` → `<java.version>17</java.version>`. Probado con OpenJDK 17.0.21 |
| Maven | 3.6.3 o superior (requisito de Spring Boot 3.2). Probado con 3.10.0 | No hay Maven Wrapper (`mvnw`) en el proyecto |
| Node.js | `^22.22.3`, `^24.15.0` o `>=26` | Campo `engines` de `@angular/core` 22.2.4. Probado con 24.21.0 |
| npm | 11.x | `package.json` → `"packageManager": "npm@11.19.0"` |
| MySQL | 8 o superior (solo perfil `mysql`) | Comentario en `application-mysql.properties`. **Pendiente de confirmar** la versión que usa el equipo |
| Git | cualquier versión reciente | — |

## Puertos

| Servicio | Puerto | Dónde se configura |
|---|---|---|
| Backend Spring Boot | 8080 | `server.port` en `application.properties` |
| Frontend `ng serve` | 4200 | Valor por defecto de Angular CLI (`ng serve --port N` para cambiarlo) |
| MySQL | 3306 | URL de `application-mysql.properties` o `DB_URL` |

Si cambia el puerto del frontend, agregue el nuevo origen a `CORS_ALLOWED_ORIGINS`. Si cambia el del backend, actualice `apiUrl` en el frontend.

## Variables de entorno del backend

El backend las lee de las variables del sistema o del archivo `backend/.env`. Este archivo es opcional, Git lo ignora y se crea copiando `backend/.env.example`. La carga se hace con `spring.config.import=optional:file:.env[.properties]`. Las variables del sistema tienen prioridad sobre el archivo.

| Variable | Obligatoria | Por defecto | Uso |
|---|---|---|---|
| `JWT_SECRET` | **Sí** | — (sin valor por defecto) | Clave HS256 en Base64 de al menos 256 bits. Sin ella el backend no arranca: `Could not resolve placeholder 'JWT_SECRET'` |
| `JWT_EXPIRATION_MS` | No | `7200000` (2 h) | Vigencia del token |
| `CORS_ALLOWED_ORIGINS` | No | `http://localhost:4200` | Orígenes permitidos, separados por coma |
| `DB_URL` | No (solo `mysql`) | `jdbc:mysql://localhost:3306/urbanalert?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=America/Lima` | Conexión MySQL |
| `DB_USER` | No (solo `mysql`) | `root` | Usuario MySQL |
| `DB_PASSWORD` | No (solo `mysql`) | vacío | Contraseña MySQL |

Generar un secreto JWT:

```bash
openssl rand -base64 48
```

Cada desarrollador genera el suyo. Si alguien obtiene el secreto puede firmar tokens válidos, así que no debe compartirse ni subirse a Git.

Las pruebas (`mvn test`) **no** necesitan `.env`: usan una clave exclusiva de pruebas en `backend/src/test/resources/config/application.properties`.

## Perfiles de Spring

| Perfil | Activación | Base de datos | Notas |
|---|---|---|---|
| (por defecto) | `mvn spring-boot:run` | H2 en memoria | Consola H2 habilitada, `show-sql=true`. Los datos se pierden al detenerlo |
| `mysql` | `mvn spring-boot:run -Dspring-boot.run.profiles=mysql` o `SPRING_PROFILES_ACTIVE=mysql` | MySQL | Desactiva la consola H2 |

No hay un perfil de producción. Para desplegar: perfil `mysql`, `JWT_SECRET` propio, el origen real en `CORS_ALLOWED_ORIGINS` y desactivar los datos de prueba de `DataLoader`.

## Configuración del frontend

Angular no usa archivos `.env`. La configuración está en `frontend/src/environments/`:

| Archivo | Cuándo se usa | `apiUrl` |
|---|---|---|
| `environment.development.ts` | `npm start` / `ng serve` (configuración `development`) | `http://localhost:8080/api` |
| `environment.ts` | `npm run build` (configuración `production`) | `http://localhost:8080/api`; **cámbielo a la URL pública de la API antes de desplegar** |

El reemplazo de archivos está definido en `angular.json` (`fileReplacements`). Estos archivos no contienen secretos: todo lo que va al frontend es público.

## Desarrollo local: checklist

1. JDK 17, Maven y Node 22.22+/24.15+ instalados (`java -version`, `mvn -v`, `node -v`).
2. `cp backend/.env.example backend/.env` y completar `JWT_SECRET`.
3. Backend: `cd backend && mvn spring-boot:run` → `http://localhost:8080/api/publico/resumen` responde JSON.
4. Frontend: `cd frontend && npm install && npm start` → abrir `http://localhost:4200`.
5. Entrar con un usuario de prueba (ver `backend/README.md`).
