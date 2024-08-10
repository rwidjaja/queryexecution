package com.queryexecution;

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
    private final String loginType;
    private static final String API_URL_I = "/api/1.0/org/default/folders";
    private static final String API_URL_C = "/wapi/p/catalog";

    public ProjectFetcher(String jwtToken, String hostname, String loginType) {
        this.jwtToken = jwtToken;
        this.hostname = hostname;
        this.loginType = loginType;
    }

    private String fetchData(String endpoint) throws Exception {
        URL url = new URL("https://" + hostname + (loginType.equals("I") ? ":10500" : "") + endpoint);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Authorization", "Bearer " + jwtToken);

        int responseCode = conn.getResponseCode();
        if (responseCode != HttpURLConnection.HTTP_OK) {
            throw new RuntimeException("Failed : HTTP error code : " + responseCode);
        }

        BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
            response.append(line);
        }
        conn.disconnect();
        return response.toString();
    }

    private boolean isValidJson(String response) {
        try {
            new JSONArray(response);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public List<String> fetchProjects() throws Exception {
        String endpoint = loginType.equals("I") ? API_URL_I : API_URL_C;
        String response = fetchData(endpoint);
        List<String> projectNames = new ArrayList<>();
        
        // Debugging output
       // System.out.println("Raw Response: " + response);
        
        if (loginType.equals("I")) {
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
        } else {
            if (isValidJson(response)) {
                JSONArray projectsArray = new JSONArray(response);
                for (int i = 0; i < projectsArray.length(); i++) {
                    JSONObject project = projectsArray.getJSONObject(i);
                    projectNames.add(project.getString("caption"));
                }
            } else {
                System.err.println("Invalid JSON response for type C.");
            }
        }
        return projectNames;
    }

    public List<String> fetchCubesForProject(String projectName) throws Exception {
        String endpoint = loginType.equals("I") ? API_URL_I : API_URL_C;
        String response = fetchData(endpoint);
        List<String> cubeNames = new ArrayList<>();
        
        // Debugging output
      //  System.out.println("Raw Response: " + response);
        
        if (loginType.equals("I")) {
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
        } else {
            if (isValidJson(response)) {
                JSONArray projectsArray = new JSONArray(response);
                for (int i = 0; i < projectsArray.length(); i++) {
                    JSONObject project = projectsArray.getJSONObject(i);
                    if (projectName.equals(project.getString("caption"))) {
                        JSONArray cubesArray = project.getJSONArray("cubes");
                        for (int j = 0; j < cubesArray.length(); j++) {
                            JSONObject cube = cubesArray.getJSONObject(j);
                            cubeNames.add(cube.getString("caption"));
                        }
                    }
                }
            } else {
                System.err.println("Invalid JSON response for type C.");
            }
        }
        return cubeNames;
    }
}