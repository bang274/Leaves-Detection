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

    public MainFrame(ImageProcessor processor, ResultDisplay display) {
        this.imageProcessor = processor;
        this.resultDisplay = display;
        this.graphDisplay = new GraphDisplay(display, resultArea);
        initializeUI();
    }

    private void initializeUI() {
        setTitle("Leaf Detection App");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Image display
        imageLabel = new JLabel("No image selected", SwingConstants.CENTER);
        imageLabel.setPreferredSize(new Dimension(400, 300));
        add(new JScrollPane(imageLabel), BorderLayout.CENTER);

        // Result display
        resultArea = new JTextArea(10, 40);
        resultArea.setEditable(false);
        add(new JScrollPane(resultArea), BorderLayout.SOUTH);

        // Control panel
        JPanel controlPanel = new JPanel();
        JButton uploadButton = new JButton("Upload Image");
        JButton detectButton = new JButton("Run Detection");
        JButton graphButton = new JButton("Show Graph");
        detectButton.setEnabled(false);
        graphButton.setEnabled(false);

        // Graph panel (initially hidden)
        graphPanel = new JPanel(new BorderLayout());
        graphPanel.setVisible(false);

        uploadButton.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Images", "jpg", "png"));
            if (fileChooser.showOpenDialog(MainFrame.this) == JFileChooser.APPROVE_OPTION) {
                selectedImage = fileChooser.getSelectedFile();
                imageProcessor.loadImage(selectedImage);
                displayImage();
                detectButton.setEnabled(true);
                graphButton.setEnabled(graphDisplay.hasData());
            }
        });

        detectButton.addActionListener(e -> {
            try {
                String result = imageProcessor.runInference();
                resultDisplay.showResult(resultArea, result);
                graphDisplay.addLeafData(imageProcessor.getLeafData());
                graphButton.setEnabled(true);
                if (graphPanel.isVisible()) {
                    graphPanel.removeAll();
                    graphComponent = graphDisplay.createGraphComponent();
                    graphPanel.add(graphComponent, BorderLayout.CENTER);
                    graphPanel.revalidate();
                    graphPanel.repaint();
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
            } else {
                if (graphComponent == null || graphDisplay.isGraphUpdated()) {
                    graphComponent = graphDisplay.createGraphComponent();
                    graphPanel.removeAll();
                    graphPanel.add(graphComponent, BorderLayout.CENTER);
                }
                add(graphPanel, BorderLayout.EAST);
                graphPanel.setVisible(true);
                graphButton.setText("Hide Graph");
            }
            revalidate();
            repaint();
        });

        controlPanel.add(uploadButton);
        controlPanel.add(detectButton);
        controlPanel.add(graphButton);
        add(controlPanel, BorderLayout.NORTH);

        setVisible(true);
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
            // Read all lines, but expect JSON on the last non-empty line
            while ((line = reader.readLine()) != null) {
                if (!line.trim().isEmpty()) {
                    output.setLength(0); // Clear previous lines, keep only the last non-empty
                    output.append(line);
                }
                errorOutput.append(line).append("\n"); // Collect all for errors
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
        resultArea.setText(result);
    }
}

class GraphDisplay {
    private final ResultDisplay resultDisplay;
    private final JTextArea resultArea;
    private mxGraph graph;
    private Map<String, java.util.List<String>> leafFeatureMap;
    private Map<String, Object> leafNodes;
    private Map<String, Object> featureNodes; // Global map for feature nodes
    private boolean graphUpdated;

    public GraphDisplay(ResultDisplay display, JTextArea resultArea) {
        this.resultDisplay = display;
        this.resultArea = resultArea;
        this.graph = new mxGraph();
        this.leafFeatureMap = new HashMap<>();
        this.leafNodes = new HashMap<>();
        this.featureNodes = new HashMap<>(); // Single map for all feature nodes
        this.graphUpdated = false;
    }

    public void addLeafData(JSONObject leafData) {
        String leafType = leafData.getString("leaf_type");
        JSONArray featuresArray = leafData.getJSONArray("features");
        java.util.List<String> features = new ArrayList<>();
        for (int i = 0; i < featuresArray.length(); i++) {
            features.add(featuresArray.getString(i));
        }

        // Update feature map
        leafFeatureMap.compute(leafType, (k, v) -> {
            if (v == null) {
                return new ArrayList<>(features);
            } else {
                v.addAll(features);
                return new ArrayList<>(new LinkedHashSet<>(v)); // Remove duplicates
            }
        });

        // Mark graph as updated
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

            // Calculate position for new nodes
            int leafX = 100;
            int leafY = 100;
            int leafSpacing = 150;
            int maxY = leafY;

            // Determine the maximum Y position to place new nodes
            for (Object vertex : leafNodes.values()) {
                double y = graph.getModel().getGeometry(vertex).getY();
                double height = graph.getModel().getGeometry(vertex).getHeight();
                String leafType = (String) graph.getModel().getValue(vertex);
                int featureCount = leafFeatureMap.get(leafType).size();
                maxY = Math.max(maxY, (int) (y + height + featureCount * 50 + leafSpacing));
            }

            // Add or update the leaf node
            Object newLeafNode;
            if (!leafNodes.containsKey(newLeafType)) {
                newLeafNode = graph.insertVertex(parent, null, newLeafType, leafX, maxY, 80, 30);
                leafNodes.put(newLeafType, newLeafNode);
            } else {
                newLeafNode = leafNodes.get(newLeafType);
            }

            // Add or update feature nodes and connect to leaf node
            java.util.List<String> existingFeatures = leafFeatureMap.get(newLeafType);
            for (int i = 0; i < existingFeatures.size(); i++) {
                String feature = existingFeatures.get(i);
                Object featureNode;
                if (!featureNodes.containsKey(feature)) {
                    // Create new feature node if it doesn't exist
                    featureNode = graph.insertVertex(parent, null, feature, leafX + 150, maxY + i * 50, 100, 30);
                    featureNodes.put(feature, featureNode);
                } else {
                    // Reuse existing feature node
                    featureNode = featureNodes.get(feature);
                }
                // Ensure edge exists between leaf node and feature node (avoid duplicates)
                Object[] edges = graph.getEdgesBetween(newLeafNode, featureNode);
                if (edges.length == 0) {
                    graph.insertEdge(parent, null, "", newLeafNode, featureNode);
                }
            }

        } finally {
            graph.getModel().endUpdate();
            graphUpdated = false;
        }
    }

    public mxGraphComponent createGraphComponent() {
        mxGraphComponent graphComponent = new mxGraphComponent(graph);
        graphComponent.setConnectable(false);
        graphComponent.getGraphControl().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                Object cell = graphComponent.getCellAt(e.getX(), e.getY());
                if (cell != null) {
                    String label = (String) graph.getModel().getValue(cell);
                    if (leafNodes.containsKey(label)) {
                        java.util.List<String> features = leafFeatureMap.get(label);
                        resultDisplay.showResult(resultArea, "Leaf Type: " + label + "\nFeatures: " + String.join(", ", features));
                    } else {
                        resultDisplay.showResult(resultArea, "Feature: " + label);
                    }
                }
            }
        });
        return graphComponent;
    }
}