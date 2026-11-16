-- El descuento es un porcentaje de 0 a 100: no necesita el rango del SMALLINT sin signo y
-- asi el tipo coincide con el Integer de la entidad.
ALTER TABLE producto MODIFY descuento INT NULL;
