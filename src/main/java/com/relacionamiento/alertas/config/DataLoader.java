package com.relacionamiento.alertas.config;

import com.relacionamiento.alertas.repository.AlertaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataLoader implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataLoader.class);
    private final AlertaRepository alertaRepository;

    public DataLoader(AlertaRepository alertaRepository) {
        this.alertaRepository = alertaRepository;
    }

    @Override
    public void run(String... args) {
        log.info("DataLoader comprobado. Base de datos vacia o en uso. No se inyectaran datos simulados.");
    }
}