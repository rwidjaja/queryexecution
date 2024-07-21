package com.ubuntu;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class SQLQueryExecutor {

    public static void executeSQLQuery(String jwt, String projectName, String hostname, String query) throws IOException {
        URL url = new URL("https", hostname, 10502, "/query/orgId/default/submit");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Authorization", "Bearer " + jwt);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);

        // Properly escape the query for JSON
        String escapedQuery = query.replace("\\", "\\\\")
                                   .replace("\"", "\\\"")
                                   .replace("\n", "\\n")
                                   .replace("\r", "\\r");

        // Define the payload for the SQL query
        String payload = String.format(
            "{ \"language\": \"SQL\", \"query\": \"%s\", \"context\": { \"organization\": { \"id\": \"default\" }, \"environment\": { \"id\": \"default\" }, \"project\": { \"name\": \"%s\" } }, \"aggregation\": { \"useAggregates\": false, \"genAggregates\": false }, \"fakeResults\": false, \"dryRun\": false, \"useLocalCache\": true, \"timeout\": \"2.minutes\" }",
            escapedQuery, projectName
        );

        // Debug: print the payload
        System.out.println("Payload for SQL query: " + payload);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(payload.getBytes(StandardCharsets.UTF_8));
            os.flush();
        }

        int responseCode = conn.getResponseCode();
        if (responseCode != 200) {
            // Read and log the response body for more details
            StringBuilder response = new StringBuilder();
            try (BufferedReader in = new BufferedReader(new InputStreamReader(conn.getErrorStream()))) {
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
            }
            throw new IOException("SQL query execution failed: HTTP response code " + responseCode + " - Response body: " + response.toString());
        } else {
            System.out.println("Successfully executed SQL query.");
        }
    }
}