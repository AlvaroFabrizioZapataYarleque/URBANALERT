package com.utp.urbanalert;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Punto de entrada de la API REST de UrbanAlert.
 *
 * Fase 1 (AP1 - Unidad 1): gestion basica de Ciudadanos, Areas Responsables
 * e Incidencias, con base de datos en memoria (H2) para pruebas rapidas.
 */
@SpringBootApplication
public class UrbanAlertApplication {

    public static void main(String[] args) {
        SpringApplication.run(UrbanAlertApplication.class, args);
    }
}
