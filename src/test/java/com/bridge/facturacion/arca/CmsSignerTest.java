package com.bridge.facturacion.arca;

import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.SignerInformation;
import org.bouncycastle.cms.jcajce.JcaSimpleSignerInfoVerifierBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class CmsSignerTest {

    @TempDir
    Path dir;

    @Test
    void signBase64_generaUnCmsVerificable_conElContenidoAdentro() throws Exception {
        CertificadosDePrueba.Rutas rutas = CertificadosDePrueba.generarEn(dir);
        String tra = TraBuilder.build();

        String base64 = CmsSigner.signBase64(tra, rutas.cert(), rutas.key());

        // 1) Es Base64 valido y es una estructura CMS valida.
        CMSSignedData cms = new CMSSignedData(Base64.getDecoder().decode(base64));

        // 2) El contenido firmado es exactamente el TRA (firma "attached").
        byte[] contenido = (byte[]) cms.getSignedContent().getContent();
        assertEquals(tra, new String(contenido, StandardCharsets.UTF_8));

        // 3) La firma verifica contra el certificado incluido en el sobre.
        SignerInformation firmante = cms.getSignerInfos().getSigners().iterator().next();
        X509CertificateHolder cert = (X509CertificateHolder) cms.getCertificates()
                .getMatches(firmante.getSID()).iterator().next();
        assertTrue(firmante.verify(
                new JcaSimpleSignerInfoVerifierBuilder().setProvider("BC").build(cert)));
    }

    @Test
    void signBase64_tiraArcaException_siLaClaveNoExiste() {
        assertThrows(ArcaException.class,
                () -> CmsSigner.signBase64("tra", "/no/existe.crt", "/no/existe.key"));
    }
}
