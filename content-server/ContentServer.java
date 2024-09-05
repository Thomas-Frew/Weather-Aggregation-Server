import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ContentServer implements AggregationClient {

    @Override
    public void startClient()  {
    }

    @Override
    public void processCommand() {
    }

    @Override
    public HttpRequest createRequest(URI uri) {
    }

    @Override
    public HttpResponse<String> sendRequest(HttpRequest request) {
    }

    @Override
    public void processResponse(HttpResponse<String> response) {
    }
}
