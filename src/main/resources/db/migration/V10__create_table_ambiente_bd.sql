-- =============================================================================
--  V10 — La base guarda en qué ambiente vive
--
--  Hasta acá, homologación y producción eran bases idénticas por dentro. El
--  ambiente solo existía en el .env, que lee la aplicación Java; un script SQL
--  corriendo dentro del contenedor de Postgres no tenía forma de saber dónde
--  estaba parado. Eso hacía imposible que un script destructivo se protegiera
--  solo: la única defensa era que la persona se acordara.
--
--  Esta tabla lleva UNA fila que dice HOMOLOGACION o PRODUCCION. La escribe la
--  aplicación al arrancar (RegistroDeAmbiente), a partir de arca.ambiente, que
--  ya viene validado contra las URLs por ArcaAmbienteValidator.
--
--  Queda vacía a propósito: el valor no lo pone la migración sino el arranque,
--  porque depende del .env de cada despliegue y no del código.
-- =============================================================================

CREATE TABLE ambiente_bd
(
    id            SMALLINT    PRIMARY KEY DEFAULT 1,
    ambiente      VARCHAR(20) NOT NULL,
    registrado_en TIMESTAMP   NOT NULL DEFAULT now(),

    -- Una sola fila posible: el ambiente es uno solo por base.
    CONSTRAINT ck_ambiente_bd_fila_unica CHECK (id = 1),
    CONSTRAINT ck_ambiente_bd_valor CHECK (ambiente IN ('HOMOLOGACION', 'PRODUCCION'))
);
