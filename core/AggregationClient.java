import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public interface AggregationClient {
    void startClient();
    void processCommand();
    HttpRequest createRequest(URI uri);
    HttpResponse<String> sendRequest(HttpRequest request);
    void processResponse(HttpResponse<String> response);
}
