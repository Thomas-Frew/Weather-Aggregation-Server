package weatheraggregation.jsonparser;

/**
 * An exception for the CustomJsonParser.
 */
public class CustomParseException extends Exception {
    /**
     * Raise a CustomParseException with no message.
     */
    public CustomParseException() {
        super();
    }

    /**
     * Raise a CustomParseException with a message.
     * @param message The message to raise.
     */
    public CustomParseException(String message) {
        super(message);
    }
}
