-- Microservicio catalogo · base catalogo
-- Kardex: el stock deja de ser un numero que se pisa y pasa a ser la suma de sus movimientos.
-- Una venta es una SALIDA; una cancelacion, una ENTRADA nueva. Nunca se edita el movimiento viejo.
CREATE TABLE movimiento_stock (
    id                BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    producto_id       BIGINT UNSIGNED NOT NULL,
    tipo              ENUM('ENTRADA','SALIDA') NOT NULL,
    cantidad          INT NOT NULL,
    motivo            VARCHAR(40) NOT NULL,
    referencia        VARCHAR(60) NULL,
    stock_resultante  INT NOT NULL,
    creado_en         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_movimiento_producto FOREIGN KEY (producto_id) REFERENCES producto(id),
    -- Idempotencia: la misma venta o la misma cancelacion no se aplican dos veces.
    -- Referencia nula (ajuste manual) se repite sin problema porque MySQL admite varios NULL.
    CONSTRAINT uq_movimiento UNIQUE (producto_id, motivo, referencia),
    CONSTRAINT ck_movimiento_cantidad CHECK (cantidad > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
