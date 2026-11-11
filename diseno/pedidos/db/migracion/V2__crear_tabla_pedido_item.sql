-- Microservicio pedidos · base pedidos
-- nombre_producto y precio_unitario quedan copiados a proposito: el pedido no debe cambiar
-- si despues se edita el precio en el catalogo.
CREATE TABLE pedido_item (
    id               BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    pedido_id        BIGINT UNSIGNED NOT NULL,
    producto_id      BIGINT UNSIGNED NOT NULL,
    nombre_producto  VARCHAR(150) NOT NULL,
    precio_unitario  DECIMAL(12,2) NOT NULL,
    cantidad         INT NOT NULL,
    subtotal         DECIMAL(12,2) NOT NULL,
    CONSTRAINT fk_item_pedido FOREIGN KEY (pedido_id) REFERENCES pedido (id) ON DELETE CASCADE,
    CONSTRAINT ck_item_cantidad CHECK (cantidad > 0),
    CONSTRAINT ck_item_precio CHECK (precio_unitario >= 0),
    CONSTRAINT ck_item_subtotal CHECK (subtotal >= 0),
    INDEX idx_item_pedido (pedido_id),
    INDEX idx_item_producto (producto_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;
