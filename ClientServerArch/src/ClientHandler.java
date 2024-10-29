import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.file.Files;
import java.util.Date;
import java.util.StringTokenizer;

class ClientHandler extends Thread {
    private final Socket clientSocket;
    private static final String DOCUMENT_ROOT = "/path/to/your/document/root"; // Define document root

    public ClientHandler(Socket socket) {
        this.clientSocket = socket;
    }

    @Override
    public void run() {
        try (
                InputStream input = clientSocket.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(input));
                PrintWriter writer = new PrintWriter(clientSocket.getOutputStream(), true) // Moved here to ensure
                                                                                           // exception handling
        ) {
            String requestLine = reader.readLine(); // Read HTTP request line

            if (requestLine == null || !requestLine.startsWith("GET")) {
                sendError(writer, 400, "Bad Request");
                return;
            }

            StringTokenizer tokenizer = new StringTokenizer(requestLine);
            tokenizer.nextToken(); // Skip "GET"
            String fileRequested = tokenizer.nextToken();

            if (fileRequested.equals("/")) {
                fileRequested = "/index.html"; // Default file handling
            }

            File file = new File(DOCUMENT_ROOT, fileRequested);
            if (!file.exists()) {
                sendError(writer, 404, "File Not Found");
            } else if (!file.canRead()) {
                sendError(writer, 403, "Forbidden");
            } else {
                // Check for supported file types before serving
                String fileType = getFileExtension(file);
                if (isSupportedFileType(fileType)) {
                    sendFile(writer, clientSocket.getOutputStream(), file); // Pass the output stream directly
                } else {
                    sendError(writer, 403, "Forbidden - Unsupported File Type");
                }
            }
        } catch (IOException e) {
            System.err.println("Error handling client: " + e.getMessage());
            // Send internal server error response
            try {
                PrintWriter writer = new PrintWriter(clientSocket.getOutputStream(), true);
                sendError(writer, 500, "Internal Server Error");
            } catch (IOException ioException) {
                System.err.println("Error sending error response: " + ioException.getMessage());
            }
        } finally {
            try {
                clientSocket.close(); // Close client connection after each request
            } catch (IOException e) {
                System.err.println("Error closing connection: " + e.getMessage());
            }
        }
    }

    private void sendError(PrintWriter writer, int statusCode, String message) {
        writer.println("HTTP/1.0 " + statusCode + " " + message);
        writer.println("Content-Type: text/html");
        writer.println();
        writer.println("<html><body><h1>" + statusCode + " " + message + "</h1></body></html>");
        writer.flush();
    }

    private void sendFile(PrintWriter writer, OutputStream output, File file) {
        try {
            String contentType = Files.probeContentType(file.toPath());
            long fileLength = file.length();
            Date now = new Date();

            writer.println("HTTP/1.0 200 OK");
            writer.println("Server: SimpleWebServer");
            writer.println("Date: " + now);
            writer.println("Content-Type: " + contentType);
            writer.println("Content-Length: " + fileLength);
            writer.println();
            writer.flush();

            // Send file content
            try (BufferedInputStream fileInput = new BufferedInputStream(new FileInputStream(file))) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = fileInput.read(buffer)) != -1) {
                    output.write(buffer, 0, bytesRead);
                }
                output.flush();
            }
        } catch (IOException e) {
            System.err.println("Error sending file: " + e.getMessage());
        }
    }

    private String getFileExtension(File file) {
        String name = file.getName();
        int lastIndex = name.lastIndexOf('.');
        return (lastIndex == -1) ? "" : name.substring(lastIndex + 1).toLowerCase();
    }

    private boolean isSupportedFileType(String fileType) {
        return fileType.equals("html") || fileType.equals("txt") ||
                fileType.equals("jpg") || fileType.equals("jpeg") ||
                fileType.equals("gif") || fileType.equals("png") ||
                fileType.equals("css") || fileType.equals("js") ||
                fileType.equals("json") || fileType.equals("svg") ||
                fileType.equals("xml") || fileType.equals("pdf") ||
                fileType.equals("ico");
    }
}