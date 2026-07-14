-- El numero de comprobante (correlativo por punto de venta) lo asigna ARCA
-- al emitir. Hasta ahora lo descartabamos; el PDF del comprobante y el QR
-- obligatorio (RG 4892) lo necesitan, asi que pasa a persistirse.
ALTER TABLE facturas
    ADD COLUMN numero_comprobante BIGINT;
