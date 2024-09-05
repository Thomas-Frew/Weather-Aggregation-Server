import org.json.simple.JSONObject;
public record Message (int lamportTime, MessageType type, JSONObject payload) { }