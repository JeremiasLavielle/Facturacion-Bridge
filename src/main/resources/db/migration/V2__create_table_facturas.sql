CREATE TABLE facturas
(
    id              BIGSERIAL PRIMARY KEY,
    monto           NUMERIC(12, 2) NOT NULL,
    periodo         DATE           NOT NULL,
    estado          VARCHAR(40)    NOT NULL,
    fecha_emision   TIMESTAMP,
    cae             VARCHAR(20),
    vencimiento_cae DATE,
    mensaje_error   TEXT,
    alumno_id       BIGINT         NOT NULL,
    CONSTRAINT fk_factura_alumno FOREIGN KEY (alumno_id) REFERENCES alumnos (id)
);