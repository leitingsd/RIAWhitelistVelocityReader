package com.ghostchu.plugins.riawhitelistvelocity;

import cc.carm.lib.easysql.EasySQL;
import com.ghostchu.plugins.riawhitelistvelocity.database.DatabaseManager;
import com.ghostchu.plugins.riawhitelistvelocity.manager.WhitelistManager;
import com.ghostchu.plugins.riawhitelistvelocity.manager.bean.FastWhitelistQuery;
import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PreLoginEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.TextReplacementConfig;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bspfsystems.yamlconfiguration.configuration.ConfigurationSection;
import org.bspfsystems.yamlconfiguration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

@Plugin(
        id = "riawhitelistvelocity",
        name = "RIAWhitelistVelocity",
        version = "1.0-SNAPSHOT"
)
public class RIAWhitelistVelocity {
    private YamlConfiguration configuration;
    @Getter
    private DatabaseManager databaseManager;
    @Getter
    private WhitelistManager whitelistManager;
    @Inject
    private Logger logger;

    @Inject
    @DataDirectory
    private Path dataDirectory;

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        saveDefaultConfig();
        reloadConfig();
        setupDatabase();
        this.whitelistManager = new WhitelistManager(this, databaseManager);
    }

    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) {
        if (databaseManager != null) {
            EasySQL.shutdownManager(databaseManager.getSqlManager());
        }
    }

    @Subscribe
    public void onPlayerConnect(PreLoginEvent event) {
        String username = event.getUsername();
        if (username == null) {
            event.setResult(PreLoginEvent.PreLoginComponentResult.denied(Component.text("[WhitelistVelocity] Username in connection cannot be null")));
            return;
        }
        try {
            FastWhitelistQuery query = whitelistManager.fastCheckWhitelist(username).join();
            if (!query.isWhitelisted()) {
                event.setResult(PreLoginEvent.PreLoginComponentResult.denied(text("general.reject-join-no-whitelist")));
                return;
            }
            if (!query.isAllowed()) {
                event.setResult(PreLoginEvent.PreLoginComponentResult.denied(text("general.reject-join-incorrect-cases", username, query.getCorrectWhitelistedName())));
            }
            logger.info("Player {} passed whitelist check", event.getUsername());
        } catch (Exception e) {
            e.printStackTrace();
            event.setResult(PreLoginEvent.PreLoginComponentResult.denied(text("general.internal-error", e.getMessage())));
        }
    }


    private void setupDatabase() {
        try {
            this.databaseManager = new DatabaseManager(this);
        }catch (Exception e){
            e.printStackTrace();
            System.exit(-1);
        }
    }

    public void saveConfig() {
        try {
            configuration.save(new File(dataDirectory.toFile(), "config.yml"));
        } catch (IOException e) {
            logger.warn("Cannot save the configuration.", e);
        }
    }

    public void saveDefaultConfig() {
        // Create plugin config folder if it doesn't exist
        try {
            if (!dataDirectory.toFile().exists()) {
                logger.info("Created config folder: " + dataDirectory.toFile().mkdir());
            }
            File configFile = new File(dataDirectory.toFile(), "config.yml");
            // Copy default config if it doesn't exist
            if (!configFile.exists()) {
                FileOutputStream outputStream = new FileOutputStream(configFile); // Throws IOException
                InputStream in = getClass().getResourceAsStream("config.yml"); // This file must exist in the jar resources folder
                in.transferTo(outputStream); // Throws IOException
            }
        } catch (IOException e) {
            logger.warn("Cannot save the default configuration.", e);
        }
    }

    public ConfigurationSection getConfig() {
        return configuration;
    }

    public void reloadConfig() {
        this.configuration = YamlConfiguration.loadConfiguration(new File(dataDirectory.toFile(), "config.yml"));
    }

    public Component text(String key, Object... args) {
        ConfigurationSection section = getConfig().getConfigurationSection("lang");
        if (section == null) throw new IllegalStateException("lang section not found");
        String string = section.getString(key, "missing:" + key);
        Component component = MiniMessage.miniMessage().deserialize(string);
        return fillArgs(component, convert(args));
    }

    @NotNull
    public Component[] convert(@Nullable Object... args) {
        if (args == null || args.length == 0) {
            return new Component[0];
        }
        Component[] components = new Component[args.length];
        for (int i = 0; i < args.length; i++) {
            Object obj = args[i];
            if (obj == null) {
                components[i] = Component.empty();
                continue;
            }
            Class<?> clazz = obj.getClass();
            if (obj instanceof Component component) {
                components[i] = component;
                continue;
            }
            if (obj instanceof ComponentLike componentLike) {
                components[i] = componentLike.asComponent();
                continue;
            }
            // Check
            try {
                if (Character.class.equals(clazz)) {
                    components[i] = Component.text((char) obj);
                    continue;
                }
                if (Byte.class.equals(clazz)) {
                    components[i] = Component.text((Byte) obj);
                    continue;
                }
                if (Integer.class.equals(clazz)) {
                    components[i] = Component.text((Integer) obj);
                    continue;
                }
                if (Long.class.equals(clazz)) {
                    components[i] = Component.text((Long) obj);
                    continue;
                }
                if (Float.class.equals(clazz)) {
                    components[i] = Component.text((Float) obj);
                    continue;
                }
                if (Double.class.equals(clazz)) {
                    components[i] = Component.text((Double) obj);
                    continue;
                }
                if (Boolean.class.equals(clazz)) {
                    components[i] = Component.text((Boolean) obj);
                    continue;
                }
                if (String.class.equals(clazz)) {
                    components[i] = LegacyComponentSerializer.legacySection().deserialize((String) obj);
                    continue;
                }
                components[i] = LegacyComponentSerializer.legacySection().deserialize(obj.toString());
            } catch (Exception exception) {
                exception.printStackTrace();
                components[i] = LegacyComponentSerializer.legacySection().deserialize(obj.toString());
            }
        }
        return components;
    }

    /**
     * Replace args in origin to args
     *
     * @param origin origin
     * @param args   args
     * @return filled component
     */
    @NotNull
    public static Component fillArgs(@NotNull Component origin, @Nullable Component... args) {
        for (int i = 0; i < args.length; i++) {
            origin = origin.replaceText(TextReplacementConfig.builder()
                    .matchLiteral("{" + i + "}")
                    .replacement(args[i] == null ? Component.empty() : args[i])
                    .build());
        }
        return origin.compact();
    }
}
