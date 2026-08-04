-- Fase 8: notas de credito C (tipo 13). Una factura con CAE nunca se
-- borra: su unica correccion valida es una NC por el total, que deja la
-- factura en estado ANULADA (el estado es VARCHAR: no hace falta DDL).
CREATE TABLE notas_credito
(
    id                 BIGSERIAL PRIMARY KEY,
    factura_id         BIGINT         NOT NULL,
    emisor_id          BIGINT         NOT NULL,
    monto              NUMERIC(12, 2) NOT NULL,
    motivo             TEXT           NOT NULL,
    estado             VARCHAR(40)    NOT NULL,
    fecha_emision      TIMESTAMP,
    cae                VARCHAR(20),
    vencimiento_cae    DATE,
    numero_comprobante BIGINT,
    mensaje_error      TEXT,
    CONSTRAINT fk_nc_factura FOREIGN KEY (factura_id) REFERENCES facturas (id),
    CONSTRAINT fk_nc_emisor FOREIGN KEY (emisor_id) REFERENCES emisores (id),
    -- Una sola NC por factura (anulacion total, sin parciales).
    CONSTRAINT uq_nc_factura UNIQUE (factura_id)
);
