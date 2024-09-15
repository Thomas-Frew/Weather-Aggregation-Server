# Tom's Weather Aggregation System

Welcome to my Weather Aggregation System and submission for Assignment 2 of *COMP SCI 3012 - Distributed Systems*.
I had a great time implementing this assignment, and I hope you can appreciate the effort put into its design and test suite.

> Note: I have tried to achieve for all marks in this assignment.
> This includes all points from Appendix A, Appendix B and bonus marks (JSON parser).

## Requirements

This project uses modern Java features are requires JDK 22 to execute. If you don't have JDK 22, please install it [here](https://www.oracle.com/au/java/technologies/downloads/#jdk22-windows). Running `java` in  your terminal should return something like:

```
java version "22.0.2" 2024-07-16
Java(TM) SE Runtime Environment (build 22.0.2+9-70)
Java HotSpot(TM) 64-Bit Server VM (build 22.0.2+9-70, mixed mode, sharing)
```

## Directory Structure

`src/weatheraggregation/` contains packages with all source code and testing files for this project:

- `aggregationserver`: The code and entry point for AggregationServer.
- `contentserver`: The code and entry points ContentServer and ReplicatedContentServer. 
- `core`: Core interfaces and classes reused throughout the system.
- `getclient`: The code and entry point for GETClient.
- `jsonparser`: The code for my custom JSON parser, CustomJsonParser.
- `test`: JUnit test files for all parts of the application.
- `test/testdata`: Data used by the tests.

## Services

This system contains four entry points.

### AggregationServer

Receives weather data from ContentServers and services that data to GETClients. Execute with the command:

```
java .\src\weatheraggregation\aggregationserver\AggregationServer.java <content_filename> <port>?
```

- `<content_filename>`: The filename that weather data will be committed to and read from. If this file does not exist, it will be created.
- `<port>`: (Optional) The port to run the AggregationServer on. If omitted, the port 4567 will be used.

Upon running this command, the AggregationServer will start running on your localhost with the designated port. 
It will listen out for HTTP requests and service GET/PUTS.
Every 30 seconds, it will purge any weather data that is 30 or more seconds old.

### GETClient

Pulls weather data from AggregationServers and prints it to stdout. Execute with the command:

```
java .\src\weatheraggregation\getclient\GETClient.java <server_hostname> <station_id>?
```

- `<server_hostname>`: The full hostname of the AggregationServer to fetch data from (in the form `ip:port`).
- `<station_id>`: (Optional) The station ID to fetch data from. If omitted, the client will instead fetch the most recent data from any station.

Upon running this command, the GETClient will start running and send a GET request to the AggregationServer every 2 seconds.
Fetched weather data or error status codes will be printed to stdout.

### ContentServer

Pushes weather data to AggregationServers from a local file. Execute with the command:

```
java .\src\weatheraggregation\contentserver\ContentServer.java <server_hostname> <content_filename>
```

- `<server_hostname>`: The full hostname of the AggregationServer to push data to (in the form `ip:port`).
- `<content_filename>`: The filename that weather data will be read and sent from.

Upon running this command, the ContentServer will start running and send a PUT request to the AggregationServer every 2 seconds.
Returned status codes will be printed to stdout.

### ReplicatedContentServer

Manages a number of ContentServers and fails over when the primary fails to send data to its AggregationServer.
```
java .\src\weatheraggregation\contentserver\ContentServer.java <content_filename> <server_hostname1> <server_hostname2> ... <server_hostnameN>  
```

- `<content_filename>`: The filename that weather data will be read and sent from.
- `<server_hostnameX>`: The full hostname of the Xth AggregationServer to fetch data to (in the form `ip:port`).

Upon running this command, the ReplicatedContentServer will elect a ContentServer as its primary and run it. 
If this server fails to send data to its AggregationServer, the next primary in the list is selected.
The selection of primaries cycles back to the front of the list if the Nth one fails.

## Test Coverage

...