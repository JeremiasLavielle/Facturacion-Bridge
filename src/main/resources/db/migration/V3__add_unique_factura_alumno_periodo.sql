ALTER TABLE facturas
    ADD CONSTRAINT uk_factura_alumno_periodo UNIQUE (alumno_id, periodo);
