-- Microservicio usuarios · base usuarios
-- rut se guarda limpio (sin puntos, con guion) y password nunca en claro: va el hash.
CREATE TABLE usuario (
    id            BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    nombre        VARCHAR(120) NOT NULL,
    rut           VARCHAR(12) NOT NULL,
    email         VARCHAR(150) NOT NULL,
    region        VARCHAR(80),
    comuna        VARCHAR(80),
    password_hash VARCHAR(255) NOT NULL,
    rol_id        BIGINT UNSIGNED NOT NULL,
    activo        BOOLEAN NOT NULL DEFAULT TRUE,
    creado_en     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_usuario_rut UNIQUE (rut),
    CONSTRAINT uq_usuario_email UNIQUE (email),
    CONSTRAINT fk_usuario_rol FOREIGN KEY (rol_id) REFERENCES rol (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;
