-- Microservicio usuarios · base usuarios
-- Las dos primeras migraciones quedaron con utf8mb4_unicode_ci y el resto del proyecto usa
-- utf8mb4_0900_ai_ci. Se alinean para que una comparacion entre servicios no reviente.
ALTER TABLE rol CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
ALTER TABLE usuario CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
