package client;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class VotingClientApp extends Application {
    
    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/client/voting-client.fxml"));
        Parent root = loader.load();
        
        primaryStage.setTitle("Distributed Voting System");
        primaryStage.setScene(new Scene(root, 800, 600));
        primaryStage.setMinWidth(600);
        primaryStage.setMinHeight(500);
        primaryStage.show();
        
        // Handle application close
        primaryStage.setOnCloseRequest(event -> {
            ClientController controller = loader.getController();
            // Add any cleanup logic here
        });
    }

    public static void main(String[] args) {
        launch(args);
    }
}