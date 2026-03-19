//****************************************************************************//
// Copyright (C) 2024 German Airspace Center - All Rights Reserved
// Unauthorized copying of this file, via any medium is strictly prohibited
// Proprietary and confidential
// Written by Maximiliano Bottazzi <maximiliano.bottazzi@dlr.de>
//****************************************************************************//
package main;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class ResourcesFileReader
{

    public static void main(String[] args) {
        String read = ResourcesFileReader.read("html/converter.html");
        System.out.println(read);
    }

    public static String read(String fileName) {
        // Get the input stream for the file using the class loader
        InputStream inputStream = ResourcesFileReader.class.getClassLoader().getResourceAsStream(fileName);
        StringBuilder sb = new StringBuilder();

        if (inputStream != null) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    // Process each line
                    //System.out.println(line);
                    sb.append(line).append("\n");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return sb.toString();
    }
}
