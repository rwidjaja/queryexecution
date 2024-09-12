package com.queryexecution;

import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.util.Callback;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import java.util.regex.Matcher;

public class QueryDetailCell extends ListCell<String> {

    @Override
    protected void updateItem(String item, boolean empty) {
        super.updateItem(item, empty);

        if (empty || item == null) {
            setText(null);
            setGraphic(null);
        } else {
            TextFlow textFlow = new TextFlow();
            applyPatternHighlight(item, textFlow);  // Highlight based on patterns
            setGraphic(textFlow);
        }
    }

    private void applyPatternHighlight(String text, TextFlow textFlow) {
        // Define all the patterns to match specific data
        Pattern[] patterns = {
            Pattern.compile("user=([^,]+)"),  // Capture user
            Pattern.compile("queryId=([^,]+)"),  // Capture queryId
            Pattern.compile("resultSize = (\\d+)"),  // For resultSize, if present
            Pattern.compile("usedLocalCache = (\\w+)"),  // For local cache usage, if present
            Pattern.compile("usedAggregateCache = (\\w+)"),  // For aggregate cache usage, if present
            Pattern.compile("connectionId = ([^,]+)"),  // For connectionId
            Pattern.compile("time = ([\\d.]+ ms)"),  // For time
            Pattern.compile("completed = ([^,]+)"),  // For completion status
            Pattern.compile("SubqueryExecFinishedEvent\\(([^,]+),"),  // For subquery execution event
            Pattern.compile("Agg estimate was InvalidEstimate.*FullEstimate\\(([^)]+)\\)"),  // For estimate
            Pattern.compile("Error when running query"),  // For error
            Pattern.compile("Aggregate request rejected because of:.*rejecting duplicate of instance \\[([^\\]]+)\\]"),  // For duplicate instance rejection
            Pattern.compile("AggregatePredictor.*was rejected by rule ([^\\.]+)\\. Reason: (.+)"),  // For predictor rejection rule and reason
            Pattern.compile("PlanningError: (.+)"),  // For planning error
            Pattern.compile("projectId=([^,]+)"),  // Capture projectId
            Pattern.compile("cubeId=([^,]+)")  // Capture cubeId
        };
    
        // To keep track of the last processed position in the text
        int lastEnd = 0;
    
        // Iterate through all patterns and search for matches
        while (lastEnd < text.length()) {
            int nearestMatchStart = text.length();
            int nearestMatchEnd = text.length();
            Matcher nearestMatcher = null;
            String matchedText = "";
    
            // Find the nearest match across all patterns
            for (Pattern pattern : patterns) {
                Matcher matcher = pattern.matcher(text);
                if (matcher.find(lastEnd)) {
                    if (matcher.start() < nearestMatchStart) {
                        nearestMatchStart = matcher.start();
                        nearestMatchEnd = matcher.end();
                        nearestMatcher = matcher;
                        matchedText = matcher.group();
                    }
                }
            }
    
            // If a match is found
            if (nearestMatcher != null) {
                // Add any text before the match as normal
                if (nearestMatchStart > lastEnd) {
                    textFlow.getChildren().add(new Text(text.substring(lastEnd, nearestMatchStart)));
                }
    
                // Highlight the matched text
                Text highlightedText = new Text(matchedText);
                highlightedText.setFill(Color.RED); // Change highlight color as needed
                textFlow.getChildren().add(highlightedText);
    
                // Move the lastEnd pointer to the end of the matched region
                lastEnd = nearestMatchEnd;
            } else {
                // No more matches, add the rest of the text
                textFlow.getChildren().add(new Text(text.substring(lastEnd)));
                break;
            }
        }
    }
}