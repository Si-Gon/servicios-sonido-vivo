-- Microservicio pedidos · base pedidos
-- usuario_id es solo el identificador: no lleva clave foranea porque usuarios vive en otro servicio.
CREATE TABLE pedido (
    id             BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    usuario_id     BIGINT UNSIGNED NOT NULL,
    nombre_cliente VARCHAR(120) NOT NULL,
    email_cliente  VARCHAR(150) NOT NULL,
    total          DECIMAL(12,2) NOT NULL,
    estado         ENUM ('PENDIENTE', 'EN_PREPARACION', 'DESPACHADO', 'ENTREGADO', 'CANCELADO')
                   NOT NULL DEFAULT 'PENDIENTE',
    fecha_creacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_pedido_total CHECK (total >= 0),
    INDEX idx_pedido_usuario (usuario_id),
    INDEX idx_pedido_estado (estado)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;
