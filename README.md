# Arquitectura de Microservicios - PC Hardware Store

Este directorio contiene la arquitectura de **microservicios distribuidos** donde **cada una de las 5 entidades relacionales es un microservicio autónomo e independiente**, orquestados por un **API Gateway** centralizado con autenticación JWT y control de acceso basado en roles (RBAC).

---

## 1. Diagrama de Arquitectura y Puertos

```
                              ┌────────────────────────────────────────┐
                              │  Frontend React (pcHardware-FRONTEND)  │
                              │         http://localhost:5173          │
                              └───────────────────┬────────────────────┘
                                                  │ REST (JWT Bearer)
                                                  ▼
                              ┌────────────────────────────────────────┐
                              │        API Gateway (gateway-api)       │
                              │         http://localhost:8080          │
                              │     Spring Security + JJWT + RBAC      │
                              └───────┬──────────┬──────────┬──────────┘
                                      │          │          │
         ┌────────────────────────────┼──────────┼──────────┼────────────────────────────┐
         │                            │          │          │                            │
         ▼                            ▼          ▼          ▼                            ▼
┌──────────────────┐        ┌──────────────────┐ │ ┌──────────────────┐        ┌──────────────────┐
│ usuario-service  │        │categoria-service │ │ │  boleta-service  │        │ detalle-service  │
│  (Puerto 8081)   │        │  (Puerto 8082)   │ │ │  (Puerto 8084)   │───────▶│  (Puerto 8085)   │
│ Entidad: USUARIO │        │Entidad: CATEGORIA│ │ │ Entidad: BOLETA  │ (REST) │Entidad: DETALLE  │
│ DB: usuariodb    │        │ DB: categoriadb  │ │ │ DB: boletadb     │        │ DB: detalledb    │
└──────────────────┘        └──────────────────┘ │ └────────┬─────────┘        └──────────────────┘
                                                 │          │
                                                 │ (REST)   │ (REST: Stock / Validar)
                                                 ▼          ▼
                                            ┌──────────────────┐
                                            │ producto-service │
                                            │  (Puerto 8083)   │
                                            │Entidad: PRODUCTO │
                                            │ DB: productodb   │
                                            └──────────────────┘
```

---

## 2. Mapa de Microservicios y Entidades Relacionales

| # | Microservicio | Puerto | Entidad Relacional | Base de Datos (Dev/MySQL) | Responsabilidad Principal |
|---|---|---|---|---|---|
| **1** | **`usuario-service`** | `8081` | `USUARIO` | `usuariodb` / `pchw_usuarios` | CRUD de usuarios, roles (ADMIN, VENDEDOR, CLIENTE), hash BCrypt, endpoints internos para gateway. |
| **2** | **`categoria-service`** | `8082` | `CATEGORIA` | `categoriadb` / `pchw_categorias` | Catálogo de categorías de hardware con iconos y estado activo. |
| **3** | **`producto-service`** | `8083` | `PRODUCTO` | `productodb` / `pchw_productos` | Catálogo de componentes, control de stock, precios, ofertas y vinculación lógica con categorías. |
| **4** | **`boleta-service`** | `8084` | `BOLETA` | `boletadb` / `pchw_boletas` | Cabeceras de compras/boletas, validación de stock vía REST (`producto-service`) y orquestación de items vía REST (`detalle-service`). |
| **5** | **`detalle-service`** | `8085` | `DETALLE_BOLETA` | `detalledb` / `pchw_detalles` | Líneas de detalle de boletas, cantidades, precios unitarios y subtotales por item. |
| **GW** | **`gateway-api`** | `8080` | *Orquestador* | Embebida / Memoria | Punto único de entrada para el Frontend, Login/Registro, validación de tokens JWT, RBAC y proxy transparente hacia los 5 microservicios. |

---

## 3. Despliegue con Docker y Docker Compose

Todos los microservicios cuentan con su propio `Dockerfile` multi-stage optimizado sobre Eclipse Temurin JDK 21.

### Levantar toda la arquitectura con un solo comando:
```bash
docker compose up -d --build
```

### Ver el estado de los contenedores:
```bash
docker compose ps
```

### Ver logs en tiempo real:
```bash
docker compose logs -f
```

### Detener los servicios:
```bash
docker compose down
```

---

## 4. Ejecución Nativa en Windows (PowerShell)

Para entornos sin Docker instalado, se incluyen scripts listos para ejecutar con Java 21:

### 1. Compilar los 6 microservicios:
```powershell
.\build-all.ps1
```

### 2. Iniciar todos los microservicios en paralelo:
```powershell
.\start-all.ps1
```

### 3. Detener todos los microservicios (libera puertos 8080-8085):
```powershell
.\stop-all.ps1
```

---

## 5. Compatibilidad Total con el Frontend

El Frontend React (`c:\panchicosas\pcHardware-FRONTEND\pcHardware-FRONTEND\`) se comunica directamente con `http://localhost:8080/api/v1/`:
- **Login / Registro:** `POST /api/v1/auth/login`, `POST /api/v1/auth/register`
- **Productos:** `GET /api/v1/productos`, `POST /api/v1/productos`, etc.
- **Categorías:** `GET /api/v1/categorias`, `POST /api/v1/categorias`, etc.
- **Usuarios:** `GET /api/v1/usuarios`, `POST /api/v1/usuarios`, etc.
- **Boletas:** `GET /api/v1/boletas`, `POST /api/v1/boletas`, `GET /api/v1/boletas/mis-boletas`

El `gateway-api` recibe cada petición, valida los permisos según el rol contenido en el JWT y la redirige al microservicio correspondiente, garantizando **cero cambios necesarios en el Frontend**.

---

## 6. Documentación Swagger / OpenAPI 3

Una vez iniciado el sistema, la documentación interactiva Swagger se encuentra disponible en:
- **API Gateway (Consolidado):** `http://localhost:8080/swagger-ui.html`
- **Usuario Service:** `http://localhost:8081/swagger-ui.html`
- **Categoria Service:** `http://localhost:8082/swagger-ui.html`
- **Producto Service:** `http://localhost:8083/swagger-ui.html`
- **Boleta Service:** `http://localhost:8084/swagger-ui.html`
- **Detalle Service:** `http://localhost:8085/swagger-ui.html`
