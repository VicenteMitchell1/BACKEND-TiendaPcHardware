# Arquitectura de Microservicios - PC Hardware Store

Backend de la tienda de hardware **PC Hardware** (DSY1104 Desarrollo Fullstack II). Cada una de las **5 entidades del modelo relacional** es un microservicio Spring Boot autónomo con su propia base de datos, y un **API Gateway** centraliza la autenticación JWT, el control de acceso por rol (RBAC) y el enrutamiento.

**Stack:** Java 21 · Spring Boot 3.3.5 · Spring Security · JJWT 0.12 · Spring Data JPA · MySQL 8.4 (H2 en desarrollo) · springdoc-openapi (Swagger) · JUnit 5 + Mockito + JaCoCo · Docker Compose.

---

## Equipo

DSY1104 Desarrollo Fullstack II · Evaluación Parcial N° 3 · Duoc UC

| Integrante | RUT |
|---|---|
| Adán Valdebenito | 21.174.728-5 |
| Ronald Silva | 21.081.263-6 |
| Marcos Godoy | 20.882.869-K |
| Vicente Mitchell | 21.082.713-7 |

Repositorios: [backend](https://github.com/VicenteMitchell1/BACKEND-TiendaPcHardware) · [frontend](https://github.com/AdanValdebenitoDuocUC/pcHardware-FRONTEND)

---

## 1. Arquitectura

```
                     Frontend React (http://localhost:5173)
                                    │  REST + JWT (Authorization: Bearer)
                                    ▼
                ┌──────────────────────────────────────────┐
                │        API Gateway  ·  puerto 8080         │  ← único puerto expuesto
                │  Login/registro · refresh/logout · JWT     │
                │  RBAC por rol · proxy · Swagger tipado     │
                └───┬─────────┬─────────┬─────────┬─────────┬┘
       X-Internal-Token + X-User-Id/Email/Role (identidad tomada del JWT)
                    ▼         ▼         ▼         ▼         ▼
             usuario    categoria   producto   boleta    detalle      (red interna Docker)
              8081  ◄──►  8082  ◄──►  8083  ◄──  8084  ──►  8085
                │           │           │          │          │
                └───────────┴─────┬─────┴──────────┴──────────┘
                                  ▼
                   MySQL 8.4 · un esquema por servicio (pchw_*)
```

| Servicio | Puerto | Entidad | Esquema MySQL | Responsabilidad |
|---|---|---|---|---|
| `gateway-api` | 8080 | `REFRESH_TOKEN` | `pchw_gateway` | Punto único de entrada, emisión/renovación de JWT, RBAC, proxy |
| `usuario-service` | 8081 | `USUARIO` | `pchw_usuarios` | Usuarios y roles, hash BCrypt, verificación de credenciales |
| `categoria-service` | 8082 | `CATEGORIA` | `pchw_categorias` | Categorías; no permite borrar categorías con productos |
| `producto-service` | 8083 | `PRODUCTO` | `pchw_productos` | Catálogo, precios, ofertas, **descuento atómico de stock** |
| `boleta-service` | 8084 | `BOLETA`, `CUPON` | `pchw_boletas` | Compras: orquesta stock y detalles, calcula montos y cupones, anulaciones |
| `detalle-service` | 8085 | `DETALLE_BOLETA` | `pchw_detalles` | Líneas de detalle de cada boleta |

**Relaciones entre entidades** (`USUARIO ⇢ BOLETA → DETALLE_BOLETA ← PRODUCTO ← CATEGORIA`): al usar una base de datos por servicio no hay claves foráneas entre esquemas; la integridad referencial se valida por REST (p. ej. un producto sólo se crea si su categoría existe, y una categoría con productos no se puede eliminar).

---

## 2. Seguridad

| Mecanismo | Detalle |
|---|---|
| **Autenticación** | `POST /api/v1/auth/login` devuelve un **access token JWT** (1 h, firmado HMAC-SHA) y un **refresh token** (7 días). |
| **Renovación** | `POST /api/v1/auth/refresh` entrega un par nuevo. Cada refresh token es de **un solo uso** (rotación); si se reutiliza uno ya usado se revocan todas las sesiones del usuario. Sólo se guarda su hash SHA-256. |
| **Cierre de sesión** | `POST /api/v1/auth/logout` revoca el refresh token. |
| **Contraseñas** | BCrypt. La verificación ocurre dentro de `usuario-service`: el hash nunca viaja por la red. |
| **Roles (RBAC)** | `ADMIN` acceso total · `VENDEDOR` ve productos, boletas y detalles · `CLIENTE` tienda, compra y **sólo sus propias boletas**. |
| **Códigos** | `401` sin token / token inválido o expirado (el frontend debe renovar o pedir login) · `403` rol sin permiso o cuenta desactivada. |
| **Servicios internos** | Sólo aceptan peticiones con la cabecera `X-Internal-Token` (clave compartida) y no publican puertos en Docker. La identidad (`X-User-*`) la construye el gateway desde el JWT; el cliente no puede falsificarla. |
| **Secretos** | `JWT_SECRET`, `INTERNAL_API_KEY` y contraseñas de MySQL se leen de variables de entorno (`.env`, excluido de git). Sin ellos la aplicación no arranca. |

### Reglas de negocio de la compra (`POST /api/v1/boletas`)
1. `producto-service` descuenta el stock de **todos** los items en una sola transacción con un `UPDATE ... WHERE stock >= cantidad` atómico: o se descuenta todo o nada, y dos compras simultáneas nunca venden la misma unidad.
2. Los montos se calculan en el servidor con los precios del catálogo (se ignoran `total`, `subtotal`, `descuento` e `idUsuario` enviados por el cliente). Si se envía `codigoCupon`, boleta-service lo valida **antes** de tocar el stock (activo, sin distinguir mayúsculas) y aplica su porcentaje redondeado al peso; un código inválido responde `400`. La boleta guarda el código y el monto descontado. Cupones semilla: `HARDWARE2026` (15 %), `GAMER10` (10 %), `BIENVENIDO` (5 %).
3. Se guarda la boleta y luego sus detalles. Si `detalle-service` falla, se **repone el stock** (compensación) y la boleta se revierte → respuesta `503`.
4. Anular una boleta (`PUT` con `estado: ANULADA`) repone el stock; sólo las boletas anuladas se pueden eliminar.

---

## 3. Puesta en marcha

### Opción A — Docker (recomendada: usa MySQL persistente)
```powershell
.\setup-env.ps1                 # genera .env con secretos aleatorios (sólo la primera vez)
docker compose up -d --build    # MySQL + 5 microservicios + gateway
docker compose ps               # estado
docker compose logs -f gateway-api
docker compose down             # detener (los datos quedan en el volumen mysql-data)
docker compose down -v          # detener y BORRAR la base de datos
```
MySQL queda disponible sólo en la máquina local en `127.0.0.1:3307` (usuario `pchw`, clave `DB_PASSWORD` del `.env`) para revisarla con MySQL Workbench o DBeaver.

### Opción B — Sin Docker (Java 21 + PowerShell, base H2 en memoria)
```powershell
.\build-all.ps1      # compila, ejecuta los tests y genera los .jar (-SkipTests para omitirlos)
.\start-all.ps1      # crea .env si falta, inicia los 6 servicios y espera a que respondan (logs en .\logs)
.\stop-all.ps1       # detiene sólo los procesos Java de los puertos 8080-8085
```

### Credenciales de prueba (datos semilla)
| Rol | Email | Contraseña |
|---|---|---|
| ADMIN | `admin@duoc.cl` | `admin` |
| ADMIN | `profesor@profesor.duoc.cl` | `duoc` |
| VENDEDOR | `vendedor@duoc.cl` | `vendedor` |
| CLIENTE | `alumno@duoc.cl` | `1234` |
| CLIENTE | `pancho@gmail.com` | `gamer` (tiene la boleta `BOL-18392044`) |

---

## 4. API (vía gateway, `http://localhost:8080/api/v1`)

| Recurso | Endpoints | Acceso |
|---|---|---|
| Auth | `POST /auth/login`, `/auth/register`, `/auth/refresh`, `/auth/logout` | Público |
| Productos | `GET /productos`, `/productos/activos`, `/productos/{id}` · `POST`, `PUT /{id}`, `DELETE /{id}` | Lectura pública · escritura ADMIN |
| Categorías | `GET /categorias`, `/categorias/{id}` · `POST`, `PUT /{id}`, `DELETE /{id}` | Lectura pública · escritura ADMIN |
| Usuarios | `GET`, `GET /{id}`, `POST`, `PUT /{id}`, `DELETE /{id}` | ADMIN |
| Boletas | `GET /boletas` (ADMIN, VENDEDOR) · `GET /boletas/mis-boletas`, `/{id}`, `/numero/{n}`, `POST /boletas` (autenticados; CLIENTE sólo las suyas) · `PUT /{id}`, `DELETE /{id}` (ADMIN) | Según rol |
| Detalles | `GET /detalles`, `/{id}`, `/boleta/{id}` (ADMIN, VENDEDOR) · `POST`, `PUT /{id}`, `DELETE /{id}` (ADMIN) | Según rol |
| Cupones | `GET /cupones/validar/{codigo}` (autenticados) · `GET`, `GET /{id}`, `POST`, `PUT /{id}`, `DELETE /{id}` (ADMIN) | Según rol |

Errores con formato común: `{"timestamp", "status", "error", "detalles"?}` (400 validación, 401, 403, 404, 405, 409 duplicado/en uso, 503 servicio no disponible).

**Swagger UI:** [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html) — esquemas y ejemplos de cada cuerpo y respuesta. Para probar endpoints protegidos: hacer login, copiar `token` y pulsar **Authorize**.

---

## 5. Pruebas y cobertura
```powershell
cd producto-service; .\mvnw.cmd test     # o .\build-all.ps1 para todos
```
- 137 pruebas (unitarias con Mockito + integración con MockMvc/H2): reglas de negocio, validaciones, cupones, matriz de permisos por rol, ciclo JWT/refresh, atomicidad y concurrencia del stock (20 compras simultáneas sobre stock 10 → exactamente 10 éxitos).
- Reporte de cobertura JaCoCo por servicio: `<servicio>/target/site/jacoco/index.html`.

---

## 6. Estructura de cada microservicio
```
src/main/java/cl/pchardware/<servicio>/
├── controller/   endpoints REST (+ anotaciones Swagger)
├── service/      lógica de negocio y transacciones
├── repository/   Spring Data JPA
├── model/        entidades JPA
├── dto/          contratos de entrada/salida con validaciones (Jakarta Validation)
├── client/       llamadas REST a otros microservicios
├── config/       seguridad (clave interna), Swagger, RestTemplate, datos semilla
└── exception/    excepciones de negocio y manejador global de errores
```
