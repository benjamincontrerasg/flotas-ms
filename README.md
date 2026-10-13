# flotas-ms

Microservicio de flota de RutaLimpia, un sistema de recoleccion de reciclaje
a domicilio.

## Estado actual

El proyecto compila y genera un JAR. Incluye Spring Boot, Maven Wrapper,
JPA, validacion, el driver de PostgreSQL y la configuracion del puerto 8083.
La conexion a PostgreSQL nativo usa flotas_db y configuracion local.
La entidad Camion y el enum EstadoCamion estan implementados. Hibernate
crea la tabla camiones al iniciar. CamionRepository usa JpaRepository para
guardar, listar, consultar y eliminar camiones. CamionRequest y CamionResponse
definen los datos de entrada y salida. POST /camiones ya permite crear camiones
y guardarlos en PostgreSQL. GET /camiones permite listar; los filtros por
estado y conductor tambien estan implementados. GET /camiones/{id} consulta
un camion por ID y PUT /camiones/{id} guarda sus cambios. DELETE esta pendiente.

## Alcance del CRUD

Este avance gestionara camiones: crear, listar, consultar por ID, actualizar
y eliminar. Tambien permitira consultar por estado y por conductor asignado.
Los datos se guardaran en PostgreSQL nativo, en `flotas_db`, tabla `camiones`.

Los campos y reglas siguientes son decisiones de este proyecto. Describen
lo que implementaremos en la API. POST, las consultas GET y PUT estan implementados; eliminacion y las
validaciones completas estan pendientes.

## Campos previstos del camion

| Campo | Tipo Java | Regla prevista |
| --- | --- | --- |
| id | Long | Generado por PostgreSQL; se conserva al actualizar. |
| patente | String | Obligatoria, unica y de hasta 10 caracteres. |
| capacidadKg | Double | Obligatoria y mayor que cero; expresada en kilogramos. |
| estado | EstadoCamion (enum) | DISPONIBLE, EN_RUTA o MANTENCION. |
| conductorId | Long | Opcional; si se indica, debe ser mayor que cero. |

CamionRequest recibe patente, capacidadKg, estado y conductorId. No incluye
un campo ID. Estado y conductorId pueden quedar null para que el service
aplique las reglas previstas en creacion y actualizacion.
CamionResponse incluye los cinco campos de la tabla anterior.
El service comprueba patente obligatoria y capacidad finita mayor que cero. Las validaciones completas de los DTO se agregaran en el paso 14.

## Estados previstos

- `DISPONIBLE`: registrado como disponible para asignacion.
- `EN_RUTA`: registrado como realizando una ruta de recoleccion.
- `MANTENCION`: registrado como en mantenimiento.

Al crear, el estado sera DISPONIBLE si se omite o se envia null.
Al actualizar, omitir el estado o enviarlo null conservara el estado anterior.
Un valor diferente de los tres definidos devolvera 400.

En este CRUD el estado se gestionara desde Flota. La sincronizacion del estado
con las operaciones de Rutas queda para una etapa posterior.

## Reglas previstas

- POST y PUT requeriran patente y capacidadKg.
- La patente se guardara sin espacios exteriores y en mayusculas.
- Los duplicados se comprobaran ignorando mayusculas y minusculas; la tabla
  tendra una restriccion UNIQUE sobre la patente normalizada.
- PUT conservara el ID y permitira mantener la patente del propio camion.
- Omitir conductorId o enviarlo null en PUT quitara la asignacion anterior.
- Un ID inexistente devolvera 404; una patente duplicada devolvera 409.
- Los datos invalidos devolveran 400 y los errores tendran formato uniforme.
- Los listados por estado o conductor sin coincidencias devolveran `[]`.

No se exigira un patron de patente de un pais especifico en este avance.
La comprobacion de existencia y rol del conductor en Usuarios queda pendiente
para la coordinacion entre servicios.

## Endpoints previstos

Base local: `http://localhost:8083`. POST /camiones y los GET de listado, estado
y conductor, junto con GET y PUT /camiones/{id}, estan implementados. DELETE
se agregara en el paso siguiente.

| Metodo | Ruta | Funcion | Respuesta correcta |
| --- | --- | --- | --- |
| POST | `/camiones` | Crear un camion. | 201 Created |
| GET | `/camiones` | Listar camiones. | 200 OK |
| GET | `/camiones/{id}` | Consultar por ID. | 200 OK |
| PUT | `/camiones/{id}` | Actualizar un camion. | 200 OK |
| DELETE | `/camiones/{id}` | Eliminar un camion. | 204 No Content |
| GET | `/camiones/estado/{estado}` | Consultar por estado. | 200 OK |
| GET | `/camiones/conductor/{conductorId}` | Consultar por conductor. | 200 OK |

Ejemplo del JSON para POST /camiones, con estado inicial DISPONIBLE y sin
conductor asignado:

```json
{
  "patente": "ABCD12",
  "capacidadKg": 3500.0,
  "conductorId": null
}
```

POST devuelve 201 Created con id, patente, capacidadKg, estado y conductorId.
La patente se guarda sin espacios exteriores y en mayusculas. Si se omite
el estado o se envia null, se conserva el valor inicial DISPONIBLE.
Patente vacia o capacidad ausente, no finita o menor o igual a cero devuelven 400.
Las longitudes, conductorId, duplicados y errores uniformes se completaran
en los pasos de validacion y manejo de errores.

## Listar y filtrar camiones

Con el servicio iniciado, ejecutar desde otra terminal:

```powershell
Invoke-RestMethod -Uri 'http://localhost:8083/camiones' -Method Get
Invoke-RestMethod -Uri 'http://localhost:8083/camiones/estado/DISPONIBLE' -Method Get
Invoke-RestMethod -Uri 'http://localhost:8083/camiones/conductor/1' -Method Get
```

Los tres endpoints devuelven 200 OK y una lista de CamionResponse con id,
patente, capacidadKg, estado y conductorId. Si no hay coincidencias, devuelven
`[]`. El listado general incluye tambien camiones sin conductor asignado.
El orden de las listas no esta definido.

El filtro de estado recibe DISPONIBLE, EN_RUTA o MANTENCION en mayusculas.
Un estado desconocido devuelve 400. El ID del conductor debe ser un Long
positivo: texto, cero o valores negativos devuelven 400. Un ID positivo sin
camiones asignados devuelve una lista vacia.

El filtro consulta el conductorId guardado en Flota. La comprobacion del
usuario y su rol en el servicio Usuarios sigue pendiente.

## Consultar un camion por ID

Enviar GET a `http://localhost:8083/camiones/{id}`, reemplazando `{id}` por
el ID generado al crear un camion. Devuelve 200 OK con id, patente,
capacidadKg, estado y conductorId del registro guardado en PostgreSQL.

Un ID numerico inexistente devuelve 404 Not Found. Un ID con texto o fuera
del rango de Long devuelve 400 Bad Request. Esta consulta no modifica datos.

## Actualizar un camion

Enviar PUT a `http://localhost:8083/camiones/{id}` con el ID real del camion,
`Content-Type: application/json` y el body:

```json
{
  "patente": "ABCD12",
  "capacidadKg": 4000.0,
  "estado": "MANTENCION",
  "conductorId": null
}
```

Devuelve 200 OK con los datos actualizados. Conserva el ID, normaliza la
patente y guarda los cambios en PostgreSQL. Patente y capacidadKg son
obligatorias. Si se omite estado o se envia null, conserva el estado anterior.
Si se omite conductorId o se envia null, quita la asignacion anterior.

Un ID inexistente devuelve 404 y no crea un camion nuevo. Patente vacia,
capacidad ausente, no finita o menor o igual a cero devuelven 400. Un estado
desconocido, JSON incorrecto o ID no numerico tambien devuelve 400.
Las validaciones completas y el error 409 por duplicados siguen pendientes.

## Relacion con los otros microservicios

Rutas guarda `camionId` como Long. Por eso el ID de Camion tambien sera Long.
`conductorId` referenciara el ID de un usuario del microservicio Usuarios.

Flota mantendra su propia base de datos. Las referencias a otros servicios
se guardaran como IDs; las relaciones JPA se limitaran a entidades de esta
misma base. Las llamadas HTTP entre servicios quedan para una etapa posterior.

Eliminar un camion en Flota no eliminara ni comprobara sus rutas. Antes de
borrar camiones usados en pruebas compartidas, revisar sus referencias con Rutas.

## Organizacion prevista

- `controller`: endpoints REST de camiones.
- `service`: operaciones y reglas de flota.
- `repository`: CamionRepository proporciona acceso a PostgreSQL con JpaRepository.
- `model`: entidad Camion y enum EstadoCamion.
- `dto`: CamionRequest para entrada y CamionResponse para respuesta. El DTO de errores se agregara mas adelante.
- `exception`: manejo centralizado de errores.

## Tecnologias

- Java 21.
- Spring Boot 4.0.5.
- Spring Data JPA y Jakarta Validation.
- Driver de PostgreSQL para la persistencia.
- Maven mediante el Wrapper incluido.

## Requisitos

Instalar un JDK 21 y configurar JAVA_HOME con su carpeta de instalacion.
Ejemplo para esta instalacion de Windows:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-21.0.10'
.\mvnw.cmd -version
```

La salida de Maven debe indicar Java 21. La primera compilacion necesita
internet para descargar las dependencias.

## Preparar PostgreSQL nativo

Con PostgreSQL instalado directamente y en ejecucion, comprobar la conexion:

```powershell
pg_isready -h localhost -p 5432
```

Crear la base una sola vez, desde la raiz del proyecto:

```powershell
psql -h localhost -U postgres -d postgres -f sql/crear_base.sql
```

Si `flotas_db` ya existe, conservarla y omitir el script de creacion.
Copiar el ejemplo solo si aun no existe la configuracion local:

```powershell
if (-not (Test-Path application-local.properties)) {
    Copy-Item application-local.properties.example application-local.properties
}
```

Editar `application-local.properties` con las credenciales propias:

```properties
DB_URL=jdbc:postgresql://localhost:5432/flotas_db
DB_USERNAME=postgres
DB_PASSWORD=TU_CONTRASENA
```

Este archivo esta ignorado por Git. Tambien se pueden definir DB_URL,
DB_USERNAME y DB_PASSWORD como variables de entorno. Ejecutar Spring Boot
desde la raiz del proyecto para que encuentre el archivo local.

Para consultar la base desde otra terminal:

```powershell
psql -h localhost -U postgres -d flotas_db
```

Dentro de psql, `SELECT current_database();` debe devolver `flotas_db`.
Usar `\dt` para listar tablas y `\q` para salir. Hibernate crea la tabla
`camiones` a partir de la entidad implementada al iniciar la aplicacion.
Dentro de psql, `\d camiones` muestra sus columnas y restricciones.

## Compilar y ejecutar

Abrir PowerShell en la raiz del proyecto y compilar:

```powershell
.\mvnw.cmd clean package
```

Maven genera `target/flotas-ms-0.0.1-SNAPSHOT.jar`.
Para iniciar durante el desarrollo:

```powershell
.\mvnw.cmd spring-boot:run
```

Como alternativa, ejecutar el JAR:

```powershell
& "$env:JAVA_HOME\bin\java.exe" -jar target/flotas-ms-0.0.1-SNAPSHOT.jar
```

La aplicacion usa `http://localhost:8083`. Iniciar una sola instancia a la
vez y detenerla con Ctrl+C. POST, todas las consultas GET y PUT estan disponibles; DELETE esta pendiente.

## Archivos de desarrollo

- `.gitignore` excluye archivos generados, configuraciones locales de IDE
  y credenciales de los commits.
- `.gitattributes` mantiene LF en los archivos de texto y CRLF en `mvnw.cmd`.
- `.mvn`, `mvnw` y `mvnw.cmd` permiten usar Maven sin instalarlo por separado.

## Proximos pasos

Implementar eliminacion; despues, validaciones completas, control de
patentes duplicadas y manejo de errores.
Despues se agregaran pruebas automatizadas y una coleccion Postman.
