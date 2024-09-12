import com.sun.net.httpserver.Headers;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.*;
import java.util.*;

public class ConversionHelpers {

    public static JSONObject stringToJSON(String jsonString) throws ParseException {
        JSONParser parser = new JSONParser();
        return (JSONObject) parser.parse(jsonString);
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
