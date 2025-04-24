import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;

public class NetworkInfoFX extends Application {
    private ComboBox<String> commandBox;
    private TextArea outputArea;
    private Label statusLabel;
    private Button executeButton, cancelButton, resetButton;

    private Map<String, String> commandMap;
    private ExecutorService executor;
    private Process currentProcess;
    private volatile boolean isRunning;

    @Override
    public void start(Stage primaryStage) {
        executor = Executors.newSingleThreadExecutor();
        commandMap = initializeCommandMap();

        commandBox = new ComboBox<>();
        commandBox.getItems().addAll(commandMap.keySet());
        commandBox.setPromptText("Choose command");

        outputArea = new TextArea();
        outputArea.setEditable(false);

        statusLabel = new Label("Select a command to execute.");

        executeButton = new Button("Execute");
        cancelButton = new Button("Cancel");
        resetButton = new Button("Reset");

        executeButton.setOnAction(e -> executor.execute(() -> {
            String command = commandMap.get(commandBox.getValue());
            if (command != null && !command.isEmpty()) {
                executeCommand(command);
            } else {
                Platform.runLater(() -> outputArea.setText("Please select a valid command."));
            }
        }));

        cancelButton.setOnAction(e -> stopExecution());
        resetButton.setOnAction(e -> resetUI());

        HBox controls = new HBox(10, new Label("Select Command:"), commandBox, executeButton, cancelButton, resetButton);
        controls.setPadding(new Insets(10));

        VBox layout = new VBox(10, controls, outputArea, statusLabel);
        layout.setPadding(new Insets(10));
        Scene scene = new Scene(layout, 650, 400);

        // Optional CSS styling
        scene.getStylesheets().add(getClass().getResource("/styles/styles.css").toExternalForm());

        primaryStage.setTitle("Network Info Tool (JavaFX)");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private Map<String, String> initializeCommandMap() {
        Map<String, String> map = new HashMap<>();
        map.put("View basic network configuration", "ipconfig");
        map.put("View detailed network configuration", "ipconfig /all");
        map.put("Flush DNS resolver cache", "ipconfig /flushdns");
        map.put("Check connectivity to Google", "ping google.com");
        map.put("Trace route to Google", "tracert google.com");
        map.put("Perform DNS lookup for Google", "nslookup google.com");
        map.put("Display active network connections", "netstat -a");
        map.put("View ARP cache", "arp -a");
        map.put("Show routing table", "route print");
        map.put("View MAC address of your device", "getmac");
        map.put("Get device hostname", "hostname");
        map.put("Display active TCP connections and ports", "netstat -n");
        map.put("List all listening ports", "netstat -an");
        map.put("Show wireless network profiles", "netsh wlan show profiles");
        map.put("Display Wi-Fi passwords (admin required)", "netsh wlan show profile name=WiFi-Name key=clear");
        map.put("Test network speed to a server", "ping 8.8.8.8");
        return map;
    }

    private void executeCommand(String command) {
        try {
            if (currentProcess != null) currentProcess.destroyForcibly();
            isRunning = true;

            Platform.runLater(() -> {
                outputArea.clear();
                statusLabel.setText("Executing...");
            });

            ProcessBuilder builder = new ProcessBuilder("cmd.exe", "/c", command);
            builder.redirectErrorStream(true);
            currentProcess = builder.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(currentProcess.getInputStream()));
            long startTime = System.currentTimeMillis();

            new Thread(() -> {
                while (isRunning) {
                    long elapsed = (System.currentTimeMillis() - startTime) / 1000;
                    Platform.runLater(() -> statusLabel.setText("Executing... " + elapsed + "s elapsed"));
                    try { Thread.sleep(1000); } catch (InterruptedException ignored) {}
                }
            }).start();

            new Thread(() -> {
                String line;
                try {
                    while ((line = reader.readLine()) != null && isRunning) {
                        String finalLine = line;
                        Platform.runLater(() -> outputArea.appendText(finalLine + "\n"));
                    }
                } catch (Exception ignored) {}
            }).start();

            boolean finished = currentProcess.waitFor(60, TimeUnit.SECONDS);
            isRunning = false;
            Platform.runLater(() -> {
                if (!finished) {
                    outputArea.appendText("\nCommand timed out.\n");
                }
                statusLabel.setText("Execution completed.");
            });

        } catch (Exception e) {
            Platform.runLater(() -> outputArea.setText("Error: " + e.getMessage()));
        }
    }

    private void stopExecution() {
        if (currentProcess != null) {
            currentProcess.destroyForcibly();
            isRunning = false;
            Platform.runLater(() -> {
                outputArea.appendText("\nExecution cancelled.\n");
                statusLabel.setText("Select a command to execute.");
            });
        }
    }

    private void resetUI() {
        stopExecution();
        Platform.runLater(() -> {
            commandBox.getSelectionModel().clearSelection();
            outputArea.clear();
            statusLabel.setText("Select a command to execute.");
        });
    }

    public static void main(String[] args) {
        launch(args);
    }
}
