-- Microservicio despachos · base despachos
-- pedido_id identifica la boleta, pero no lleva clave foranea: la boleta vive en otro servicio.
-- tipo_entrega y costo_envio existen para la pantalla de opciones de entrega:
-- con RETIRO la direccion queda nula y el costo es cero.
CREATE TABLE despacho (
    id           BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    pedido_id    BIGINT UNSIGNED NOT NULL,
    tipo_entrega ENUM ('DOMICILIO', 'RETIRO') NOT NULL DEFAULT 'DOMICILIO',
    nombre       VARCHAR(120) NOT NULL,
    telefono     VARCHAR(20) NOT NULL,
    region       VARCHAR(80),
    comuna       VARCHAR(80),
    direccion    VARCHAR(200),
    referencia   VARCHAR(200),
    costo_envio  DECIMAL(10,2) NOT NULL DEFAULT 0,
    creado_en    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_despacho_pedido UNIQUE (pedido_id),
    CONSTRAINT ck_despacho_costo CHECK (costo_envio >= 0),
    CONSTRAINT ck_despacho_direccion CHECK (
        tipo_entrega = 'RETIRO'
        OR (region IS NOT NULL AND comuna IS NOT NULL AND direccion IS NOT NULL)
    )
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;
