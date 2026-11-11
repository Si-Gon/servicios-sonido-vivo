# Sonido Vivo · servicios

Microservicios del proyecto Sonido Vivo (DSY1104). Aquí vive el backend: un proyecto Spring
Boot por responsabilidad de negocio, cada uno con su propia base de datos y sus migraciones
Flyway. El front del sitio está en otro repositorio (`sonido-vivo-2`).

## Servicios

| Servicio | Responsabilidad | Puerto | Base de datos | Estado |
|---|---|---|---|---|
| `mensajeria` | Mensajes del formulario de contacto y su bandeja en el panel | 8082 | `mensajeria` | funcionando |
| `catalogo` | Productos, categorías y stock | 8081 | `catalogo` | por crear |
| `usuarios` | Registro, roles y estado de las cuentas | 8083 | `usuarios` | por crear |
| `pedidos` | Pedidos, líneas, despacho y boleta | 8084 | `pedidos` | por crear |

## Arrancar mensajería

1. Levantar MySQL (Laragon) y crear la base `mensajeria` con `utf8mb4`.
2. Ejecutar `compilar.bat`: fija `JAVA_HOME` en el JDK 21 (el del sistema es 17 y con 17 falla
   `release version 21 not supported`) y corre `mvn clean package -DskipTests`.
3. Ejecutar `arrancar.bat`: levanta el jar en el puerto 8082.

Las tablas las crea Flyway al arrancar. No se crean a mano, porque el proyecto usa
`spring.jpa.hibernate.ddl-auto=validate`.

## Endpoints de mensajería

| Método | Ruta | Uso |
|---|---|---|
| `POST` | `/api/contacto` | Registrar un mensaje desde el formulario público |
| `GET` | `/api/contacto` | Listar mensajes, con filtro opcional `?estado=PENDIENTE` |
| `GET` | `/api/contacto/{id}` | Ver un mensaje completo |
| `PATCH` | `/api/contacto/{id}/estado` | Marcar como `RESPONDIDO` o `PENDIENTE` |
| `DELETE` | `/api/contacto/{id}` | Eliminar un mensaje desde la bandeja |

## Migraciones

Cada servicio lleva sus migraciones en `src/main/resources/db/migracion`, con el nombre
`V<n>__descripcion.sql`. Una migración ya aplicada no se edita: se corrige con una nueva.

En `diseno/` están las migraciones escritas para los servicios que faltan, con la estructura
de carpetas que espera Spring, para copiarlas cuando se cree cada proyecto.

Convenciones: Java 21, Spring Boot 4.0.9, Maven, `db/migracion` (no `db/migration`), `V1__`
con doble guion bajo, nombres de base sin sufijo `_db` y una base por servicio, sin claves
foráneas entre bases distintas.
