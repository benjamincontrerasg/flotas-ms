# Coleccion Postman de Flota

La coleccion contiene 40 peticiones con comprobaciones automaticas para el
CRUD, los filtros y las respuestas de error de Flota. No requiere un archivo
de entorno ni contrasenas en Postman.

## Ejecutar en Postman

1. Iniciar PostgreSQL y configurar `application-local.properties` siguiendo
   el README principal del repositorio.
2. Desde la raiz del proyecto, iniciar una sola instancia:

   ```powershell
   .\mvnw.cmd spring-boot:run
   ```

3. En Postman, seleccionar **Import** e importar
   `postman/Flota.postman_collection.json`.
4. Revisar la variable de la coleccion `base_url`. Su valor inicial es
   `http://localhost:8083`; cambiarlo si la aplicacion usa otro puerto.
5. Abrir **Run collection**, seleccionar las 40 peticiones y ejecutarlas en
   su orden original, con una iteracion. Activar **Keep variable values**
   para conservar los IDs si se interrumpe la ejecucion.
6. Revisar los resultados. Las respuestas 400, 404, 409, 405 y 415 de los
   casos de error son esperadas; sus comprobaciones deben aprobarse.

| Carpeta | Peticiones | Contenido |
| --- | --- | --- |
| 01 Crear y consultar | 01-08 | Crear dos camiones, consultar y filtrar. |
| 02 Casos de error | 09-29 | Validaciones, duplicados, formatos y recursos inexistentes. |
| 03 Actualizar y filtrar | 30-35 | Actualizar, conservar el estado y quitar el conductor. |
| 04 Limpieza | 36-40 | Eliminar los dos camiones creados y comprobar su ausencia. |

La primera peticion genera patentes y un ID de conductor para la prueba.
Los IDs de los camiones se guardan automaticamente al crearlos. Los filtros
no suponen que la base este vacia. El conductor es solo una referencia:
esta coleccion no crea usuarios ni comprueba su existencia en Usuarios.

La limpieza elimina solo los dos camiones creados en esa ejecucion. Si se
interrumpe, conservar las variables y terminar la carpeta **04 Limpieza**
antes de comenzar otra vez: la primera peticion reinicia los IDs guardados.
Tambien se pueden enviar las peticiones individualmente siguiendo su orden.

## Ejecutar desde la terminal

Con Node.js instalado y la aplicacion iniciada, desde la raiz del proyecto:

```powershell
npx --yes --package=newman@6.2.1 newman run postman/Flota.postman_collection.json
```

Para otro puerto, agregar `--env-var "base_url=http://localhost:8084"`.
Newman descarga el paquete si hace falta. La coleccion se verifico con
Newman contra PostgreSQL de pruebas: 40 peticiones y 147 comprobaciones
aprobadas. Al terminar, no quedaron camiones de la ejecucion.
