CREATE TABLE emisores
(
    id                 BIGSERIAL PRIMARY KEY,
    cuit               VARCHAR(11)  NOT NULL,
    razon_social       VARCHAR(120) NOT NULL,
    nombre_fantasia    VARCHAR(120) NOT NULL,
    domicilio          VARCHAR(200) NOT NULL,
    condicion_fiscal   VARCHAR(60)  NOT NULL,
    ingresos_brutos    VARCHAR(40)  NOT NULL,
    inicio_actividades VARCHAR(10)  NOT NULL,
    punto_venta        INT          NOT NULL,
    cert_path          VARCHAR(255) NOT NULL,
    key_path           VARCHAR(255) NOT NULL,
    activo             BOOLEAN      NOT NULL DEFAULT TRUE,
    CONSTRAINT uq_emisor_cuit_punto_venta UNIQUE (cuit, punto_venta)
);

-- Datos de HOMOLOGACION: ambos emisores comparten el CUIT y el certificado
-- de prueba (20463447277); el segundo usa el punto de venta 2 (hay que
-- habilitarlo en el portal de homologacion de ARCA). Los datos reales de
-- cada titular se cargan en la etapa final, antes del pase a produccion.
-- cert_path y key_path son RELATIVOS a la propiedad arca.certs-dir.
INSERT INTO emisores (cuit, razon_social, nombre_fantasia, domicilio,
                      condicion_fiscal, ingresos_brutos, inicio_actividades,
                      punto_venta, cert_path, key_path)
VALUES ('20463447277', 'Titular Uno (homologacion)', 'Instituto Bridge',
        'Domicilio del instituto - Ciudad, Provincia',
        'Responsable Monotributo', 'Exento', '01/01/2020',
        1, '20463447277/certificado.crt', '20463447277/clave-privada.key'),
       ('20463447277', 'Titular Dos (homologacion)', 'Instituto Bridge',
        'Domicilio del instituto - Ciudad, Provincia',
        'Responsable Monotributo', 'Exento', '01/01/2020',
        2, '20463447277/certificado.crt', '20463447277/clave-privada.key');
