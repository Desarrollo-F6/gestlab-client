package com.f6.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AppConfigTest {

    private static Properties props(String key, String value) {
        Properties p = new Properties();
        p.setProperty(key, value);
        return p;
    }

    @Test
    void usaDefaultSiNoHayNadaConfigurado() {
        AppConfig config = AppConfig.resolve(new Properties(), Collections.emptyMap(), new Properties());
        assertEquals(AppConfig.DEFAULT_SERVER_URL, config.getServerUrl());
    }

    @Test
    void propiedadDeSistemaGanaSobreEnvYArchivo() {
        AppConfig config = AppConfig.resolve(
                props(AppConfig.SYSTEM_PROPERTY, "https://sys"),
                Map.of(AppConfig.ENV_VARIABLE, "https://env"),
                props(AppConfig.FILE_KEY, "https://file"));
        assertEquals("https://sys", config.getServerUrl());
    }

    @Test
    void envGanaSobreArchivo() {
        AppConfig config = AppConfig.resolve(
                new Properties(),
                Map.of(AppConfig.ENV_VARIABLE, "https://env"),
                props(AppConfig.FILE_KEY, "https://file"));
        assertEquals("https://env", config.getServerUrl());
    }

    @Test
    void valoresEnBlancoSeIgnoran() {
        AppConfig config = AppConfig.resolve(
                props(AppConfig.SYSTEM_PROPERTY, "  "),
                Map.of(AppConfig.ENV_VARIABLE, ""),
                props(AppConfig.FILE_KEY, " http://localhost:3000 "));
        assertEquals("http://localhost:3000", config.getServerUrl());
    }

    @Test
    void detectaUrlsSinTls() {
        assertTrue(AppConfig.isSecure("https://notilab.urbe.edu"));
        assertTrue(AppConfig.isSecure("WSS://host"));
        assertFalse(AppConfig.isSecure("http://localhost:3000"));
    }

    @Test
    void leeArchivoDePropiedades(@TempDir Path dir) throws IOException {
        Path file = dir.resolve("gestlab.properties");
        Files.write(file, "server.url=https://desde-archivo\n".getBytes());
        assertEquals("https://desde-archivo", AppConfig.readFile(file.toFile()).getProperty(AppConfig.FILE_KEY));
    }

    @Test
    void archivoInexistenteDevuelvePropiedadesVacias() {
        assertTrue(AppConfig.readFile(new File("no-existe-" + System.nanoTime())).isEmpty());
    }
}
