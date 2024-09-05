import com.sun.net.httpserver.Headers;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConversionHelpers {

    public static JSONParser jsonParser = new JSONParser();

    public static String readContentFile(String filePath) throws IOException {
        Map<String, String> jsonMap = new HashMap<>();
        String line;

        // Read file into map
        BufferedReader reader = new BufferedReader(new FileReader(filePath));

        while ((line = reader.readLine()) != null) {
            String[] parts = line.split(":", 2);
            if (parts.length == 2) jsonMap.put(parts[0].trim(), parts[1].trim());
        }

        JSONObject jsonObject = new JSONObject(jsonMap);
        return jsonObject.toJSONString();
    }

    public static String readJSONFile(String filePath) throws IOException, ParseException {
        FileReader reader = new FileReader(filePath);
        JSONObject jsonObject = (JSONObject) jsonParser.parse(reader);
        return jsonObject.toJSONString();
    }

    public static Map<String, String> requestHeadersToMap(Headers headers) {   // Extract headers
        Map<String, String> headersMap = new HashMap<>();
        for (Map.Entry<String, List<String>> header : headers.entrySet()) {
            String value = String.join(", ", header.getValue());
            headersMap.put(header.getKey(), value);
        }
        return headersMap;
    }

    public static String requestBodyToString(InputStream inputStream) throws IOException {
        StringBuilder stringBuilder = new StringBuilder();
        String line;

        // Read stream into string
        try (BufferedReader br = new BufferedReader(new InputStreamReader(inputStream))) {
            while ((line = br.readLine()) != null) {
                stringBuilder.append(line).append('\n');
            }
        }

        return stringBuilder.toString();
    }
}
