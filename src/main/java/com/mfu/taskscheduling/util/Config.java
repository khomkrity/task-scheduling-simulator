package com.mfu.taskscheduling.util;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * Config is a class that loads and manages a configuration properties file.
 * It provides a simple way to read values from the properties file using keys.
 */
public class Config {
    private final Properties properties;

    /**
     * Creates a new Config instance and loads the properties from the specified file.
     *
     * @param configFilePath The path to the configuration properties file.
     */
    public Config(String configFilePath) {
        properties = new Properties();
        try (FileInputStream in = new FileInputStream(configFilePath)) {
            properties.load(in);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Retrieves the value associated with the specified key from the properties file.
     *
     * @param key The key for the property value to be retrieved.
     * @return The value associated with the key or null if the key is not found.
     */
    public String getProperty(String key) {
        return properties.getProperty(key);
    }
}