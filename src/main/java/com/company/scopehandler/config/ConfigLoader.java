package com.company.scopehandler.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public final class ConfigLoader {
    private ConfigLoader() {
    }

    public static Properties load(Path externalFile) {
        Properties properties = new Properties();
        try (InputStream in = ConfigLoader.class.getClassLoader().getResourceAsStream("application.properties")) {
            if (in != null) {
                properties.load(in);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load application.properties", e);
        }

        if (externalFile != null) {
            try (InputStream in = Files.newInputStream(externalFile)) {
                properties.load(in);
            } catch (IOException e) {
                throw new IllegalStateException("Failed to load config file: " + externalFile, e);
            }
        }

        return properties;
    }
}
