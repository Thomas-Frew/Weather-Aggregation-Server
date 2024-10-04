package weatheraggregation.core;

import com.sun.net.httpserver.Headers;

import java.io.*;
import java.util.*;

public class ConversionHelpers {

    /**
     * Convert a Headers object to a map of headers.
     * @param headers The Headers object.
     * @return The returned map of headers.
     */
    public static Map<String, String> requestHeadersToMap(Headers headers) {   // Extract headers
        Map<String, String> headersMap = new HashMap<>();
        for (Map.Entry<String, List<String>> header : headers.entrySet()) {
            String value = String.join(", ", header.getValue());
            headersMap.put(header.getKey(), value);
        }
        return headersMap;
    }

    /**
     * Convert an InputStream object to a String of its contents.
     * @param inputStream The InputStream object.
     * @return The returned String of its contents.
     * @throws IOException The InputStream might throw an exception.
     */
    public static String inputStreamToString(InputStream inputStream) throws IOException {
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

    /**
     * Convert a Headers object to a map of headers.
     * @param canonicalHostname The raw hostname string.
     * @return The hostname without a protocol.
     */
    public static String canonicalHostnameToHostname(String canonicalHostname) {   // Extract headers
        return canonicalHostname.replaceFirst("^https?://", "");
    }
}
