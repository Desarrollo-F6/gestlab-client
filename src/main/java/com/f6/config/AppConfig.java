package com.f6.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Properties;

import org.tinylog.Logger;

/**
 * Configuracion del cliente. Orden de prioridad para la URL del servidor:
 * <ol>
 *   <li>Propiedad de sistema {@code -Dgestlab.server.url=...}</li>
 *   <li>Variable de entorno {@code GESTLAB_SERVER_URL}</li>
 *   <li>Clave {@code server.url} en {@code gestlab.properties} (directorio de trabajo)</li>
 *   <li>{@link #DEFAULT_SERVER_URL}</li>
 * </ol>
 */
public final class AppConfig {

    public static final String DEFAULT_SERVER_URL = "https://notilab.urbe.edu";
    static final String SYSTEM_PROPERTY = "gestlab.server.url";
    static final String ENV_VARIABLE = "GESTLAB_SERVER_URL";
    static final String FILE_KEY = "server.url";
    static final String FILE_NAME = "gestlab.properties";

    private final String serverUrl;

    AppConfig(String serverUrl) {
        this.serverUrl = serverUrl;
    }

    public String getServerUrl() {
        return serverUrl;
    }

    /** Carga la configuracion real del proceso. */
    public static AppConfig load() {
        return resolve(System.getProperties(), System.getenv(), readFile(new File(FILE_NAME)));
    }

    static AppConfig resolve(Properties systemProperties, Map<String, String> env, Properties file) {
        String url = firstNonBlank(
                systemProperties.getProperty(SYSTEM_PROPERTY),
                env.get(ENV_VARIABLE),
                file.getProperty(FILE_KEY),
                DEFAULT_SERVER_URL);

        if (!isSecure(url)) {
            Logger.warn("La URL del servidor no usa TLS ({}). Las ordenes viajarian sin cifrar.", url);
        }
        return new AppConfig(url);
    }

    static boolean isSecure(String url) {
        String lower = url.toLowerCase();
        return lower.startsWith("https://") || lower.startsWith("wss://");
    }

    static Properties readFile(File file) {
        Properties properties = new Properties();
        if (!file.isFile()) {
            return properties;
        }
        try (InputStream in = new FileInputStream(file)) {
            properties.load(in);
        } catch (IOException e) {
            Logger.warn(e, "No se pudo leer {}", file.getAbsolutePath());
        }
        return properties;
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                return value.trim();
            }
        }
        return null;
    }
}
