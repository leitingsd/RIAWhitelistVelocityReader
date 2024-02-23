package com.ghostchu.plugins.riawhitelistvelocity.database;

import org.bspfsystems.yamlconfiguration.configuration.ConfigurationSection;

public class HikariUtil {
    private HikariUtil() {
    }

    public static cc.carm.lib.easysql.hikari.HikariConfig createHikariConfig(ConfigurationSection section) {
        cc.carm.lib.easysql.hikari.HikariConfig config = new cc.carm.lib.easysql.hikari.HikariConfig();
        config.setDriverClassName("com.mysql.cj.jdbc.Driver");
        if (section == null) {
            throw new IllegalArgumentException("database.properties section in configuration not found");
        }
        for (String key : section.getKeys(false)) {
            config.addDataSourceProperty(key, String.valueOf(section.get(key)));
        }
        return config;
    }
}
