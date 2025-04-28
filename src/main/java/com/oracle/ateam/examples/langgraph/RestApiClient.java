/** *****************************************************************************
 * Embedding Python-based AI Agents into Java Applications version 1.0.
 *
 * Copyright (c)  2024,  Oracle and/or its affiliates.
 * Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.
 ***************************************************************************** */
package com.oracle.ateam.examples.langgraph;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.json.JSONObject;

/**
 * The RestApiClient class invoke the Rest API, currently only support Get and
 * Post. This class will be invoked by the Agent tool in python.
 *
 */
public class RestApiClient {

    private static final Logger LOGGER = Logger.getLogger(RestApiClient.class.getName());
    private static final HttpClient CLIENT = HttpClient.newHttpClient();

    private static String encodeCredentials(String username, String password) {
        String credentials = username + ":" + password;
        return Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Send Get Request to a HTTP REST endpoint
     *
     * @param urlString URL of the REST Service.
     * @param queryString Query String to be used for the REST call.
     * @param username Username of the REST service, it will be retrieved from
     * the application.yaml and pass it in to this method.
     * @param password Passwrod of the user to be used to invoke the REST
     * service, it will be retrieved from the OCI vault defined in the
     * application.yaml
     * @return The output of the REST Call
     *
     */
    public String sendGetRequest(String urlString, Object queryString, String username, String password) {
        String result = "";
        try {
            String queryParams = convertToQueryString(queryString);
            String fullURL = urlString + "?" + queryParams + "&onlyData=True";
            LOGGER.log(Level.INFO, "Sending GET request to {0}", fullURL);
            URI uri = new URI(fullURL);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(uri)
                    .header("Authorization", "Basic " + encodeCredentials(username, password))
                    .GET()
                    .build();

            HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            result = response.body();
            LOGGER.log(Level.INFO, "Rest call response code: {0}", response.statusCode());

        } catch (IOException | InterruptedException | URISyntaxException e) {
            LOGGER.log(Level.SEVERE, "Error sending GET request: {0} ", e);
        }
        return result;
    }

    /**
     * Send POST Request to a HTTP REST endpoint
     *
     * @param urlString URL of the REST Service.
     * @param payload Paylod in JSON format String to be used for the REST call.
     * @param username Username of the REST service, it will be retrieved from
     * the application.yaml and pass it in to this method.
     * @param password Passwrod of the user to be used to invoke the REST
     * service, it will be retrieved from the OCI vault defined in the
     * application.yaml
     * @return The output of the REST Call
     */
    public String sendPostRequest(String urlString, Object payload, String username, String password) {
        LOGGER.log(Level.INFO, "Sending POST request to {0}", urlString);
        String result = "";
        try {
            URI uri = new URI(urlString);
            String payloadString = convertToJSONString(payload);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(uri)
                    .header("Authorization", "Basic " + encodeCredentials(username, password))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payloadString))
                    .build();

            HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            result = response.body();

        } catch (IOException | InterruptedException | URISyntaxException e) {
            LOGGER.log(Level.SEVERE, "Error sending POST request: {0} ", e);
        }
        return result;
    }

    private String convertToJSONString(Object payload) {
        if (payload instanceof String string) {
            return string;
        } else if (payload instanceof Map) {
            return new JSONObject((Map<?, ?>) payload).toString();
        } else {
            return new JSONObject(payload).toString();
        }
    }

    private String convertToQueryString(Object queryParameters) {
        if (queryParameters instanceof Map<?, ?> params) {
            // Convert the map to a query string using streams, with proper encoding
            return params.entrySet()
                    .stream()
                    .map(entry -> {
                        String key = entry.getKey().toString();
                        String value = entry.getValue().toString();
                        return key + "=" + value.replace("{", "").replace("}", "").replace("'", "").replace(": ", "=").replace(", ", ";");
                    })
                    .collect(Collectors.joining("&"));
        }

        // If it's already a string, just return it
        return queryParameters != null ? queryParameters.toString() : "";
    }
}
