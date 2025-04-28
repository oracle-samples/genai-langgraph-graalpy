/** *****************************************************************************
 * Embedding Python-based AI Agents into Java Applications version 1.0.
 *
 * Copyright (c)  2024,  Oracle and/or its affiliates.
 * Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.
 ***************************************************************************** */
package com.oracle.ateam.examples.langgraph;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.PolyglotException;

import io.helidon.config.Config;
import io.helidon.logging.common.LogConfig;
import io.helidon.microprofile.server.Server;

public class Main {

    private final static Logger LOGGER = Logger.getLogger(PyContext.class.getName());

    /**
     * Main method to start the Helidon MP Server and Load the GraalPy context.
     * 
     */
    public static void main(String[] args) {
        try {
            LogConfig.configureRuntime();
            Config config = Config.create();
            Server server = startServer(config);
            PyContext pyContext = new PyContext();
            Context context = pyContext.createPyContext();
            context.eval("python", "import hcm_agent");
            LOGGER.log(Level.INFO, "Server Started with PyContext: http://localhost:{0}/langgraph", server.port());
        } catch (PolyglotException e) {
            if (e.isExit()) {
                System.exit(e.getExitStatus());
            } else {
                throw e;
            }
        }

    }

    static Server startServer(Config config) {
        Config.global(config);
        Server server = Server.builder()
                .config(config)
                .build();
        return server.start();
    }
}
