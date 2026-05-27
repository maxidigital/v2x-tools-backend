package main.stats;

import a.tools.FileTools;
import a.tools.Lines;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class StatsHandler {

    private final String folder;

    public StatsHandler(String folder) {
        this.folder = folder;
    }

    public List<CSVLine> readLines() throws IOException {
        List<CSVLine> ret = new ArrayList<>();
        File _folder = new File(folder);
        if (!_folder.exists()) return ret;
        for (File f : _folder.listFiles()) {
            if (f.getName().endsWith("csv")) {
                Lines lines = FileTools.readTextFile(f.getAbsolutePath());
                for (String line : lines) {
                    try {
                        ret.add(new CSVLine(line));
                    } catch (Exception e) {
                    }
                }
            }
        }
        return ret;
    }
}
