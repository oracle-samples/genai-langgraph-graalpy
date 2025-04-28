/*******************************************************************************
 * Embedding Python-based AI Agents into Java Applications version 1.0.
 *
 * Copyright (c)  2024,  Oracle and/or its affiliates.
 * Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.
 ******************************************************************************/
package com.oracle.ateam.examples.langgraph;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Value;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.json.JsonObject;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

/**
 * A simple langgraph agent service. Examples:
 *
 * Get agent message: curl -X GET http://localhost:8080/langgraph
 *
 * The message is returned as a String object.
 */
@Path("/langgraph")
@RequestScoped
public class LangGraphAgentService {
    private final static Logger LOGGER = Logger.getLogger(LangGraphAgentService.class.getName());
    
    @Inject
    private PyContext pyContext;


    /**
     * Get Answer Service
     * Return a agent message.
     *
     * @param jsonData  The json payload of the request, it must contain a field called question.
     * @return The output of the agent.
     */
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/invoke")
    public String getAnswer(JsonObject jsonData) {
        String question = jsonData.getString("question");
        LOGGER.log(Level.INFO, "Received question: {0}", question);
        String msg = "";
        try {
            Context context = pyContext.getPyContext();
            Value pyHCMAgentClass = context.getBindings("python").getMember("hcm_agent");
            HCMAgent hcmAgent = pyHCMAgentClass.as(HCMAgent.class);
            msg = hcmAgent.invoke(question);
            LOGGER.log(Level.INFO, "Agnet response: {0}", msg);
        } catch (PolyglotException e) {
            if (e.isExit()) {
                System.exit(e.getExitStatus());
            } else {
                throw e;
            }
        }
        return msg;
    }
}
