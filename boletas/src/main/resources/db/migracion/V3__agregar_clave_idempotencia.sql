-- Microservicio boletas · base boletas
-- Clave de idempotencia del checkout: si el navegador reintenta la emision (se corto la red despues
-- de que el servidor la creo), la misma clave devuelve la boleta que ya existe en vez de duplicarla.
-- MySQL admite varios NULL en un UNIQUE, asi que las boletas sin clave no se estorban.
ALTER TABLE pedido
    ADD COLUMN clave_idempotencia VARCHAR(64) NULL AFTER estado,
    ADD CONSTRAINT uq_pedido_idempotencia UNIQUE (clave_idempotencia);
