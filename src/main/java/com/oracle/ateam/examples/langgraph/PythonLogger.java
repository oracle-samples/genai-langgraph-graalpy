/** *****************************************************************************
 * Embedding Python-based AI Agents into Java Applications version 1.0.
 *
 * Copyright (c)  2024,  Oracle and/or its affiliates.
 * Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.
 ***************************************************************************** */
package com.oracle.ateam.examples.langgraph;

import java.util.logging.Logger;

/**
 * The PythonLogger class is the utility class to be invoked by the Python code
 * to log message in the Java log.
 *
 */
public final class PythonLogger {

    private static final Logger LOGGER = Logger.getLogger(PythonLogger.class.getName());

    public static void info(String message) {
        LOGGER.info(message);
    }

    public static void debug(String message) {
        LOGGER.fine(message);
    }

    public static void warn(String message) {

        LOGGER.warning(message);
    }

    public static void error(String message) {
        LOGGER.severe(message);
    }
}
