package com.bridge.facturacion.arca;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

import java.io.FileWriter;
import java.math.BigInteger;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.util.Date;


// public: tambien lo usa la base de tests de integracion (paquete raiz).
public final class CertificadosDePrueba {

    public record Rutas(String cert, String key) {}

    static {
        if (Security.getProvider("BC") == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    private CertificadosDePrueba() {}

    public static Rutas generarEn(Path dir) throws Exception {
        return generar(dir.resolve("cert.pem"), dir.resolve("key.pem"));
    }

    /**
     * Genera certificado y clave con la convencion REAL de la Fase 7:
     * {@code <base>/<cuit>/certificado.crt} y {@code <base>/<cuit>/clave-privada.key},
     * como los espera un {@code Emisor} cuyo certs-dir es {@code base}.
     */
    public static Rutas generarParaCuit(Path base, String cuit) throws Exception {
        Path dir = base.resolve(cuit);
        java.nio.file.Files.createDirectories(dir);
        return generar(dir.resolve("certificado.crt"), dir.resolve("clave-privada.key"));
    }

    private static Rutas generar(Path certPath, Path keyPath) throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair keyPair = generator.generateKeyPair();

        X500Name nombre = new X500Name("CN=Test Bridge, O=Test, C=AR");
        X509CertificateHolder holder = new JcaX509v3CertificateBuilder(
                nombre,
                BigInteger.ONE,
                Date.from(Instant.now().minusSeconds(3600)),
                Date.from(Instant.now().plusSeconds(86400)),
                nombre,
                keyPair.getPublic())
                .build(crearFirmante(keyPair));
        X509Certificate cert = new JcaX509CertificateConverter()
                .setProvider("BC").getCertificate(holder);

        escribirPem(certPath, cert);
        escribirPem(keyPath, keyPair.getPrivate());
        return new Rutas(certPath.toString(), keyPath.toString());
    }

    private static ContentSigner crearFirmante(KeyPair keyPair) throws Exception {
        return new JcaContentSignerBuilder("SHA256withRSA")
                .setProvider("BC").build(keyPair.getPrivate());
    }

    private static void escribirPem(Path path, Object objeto) throws Exception {
        try (JcaPEMWriter writer = new JcaPEMWriter(new FileWriter(path.toFile()))) {
            writer.writeObject(objeto);
        }
    }
}
