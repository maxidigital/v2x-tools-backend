package main.telegram;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class TelegramSender
{

    private final String chatId = "20088993";
    //private final String botToken = "5205695298:AAF-1wfkx8UgxwqqND43OgrLmV6OGzKPmu0";  // Freedive mallorca
    private final String botToken = "6905485918:AAGkBzlywwxtM2aENMfHSlfK5AaWP-urrfQ";    // V2X.tools
    private final String urlString = "https://api.telegram.org/bot" + botToken + "/sendMessage";
    private HttpURLConnection connection;

    //https://api.telegram.org/bot5205695298:AAF-1wfkx8UgxwqqND43OgrLmV6OGzKPmu0/getUpdates
    /**
     *
     * @param message
     * @throws IOException
     * @throws InterruptedException
     */
    public void send(String message) throws IOException, InterruptedException {
        URL url = new URL(urlString);
        connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);

        // Token de acceso del bot
        // ID del chat al que quieres enviar el mensaje
        // Contenido del mensaje
        String messageText = message;

        // Parámetros del mensaje a enviar
        String parameters = "chat_id=" + chatId + "&text=" + messageText;
        byte[] postData = parameters.getBytes(StandardCharsets.UTF_8);

        // Enviar la solicitud
        try (DataOutputStream wr = new DataOutputStream(connection.getOutputStream())) {
            wr.write(postData);
        } catch (Exception e) {
            System.out.println("Error sending request");
        }

        // Leer la respuesta
        StringBuilder response = new StringBuilder();
        try (BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
        }

        // Procesar la respuesta
        if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
            System.out.println("Mensaje enviado con éxito");
        } else {
            System.out.println("Hubo un error al enviar el mensaje: " + response.toString());
        }

        connection.disconnect();
    }

}
