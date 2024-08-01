package com.queryexecution;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.List;

public class QueryExecution extends Application {

    private TextField hostnameField;
    private TextField usernameField;
    private PasswordField passwordField;
    private ComboBox<String> projectNameComboBox;
    private ComboBox<String> cubeNameComboBox;
    private ComboBox<String> queryTypeComboBox;
    private TextArea queryTextArea;
    private TableView<List<String>> resultTableView;
    private Button executeButton;
    private Button loginButton;
    private Label statusLabel;
    private String jwtToken;

    @Override
    public void start(Stage primaryStage) {
        configureSSL();  // Configure SSL before starting the application

        primaryStage.setTitle("Query Executor");

        // Create UI elements for login
        Label hostnameLabel = new Label("Hostname:");
        hostnameField = new TextField();

        Label usernameLabel = new Label("Username:");
        usernameField = new TextField();

        Label passwordLabel = new Label("Password:");
        passwordField = new PasswordField();

        loginButton = new Button("Login");
        statusLabel = new Label("Status: Not logged in");

        // Create UI elements for query input
        Label projectNameLabel = new Label("Project:");
        projectNameComboBox = new ComboBox<>();
        projectNameComboBox.setOnAction(e -> handleProjectSelection());

        Label cubeNameLabel = new Label("Cube:");
        cubeNameComboBox = new ComboBox<>();

        Label queryTypeLabel = new Label("Query Type:");
        queryTypeComboBox = new ComboBox<>();
        queryTypeComboBox.getItems().addAll("SQL", "Analytic");

        Label queryLabel = new Label("Query:");
        queryTextArea = new TextArea();
        queryTextArea.setPrefSize(600, 200); // Increase size of TextArea

        executeButton = new Button("Execute");
        executeButton.setDisable(true);  // Initially disabled

        // Create UI elements for result display
        resultTableView = new TableView<>();
        resultTableView.setEditable(false);

        // Layout for login section
        GridPane loginGrid = new GridPane();
        loginGrid.setVgap(10);
        loginGrid.setHgap(10);
        loginGrid.setPadding(new Insets(10));

        loginGrid.add(hostnameLabel, 0, 0);
        loginGrid.add(hostnameField, 1, 0);
        loginGrid.add(usernameLabel, 0, 1);
        loginGrid.add(usernameField, 1, 1);
        loginGrid.add(passwordLabel, 0, 2);
        loginGrid.add(passwordField, 1, 2);
        loginGrid.add(loginButton, 0, 3, 2, 1);
        loginGrid.add(statusLabel, 0, 4, 2, 1);

        TitledPane loginPane = new TitledPane("Login", loginGrid);
        loginPane.setCollapsible(false); // Ensure the pane is not collapsible

        // Layout for query input section
        GridPane queryGrid = new GridPane();
        queryGrid.setVgap(10);
        queryGrid.setHgap(10);
        queryGrid.setPadding(new Insets(10));

        queryGrid.add(projectNameLabel, 0, 0);
        queryGrid.add(projectNameComboBox, 1, 0);
        queryGrid.add(cubeNameLabel, 0, 1);
        queryGrid.add(cubeNameComboBox, 1, 1);
        queryGrid.add(queryTypeLabel, 0, 2);
        queryGrid.add(queryTypeComboBox, 1, 2);
        queryGrid.add(queryLabel, 0, 3);
        queryGrid.add(queryTextArea, 1, 3, 2, 1);
        queryGrid.add(executeButton, 1, 4);

        TitledPane queryPane = new TitledPane("Query Input", queryGrid);
        queryPane.setCollapsible(false); // Ensure the pane is not collapsible

        // Layout for result display section
        VBox resultVBox = new VBox(10);
        resultVBox.getChildren().add(resultTableView);
        TitledPane resultPane = new TitledPane("Results", resultVBox);
        resultPane.setCollapsible(false); // Ensure the pane is not collapsible

        // Main layout
        VBox mainVBox = new VBox(20);
        mainVBox.getChildren().addAll(loginPane, queryPane, resultPane);

        // Create the scene and add it to the stage
        Scene scene = new Scene(mainVBox, 850, 800);
        primaryStage.setScene(scene);
        primaryStage.show();

        // Set button actions
        loginButton.setOnAction(e -> handleLogin());
        executeButton.setOnAction(e -> handleExecuteQuery());
    }

    private void configureSSL() {
        // Set the path to your custom truststore and its password
        System.setProperty("javax.net.ssl.trustStore", "/Volumes/4TB/Development/certificate/ubuntu-atscale/ubuntu-atscale.jks");
        System.setProperty("javax.net.ssl.trustStorePassword", "password");
    }

    private void handleLogin() {
        String username = usernameField.getText();
        String password = passwordField.getText();
        String hostname = hostnameField.getText();

        Task<Void> loginTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                try {
                    // Handle login and get JWT token
                    jwtToken = Authenticator.authenticate(username, password, hostname);

                    // Fetch and populate projects
                    ProjectFetcher projectFetcher = new ProjectFetcher(jwtToken, hostname);
                    List<String> projects = projectFetcher.fetchProjects();

                    // Update UI on the JavaFX Application Thread
                    Platform.runLater(() -> {
                        projectNameComboBox.getItems().setAll(projects);
                        statusLabel.setText("Status: Login successful");
                        executeButton.setDisable(false);
                    });
                } catch (Exception ex) {
                    Platform.runLater(() -> {
                        statusLabel.setText("Status: Error - " + ex.getMessage());
                        executeButton.setDisable(true);
                    });
                }
                return null;
            }
        };
        new Thread(loginTask).start();
    }

    private void handleProjectSelection() {
        String selectedProject = projectNameComboBox.getValue();
        if (selectedProject != null) {
            Task<Void> fetchCubesTask = new Task<>() {
                @Override
                protected Void call() throws Exception {
                    try {
                        ProjectFetcher projectFetcher = new ProjectFetcher(jwtToken, hostnameField.getText());
                        List<String> cubes = projectFetcher.fetchCubesForProject(selectedProject);

                        // Update UI on the JavaFX Application Thread
                        Platform.runLater(() -> {
                            cubeNameComboBox.getItems().setAll(cubes);
                        });
                    } catch (Exception ex) {
                        Platform.runLater(() -> {
                            statusLabel.setText("Status: Error fetching cubes - " + ex.getMessage());
                        });
                    }
                    return null;
                }
            };
            new Thread(fetchCubesTask).start();
        }
    }

    private void handleExecuteQuery() {
        String projectName = projectNameComboBox.getValue();
        String cubeName = cubeNameComboBox.getValue();
        String hostname = hostnameField.getText();
        String queryType = queryTypeComboBox.getValue();
        String query = queryTextArea.getText();

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
                resultTableView.getItems().clear();
                resultTableView.getItems().add(List.of("Error: " + getException().getMessage()));
            }
        };
        new Thread(executeQueryTask).start();
    }

    private void updateTableView(List<List<String>> rows) {
        resultTableView.getColumns().clear();
        resultTableView.getItems().clear();

        if (rows.isEmpty()) {
            return;
        }
        List<String> headers = rows.get(0);
        for (int i = 0; i < headers.size(); i++) {
            final int colIndex = i;
            TableColumn<List<String>, String> column = new TableColumn<>(headers.get(i));
            column.setCellValueFactory(cellData ->new SimpleStringProperty(cellData.getValue().get(colIndex)));
            resultTableView.getColumns().add(column);
            }
            for (int i = 1; i < rows.size(); i++) {
                resultTableView.getItems().add(rows.get(i));
            }
        }
        
        public static void main(String[] args) {
            launch(args);
        }
    }