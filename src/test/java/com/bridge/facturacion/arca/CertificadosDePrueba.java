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


final class CertificadosDePrueba {

    record Rutas(String cert, String key) {}

    static {
        if (Security.getProvider("BC") == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    private CertificadosDePrueba() {}

    static Rutas generarEn(Path dir) throws Exception {
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

        Path certPath = dir.resolve("cert.pem");
        Path keyPath = dir.resolve("key.pem");
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
