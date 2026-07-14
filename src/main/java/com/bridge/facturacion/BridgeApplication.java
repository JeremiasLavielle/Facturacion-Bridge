package com.bridge.facturacion;

import com.bridge.facturacion.arca.ArcaProperties;
import com.bridge.facturacion.pdf.EmisorProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import java.util.TimeZone;

@SpringBootApplication
@EnableConfigurationProperties({ArcaProperties.class, EmisorProperties.class})
public class BridgeApplication {

    static {
        TimeZone.setDefault(TimeZone.getTimeZone("America/Argentina/Buenos_Aires"));
    }

    public static void main(String[] args) {
        SpringApplication.run(BridgeApplication.class, args);
    }

}
