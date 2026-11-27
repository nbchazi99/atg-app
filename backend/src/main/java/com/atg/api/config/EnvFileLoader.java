package com.atg.api.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Loads a gitignored {@code .env} file into system properties so local
 * development does not need every secret exported by hand.
 *
 * <p>Rules: real environment variables always win, blank values and {@code #}
 * comments are skipped, and values may optionally be quoted. Spring's
 * {@code @Value("${KEY:}")} placeholders resolve system properties before the
 * environment, so this composes with the existing configuration.</p>
 */
public final class EnvFileLoader {

    private static final Logger log = LoggerFactory.getLogger(EnvFileLoader.class);

    private EnvFileLoader() {
    }

    public static void load() {
        for (String candidate : new String[]{".env", "backend/.env"}) {
            Path path = Path.of(candidate);
            if (!Files.isRegularFile(path)) continue;
            int loaded = 0;
            try {
                for (String line : Files.readAllLines(path)) {
                    String trimmed = line.trim();
                    if (trimmed.isEmpty() || trimmed.startsWith("#")) continue;
                    int eq = trimmed.indexOf('=');
                    if (eq <= 0) continue;
                    String key = trimmed.substring(0, eq).trim();
                    String value = trimmed.substring(eq + 1).trim();
                    if (value.length() >= 2
                            && ((value.startsWith("\"") && value.endsWith("\""))
                            || (value.startsWith("'") && value.endsWith("'")))) {
                        value = value.substring(1, value.length() - 1);
                    }
                    if (value.isEmpty()) continue;
                    if (System.getenv(key) != null) continue; // real env wins
                    System.setProperty(key, value);
                    loaded++;
                }
                log.info("Loaded {} entries from {} (values not logged)", loaded, path.toAbsolutePath());
            } catch (Exception e) {
                log.warn("Could not read {}", path.toAbsolutePath(), e);
            }
        }
    }
}
