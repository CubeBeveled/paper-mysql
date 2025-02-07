package org.beveled.paperMysql;

import ch.vorburger.exec.ManagedProcessException;
import ch.vorburger.mariadb4j.DB;
import ch.vorburger.mariadb4j.DBConfigurationBuilder;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public final class PaperMysql extends JavaPlugin {
    private static Logger logger;
    private DB database;
    private final FileConfiguration config = this.getConfig();
    private final File configFile = new File(this.getDataFolder(), "config.yml");

    @Override
    public void onEnable() {
        logger = getLogger();

        logger.info("Reading config");
        if (!this.getDataFolder().exists()) this.getDataFolder().mkdir();
        if (!configFile.exists()) {

                Map<String, Object> userData = new HashMap<>();
                Map<String, Object> databases = new HashMap<>();
                Map<String, Object> exampleDB = new HashMap<>();

                // Set user data
                userData.put("username", "root");
                userData.put("password", getRandomString(8));

                exampleDB.put("user", userData); // Put said user data into an user: thing
                databases.put("luckperms", exampleDB); // Put user: into luckperms:

                config.set("port", 3306);
                config.set("databases", databases);

                try {
                    config.save(configFile);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
        }

        DBConfigurationBuilder dbConfig = DBConfigurationBuilder.newBuilder();

        dbConfig.setPort(Integer.parseInt(config.get("port").toString()));
        dbConfig.setDataDir(this.getDataFolder().toString().concat("/data"));
        dbConfig.setLibDir(this.getDataFolder().toString().concat("/libs"));

        logger.info("Installing database");
        try {
            database = DB.newEmbeddedDB(dbConfig.build());
        } catch (ManagedProcessException e) {
            throw new RuntimeException(e);
        }

        logger.info("Starting database on port " + config.get("port").toString());
        try {
            database.start();
        } catch (ManagedProcessException e) {
            throw new RuntimeException(e);
        }

        Map<String, Object> databases = (Map<String, Object>) config.get("databases");
        for (String dbName : databases.keySet()) {
            Map<String, String> user = (Map<String, String>) ((Map<String, Object>) databases.get(dbName)).get("user");
            try {
                database.createDB(dbName, user.get("username"), user.get("password"));
            } catch (ManagedProcessException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public void onDisable() {
        if (database != null) {
            try {
                database.stop();
            } catch (ManagedProcessException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static String getRandomString(int length) {
        String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder(length);

        for (int i = 0; i < length; i++) {
            int index = random.nextInt(characters.length());
            sb.append(characters.charAt(index));
        }

        return sb.toString();
    }
}
