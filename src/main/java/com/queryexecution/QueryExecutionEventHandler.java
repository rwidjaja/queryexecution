package com.queryexecution;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.concurrent.Task;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class QueryExecutionEventHandler {
    private final QueryExecution ui;
    private String jwtToken;
    private String loginType;
    private CheckBox httpFlag;

    public QueryExecutionEventHandler(QueryExecution ui) {
        this.ui = ui;
    }

    public void handleLogin() {
        String username = ui.getUsernameField().getText();
        String password = ui.getPasswordField().getText();
        String hostname = ui.getHostnameField().getText();

        if (httpFlag == null) {
            this.httpFlag = new CheckBox();
            this.httpFlag.setSelected(false); // Set default to false
        } else {
            this.httpFlag = httpFlag;
        }

        Task<Void> loginTask = new Task<>() {
            @Override
            protected Void call() {
                try {
                    boolean isI2024 = Authenticator.isI2024Host(hostname);
                    httpFlag.setSelected(false);

                    if (isI2024) {
                        loginType = "I";
                        jwtToken = Authenticator.authenticateI2024(username, password, hostname);
                    } else {
                        loginType = "C";
                        jwtToken = Authenticator.authenticateC2024(username, password, hostname);
                    }

                    QueryProjectFetcher fetcher = new QueryProjectFetcher(jwtToken, hostname, loginType, httpFlag);
                    Map<String, String> projects = fetcher.fetchProjects();

                    Platform.runLater(() -> {
                        ui.getProjectNameComboBox().getItems().setAll(projects.values());
                        ui.getStatusLabel().setText("Status: Login successful");
                        ui.getExecuteButton().setDisable(false);
                        ui.getQueryHistoryButton().setDisable(false);
                    });
                } catch (IOException ex) {
                    handleError("Network error during login: " + ex.getMessage());
                } catch (Exception ex) {
                    handleError("Error during login: " + ex.getMessage());
                }
                return null;
            }
        };
        new Thread(loginTask).start();
    }

    public void handleProjectSelection() {
        String selectedProject = ui.getProjectNameComboBox().getValue();
        if (selectedProject != null) {
            Task<Void> fetchCubesTask = new Task<>() {
                @Override
                protected Void call() {
                    try {
                        QueryProjectFetcher fetcher = new QueryProjectFetcher(jwtToken, ui.getHostnameField().getText(), loginType, httpFlag);
                        Map<String, Map<String, String>> projectsAndCubes = fetcher.fetchProjectsAndCubes();
                        
                        String selectedProjectID = null;
                        for (Map.Entry<String, String> entry : fetcher.fetchProjects().entrySet()) {
                            if (entry.getValue().equals(selectedProject)) {
                                selectedProjectID = entry.getKey();
                                break;
                            }
                        }
                        
                        if (selectedProjectID != null) {
                            Map<String, String> cubes = projectsAndCubes.get(selectedProjectID);

                            Platform.runLater(() -> {
                                ui.getCubeNameComboBox().getItems().setAll(cubes.values());
                                ui.getCubeNameComboBox().setDisable(false);
                            });
                        } else {
                            Platform.runLater(() -> ui.getCubeNameComboBox().setDisable(true));
                        }
                    } catch (IOException ex) {
                        handleError("Network error while fetching cubes: " + ex.getMessage());
                    } catch (Exception ex) {
                        handleError("Error fetching cubes: " + ex.getMessage());
                    }
                    return null;
                }
            };
            new Thread(fetchCubesTask).start();
        }
    }

    public void handleExecuteQuery() {
        String projectName = ui.getProjectNameComboBox().getValue();
        String cubeName = ui.getCubeNameComboBox().getValue();
        String hostname = ui.getHostnameField().getText();
        String queryType = ui.getQueryTypeComboBox().getValue();
        String query = ui.getQueryTextArea().getText();

        Task<List<List<String>>> executeQueryTask = new Task<>() {
            @Override
            protected List<List<String>> call() {
                try {
                    String xmlResult;
                    if ("Analytic".equals(queryType)) {
                        xmlResult = AnalyticQueryExecutor.executeAnalyticQuery(jwtToken, projectName, cubeName, hostname, query, loginType);
                    } else if ("SQL".equals(queryType)) {
                        xmlResult = SQLQueryExecutor.executeSQLQuery(jwtToken, projectName, hostname, query, loginType);
                    } else {
                        throw new IllegalArgumentException("Invalid query type selected.");
                    }
                    return SQLQueryExecutor.parseXMLToRows(xmlResult);
                } catch (IOException ex) {
                    return List.of(List.of("Network error: " + ex.getMessage()));
                } catch (IllegalArgumentException ex) {
                    return List.of(List.of("Error: " + ex.getMessage()));
                } catch (Exception ex) {
                    return List.of(List.of("Error executing query: " + ex.getMessage()));
                }
            }
    
            @Override
            protected void succeeded() {
                List<List<String>> rows = getValue();
                updateResultTable(rows); // Update the result table
            }
    
            @Override
            protected void failed() {
                ui.getResultTableView().getItems().clear();
                ui.getResultTableView().getItems().add(FXCollections.observableArrayList(List.of("Error: " + getException().getMessage())));
            }
        };
        new Thread(executeQueryTask).start();
    }

    private void updateResultTable(List<List<String>> results) {
        ui.getResultTableView().getColumns().clear(); // Clear existing columns
    
        if (results.isEmpty()) return;
    
        // Extract headers from the first row
        List<String> headers = results.get(0);
    
        // Create columns based on the headers
        ObservableList<TableColumn<ObservableList<String>, ?>> columns = FXCollections.observableArrayList();
        for (int i = 0; i < headers.size(); i++) {
            TableColumn<ObservableList<String>, String> column = new TableColumn<>(headers.get(i));
            int columnIndex = i; // Capture the index to use in cellValueFactory
            column.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().get(columnIndex)));
            columns.add(column);
        }
        ui.getResultTableView().getColumns().setAll(columns);
    
        // Add rows data
        ObservableList<ObservableList<String>> observableData = FXCollections.observableArrayList();
        for (int i = 1; i < results.size(); i++) { // Start from 1 to skip header row
            observableData.add(FXCollections.observableArrayList(results.get(i)));
        }
        ui.updateTableData(observableData);
    }

    public String getJwtToken() {
        return jwtToken;
    }

    public String getLoginType() {
        return loginType;
    }

    private void handleError(String errorMessage) {
        Platform.runLater(() -> {
            ui.getStatusLabel().setText("Status: " + errorMessage);
            ui.getExecuteButton().setDisable(true);
        });
    }
}