package weatheraggregation.core;

import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;

public class FileHelpers {

    public static final String DELIMITER = ":";
    public static final String TMP_FILENAME = "src/weatheraggregation/aggregationserver/weather_data.tmp";

    public static String readContentFile(String filePath) throws IOException {

        LinkedHashMap<String, String> jsonMap = new LinkedHashMap<>(); // Preserve order
        String line;

        BufferedReader reader = new BufferedReader(new FileReader(filePath));

        while ((line = reader.readLine()) != null) {
            String[] parts = line.split(DELIMITER, 2);
            if (parts.length == 2) jsonMap.put(parts[0].trim(), parts[1].trim());
        }

        JSONObject jsonObject = new JSONObject(jsonMap);
        return jsonObject.toJSONString();
    }

    public static String readWeatherFile(String filePath, String searchedStation) throws IOException, ParseException {
        String line;
        try (BufferedReader reader = Files.newBufferedReader(Paths.get(filePath))) {
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(DELIMITER, 4);
                String station = parts[0];
                if (Objects.equals(station, searchedStation)) {
                    String jsonString = parts[3];
                    JSONObject jsonObject = ConversionHelpers.stringToJSON(jsonString);
                    return jsonObject.toJSONString();
                }
            }
        }
        return null;
    }

    public static String readWeatherFile(String filePath) throws IOException, ParseException {
        try (BufferedReader reader = Files.newBufferedReader(Paths.get(filePath))) {
            String line = reader.readLine();
            if (line != null) {
                String jsonString = line.split(DELIMITER, 4)[3];
                JSONObject jsonObject = ConversionHelpers.stringToJSON(jsonString);
                return jsonObject.toJSONString();
            } else {
                return null;
            }
        }
    }

    public static boolean writeAndSwapWeatherFile(String filePath, String stationId, int realTime, int lamportTime, String weatherString) throws IOException, ParseException, IllegalStateException {
        // Clone file
        Path originalFile = Paths.get(filePath);
        Path tempFile = Paths.get(TMP_FILENAME);
        Files.copy(originalFile, tempFile, StandardCopyOption.REPLACE_EXISTING);

        List<String> entries = new ArrayList<>();
        String newEntry = stationId + DELIMITER + realTime + DELIMITER + lamportTime + DELIMITER + weatherString;
        entries.add(newEntry.trim());

        boolean replaced = false;
        try (BufferedReader reader = new BufferedReader(new FileReader(tempFile.toFile()))) {
            String entry;
            while ((entry = reader.readLine()) != null) {
                if (entry.trim().isEmpty()) continue;

                String station = entry.split(DELIMITER, 4)[0];

                if (!Objects.equals(station, stationId)) {
                    entries.add(entry.trim());
                } else {
                    /*
                     DEBUG: Usually we'd check to see if the entry is up-to-date
                     int otherTime = Integer.parseInt(entry.split(DELIMITER, 4)[2]);
                     if (lamportTime <= otherTime) throw new IllegalStateException("Record is out of date");
                     */
                    replaced = true;
                }
            }
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile.toFile()))) {
            for (String entry : entries) {
                writer.write(entry);
                writer.newLine();
            }
        }

        // Swap files
        Files.move(tempFile, originalFile, StandardCopyOption.REPLACE_EXISTING);

        // Return whether the file was replaced
        return replaced;
    }

    public static void expungeAndSwapWeatherFile(String filePath, int realTime) throws IOException, IllegalStateException {
        // Clone file
        Path originalFile = Paths.get(filePath);
        Path tempFile = Paths.get(TMP_FILENAME);
        Files.copy(originalFile, tempFile, StandardCopyOption.REPLACE_EXISTING);

        List<String> entries = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(tempFile.toFile()))) {
            String entry;
            while ((entry = reader.readLine()) != null) {
                if (entry.trim().isEmpty()) continue;

                int entryRealTime = Integer.parseInt(entry.split(DELIMITER, 4)[1]);
                if (realTime - entryRealTime <= 30) {
                    entries.add(entry.trim());
                }
            }
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile.toFile()))) {
            for (String entry : entries) {
                writer.write(entry);
                writer.newLine();
            }
        }

        // Swap files
        Files.move(tempFile, originalFile, StandardCopyOption.REPLACE_EXISTING);
    }
}
