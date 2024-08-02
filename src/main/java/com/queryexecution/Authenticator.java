package com.queryexecution;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Base64;
import org.json.JSONObject;

public class Authenticator {

    // Check if the host is I-2024
    public static boolean isI2024Host(String hostname) throws IOException {
        URL url = new URL("https://" + hostname + ":10502/ping");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");

        try {
            int responseCode = conn.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    return response.toString().contains("OK");
                }
            }
        } catch (IOException e) {
            // Handle any connection errors or exceptions
        }
        return false;
    }

    // Authenticate and get JWT token for I-2024
    public static String authenticateI2024(String username, String password, String hostname) throws IOException {
        URL url = new URL("https://" + hostname + ":10500/default/auth");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Authorization", "Basic " + encodeCredentials(username, password));
        
        int responseCode = conn.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                return response.toString(); // Assuming the response body contains the JWT token
            }
        } else {
            throw new IOException("Authentication failed: HTTP response code " + responseCode);
        }
    }
    
    // Authenticate and get JWT token for C-2024
    public static String authenticateC2024(String username, String password, String hostname) throws IOException {
        URL url = new URL("https://" + hostname + "/auth/realms/atscale/protocol/openid-connect/token");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
    
        String body = "client_id=atscale-api&client_secret=YWCz6qDw9n4V5Zb2sVc6vPZN6Lxu5Il7"
                + "&username=" + username + "&password=" + password
                + "&grant_type=password";
        try (OutputStream os = conn.getOutputStream()) {
            os.write(body.getBytes());
            os.flush();
        }
    
        int responseCode = conn.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    response.append(line);
                }
    
                JSONObject jsonResponse = new JSONObject(response.toString());
                return jsonResponse.getString("access_token");
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
}