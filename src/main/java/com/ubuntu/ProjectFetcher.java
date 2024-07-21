package com.ubuntu;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class ProjectFetcher {

    private final String jwtToken;
    private final String hostname;
    private static final String API_URL = "/api/1.0/org/default/folders";

    public ProjectFetcher(String jwtToken, String hostname) {
        this.jwtToken = jwtToken;
        this.hostname = hostname;
    }

    private String fetchData(String endpoint) throws Exception {
        URL url = new URL("https://" + hostname + ":10500" + endpoint);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Authorization", "Bearer " + jwtToken);

        int responseCode = conn.getResponseCode();
        if (responseCode != HttpURLConnection.HTTP_OK) {
            throw new RuntimeException("Failed : HTTP error code : " + responseCode);
        }

        BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream())));
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
            response.append(line);
        }
        conn.disconnect();
        return response.toString();
    }

    public List<String> fetchProjects() throws Exception {
        String response = fetchData(API_URL);
        List<String> projectNames = new ArrayList<>();
        JSONObject jsonObject = new JSONObject(response);
        JSONObject responseJson = jsonObject.getJSONObject("response");
        JSONArray childFolders = responseJson.getJSONArray("child_folders");

        for (int i = 0; i < childFolders.length(); i++) {
            JSONObject folder = childFolders.getJSONObject(i);
            JSONArray items = folder.getJSONArray("items");
            for (int j = 0; j < items.length(); j++) {
                JSONObject item = items.getJSONObject(j);
                if ("Project".equals(item.getString("type"))) {
                    projectNames.add(item.getString("caption"));
                }
            }
        }
        return projectNames;
    }

    public List<String> fetchCubesForProject(String projectName) throws Exception {
        String response = fetchData(API_URL);
        List<String> cubeNames = new ArrayList<>();
        JSONObject jsonObject = new JSONObject(response);
        JSONObject responseJson = jsonObject.getJSONObject("response");
        JSONArray childFolders = responseJson.getJSONArray("child_folders");

        for (int i = 0; i < childFolders.length(); i++) {
            JSONObject folder = childFolders.getJSONObject(i);
            JSONArray items = folder.getJSONArray("items");
            for (int j = 0; j < items.length(); j++) {
                JSONObject item = items.getJSONObject(j);
                if ("Project".equals(item.getString("type")) && projectName.equals(item.getString("caption"))) {
                    JSONArray cubes = item.getJSONArray("cubes");
                    for (int k = 0; k < cubes.length(); k++) {
                        JSONObject cube = cubes.getJSONObject(k);
                        cubeNames.add(cube.getString("caption"));
                    }
                }
            }
        }
        return cubeNames;
    }
}