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

    public ClientHandler(Socket socket, VoteService voteService, VotingServer server) {
        this.socket = socket;
        this.voteService = voteService;
        this.server = server;
    }

    @Override
    public void run() {
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            // Read client ID
            clientId = in.readLine();
            System.out.println("Client connected: " + clientId);

            // Send current results to new client
            sendCurrentResults();

            String message;
            while ((message = in.readLine()) != null) {
                if (message.startsWith("VOTE:")) {
                    String option = message.substring(5);
                    handleVote(option);
                }
            }
        } catch (IOException e) {
            System.out.println("Client disconnected: " + clientId);
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void handleVote(String option) {
        boolean success = voteService.castVote(clientId, option);
        if (success) {
            System.out.println("Vote received from " + clientId + " for " + option);
            server.broadcastResults();
        } else {
            sendMessage("ERROR: You have already voted or invalid option");
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

    public void sendMessage(String message) {
        if (out != null) {
            out.println(message);
        }
    }
}