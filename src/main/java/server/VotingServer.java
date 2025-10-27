package server;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class VotingServer {
    private static final int PORT = 12345;
    private final List<ClientHandler> clients;
    private final VoteService voteService;
    private boolean running;

    public VotingServer() {
        clients = new ArrayList<>();
        voteService = new VoteService();
    }

    public void start() {
        running = true;
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Voting Server started on port " + PORT);
            
            while (running) {
                Socket clientSocket = serverSocket.accept();
                ClientHandler clientHandler = new ClientHandler(clientSocket, voteService, this);
                clients.add(clientHandler);
                new Thread(clientHandler).start();
            }
        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
        }
    }

    public void broadcastResults() {
        Map<String, Integer> results = voteService.getVoteResults();
        StringBuilder resultMessage = new StringBuilder("RESULTS:");
        results.forEach((option, count) -> 
            resultMessage.append(option).append(":").append(count).append(";")
        );
        
        // Display on server console
        System.out.println("Current Results: " + resultMessage);
        System.out.println("Total Votes: " + voteService.getTotalVotes());
        
        // Broadcast to all clients
        for (ClientHandler client : clients) {
            client.sendMessage(resultMessage.toString());
        }
    }

    public void removeClient(ClientHandler client) {
        clients.remove(client);
    }

    public static void main(String[] args) {
        VotingServer server = new VotingServer();
        server.start();
    }
}