package com.queryexecution;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class JsonParser {

    public static List<String> parseProjectNames(String jsonResponse) {
        List<String> projectNames = new ArrayList<>();
        try {
            JSONObject jsonObject = new JSONObject(jsonResponse);
            JSONObject response = jsonObject.getJSONObject("response");
            JSONArray childFolders = response.getJSONArray("child_folders");

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
        } catch (Exception e) {
            e.printStackTrace();
            // Optionally log the error or rethrow as a custom exception
        }
        return projectNames;
    }

    public static List<String> parseCubeNames(String jsonResponse, String projectName) {
        List<String> cubeNames = new ArrayList<>();
        try {
            JSONObject jsonObject = new JSONObject(jsonResponse);
            JSONObject response = jsonObject.getJSONObject("response");
            JSONArray childFolders = response.getJSONArray("child_folders");

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
        } catch (Exception e) {
            e.printStackTrace();
            // Optionally log the error or rethrow as a custom exception
        }
        return cubeNames;
    }
}