/** *****************************************************************************
 * Embedding Python-based AI Agents into Java Applications version 1.0.
 *
 * Copyright (c)  2024,  Oracle and/or its affiliates.
 * Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.
 ***************************************************************************** */
package com.oracle.ateam.examples.langgraph;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * The Utility class
 *
 */
public final class Util {

    private final static Logger LOGGER = Logger.getLogger(Util.class.getName());

    /**
     * The getSystemProperty method get the property value stored in application.yaml.
     * @param key   The property key in the application.yaml
     * @return  The value of the property in String.
     *
     */
    public static String getSystemProperty(String key) {
        LOGGER.log(Level.INFO, "Get System Property using key: {0}", key);
        Config config = ConfigProvider.getConfig();
        return config.getOptionalValue(key, String.class).orElse(null);
    }

    public static Map<String, String> convertStringToMap(String input) {
        Map<String, String> map = new HashMap<>();

        if (input == null || input.trim().isEmpty()) {
            return map;
        }

        // Remove the curly braces if present
        input = input.trim();
        if (input.startsWith("{") && input.endsWith("}")) {
            input = input.substring(1, input.length() - 1).trim();
        }

        // Replace commas with newlines to prepare for Properties loading
        input = input.replaceAll(",", "\n");

        Properties props = new Properties();
        try {
            props.load(new StringReader(input));
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse input string into properties", e);
        }

        for (String name : props.stringPropertyNames()) {
            map.put(name.trim(), props.getProperty(name).trim());
        }

        return map;
    }

    public static Map<String, Object> convertJSONObjectToMap(JSONObject jsonObject) {
        Map<String, Object> map = new HashMap<>();

        Iterator<String> keys = jsonObject.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            Object value = jsonObject.get(key);

            switch (value) {
                case JSONArray jSONArray ->
                    value = convertJSONArrayToList(jSONArray);
                case JSONObject jSONObject ->
                    value = convertJSONObjectToMap(jSONObject);
                default -> {
                }
            }
            map.put(key, value);
        }
        return map;
    }

    private static List<Object> convertJSONArrayToList(JSONArray array) {
        List<Object> list = new ArrayList<>();
        for (int i = 0; i < array.length(); i++) {
            Object value = array.get(i);
            switch (value) {
                case JSONArray jSONArray ->
                    value = convertJSONArrayToList(jSONArray);
                case JSONObject jSONObject ->
                    value = convertJSONObjectToMap(jSONObject);
                default -> {
                }
            }
            list.add(value);
        }
        return list;
    }
}
