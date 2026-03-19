//****************************************************************************//
// Copyright (C) 2024 German Airspace Center - All Rights Reserved
// Unauthorized copying of this file, via any medium is strictly prohibited
// Proprietary and confidential
// Written by Maximiliano Bottazzi <maximiliano.bottazzi@dlr.de>
//****************************************************************************//
package main.handlers;

import a.tools.Tools;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class PostTest
{

    public static void main(String[] args) {
        try {
            // Replace this with your actual blob data
            byte[] blobData = Tools.hexStringToBytes("0202000000000000000d693a403ad274803ffffffc23b7742000e11fdffffe3fe1ed0737fe03fff400");

            // Set the URL for the POST request
            URL url = new URL("http://wind-v2x.de:8080/uper2json");

            // Open a connection to the URL
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            // Set the request method to POST
            connection.setRequestMethod("POST");

            // Set content type to binary data
            connection.setRequestProperty("Content-Type", "application/octet-stream");

            // Enable input and output streams
            connection.setDoOutput(true);

            // Write the blob data to the request body
            try (OutputStream os = connection.getOutputStream()) {
                os.write(blobData);
            }

            // Get the response code (optional)
            int responseCode = connection.getResponseCode();
            System.out.println("Response Code: " + responseCode);

            // Read the response content
            try (InputStream is = connection.getInputStream();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {

                String line;
                StringBuilder responseContent = new StringBuilder();
                while ((line = reader.readLine()) != null) {
                    responseContent.append(line);
                }

                // Print the response content
                System.out.println("Response Content: " + responseContent.toString());
            }

            // Close the connection
            connection.disconnect();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
