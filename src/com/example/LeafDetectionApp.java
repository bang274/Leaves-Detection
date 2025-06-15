package com.example;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.nio.file.*;
import com.mxgraph.swing.mxGraphComponent;
import com.mxgraph.view.mxGraph;
import org.json.JSONObject;
import org.json.JSONArray;
import java.util.*;
import com.mxgraph.util.mxConstants;

public class LeafDetectionApp {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            ImageProcessor processor = new ImageProcessor();
            ResultDisplay display = new ResultDisplay();
            new MainFrame(processor, display);
        });
    }
}

class MainFrame extends JFrame {
    private final ImageProcessor imageProcessor;
    private final ResultDisplay resultDisplay;
    private JLabel imageLabel;
    private JTextArea resultArea;
    private File selectedImage;
    private JPanel graphPanel;
    private mxGraphComponent graphComponent;
    private GraphDisplay graphDisplay;
    private JButton edgeToggleButton; // New button for toggling edges

    public MainFrame(ImageProcessor processor, ResultDisplay display) {
        this.imageProcessor = processor;
        this.resultDisplay = display;
        this.graphDisplay = new GraphDisplay(display, resultArea);
        initializeUI();
    }

    private void initializeUI() {
        setTitle("Leaf Detection Application");
        setSize(1800, 1000);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));
        getContentPane().setBackground(new Color(245, 245, 245));

        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        mainPanel.setBackground(new Color(245, 245, 245));

        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        controlPanel.setBackground(new Color(220, 220, 220));
        controlPanel.setBorder(BorderFactory.createTitledBorder("Controls"));

        JButton uploadButton = createStyledButton("Upload Image");
        JButton detectButton = createStyledButton("Run Detection");
        JButton graphButton = createStyledButton("Show Graph");
        edgeToggleButton = createStyledButton("Hide Edges"); // Initialize new button
        detectButton.setEnabled(false);
        graphButton.setEnabled(false);
        edgeToggleButton.setEnabled(false); // Disabled until graph is shown

        // Graph panel
        graphPanel = new JPanel(new BorderLayout());
        graphPanel.setBorder(BorderFactory.createTitledBorder("Feature Graph"));
        graphPanel.setPreferredSize(new Dimension(1200, 0));
        graphPanel.setVisible(false);

        // Image display with border
        imageLabel = new JLabel("No image selected", SwingConstants.CENTER);
        imageLabel.setPreferredSize(new Dimension(400, 300));
        imageLabel.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        JPanel imagePanel = new JPanel(new BorderLayout());
        imagePanel.setBorder(BorderFactory.createTitledBorder("Image Preview"));
        imagePanel.add(new JScrollPane(imageLabel), BorderLayout.CENTER);

        // Result display with modern styling
        resultArea = new JTextArea(10, 40);
        resultArea.setEditable(false);
        resultArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
        resultArea.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        JPanel resultPanel = new JPanel(new BorderLayout());
        resultPanel.setBorder(BorderFactory.createTitledBorder("Detection Results"));
        resultPanel.add(new JScrollPane(resultArea), BorderLayout.CENTER);

        // Button actions
        uploadButton.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Images", "jpg", "png"));
            if (fileChooser.showOpenDialog(MainFrame.this) == JFileChooser.APPROVE_OPTION) {
                selectedImage = fileChooser.getSelectedFile();
                imageProcessor.loadImage(selectedImage);
                displayImage();
                detectButton.setEnabled(true);
                graphButton.setEnabled(graphDisplay.hasData());
                edgeToggleButton.setEnabled(graphPanel.isVisible() && graphDisplay.hasData());
            }
        });

        detectButton.addActionListener(e -> {
            try {
                String result = imageProcessor.runInference();
                resultDisplay.showResult(resultArea, result);
                graphDisplay.addLeafData(imageProcessor.getLeafData());
                graphButton.setEnabled(true);
                edgeToggleButton.setEnabled(graphPanel.isVisible() && graphDisplay.hasData());
                if (graphPanel.isVisible()) {
                    updateGraphPanel();
                }
            } catch (Exception ex) {
                resultDisplay.showResult(resultArea, "Error: " + ex.getMessage());
            }
        });

        graphButton.addActionListener(e -> {
            if (graphPanel.isVisible()) {
                graphPanel.setVisible(false);
                remove(graphPanel);
                graphButton.setText("Show Graph");
                edgeToggleButton.setEnabled(false);
            } else {
                updateGraphPanel();
                add(graphPanel, BorderLayout.EAST);
                graphPanel.setVisible(true);
                graphButton.setText("Hide Graph");
                edgeToggleButton.setEnabled(graphDisplay.hasData());
            }
            revalidate();
            repaint();
        });

        edgeToggleButton.addActionListener(e -> {
            boolean edgesVisible = graphDisplay.toggleEdges();
            edgeToggleButton.setText(edgesVisible ? "Hide Edges" : "Show Edges");
            graphPanel.revalidate();
            graphPanel.repaint();
        });

        controlPanel.add(uploadButton);
        controlPanel.add(detectButton);
        controlPanel.add(graphButton);
        controlPanel.add(edgeToggleButton); // Add new button to control panel

        mainPanel.add(controlPanel, BorderLayout.NORTH);
        mainPanel.add(imagePanel, BorderLayout.CENTER);
        mainPanel.add(resultPanel, BorderLayout.SOUTH);
        add(mainPanel, BorderLayout.CENTER);

        setLocationRelativeTo(null);
        setVisible(true);
    }

    private JButton createStyledButton(String text) {
        JButton button = new JButton(text);
        button.setFont(new Font("Arial", Font.BOLD, 14));
        button.setBackground(new Color(100, 150, 200));
        button.setForeground(Color.BLACK);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Color.BLACK, 2),
            BorderFactory.createEmptyBorder(8, 15, 8, 15)
        ));
        return button;
    }

    private void updateGraphPanel() {
        if (graphComponent == null || graphDisplay.isGraphUpdated()) {
            graphComponent = graphDisplay.createGraphComponent();
            graphPanel.removeAll();
            graphPanel.add(graphComponent, BorderLayout.CENTER);
        }
        edgeToggleButton.setEnabled(graphDisplay.hasData());
        graphPanel.revalidate();
        graphPanel.repaint();
    }

    private void displayImage() {
        if (imageProcessor.getImage() != null) {
            Image scaledImage = imageProcessor.getImage().getScaledInstance(400, 300, Image.SCALE_SMOOTH);
            imageLabel.setIcon(new ImageIcon(scaledImage));
            imageLabel.setText("");
        }
    }
}
class ImageProcessor {
    private Image image;
    private File imageFile;
    private JSONObject leafData;

    public void loadImage(File file) {
        try {
            this.imageFile = file;
            this.image = new ImageIcon(file.getAbsolutePath()).getImage();
        } catch (Exception e) {
            throw new RuntimeException("Failed to load image: " + e.getMessage());
        }
    }

    public String runInference() throws IOException, InterruptedException {
        if (imageFile == null) {
            throw new IllegalStateException("No image loaded");
        }

        String pythonCommand = "python";
        String scriptPath = "infer.py";
        ProcessBuilder pb = new ProcessBuilder(pythonCommand, scriptPath, imageFile.getAbsolutePath());
        pb.redirectErrorStream(true);
        Process process = pb.start();

        StringBuilder output = new StringBuilder();
        StringBuilder errorOutput = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.trim().isEmpty()) {
                    output.setLength(0);
                    output.append(line);
                }
                errorOutput.append(line).append("\n");
            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new IOException("Inference script failed with exit code " + exitCode + "\nCommand: " +
                String.join(" ", pb.command()) + "\nOutput: " + errorOutput.toString());
        }

        try {
            String jsonOutput = output.toString().trim();
            if (jsonOutput.isEmpty()) {
                throw new IOException("Inference script produced no output");
            }
            leafData = new JSONObject(jsonOutput);
        } catch (Exception e) {
            throw new IOException("Failed to parse inference output as JSON: " + e.getMessage() +
                "\nOutput: " + output.toString());
        }

        return output.toString();
    }

    public Image getImage() {
        return image;
    }

    public JSONObject getLeafData() {
        return leafData;
    }
}

class ResultDisplay {
    public void showResult(JTextArea resultArea, String result) {
        // Parse JSON result for formatted display
        try {
            JSONObject jsonResult = new JSONObject(result);
            String leafType = jsonResult.getString("leaf_type");
            JSONArray featuresArray = jsonResult.getJSONArray("features");
            StringBuilder formattedResult = new StringBuilder();
            formattedResult.append("=== Detection Result ===\n");
            formattedResult.append("Leaf Type: ").append(leafType).append("\n");
            formattedResult.append("Features:\n");
            for (int i = 0; i < featuresArray.length(); i++) {
                formattedResult.append("  - ").append(featuresArray.getString(i)).append("\n");
            }
            resultArea.setText(formattedResult.toString());
        } catch (Exception e) {
            resultArea.setText("Error parsing result: " + e.getMessage());
        }
    }

    public void showSingleLeafResult(JTextArea resultArea, String leafType, java.util.List<String> features, String similarLeaf, int commonCount) {
        StringBuilder formattedResult = new StringBuilder();
        formattedResult.append("=== Single Leaf Selection ===\n");
        formattedResult.append("Selected Leaf: ").append(leafType).append("\n");
        formattedResult.append("Features:\n");
        for (String feature : features) {
            formattedResult.append("  - ").append(feature).append("\n");
        }
        if (similarLeaf != null && commonCount > 0) {
            formattedResult.append("Most Similar Leaf: ").append(similarLeaf)
                           .append(" (").append(commonCount).append(" common features)\n");
        }
        resultArea.setText(formattedResult.toString());
    }

    public void showMultipleLeafResult(JTextArea resultArea, Set<String> commonFeatures) {
        StringBuilder formattedResult = new StringBuilder();
        formattedResult.append("=== Multiple Leaf Selection ===\n");
        formattedResult.append("Common Features:\n");
        if (commonFeatures.isEmpty()) {
            formattedResult.append("  - None\n");
        } else {
            for (String feature : commonFeatures) {
                formattedResult.append("  - ").append(feature).append("\n");
            }
        }
        resultArea.setText(formattedResult.toString());
    }

    public void showFeatureResult(JTextArea resultArea, Set<String> selectedFeatures, Set<String> matchingLeaves) {
        StringBuilder formattedResult = new StringBuilder();
        formattedResult.append("=== Feature Selection ===\n");
        formattedResult.append("Selected Features:\n");
        for (String feature : selectedFeatures) {
            formattedResult.append("  - ").append(feature).append("\n");
        }
        formattedResult.append("Matching Leaves:\n");
        if (matchingLeaves.isEmpty()) {
            formattedResult.append("  - None\n");
        } else {
            for (String leaf : matchingLeaves) {
                formattedResult.append("  - ").append(leaf).append("\n");
            }
        }
        resultArea.setText(formattedResult.toString());
    }
}

class GraphDisplay {
    private final ResultDisplay resultDisplay;
    private final JTextArea resultArea;
    private mxGraph graph;
    private Map<String, java.util.List<String>> leafFeatureMap;
    private Map<String, Object> leafNodes;
    private Map<String, Object> featureNodes;
    private boolean graphUpdated;
    private Set<Object> selectedCells;
    private boolean edgesVisible; // Track edge visibility state
    private Map<String, java.util.List<Object>> edgeMap; // Store edges for restoration

    public GraphDisplay(ResultDisplay display, JTextArea resultArea) {
        this.resultDisplay = display;
        this.resultArea = resultArea;
        this.graph = new mxGraph();
        this.leafFeatureMap = new HashMap<>();
        this.leafNodes = new HashMap<>();
        this.featureNodes = new HashMap<>();
        this.selectedCells = new HashSet<>();
        this.graphUpdated = false;
        this.edgesVisible = true; // Edges are visible by default
        this.edgeMap = new HashMap<>(); // Initialize edge storage
    }

    public void addLeafData(JSONObject leafData) {
        String leafType = leafData.getString("leaf_type");
        JSONArray featuresArray = leafData.getJSONArray("features");
        java.util.List<String> features = new ArrayList<>();
        for (int i = 0; i < featuresArray.length(); i++) {
            features.add(featuresArray.getString(i));
        }

        leafFeatureMap.compute(leafType, (k, v) -> {
            if (v == null) {
                return new ArrayList<>(features);
            } else {
                v.addAll(features);
                return new ArrayList<>(new LinkedHashSet<>(v));
            }
        });

        graphUpdated = true;
        updateGraph(leafType, features);
    }

    public boolean hasData() {
        return !leafFeatureMap.isEmpty();
    }

    public boolean isGraphUpdated() {
        return graphUpdated;
    }

    private void updateGraph(String newLeafType, java.util.List<String> newFeatures) {
        graph.getModel().beginUpdate();
        try {
            Object parent = graph.getDefaultParent();
            int originX = 600;
            int originY = 500;
            int leafRadius = 400;
            int featureRadius = 250;
            int numberOfLeaves = leafNodes.size() + (leafNodes.containsKey(newLeafType) ? 0 : 1);
            int leafIndex = leafNodes.size();

            java.util.List<Double> xPositions = new java.util.ArrayList<>();
            java.util.List<Double> yPositions = new java.util.ArrayList<>();
            for (double i = -1.0; i <= 1.0; i += 0.2) {
                xPositions.add(Math.round(i * 10.0) / 10.0);
                yPositions.add(Math.round(i * 10.0) / 10.0);
            }
            java.util.Collections.shuffle(xPositions);
            java.util.Collections.shuffle(yPositions);

            Object newLeafNode;
            if (!leafNodes.containsKey(newLeafType)) {
                int width = Math.max(80, newLeafType.length() * 8);
                double angle = Math.toRadians(360.0 / 7.0 * leafIndex + 180);
                int leafX = (int) (originX + leafRadius * Math.cos(angle));
                int leafY = (int) (originY + leafRadius * Math.sin(angle));
                newLeafNode = graph.insertVertex(parent, null, newLeafType, leafX, leafY, width, 30,
                    "fillColor=#ADD8E6;strokeColor=#4682B4;fontColor=black;fontSize=12");
                leafNodes.put(newLeafType, newLeafNode);
            } else {
                newLeafNode = leafNodes.get(newLeafType);
            }

            // Store edges for this leaf
            java.util.List<Object> leafEdges = edgeMap.computeIfAbsent(newLeafType, k -> new ArrayList<>());
            java.util.List<String> existingFeatures = leafFeatureMap.get(newLeafType);
            for (int i = 0; i < existingFeatures.size(); i++) {
                String feature = existingFeatures.get(i);
                Object featureNode;
                if (!featureNodes.containsKey(feature)) {
                    int width = Math.max(100, feature.length() * 8);
                    double xPos = xPositions.isEmpty() ? 0.0 : xPositions.remove(0);
                    double yPos = yPositions.isEmpty() ? 0.0 : yPositions.remove(0);
                    int featureX = (int) (originX + featureRadius * (xPos + Math.random() * 0.01));
                    int featureY = (int) (originY + featureRadius * (yPos + Math.random() * 0.01));
                    featureNode = graph.insertVertex(parent, null, feature, featureX, featureY, width, 30,
                        "fillColor=#98FB98;strokeColor=#228B22;fontColor=black;fontSize=12");
                    featureNodes.put(feature, featureNode);
                } else {
                    featureNode = featureNodes.get(feature);
                }
                Object[] edges = graph.getEdgesBetween(newLeafNode, featureNode);
                if (edges.length == 0 && edgesVisible) {
                    Object edge = graph.insertEdge(parent, null, "", newLeafNode, featureNode);
                    leafEdges.add(edge);
                }
            }

        } finally {
            graph.getModel().endUpdate();
            graphUpdated = false;
        }
    }

    public boolean toggleEdges() {
        graph.getModel().beginUpdate();
        try {
            if (edgesVisible) {
                // Hide edges by removing them
                for (Object edge : graph.getChildEdges(graph.getDefaultParent())) {
                    graph.getModel().remove(edge);
                }
                edgesVisible = false;
            } else {
                // Show edges by re-adding them
                Object parent = graph.getDefaultParent();
                for (Map.Entry<String, java.util.List<Object>> entry : edgeMap.entrySet()) {
                    String leafType = entry.getKey();
                    Object leafNode = leafNodes.get(leafType);
                    java.util.List<String> features = leafFeatureMap.get(leafType);
                    for (String feature : features) {
                        Object featureNode = featureNodes.get(feature);
                        if (leafNode != null && featureNode != null) {
                            Object edge = graph.insertEdge(parent, null, "", leafNode, featureNode);
                            entry.getValue().add(edge);
                        }
                    }
                }
                edgesVisible = true;
            }
        } finally {
            graph.getModel().endUpdate();
        }
        return edgesVisible;
    }

    public mxGraphComponent createGraphComponent() {
        mxGraphComponent graphComponent = new mxGraphComponent(graph);
        graphComponent.setConnectable(false);
        graphComponent.getGraphControl().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                Object cell = graphComponent.getCellAt(e.getX(), e.getY());
                if (cell != null) {
                    handleCellClick(cell, e, graphComponent);
                }
            }
        });
        return graphComponent;
    }

    private void handleCellClick(Object cell, MouseEvent e, mxGraphComponent graphComponent) {
        graph.getModel().beginUpdate();
        try {
            resetHighlights();
            String label = (String) graph.getModel().getValue(cell);
            if (leafNodes.containsKey(label)) {
                if (e.isControlDown()) {
                    selectedCells.add(cell);
                } else {
                    selectedCells.clear();
                    selectedCells.add(cell);
                }

                if (selectedCells.size() == 1) {
                    java.util.List<String> features = leafFeatureMap.get(label);
                    highlightFeatures(features);
                    showSimilarLeaves(label, features);
                    resultDisplay.showResult(resultArea, "Leaf Type: " + label + "\nFeatures: " + String.join(", ", features));
                } else {
                    Set<String> commonFeatures = getCommonFeatures();
                    highlightFeatures(commonFeatures);
                    resultDisplay.showResult(resultArea, "Common Features: " +
                        (commonFeatures.isEmpty() ? "None" : String.join(", ", commonFeatures)));
                }
            } else if (featureNodes.containsKey(label)) {
                if (e.isControlDown()) {
                    selectedCells.add(cell);
                } else {
                    selectedCells.clear();
                    selectedCells.add(cell);
                }

                Set<String> selectedFeatures = new HashSet<>();
                for (Object selectedCell : selectedCells) {
                    String featureLabel = (String) graph.getModel().getValue(selectedCell);
                    if (featureNodes.containsKey(featureLabel)) {
                        selectedFeatures.add(featureLabel);
                    }
                }

                highlightFeatures(selectedFeatures);
                showLeavesWithFeatures(selectedFeatures);
            }
        } finally {
            graph.getModel().endUpdate();
            graphComponent.refresh();
        }
    }

    private void resetHighlights() {
        for (Object node : leafNodes.values()) {
            graph.setCellStyles(mxConstants.STYLE_FILLCOLOR, "#ADD8E6", new Object[]{node});
            graph.setCellStyles(mxConstants.STYLE_STROKECOLOR, "#4682B4", new Object[]{node});
        }
        for (Object node : featureNodes.values()) {
            graph.setCellStyles(mxConstants.STYLE_FILLCOLOR, "#98FB98", new Object[]{node});
            graph.setCellStyles(mxConstants.STYLE_STROKECOLOR, "#228B22", new Object[]{node});
        }
    }

    private void highlightFeatures(Collection<String> features) {
        for (String feature : features) {
            Object node = featureNodes.get(feature);
            if (node != null) {
                graph.setCellStyles(mxConstants.STYLE_FILLCOLOR, "#FFFFE0", new Object[]{node});
                graph.setCellStyles(mxConstants.STYLE_STROKECOLOR, "#FFD700", new Object[]{node});
            }
        }
    }

    private void highlightLeaves(Collection<String> leaves) {
        for (String leaf : leaves) {
            Object node = leafNodes.get(leaf);
            if (node != null) {
                graph.setCellStyles(mxConstants.STYLE_FILLCOLOR, "#FFFFE0", new Object[]{node});
                graph.setCellStyles(mxConstants.STYLE_STROKECOLOR, "#FFD700", new Object[]{node});
            }
        }
    }

    private void showSimilarLeaves(String leafType, java.util.List<String> features) {
        Map<String, Integer> similarityScores = new HashMap<>();
        for (Map.Entry<String, java.util.List<String>> entry : leafFeatureMap.entrySet()) {
            if (!entry.getKey().equals(leafType)) {
                Set<String> otherFeatures = new HashSet<>(entry.getValue());
                Set<String> common = new HashSet<>(features);
                common.retainAll(otherFeatures);
                similarityScores.put(entry.getKey(), common.size());
            }
        }

        String mostSimilarLeaf = null;
        int maxCommon = -1;
        for (Map.Entry<String, Integer> entry : similarityScores.entrySet()) {
            if (entry.getValue() > maxCommon) {
                maxCommon = entry.getValue();
                mostSimilarLeaf = entry.getKey();
            }
        }

        if (mostSimilarLeaf != null && maxCommon > 0) {
            resultArea.append("\nMost similar leaf: " + mostSimilarLeaf + " (" + maxCommon + " common features)");
            highlightLeaves(Collections.singletonList(mostSimilarLeaf));
        }
    }

    private Set<String> getCommonFeatures() {
        Set<String> commonFeatures = null;
        for (Object cell : selectedCells) {
            String leafType = (String) graph.getModel().getValue(cell);
            if (leafNodes.containsKey(leafType)) {
                Set<String> features = new HashSet<>(leafFeatureMap.get(leafType));
                if (commonFeatures == null) {
                    commonFeatures = features;
                } else {
                    commonFeatures.retainAll(features);
                }
            }
        }
        return commonFeatures != null ? commonFeatures : new HashSet<>();
    }

    private void showLeavesWithFeatures(Set<String> selectedFeatures) {
        Set<String> matchingLeaves = new HashSet<>();
        for (Map.Entry<String, java.util.List<String>> entry : leafFeatureMap.entrySet()) {
            if (entry.getValue().containsAll(selectedFeatures)) {
                matchingLeaves.add(entry.getKey());
            }
        }

        highlightLeaves(matchingLeaves);
        resultDisplay.showResult(resultArea, "Leaves containing features [" + String.join(", ", selectedFeatures) + "]:\n" +
            (matchingLeaves.isEmpty() ? "None" : String.join(", ", matchingLeaves)));
    }
}