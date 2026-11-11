-- Crea la tabla 
 
CREATE TABLE contacto (
    id BIGINT NOT NULL AUTO_INCREMENT,
    nombre_completo VARCHAR(150) NOT NULL,
    correo VARCHAR(150) NOT NULL,
    mensaje TEXT NOT NULL,
    fecha_creacion DATETIME(6) NOT NULL,
    PRIMARY KEY (id)
);