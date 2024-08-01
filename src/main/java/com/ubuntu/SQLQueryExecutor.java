package com.ubuntu;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class SQLQueryExecutor {

    public static String executeSQLQuery(String jwt, String projectName, String hostname, String query) throws IOException {
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
            // Read and return the response body
            StringBuilder response = new StringBuilder();
            try (BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
            }

            // Parse the XML response and format it as a table
            return parseAndFormatXML(response.toString());
        }
    }
    private static String parseAndFormatXML(String xml) {
        StringBuilder result = new StringBuilder();
    
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new java.io.ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
            doc.getDocumentElement().normalize();
    
            NodeList columns = doc.getElementsByTagName("column");
            NodeList rows = doc.getElementsByTagName("row");
    
            // Print column headers
            for (int i = 0; i < columns.getLength(); i++) {
                Element column = (Element) columns.item(i);
                if (column != null) {
                    NodeList names = column.getElementsByTagName("name");
                    if (names.getLength() > 0) {
                        result.append(names.item(0).getTextContent()).append("\t");
                    }
                }
            }
            result.append("\n");
    
            // Print rows
            for (int i = 0; i < rows.getLength(); i++) {
                NodeList rowColumns = rows.item(i).getChildNodes();
                for (int j = 0; j < rowColumns.getLength(); j++) {
                    if (rowColumns.item(j) != null) {
                        result.append(rowColumns.item(j).getTextContent()).append("\t");
                    }
                }
                result.append("\n");
            }
    
        } catch (Exception e) {
            e.printStackTrace();
            return "Error parsing XML: " + e.getMessage();
        }
    
        return result.toString();
    }
}
