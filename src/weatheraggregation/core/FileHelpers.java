package weatheraggregation.core;

import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;

public class FileHelpers {

    public static final String DELIMITER = ":";
    public static final String TMP_FILENAME = "src/weatheraggregation/aggregationserver/weather_data.tmp";

    public static void tryCreateFile(String filename) {
        Path oldFilePath = Paths.get(filename);
        if (Files.notExists(oldFilePath)) {
            try {
                Files.createFile(oldFilePath);
            } catch (IOException e) {
                System.err.println("Couldn't create weather data file.");
            }
        }
    }
    public static String readContentFile(String filename) throws IOException {
        HashMap<String, String> jsonMap = new HashMap<>(); // Preserve order
        String line;

        BufferedReader reader = new BufferedReader(new FileReader(filename));

        while ((line = reader.readLine()) != null) {
            String[] parts = line.split(DELIMITER, 2);
            if (parts.length == 2) jsonMap.put(parts[0].trim(), parts[1].trim());
        }

        JSONObject jsonObject = new JSONObject(jsonMap);
        return jsonObject.toJSONString();
    }

    public static String readWeatherFile(String filename, String searchedStation) throws IOException, ParseException {
        String line;
        try (BufferedReader reader = Files.newBufferedReader(Paths.get(filename))) {
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

    public static String readWeatherFileFirst(String filename) throws IOException, ParseException {
        try (BufferedReader reader = Files.newBufferedReader(Paths.get(filename))) {
            String line = reader.readLine();
            if (line != null) {
                return line.split(DELIMITER, 4)[3];
            } else {
                return null;
            }
        }
    }

    public static List<String[]> readWeatherFileAll(String filename) throws IOException, ParseException {
        List<String[]> entries = new ArrayList<>();
        String line;
        try (BufferedReader reader = Files.newBufferedReader(Paths.get(filename))) {
            while ((line = reader.readLine()) != null) {
                entries.add(line.split(DELIMITER, 4));
            }
        }
        return entries;
    }


    public static boolean writeAndSwapWeatherFile(String filename, String stationId, int realTime, int lamportTime, String weatherString) throws IOException, ParseException, IllegalStateException {
        // Clone file
        Path originalFilePath = Paths.get(filename);
        Path tempFilePath = Paths.get(TMP_FILENAME);
        Files.copy(originalFilePath, tempFilePath, StandardCopyOption.REPLACE_EXISTING);

        List<String> entries = new ArrayList<>();
        String newEntry = stationId + DELIMITER + realTime + DELIMITER + lamportTime + DELIMITER + weatherString;
        entries.add(newEntry.trim());

        boolean replaced = false;
        try (BufferedReader reader = new BufferedReader(new FileReader(tempFilePath.toFile()))) {
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

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(tempFilePath.toFile()))) {
            for (String entry : entries) {
                writer.write(entry);
                writer.newLine();
            }
        }

        // Swap files
        Files.move(tempFilePath, originalFilePath, StandardCopyOption.REPLACE_EXISTING);

        // Return whether the file was replaced
        return replaced;
    }

    public static void expungeAndSwapWeatherFile(String filename, int realTime) throws IOException, IllegalStateException {
        // Clone file
        Path originalFilePath = Paths.get(filename);
        Path tempFilePath = Paths.get(TMP_FILENAME);
        Files.copy(originalFilePath, tempFilePath, StandardCopyOption.REPLACE_EXISTING);

        List<String> entries = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(tempFilePath.toFile()))) {
            String entry;
            while ((entry = reader.readLine()) != null) {
                if (entry.trim().isEmpty()) continue;

                int entryRealTime = Integer.parseInt(entry.split(DELIMITER, 4)[1]);
                if (realTime - entryRealTime < 30) {
                    entries.add(entry.trim());
                }
            }
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(tempFilePath.toFile()))) {
            for (String entry : entries) {
                writer.write(entry);
                writer.newLine();
            }
        }

        // Swap files
        Files.move(tempFilePath, originalFilePath, StandardCopyOption.REPLACE_EXISTING);
    }
}
