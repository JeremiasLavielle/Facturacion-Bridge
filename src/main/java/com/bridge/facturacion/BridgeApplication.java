package com.bridge.facturacion;

import com.bridge.facturacion.arca.ArcaProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import java.util.TimeZone;

@SpringBootApplication
@EnableConfigurationProperties(ArcaProperties.class)
public class BridgeApplication {

    // Bloque static: corre apenas se carga la clase, tanto en main() como
    // cuando un @SpringBootTest levanta el contexto. Nombre IANA canonico:
    // en Windows la JVM resuelve el alias viejo "America/Buenos_Aires",
    // que Postgres (Docker) rechaza al conectar.
    static {
        TimeZone.setDefault(TimeZone.getTimeZone("America/Argentina/Buenos_Aires"));
    }

    public static void main(String[] args) {
        SpringApplication.run(BridgeApplication.class, args);
    }

}
