package com.queryexecution;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class QueryDetailFetcher {

    private String hostname;
    private int port;

    public QueryDetailFetcher(String hostname, int port) {
        this.hostname = hostname;
        this.port = port;
    }

    public List<String> fetchQueryDetails(String queryID) throws Exception {
        List<String> details = new ArrayList<>();

        try (Socket socket = new Socket(hostname, port);
             PrintWriter output = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            // Send the query ID to the agent
            output.println(queryID);

            // Read the response from the agent
            String line;
            while ((line = input.readLine()) != null) {
                details.add(line);
            }
        }

        return details;
    }
}