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
    @FXML private Button disconnectButton;
    @FXML private Button voteButton;
    @FXML private Button changeVoteButton;
    @FXML private RadioButton optionA, optionB, optionC;
    @FXML private Label statusLabel;
    @FXML private Label totalVotesLabel;
    @FXML private Label connectionLabel;
    @FXML private Label myVoteLabel;
    @FXML private PieChart voteChart;
    @FXML private TextArea historyTextArea;

    @FXML
    private final ToggleGroup voteGroup = new ToggleGroup();
    private VotingClient client;
    private Map<String, Integer> voteResults = new HashMap<>();
    private List<String> voteHistory = new ArrayList<>();
    private String currentVote = null;
    private String username = "";

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
        updateHistoryDisplay();

        // Add listener to vote group to enable change vote button
        voteGroup.selectedToggleProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null && currentVote != null) {
                String selectedOption = ((RadioButton) newValue).getText();
                if (!selectedOption.equals(currentVote)) {
                    changeVoteButton.setDisable(false);
                } else {
                    changeVoteButton.setDisable(true);
                }
            }
        });
    }

    @FXML
    private void handleConnect() {
        username = usernameField.getText().trim();
        if (username.isEmpty()) {
            showAlert("Error", "Please enter a username");
            return;
        }

        try {
            client = new VotingClient("localhost", 12345, username, this);
            new Thread(client).start();

            connectButton.setDisable(true);
            disconnectButton.setDisable(false);
            usernameField.setDisable(true);
            voteButton.setDisable(false);
            statusLabel.setText("Connected as: " + username);
            statusLabel.setStyle("-fx-text-fill: #27ae60;");
            connectionLabel.setText("Connected to server");
            connectionLabel.setStyle("-fx-text-fill: #27ae60;");

        } catch (Exception e) {
            showAlert("Connection Error", "Cannot connect to server: " + e.getMessage());
            handleDisconnection();
        }
    }

    @FXML
    void handleDisconnect() {
        if (client != null) {
            client.disconnect();
        }
        handleDisconnection();
    }

    @FXML
    private void handleVote() {
        RadioButton selected = (RadioButton) voteGroup.getSelectedToggle();
        if (selected == null) {
            showAlert("Error", "Please select an option to vote");
            return;
        }

        if (client != null && client.isConnected()) {
            String option = selected.getText();
            client.sendVote(option);
            statusLabel.setText("Submitting vote...");
        } else {
            showAlert("Error", "Not connected to server");
        }
    }

    @FXML
    private void handleChangeVote() {
        RadioButton selected = (RadioButton) voteGroup.getSelectedToggle();
        if (selected == null) {
            showAlert("Error", "Please select a new option to change your vote");
            return;
        }

        if (client != null && client.isConnected()) {
            String newOption = selected.getText();
            client.sendVote(newOption);
            statusLabel.setText("Changing vote...");
        } else {
            showAlert("Error", "Not connected to server");
        }
    }

    private void updateMyVoteLabel() {
        if (currentVote != null) {
            myVoteLabel.setText("You have voted");
            myVoteLabel.setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
        } else {
            myVoteLabel.setText("Not voted yet");
            myVoteLabel.setStyle("-fx-text-fill: #2c3e50;");
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
            PieChart.Data data = new PieChart.Data(entry.getKey() + " (" + entry.getValue() + ")", entry.getValue());
            voteChart.getData().add(data);
        }
    }

    private void updateHistoryDisplay() {
        StringBuilder historyText = new StringBuilder();
        // Show only the latest 10 activities to avoid clutter
        int startIndex = Math.max(0, voteHistory.size() - 10);
        for (int i = startIndex; i < voteHistory.size(); i++) {
            historyText.append("• ").append(voteHistory.get(i)).append("\n");
        }
        historyTextArea.setText(historyText.toString());
    }

    public void handleServerMessage(String message) {
        Platform.runLater(() -> {
            System.out.println("Processing message: " + message);

            if (message.startsWith("RESULTS:")) {
                parseResults(message);
                statusLabel.setText("Results updated");
            } else if (message.startsWith("HISTORY:")) {
                parseHistory(message);
            } else if (message.startsWith("VOTE_ACCEPTED:")) {
                String option = message.substring(14);
                currentVote = option;
                updateMyVoteLabel();
                voteButton.setDisable(true);
                changeVoteButton.setDisable(true);
                statusLabel.setText("Vote submitted successfully");
                statusLabel.setStyle("-fx-text-fill: #27ae60;");
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

    private void parseHistory(String message) {
        try {
            String historyStr = message.substring(8); // Remove "HISTORY:"
            String[] entries = historyStr.split("\\|");

            voteHistory.clear();
            for (String entry : entries) {
                if (!entry.isEmpty()) {
                    voteHistory.add(entry);
                }
            }
            updateHistoryDisplay();
        } catch (Exception e) {
            System.err.println("Error parsing history: " + e.getMessage());
        }
    }

    public void handleDisconnection() {
        Platform.runLater(() -> {
            statusLabel.setText("Disconnected");
            statusLabel.setStyle("-fx-text-fill: #e74c3c;");
            connectionLabel.setText("Disconnected from server");
            connectionLabel.setStyle("-fx-text-fill: #e74c3c;");
            connectButton.setDisable(false);
            disconnectButton.setDisable(true);
            usernameField.setDisable(false);
            voteButton.setDisable(true);
            changeVoteButton.setDisable(true);
            currentVote = null;
            updateMyVoteLabel();

            // Clear selection
            voteGroup.selectToggle(null);
        });
    }

    private void showAlert(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }
}