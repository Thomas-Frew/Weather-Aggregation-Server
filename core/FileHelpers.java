import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;

public class FileHelpers {

    public static final String DELIMITER = ":";
    public static int FILE_COMMIT_ATTEMPTS = 10;

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
        BufferedReader reader = new BufferedReader(new FileReader(filePath));

        while ((line = reader.readLine()) != null) {
            String[] parts = line.split(":", 4);
            String station = parts[0];
            if (Objects.equals(station, searchedStation)) {
                String jsonString = parts[3];
                JSONObject jsonObject = ConversionHelpers.stringToJSON(jsonString);
                return jsonObject.toJSONString();
            }
        }
        return "Not Found";
    }

    public static String readWeatherFile(String filePath) throws IOException, ParseException {
        return "TBA";
    }

    public static void trySwapWeatherFile(String filePath, String stationId, int timestamp, int eventTime, String weatherString) throws IOException, ParseException {
        // Clone file
        Path originalFile = Paths.get(filePath);
        Path tempFile = Paths.get("aggregation-server/weather_data.tmp");
        Files.copy(originalFile, tempFile, StandardCopyOption.REPLACE_EXISTING);

        List<String> entries = new ArrayList<>();
        String newEntry = stationId + DELIMITER + timestamp + DELIMITER + eventTime + DELIMITER + weatherString;
        entries.add(newEntry.trim());

        try (BufferedReader reader = new BufferedReader(new FileReader(tempFile.toFile()))) {
            String entry;
            while ((entry = reader.readLine()) != null) {
                if (entry.trim().isEmpty()) continue;

                String station = entry.split(":", 4)[0];
                if (!Objects.equals(station, stationId)) {
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
