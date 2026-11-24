-- Se ejecuta UNA sola vez, cuando el volumen de MySQL está vacío.
-- Patrón "una base de datos por microservicio": cada servicio sólo accede a su propio esquema.
CREATE DATABASE IF NOT EXISTS pchw_usuarios   CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS pchw_categorias CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS pchw_productos  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS pchw_boletas    CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS pchw_detalles   CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS pchw_gateway    CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- El usuario de aplicación 'pchw' (creado por la imagen con MYSQL_USER) sólo tiene permisos sobre los esquemas pchw_*.
GRANT ALL PRIVILEGES ON `pchw\_%`.* TO 'pchw'@'%';
FLUSH PRIVILEGES;
