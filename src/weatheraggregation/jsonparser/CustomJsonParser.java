package weatheraggregation.jsonparser;
import java.util.HashMap;
import java.util.Map;

public class CustomJsonParser {

    private static final String FIELD_DELIMITER = ",";
    private static final String ITEM_DELIMITER = ":";
    private static final String QUOTE = "\"";

    /**
     * Convert a JSON string to a JSON object.
     * @param jsonString The JSON string.
     * @return The returned JSON object.
     * @throws CustomParseException The JSON string may be invalid and raise an exception.
     */
    public static Map<String, String> stringToJson(String jsonString) throws CustomParseException {
        Map<String, String> jsonObject = new HashMap<>();

        // Trim all leading and trailing whitespace
        jsonString = jsonString.trim();

        // If this is a JSON object...
        if (jsonString.startsWith("{") && jsonString.endsWith("}")) {

            // Trim the leading and trailing curly braces
            jsonString = jsonString.substring(1, jsonString.length() - 1).trim();
            if (jsonString.isEmpty()) return jsonObject;

            // Pairs are delimited by comma
            String[] pairs = jsonString.split(FIELD_DELIMITER);

            for (String pair : pairs) {

                // Items are delimited by colon
                String[] keyValue = pair.split(ITEM_DELIMITER, 2);

                // If there are exactly two items, we have a pair!
                if (keyValue.length == 2) {
                    // Remove all quotes, we don't need them
                    String key = keyValue[0].trim().replaceAll(QUOTE, "");
                    String value = keyValue[1].trim().replaceAll(QUOTE, "");
                    jsonObject.put(key, value);
                } else {
                    throw new CustomParseException("Field is not in the form \"key:value\"");
                }
            }
        } else {
            throw new CustomParseException("String is not a JSON object");
        }
        return jsonObject;
    }

    /**
     * Convert a JSON object to a JSON string.
     * @param jsonObject The JSON object.
     * @return The returned JSON string.
     */
    public static String jsonToString(Map<String, String> jsonObject) {
        StringBuilder jsonString = new StringBuilder();

        // Append the leading curly brace
        jsonString.append("{");

        // Write each key-value pair to the JSON string
        for (Map.Entry<String, String> entry : jsonObject.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            jsonString.append(QUOTE);
            jsonString.append(key);
            jsonString.append(QUOTE);
            jsonString.append(ITEM_DELIMITER);
            jsonString.append(QUOTE);
            jsonString.append(value);
            jsonString.append(QUOTE);
            jsonString.append(FIELD_DELIMITER);
        }

        // Append the tracking curly brace
        jsonString.append("}");
        return jsonString.toString();
    }
}