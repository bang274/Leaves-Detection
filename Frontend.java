import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class Frontend {
    private JFrame frame;
    private JTextArea outputArea;
    private JButton runButton;

    public Frontend() {
        // Set up the main window
        frame = new JFrame("Python Background App");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(400, 300);
        frame.setLayout(new BorderLayout());

        // Create output area
        outputArea = new JTextArea();
        outputArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(outputArea);
        frame.add(scrollPane, BorderLayout.CENTER);

        // Create run button
        runButton = new JButton("Run Python Script");
        runButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                runPythonScript();
            }
        });
        frame.add(runButton, BorderLayout.SOUTH);

        // Display the window
        frame.setVisible(true);
    }

    private void runPythonScript() {
        try {
            // Use ProcessBuilder to run the Python script
            ProcessBuilder pb = new ProcessBuilder("python", "background_script.py");
            pb.redirectErrorStream(true);
            Process process = pb.start();

            // Read the output from the Python script
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }

            // Wait for the process to complete and get exit code
            int exitCode = process.waitFor();
            outputArea.setText("Python Script Output:\n" + output.toString() + "Exit Code: " + exitCode);

        } catch (IOException | InterruptedException ex) {
            outputArea.setText("Error: " + ex.getMessage());
        }
    }

    public static void main(String[] args) {
        // Run the GUI on the Event Dispatch Thread
        SwingUtilities.invokeLater(() -> new Frontend());
    }
}