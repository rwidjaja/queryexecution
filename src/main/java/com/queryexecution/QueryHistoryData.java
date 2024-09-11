package com.queryexecution;

public class QueryHistoryData {

    private String queryId;
    private String userId;
    private String projectName;
    private String cubeName;
    private String queryText;
    private String queryLanguage;
    private String queryStart;
    private String durations;
    private String status;

    public QueryHistoryData(String queryId, String userId, String projectName, String cubeName,
                            String queryText, String queryLanguage, String queryStart,
                            String durations, String status) {
        this.queryId = queryId;
        this.userId = userId;
        this.projectName = projectName;
        this.cubeName = cubeName;
        this.queryText = queryText;
        this.queryLanguage = queryLanguage;
        this.queryStart = queryStart;
        this.durations = durations;
        this.status = status;
    }

    // Getters and setters for each field

    public String getQueryId() {
        return queryId;
    }

    public String getUserId() {
        return userId;
    }

    public String getProjectName() {
        return projectName;
    }

    public String getCubeName() {
        return cubeName;
    }

    public String getQueryText() {
        return queryText;
    }

    public String getQueryLanguage() {
        return queryLanguage;
    }

    public String getQueryStart() {
        return queryStart;
    }

    public String getDurations() {
        return durations;
    }

    public String getStatus() {
        return status;
    }
}