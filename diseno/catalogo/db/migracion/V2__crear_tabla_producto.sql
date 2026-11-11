-- Microservicio catalogo · base catalogo
-- Va despues de V1 porque la clave foranea necesita la tabla categoria.
CREATE TABLE producto (
    id             BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    codigo         VARCHAR(20) NOT NULL,
    categoria_id   BIGINT UNSIGNED NOT NULL,
    nombre         VARCHAR(150) NOT NULL,
    marca          VARCHAR(60) NOT NULL,
    modelo         VARCHAR(60) NOT NULL,
    precio         DECIMAL(12,2) NOT NULL,
    descuento      TINYINT UNSIGNED NULL,
    stock          INT NOT NULL DEFAULT 0,
    descripcion    TEXT,
    imagen         VARCHAR(160),
    activo         BOOLEAN NOT NULL DEFAULT TRUE,
    creado_en      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actualizado_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uq_producto_codigo UNIQUE (codigo),
    CONSTRAINT fk_producto_categoria FOREIGN KEY (categoria_id) REFERENCES categoria (id),
    CONSTRAINT ck_producto_precio CHECK (precio >= 0),
    CONSTRAINT ck_producto_descuento CHECK (descuento IS NULL OR descuento BETWEEN 0 AND 100),
    CONSTRAINT ck_producto_stock CHECK (stock >= 0),
    INDEX idx_producto_categoria (categoria_id),
    INDEX idx_producto_stock (stock)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;
