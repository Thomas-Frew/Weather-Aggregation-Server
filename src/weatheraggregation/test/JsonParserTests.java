package weatheraggregation.test;

import org.junit.Test;
import weatheraggregation.aggregationserver.AggregationServer;
import weatheraggregation.contentserver.ContentServer;
import weatheraggregation.core.FileHelpers;
import weatheraggregation.jsonparser.CustomJsonParser;
import weatheraggregation.jsonparser.CustomParseException;

import java.io.IOException;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;

public class JsonParserTests {
    /**
     Parse a JSON object with one string field.
     */
    @Test
    public void parseOneField() throws CustomParseException {
        // Define test JSON string
        String jsonString = "{\"name\":\"Maddie Frew\"}";

        // Convert to JSON
        Map<String, String> jsonObject = CustomJsonParser.stringToJson(jsonString);

        // Test the correct count and value of fields have parsed
        assertEquals(1, jsonObject.size());
        assertEquals("Maddie Frew", jsonObject.get("name"));
    }

    /**
     Parse a JSON object with many string and non-string fields.
     */
    @Test
    public void parseManyFields() throws CustomParseException {
        // Define test JSON string
        String jsonString = "{\"name\":\"Snow Yapper\",\"age\":1,\"color\":\"white\"}";

        // Convert to JSON
        Map<String, String> jsonObject = CustomJsonParser.stringToJson(jsonString);

        // Test the correct count and value of fields have parsed
        assertEquals(3, jsonObject.size());
        assertEquals("Snow Yapper", jsonObject.get("name"));
        assertEquals("1", jsonObject.get("age"));
        assertEquals("white", jsonObject.get("color"));

    }

    /**
     Parse a JSON object with no fields.
    */
    @Test
    public void parseNoFields() throws CustomParseException {
        // Define test JSON string
        String jsonString = "{}";

        // Convert to JSON
        Map<String, String> jsonObject = CustomJsonParser.stringToJson(jsonString);

        // Test the correct count and value of fields have parsed
        assertEquals(0, jsonObject.size());
    }

    /**
     Parse a JSON object with a colon in the field.
     */
    @Test
    public void parseFieldWithColon() throws CustomParseException {
        // Define test JSON string
        String jsonString = "{\"objective\":\"BSides:Canberra\"}";

        // Convert to JSON
        Map<String, String> jsonObject = CustomJsonParser.stringToJson(jsonString);

        // Test the correct count and value of fields have parsed
        assertEquals(1, jsonObject.size());
        assertEquals("BSides:Canberra", jsonObject.get("objective"));
    }

    /**
     Parse a JSON object with whitespace at the start and end.
     */
    @Test
    public void parseWithWhitespace() throws CustomParseException {
        // Define test JSON string
        String jsonString = "    {\"two\":2}  ";

        // Convert to JSON
        Map<String, String> jsonObject = CustomJsonParser.stringToJson(jsonString);

        // Test the correct count and value of fields have parsed
        assertEquals(1, jsonObject.size());
        assertEquals("2", jsonObject.get("two"));
    }

    /**
     Fail to parse a JSON object that is missing its leading curly brace.
     */
    @Test(expected = CustomParseException.class)
    public void failParseNoLeadingCurlyBrace() throws CustomParseException {
        // Define test JSON string
        String jsonString = "\"key\":\"value\"}";

        // Fail to convert to JSON
        CustomJsonParser.stringToJson(jsonString);
    }

    /**
     Fail to parse a JSON object that is missing its trailing curly brace.
     */
    @Test(expected = CustomParseException.class)
    public void failParseNoTrailingCurlyBrace() throws CustomParseException {
        // Define test JSON string
        String jsonString = "{\"key\":\"value\"";

        // Fail to convert to JSON
        CustomJsonParser.stringToJson(jsonString);
    }

    /**
     Fail to parse a JSON object that has a comma in the line. This is expected behaviour.
     */
    @Test(expected = CustomParseException.class)
    public void failParseCommaItem() throws CustomParseException {
        // Define test JSON string
        String jsonString = "{\"key\":\"value1,value2\"}";

        // Fail to convert to JSON
        CustomJsonParser.stringToJson(jsonString);
    }

    /**
     Parse a simple JSON object (1 key-value pair) into a string.
     */
    @Test
    public void parseSimpleString() {
        // Define test JSON object
        Map<String, String> jsonObject = new HashMap<>();
        jsonObject.put("key","value1");

        // Convert to and check it is correct
        String jsonString = CustomJsonParser.jsonToString(jsonObject);
        assertEquals("{\"key\":\"value1\",}", jsonString);
    }

    /**
     Parse a complex JSON object (3 key-value pairs, whitespace, colons) into a string.
     */
    @Test
    public void parseComplexString() {
        // Define test JSON object
        Map<String, String> jsonObject = new HashMap<>();
        jsonObject.put("keyA"," helloA ");
        jsonObject.put("keyB","hey:B");
        jsonObject.put("key:C","hi C");


        // Convert to and check it is correct
        String jsonString = CustomJsonParser.jsonToString(jsonObject);
        assertEquals("{\"keyA\":\" helloA \",\"keyB\":\"hey:B\",\"key:C\":\"hi C\",}", jsonString);
    }

    /**
     Parse an empty JSON object into a string.
     */
    @Test
    public void parseEmptyString() {
        // Define test JSON object
        Map<String, String> jsonObject = new HashMap<>();

        // Convert to and check it is correct
        String jsonString = CustomJsonParser.jsonToString(jsonObject);
        assertEquals("{}", jsonString);
    }
}