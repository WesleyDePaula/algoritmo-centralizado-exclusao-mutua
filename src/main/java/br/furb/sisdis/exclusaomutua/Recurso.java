package br.furb.sisdis.exclusaomutua;

import lombok.extern.slf4j.Slf4j;

import java.util.Random;

@Slf4j
public class Recurso {

    private static final Random RANDOM = new Random();

    /**
     * Define tempo de consumo do recurso, entre 5-15 segundos
     */
    public static void consumir() {
        log.info("### Recurso sendo consumido...");
        try {
            Thread.sleep(RANDOM.nextLong(5000, 15001));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        log.info("### Recurso consumido.");
    }
}
