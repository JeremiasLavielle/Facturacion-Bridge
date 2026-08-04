ALTER TABLE facturas
    ADD COLUMN emisor_id BIGINT;

-- Las facturas previas a la Fase 7 (homologacion) quedan a nombre del
-- primer emisor cargado en V6.
UPDATE facturas
SET emisor_id = (SELECT MIN(id) FROM emisores);

ALTER TABLE facturas
    ALTER COLUMN emisor_id SET NOT NULL;

ALTER TABLE facturas
    ADD CONSTRAINT fk_factura_emisor FOREIGN KEY (emisor_id) REFERENCES emisores (id);
