package com.queryexecution;

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Callback;

import java.util.List;

public class QueryHistoryUI {

    private static String jwtToken;
    private static String hostname;
    private static String loginType;
    private static CheckBox httpFlag;

    private ComboBox<String> timeRangeDropdown;
    private TextField queryIdField;
    private Button retrieveQueryButton;
    private TreeTableView<QueryHistoryData> queryHistoryTable;
    private ListView<String> queryDetailsListView;
    private QueryHistoryTable queryHistoryTableHandler;
    private QueryDetailFetcher queryDetailFetcher;

    public QueryHistoryUI(Stage primaryStage, String jwtToken, String hostname, String loginType, CheckBox httpFlag) {
        QueryHistoryUI.jwtToken = jwtToken;
        QueryHistoryUI.hostname = hostname;
        QueryHistoryUI.loginType = loginType;
        QueryHistoryUI.httpFlag = httpFlag;

        if (httpFlag == null) {
            this.httpFlag = new CheckBox();
            this.httpFlag.setSelected(false); // Set default to false
        } else {
            this.httpFlag = httpFlag;
        }

        queryDetailFetcher = new QueryDetailFetcher(hostname, Config.QUERY_DETAIL_PORT); // Use the port from Config

        primaryStage.setTitle("Query History");

        // Create the dropdown
        Label timeRangeLabel = new Label("Time Range:");
        timeRangeDropdown = new ComboBox<>();
        timeRangeDropdown.getItems().addAll("5 Min", "30 Min", "QueryID");
        timeRangeDropdown.setOnAction(e -> handleDropdownSelection());

        // Create the QueryID entry field, disabled initially
        Label queryIdLabel = new Label("QueryID:");
        queryIdField = new TextField();
        queryIdField.setDisable(true);

        // Create the Retrieve Query button
        retrieveQueryButton = new Button("Retrieve Query");
        retrieveQueryButton.setOnAction(e -> handleRetrieveQuery()); // Set the event handler

        // Create the TableTreeView for Query History
        queryHistoryTableHandler = new QueryHistoryTable();
        queryHistoryTable = queryHistoryTableHandler.createQueryHistoryTable();

        // Add selection listener to queryHistoryTable
        queryHistoryTable.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                String queryID = newValue.getValue().getQueryId(); // Assuming getQueryId() returns the QueryID
                handleQuerySelection(queryID);
            }
        });

        // Create ListView for displaying detailed query information
        queryDetailsListView = new ListView<>();
        queryDetailsListView.setCellFactory(new Callback<ListView<String>, ListCell<String>>() {
            @Override
            public ListCell<String> call(ListView<String> param) {
                return new QueryDetailCell();
            }
        });

        // Layout configuration
        GridPane grid = new GridPane();
        grid.setVgap(10);
        grid.setHgap(10);
        grid.setPadding(new Insets(10));

        grid.add(timeRangeLabel, 0, 0);
        grid.add(timeRangeDropdown, 1, 0);
        grid.add(queryIdLabel, 0, 1);
        grid.add(queryIdField, 1, 1);
        grid.add(retrieveQueryButton, 1, 2);

        VBox layout = new VBox(10);
        layout.getChildren().addAll(grid, queryHistoryTable, queryDetailsListView);

        Scene scene = new Scene(layout, 800, 600);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    // Enable the QueryID entry field when 'QueryID' is selected in the dropdown
    private void handleDropdownSelection() {
        String selectedOption = timeRangeDropdown.getValue();
        if ("QueryID".equals(selectedOption)) {
            queryIdField.setDisable(false);
        } else {
            queryIdField.setDisable(true);
        }
    }

    // Handle the Retrieve Query button click event
    private void handleRetrieveQuery() {
        String selectedTimeRange = timeRangeDropdown.getValue();
        String queryId = queryIdField.getText();

        try {
            QueryHistoryFetcher fetcher = new QueryHistoryFetcher(jwtToken, hostname, loginType, httpFlag, selectedTimeRange, queryId);
            List<QueryHistoryData> queryHistoryData;

            // Fetch query history based on the login type
            if ("I".equals(loginType)) {
                queryHistoryData = fetcher.fetchQueryHistoryI();
            } else {
                queryHistoryData = fetcher.fetchQueryHistoryC();
            }

            // Populate the query history table with the fetched data
            queryHistoryTableHandler.populateTable(queryHistoryTable, queryHistoryData);

        } catch (Exception e) {
            e.printStackTrace();
            showErrorDialog("Error retrieving query history", e.getMessage());
        }
    }

    // Handle the selection of a query ID from the query history table
// Handle the selection of a query ID from the query history table
private void handleQuerySelection(String queryID) {
    try {
        List<String> queryDetails = queryDetailFetcher.fetchQueryDetails(queryID);
        queryDetailsListView.getItems().clear();
        queryDetailsListView.getItems().addAll(queryDetails);  // This will trigger the custom cell factory
    } catch (Exception e) {
        e.printStackTrace();
        showErrorDialog("Error retrieving query details", e.getMessage());
    }
}



    // Show error dialog in case of failure
    private void showErrorDialog(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}