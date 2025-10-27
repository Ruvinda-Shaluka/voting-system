package client;

import java.io.*;
import java.net.Socket;
import java.util.UUID;

public class VotingClient implements Runnable {
    private final String host;
    private final int port;
    private final String username;
    private final ClientController controller;

    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private boolean connected;
    private final String clientId;

    public VotingClient(String host, int port, String username, ClientController controller) {
        this.host = host;
        this.port = port;
        this.username = username;
        this.controller = controller;
        this.clientId = username + "_" + UUID.randomUUID().toString().substring(0, 8);
    }

    @Override
    public void run() {
        try {
            socket = new Socket(host, port);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            connected = true;

            out.println(clientId + ":" + username);
            System.out.println("Connected to server as: " + username);

            String message;
            while (connected && (message = in.readLine()) != null) {
                System.out.println("Received from server: " + message);
                controller.handleServerMessage(message);
            }
        } catch (IOException e) {
            System.err.println("Client connection error: " + e.getMessage());
            controller.handleDisconnection();
        } finally {
            disconnect();
        }
    }

    public void sendVote(String option) {
        if (connected && out != null) {
            out.println("VOTE:" + option);
            System.out.println("Sent vote: " + option);
        } else {
            System.err.println("Cannot send vote - not connected to server");
        }
    }

    public void disconnect() {
        connected = false;
        try {
            if (out != null) {
                out.println("DISCONNECT");
            }
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException e) {
            System.err.println("Error disconnecting: " + e.getMessage());
        }
        System.out.println("Disconnected from server");
    }

    public boolean isConnected() {
        return connected && socket != null && !socket.isClosed() && socket.isConnected();
    }
}