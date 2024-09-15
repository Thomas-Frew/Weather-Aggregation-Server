package weatheraggregation.test;

import weatheraggregation.core.FileHelpers;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

public class TestHelpers {
    public static final int PORT = 2567;

    // Resolve localhost IP at runtime
    public static final String IP;
    static {
        try {
            IP = InetAddress.getLocalHost().toString().split("/", 2)[1];
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }

    public static final String HOSTNAME = IP+":"+PORT;
    public static final String DIRECTORY = "src/weatheraggregation/test/";
    public static final String WEATHER_DATA_FILENAME = DIRECTORY + "test_weather_data.txt";

    public static void swapFiles(String newFilename, String oldFilename) throws IOException {
        FileHelpers.tryCreateFile(oldFilename);
        Path oldFilePath = Paths.get(oldFilename);
        Path newFilePath = Paths.get(newFilename);
        Files.copy(newFilePath, oldFilePath, StandardCopyOption.REPLACE_EXISTING);
    }
}