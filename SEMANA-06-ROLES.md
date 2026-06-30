# Semana 6 — Autorización por ROLES con Custom Claims de Azure AD B2C

Actividad **sumativa** (CDY2204, Exp2, S6). Continúa sobre el microservicio de guías de la
Semana 5, agregando **autorización basada en roles** mediante un *custom claim* de Azure AD B2C.

---

## 1. Qué pide la guía

Crear **2 roles** en Azure B2C (custom claim `extension_consultaRole`):

| Rol | Permiso |
|-----|---------|
| **descarga** | SOLO el endpoint de descargar guías |
| **gestion**  | El resto: crear, subir a S3, actualizar, eliminar, consultar |

La autorización se aplica en el backend con Spring Security, leyendo el rol del token.

---

## 2. Custom claims usados (2)

| Claim en el token | Se crea en Azure como | Para qué |
|-------------------|------------------------|----------|
| `extension_consultaRole` | `consultaRole` | **Autorización**: rol del usuario (`descarga` / `gestion`) |
| `extension_nombreTransportista` | `nombreTransportista` | **Identidad de negocio**: nombre del transportista dueño de la guía |

> Azure B2C antepone `extension_` automáticamente a los custom attributes.

---

## 3. Cambios de código

### `application.properties`
```properties
# Issuer B2C (OJO: termina en /v2.0/ con slash final, debe coincidir con el iss del token)
spring.security.oauth2.resourceserver.jwt.issuer-uri=https://duoc2026grp2.b2clogin.com/23369869-d28c-4ace-89dd-d87bf705f9c3/v2.0/
# jwks_uri (claves públicas; en B2C conviene darlo explícito)
spring.security.oauth2.resourceserver.jwt.jwk-set-uri=https://duoc2026grp2.b2clogin.com/duoc2026grp2.onmicrosoft.com/B2C_1_registro_login/discovery/v2.0/keys
app.oauth2.audience=54a7a174-e59c-468e-a9f8-d821e51a900c
# Roles
app.oauth2.role-claim=extension_consultaRole
app.oauth2.role-descarga=descarga
app.oauth2.role-gestion=gestion
# Dueño de la guía
app.oauth2.transportista-claim=extension_nombreTransportista
```

### `SecurityConfig.java`
- `JwtAuthenticationConverter` que lee el claim `extension_consultaRole` y lo mapea a `ROLE_xxx`.
  Es **robusto**: acepta el claim como String simple (`"descarga"`), lista, o varios separados por espacio/coma.
- Reglas por ruta+método (el orden importa, lo específico primero):
  - `GET /api/guias/*/descargar` → `hasRole("descarga")`
  - `POST /api/guias`, `POST /api/guias/*/s3`, `PUT`, `DELETE`, `GET /api/guias`, `GET /api/guias/*` → `hasRole("gestion")`

### `GuiaController.java`
- El endpoint *descargar* identifica al dueño con el claim `extension_nombreTransportista`
  (respaldo al `sub` si no viene).

---

## 4. Cómo se comporta (probado en local)

| Caso | Resultado |
|------|-----------|
| Sin token | **401** |
| Token válido con rol que NO aplica (ej. `Transporte1`) | **403** "Su rol no tiene permiso" |
| Token con rol `gestion` → crear/listar/etc. | **200** |
| Token con rol `descarga` → descargar | **200** (si es el dueño) |
| Token con rol `gestion` → intentar descargar | **403** (descarga es solo rol `descarga`) |
| Token con rol `descarga` → intentar crear | **403** |

> El 403 (no 401) con un token de B2C confirma que la **firma, issuer y audiencia se validaron OK**;
> solo falló la **autorización por rol**. Toda la cadena RBAC funciona.

---

## 5. Qué configurar en Azure B2C (pasos de la guía, Figuras 1–9)

1. **User Attributes → Add** → crear `consultaRole` (String). → llega como `extension_consultaRole`.
2. **User Attributes → Add** → crear `nombreTransportista` (String). → `extension_nombreTransportista`.
3. **User Flows** → tu flujo `B2C_1_registro_login` → **Application claims**: marca ambos atributos
   para que salgan en el token. En **User attributes** (página de signup) también, si quieres pedirlos al registrar.
4. Crea/edita 2 usuarios de prueba:
   - Usuario A: `consultaRole = gestion`, `nombreTransportista = TransportistaDiego`
   - Usuario B: `consultaRole = descarga`, `nombreTransportista = TransportistaDiego`
5. Ejecuta el User Flow (Run user flow) → jwt.ms → verifica que el token trae ambos claims.

> Las guías demo en `data.sql` usan `transportista = TransportistaDiego`. Asigna ese MISMO valor
> al `nombreTransportista` de tu usuario para poder descargarlas.

---

## 6. API Gateway

El JWT Authorizer del API Gateway (igual que S5) valida el token de Azure a nivel de API.
La autorización **por rol** la hace el backend (Spring Security), no el Gateway: el Gateway
deja pasar cualquier token válido de Azure; el rol se evalúa dentro de la app.
Issuer/audience del authorizer del Gateway deben actualizarse a los de B2C si cambiaste de tenant.
