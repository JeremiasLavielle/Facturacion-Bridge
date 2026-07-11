package com.bridge.facturacion;

import com.bridge.facturacion.arca.ArcaProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import java.util.TimeZone;

@SpringBootApplication
@EnableConfigurationProperties(ArcaProperties.class)
public class BridgeApplication {

    public static void main(String[] args) {
        // Nombre IANA canonico. Sin esto, en Windows la JVM resuelve el alias
        // viejo "America/Buenos_Aires", que Postgres (Docker) rechaza al
        // conectar. Ademas fija las fechas fiscales en hora argentina,
        // corra donde corra la app (cabo suelto de Fase 6, resuelto aca).
        TimeZone.setDefault(TimeZone.getTimeZone("America/Argentina/Buenos_Aires"));
        SpringApplication.run(BridgeApplication.class, args);
    }

}
