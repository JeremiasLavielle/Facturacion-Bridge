-- =============================================================================
--  V9 — Baja de los emisores placeholder sembrados en V6
--
--  V6 mezcló dos cosas que conviene tener separadas: la ESTRUCTURA de la base
--  (que es igual en todos los ambientes y va versionada en git) y los DATOS de
--  quién factura (que cambian por ambiente, incluyen datos personales reales y
--  no tienen por qué estar en un repositorio público).
--
--  Desde acá las migraciones solo tocan estructura. Los emisores reales se
--  cargan con seed/emisores.sql, que está fuera de git. Ver seed/emisores.sql.example.
--
--  Por qué desactivar y no borrar ni editar: los emisores de V6 ya tienen
--  facturas con CAE apuntando a ellos (facturas.emisor_id). Borrarlos rompería
--  la clave foránea y reescribir su CUIT falsearía el histórico: comprobantes
--  emitidos bajo un CUIT figurarían como emitidos por otra persona.
--
--  GET /emisores ya filtra por activo, así que los desactivados desaparecen del
--  selector del frontend sin tocar código Java, y las facturas viejas siguen
--  resolviendo su emisor por clave foránea.
-- =============================================================================

-- Todo lo que existe en este punto viene del seed de prueba de V6.
UPDATE emisores
SET activo = FALSE;
