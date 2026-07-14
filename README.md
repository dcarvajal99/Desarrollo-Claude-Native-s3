# Desarrollo-Claude-Native

test01
Microservicio del **Sistema de Gestión de Pedidos y Generación de Guías de Despacho**
(*Desarrollo Cloud Native* · CDY2204 · Duoc UC).

Una empresa transportista genera **guías de despacho** en PDF, las guarda
temporalmente en un **EFS** y las publica en **AWS S3** organizadas por fecha y
transportista. Los endpoints se exponen detrás de un **API Gateway (AWS)** y se
autentican/autorizan con **Azure AD B2C (IDaaS)** mediante **OAuth2 + roles**.
Cada operación sobre las guías se publica de forma **asíncrona en RabbitMQ** (con
cola de errores *dead-letter*) y un consumidor la persiste en **PostgreSQL**.
El despliegue es automático vía **GitHub Actions** (Docker Hub → EC2).

> **Evolución del proyecto**
> - **Semana 3:** EFS + S3 + CI/CD (con login JWT propio, ya retirado).
> - **Semana 5:** se reemplaza el login propio por **OAuth2 Resource Server** validando
>   tokens JWT de **Azure AD** + registro de endpoints en **API Gateway**. Ver `SEMANA-05-OAUTH2-AZURE.md`.
> - **Semana 6:** se agrega **autorización por roles** con *custom claims* de **Azure AD B2C**.
>   Ver `SEMANA-06-ROLES.md`.
> - **Semana 8:** mensajería asíncrona con **dos colas RabbitMQ** (principal + *dead-letter*)
>   y consumidor que persiste los eventos en **PostgreSQL**. Ver sección *Mensajería asíncrona*.

## Arquitectura

Arquitectura por capas:

```
controller/  GuiaController        -> 6 endpoints REST + detalle
service/     GuiaService           -> orquesta el ciclo de vida de la guía
             EfsStorageService     -> genera el PDF (OpenPDF) y lo escribe en el EFS
             S3StorageService      -> sube/descarga/elimina en AWS S3 (SDK v2)
repository/  GuiaRepository        -> JpaRepository (H2 en memoria)
model/       Guia                  -> entidad JPA
dto/         CrearGuiaRequest, ActualizarGuiaRequest, GuiaDTO,
             ApiResponse, ErrorResponse, EntityMapper
config/      S3Config              -> bean S3Client (DefaultCredentialsProvider)
             SecurityConfig        -> OAuth2 Resource Server + autorización por roles
             OpenApiConfig         -> Swagger UI
             RabbitMQConfig        -> 2 colas (principal + dead-letter) + exchange DLX
service/     GuiaEventProducer     -> publica los eventos de guías en la Cola 1
             GuiaEventConsumer     -> consume la Cola 1 (guarda en BD) y la Cola 2 (errores)
model/       Guia, EventoGuia      -> entidades JPA (EventoGuia = tabla nueva de eventos)
aspect/      LoggingAspect         -> @Around sobre la capa service
exception/   GlobalExceptionHandler, ResourceNotFoundException, StorageException
```

Flujo de almacenamiento: **EFS (temporal) → S3 (definitivo)**.

## Mensajería asíncrona (RabbitMQ · Semana 8)

Cada operación sobre una guía (crear, actualizar, eliminar, subir a S3) publica un
**evento** en una cola de RabbitMQ. Un consumidor lo lee y lo guarda en una **tabla nueva**
(`eventos_guia`) en **PostgreSQL**. Si un mensaje falla, RabbitMQ lo redirige a una
**segunda cola** (patrón *dead-letter*) que almacena los mensajes con error.

```
Operación de guía
    └─► COLA 1  (guias.eventos.queue)
             ├─ OK    → consumidor → tabla eventos_guia (PostgreSQL)
             └─ falla → DLX (guias.dlx) → COLA 2 (guias.eventos.error.queue)
```

| Componente | Rol |
|---|---|
| `docker-compose.yml` | Levanta **RabbitMQ** (`rabbitmq:3-management`) y **PostgreSQL** (`postgres:16`) |
| Cola 1 `guias.eventos.queue` | Cola principal, durable, con `x-dead-letter-exchange` hacia el DLX |
| Exchange `guias.dlx` + Cola 2 `guias.eventos.error.queue` | Dead-letter: almacena los mensajes que fallan |
| `GuiaEventProducer` | Publica los eventos en la Cola 1 |
| `GuiaEventConsumer` | `@RabbitListener` de la Cola 1 (persiste; si falla → rechaza → Cola 2) y de la Cola 2 |
| `EventoGuia` / `eventos_guia` | Tabla nueva donde se guardan los eventos procesados |
| `GET /api/eventos` | Verifica los eventos guardados (rol `gestion`) |

**Demostrar el flujo de error:** en el `POST /api/guias`, enviar `"forzarError": true` marca
el evento para que el consumidor lo rechace y termine en la Cola 2.

### Perfiles de base de datos
- **Sin perfil** → **H2** en memoria (desarrollo rápido).
- **Perfil `postgres`** → **PostgreSQL** (contenedor Docker / EC2). La tabla `eventos_guia` queda ahí.
  Se activa con `SPRING_PROFILES_ACTIVE=postgres`.

## Seguridad — OAuth2 + Azure AD B2C + Roles

El backend es un **OAuth2 Resource Server**: NO emite tokens, **valida** los JWT (RS256)
emitidos por **Azure AD B2C**. De cada request comprueba firma, issuer, audiencia,
expiración y el **rol** del usuario.

### Doble capa de seguridad
1. **API Gateway (AWS):** un *JWT Authorizer* valida el token de Azure a nivel de API.
2. **Spring Security (backend):** vuelve a validar el token y, además, **autoriza por rol**.

### Dos roles (custom claim `extension_consultaRole`)

| Rol | Permiso |
|-----|---------|
| **gestion**  | Crear, subir a S3, actualizar, eliminar, consultar |
| **descarga** | SOLO descargar guías (y solo las **propias**) |

La **descarga** valida además que el `extension_nombreTransportista` del token sea el
**dueño** de la guía (si otro lo intenta, responde 403/404).

### Cómo obtener el token

No hay login propio. El token se obtiene desde **Azure AD B2C** (Run user flow → jwt.ms,
o con OAuth2 en Postman) y se envía en cada request:

```
Authorization: Bearer <token de Azure>
```

En **Swagger** usa el botón **Authorize** (candado) y pega el token.

## Endpoints REST (requieren token de Azure + rol)

| # | Método | Ruta | Rol | Descripción |
|---|--------|------|-----|-------------|
| 1 | `POST`   | `/api/guias`               | `gestion`  | Crear guía (genera el PDF en el EFS) |
| 2 | `POST`   | `/api/guias/{id}/s3`       | `gestion`  | Subir la guía generada a AWS S3 |
| 3 | `GET`    | `/api/guias/{id}/descargar`| `descarga` | Descargar guía (valida que seas el dueño) |
| 4 | `PUT`    | `/api/guias/{id}`          | `gestion`  | Modificar / actualizar guía (regenera PDF y re-sube a S3) |
| 5 | `DELETE` | `/api/guias/{id}`          | `gestion`  | Eliminar guía (de S3, EFS y BD) |
| 6 | `GET`    | `/api/guias?transportista=&fecha=` | `gestion` | Consultar guías por transportista y fecha |
|   | `GET`    | `/api/guias/{id}`          | `gestion`  | Detalle de una guía (apoyo a la demo) |

**Rutas públicas** (sin token): `/actuator/health`, `/actuator/info`, Swagger UI, consola H2.

### Comportamiento esperado

| Caso | Respuesta |
|------|-----------|
| Sin token | **401** |
| Token válido, rol que no aplica al endpoint | **403** |
| Rol `descarga` intenta descargar guía ajena | **403/404** (no es el dueño) |
| Rol y dueño correctos | **200** (+ PDF en descargar) |

### Organización en S3

Cada guía se sube con una *key* organizada por **fecha** y **transportista**:

```
{yyyyMMdd}/{transportista}/guia{numero}.pdf
ej:  20210101/transportistadiego/guia123.pdf
```

## Configuración

`src/main/resources/application.properties` (todo parametrizable por variables de entorno):

| Propiedad | Env var | Descripción |
|-----------|---------|-------------|
| `app.efs.path` | `EFS_PATH` | Carpeta del EFS (en el contenedor: `/app/efs`) |
| `app.s3.bucket`| `S3_BUCKET`| Bucket de S3 |
| `app.s3.region`| `AWS_REGION`| Región de AWS |
| `spring.security.oauth2.resourceserver.jwt.issuer-uri` | `OAUTH2_ISSUER_URI` | Issuer de Azure B2C (debe coincidir EXACTO con el `iss` del token, con `/v2.0/` final) |
| `spring.security.oauth2.resourceserver.jwt.jwk-set-uri` | `OAUTH2_JWKS_URI` | Claves públicas (jwks) de B2C para validar la firma |
| `app.oauth2.audience` | `OAUTH2_AUDIENCE` | Audiencia esperada (Client ID de la API) |
| `app.oauth2.role-claim` | `OAUTH2_ROLE_CLAIM` | Claim del rol (def: `extension_consultaRole`) |
| `app.oauth2.role-descarga` / `role-gestion` | — | Valores de los dos roles |
| `app.oauth2.transportista-claim` | `OAUTH2_TRANSPORTISTA_CLAIM` | Claim del dueño (def: `extension_nombreTransportista`) |

Las **credenciales de AWS** NO se escriben en el código: el SDK usa
`DefaultCredentialsProvider` (variables de entorno / instance profile /
`~/.aws/credentials`), compatible con las credenciales temporales de **AWS Academy**
(incluido `AWS_SESSION_TOKEN`).

## EFS en el contenedor

En la EC2 el disco EFS se monta en `/mnt/efs` y se inyecta al contenedor con::

```bash
docker run -d --name desarrollo-claude-native-s3 \
  -v /mnt/efs:/app/efs \
  -e EFS_PATH=/app/efs \
  -e S3_BUCKET=<bucket> -e AWS_REGION=us-east-1 \
  -e AWS_ACCESS_KEY_ID=... -e AWS_SECRET_ACCESS_KEY=... -e AWS_SESSION_TOKEN=... \
  -p 8080:8080 <usuario>/desarrollo-claude-native-s3:latest
```

Este montaje (`-v /mnt/efs:/app/efs`) ya está incluido en el paso 6 del pipeline
`.github/workflows/deploy.yml`. El issuer/audience de Azure tienen su valor por defecto
en `application.properties` (no requieren variables en el `docker run`).

## Ejecutar localmente

```bash
# 1) Levantar la infraestructura (RabbitMQ + PostgreSQL) en Docker
docker compose up -d
#    Consola RabbitMQ: http://localhost:15672  (guest/guest)
#    PostgreSQL:        localhost:5433  (BD guiasdb, user guias)

# 2a) Arrancar con H2 (rápido, sin perfil)
./mvnw spring-boot:run

# 2b) o arrancar con PostgreSQL (perfil postgres, como en la EC2)
SPRING_PROFILES_ACTIVE=postgres ./mvnw spring-boot:run

./mvnw test                   # corre las pruebas (mockean RabbitMQ/S3/Azure)
```

> En local, sin credenciales de AWS, los endpoints que tocan S3 fallarán con
> `502` (StorageException); el resto del flujo (crear/consultar/descargar desde EFS,
> y la publicación de eventos en RabbitMQ) funciona contra `./efs-local`.
>
> Para validar tokens reales de Azure B2C en local, la app necesita acceso a internet
> (descarga las claves públicas del `jwk-set-uri`).

## Pruebas

- `EfsStorageServiceTest` — genera un PDF real (firma `%PDF`) en un `@TempDir`, lo lee y lo elimina.
- `S3StorageServiceTest` — `S3Client` simulado con Mockito: verifica la key por fecha/transportista y subir/descargar/eliminar.
- `GuiaServiceTest` — orquestación de los 6 endpoints con EFS y S3 mockeados (incluida la validación de permisos de descarga).
- `GuiasApplicationTests` — el contexto Spring levanta (con `S3Client` y `JwtDecoder` mock, perfil `test`).

## Despliegue (CI/CD)

`push` a `main` dispara GitHub Actions: build de la imagen → push a Docker Hub →
SSH a la EC2 → `docker run` montando el EFS. Secrets requeridos en el repo:
`DOCKERHUB_USERNAME`, `DOCKERHUB_TOKEN`, `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`,
`AWS_SESSION_TOKEN`, `S3_BUCKET`, `EC2_HOST`, `USER_SERVER`, `EC2_SSH_KEY`.

> **AWS Academy:** las credenciales (`AWS_*`) son temporales; refréscalas en los
> *Secrets* de GitHub cada vez que reinicies el laboratorio antes de desplegar.
