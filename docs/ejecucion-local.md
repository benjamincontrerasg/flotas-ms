# Ejecutar Flota en esta instalacion de Windows

Esta guia usa el repositorio del escritorio, JDK 21.0.10 y PostgreSQL 18
instalados en este equipo. En otro equipo, adaptar las rutas y preparar las
bases y credenciales segun el README principal.

## 1. Abrir PowerShell y entrar al proyecto

```powershell
Set-Location "$env:USERPROFILE\Desktop\flota"
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-21.0.10'
$env:Path = "$env:USERPROFILE\Documents\PostgreSQL\18\bin;$env:JAVA_HOME\bin;$env:Path"
.\mvnw.cmd -version
```

Debe mostrar Java 21. Los cambios de estas variables duran en esta terminal.

## 2. Comprobar PostgreSQL

```powershell
pg_isready -h localhost -p 5432
```

Si indica `accepting connections`, continuar. Si PostgreSQL esta detenido,
iniciar el servidor existente:

```powershell
pg_ctl -D "$env:USERPROFILE\Documents\PostgreSQL\data18" -l "$env:USERPROFILE\Documents\PostgreSQL\server.log" -w start
```

El log se guarda fuera de la carpeta de datos. No volver a inicializar el
servidor ni crear las bases si ya existen.

## 3. Revisar la configuracion local

`application-local.properties` contiene la conexion a `flotas_db` y las
credenciales de `flotas_test_user`. Ya esta configurado en este equipo y
Git lo ignora. Para otra instalacion, usar el archivo `.example` y las
instrucciones de preparacion del README.

La base de desarrollo es `flotas_db`; `flotas_test_db` se reserva para las
pruebas de integracion. Sus credenciales deben mantenerse separadas.

## 4. Iniciar Spring Boot

```powershell
.\mvnw.cmd spring-boot:run
```

Esperar el mensaje `Started FlotasMsApplication` y dejar abierta la terminal.
Desde otra terminal:

```powershell
Invoke-RestMethod -Uri 'http://localhost:8083/camiones' -Method Get
```

Si no hay registros, la respuesta es `[]`. Para ver el JSON sin formatear:

```powershell
(Invoke-WebRequest -Uri 'http://localhost:8083/camiones').Content
```

## 5. Probar en Postman

Importar `postman/Flota.postman_collection.json`. Mantener `base_url` en
`http://localhost:8083` y ejecutar las 40 peticiones en orden con una
iteracion. Seguir la [guia de Postman](../postman/README.md), incluida la
limpieza si se interrumpe la ejecucion.

## 6. Ejecutar pruebas y generar el JAR

Detener Spring Boot con Ctrl+C. Con PostgreSQL iniciado, desde la raiz:

```powershell
.\mvnw.cmd clean package
```

Deben aprobarse las 52 pruebas y aparecer `BUILD SUCCESS`. Este comando
genera `target/flotas-ms-0.0.1-SNAPSHOT.jar`. Las pruebas de integracion
limpian sus registros en `flotas_test_db`, por eso esa base es exclusiva
para pruebas.

## 7. Iniciar el JAR

Con el puerto 8083 libre, ejecutar desde la misma carpeta:

```powershell
& "$env:JAVA_HOME\bin\java.exe" -jar target/flotas-ms-0.0.1-SNAPSHOT.jar
```

Comprobar de nuevo GET `/camiones` desde otra terminal. Detener el JAR con
Ctrl+C al terminar. No iniciar el JAR y `spring-boot:run` al mismo tiempo.

## Consultar PostgreSQL

```powershell
psql -h localhost -U postgres -d flotas_db
```

Dentro de psql:

```sql
SELECT current_database();
SELECT id, patente, capacidad_kg, estado, conductor_id FROM camiones;
```

Usar `\d camiones` para revisar las columnas y `\q` para salir.

## Problemas frecuentes

| Mensaje o situacion | Revisar |
| --- | --- |
| `psql` o `pg_ctl` no se reconoce | Ejecutar el ajuste de PATH del paso 1 en esa terminal. |
| Java distinto de 21 | Corregir JAVA_HOME y comprobar `.\mvnw.cmd -version`. |
| Conexion rechazada en 5432 | Comprobar PostgreSQL con `pg_isready` y el paso 2. |
| Error de autenticacion | Revisar usuario y contrasena en el archivo local o las variables de entorno. |
| Puerto 8083 ocupado | Detener la instancia anterior de Flota desde su terminal. |
| Fallan pruebas al conectar | Revisar `flotas_test_db`, `flotas_test_user` y TEST_DB_PASSWORD. |
| Error 409 en POST o PUT | La patente ya pertenece a otro camion; usar una diferente. |
| Error 400 | Revisar campos, JSON, estado y parametros segun el README. |

Los logs de Spring Boot aparecen en su terminal. Los reportes de las pruebas
estan en `target/surefire-reports`.
