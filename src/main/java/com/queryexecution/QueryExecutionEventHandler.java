package com.queryexecution;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.concurrent.Task;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import java.util.List;

public class QueryExecutionEventHandler {
    private final QueryExecution ui;
    private String jwtToken;
    private String loginType;

    public QueryExecutionEventHandler(QueryExecution ui) {
        this.ui = ui;
    }

    public void handleLogin() {
        String username = ui.getUsernameField().getText();
        String password = ui.getPasswordField().getText();
        String hostname = ui.getHostnameField().getText();

        Task<Void> loginTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                try {
                    // Determine login type and authenticate
                    if (Authenticator.isI2024Host(hostname)) {
                        jwtToken = Authenticator.authenticateI2024(username, password, hostname);
                        loginType = "I";
                    } else {
                        jwtToken = Authenticator.authenticateC2024(username, password, hostname);
                        loginType = "C";
                    }

                    // Fetch and populate projects
                    ProjectFetcher projectFetcher = new ProjectFetcher(jwtToken, hostname);
                    List<String> projects = projectFetcher.fetchProjects();

                    // Update UI on the JavaFX Application Thread
                    Platform.runLater(() -> {
                        ui.getProjectNameComboBox().getItems().setAll(projects);
                        ui.getStatusLabel().setText("Status: Login successful");
                        ui.getExecuteButton().setDisable(false);
                    });
                } catch (Exception ex) {
                    Platform.runLater(() -> {
                        ui.getStatusLabel().setText("Status: Error - " + ex.getMessage());
                        ui.getExecuteButton().setDisable(true);
                    });
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
                protected Void call() throws Exception {
                    try {
                        ProjectFetcher projectFetcher = new ProjectFetcher(jwtToken, ui.getHostnameField().getText());
                        List<String> cubes = projectFetcher.fetchCubesForProject(selectedProject);

                        // Update UI on the JavaFX Application Thread
                        Platform.runLater(() -> {
                            ui.getCubeNameComboBox().getItems().setAll(cubes);
                        });
                    } catch (Exception ex) {
                        Platform.runLater(() -> {
                            ui.getStatusLabel().setText("Status: Error fetching cubes - " + ex.getMessage());
                        });
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
            protected List<List<String>> call() throws Exception {
                try {
                    String xmlResult;
                    if ("Analytic".equals(queryType)) {
                        xmlResult = AnalyticQueryExecutor.executeAnalyticQuery(jwtToken, projectName, cubeName, hostname, query);
                    } else if ("SQL".equals(queryType)) {
                        xmlResult = SQLQueryExecutor.executeSQLQuery(jwtToken, projectName, hostname, query);
                    } else {
                        return List.of(List.of("Invalid query type selected."));
                    }

                    return SQLQueryExecutor.parseXMLToRows(xmlResult);
                } catch (Exception ex) {
                    return List.of(List.of("Error: " + ex.getMessage()));
                }
            }

            @Override
            protected void succeeded() {
                List<List<String>> rows = getValue();
                updateTableView(rows);
            }

            @Override
            protected void failed() {
                ui.getResultTableView().getItems().clear();
                ui.getResultTableView().getItems().add(List.of("Error: " + getException().getMessage()));
            }
        };
        new Thread(executeQueryTask).start();
    }

    private void updateTableView(List<List<String>> rows) {
        ui.getResultTableView().getColumns().clear();
        ui.getResultTableView().getItems().clear();

        if (rows.isEmpty()) {
            return;
        }
        List<String> headers = rows.get(0);
        for (int i = 0; i < headers.size(); i++) {
            final int colIndex = i;
            TableColumn<List<String>, String> column = new TableColumn<>(headers.get(i));
            column.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().get(colIndex)));
            ui.getResultTableView().getColumns().add(column);
        }
        for (int i = 1; i < rows.size(); i++) {
            ui.getResultTableView().getItems().add(rows.get(i));
        }
    }
}