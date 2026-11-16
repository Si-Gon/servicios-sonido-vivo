# Sonido Vivo · servicios

Microservicios del proyecto Sonido Vivo (DSY1104). Un proyecto Spring Boot por responsabilidad
de negocio, cada uno con su propia base de datos y sus migraciones Flyway. El front del sitio
está en otro repositorio (`sonido-vivo-2`).

## Servicios

### Servicios de negocio

| Servicio | Responsabilidad | Puerto | Base de datos | Estado |
|---|---|---|---|---|
| `mensajeria` | Mensajes del formulario de contacto y su bandeja en el panel | 8082 | `mensajeria` | funcionando |
| `catalogo` | Productos, categorías y stock | 8081 | `catalogo` | esqueleto con migraciones aplicadas |
| `usuarios` | Registro, roles y estado de las cuentas | 8083 | `usuarios` | esqueleto con migraciones aplicadas |
| `boletas` | Boleta de venta: pedido y sus líneas | 8084 | `boletas` | esqueleto con migraciones aplicadas |
| `despachos` | Entrega del pedido: domicilio o retiro, costo y datos de envío | 8085 | `despachos` | esqueleto con migraciones aplicadas |

La boleta y el despacho van separados: la boleta registra qué se vendió y por cuánto, y el
despacho se encarga de la entrega. `despacho.pedido_id` identifica la boleta pero no lleva clave
foránea, porque cada servicio tiene su propia base.

### Servicios de soporte

| Servicio | Responsabilidad | Puerto |
|---|---|---|
| `config` | Entrega a cada servicio su configuración desde `configurations/*.yml` | 8888 |
| `eureka` | Registro y descubrimiento: cada servicio se anuncia con su nombre | 8761 |
| `gateway` | Puerta de entrada única; reparte `/api/**` al servicio que corresponde | 8090 |

El gateway usa el 8090 porque el 8080 lo ocupa Docker en este equipo.

Los tres esqueletos arrancan, Flyway crea sus tablas y el proyecto compila, pero todavía no
tienen entidad, repositorio ni controlador: eso lo escribe quien tome cada servicio, siguiendo
el de `mensajeria` como modelo.

## Orden de arranque

Los servicios de soporte van primero, porque los demás dependen de ellos:

```
config (8888)  →  eureka (8761)  →  catalogo / usuarios / boletas / despachos / mensajeria  →  gateway (8090)
```

Cada servicio declara `spring.config.import=optional:configserver:http://localhost:8888`, así que si el
servidor de configuración no está arriba igual arranca con lo que tiene en su propio
`application.properties`. Lo mismo con Eureka: sin registro, el servicio funciona, solo que el gateway
no lo encuentra.

Registro en ejecución: `http://localhost:8761` muestra qué servicios están anunciados.
Rutas del gateway, con el servicio de mensajería como ejemplo:

```
GET http://localhost:8090/api/contacto     ->  lb://mensajeria
GET http://localhost:8090/api/productos/** ->  lb://catalogo
GET http://localhost:8090/api/pedidos/**   ->  lb://boletas
GET http://localhost:8090/api/despachos/** ->  lb://despachos
```

## Preparar el entorno

Crear las cuatro bases (una por servicio, sin sufijo `_db`):

```sql
CREATE DATABASE mensajeria CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
CREATE DATABASE catalogo   CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
CREATE DATABASE usuarios   CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
CREATE DATABASE boletas    CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
CREATE DATABASE despachos  CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
```

Requisitos: JDK 21 (`java -version` debe responder 21) y Maven.

## Compilar y arrancar

El repositorio es un proyecto Maven multimódulo: el `pom.xml` de la raíz es el padre y define
Java 21, Spring Boot 4.0.9, Spring Cloud 2025.1.4 y las dependencias de soporte. Cada servicio
declara solo lo propio de su responsabilidad.

```
mvn clean package -DskipTests          # compila los siete módulos
mvn -pl mensajeria -am package         # solo uno, con lo que necesite
java -jar mensajeria/target/mensajeria-0.0.1-SNAPSHOT.jar
```

Para agregar un servicio: crear la carpeta con su `pom.xml` (padre `com.sonidovivo:sonido-vivo-servicios`),
declararlo en `<modules>` del pom padre y sumar solo sus dependencias particulares.

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
| `boletas` | V1 pedido, V2 pedido_item | `pedido`, `pedido_item` |
| `despachos` | V1 despacho | `despacho` |

## Convenciones

- Maven multimódulo: el padre es `sonido-vivo-servicios`, que hereda de `spring-boot-starter-parent` 4.0.9.
- Spring Cloud **2025.1.4** para Eureka, Config y Gateway: es la línea compatible con Spring Boot 4.0
  (Spring Cloud 2023, la que usan los tutoriales, es para Boot 3).
- Una base por servicio y sin claves foráneas entre bases distintas.
- `db/migracion` (no `db/migration`) y `V1__` con doble guion bajo.
- Nombres de base sin sufijo `_db`.
- Paquete `com.sonidovivo.<servicio>`.
