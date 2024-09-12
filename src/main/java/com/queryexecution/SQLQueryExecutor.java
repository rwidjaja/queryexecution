package com.queryexecution;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;


import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;

public class SQLQueryExecutor {

    public static String executeSQLQuery(String jwt, String projectName, String hostname, String query, String loginType) throws IOException {
        String urlStr;
        if ("I".equals(loginType)) {
            urlStr = String.format("https://%s:10502/query/orgId/default/submit", hostname);
        } else if ("C".equals(loginType)) {
            urlStr = String.format("https://%s/engine/query/submit", hostname);
        } else {
            throw new IllegalArgumentException("Invalid loginType: " + loginType);
        }

        HttpURLConnection conn = null;
        try {
            URL url = new URL(urlStr);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Authorization", "Bearer " + jwt);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            String escapedQuery = query.replace("\\", "\\\\")
                                       .replace("\"", "\\\"")
                                       .replace("\n", "\\n")
                                       .replace("\r", "\\r");

            String payload = String.format(
                "{ \"language\": \"SQL\", \"query\": \"%s\", \"context\": { \"organization\": { \"id\": \"default\" }, \"environment\": { \"id\": \"default\" }, \"project\": { \"name\": \"%s\" } }, \"aggregation\": { \"useAggregates\": false, \"genAggregates\": false }, \"fakeResults\": false, \"dryRun\": false, \"useLocalCache\": true, \"timeout\": \"2.minutes\" }",
                escapedQuery, projectName
            );

            // Debugging purposes
            // System.out.println("Payload: " + payload);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(payload.getBytes(StandardCharsets.UTF_8));
                os.flush();
            }

            int responseCode = conn.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                StringBuilder errorResponse = new StringBuilder();
                try (BufferedReader errorReader = new BufferedReader(new InputStreamReader(conn.getErrorStream()))) {
                    String inputLine;
                    while ((inputLine = errorReader.readLine()) != null) {
                        errorResponse.append(inputLine);
                    }
                }
                throw new IOException("SQL query execution failed: HTTP response code " + responseCode + " - Response body: " + errorResponse.toString());
            }


            // Read successful response
            StringBuilder response = new StringBuilder();
            try (BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
            }

                        // Debugging: Print the raw inbound result
                     //   System.out.println("Raw response from server: " + response.toString());
                        
            return response.toString();

        } catch (IOException e) {
            System.err.println("Error executing SQL query: " + e.getMessage());
            throw e;
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    public static List<List<String>> parseXMLToRows(String xmlResult) {
        List<List<String>> rowsList = new ArrayList<>();
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new java.io.ByteArrayInputStream(xmlResult.getBytes(StandardCharsets.UTF_8)));
            doc.getDocumentElement().normalize();
    
            // Extract column headers from the metadata section
            NodeList columnNodes = doc.getElementsByTagName("column");
            List<String> headers = new ArrayList<>();
            
            // Add headers based on name tags
            for (int i = 0; i < columnNodes.getLength(); i++) {
                Node columnNode = columnNodes.item(i);
                if (columnNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element columnElement = (Element) columnNode;
                    Node nameNode = columnElement.getElementsByTagName("name").item(0);
                    if (nameNode != null) {
                        String header = nameNode.getTextContent().trim();
                        headers.add(header);
                    }
                }
            }
    
            // Debugging: Print extracted headers
        //    System.out.println("Extracted Headers: " + headers);
    
            // Add headers as the first row if they exist
            if (!headers.isEmpty()) {
                rowsList.add(headers);
            }
    
            // Extract row data
            NodeList rowNodes = doc.getElementsByTagName("row");
            for (int i = 0; i < rowNodes.getLength(); i++) {
                NodeList rowColumns = rowNodes.item(i).getChildNodes();
                List<String> row = new ArrayList<>();
                boolean isEmptyRow = true; // to track if all columns are empty
    
                for (int j = 0; j < rowColumns.getLength(); j++) {
                    Node columnNode = rowColumns.item(j);
                    if (columnNode.getNodeType() == Node.ELEMENT_NODE) {
                        String value = columnNode.getTextContent().trim();
                        if (!value.isEmpty()) {
                            isEmptyRow = false;
                        }
                        row.add(value.isEmpty() ? "" : value);
                    }
                }
    
                // If it's not an entirely empty row, add it
                if (!isEmptyRow) {
                    // Adjust row alignment: handle cases where the first value is missing (e.g., for Color)
                    if (row.size() < headers.size()) {
                        row.add(0, ""); // Add an empty value to the start if a column is missing
                    }
    
                    rowsList.add(row);
                }
            }
    
        } catch (Exception e) {
            System.err.println("Error parsing XML response: " + e.getMessage());
            e.printStackTrace();
        }
    
        return rowsList;
    }
}