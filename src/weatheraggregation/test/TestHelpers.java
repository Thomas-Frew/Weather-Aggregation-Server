package weatheraggregation.test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

public class TestHelpers {
    public static void swapFiles(String newFilePath, String oldFilePath) throws IOException {
        Path oldFile = Paths.get(oldFilePath);
        Path newFile = Paths.get(newFilePath);
        Files.move(newFile, oldFile, StandardCopyOption.REPLACE_EXISTING);
    }
}