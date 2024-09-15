package weatheraggregation.core;

import weatheraggregation.jsonparser.CustomJsonParser;
import weatheraggregation.jsonparser.CustomParseException;

import java.io.*;
import java.util.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

public class FileHelpers {

    public static final String ITEM_DELIMITER = ":";
    public static final String TMP_FILENAME = "src/weatheraggregation/aggregationserver/weather_data.tmp";

    /**
     * Try to create a file with path filename.
     * @param filename The filename of the path to create.
     */
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

    /**
     * Read the content from a content server file as JSON.
     * @param filename The filename of the content server file.
     * @return The JSON string contained by the file.
     * @throws IOException The file may not exist.
     */
    public static String readContentFile(String filename) throws IOException {
        HashMap<String, String> jsonMap = new HashMap<>(); // Preserve order
        String line;

        BufferedReader reader = new BufferedReader(new FileReader(filename));

        while ((line = reader.readLine()) != null) {
            String[] parts = line.split(ITEM_DELIMITER, 2);
            if (parts.length == 2) jsonMap.put(parts[0].trim(), parts[1].trim());
        }

        return CustomJsonParser.jsonToString(jsonMap);
    }

    /**
     * Read weather data for a specific station ID from a weather data file, as a JSON string.
     * @param filename The filename of the weather data file.
     * @param searchedStation The ID of the weather station of extract data from.
     * @return The JSON string contained by the file.
     * @throws IOException The file may not exist.
     * @throws CustomParseException The read JSON might not be valid.
     */
    public static String readWeatherFile(String filename, String searchedStation) throws IOException, CustomParseException {
        String line;
        try (BufferedReader reader = Files.newBufferedReader(Paths.get(filename))) {
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(ITEM_DELIMITER, 4);
                String station = parts[0];
                if (Objects.equals(station, searchedStation)) {
                    return parts[3];
                }
            }
        }
        return null;
    }

    /**
     * Read most recent weather data from weather data file, as a JSON string.
     * @param filename The filename of the weather data file.
     * @return The JSON string contained by the file.
     * @throws IOException The file may not exist.
     * @throws CustomParseException The read JSON might not be valid.
     */
    public static String readWeatherFileFirst(String filename) throws IOException, CustomParseException {
        try (BufferedReader reader = Files.newBufferedReader(Paths.get(filename))) {
            String line = reader.readLine();
            if (line != null) {
                return line.split(ITEM_DELIMITER, 4)[3];
            } else {
                return null;
            }
        }
    }

    /**
     * Read all recent weather data from weather data file, as a list of string entries.
     * @param filename The filename of the weather data file.
     * @return A list of entries contained by the file.
     * @throws IOException The file may not exist.
     */
    public static List<String[]> readWeatherFileAll(String filename) throws IOException {
        List<String[]> entries = new ArrayList<>();
        String line;
        try (BufferedReader reader = Files.newBufferedReader(Paths.get(filename))) {
            while ((line = reader.readLine()) != null) {
                entries.add(line.split(ITEM_DELIMITER, 4));
            }
        }
        return entries;
    }

    /**
     * Write a new entry to a weather file.
     * @param filename The path of the weather file.
     * @param stationId The station ID of the new entry.
     * @param realTime The real timestamp of the new entry.
     * @param lamportTime The lamport time of the new entry.
     * @param jsonString The JSON data of the new entry.
     * @return Whether an existing entry was overwritten.
     * @throws IOException The file may not exist.
     * @throws IllegalStateException The file may contain a newer version of the data we want to write.
     */
    public static boolean writeAndSwapWeatherFile(String filename, String stationId, int realTime, int lamportTime, String jsonString) throws IOException, IllegalStateException {
        // Clone file
        Path originalFilePath = Paths.get(filename);
        Path tempFilePath = Paths.get(TMP_FILENAME);
        Files.copy(originalFilePath, tempFilePath, StandardCopyOption.REPLACE_EXISTING);

        List<String> entries = new ArrayList<>();
        String newEntry = stationId + ITEM_DELIMITER + realTime + ITEM_DELIMITER + lamportTime + ITEM_DELIMITER + jsonString;
        entries.add(newEntry.trim());

        boolean replaced = false;
        try (BufferedReader reader = new BufferedReader(new FileReader(tempFilePath.toFile()))) {
            String entry;
            while ((entry = reader.readLine()) != null) {
                if (entry.trim().isEmpty()) continue;

                String station = entry.split(ITEM_DELIMITER, 4)[0];

                if (!Objects.equals(station, stationId)) {
                    entries.add(entry.trim());
                } else {
                    // Reject the new entry if it is out of date
                    int otherTime = Integer.parseInt(entry.split(ITEM_DELIMITER, 4)[2]);
                    System.out.println(otherTime + " " + lamportTime);
                    if (lamportTime <= otherTime) throw new IllegalStateException("Record is out of date");
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

    /**
     * Expunge outdated data from a weather file.
     * @param filename The path of the weather file.
     * @param realTime The real timestamp to compare against.
     * @throws IOException The file may not exist.
     * @throws IllegalStateException The file may contain a newer version of the data we want to write.
     */
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

                int entryRealTime = Integer.parseInt(entry.split(ITEM_DELIMITER, 4)[1]);
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
