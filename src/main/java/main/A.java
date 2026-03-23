package main;

import a.tools.DLRLogger;
import a.tools.FileTools;
import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class A {

    private static String counterFileName;
    private static String logFileName;
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
    private static final DateTimeFormatter fileNameFormatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    private static boolean log = true;
    private static LocalDate currentDate;

    static {
        updateFileNames();
    }

    private static void updateFileNames() {
        LocalDateTime currentDateTime = LocalDateTime.now();
        currentDate = currentDateTime.toLocalDate();
        String dateTime = currentDateTime.format(fileNameFormatter);

        File counterDir = new File("counter");
        counterDir.mkdirs();
        counterFileName = new File(counterDir, dateTime + "_counter.csv").getAbsolutePath();

        File logDir = new File("log");
        logDir.mkdirs();
        logFileName = new File(logDir, dateTime + "_output.txt").getAbsolutePath();

        try {
            FileTools.appendToTextFile(counterFileName, "timestamp, ip, received, sent");
        } catch (IOException ex) {
            Logger.getLogger(A.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private static void checkAndUpdateFiles() {
        LocalDate now = LocalDate.now();
        if (!now.equals(currentDate)) {
            updateFileNames();
        }
    }

    public static void counterLogger(String text, Object... args) {
        checkAndUpdateFiles();
        try {
            FileTools.appendToTextFile(counterFileName, String.format(text, args));
        } catch (IOException ex) {
            Logger.getLogger(A.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static void pt(String text, Object... args) {
        p(text, args);
    }

    public static void p(String text, Object... args) {
        checkAndUpdateFiles();
        DLRLogger.info(null, text, args);
        if (log) {
            LocalDateTime currentDateTime = LocalDateTime.now();
            String dateTime = currentDateTime.format(formatter);
            String info = dateTime + ": " + String.format(text, args);
            try {
                FileTools.appendToTextFile(logFileName, info);
            } catch (IOException ex) {
                Logger.getLogger(A.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    public static void setLog(boolean log) {
        A.log = log;
    }
}
