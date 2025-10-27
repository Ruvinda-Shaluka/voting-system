package server;

import java.io.*;
import java.net.Socket;
import java.util.Map;

public class ClientHandler implements Runnable {
    private final Socket socket;
    private final VoteService voteService;
    private final VotingServer server;
    private PrintWriter out;
    private BufferedReader in;
    private String clientId;
    private String clientName;
    private boolean connected;

    public ClientHandler(Socket socket, VoteService voteService, VotingServer server) {
        this.socket = socket;
        this.voteService = voteService;
        this.server = server;
        this.connected = true;
    }

    @Override
    public void run() {
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            String clientInfo = in.readLine();
            if (clientInfo != null && clientInfo.contains(":")) {
                String[] parts = clientInfo.split(":");
                clientId = parts[0];
                clientName = parts.length > 1 ? parts[1] : clientId;
            } else {
                clientId = clientInfo;
                clientName = clientId;
            }

            System.out.println("Client connected: " + clientName + " (" + clientId + ")");

            sendCurrentResults();
            sendVoteHistory();

            String message;
            while (connected && (message = in.readLine()) != null) {
                System.out.println("Received from " + clientName + ": " + message);
                processMessage(message);
            }

        } catch (IOException e) {
            System.out.println("Client disconnected: " + clientName + " - " + e.getMessage());
        } finally {
            disconnect();
        }
    }

    private void processMessage(String message) {
        if (message.startsWith("VOTE:")) {
            String option = message.substring(5);
            handleVote(option);
        } else if (message.equals("GET_HISTORY")) {
            sendVoteHistory();
        } else if (message.equals("GET_RESULTS")) {
            sendCurrentResults();
        } else if (message.equals("DISCONNECT")) {
            disconnect();
        }
    }

    private void handleVote(String option) {
        boolean success = voteService.castVote(clientId, clientName, option);
        if (success) {
            System.out.println("Vote received from " + clientName + " for " + option);
            server.broadcastResults();
            server.broadcastVoteHistory();

            // Send confirmation to client
            sendMessage("VOTE_ACCEPTED:" + option);
        } else {
            sendMessage("ERROR: Invalid option");
        }
    }

    private void sendCurrentResults() {
        Map<String, Integer> results = voteService.getVoteResults();
        StringBuilder resultMessage = new StringBuilder("RESULTS:");
        results.forEach((option, count) ->
                resultMessage.append(option).append(":").append(count).append(";")
        );
        sendMessage(resultMessage.toString());
    }

    private void sendVoteHistory() {
        StringBuilder historyMessage = new StringBuilder("HISTORY:");
        voteService.getVoteHistory().forEach(entry ->
                historyMessage.append(entry).append("|")
        );
        sendMessage(historyMessage.toString());
    }

    public void sendMessage(String message) {
        if (out != null) {
            out.println(message);
            out.flush(); // Ensure message is sent immediately
        }
    }

    public void disconnect() {
        connected = false;
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException e) {
            System.out.println("Error closing client connection: " + e.getMessage());
        }
        server.removeClient(this);
        System.out.println("Client fully disconnected: " + clientName);
    }
}