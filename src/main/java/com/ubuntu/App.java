package com.ubuntu;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;

import java.util.List;

public class App extends Application {

    private TextField hostnameField;
    private TextField usernameField;
    private PasswordField passwordField;
    private ComboBox<String> projectNameComboBox;
    private ComboBox<String> cubeNameComboBox;
    private ComboBox<String> queryTypeComboBox;
    private TextArea queryTextArea;
    private TextArea resultTextArea;
    private Button executeButton;
    private Button loginButton;
    private Label statusLabel;
    private String jwtToken;

    @Override
    public void start(Stage primaryStage) {
        configureSSL();  // Configure SSL before starting the application

        primaryStage.setTitle("Query Executor");

        // Create UI elements
        Label hostnameLabel = new Label("Hostname:");
        hostnameField = new TextField();

        Label usernameLabel = new Label("Username:");
        usernameField = new TextField();

        Label passwordLabel = new Label("Password:");
        passwordField = new PasswordField();

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

        loginButton = new Button("Login");
        executeButton = new Button("Execute");
        executeButton.setDisable(true);  // Initially disabled

        resultTextArea = new TextArea();
        resultTextArea.setEditable(false);

        statusLabel = new Label("Status: Not logged in");

        // Layout
        GridPane grid = new GridPane();
        grid.setVgap(10);
        grid.setHgap(10);
        grid.add(hostnameLabel, 0, 0);
        grid.add(hostnameField, 1, 0);
        grid.add(usernameLabel, 0, 1);
        grid.add(usernameField, 1, 1);
        grid.add(passwordLabel, 0, 2);
        grid.add(passwordField, 1, 2);
        grid.add(loginButton, 0, 3, 2, 1);
        grid.add(statusLabel, 0, 4, 2, 1);
        grid.add(projectNameLabel, 0, 5);
        grid.add(projectNameComboBox, 1, 5);
        grid.add(cubeNameLabel, 0, 6);
        grid.add(cubeNameComboBox, 1, 6);
        grid.add(queryTypeLabel, 0, 7);
        grid.add(queryTypeComboBox, 1, 7);
        grid.add(queryLabel, 0, 8);
        grid.add(queryTextArea, 1, 8, 2, 1);
        grid.add(executeButton, 0, 9);
        grid.add(resultTextArea, 0, 10, 3, 1);

        // Create the scene and add it to the stage
        Scene scene = new Scene(grid, 800, 600);
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

        try {
            // Handle login and get JWT token
            jwtToken = Authenticator.authenticate(username, password, hostname);

            // Fetch and populate projects
            ProjectFetcher projectFetcher = new ProjectFetcher(jwtToken, hostname);
            List<String> projects = projectFetcher.fetchProjects();
            projectNameComboBox.getItems().setAll(projects);

            // Handle successful login
            statusLabel.setText("Status: Login successful");
            executeButton.setDisable(false);  // Enable the execute button
        } catch (Exception ex) {
            statusLabel.setText("Status: Error - " + ex.getMessage());
            executeButton.setDisable(true);  // Disable the execute button
        }
    }

    private void handleProjectSelection() {
        String selectedProject = projectNameComboBox.getValue();
        if (selectedProject != null) {
            try {
                ProjectFetcher projectFetcher = new ProjectFetcher(jwtToken, hostnameField.getText());
                List<String> cubes = projectFetcher.fetchCubesForProject(selectedProject);
                cubeNameComboBox.getItems().setAll(cubes);
            } catch (Exception ex) {
                statusLabel.setText("Status: Error fetching cubes - " + ex.getMessage());
            }
        }
    }

    private void handleExecuteQuery() {
        String projectName = projectNameComboBox.getValue();
        String cubeName = cubeNameComboBox.getValue();
        String hostname = hostnameField.getText();
        String queryType = queryTypeComboBox.getValue();
        String query = queryTextArea.getText();
    
        try {
            if ("Analytic".equals(queryType)) {
                AnalyticQueryExecutor.executeAnalyticQuery(jwtToken, projectName, cubeName, hostname, query);
                resultTextArea.setText("Analytic query executed successfully.");
            } else if ("SQL".equals(queryType)) {
                SQLQueryExecutor.executeSQLQuery(jwtToken, projectName, hostname, query);
                resultTextArea.setText("SQL query executed successfully.");
            } else {
                resultTextArea.setText("Invalid query type selected.");
            }
        } catch (Exception ex) {
            resultTextArea.setText("Error: " + ex.getMessage());
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}