# Semana 5 — Migración a OAuth2 / Azure AD (IDaaS) + API Gateway

Este documento resume los cambios que se hicieron al microservicio de **guías de despacho**
(originalmente de la Semana 3) para cumplir la actividad formativa de la **Semana 5**:
autenticar los endpoints con un **IDaaS (Azure AD)** validando tokens **JWT vía OAuth2**, y
exponerlos detrás de un **API Manager (AWS API Gateway)**.

---

## 1. Qué cambió (resumen)

| Antes (Semana 3)                            | Ahora (Semana 5)                                            |
|---------------------------------------------|------------------------------------------------------------|
| Login propio `/api/auth/login` + `/register`| **Eliminado.** La identidad la entrega Azure AD            |
| JWT casero firmado HS512 (jjwt)             | **JWT de Azure AD** validados como OAuth2 Resource Server  |
| `JwtAuthenticationFilter` propio            | Filtro `BearerTokenAuthenticationFilter` de Spring Security|
| Tabla `transportistas` + BCrypt            | **Eliminada.** Sin usuarios locales                        |
| `authentication.getName()` = username local | `jwt.getSubject()` = claim **`sub`** del token de Azure    |

### Archivos modificados
- `pom.xml` — se quitó `jjwt-*` y se agregó `spring-boot-starter-oauth2-resource-server`.
- `config/SecurityConfig.java` — reescrito como **OAuth2 Resource Server** (valida issuer + audiencia + firma).
- `resources/application.properties` — se quitó `app.jwt.*` y se agregó `issuer-uri` + `audience`.
- `controller/GuiaController.java` — el endpoint *descargar* lee el dueño del claim `sub` del token.
- `resources/data.sql` — se quitó la tabla de usuarios; las guías demo usan el `sub` como dueño.
- `.github/workflows/deploy.yml` — **sin cambios funcionales** (ver nota sobre la Figura 9 abajo).

### Archivos eliminados
`JwtService`, `JwtAuthenticationFilter`, `CustomUserDetailsService`, `AuthController`,
`AuthService`, `model/Transportista`, `TransportistaRepository`, `TransportistaDTO`,
`LoginRequest`, `LoginResponse`, `RegisterRequest`, y los tests `JwtServiceTest` y `AuthSecurityTest`.

---

## 2. Configuración de Azure (tus valores reales)

Sacados del token decodificado en **jwt.ms** (Figura 6 de la guía):

```
issuer (iss) : https://login.microsoftonline.com/23369869-d28c-4ace-89dd-d87bf705f9c3/v2.0
audience(aud): 54a7a174-e59c-468e-a9f8-d821e51a900c   (Client ID de tu API)
sub          : 63afb75d-7d95-440b-bad0-e2568da64751   (service principal / dueño de las guías demo)
```

> **Nota:** tu token es de tipo **Client Credentials** (máquina-a-máquina, claim `azpacr=1`).
> Por eso NO trae email ni nombre de persona, y el único identificador estable es `sub`.
> Tu `issuer` es de `login.microsoftonline.com` (Entra ID estándar), **no** de `b2clogin.com`
> — el "gotcha" del issuer de B2C que muestra la guía no aplica en tu caso.

En `application.properties` (con override por variable de entorno para producción):

```properties
spring.security.oauth2.resourceserver.jwt.issuer-uri=${OAUTH2_ISSUER_URI:https://login.microsoftonline.com/23369869-d28c-4ace-89dd-d87bf705f9c3/v2.0}
app.oauth2.audience=${OAUTH2_AUDIENCE:54a7a174-e59c-468e-a9f8-d821e51a900c}
```

---

## 3. Cómo probar localmente

1. Arranca la app:
   ```bash
   mvn spring-boot:run
   ```
2. **Sin token** → `GET http://localhost:8080/api/guias` debe devolver **401** (Figura 7 de la guía).
3. **Obtén un token** desde Azure (Postman → Authorization → OAuth 2.0 → *Client Credentials*,
   con tu Access Token URL, Client ID, Secret y Scope — Figura 16).
4. **Con token** → repite la llamada con `Authorization: Bearer <token>` → **200 OK**.
5. **Descargar guía:** `GET /api/guias/1/descargar` con tu token funciona (guía 1001 es tuya,
   su dueño es tu `sub`). `GET /api/guias/3/descargar` da error de permiso (guía 1003 es de otro dueño).

> ⚠️ **Importante sobre el dueño:** como el token Client Credentials siempre trae el mismo `sub`
> (`63afb75d-...`), para que la validación de "solo el dueño descarga" funcione, la guía debe
> tener ese `sub` como `transportista`. Las guías demo 1001/1002 ya lo tienen. Si **creas** una
> guía nueva y quieres poder descargarla, ponle `"transportista": "63afb75d-7d95-440b-bad0-e2568da64751"`.

---

## 4. Sobre el step "Create application.yml" de la guía (Figura 9) — NO se agrega

La guía pide añadir al pipeline un step que genera un `application.yml` con un bloque
`cloud.aws.credentials.*`. **Ese step NO se agregó a este proyecto, a propósito**, porque:

- Ese formato (`cloud.aws.*`) es de la librería **Spring Cloud AWS**, que usa la app base
  de la guía (`bdget`).
- **Este proyecto usa el AWS SDK v2** (`software.amazon.awssdk:s3`) con
  `DefaultCredentialsProvider`, que lee las credenciales desde **variables de entorno**
  (`AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`, `AWS_SESSION_TOKEN`).
- Esas variables **ya se inyectan** en el `docker run` del `deploy.yml` (Paso 6).

Por lo tanto, generar ese `application.yml` no aportaría nada: la app lo ignoraría. Las
credenciales AWS ya llegan correctamente por otra vía. (Para el video: este es un buen punto
para demostrar que se entiende el pipeline, no que se copió.)

---

## 5. Lado AWS (API Gateway) — pasos en consola (Figuras 12–15)

1. Despliega el proyecto a tu EC2 con el pipeline (`git push` a `main`).
2. Llama al endpoint por la **IP elástica** → debe dar **401** (aún sin token, Figura 12).
3. En **API Gateway** → tu API → **Routes** → la ruta → **Integration** → *Configure* (Figura 13).
4. Edita la **Integration URL** → `http://<TU-IP-ELASTICA>:8080/api/guias` (Figura 14).
5. **Deploy** a un stage (p.ej. `desarrollo`) → obtienes la **URL base securitizada** (Figura 15).
6. En **Postman**, usa esa URL + el token de Azure (Authorization OAuth 2.0) → **200 OK** (Figura 16).

Con esto tienes la **doble autenticación**: API Gateway (nivel API) + Spring Security (nivel app),
ambos respaldados por el IDaaS (Azure AD).
