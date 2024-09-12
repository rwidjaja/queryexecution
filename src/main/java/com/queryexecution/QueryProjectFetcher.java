package com.queryexecution;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;
import javafx.scene.control.CheckBox;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class QueryProjectFetcher {

  private final String jwtToken;
  private final String hostname;
  private final String loginType;
  private static final String API_URL_I = "/projects/orgId/default?includeCubes=true";
  private static final String API_URL_C = "/wapi/p/catalog";
  private final CheckBox httpFlag;

  public QueryProjectFetcher(String jwtToken, String hostname, String loginType, CheckBox httpFlag) {
    this.jwtToken = jwtToken;
    this.hostname = hostname;
    this.loginType = loginType;
    this.httpFlag = httpFlag;

    if (httpFlag == null) {
      httpFlag.setSelected(false); // Set default to false
  } else {
      httpFlag = httpFlag;
  }
  }

  private String fetchData(String endpoint) throws Exception {
    URL url = new URL("https://" + hostname + (loginType.equals("I") ? ":10502" : "") + endpoint);
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

  public Map < String, String > fetchProjects() throws Exception {
    String endpoint = loginType.equals("I") ? API_URL_I : API_URL_C;
    String response = fetchData(endpoint);
    Map < String, String > projectMap = new HashMap < > ();

    try {
      if (loginType.equals("I")) {
        JSONObject jsonObject = new JSONObject(response);
        JSONObject responseObject = jsonObject.getJSONObject("response");
        JSONArray projects = responseObject.getJSONArray("projects");

        for (int i = 0; i < projects.length(); i++) {
          JSONObject project = projects.getJSONObject(i);
          String projectId = project.optString("id", null);
          String projectCaption = project.optString("caption", null);

          // Debugging information
          //   System.out.println("fetchProjects Project ID: " + projectId);
          //   System.out.println("fetchProjects Project Caption: " + projectCaption);

          if (projectId != null && projectCaption != null) {
            projectMap.put(projectId, projectCaption);
          } else {
            System.err.println("Missing project ID or caption for project: " + project.toString());
          }
        }
      } else {
        if (isValidJson(response)) {
          JSONArray projectsArray = new JSONArray(response);
          for (int i = 0; i < projectsArray.length(); i++) {
            JSONObject project = projectsArray.getJSONObject(i);
            String projectId = project.optString("id", null);
            String projectCaption = project.optString("name", null);

            // Debugging information
            //    System.out.println("fetchProjects Project ID: " + projectId);
            //    System.out.println("fetchProjects Project Caption: " + projectCaption);

            if (projectId != null && projectCaption != null) {
              projectMap.put(projectId, projectCaption);
            } else {
              System.err.println("Missing project ID or caption for project: " + project.toString());
            }
          }
        } else {
          System.err.println("Invalid JSON response for type C.");
        }
      }
    } catch (JSONException e) {
      System.err.println("JSON parsing error: " + e.getMessage());
      e.printStackTrace();
    }

    return projectMap;
  }

  public Map < String, Map < String, String >> fetchProjectsAndCubes() throws Exception {
    String endpoint = loginType.equals("I") ? API_URL_I : API_URL_C;
    String response = fetchData(endpoint);
    Map < String, Map < String, String >> projectToCubesMap = new HashMap < > ();

    if (loginType.equals("I")) {
      JSONObject jsonObject = new JSONObject(response);
      JSONObject responseObject = jsonObject.getJSONObject("response");
      JSONArray projects = responseObject.getJSONArray("projects");

      for (int i = 0; i < projects.length(); i++) {
        JSONObject project = projects.getJSONObject(i);
        String projectId = project.getString("id");
        String projectCaption = project.getString("caption");

        //    System.out.println("fetchProjectsAndCubes Project ID: " + projectId);
        //    System.out.println("fetchProjectsAndCubes Project Caption: " + projectCaption);

        JSONArray cubesArray = project.getJSONArray("cubes");
        Map < String, String > cubes = new HashMap < > ();
        for (int j = 0; j < cubesArray.length(); j++) {
          JSONObject cube = cubesArray.getJSONObject(j);
          String cubeType = cube.getString("type");
          if (cubeType.equals("cube")) {
            String cubeId = cube.getString("id");
            String cubeCaption = cube.getString("caption");

            //               System.out.println("fetchProjectsAndCubes Cube ID: " + cubeId);
            //               System.out.println("fetchProjectsAndCubes Cube Caption: " + cubeCaption);

            cubes.put(cubeId, cubeCaption);
          }
        }
        projectToCubesMap.put(projectId, cubes);
      }
    } else {
      if (isValidJson(response)) {
        JSONArray projectsArray = new JSONArray(response);
        for (int i = 0; i < projectsArray.length(); i++) {
          JSONObject project = projectsArray.getJSONObject(i);
          String projectId = project.optString("id", null);
          String projectCaption = project.optString("caption", null);

          // Debugging information
          //   System.out.println("fetchProjectsAndCubes Project ID: " + projectId);
          //   System.out.println("fetchProjectsAndCubes Project Caption: " + projectCaption);

          if (projectId != null && projectCaption != null) {
            // Assuming the response for type C has cubes info
            JSONArray cubesArray = project.optJSONArray("cubes");
            Map < String, String > cubes = new HashMap < > ();
            if (cubesArray != null) {
              for (int j = 0; j < cubesArray.length(); j++) {
                JSONObject cube = cubesArray.getJSONObject(j);
                String cubeId = cube.optString("id", null);
                String cubeCaption = cube.optString("caption", null);

                if (cubeId != null && cubeCaption != null) {
                  cubes.put(cubeId, cubeCaption);
                }
              }
            }
            projectToCubesMap.put(projectId, cubes);
          } else {
            System.err.println("Missing project ID or caption for project: " + project.toString());
          }
        }
      } else {
        System.err.println("Invalid JSON response for type C.");
      }
    }
    return projectToCubesMap;
  }

  private boolean isValidJson(String response) {
    try {
      new JSONArray(response);
      return true;
    } catch (Exception e) {
      return false;
    }
  }
}