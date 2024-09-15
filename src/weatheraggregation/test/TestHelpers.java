package weatheraggregation.test;

import weatheraggregation.core.FileHelpers;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

public class TestHelpers {
    public static final int PORT = 2567;
    public static final String IP = "172.29.96.1";
    public static final String HOSTNAME = IP+":"+PORT;
    public static final String DIRECTORY = "src/weatheraggregation/test/";
    public static final String WEATHER_DATA_FILENAME = DIRECTORY + "test_weather_data.txt";

    public static void swapFiles(String newFilePath, String oldFilePath) throws IOException {
        FileHelpers.tryCreateFile(oldFilePath);
        Path oldFile = Paths.get(oldFilePath);
        Path newFile = Paths.get(newFilePath);
        Files.copy(newFile, oldFile, StandardCopyOption.REPLACE_EXISTING);
    }
}