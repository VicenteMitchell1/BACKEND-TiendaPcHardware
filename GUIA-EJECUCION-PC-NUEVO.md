# Guía de Ejecución del Proyecto en un PC Nuevo
## PC Hardware Store - Control 3 Fullstack II (DSY1104)

Esta guía detalla los pasos para clonar, instalar y poner en marcha el proyecto completo (Frontend React + Arquitectura de 6 Microservicios Spring Boot) en cualquier computadora nueva desde cero.

---

## 🛠️ Paso 0: Requisitos Previos

En el equipo nuevo únicamente necesitas contar con el siguiente software base:

1. **Node.js (v18, v20 o superior):**
   - Descarga e instalación: [nodejs.org](https://nodejs.org/) (incluye `npm`).
2. **Java JDK 21:**
   - Descarga recomendada: Eclipse Temurin JDK 21 desde [adoptium.net](https://adoptium.net/).
   - *(Alternativa)*: **Docker Desktop** si deseas ejecutar la arquitectura contenerizada.

> [!NOTE]
> **No es necesario instalar Maven ni MySQL:**
> - Cada microservicio cuenta con su propio **Maven Wrapper** (`mvnw.cmd` / `mvnw`).
> - Con Docker (Opción A) se levanta automáticamente un contenedor **MySQL 8.4** con un esquema por microservicio; los datos persisten entre reinicios.
> - Sin Docker (Opción B) cada microservicio usa una base **H2 en memoria** (los datos se reinician al detenerlo).
> - En ambos casos los datos de prueba se siembran automáticamente sólo cuando la base está vacía.

> [!IMPORTANT]
> Si tienes más de un JDK instalado, `JAVA_HOME` debe apuntar al **JDK 21**: con JDK 22 o superior Lombok no compila con esta versión de Spring Boot.

---

## 📂 Paso 1: Estructura de Directorios

Coloca ambos proyectos en una carpeta de tu preferencia (por ejemplo `C:\proyectos\` o en el Escritorio):

```text
C:\proyectos\
├── pcHardware-FRONTEND\       (código frontend descomprimido de 07)
└── pcHardware-MICROSERVICES\  (código microservicios descomprimido de 08)
```

---

## 🚀 Paso 2: Ejecutar los Microservicios (Backend)

Tienes dos alternativas de despliegue según el entorno:

### Opción A: Con Docker (Recomendado si tienes Docker Desktop)

1. Abre una consola de **PowerShell** en `pcHardware-MICROSERVICES` y genera el archivo de secretos `.env` (sólo la primera vez):
   ```powershell
   cd C:\proyectos\pcHardware-MICROSERVICES
   .\setup-env.ps1
   ```
   *(Alternativa manual: copiar `.env.example` como `.env` y completar los valores. El archivo `.env` nunca se sube a GitHub.)*
2. Levanta la arquitectura:
   ```powershell
   docker compose up -d --build
   ```
3. Docker descargará las imágenes base, compilará y levantará 7 contenedores en la red interna `pchw-net`:
   - `mysql` (sólo accesible desde este PC en `127.0.0.1:3307`)
   - `gateway-api` (Puerto `8080`, **único puerto publicado**)
   - `usuario-service`, `categoria-service`, `producto-service`, `boleta-service`, `detalle-service` (puertos 8081-8085, sólo dentro de la red de Docker)

---

### Opción B: Con Java 21 y PowerShell (Sin Docker)

1. Abre una consola de **PowerShell** en `pcHardware-MICROSERVICES`.
2. *(Opcional)* Si `java` no está en tu variable de entorno PATH, indica la ruta de tu JDK:
   ```powershell
   $env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-21.0.x"
   ```
3. Compila los 6 microservicios (descargará dependencias, ejecutará las pruebas y empaquetará los `.jar`):
   ```powershell
   cd C:\proyectos\pcHardware-MICROSERVICES
   .\build-all.ps1            # agrega -SkipTests para compilar más rápido
   ```
4. Inicia todos los servicios en segundo plano (si no existe `.env`, se genera automáticamente; los logs quedan en `.\logs`):
   ```powershell
   .\start-all.ps1
   ```
5. **Verificación:** Abre en tu navegador [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html) para ver la documentación interactiva Swagger del API Gateway consolidado.

---

## 💻 Paso 3: Ejecutar el Frontend (React + Vite)

1. Abre una **nueva terminal** en la carpeta del Frontend:
   ```powershell
   cd C:\proyectos\pcHardware-FRONTEND
   ```
2. Instala las dependencias de Node.js (solo la primera vez):
   ```powershell
   npm install
   ```
3. Inicia el servidor de desarrollo Vite:
   ```powershell
   npm run dev
   ```
4. Abre la URL local en tu navegador:
   ```text
   http://localhost:5173
   ```

---

## 🔑 Paso 4: Credenciales de Prueba

En la pantalla de Login ([http://localhost:5173/login](http://localhost:5173/login)) puedes iniciar sesión con las siguientes cuentas precargadas en el semillero:

| Rol | Correo Electrónico | Contraseña | Capacidades y Permisos |
|---|---|---|---|
| **Administrador** | `admin@duoc.cl` | `admin` | Acceso completo: Dashboard, CRUD de Productos, Categorías, Control de Inventario y Administración de Usuarios. |
| **Vendedor** | `vendedor@duoc.cl` | `vendedor` | Acceso restringido: Consulta de catálogo en modo Solo Lectura y revisión de boletas emitidas. |
| **Cliente** | `alumno@duoc.cl` | `1234` | Catálogo público, carrito de compras, emisión de boletas y módulo "Mis Compras". |
| **Cliente 2** | `pancho@gmail.com` | `gamer` | Cuenta cliente con historial previo de órdenes de compra. |

---

## 🛑 Paso 5: Cómo Detener el Proyecto

- **Frontend:** Presiona `Ctrl + C` en la terminal donde corre Vite.
- **Microservicios:**
  - Si usaste Docker:
    ```bash
    docker compose down        # conserva los datos de MySQL
    docker compose down -v     # además borra la base de datos (vuelve a los datos semilla)
    ```
  - Si usaste PowerShell nativo:
    ```powershell
    .\stop-all.ps1
    ```
    *(Detiene sólo los procesos Java que usan los puertos 8080 al 8085).*

---

## 🔐 Nota sobre la sesión (JWT)

El login entrega un `token` (válido 1 hora) y un `refreshToken` (válido 7 días, de un solo uso). Cuando una petición responde **401**, el frontend debe llamar a `POST /api/v1/auth/refresh` con `{"refreshToken": "..."}` para obtener un par nuevo, o volver a pedir login. Al cerrar sesión debe llamar a `POST /api/v1/auth/logout`.
