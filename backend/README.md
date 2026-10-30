# UrbanAlert — Backend (API REST)

API REST en Spring Boot para registrar y seguir incidencias de infraestructura urbana: alumbrado, pistas, veredas, semáforos, residuos, señalización e infraestructura pública. Expone autenticación JWT, gestión de incidencias con historial, áreas responsables, usuarios e indicadores.

## Información general

| | |
|---|---|
| Lenguaje | Java **17** |
| Framework | Spring Boot **3.2.5** (Web, Data JPA, Validation, Security) |
| Construcción | Maven (no hay Maven Wrapper; se usa `mvn` instalado) |
| Autenticación | JWT HS256 con jjwt **0.12.5**; contraseñas con BCrypt |
| Persistencia | JPA/Hibernate · H2 **2.2.224** (por defecto) o MySQL con `mysql-connector-j` **8.3.0** (perfil `mysql`) |
| Otras | Lombok 1.18.32, Spring Boot DevTools |
| Pruebas | JUnit 5, Mockito, MockMvc, AssertJ (`spring-boot-starter-test`) |
| Puerto | 8080 |

**Arquitectura en capas** (`src/main/java/com/utp/urbanalert`):

```
config/      SecurityConfig (rutas públicas, CORS, JWT), DataLoader (datos iniciales)
controller/  Endpoints REST (seguridad por método con @PreAuthorize / @Secured)
service/     Interfaces + impl/ con las reglas de negocio y @Transactional
repository/  Spring Data JPA + consultas JPQL
model/       Entidades y enums (NombreRol, Permiso, EstadoIncidencia, ...)
dto/         Request/Response con Bean Validation
mapper/      Entidad → DTO
security/    JwtService, JwtAuthenticationFilter, UserDetailsService, manejadores 401/403
exception/   Excepciones de negocio + GlobalExceptionHandler
```

Roles: `ADMIN`, `OPERADOR` y `CIUDADANO`. La tabla de permisos está en [docs/ARQUITECTURA.md](../docs/ARQUITECTURA.md#autorización-por-roles) y la lista de endpoints en [docs/API_ENDPOINTS.md](../docs/API_ENDPOINTS.md).

## Requisitos previos

- **JDK 17** (`java -version`). Por ejemplo, en macOS: `brew install openjdk@17`.
- **Maven 3.6.3+** (`mvn -v`). En macOS: `brew install maven`.
- **Base de datos**: ninguna para desarrollo (usa H2 en memoria). Para persistencia real: **MySQL 8+** con el perfil `mysql`.
- **OpenSSL** u otra herramienta para generar el secreto JWT.
- Opcional: Postman, para la colección en `postman/`.

## Configuración del proyecto

```bash
git clone https://github.com/AlvaroFabrizioZapataYarleque/URBANALERT.git
cd URBANALERT/backend
cp .env.example .env
```

Edite `backend/.env` y complete **`JWT_SECRET`** con un valor generado así:

```bash
openssl rand -base64 48
```

`.env` está en `.gitignore`. El backend lo lee al arrancar (`spring.config.import=optional:file:.env[.properties]`), por eso debe ejecutarse desde la carpeta `backend/`. También puede definir las variables directamente en el sistema (`export JWT_SECRET=...`), que tienen prioridad sobre el archivo. Todas las variables están explicadas en [docs/CONFIGURACION_ENTORNO.md](../docs/CONFIGURACION_ENTORNO.md).

Maven descarga las dependencias la primera vez que compila. Para descargarlas antes:

```bash
mvn dependency:resolve
```

## Configuración de base de datos

### Opción A — H2 en memoria (por defecto, sin instalar nada)

No requiere configuración. La base `urbanalertdb` se crea al arrancar y se borra al detener la aplicación.

- Consola: `http://localhost:8080/h2-console`
- JDBC URL: `jdbc:h2:mem:urbanalertdb` · Usuario: `sa` · Contraseña: vacía

### Opción B — MySQL (perfil `mysql`)

1. Inicie MySQL 8+ en `localhost:3306`.
2. La base `urbanalert` se crea sola (`createDatabaseIfNotExist=true`) si el usuario tiene permisos. Para crearla con un usuario propio, vea [docs/BASE_DE_DATOS.md](../docs/BASE_DE_DATOS.md#mysql).
3. En `backend/.env` complete `DB_USER` y `DB_PASSWORD`, y `DB_URL` si cambia el host, el puerto o el nombre.
4. Arranque con el perfil `mysql` (ver Ejecución).

**Migraciones**: no hay Flyway, Liquibase ni scripts SQL. Hibernate crea y actualiza las tablas (`ddl-auto=update`) y `DataLoader` inserta los datos iniciales la primera vez.

**Verificar la conexión**: el log debe mostrar `Started UrbanAlertApplication` sin errores de `HikariPool`, y este comando debe responder JSON:

```bash
curl http://localhost:8080/api/publico/resumen
```

## Ejecución

Desde `backend/`:

```bash
# H2 en memoria (por defecto)
mvn spring-boot:run

# MySQL
mvn spring-boot:run -Dspring-boot.run.profiles=mysql
```

Con el JAR empaquetado (también desde `backend/`, para que lea `.env`):

```bash
mvn clean package
java -jar target/urbanalert-api-0.2.0.jar
java -jar target/urbanalert-api-0.2.0.jar --spring.profiles.active=mysql
```

API disponible en `http://localhost:8080/api`.

### Usuarios de prueba (creados por `DataLoader`)

| Usuario | Contraseña | Rol |
|---|---|---|
| `admin` | `Admin123*` | ADMIN |
| `op.alumbrado` | `Operador123*` | OPERADOR (Alumbrado Publico) |
| `op.pistas` | `Operador123*` | OPERADOR (Pistas y Veredas) |
| `op.semaforos` | `Operador123*` | OPERADOR (Semaforizacion) |
| `maria`, `jose`, `lucia` | `Ciudadano123*` | CIUDADANO |

Son credenciales de demostración y se crean en **todos** los perfiles, incluido `mysql`. Cámbielas o desactive la carga de ejemplo antes de usar una base real.

## Pruebas

```bash
mvn test            # 73 pruebas: unitarias (Mockito) + integración de seguridad (MockMvc)
mvn clean package   # compila, ejecuta las pruebas y genera target/urbanalert-api-0.2.0.jar
```

Las pruebas no necesitan `.env` ni base de datos externa. Usan H2 y una clave JWT exclusiva de pruebas (`src/test/resources/config/application.properties`).

| Clase | Qué prueba |
|---|---|
| `JwtServiceTest` | Generación y validación de tokens (vencido, manipulado, otra clave, secreto débil) |
| `AuthServiceTest`, `IncidenciaServiceTest` | Reglas de negocio con Mockito |
| `SeguridadIntegrationTest` | Aplicación completa: 401/403 por rol, filtros y flujo de una incidencia |

**Verificar que el servidor funciona**:

```bash
curl http://localhost:8080/api/publico/resumen
curl -X POST http://localhost:8080/api/auth/login -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"Admin123*"}'
```

La colección `postman/UrbanAlert-AP2.postman_collection.json` cubre todos los endpoints. Su variable `baseUrl` es `http://localhost:8080`.

## Problemas frecuentes

| Síntoma | Causa | Solución |
|---|---|---|
| `Could not resolve placeholder 'JWT_SECRET'` | Falta el secreto | Crear `backend/.env` con `JWT_SECRET` o exportar la variable. Ejecutar desde `backend/` |
| `WeakKeyException` al arrancar | El secreto tiene menos de 256 bits o no está en Base64 | Generarlo con `openssl rand -base64 48` |
| `Port 8080 was already in use` | Otra aplicación usa el puerto | `lsof -i :8080` para identificarla, o arrancar con `mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=8081` (y actualizar `apiUrl` del frontend) |
| `Communications link failure` / `Connection refused` (perfil `mysql`) | MySQL no está iniciado o la URL o el puerto no son correctos | Iniciar MySQL y revisar `DB_URL` |
| `Access denied for user` | Credenciales MySQL incorrectas | Revisar `DB_USER` y `DB_PASSWORD` en `.env` |
| `Unable to locate a Java Runtime` / `invalid target release: 17` | Falta el JDK 17 o Maven usa otro JDK | Instalar el JDK 17 y apuntar `JAVA_HOME` a él (`mvn -v` muestra cuál usa) |
| El frontend recibe errores CORS | El origen no está permitido | Agregarlo a `CORS_ALLOWED_ORIGINS` |
| Los datos desaparecen al reiniciar | Perfil H2 (en memoria) | Usar el perfil `mysql` |
| Dependencias que no descargan | Sin red o caché corrupta | `mvn -U dependency:resolve`; si persiste, borrar el artefacto en `~/.m2/repository` |
