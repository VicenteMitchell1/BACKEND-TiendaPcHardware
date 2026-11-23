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
> - Cada microservicio utiliza por defecto una base de datos embebida **H2 en memoria** con modo de compatibilidad MySQL y datos de prueba ya sembrados al iniciar.

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

1. Abre una terminal en `pcHardware-MICROSERVICES`:
   ```bash
   cd C:\proyectos\pcHardware-MICROSERVICES
   docker compose up -d --build
   ```
2. Docker descargará las imágenes base, compilará y levantará los 6 contenedores en la red interna `pchw-net`:
   - `gateway-api` (Puerto `8080`)
   - `usuario-service` (Puerto `8081`)
   - `categoria-service` (Puerto `8082`)
   - `producto-service` (Puerto `8083`)
   - `boleta-service` (Puerto `8084`)
   - `detalle-service` (Puerto `8085`)

---

### Opción B: Con Java 21 y PowerShell (Sin Docker)

1. Abre una consola de **PowerShell** en `pcHardware-MICROSERVICES`.
2. *(Opcional)* Si `java` no está en tu variable de entorno PATH, indica la ruta de tu JDK:
   ```powershell
   $env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-21.0.x"
   ```
3. Compila los 6 microservicios (descargará dependencias y empaquetará los `.jar`):
   ```powershell
   cd C:\proyectos\pcHardware-MICROSERVICES
   .\build-all.ps1
   ```
4. Inicia todos los servicios en segundo plano:
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
    docker compose down
    ```
  - Si usaste PowerShell nativo:
    ```powershell
    .\stop-all.ps1
    ```
    *(Este comando localiza y libera automáticamente los puertos 8080 al 8085).*
