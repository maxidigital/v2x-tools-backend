package main.stats;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import org.json.JSONObject;

public class CSVLine
{

    public enum Origin
    {
        WEB, API
    }
    private Origin origin = Origin.WEB;
    private Instant timestamp;
    private String clientUrl;
    private String received;
    private String sent;

    /**
     *
     * @param origin
     * @param timestamp
     * @param clientUrl
     * @param received
     * @param sent
     */
    public CSVLine(Origin origin, long timestamp, String clientUrl, String received, String sent) {
        this.origin = origin;
        this.timestamp = Instant.ofEpochMilli(timestamp);
        this.clientUrl = clientUrl;
        this.received = received;
        this.sent = sent.replaceAll("\\n", "");
    }

    public CSVLine(String csvLine) {
        parseCsvLine(csvLine);
    }

    public String getLine() {
        String line;

        line = String.format("%s, %s, %s, %s, %s", timestamp.toEpochMilli(), clientUrl, received, sent, origin);

        return line;
    }

    private void parseCsvLine(String csvLine) {
        String[] parts = csvLine.split(", ", 5);

        if (parts.length != 5) {
            throw new IllegalArgumentException("Invalid CSV line format");
        }

        try {
            this.timestamp = Instant.ofEpochMilli(Long.parseLong(parts[0].trim()));
            this.clientUrl = parts[1].trim();
            this.received = parts[2].trim();
            this.sent = parts[3].trim();
            this.origin = Origin.valueOf(parts[4]);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid CSV line format");
        }
    }

    public String toJson() {
        JSONObject json = new JSONObject();
        LocalDateTime dateTime = LocalDateTime.ofInstant(this.timestamp, ZoneId.systemDefault());

        json.put("ip", this.clientUrl.split(":")[0].replace("/", ""));
        json.put("date", dateTime.format(DateTimeFormatter.ISO_LOCAL_DATE));
        json.put("time", dateTime.format(DateTimeFormatter.ISO_LOCAL_TIME));
        json.put("origin", this.origin);
        json.put("sent", this.sent);
        json.put("received", this.received);

        return json.toString(2);  // The argument '2' is for indentation
    }

    @Override
    public String toString() {
        return String.format("CSVLine{timestamp=%s, origin=%s, clientUrl='%s', received='%s', sent='%s'}",
                timestamp, origin, clientUrl, received, sent);
    }

    // Getters
    public Instant getTimestamp() {
        return timestamp;
    }

    public String getClientUrl() {
        return clientUrl;
    }

    public String getReceived() {
        return received;
    }

    public String getSent() {
        return sent;
    }

    public Origin getOrigin() {
        return origin;
    }

    // Example usage
    public static void main(String[] args) {
        String csvLine = "1721236703530, WEB, /127.0.0.1:58639, {\"textData\":\"aa\",\"sendFormat\":\"UPER\",\"receiveFormat\":\"XML\"}, Error processing V2X message";
        CSVLine line = new CSVLine(csvLine);
        System.out.println(line);

        // Demonstrating how to access individual fields
        System.out.println("Timestamp: " + line.getTimestamp());
        System.out.println("Client URL: " + line.getClientUrl());
        System.out.println("Origin: " + line.origin);
        System.out.println("Received: " + line.getReceived());
        System.out.println("Sent: " + line.getSent());

        System.out.println(line.toJson());
    }
}
