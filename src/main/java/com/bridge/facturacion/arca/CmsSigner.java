package com.bridge.facturacion.arca;

import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.cert.jcajce.JcaCertStore;
import org.bouncycastle.cms.CMSProcessableByteArray;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.CMSSignedDataGenerator;
import org.bouncycastle.cms.jcajce.JcaSignerInfoGeneratorBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder;
import org.bouncycastle.pkcs.PKCS8EncryptedPrivateKeyInfo;

import java.io.FileInputStream;
import java.io.FileReader;
import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.List;

/**
 * Firma el TRA con CMS/PKCS#7 usando BouncyCastle.
 * Es el "sobre firmado" que WSAA exige para probar nuestra identidad:
 * contiene el TRA + la firma + nuestro certificado.
 */
final class CmsSigner {

    static {
        if (Security.getProvider("BC") == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    private CmsSigner() {}

    /** Devuelve el CMS firmado, codificado en Base64 (formato que espera loginCms). */
    static String signBase64(String tra, String certPath, String keyPath) {
        try {
            X509Certificate cert = loadCertificate(certPath);
            PrivateKey key = loadPrivateKey(keyPath);

            CMSSignedDataGenerator generator = new CMSSignedDataGenerator();
            generator.addSignerInfoGenerator(
                    new JcaSignerInfoGeneratorBuilder(
                            new JcaDigestCalculatorProviderBuilder().setProvider("BC").build())
                            .build(new JcaContentSignerBuilder("SHA256withRSA")
                                    .setProvider("BC").build(key), cert));
            generator.addCertificates(new JcaCertStore(List.of(cert)));

            CMSSignedData signed = generator.generate(
                    new CMSProcessableByteArray(tra.getBytes(StandardCharsets.UTF_8)), true);

            return Base64.getEncoder().encodeToString(signed.getEncoded());
        } catch (Exception e) {
            throw new ArcaException("No se pudo firmar el TRA (revisar certificado/clave): " + e.getMessage(), e);
        }
    }

    private static X509Certificate loadCertificate(String path) throws Exception {
        try (FileInputStream in = new FileInputStream(path)) {
            return (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(in);
        }
    }

    /** Soporta claves PEM en formato PKCS#1 (RSA PRIVATE KEY) y PKCS#8 (PRIVATE KEY). */
    private static PrivateKey loadPrivateKey(String path) throws Exception {
        try (PEMParser parser = new PEMParser(new FileReader(path))) {
            Object pem = parser.readObject();
            JcaPEMKeyConverter converter = new JcaPEMKeyConverter().setProvider("BC");
            if (pem instanceof PEMKeyPair keyPair) {
                return converter.getPrivateKey(keyPair.getPrivateKeyInfo());
            }
            if (pem instanceof PrivateKeyInfo info) {
                return converter.getPrivateKey(info);
            }
            if (pem instanceof PKCS8EncryptedPrivateKeyInfo) {
                throw new ArcaException("La clave privada esta encriptada; se espera una clave PEM sin passphrase");
            }
            throw new ArcaException("Formato de clave privada no reconocido: " + path);
        }
    }
}
