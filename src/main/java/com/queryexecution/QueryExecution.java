package com.queryexecution;

import javafx.application.Application;
import javafx.geometry.Insets;
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
    private QueryExecutionEventHandler eventHandler;

    @Override
    public void start(Stage primaryStage) {
        configureSSL();  // Handle any errors related to SSL configuration here
        eventHandler = new QueryExecutionEventHandler(this);

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
        projectNameComboBox.setOnAction(e -> {
            try {
                eventHandler.handleProjectSelection();
            } catch (Exception ex) {
                showError("Error selecting project: " + ex.getMessage());
            }
        });

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
        loginButton.setOnAction(e -> {
            try {
                eventHandler.handleLogin();
            } catch (Exception ex) {
                showError("Login failed: " + ex.getMessage());
            }
        });

        executeButton.setOnAction(e -> {
            try {
                eventHandler.handleExecuteQuery();
            } catch (Exception ex) {
                showError("Query execution failed: " + ex.getMessage());
            }
        });
    }

    public TextField getHostnameField() {
        return hostnameField;
    }

    public TextField getUsernameField() {
        return usernameField;
    }

    public PasswordField getPasswordField() {
        return passwordField;
    }

    public ComboBox<String> getProjectNameComboBox() {
        return projectNameComboBox;
    }

    public ComboBox<String> getCubeNameComboBox() {
        return cubeNameComboBox;
    }

    public ComboBox<String> getQueryTypeComboBox() {
        return queryTypeComboBox;
    }

    public TextArea getQueryTextArea() {
        return queryTextArea;
    }

    public TableView<List<String>> getResultTableView() {
        return resultTableView;
    }

    public Button getExecuteButton() {
        return executeButton;
    }

    public Button getLoginButton() {
        return loginButton;
    }

    public Label getStatusLabel() {
        return statusLabel;
    }

    public static void main(String[] args) {
        launch(args);
    }

    private void configureSSL() {
        try {
            // Set the path to your custom truststore and its password
            System.setProperty("javax.net.ssl.trustStore", "./cacerts");
            System.setProperty("javax.net.ssl.trustStorePassword", "password");
        } catch (Exception e) {
            showError("SSL configuration failed: " + e.getMessage());
        }
    }

    // Helper method to display error messages
    private void showError(String message) {
        statusLabel.setText("Error: " + message);
        statusLabel.setStyle("-fx-text-fill: red;"); // Change text color to red for errors
    }
}