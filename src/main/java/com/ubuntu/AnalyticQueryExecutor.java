package com.ubuntu;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class AnalyticQueryExecutor {

    public static void executeAnalyticQuery(String jwt, String projectName, String cubeName, String hostname, String query) throws IOException {
        URL url = new URL("https", hostname, 10502, "/xmla/default");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Authorization", "Bearer " + jwt);
        conn.setRequestProperty("Content-Type", "application/xml");
        conn.setDoOutput(true);

        String xmlPayload = String.format(
            "<Envelope xmlns=\"http://schemas.xmlsoap.org/soap/envelope/\">" +
            "<Body>" +
            "<Execute xmlns=\"urn:schemas-microsoft-com:xml-analysis\">" +
            "<Command>" +
            "<Statement><![CDATA[%s]]></Statement>" +
            "</Command>" +
            "<Properties>" +
            "<PropertyList>" +
            "<Catalog>%s</Catalog>" +
            "<Cube>%s</Cube>" +
            "</PropertyList>" +
            "</Properties>" +
            "</Execute>" +
            "</Body>" +
            "</Envelope>",
            query, projectName, cubeName
        );

        try (OutputStream os = conn.getOutputStream()) {
            os.write(xmlPayload.getBytes());
            os.flush();
        }

        int responseCode = conn.getResponseCode();
        if (responseCode != 200) {
            throw new IOException("Analytic query execution failed: HTTP response code " + responseCode);
        }
    }
}