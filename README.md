# Desarrollo-Claude-Native-s3

Microservicio del **Sistema de Gestión de Pedidos y Generación de Guías de Despacho**
(Semana 3 · *Desarrollo Cloud Native* · CDY2204 · Duoc UC).

Una empresa transportista genera **guías de despacho** en PDF, las guarda
temporalmente en un **EFS** y las publica en **AWS S3** organizadas por fecha y
transportista. El despliegue es automático vía **GitHub Actions** (Docker Hub → EC2).

## Arquitectura

Arquitectura por capas (mismo patrón que la Semana 1):

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
aspect/      LoggingAspect         -> @Around sobre la capa service
exception/   GlobalExceptionHandler, ResourceNotFoundException, StorageException
```

Flujo de almacenamiento: **EFS (temporal) → S3 (definitivo)**.

## Seguridad (JWT)

Los endpoints de `/api/guias/**` requieren un **token JWT**. El flujo es:

1. `POST /api/auth/login` con `{ "username": "...", "password": "..." }` → devuelve un `token`.
2. Enviar ese token en cada request a `/api/guias` con el header
   `Authorization: Bearer <token>`.

En **Swagger** usa el botón **Authorize** (candado) y pega solo el token.

**Transportistas precargados (demo):**

| Usuario | Clave | Rol |
|---------|-------|-----|
| `TransportistaX` | `1234` | TRANSPORTISTA |
| `TransportistaHermes` | `1234` | TRANSPORTISTA |
| `admin` | `admin1234` | ADMIN |

La **descarga** valida que el transportista del token sea el **dueño** de la guía
(si otro intenta descargarla, responde 404).

### Endpoints de autenticación (abiertos)

| Método | Ruta | Descripción |
|--------|------|-------------|
| `POST` | `/api/auth/login`    | Inicia sesión; devuelve token JWT |
| `POST` | `/api/auth/register` | Registra un transportista nuevo (clave BCrypt) |

## Endpoints REST (requieren token)

| # | Método | Ruta | Descripción |
|---|--------|------|-------------|
| 1 | `POST`   | `/api/guias`               | Crear guía (genera el PDF en el EFS) |
| 2 | `POST`   | `/api/guias/{id}/s3`       | Subir la guía generada a AWS S3 |
| 3 | `GET`    | `/api/guias/{id}/descargar`| Descargar guía con validación de permisos (dueño = transportista del token) |
| 4 | `PUT`    | `/api/guias/{id}`          | Modificar / actualizar guía (regenera PDF y re-sube a S3) |
| 5 | `DELETE` | `/api/guias/{id}`          | Eliminar guía (de S3, EFS y BD) |
| 6 | `GET`    | `/api/guias?transportista=&fecha=` | Consultar guías por transportista y fecha |
|   | `GET`    | `/api/guias/{id}`          | Detalle de una guía (apoyo a la demo) |

### Organización en S3

Cada guía se sube con una *key* organizada por **fecha** y **transportista**:

```
{yyyyMMdd}/{transportista}/guia{numero}.pdf
ej:  20210101/transportistax/guia123.pdf
```

## Configuración

`src/main/resources/application.properties` (todo parametrizable por variables de entorno):

| Propiedad | Env var | Default | Descripción |
|-----------|---------|---------|-------------|
| `app.efs.path` | `EFS_PATH` | `./efs-local` | Carpeta del EFS (en el contenedor: `/app/efs`) |
| `app.s3.bucket`| `S3_BUCKET`| `guias-despacho-duoc` | Bucket de S3 |
| `app.s3.region`| `AWS_REGION`| `us-east-1` | Región de AWS |
| `app.s3.endpoint`| `S3_ENDPOINT`| *(vacío)* | Endpoint S3 alternativo (tests / emuladores) |

Las **credenciales de AWS** NO se escriben en el código: el SDK usa
`DefaultCredentialsProvider` (variables de entorno / instance profile /
`~/.aws/credentials`), compatible con las credenciales temporales de **AWS Academy**
(incluido `AWS_SESSION_TOKEN`).

## EFS en el contenedor

En la EC2 el disco EFS se monta en `/mnt/efs` y se inyecta al contenedor con:

```bash
docker run -d --name desarrollo-claude-native-s3 \
  -v /mnt/efs:/app/efs \
  -e EFS_PATH=/app/efs \
  -e S3_BUCKET=<bucket> -e AWS_REGION=us-east-1 \
  -e AWS_ACCESS_KEY_ID=... -e AWS_SECRET_ACCESS_KEY=... -e AWS_SESSION_TOKEN=... \
  -p 8080:8080 <usuario>/desarrollo-claude-native-s3:latest
```

Este montaje (`-v /mnt/efs:/app/efs`) ya está incluido en el paso 6 del pipeline
`.github/workflows/deploy.yml`.

## Ejecutar localmente

```bash
./mvnw spring-boot:run        # http://localhost:8080
./mvnw test                   # corre las pruebas (EFS + S3 + servicio)
```

> En local, sin credenciales de AWS, los endpoints que tocan S3 fallarán con
> `502` (StorageException); el resto del flujo (crear/consultar/descargar desde EFS)
> funciona contra `./efs-local`.

## Pruebas

- `EfsStorageServiceTest` — genera un PDF real (firma `%PDF`) en un `@TempDir`, lo lee y lo elimina.
- `S3StorageServiceTest` — `S3Client` simulado con Mockito: verifica la key por fecha/transportista y subir/descargar/eliminar.
- `GuiaServiceTest` — orquestación de los 6 endpoints con EFS y S3 mockeados (incluida la validación de permisos de descarga).
- `GuiasApplicationTests` — el contexto Spring levanta (con `S3Client` mock).

## Despliegue (CI/CD)

`push` a `main` dispara GitHub Actions: build de la imagen → push a Docker Hub →
SSH a la EC2 → `docker run` montando el EFS. Secrets requeridos en el repo:
`DOCKERHUB_USERNAME`, `DOCKERHUB_TOKEN`, `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`,
`AWS_SESSION_TOKEN`, `S3_BUCKET`, `EC2_HOST`, `USER_SERVER`, `EC2_SSH_KEY`.
