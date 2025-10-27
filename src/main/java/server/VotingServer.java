package server;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

public class VotingServer {
    private static final int PORT = 12345;
    private final List<ClientHandler> clients;
    private final VoteService voteService;
    private boolean running;

    public VotingServer() {
        clients = new CopyOnWriteArrayList<>();
        voteService = new VoteService();
    }

    public void start() {
        running = true;
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Voting Server started on port " + PORT);
            System.out.println("Server is accepting multiple votes per client");
            System.out.println("Waiting for client connections...");

            while (running) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    ClientHandler clientHandler = new ClientHandler(clientSocket, voteService, this);
                    clients.add(clientHandler);
                    new Thread(clientHandler).start();
                    System.out.println("New client connected. Total clients: " + clients.size());
                } catch (IOException e) {
                    System.err.println("Error accepting client connection: " + e.getMessage());
                }
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

        System.out.println("Current Results: " + resultMessage);
        System.out.println("Total Unique Voters: " + voteService.getTotalVotes());

        for (ClientHandler client : clients) {
            client.sendMessage(resultMessage.toString());
        }
    }

    public void broadcastVoteHistory() {
        StringBuilder historyMessage = new StringBuilder("HISTORY:");
        voteService.getVoteHistory().forEach(entry ->
                historyMessage.append(entry).append("|")
        );

        for (ClientHandler client : clients) {
            client.sendMessage(historyMessage.toString());
        }
    }

    public void removeClient(ClientHandler client) {
        clients.remove(client);
        System.out.println("Client removed. Total clients: " + clients.size());
    }

    public static void main(String[] args) {
        VotingServer server = new VotingServer();
        server.start();
    }
}