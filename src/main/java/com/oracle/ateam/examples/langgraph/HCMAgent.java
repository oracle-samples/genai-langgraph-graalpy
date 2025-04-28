/** *****************************************************************************
 * Embedding Python-based AI Agents into Java Applications version 1.0.
 *
 * Copyright (c)  2024,  Oracle and/or its affiliates.
 * Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.
 ***************************************************************************** */
package com.oracle.ateam.examples.langgraph;

/**
 * Interface for HCM Agent. It need to match the python agent method that you
 * want to invoke in your Java code.
 */
public interface HCMAgent {

    String invoke(String txt);
}
