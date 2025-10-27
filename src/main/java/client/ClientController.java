package client;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.PieChart;
import javafx.scene.control.*;

import java.net.URL;
import java.util.*;

public class ClientController implements Initializable {
    @FXML private TextField usernameField;
    @FXML private Button connectButton;
    @FXML private Button voteButton;
    @FXML private RadioButton optionA, optionB, optionC;
    @FXML private Label statusLabel;
    @FXML private Label totalVotesLabel;
    @FXML private Label connectionLabel;
    @FXML private PieChart voteChart;
    
    @FXML
    private final ToggleGroup voteGroup = new ToggleGroup();
    private VotingClient client;
    private Map<String, Integer> voteResults = new HashMap<>();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Initialize toggle group
        optionA.setToggleGroup(voteGroup);
        optionB.setToggleGroup(voteGroup);
        optionC.setToggleGroup(voteGroup);
        
        // Initialize vote results
        voteResults.put("Option A", 0);
        voteResults.put("Option B", 0);
        voteResults.put("Option C", 0);
        
        updateChart();
    }

    @FXML
    private void handleConnect() {
        String username = usernameField.getText().trim();
        if (username.isEmpty()) {
            showAlert("Error", "Please enter a username");
            return;
        }

        try {
            client = new VotingClient("localhost", 12345, username, this);
            new Thread(client).start();
            
            connectButton.setDisable(true);
            usernameField.setDisable(true);
            voteButton.setDisable(false);
            statusLabel.setText("Connected");
            statusLabel.setStyle("-fx-text-fill: #27ae60;");
            connectionLabel.setText("Connected to server");
            connectionLabel.setStyle("-fx-text-fill: #27ae60;");
            
        } catch (Exception e) {
            showAlert("Connection Error", "Cannot connect to server: " + e.getMessage());
        }
    }

    @FXML
    private void handleVote() {
        RadioButton selected = (RadioButton) voteGroup.getSelectedToggle();
        if (selected == null) {
            showAlert("Error", "Please select an option to vote");
            return;
        }

        if (client != null) {
            String option = selected.getText();
            client.sendVote(option);
            voteButton.setDisable(true);
            statusLabel.setText("Vote submitted for " + option);
        }
    }

    public void updateVoteResults(Map<String, Integer> results) {
        Platform.runLater(() -> {
            voteResults.putAll(results);
            updateChart();
            
            int total = results.values().stream().mapToInt(Integer::intValue).sum();
            totalVotesLabel.setText("Total Votes: " + total);
        });
    }

    private void updateChart() {
        voteChart.getData().clear();
        
        for (Map.Entry<String, Integer> entry : voteResults.entrySet()) {
            PieChart.Data data = new PieChart.Data(entry.getKey(), entry.getValue());
            voteChart.getData().add(data);
        }
    }

    public void handleServerMessage(String message) {
        Platform.runLater(() -> {
            if (message.startsWith("RESULTS:")) {
                parseResults(message);
            } else if (message.startsWith("ERROR:")) {
                statusLabel.setText(message.substring(6));
                statusLabel.setStyle("-fx-text-fill: #e74c3c;");
                voteButton.setDisable(false);
            }
        });
    }

    private void parseResults(String message) {
        try {
            String resultsStr = message.substring(8); // Remove "RESULTS:"
            String[] pairs = resultsStr.split(";");
            
            Map<String, Integer> newResults = new HashMap<>();
            for (String pair : pairs) {
                if (!pair.isEmpty()) {
                    String[] keyValue = pair.split(":");
                    if (keyValue.length == 2) {
                        newResults.put(keyValue[0], Integer.parseInt(keyValue[1]));
                    }
                }
            }
            updateVoteResults(newResults);
        } catch (Exception e) {
            System.err.println("Error parsing results: " + e.getMessage());
        }
    }

    public void handleDisconnection() {
        Platform.runLater(() -> {
            statusLabel.setText("Disconnected");
            statusLabel.setStyle("-fx-text-fill: #e74c3c;");
            connectionLabel.setText("Disconnected from server");
            connectionLabel.setStyle("-fx-text-fill: #e74c3c;");
            connectButton.setDisable(false);
            usernameField.setDisable(false);
            voteButton.setDisable(true);
        });
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}