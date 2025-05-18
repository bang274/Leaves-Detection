import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;

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
    private final ImageProcessor imageProcessors;
    private final ResultDisplay resultDisplay;
    private JLabel imageLabel;
    private JTextArea resultArea;
    private File selectedImage;

    public MainFrame(ImageProcessor processor, ResultDisplay display) {
        this.imageProcessors = processor;
        this.resultDisplay = display;
        initializeUI();
    }

    private void initializeUI() {
        setTitle("Leaf Detection App");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        imageLabel = new JLabel("No image selected", SwingConstants.CENTER);
        imageLabel.setPreferredSize(new Dimension(400, 300));
        add(new JScrollPane(imageLabel), BorderLayout.CENTER);

        resultArea = new JTextArea(10, 40);
        resultArea.setEditable(false);
        add(new JScrollPane(resultArea), BorderLayout.SOUTH);

        JPanel controlPanel = new JPanel();
        JButton uploadButton = new JButton("Upload Image");
        JButton detectButton = new JButton("Run Detection");
        detectButton.setEnabled(false);

        uploadButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser fileChooser = new JFileChooser();
                fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Images", "jpg", "png"));
                if (fileChooser.showOpenDialog(MainFrame.this) == JFileChooser.APPROVE_OPTION) {
                    selectedImage = fileChooser.getSelectedFile();
                    imageProcessors.loadImage(selectedImage);
                    displayImage();
                    detectButton.setEnabled(true);
                }
            }
        });

        detectButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    String result = imageProcessors.runInference();
                    resultDisplay.showResult(resultArea, result);
                } catch (Exception ex) {
                    resultDisplay.showResult(resultArea, "Error: " + ex.getMessage());
                }
            }
        });

        controlPanel.add(uploadButton);
        controlPanel.add(detectButton);
        add(controlPanel, BorderLayout.NORTH);

        setVisible(true);
    }

    private void displayImage() {
        if (imageProcessors.getImage() != null) {
            Image scaledImage = imageProcessors.getImage().getScaledInstance(400, 300, Image.SCALE_SMOOTH);
            imageLabel.setIcon(new ImageIcon(scaledImage));
            imageLabel.setText("");
        }
    }
}

class ImageProcessor {
    private Image image;
    private File imageFile;

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
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new IOException("Inference script failed with exit code " + exitCode);
        }

        return output.toString();
    }

    public Image getImage() {
        return image;
    }
}

class ResultDisplay {
    public void showResult(JTextArea resultArea, String result) {
        resultArea.setText(result);
    }
}
