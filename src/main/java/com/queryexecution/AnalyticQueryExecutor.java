package com.queryexecution;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;
import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.VBox;
import javax.xml.namespace.NamespaceContext;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;
import java.util.Iterator;

public class AnalyticQueryExecutor {

    public static String executeAnalyticQuery(String jwt, String projectName, String cubeName, String hostname, String query, String loginType) throws IOException {
        // Determine the URL endpoint based on loginType
        String urlStr;
        String message;
        if ("I".equals(loginType)) {
            urlStr = String.format("https://%s:10502/xmla/default", hostname);
        } else if ("C".equals(loginType)) {
            urlStr = String.format("https://%s/engine/xmla", hostname);
        } else {
            throw new IllegalArgumentException("Invalid loginType: " + loginType);
        }

        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Authorization", "Bearer " + jwt);
        conn.setRequestProperty("Content-Type", "application/xml");
        conn.setDoOutput(true);

        String xmlPayload = String.format(
            "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">" +
            "<soap:Body>" +
            "<Execute xmlns=\"urn:schemas-microsoft-com:xml-analysis\">" +
            "<Command>" +
            "<Statement><![CDATA[%s]]></Statement>" +
            "</Command>" +
            "<Properties>" +
            "<PropertyList>" +
            "<Catalog>%s</Catalog>" +
            "<Cube>%s</Cube>" +
            "</PropertyList>" +
            "</Properties>" +
            "</Execute>" +
            "</soap:Body>" +
            "</soap:Envelope>",
            query, projectName, cubeName
        );

        // Send the request
        try (OutputStream os = conn.getOutputStream()) {
            os.write(xmlPayload.getBytes());
            os.flush();
        }

        int responseCode = conn.getResponseCode();
        if (responseCode != 200) {
            // Read and log the response body for more details
            StringBuilder response = new StringBuilder();
            try (BufferedReader in = new BufferedReader(new InputStreamReader(conn.getErrorStream()))) {
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
            }
            throw new IOException("Analytic query execution failed: HTTP response code " + responseCode + " - Response body: " + response.toString());
        } else {
            // Read and return the response body
            StringBuilder response = new StringBuilder();
            try (BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
            }

            // Parse the XML response and format it
           // return parseAndFormatXML(response.toString());
    // Debug: Print the raw XML response for verification
    //String rawXmlResponse = response.toString();
    // System.out.println("Raw XML Response:\n" + rawXmlResponse);

       // Handle BOM if present
      // Save the response to a file
            try (PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter("/tmp/analytic_output.soap")))) {
                out.println(response.toString());
            }

            message = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                 "<Response>\n" +
                 "    <columns>\n" +
                 "        <column>\n" +
                 "            <name>Status</name>\n" +
                 "        </column>\n" +
                 "        <column>\n" +
                 "            <name>OutputFile</name>\n" +
                 "        </column>\n" +
                 "    </columns>\n" +
                 "    <rows>\n" +
                 "        <row>\n" +
                 "            <Status>Query Completed</Status>\n" +
                 "            <OutputFile>/tmp/analytic_output.soap</OutputFile>\n" +
                 "        </row>\n" +
                 "    </rows>\n" +
                 "</Response>";

        }
       
        return message;
    }

    private static String parseAndFormatXML(String xml) {
        StringBuilder result = new StringBuilder();

        try {
            // Set up the XML parser
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true); // Important for namespace handling
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new java.io.ByteArrayInputStream(xml.getBytes()));

            // Normalize the XML document
            doc.getDocumentElement().normalize();

            // Debug: Print the raw XML for verification
            System.out.println("Raw XML Response:\n" + xml);

            // Use XPath to query elements, considering namespaces
            XPathFactory xPathFactory = XPathFactory.newInstance();
            XPath xpath = xPathFactory.newXPath();

            // Define namespaces
            NamespaceContext nsContext = new NamespaceContext() {
                @Override
                public String getNamespaceURI(String prefix) {
                    switch (prefix) {
                        case "soap":
                            return "http://schemas.xmlsoap.org/soap/envelope/";
                        case "msxmla":
                            return "urn:schemas-microsoft-com:xml-analysis";
                        case "mddataset":
                            return "urn:schemas-microsoft-com:xml-analysis:mddataset";
                        case "as":
                            return "http://xsd.atscale.com/";
                        default:
                            return null;
                    }
                }

                @Override
                public Iterator<String> getPrefixes(String namespaceURI) {
                    return null;
                }

                @Override
                public String getPrefix(String namespaceURI) {
                    return null;
                }
            };

            xpath.setNamespaceContext(nsContext);

            // Construct XPath expression based on the expected XML structure
            String expression = "//mddataset:root//mddataset:Axis//mddataset:Tuple//mddataset:Member/mddataset:Caption";
            XPathExpression expr = xpath.compile(expression);
            NodeList nodes = (NodeList) expr.evaluate(doc, XPathConstants.NODESET);

            // Print nodes
            for (int i = 0; i < nodes.getLength(); i++) {
                Node node = nodes.item(i);
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    Element element = (Element) node;
                    result.append("Node Name: ").append(element.getNodeName()).append("\n");
                    result.append("Value: ").append(element.getTextContent()).append("\n");
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            return "Error parsing XML: " + e.getMessage();
        }

        return result.toString();
    }

    }