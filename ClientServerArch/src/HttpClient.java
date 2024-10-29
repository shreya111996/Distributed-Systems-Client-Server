import java.io.*;
import java.net.*;

public class HttpClient {
    private static final String SERVER_ADDRESS = "http://localhost:8080"; // Change if needed

    public static void main(String[] args) {
        try {
            // Create a connection to the server
            URL url = new URL(SERVER_ADDRESS + "/index.html"); // Change the file as needed
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000); // 5 seconds timeout for connection
            connection.setReadTimeout(5000); // 5 seconds timeout for reading

            // Check the response code
            int responseCode = connection.getResponseCode();
            System.out.println("Response Code: " + responseCode);

            // Read and print the response
            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String inputLine;
                StringBuilder response = new StringBuilder();

                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();

                // Print the response
                System.out.println("Response:\n" + response.toString());
            } else {
                System.out.println("Error: " + connection.getResponseMessage());
            }

            connection.disconnect();
        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }
}