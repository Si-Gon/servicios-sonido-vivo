-- Microservicio catalogo · base catalogo
CREATE TABLE categoria (
    id        BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    nombre    VARCHAR(80) NOT NULL,
    creado_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_categoria_nombre UNIQUE (nombre)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;
