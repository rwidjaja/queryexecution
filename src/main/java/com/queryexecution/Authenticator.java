package com.queryexecution;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Base64;

public class Authenticator {

    // Authenticate and get JWT token
    public static String authenticate(String username, String password, String hostname) throws IOException {
        URL url = new URL("https", hostname, 10500, "/default/auth");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET"); // Changed to GET as per curl command
        conn.setRequestProperty("Authorization", "Basic " + encodeCredentials(username, password));

        int responseCode = conn.getResponseCode();
        if (responseCode == 200) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                // Assuming the response body contains the JWT token
                return response.toString();
            }
        } else {
            throw new IOException("Authentication failed: HTTP response code " + responseCode);
        }
    }

    // Encode credentials for Basic Authentication
    private static String encodeCredentials(String username, String password) {
        String credentials = username + ":" + password;
        return Base64.getEncoder().encodeToString(credentials.getBytes());
    }

    // Verify JWT token by making a secure ping request
    private static boolean verifyToken(String jwtToken, String hostname) throws IOException {
        URL url = new URL("https", hostname, 10502, "/secureping");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Authorization", "Bearer " + jwtToken);

        int responseCode = conn.getResponseCode();
        return responseCode == 200; // Return true if the response code is 200 OK
    }
}