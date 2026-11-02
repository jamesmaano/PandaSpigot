package com.phrust.engine;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.bukkit.configuration.file.YamlConfiguration;

/**
 * phrust.yml - settings for PHRust's own patches (kept separate from Panda's config
 * so upstream rebases never conflict with it).
 */
public final class PHRustConfig {

    public static boolean brandWhitelistEnabled = false;
    public static boolean logBrands = true;
    public static List<String> allowedBrands = Arrays.asList("vanilla");
    public static String brandKickMessage = "&cYour Minecraft client is not allowed on this server.";
    public static int maxSubsystemFailures = 5;

    static {
        load();
    }

    private PHRustConfig() {}

    public static synchronized void load() {
        File file = new File("phrust.yml");
        YamlConfiguration yaml = new YamlConfiguration();
        try {
            if (file.isFile()) {
                yaml.load(file);
            }
            brandWhitelistEnabled = yaml.getBoolean("client-brand-whitelist.enabled", brandWhitelistEnabled);
            logBrands = yaml.getBoolean("client-brand-whitelist.log-brands", logBrands);
            List<String> list = yaml.getStringList("client-brand-whitelist.allowed");
            if (list != null && !list.isEmpty()) {
                allowedBrands = list;
            }
            brandKickMessage = yaml.getString("client-brand-whitelist.kick-message", brandKickMessage);
            maxSubsystemFailures = Math.max(1, yaml.getInt("engine.max-subsystem-failures", maxSubsystemFailures));

            // Write defaults on first run so the file is discoverable.
            yaml.set("client-brand-whitelist.enabled", brandWhitelistEnabled);
            yaml.set("client-brand-whitelist.log-brands", logBrands);
            yaml.set("client-brand-whitelist.allowed", allowedBrands);
            yaml.set("client-brand-whitelist.kick-message", brandKickMessage);
            yaml.set("engine.max-subsystem-failures", maxSubsystemFailures);
            yaml.save(file);
        } catch (Throwable t) {
            // A broken phrust.yml must never stop the server from starting: keep defaults.
            LogManager.getLogger("PHRust").error("Could not load/save phrust.yml, using defaults", t);
        }
    }
}
