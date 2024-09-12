package com.queryexecution;

import javafx.scene.control.CheckBox;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class QueryHistoryFetcher {

    private final String jwtToken;
    private final String hostname;
    private final String loginType;
    private final CheckBox httpFlag;
    private final String timeInterval;
    private final String queryId;

    public QueryHistoryFetcher(String jwtToken, String hostname, String loginType, CheckBox httpFlag, String timeInterval, String queryId) {
        this.jwtToken = jwtToken;
        this.hostname = hostname;
        this.loginType = loginType;
        this.httpFlag = httpFlag;
        this.timeInterval = timeInterval;
        this.queryId = queryId;

        if (httpFlag == null) {
            httpFlag.setSelected(false); // Set default to false
        } else {
            httpFlag = httpFlag;
        }
    }

    // Fetch for loginType "I"
    public List<QueryHistoryData> fetchQueryHistoryI() throws Exception {
        String protocol = httpFlag.isSelected() ? "http://" : "https://";
        ZonedDateTime dateTime = Instant.now().atZone(ZoneOffset.UTC).minusMinutes(20);
        String queryDateTimeStart = DateTimeFormatter.ISO_INSTANT.withZone(ZoneOffset.UTC).format(dateTime.toInstant());

        String urlString;
        if ("QueryID".equals(timeInterval) && queryId != null && !queryId.isEmpty()) {
            urlString = String.format("%s%s:10502/queries/orgId/default?limit=21&queryId=%s", protocol, hostname, queryId);
        } else {
            String timeIntervalValue = convertTimeInterval(timeInterval);
            urlString = String.format("%s%s:10502/queries/orgId/default?limit=21&querySource=user&status=success&queryStarted=%s&queryDateTimeStart=%s",
                protocol, hostname, timeIntervalValue, queryDateTimeStart);
        }

        HttpURLConnection conn = createConnection(urlString);
        return parseResponseI(conn);
    }

    // Fetch for loginType "C"
    public List<QueryHistoryData> fetchQueryHistoryC() throws Exception {
        String protocol = httpFlag.isSelected() ? "http://" : "https://";
        ZonedDateTime qstartDateTime = Instant.now().atZone(ZoneOffset.UTC);
        String qstartDate = DateTimeFormatter.ISO_INSTANT.withZone(ZoneOffset.UTC).format(qstartDateTime.toInstant());

        ZonedDateTime qendDateTime = qstartDateTime.minusMinutes(getMinutesFromInterval(timeInterval));
        String qendDate = DateTimeFormatter.ISO_INSTANT.withZone(ZoneOffset.UTC).format(qendDateTime.toInstant());

        String urlString;
        if ("QueryID".equals(timeInterval) && queryId != null && !queryId.isEmpty()) {
            urlString = String.format("%s%s/wapi/p/queries?page=1&queryId=%s&showCanaries=true&startDate=%s&endDate=%s",
                protocol, hostname, queryId, qendDate, qstartDate);
        } else {
            urlString = String.format("%s%s/wapi/p/queries?page=1&showCanaries=true&startDate=%s&endDate=%s&status=failed&status=running&status=successful&queryType=User",
                protocol, hostname, qendDate, qstartDate);
        }

        HttpURLConnection conn = createConnection(urlString);
        return parseResponseC(conn);
    }

    // Convert the time interval string to corresponding minutes
    private int getMinutesFromInterval(String timeInterval) {
        switch (timeInterval) {
            case "5 Min":
                return 5;
            case "30 Min":
                return 30;
            default:
                return 60; // Default to 1 hour if not 5 or 30
        }
    }

    // Create and configure the HTTP connection
    private HttpURLConnection createConnection(String urlString) throws Exception {
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Authorization", "Bearer " + jwtToken);
        conn.setRequestProperty("Accept", "application/json");
        return conn;
    }

    // Parse the JSON response for loginType "I"
    private List<QueryHistoryData> parseResponseI(HttpURLConnection conn) throws Exception {
        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = in.readLine()) != null) {
            response.append(line);
        }
        in.close();

        JSONObject jsonResponse = new JSONObject(response.toString());
        JSONArray dataArray = jsonResponse.getJSONObject("response").getJSONArray("data");

        List<QueryHistoryData> queryHistoryDataList = new ArrayList<>();
        for (int i = 0; i < dataArray.length(); i++) {
            JSONObject dataObject = dataArray.getJSONObject(i);
            JSONArray timelineEventsArray = dataObject.getJSONArray("timeline_events");

            String queryId = dataObject.optString("query_id", "");
            String userId = dataObject.optString("user_id", "");
            String projectName = dataObject.optString("project_caption", "");
            String cubeName = dataObject.optString("cube_name", "");
            String queryText = dataObject.optString("query_text", "");
            String queryLanguage = dataObject.optString("query_language", "");
            String queryStart = timelineEventsArray.length() > 0 ? timelineEventsArray.getJSONObject(0).optString("started", "") : "";
            double totalDuration = 0.0;
            for (int j = 0; j < timelineEventsArray.length(); j++) {
                totalDuration += timelineEventsArray.getJSONObject(j).optDouble("duration", 0.0);
            }
            String status = jsonResponse.getJSONObject("status").optString("message", "Unknown Status");

            queryHistoryDataList.add(new QueryHistoryData(queryId, userId, projectName, cubeName, queryText, queryLanguage, queryStart, String.valueOf(totalDuration), status));
        }
        return queryHistoryDataList;
    }

    // Parse the JSON response for loginType "C"
    private List<QueryHistoryData> parseResponseC(HttpURLConnection conn) throws Exception {
        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = in.readLine()) != null) {
            response.append(line);
        }
        in.close();

        JSONObject jsonResponse = new JSONObject(response.toString());
        JSONArray resultsArray = jsonResponse.getJSONArray("results");

        List<QueryHistoryData> queryHistoryDataList = new ArrayList<>();
        for (int i = 0; i < resultsArray.length(); i++) {
            JSONObject resultObject = resultsArray.getJSONObject(i);

            if (resultObject.has("catalogName") && resultObject.has("modelName") && "User".equals(resultObject.optString("queryType", ""))) {
                String queryId = resultObject.optString("queryId", "");
                String userId = resultObject.optString("userId", "");
                String projectName = resultObject.optString("catalogName", "");
                String cubeName = resultObject.optString("modelName", "");
                String queryText = resultObject.optString("queryText", "");
                String queryLanguage = resultObject.optString("queryLanguage", "");
                String queryStart = Instant.ofEpochMilli(resultObject.optLong("startTime", 0)).toString();
                double totalDuration = resultObject.optDouble("totalDuration", 0.0);
                String status = resultObject.optString("status", "Unknown");

                queryHistoryDataList.add(new QueryHistoryData(queryId, userId, projectName, cubeName, queryText, queryLanguage, queryStart, String.valueOf(totalDuration), status));
            }
        }
        return queryHistoryDataList;
    }

    // Helper method to convert the time interval
    private String convertTimeInterval(String interval) {
        if ("5 Min".equals(interval)) return "5m";
        if ("30 Min".equals(interval)) return "30m";
        return "1h"; // Default 1 hour if not specified
    }
}