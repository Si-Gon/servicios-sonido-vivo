-- Agrega la region y la comuna que pide el formulario de contacto.
-- Van despues de correo y son opcionales: un mensaje sin ubicacion tambien es valido.
ALTER TABLE contacto
    ADD COLUMN region VARCHAR(80) NULL AFTER correo,
    ADD COLUMN comuna VARCHAR(80) NULL AFTER region;
