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

            NodeList columns = doc.getElementsByTagName("column");
            NodeList rows = doc.getElementsByTagName("row");

            // Extract column headers
            List<String> headers = new ArrayList<>();
            for (int i = 0; i < columns.getLength(); i++) {
                Element column = (Element) columns.item(i);
                if (column != null) {
                    NodeList names = column.getElementsByTagName("name");
                    if (names.getLength() > 0) {
                        headers.add(names.item(0).getTextContent());
                    }
                }
            }
            rowsList.add(headers);

            // Extract rows
            for (int i = 0; i < rows.getLength(); i++) {
                NodeList rowColumns = rows.item(i).getChildNodes();
                List<String> row = new ArrayList<>();
                for (int j = 0; j < rowColumns.getLength(); j++) {
                    if (rowColumns.item(j) != null) {
                        row.add(rowColumns.item(j).getTextContent());
                    }
                }
                rowsList.add(row);
            }

        } catch (Exception e) {
            System.err.println("Error parsing XML response: " + e.getMessage());
            e.printStackTrace();
        }

        return rowsList;
    }
}