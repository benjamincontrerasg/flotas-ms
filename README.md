# flotas-ms

Microservicio de flota de RutaLimpia, un sistema de recoleccion de reciclaje
a domicilio.

## Estado actual

El proyecto base compila y genera un JAR. Incluye Spring Boot, Maven Wrapper
y la configuracion del puerto 8083. Los endpoints de camiones y la conexion
a PostgreSQL se implementaran en los pasos siguientes.

## Tecnologias

- Java 21.
- Spring Boot 4.0.5.
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
vez y detenerla con Ctrl+C. Los endpoints CRUD todavia estan pendientes.

## Archivos de desarrollo

- `.gitignore` excluye archivos generados, configuraciones locales de IDE
  y credenciales de los commits.
- `.gitattributes` mantiene LF en los archivos de texto y CRLF en `mvnw.cmd`.
- `.mvn`, `mvnw` y `mvnw.cmd` permiten usar Maven sin instalarlo por separado.

## Proximos pasos

Definir los campos y estados de los camiones, incorporar persistencia y
validacion, configurar PostgreSQL nativo e implementar el CRUD.
Despues se agregaran pruebas automatizadas y una coleccion Postman.
