# flotas-ms

Microservicio de camiones de RutaLimpia, un sistema de recoleccion de
reciclaje a domicilio. Este repositorio contiene un avance del proyecto.

Incluye CRUD, filtros por estado y conductor, validaciones, control de
patentes duplicadas y respuestas de error uniformes. Guarda los camiones
en PostgreSQL nativo, en `flotas_db`, y atiende en `http://localhost:8083`.

Para esta instalacion de Windows, seguir la
[guia de ejecucion local](docs/ejecucion-local.md). Para probar la API,
usar la [coleccion Postman](postman/README.md).

## Requisitos

- JDK 21 y `JAVA_HOME` apuntando a su carpeta de instalacion.
- PostgreSQL instalado y escuchando en `localhost:5432`.
- Git para clonar el repositorio.
- Postman para ejecutar la coleccion desde su interfaz.

Spring Boot usa la version 4.0.5. Maven se ejecuta con el Wrapper incluido;
no hace falta instalar Maven por separado. La primera compilacion necesita
internet para descargar las dependencias.

## Obtener el proyecto

```powershell
git clone https://github.com/benjamincontrerasg/flotas-ms.git
cd flotas-ms
.\mvnw.cmd -version
```

La salida debe indicar Java 21. Si el repositorio ya esta clonado, abrir
una terminal en su carpeta y continuar con la configuracion.

## Preparar la base de desarrollo

Los comandos `psql` y `pg_isready` deben estar disponibles en PATH.
Con PostgreSQL iniciado, comprobar la conexion y crear la base una sola vez:

```powershell
pg_isready -h localhost -p 5432
psql -h localhost -U postgres -d postgres -f sql/crear_base.sql
```

Si `flotas_db` ya existe, omitir el script de creacion. Copiar el ejemplo
solo si aun no existe el archivo local:

```powershell
if (-not (Test-Path application-local.properties)) {
    Copy-Item application-local.properties.example application-local.properties
}
```

Editar sus credenciales:

```properties
DB_URL=jdbc:postgresql://localhost:5432/flotas_db
DB_USERNAME=postgres
DB_PASSWORD=TU_CONTRASENA
```

`application-local.properties` esta ignorado por Git. Tambien se pueden
definir estas variables en el entorno. Ejecutar los comandos de Spring Boot
desde la raiz del repositorio para que encuentre el archivo local.
Hibernate crea o actualiza la tabla `camiones` al iniciar la aplicacion.

## Ejecutar durante el desarrollo

```powershell
.\mvnw.cmd spring-boot:run
```

Esperar el mensaje de arranque y, desde otra terminal, comprobar la API:

```powershell
Invoke-RestMethod -Uri 'http://localhost:8083/camiones' -Method Get
```

Devuelve una lista; `[]` es correcto cuando no hay camiones. Iniciar una
sola instancia y detenerla con Ctrl+C. La ejecucion local usa PostgreSQL
nativo. Las pruebas de integracion requieren una base adicional.

## Preparar y ejecutar las pruebas

Crear la base y su usuario una sola vez, como postgres:

```powershell
psql -h localhost -U postgres -d postgres -f sql/crear_base_pruebas.sql
psql -h localhost -U postgres -d postgres -c '\password flotas_test_user'
```

El segundo comando pide la contrasena del usuario de pruebas. Agregarla
al mismo `application-local.properties`:

```properties
TEST_DB_USERNAME=flotas_test_user
TEST_DB_PASSWORD=TU_CONTRASENA_DE_PRUEBAS
```

Si ya existen la base y el usuario, conservar las credenciales actuales y
omitir su creacion. Con PostgreSQL iniciado, ejecutar:

```powershell
.\mvnw.cmd test
```

Hay 27 pruebas unitarias y 25 de integracion. Las unitarias comprueban el
service con Mockito y las restricciones de CamionRequest con Jakarta
Validation. Las de integracion usan MockMvc, JPA y PostgreSQL reales, sin
abrir el puerto 8083. Comprueban CRUD, filtros, persistencia, validaciones,
duplicados, errores HTTP y la restriccion UNIQUE.

El perfil `integration` fija la conexion a `flotas_test_db` con
`flotas_test_user`. Antes y despues de cada prueba verifica la base y el
usuario, y limpia sus registros. Usar esa base exclusivamente para pruebas.

Para ejecutar solo las unitarias, sin PostgreSQL:

```powershell
.\mvnw.cmd "-Dtest=CamionServiceTest,CamionRequestTest" test
```

Para ejecutar solo las de integracion:

```powershell
.\mvnw.cmd "-Dtest=CamionIntegrationTest" test
```

## Empaquetar y ejecutar el JAR

El empaquetado ejecuta las 52 pruebas; preparar sus credenciales y mantener
PostgreSQL iniciado:

```powershell
.\mvnw.cmd clean package
```

Detener cualquier instancia anterior y ejecutar desde la raiz del repo:

```powershell
& "$env:JAVA_HOME\bin\java.exe" -jar target/flotas-ms-0.0.1-SNAPSHOT.jar
```

El JAR usa la misma configuracion local y el puerto 8083. Detenerlo con
Ctrl+C. Los reportes de las pruebas quedan en `target/surefire-reports`.

## API

Base local: `http://localhost:8083`.

| Metodo | Ruta | Funcion | Respuesta correcta |
| --- | --- | --- | --- |
| POST | `/camiones` | Crear un camion. | 201 |
| GET | `/camiones` | Listar camiones. | 200 |
| GET | `/camiones/{id}` | Consultar por ID. | 200 |
| PUT | `/camiones/{id}` | Actualizar un camion. | 200 |
| DELETE | `/camiones/{id}` | Eliminar un camion. | 204, cuerpo vacio |
| GET | `/camiones/estado/{estado}` | Filtrar por estado. | 200 |
| GET | `/camiones/conductor/{conductorId}` | Filtrar por conductor. | 200 |

POST y PUT reciben `Content-Type: application/json`. Ejemplo para crear:

```json
{
  "patente": "ABCD12",
  "capacidadKg": 3500.0,
  "estado": "DISPONIBLE",
  "conductorId": null
}
```

| Campo | Regla |
| --- | --- |
| id | Long generado por PostgreSQL; no forma parte del request. |
| patente | Obligatoria, unica y de hasta 10 caracteres despues de normalizar. |
| capacidadKg | Numero obligatorio, finito y mayor que cero, en kilogramos. |
| estado | DISPONIBLE, EN_RUTA o MANTENCION. |
| conductorId | Long opcional; si se indica, debe ser positivo. |

La patente se guarda sin espacios exteriores y en mayusculas; los
duplicados se comparan ignorando mayusculas y minusculas. PostgreSQL
mantiene una restriccion UNIQUE. No se exige un patron de patente de un
pais especifico. Estos campos y reglas son decisiones de este proyecto.

Al crear, omitir estado o enviarlo null asigna DISPONIBLE. Al actualizar,
omitirlo o enviarlo null conserva el estado anterior. PUT exige patente y
capacidadKg, conserva el ID y permite mantener la patente del propio camion.
Omitir conductorId o enviarlo null en PUT quita la asignacion anterior.
Los listados sin coincidencias devuelven `[]`; su orden no esta definido.

## Errores

Las respuestas de error usan este formato:

```json
{
  "estado": 409,
  "mensaje": "La patente ya esta registrada",
  "detalles": []
}
```

| Codigo | Motivo |
| --- | --- |
| 400 | Datos invalidos, JSON incorrecto o parametros invalidos. |
| 404 | Camion o recurso inexistente. |
| 409 | Patente duplicada al crear o actualizar. |
| 405 | Metodo no permitido; conserva el encabezado Allow. |
| 415 | Tipo de contenido incompatible. |
| 500 | Error inesperado; detalles tecnicos en los logs. |

Los errores de validacion incluyen mensajes por campo en `detalles`, por
ejemplo `patente: La patente es obligatoria`. Su orden no esta definido.
Los otros casos usan una lista vacia. Un PUT rechazado conserva los datos
anteriores; actualizar un ID inexistente no crea un camion.

## Postman

Importar [Flota.postman_collection.json](postman/Flota.postman_collection.json)
y seguir la [guia de la coleccion](postman/README.md). Las 40 peticiones
deben ejecutarse en orden: crean dos camiones, prueban el CRUD, filtros y
errores, y eliminan sus registros. Guardan los IDs automaticamente y no
suponen que la base este vacia.

La coleccion se verifico con Newman y PostgreSQL de pruebas: 40 peticiones
y 147 comprobaciones aprobadas.

## Organizacion

- `controller`: endpoints REST.
- `service`: operaciones y reglas de camiones.
- `repository`: acceso a PostgreSQL con JpaRepository.
- `model`: Camion y EstadoCamion.
- `dto`: CamionRequest, CamionResponse y RespuestaError.
- `exception`: manejo centralizado de errores.
- `sql`: creacion de las bases de desarrollo y pruebas.
- `postman`: coleccion y guia de uso.

## Coordinacion pendiente

Rutas guarda `camionId` como Long y Flota guarda `conductorId` como referencia
a Usuarios. Cada microservicio tiene su propia base. Flota no comprueba
todavia la existencia o el rol del conductor en Usuarios ni sincroniza el
estado del camion con Rutas; las llamadas HTTP quedan para una etapa posterior.

Eliminar un camion no consulta ni elimina sus rutas. Revisar las referencias
con Rutas antes de borrar camiones usados en pruebas compartidas.
