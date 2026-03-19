//****************************************************************************//
// Copyright (C) 2024 German Airspace Center - All Rights Reserved
// Unauthorized copying of this file, via any medium is strictly prohibited
// Proprietary and confidential
// Written by Maximiliano Bottazzi <maximiliano.bottazzi@dlr.de>
//****************************************************************************//
package main;

import a.enums.LogLevel;
import a.tools.DLRLogger;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Arrays;
import main.handlers.ContactFormHandler;
import main.handlers.MainHandler;
import main.config.ConfigurationManager;
import main.monitoring.TelegramCenter;

public class MainWeb
{

    public static void main(String[] args) throws IOException {
        new MainWeb().start(args);
    }

    private boolean webEnabled = true;
    private int port = 8080;

    /**
     *
     * @param args
     * @throws IOException
     */
    public void start(String[] args) throws IOException {
        System.out.println(Arrays.toString(args));

        readArgs(args);
        
        // Initialize configuration and monitoring
        initializeMonitoring();

        HttpServer server = HttpServer.create(new InetSocketAddress("0.0.0.0", port), 0);

        // Register main handler for web requests
        server.createContext("/", new MainHandler(webEnabled));
        
        // Register contact form handler
        server.createContext("/api/contact", new ContactFormHandler());

        // Start the server
        server.start();
        A.p("Server started on port " + port);
    }

    private void readArgs(String[] args) {

        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("--port"))
                this.port = Integer.valueOf(args[++i]);
            else if (args[i].equals("--web-enabled"))
                this.webEnabled = true;
            else if (args[i].equals("--forwarding-port")) {
                String fport = args[++i];
                System.setProperty("forwarding_port", fport);
                System.out.println("Reading forwarding port " + fport);
            } else if (args[i].equals("--log"))
                A.setLog(true);
            else if (args[i].equals("--debug"))
                DLRLogger.setLevel(LogLevel.DEBUG);
            else
                System.out.println("Unknown option " + args[i]);
        }
    }
    
    private void initializeMonitoring() {
        A.p("Initializing monitoring system...");
        
        // Initialize configuration manager
        ConfigurationManager config = ConfigurationManager.getInstance();
        config.printConfiguration();
        
        // Initialize Telegram monitoring
        TelegramCenter telegramCenter = TelegramCenter.getInstance();
        
        // Send startup notification
        if (config.isTelegramEnabled()) {
            telegramCenter.notifyServerStatus("Starting", 
                "V2X.tools server starting on port " + port + 
                "\nWeb interface: " + (webEnabled ? "Enabled" : "Disabled"));
        }
        
        // Add shutdown hook for cleanup
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            A.p("Shutting down monitoring system...");
            telegramCenter.notifyServerStatus("Stopping", "V2X.tools server shutting down");
            telegramCenter.shutdown();
        }));
        
        A.p("Monitoring system initialized");
    }
}
