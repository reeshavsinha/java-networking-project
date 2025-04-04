import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class NetworkInfoGUI extends JFrame {
    private JComboBox<String> commandBox;
    private JTextArea outputArea;
    private JButton executeButton, cancelButton, resetButton;
    private JLabel statusLabel;
    private Map<String, String> commandMap;
    private ExecutorService executor;
    private Process currentProcess;
    private volatile boolean isRunning;

    public NetworkInfoGUI() {
        setTitle("Network Info Tool");
        setSize(600, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        executor = Executors.newSingleThreadExecutor();

        // Command descriptions mapped to actual commands
        commandMap = new HashMap<>();
        commandMap.put("View basic network configuration", "ipconfig");
        commandMap.put("View detailed network configuration", "ipconfig /all");
        commandMap.put("Flush DNS resolver cache", "ipconfig /flushdns");
        commandMap.put("Check connectivity to Google", "ping google.com");
        commandMap.put("Trace route to Google", "tracert google.com");
        commandMap.put("Perform DNS lookup for Google", "nslookup google.com");
        commandMap.put("Display active network connections", "netstat -a");
        commandMap.put("View ARP cache", "arp -a");
        commandMap.put("Show routing table", "route print");
        commandMap.put("View MAC address of your device", "getmac");
        commandMap.put("Get device hostname", "hostname");
        commandMap.put("Display active TCP connections and ports", "netstat -n");
        commandMap.put("List all listening ports", "netstat -an");
        commandMap.put("Show wireless network profiles", "netsh wlan show profiles");
        commandMap.put("Display Wi-Fi passwords (admin required)", "netsh wlan show profile name=WiFi-Name key=clear");
        commandMap.put("Test network speed to a server", "ping 8.8.8.8");

        commandBox = new JComboBox<>(commandMap.keySet().toArray(new String[0]));
        executeButton = new JButton("Execute");
        cancelButton = new JButton("Cancel");
        resetButton = new JButton("Reset");
        outputArea = new JTextArea();
        outputArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(outputArea);
        statusLabel = new JLabel("Select a command to execute.");

        // Panel for top controls
        JPanel topPanel = new JPanel();
        topPanel.add(new JLabel("Select Command:"));
        topPanel.add(commandBox);
        topPanel.add(executeButton);
        topPanel.add(cancelButton);
        topPanel.add(resetButton);

        add(topPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
        add(statusLabel, BorderLayout.SOUTH);

        // Action Listener for Execute Button
        executeButton.addActionListener(e -> executor.execute(() -> {
            String selectedDescription = (String) commandBox.getSelectedItem();
            String command = commandMap.get(selectedDescription);
            if (command != null && !command.isEmpty()) {
                executeCommand(command);
            } else {
                SwingUtilities.invokeLater(() -> outputArea.setText("Please select a valid command."));
            }
        }));

        // Action Listener for Cancel Button
        cancelButton.addActionListener(e -> stopExecution());

        // Action Listener for Reset Button
        resetButton.addActionListener(e -> resetUI());
    }

    // Method to execute the selected command
    private void executeCommand(String command) {
        try {
            if (currentProcess != null) {
                currentProcess.destroyForcibly(); // Ensure previous process is stopped
            }

            isRunning = true;
            SwingUtilities.invokeLater(() -> {
                statusLabel.setText("Executing... 0s elapsed");
                outputArea.setText("");
            });

            ProcessBuilder builder = new ProcessBuilder("cmd.exe", "/c", command);
            builder.redirectErrorStream(true);
            currentProcess = builder.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(currentProcess.getInputStream()));
            long timeout = 60; // Increased timeout limit to 60 seconds
            long startTime = System.currentTimeMillis();

            new Thread(() -> {
                while (isRunning) {
                    long elapsed = (System.currentTimeMillis() - startTime) / 1000;
                    SwingUtilities.invokeLater(() -> statusLabel.setText("Executing... " + elapsed + "s elapsed (Max: " + timeout + "s)"));
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ignored) {}
                }
            }).start();

            new Thread(() -> {
                try {
                    String line;
                    while ((line = reader.readLine()) != null && isRunning) {
                        String finalLine = line;
                        SwingUtilities.invokeLater(() -> {
                            outputArea.append(finalLine + "\n");
                            outputArea.setCaretPosition(outputArea.getDocument().getLength()); // Auto-scroll
                        });
                    }
                } catch (Exception ignored) {}
            }).start();

            boolean finished = currentProcess.waitFor(timeout, TimeUnit.SECONDS);
            isRunning = false;
            SwingUtilities.invokeLater(() -> statusLabel.setText("Execution completed."));

            if (!finished) {
                currentProcess.destroyForcibly();
                SwingUtilities.invokeLater(() -> outputArea.append("\nCommand timed out. Try again or check network settings."));
            }
        } catch (Exception ex) {
            SwingUtilities.invokeLater(() -> outputArea.setText("Error executing command: " + ex.getMessage()));
        }
    }

    // Method to stop execution
    private void stopExecution() {
        if (currentProcess != null) {
            currentProcess.destroyForcibly();
            isRunning = false;
            SwingUtilities.invokeLater(() -> {
                outputArea.append("\nExecution cancelled.");
                statusLabel.setText("Select a command to execute.");
            });
        }
    }

    // Method to reset the UI
    private void resetUI() {
        stopExecution();
        commandBox.setSelectedIndex(-1);
        outputArea.setText("");
        statusLabel.setText("Select a command to execute.");
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new NetworkInfoGUI().setVisible(true);
        });
    }
}
