-- Ejecutar como postgres una sola vez, desde la base postgres.
CREATE USER flotas_test_user;
CREATE DATABASE flotas_test_db OWNER flotas_test_user;

-- Despues asignar una contrasena desde psql con: \password flotas_test_user
