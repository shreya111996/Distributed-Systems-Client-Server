package assignment;

import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

class ContentNotFoundError extends Exception {
    public ContentNotFoundError(String message) {
        super(message);
    }
}

class ForbiddenAccessError extends Exception {
    public ForbiddenAccessError(String message) {
        super(message);
    }
}

public class Server {
    private static int port = 8080;
    private static String root = ".";
    private int numConnections = 0;
    private final Object lock = new Object();
    private ExecutorService executorService;

    public Server(int port, String root) {
        this.port = port;
        this.root = root;
        this.executorService = Executors.newFixedThreadPool(10); // Fixed thread pool of 10
    }

    public void startServer() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Server is running on port " + port);
            while (true) {
                Socket clientSocket = serverSocket.accept();
                synchronized (lock) {
                    numConnections++;
                }
                executorService.submit(() -> {
                    try {
                        handleClient(clientSocket);
                    } catch (ForbiddenAccessError e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
            }
        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
        }
    }

    private void handleClient(Socket clientSocket) throws ForbiddenAccessError, IOException {
        int timeout = calculateTimeout();
        try {

            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            OutputStream out = clientSocket.getOutputStream();
            clientSocket.setSoTimeout(timeout);

            String request = in.readLine();
            if (request == null)
                return;

            System.out.println("REQUEST: " + request);
            String[] requestParts = request.split(" ");
            String method = requestParts[0];
            String filePath = requestParts[1].equals("/") ? "/index.html" : requestParts[1];
            String protocol = requestParts[2];

            if ("GET".equalsIgnoreCase(method)) {
                handleRequest(out, filePath, protocol, timeout);
            } else {
                sendErrorResponse(out, protocol, 400, "Bad Request");
            }

            if ("HTTP/1.1".equals(protocol)) {
                String nextRequest = in.readLine(); // Read the next request
                if (nextRequest != null && !nextRequest.isEmpty()) {
                    handleClient(clientSocket); // Handle subsequent requests in the same connection
                }
            } else {
                clientSocket.close();
            }
        } catch (SocketTimeoutException e) {
            System.out.println("Connection timed out.");
        } catch (FileNotFoundException e) {
            sendErrorResponse(clientSocket.getOutputStream(), "HTTP/1.1", 404, "404 : File Not Found");
        } catch (Exception e) {
            System.out.println("Error processing request: " + e.getMessage());
        } finally {
            synchronized (lock) {
                numConnections--;
            }
            try {
                if (!clientSocket.isClosed()) {
                    clientSocket.close();
                }
            } catch (IOException e) {
                System.err.println("Error closing client connection: " + e.getMessage());
            }
        }
    }

    private void handleRequest(OutputStream out, String filePath, String protocol, int timeout)
            throws IOException, ForbiddenAccessError {
        if ("/".equals(filePath)) {
            filePath = "/index.html";
        }

        File file = new File(root + filePath);
        if (!file.exists()) {
            throw new FileNotFoundException("File not found");
        }

        if (isRestricted(file)) {
            sendErrorResponse(out, protocol, 403, "403 : Forbidden Access");
            return;
        }

        byte[] content = Files.readAllBytes(file.toPath());
        String fileType = getFileType(filePath);
        String response = protocol + " 200 OK\n" +
                "Content-Type: " + fileType + "\n" + // Set content type header
                "Content-Length: " + content.length + "\n" +
                "Date: " + getServerTime() + "\n" +
                "Connection: " + ("HTTP/1.1".equals(protocol) ? "Keep-Alive" : "close") + "\n\n";

        out.write(response.getBytes());
        out.write(content);
        out.flush();
    }

    private String getServerTime() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
        dateFormat.setTimeZone(TimeZone.getTimeZone("America/Los_Angeles"));
        return dateFormat.format(new Date());
    }

    private String getFileType(String filePath) {
        String fileType;
        if (filePath.endsWith(".html")) {
            fileType = "text/html";
        } else if (filePath.endsWith(".txt")) {
            fileType = "text/plain";
        } else if (filePath.endsWith(".jpg") || filePath.endsWith(".jpeg")) {
            fileType = "image/jpeg";
        } else if (filePath.endsWith(".png")) {
            fileType = "image/png";
        } else if (filePath.endsWith(".gif")) {
            fileType = "image/gif";
        } else if (filePath.endsWith(".json")) {
            fileType = "application/json";
        } else if (filePath.endsWith(".svg")) {
            fileType = "image/svg+xml";
        } else {
            fileType = "application/octet-stream";
        }
        return fileType;
    }

    private void sendErrorResponse(OutputStream out, String protocol, int errorCode, String message)
            throws IOException {
        String response = protocol + " " + errorCode + " " + message + "\n" +
                "Content-Type: text/plain\n" +
                "Content-Length: " + message.length() + "\n\n" + message;
        out.write(response.getBytes());
    }

    private boolean isRestricted(File file) {
        boolean isRestricted = !file.canRead() || file.getUsableSpace() <= 0L;
        if (isRestricted) {
            System.err.println("403 Forbidden: Access denied to " + file.getPath());
        }
        return isRestricted;
    }

    private int calculateTimeout() {
        return 200000 - (10000 * numConnections);
    }

    public static void main(String[] args) {
        if (args.length < 4) {
            System.err.println("Usage: java SimpleHttpServer -document_root <path> -port <port>");
            System.exit(1);
        }

        // Parse command-line arguments
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "-document_root":
                    if (i + 1 < args.length) {
                        root = args[++i];
                    } else {
                        System.err.println("Error: Missing value for -document_root");
                        System.exit(1);
                    }
                    break;
                case "-port":
                    if (i + 1 < args.length) {
                        try {
                            port = Integer.parseInt(args[++i]);
                        } catch (NumberFormatException e) {
                            System.err.println("Error: Invalid port number");
                            System.exit(1);
                        }
                    } else {
                        System.err.println("Error: Missing value for -port");
                        System.exit(1);
                    }
                    break;
                default:
                    System.err.println("Unknown option: " + args[i]);
                    System.exit(1);
            }
        }

        System.out.println("Starting server on port " + port + " with document root " + root);
        Server server = new Server(port, root);
        server.startServer();
    }
}

