import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.*;

public class HttpServer {
    private static final int PORT = 8080; // Specify your port number here
    private static final String DOCUMENT_ROOT = "path/to/your/document/root"; // Specify your document root here
    private static final int BASE_TIMEOUT = 30; // Base timeout set to 30 seconds

    public static void main(String[] args) {
        ExecutorService executorService = Executors.newFixedThreadPool(10); // Thread pool to handle multiple connections

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server is running on port " + PORT);

            while (true) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    executorService.submit(() -> handleClient(clientSocket));
                } catch (IOException e) {
                    System.err.println("Error accepting connection: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.err.println("Could not start server: " + e.getMessage());
        }
    }

    private static void handleClient(Socket clientSocket) {
        try {
            clientSocket.setSoTimeout(BASE_TIMEOUT * 1000); // Set the timeout for the socket
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);

            String requestLine = in.readLine();
            System.out.println("REQUEST: " + requestLine);

            // Parse the request line
            if (requestLine != null && requestLine.startsWith("GET")) {
                String[] tokens = requestLine.split(" ");
                String filePath = tokens[1].equals("/") ? "/index.html" : tokens[1];
                File file = new File(DOCUMENT_ROOT, filePath);

                // Check if the file type is supported
                if (file.exists() && file.isFile()) {
                    if (isSupportedFileType(getFileExtension(file))) {
                        if (isReadable(file)) { // Check if the file is readable
                            // Send HTTP response
                            sendResponse(out, file);
                        } else {
                            sendErrorResponse(out, 403, "Forbidden");
                        }
                    } else {
                        sendErrorResponse(out, 404, "Not Found");
                    }
                } else {
                    sendErrorResponse(out, 404, "Not Found");
                }
            } else {
                sendErrorResponse(out, 400, "Bad Request");
            }
        } catch (SocketTimeoutException e) {
            System.err.println("Connection timed out");
        } catch (IOException e) {
            System.err.println("Error handling client: " + e.getMessage());
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                System.err.println("Error closing client socket: " + e.getMessage());
            }
        }
    }

    private static void sendResponse(PrintWriter out, File file) {
        try {
            // Prepare response headers
            out.println("HTTP/1.1 200 OK");
            out.println("Content-Type: " + getContentType(file));
            out.println("Content-Length: " + file.length());
            out.println(); // End of headers

            // Send file content
            byte[] fileContent = new byte[(int) file.length()];
            FileInputStream fileInputStream = new FileInputStream(file);
            fileInputStream.read(fileContent);
            out.write(new String(fileContent));
            out.flush();
            fileInputStream.close();
        } catch (IOException e) {
            System.err.println("Error sending response: " + e.getMessage());
        }
    }

    private static void sendErrorResponse(PrintWriter out, int statusCode, String statusMessage) {
        out.println("HTTP/1.1 " + statusCode + " " + statusMessage);
        out.println("Content-Type: text/plain");
        out.println("Content-Length: " + statusMessage.length());
        out.println(); // End of headers
        out.println(statusMessage);
        out.flush();
    }

    private static String getContentType(File file) {
        String fileName = file.getName();
        if (fileName.endsWith(".html") || fileName.endsWith(".htm")) {
            return "text/html";
        } else if (fileName.endsWith(".txt")) {
            return "text/plain";
        } else if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) {
            return "image/jpeg";
        } else if (fileName.endsWith(".png")) {
            return "image/png";
        } else if (fileName.endsWith(".gif")) {
            return "image/gif";
        } else if (fileName.endsWith(".css")) {
            return "text/css";
        } else if (fileName.endsWith(".js")) {
            return "application/javascript";
        } else if (fileName.endsWith(".json")) {
            return "application/json";
        } else if (fileName.endsWith(".svg")) {
            return "image/svg+xml";
        } else if (fileName.endsWith(".xml")) {
            return "application/xml";
        } else if (fileName.endsWith(".pdf")) {
            return "application/pdf";
        } else {
            return "application/octet-stream"; // Default type
        }
    }

    private static boolean isSupportedFileType(String fileType) {
        return fileType.equals("html") || fileType.equals("txt") ||
                fileType.equals("jpg") || fileType.equals("jpeg") ||
                fileType.equals("gif") || fileType.equals("png") ||
                fileType.equals("css") || fileType.equals("js") ||
                fileType.equals("json") || fileType.equals("svg") ||
                fileType.equals("xml") || fileType.equals("pdf");
    }

    private static String getFileExtension(File file) {
        String name = file.getName();
        int lastIndexOfDot = name.lastIndexOf('.');
        return (lastIndexOfDot == -1) ? "" : name.substring(lastIndexOfDot + 1).toLowerCase();
    }

    private static boolean isReadable(File file) {
        return Files.isReadable(Paths.get(file.getAbsolutePath())); // Check if the file is readable
    }
}
