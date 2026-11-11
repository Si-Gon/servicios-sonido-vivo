# Sonido Vivo · servicios

Microservicios del proyecto Sonido Vivo (DSY1104). Un proyecto Spring Boot por responsabilidad
de negocio, cada uno con su propia base de datos y sus migraciones Flyway. El front del sitio
está en otro repositorio (`sonido-vivo-2`).

## Servicios

| Servicio | Responsabilidad | Puerto | Base de datos | Estado |
|---|---|---|---|---|
| `mensajeria` | Mensajes del formulario de contacto y su bandeja en el panel | 8082 | `mensajeria` | funcionando |
| `catalogo` | Productos, categorías y stock | 8081 | `catalogo` | esqueleto con migraciones aplicadas |
| `usuarios` | Registro, roles y estado de las cuentas | 8083 | `usuarios` | esqueleto con migraciones aplicadas |
| `pedidos` | Pedidos, líneas, despacho y boleta | 8084 | `pedidos` | esqueleto con migraciones aplicadas |

Los tres esqueletos arrancan, Flyway crea sus tablas y el proyecto compila, pero todavía no
tienen entidad, repositorio ni controlador: eso lo escribe quien tome cada servicio, siguiendo
el de `mensajeria` como modelo.

## Preparar el entorno

Crear las cuatro bases (una por servicio, sin sufijo `_db`):

```sql
CREATE DATABASE mensajeria CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
CREATE DATABASE catalogo   CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
CREATE DATABASE usuarios   CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
CREATE DATABASE pedidos    CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
```

Requisitos: JDK 21 (`java -version` debe responder 21) y Maven.

## Compilar y arrancar

```
cd mensajeria          # o catalogo, usuarios, pedidos
mvn clean package -DskipTests
mvn spring-boot:run    # o: java -jar target/mensajeria-0.0.1-SNAPSHOT.jar
```

Las tablas las crea Flyway al arrancar. No se crean a mano: el proyecto usa
`spring.jpa.hibernate.ddl-auto=validate`, así que si el esquema no calza con la entidad, no
arranca.

Si `mvn` falla con `release version 21 not supported`, Maven está usando otro JDK: revisar
`mvn -v` y corregir `JAVA_HOME`.

## Endpoints de mensajería

| Método | Ruta | Uso |
|---|---|---|
| `POST` | `/api/contacto` | Registrar un mensaje desde el formulario público |
| `GET` | `/api/contacto` | Listar mensajes, con filtro opcional `?estado=PENDIENTE` |
| `GET` | `/api/contacto/{id}` | Ver un mensaje completo |
| `PATCH` | `/api/contacto/{id}/estado` | Marcar como `RESPONDIDO` o `PENDIENTE` |
| `DELETE` | `/api/contacto/{id}` | Eliminar un mensaje desde la bandeja |

## Migraciones

Cada servicio las lleva en `src/main/resources/db/migracion`, con el nombre
`V<n>__descripcion.sql`. Una migración ya aplicada no se edita: se corrige con una nueva.

| Servicio | Migraciones | Tablas |
|---|---|---|
| `mensajeria` | V1 contacto, V2 estado, V3 región y comuna | `contacto` |
| `catalogo` | V1 categoría, V2 producto, V3 categorías base, V4 productos | `categoria`, `producto` |
| `usuarios` | V1 rol, V2 usuario, V3 roles base | `rol`, `usuario` |
| `pedidos` | V1 pedido, V2 pedido_item, V3 despacho | `pedido`, `pedido_item`, `despacho` |

## Convenciones

- Java 21, Spring Boot 4.0.9 (padre `spring-boot-starter-parent`), Maven.
- Una base por servicio y sin claves foráneas entre bases distintas.
- `db/migracion` (no `db/migration`) y `V1__` con doble guion bajo.
- Nombres de base sin sufijo `_db`.
- Paquete `com.sonidovivo.<servicio>`.
