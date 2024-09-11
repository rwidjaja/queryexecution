package com.queryexecution;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.concurrent.Task;
import javafx.scene.control.TableColumn;

import java.io.IOException;
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
            protected Void call() {
                try {
                    // Check if the host is I-2024
                    boolean isI2024 = Authenticator.isI2024Host(hostname);

                    if (isI2024) {
                        loginType = "I";
                        jwtToken = Authenticator.authenticateI2024(username, password, hostname);
                    } else {
                        loginType = "C";
                        jwtToken = Authenticator.authenticateC2024(username, password, hostname);
                    }

                    // Fetch and populate projects
                    ProjectFetcher projectFetcher = new ProjectFetcher(jwtToken, hostname, loginType);
                    List<String> projects = projectFetcher.fetchProjects();

                    // Update UI on the JavaFX Application Thread
                    Platform.runLater(() -> {
                        ui.getProjectNameComboBox().getItems().setAll(projects);
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


    public void handleProjectSelection() throws IOException {
        String selectedProject = ui.getProjectNameComboBox().getValue();
        if (selectedProject != null) {
            Task<Void> fetchCubesTask = new Task<>() {
                @Override
                protected Void call() {
                    try {
                        ProjectFetcher projectFetcher = new ProjectFetcher(jwtToken, ui.getHostnameField().getText(), loginType);
                        List<String> cubes = projectFetcher.fetchCubesForProject(selectedProject);

                        // Update UI on the JavaFX Application Thread
                        Platform.runLater(() -> {
                            ui.getCubeNameComboBox().getItems().setAll(cubes);
                        });
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
                updateResultTable(rows);
            }

            @Override
            protected void failed() {
                ui.getResultTableView().getItems().clear();
                ui.getResultTableView().getItems().add(List.of("Error: " + getException().getMessage()));
            }
        };
        new Thread(executeQueryTask).start();
    }

    private void updateResultTable(List<List<String>> results) {
        ui.getResultTableView().getColumns().clear(); // Clear existing columns

        if (results.isEmpty()) return;

        // Add columns based on the result set
        for (int i = 0; i < results.get(0).size(); i++) {
            TableColumn<List<String>, String> column = new TableColumn<>("Column " + (i + 1));
            final int colIndex = i;
            column.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().get(colIndex)));
            ui.getResultTableView().getColumns().add(column);
        }

        // Add rows to the table
        ui.getResultTableView().getItems().setAll(results);
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