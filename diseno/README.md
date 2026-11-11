# Backend · microservicios y migraciones Flyway

Las migraciones estan listas para copiar dentro de cada proyecto Spring Boot, en la misma ruta y con el
mismo nombre que usa el proyecto de la clase (`mensajeria/` (este mismo repo)):

```
src/main/resources/db/migracion/V1__crear_tabla_x.sql
```

## Reglas que salen del proyecto de la clase

1. **La carpeta se llama `db/migracion`**, no `migration`, y hay que declararla:
   ```
   spring.flyway.enabled=true
   spring.flyway.locations=classpath:db/migracion
   ```
2. **Dos guiones bajos** en el nombre: `V1__crear_tabla_producto.sql`. Con uno solo no se aplica.
3. Las tablas las crea **Flyway**, no Hibernate: `spring.jpa.hibernate.ddl-auto=validate`.
4. Un cambio posterior **no se edita** en la migracion ya aplicada: se agrega `V2__...`. Asi lo hiciste con
   `V2__agregar_estado_contacto.sql`.
5. La base se llama igual que el servicio, **sin sufijo `_db`** (`mensajeria`, `catalogo`, `usuarios`, `pedidos`).

## Mapa de microservicios

| Microservicio | Base | Puerto | Tablas | Migraciones |
|---|---|---|---|---|
| `catalogo` | `catalogo` | 8081 | `categoria`, `producto` | V1 a V4 |
| `mensajeria` | `mensajeria` | **8082** | `contacto` | V1 y V2 (ya hechas en `mensajeria/` (este mismo repo)) |
| `usuarios` | `usuarios` | 8083 | `rol`, `usuario` | V1 a V3 |
| `pedidos` | `pedidos` | 8084 | `pedido`, `pedido_item`, `despacho` | V1 a V3 |

El puerto 8082 ya esta tomado por el proyecto de mensajeria, por eso los otros tres van en 8081, 8083 y
8084. Dos servicios no pueden compartir puerto.

## Crear las bases antes de levantar

```sql
CREATE DATABASE catalogo   CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE usuarios   CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE pedidos    CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
-- mensajeria ya existe: la usa el proyecto de la clase
```

**No crees las tablas a mano.** Si corres los `.sql` en la base del servicio, Flyway se niega a arrancar
porque encuentra un esquema con tablas y sin historial. Si ya lo hiciste, se arregla una vez con
`spring.flyway.baseline-on-migrate=true`, pero lo correcto es dejar que Flyway las cree.

## El pom, copiado del proyecto de la clase

Spring Boot **4.0.9**, Java **21**, y en Boot 4 los starters cambiaron de nombre:

```xml
<dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-webmvc</artifactId></dependency>
<dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-data-jpa</artifactId></dependency>
<dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-validation</artifactId></dependency>
<dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-flyway</artifactId></dependency>
<dependency><groupId>org.flywaydb</groupId><artifactId>flyway-mysql</artifactId><scope>runtime</scope></dependency>
<dependency><groupId>com.mysql</groupId><artifactId>mysql-connector-j</artifactId><scope>runtime</scope></dependency>
<dependency><groupId>org.projectlombok</groupId><artifactId>lombok</artifactId><optional>true</optional></dependency>
```

`spring-boot-starter-web` no existe en Boot 4: es `webmvc`. Y Lombok necesita el
`annotationProcessorPaths` en el `maven-compiler-plugin`, como ya esta en tu pom.

## aplicacion.properties de cada servicio

```properties
spring.application.name=catalogo
spring.datasource.url=jdbc:mysql://localhost:3306/catalogo
spring.datasource.username=root
spring.datasource.password=
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.show-sql=true
server.port=8081
spring.flyway.enabled=true
spring.flyway.locations=classpath:db/migracion
```

## Imagenes de los productos

La base **no guarda las imagenes**: guarda el nombre del archivo en `producto.imagen` (`ga001.jpg`) y los
archivos viven en el front, en `public/img/productos/` (51 archivos, 3,1 MB). El front arma la ruta con
`/img/productos/${imagen}` y respeta una URL completa si empieza con `http`.

## Catalogo cargado

`V3__insertar_categorias.sql` trae las 10 categorias y `V4__insertar_productos.sql` los **51 productos**
del front (el "340" del sitio es copy de la marca). La migracion conserva el id original de cada producto
para que el carrito guardado en el navegador siga apuntando al mismo registro.

## Compilar y arrancar el servicio de mensajeria

El proyecto ya esta en `mensajeria/` (este mismo repo). Trae dos archivos para no pelear con el entorno:

- `compilar.bat`: fija `JAVA_HOME` en el JDK 21 (el del sistema es 17, y con 17 falla
  `release version 21 not supported`) y corre `mvn clean package -DskipTests`.
- `arrancar.bat`: levanta el jar en el puerto 8082 contra la base `mensajeria` de Laragon.

El `mvnw` del proyecto no sirve en este PC porque no logra descargar Maven, por eso los `.bat` usan el
Maven instalado en `C:\WorkSpace\tools\apache-maven-3.9.9`.
