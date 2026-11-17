# Sonido Vivo · servicios

Microservicios del proyecto Sonido Vivo (DSY1104). Un proyecto Spring Boot por responsabilidad de
negocio, cada uno con su propia base de datos y sus migraciones Flyway. El front del sitio está en
otro repositorio (`sonido-vivo-2`).

## Servicios de negocio

| Servicio | Responsabilidad | Puerto | Base de datos |
|---|---|---|---|
| `catalogo` | Productos, categorías, stock y su historial de movimientos | 8091 | `catalogo` |
| `usuarios` | Registro, login, roles y estado de las cuentas | 8092 | `usuarios` |
| `boletas` | Boleta de venta: pedido, sus líneas y su estado | 8093 | `boletas` |
| `despachos` | Entrega del pedido: domicilio o retiro, costo y datos de envío | 8094 | `despachos` |
| `mensajeria` | Mensajes del formulario de contacto y su bandeja en el panel | 8095 | `mensajeria` |

La boleta y el despacho van separados: la boleta registra qué se vendió y por cuánto, y el despacho
se encarga de la entrega. `despacho.pedido_id` identifica la boleta pero no lleva clave foránea,
porque cada servicio tiene su propia base.

## Servicios de soporte

| Servicio | Responsabilidad | Puerto |
|---|---|---|
| `config` | Entrega a cada servicio su configuración desde `configurations/*.yml` | 8888 |
| `eureka` | Registro y descubrimiento: cada servicio se anuncia con su nombre | 8761 |
| `gateway` | Puerta de entrada única; reparte `/api/**` al servicio que corresponde | 8090 |

Los servicios de negocio van del **8091 al 8095** y el gateway en el 8090, para tener los puertos en
un mismo rango. El 8080 quedó descartado porque lo ocupa Docker en este equipo. `eureka` (8761) y
`config` (8888) mantienen su puerto por defecto.

## Orden de arranque

Los servicios de soporte van primero, porque los demás dependen de ellos:

```
config (8888)  →  eureka (8761)  →  catalogo / usuarios / boletas / despachos / mensajeria  →  gateway (8090)
```

Cada servicio declara `spring.config.import=optional:configserver:http://localhost:8888`, así que si el
servidor de configuración no está arriba igual arranca con lo que tiene en su propio
`application.properties`. Lo mismo con Eureka: sin registro, el servicio funciona, solo que el gateway
no lo encuentra.

Registro en ejecución: `http://localhost:8761` muestra qué servicios están anunciados. El gateway
resuelve cada ruta por nombre (`lb://<servicio>`), así que no hay host ni puerto escritos en el front:

| Ruta pública | Servicio |
|---|---|
| `/api/contacto/**` | `lb://mensajeria` |
| `/api/productos/**`, `/api/categorias/**`, `/api/movimientos/**` | `lb://catalogo` |
| `/api/usuarios/**` | `lb://usuarios` |
| `/api/pedidos/**`, `/api/boletas/**` | `lb://boletas` |
| `/api/despachos/**` | `lb://despachos` |

El balanceador del gateway cachea la lista de Eureka; con `spring.cloud.loadbalancer.cache.ttl=5s` un
servicio recién arrancado queda ruteado en segundos en vez de los 35 por defecto.

## Endpoints

### catálogo (8091)

| Método | Ruta | Uso |
|---|---|---|
| `GET` | `/api/productos` | Listar; filtros opcionales `?categoria=`, `?texto=`, `?criticos=true&minimo=3` |
| `GET` | `/api/productos/{id}` | Ver uno |
| `POST` | `/api/productos` | Crear (la carga inicial queda como movimiento `INICIAL`) |
| `PUT` | `/api/productos/{id}` | Editar; si cambia el stock, queda un movimiento `AJUSTE` |
| `PATCH` | `/api/productos/{id}/stock` | Fijar el stock en un valor exacto (también deja `AJUSTE`) |
| `DELETE` | `/api/productos/{id}` | Baja lógica (`activo = false`) |
| `GET` | `/api/categorias` | Listar categorías con su cantidad de productos |
| `GET/POST/PUT/DELETE` | `/api/categorias[/{id}]` | CRUD de categorías |
| `POST` | `/api/productos/{id}/movimientos` | Registrar `ENTRADA` o `SALIDA` del kardex |
| `GET` | `/api/productos/{id}/movimientos` | Historial de stock de un producto |
| `GET` | `/api/movimientos` | Historial completo; filtros `?productoId=`, `?tipo=`, `?motivo=`, `?texto=` |

### usuarios (8092)

| Método | Ruta | Uso |
|---|---|---|
| `GET` | `/api/usuarios` | Listar cuentas |
| `GET` | `/api/usuarios/{id}` | Ver una cuenta |
| `POST` | `/api/usuarios` | Registrar; el campo `rol` es opcional y sin él queda `Cliente` |
| `POST` | `/api/usuarios/login` | Validar correo y contraseña (`401` si no calzan, `403` si está desactivada) |
| `PATCH` | `/api/usuarios/{id}/rol` | Cambiar rol |
| `PATCH` | `/api/usuarios/{id}/activo` | Activar o desactivar |

La contraseña se guarda cifrada con BCrypt (`spring-security-crypto`) y nunca viaja en las respuestas.
El servicio impide dejar la tienda sin administrador activo: quitarle el rol o desactivar al último
responde `400` con el motivo.

### boletas (8093)

| Método | Ruta | Uso |
|---|---|---|
| `POST` | `/api/pedidos` | Emitir la boleta; el total y los subtotales los calcula el servicio |
| `GET` | `/api/pedidos` | Listar; filtros `?usuarioId=`, `?estado=` |
| `GET` | `/api/pedidos/{id}` | Ver la boleta con sus líneas |
| `PATCH` | `/api/pedidos/{id}/estado` | Cambiar estado; cancelar devuelve el stock al catálogo |

### despachos (8094)

| Método | Ruta | Uso |
|---|---|---|
| `POST` | `/api/despachos` | Registrar la entrega de un pedido (`400` si ya tiene una) |
| `GET` | `/api/despachos` | Listar; filtro `?tipoEntrega=DOMICILIO\|RETIRO` |
| `GET` | `/api/despachos/pedido/{pedidoId}` | Entrega de un pedido puntual |
| `GET` | `/api/despachos/{id}` | Ver una entrega |
| `PUT` | `/api/despachos/{id}` | Corregir dirección o teléfono antes de despachar |

Con `DOMICILIO` se exigen región, comuna y dirección; con `RETIRO` esas columnas quedan nulas y el
costo de envío se fuerza a 0, aunque el cliente mande otra cosa.

### mensajería (8095)

| Método | Ruta | Uso |
|---|---|---|
| `POST` | `/api/contacto` | Registrar un mensaje desde el formulario público |
| `GET` | `/api/contacto` | Listar mensajes, con filtro opcional `?estado=PENDIENTE` |
| `GET` | `/api/contacto/{id}` | Ver un mensaje completo |
| `PATCH` | `/api/contacto/{id}/estado` | Marcar como `RESPONDIDO` o `PENDIENTE` |
| `DELETE` | `/api/contacto/{id}` | Eliminar un mensaje desde la bandeja |

## Documentación interactiva (Swagger)

Los cinco servicios de negocio exponen su API con springdoc:

```
http://localhost:809X/swagger-ui/index.html      (8091 catálogo … 8095 mensajería)
http://localhost:809X/v3/api-docs                (el JSON de OpenAPI)
```

La versión es **springdoc 3.2.0**: es la línea compatible con Spring Boot 4 (la 2.x de los tutoriales
es para Boot 3 y no arranca acá). La versión se gestiona en el pom padre y cada servicio solo declara
el starter. El gateway queda fuera porque es reactivo y necesita la variante `-webflux-ui`.

## Kardex de stock

El stock no se pisa: cada cambio queda como movimiento en `catalogo.movimiento_stock`, con tipo
(`ENTRADA` / `SALIDA`), motivo (`VENTA`, `CANCELACION`, `AJUSTE`, `INICIAL`), cantidad, referencia y
el stock resultante.

- Una venta escribe una `SALIDA` con `PEDIDO-<id>` como referencia.
- Cancelar una boleta escribe una `ENTRADA` nueva con la misma referencia: no se edita la salida.
- La clave única `(producto_id, motivo, referencia)` hace idempotente la operación: repetir una
  cancelación no devuelve el stock dos veces.
- Una `CANCELACION` solo se acepta si existe la `VENTA` de esa referencia; si no, se omite en vez de
  inventar unidades.

## Preparar el entorno

Las bases **no hay que crearlas a mano**: cada servicio las crea al arrancar si no existen, porque su
URL de conexión lleva `createDatabaseIfNotExist=true` y MySQL las crea con el charset por defecto
(`utf8mb4` / `utf8mb4_0900_ai_ci`, que es justo el que se necesita). Flyway después crea las tablas
con sus migraciones. Así, al clonar el repo en otro PC, basta con tener MySQL arriba.

Si se prefiere crearlas antes (o el usuario de MySQL no tiene permiso para crear bases):

```sql
CREATE DATABASE catalogo   CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
CREATE DATABASE usuarios   CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
CREATE DATABASE boletas    CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
CREATE DATABASE despachos  CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
CREATE DATABASE mensajeria CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
```

Requisitos: JDK 21 (`java -version` debe responder 21) y Maven.

## Compilar y arrancar

El repositorio es un proyecto Maven multimódulo: el `pom.xml` de la raíz es el padre y define
Java 21, Spring Boot 4.0.9, Spring Cloud 2025.1.4 y las dependencias de soporte. Cada servicio
declara solo lo propio de su responsabilidad.

```
mvn clean package -DskipTests              # compila los ocho módulos
mvn -pl boletas -am package                # solo uno, con lo que necesite
java -jar boletas/target/boletas-0.0.1-SNAPSHOT.jar
```

Para agregar un servicio: crear la carpeta con su `pom.xml` (padre `com.sonidovivo:sonido-vivo-servicios`),
declararlo en `<modules>` del pom padre y sumar solo sus dependencias particulares.

Las tablas las crea Flyway al arrancar. No se crean a mano: el proyecto usa
`spring.jpa.hibernate.ddl-auto=validate`, así que si el esquema no calza con la entidad, no
arranca.

Si `mvn` falla con `release version 21 not supported`, Maven está usando otro JDK: revisar
`mvn -v` y corregir `JAVA_HOME`.

## Primer administrador

Si la base `usuarios` está vacía, el servicio crea una cuenta para poder entrar al panel:

```
correo:     admin@sonidovivo.cl
contraseña: admin123
```

La contraseña se puede cambiar con la propiedad `usuarios.admin.password` (por ejemplo en
`configurations/usuarios.yml`) y solo se usa la primera vez, cuando no hay ninguna cuenta.

## Migraciones

Cada servicio las lleva en `src/main/resources/db/migracion`, con el nombre
`V<n>__descripcion.sql`. Una migración ya aplicada no se edita: se corrige con una nueva.

| Servicio | Migraciones | Tablas |
|---|---|---|
| `catalogo` | V1 categoría, V2 producto, V3 categorías base, V4 productos base, V5 tipo de descuento, V6 movimiento de stock | `categoria`, `producto`, `movimiento_stock` |
| `usuarios` | V1 rol, V2 usuario, V3 roles base, V4 collation | `rol`, `usuario` |
| `boletas` | V1 pedido, V2 pedido_item | `pedido`, `pedido_item` |
| `despachos` | V1 despacho | `despacho` |
| `mensajeria` | V1 contacto, V2 estado, V3 región y comuna | `contacto` |

## Convenciones

- Maven multimódulo: el padre es `sonido-vivo-servicios`, que hereda de `spring-boot-starter-parent` 4.0.9.
- Spring Cloud **2025.1.4** para Eureka, Config y Gateway: es la línea compatible con Spring Boot 4.0
  (Spring Cloud 2023, la que usan los tutoriales, es para Boot 3).
- Springdoc **3.2.0** para la documentación de la API, por la misma razón.
- Una base por servicio y sin claves foráneas entre bases distintas.
- `db/migracion` (no `db/migration`) y `V1__` con doble guion bajo.
- Nombres de base sin sufijo `_db`, con `utf8mb4` y `utf8mb4_0900_ai_ci` en las cinco.
- Paquete `com.sonidovivo.<servicio>`.
- Contraseñas cifradas con BCrypt; nunca se devuelven en las respuestas.
- Servicios autocontenidos: la única llamada entre servicios es la devolución de stock de `boletas`
  a `catalogo`, hecha con `RestClient` y la dirección resuelta por Eureka (`DiscoveryClient`). No se
  usa Feign ni se comparten DTOs entre servicios.
