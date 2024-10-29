import java.io.*;
import java.net.*;

public class WebServer {

    private static final int PORT = 8080;  // Server port
    private static final String DOCUMENT_ROOT = "/path/to/your/document/root";  // Define document root

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server started on port " + PORT);

            while (true) {
                Socket clientSocket = serverSocket.accept();  // Accept client connections
                new ClientHandler(clientSocket).start();  // Spawn new thread for each connection
            }
        } catch (IOException e) {
            System.err.println("Error starting server: " + e.getMessage());
        }
    }
}
