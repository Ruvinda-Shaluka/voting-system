package client;

import java.io.*;
import java.net.Socket;
import java.util.Map;

public class VotingClient implements Runnable {
    private final String host;
    private final int port;
    private final String username;
    private final ClientController controller;
    
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private boolean connected;

    public VotingClient(String host, int port, String username, ClientController controller) {
        this.host = host;
        this.port = port;
        this.username = username;
        this.controller = controller;
    }

    @Override
    public void run() {
        try {
            socket = new Socket(host, port);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            connected = true;

            // Send username to server
            out.println(username);

            // Listen for server messages
            String message;
            while (connected && (message = in.readLine()) != null) {
                controller.handleServerMessage(message);
            }
        } catch (IOException e) {
            System.err.println("Client error: " + e.getMessage());
        } finally {
            disconnect();
        }
    }

    public void sendVote(String option) {
        if (connected && out != null) {
            out.println("VOTE:" + option);
        }
    }

    public void disconnect() {
        connected = false;
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null) socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        controller.handleDisconnection();
    }
}