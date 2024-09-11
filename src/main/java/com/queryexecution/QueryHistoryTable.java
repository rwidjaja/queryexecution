package com.queryexecution;

import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableView;
import javafx.scene.control.cell.TreeItemPropertyValueFactory;
import java.util.List;

public class QueryHistoryTable {

    public TreeTableView<QueryHistoryData> createQueryHistoryTable() {
        TreeTableView<QueryHistoryData> table = new TreeTableView<>();

        TreeTableColumn<QueryHistoryData, String> queryIdColumn = new TreeTableColumn<>("Query ID");
        queryIdColumn.setCellValueFactory(new TreeItemPropertyValueFactory<>("queryId"));

        TreeTableColumn<QueryHistoryData, String> userIdColumn = new TreeTableColumn<>("User ID");
        userIdColumn.setCellValueFactory(new TreeItemPropertyValueFactory<>("userId"));

        TreeTableColumn<QueryHistoryData, String> projectNameColumn = new TreeTableColumn<>("Project Name");
        projectNameColumn.setCellValueFactory(new TreeItemPropertyValueFactory<>("projectName"));

        TreeTableColumn<QueryHistoryData, String> cubeNameColumn = new TreeTableColumn<>("Cube Name");
        cubeNameColumn.setCellValueFactory(new TreeItemPropertyValueFactory<>("cubeName"));

        TreeTableColumn<QueryHistoryData, String> queryTextColumn = new TreeTableColumn<>("Query Text");
        queryTextColumn.setCellValueFactory(new TreeItemPropertyValueFactory<>("queryText"));

        TreeTableColumn<QueryHistoryData, String> queryLanguageColumn = new TreeTableColumn<>("Query Language");
        queryLanguageColumn.setCellValueFactory(new TreeItemPropertyValueFactory<>("queryLanguage"));

        TreeTableColumn<QueryHistoryData, String> queryStartColumn = new TreeTableColumn<>("Query Start");
        queryStartColumn.setCellValueFactory(new TreeItemPropertyValueFactory<>("queryStart"));

        TreeTableColumn<QueryHistoryData, String> durationsColumn = new TreeTableColumn<>("Durations");
        durationsColumn.setCellValueFactory(new TreeItemPropertyValueFactory<>("durations"));

        TreeTableColumn<QueryHistoryData, String> statusColumn = new TreeTableColumn<>("Status");
        statusColumn.setCellValueFactory(new TreeItemPropertyValueFactory<>("status"));

        table.getColumns().add(queryIdColumn);
        table.getColumns().add(userIdColumn);
        table.getColumns().add(projectNameColumn);
        table.getColumns().add(cubeNameColumn);
        table.getColumns().add(queryTextColumn);
        table.getColumns().add(queryLanguageColumn);
        table.getColumns().add(queryStartColumn);
        table.getColumns().add(durationsColumn);
        table.getColumns().add(statusColumn);

        // Root item to be added later when populating the table
        TreeItem<QueryHistoryData> rootItem = new TreeItem<>(new QueryHistoryData("Root", "", "", "", "", "", "", "", ""));
        table.setRoot(rootItem);
        table.setShowRoot(false); // Hide the root element

        return table;
    }

    public void populateTable(TreeTableView<QueryHistoryData> table, List<QueryHistoryData> data) {
        TreeItem<QueryHistoryData> rootItem = table.getRoot();
        rootItem.getChildren().clear();

        for (QueryHistoryData item : data) {
            TreeItem<QueryHistoryData> treeItem = new TreeItem<>(item);
            rootItem.getChildren().add(treeItem);
        }
    }
}