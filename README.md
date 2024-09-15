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

This system contains four services with executable entry points.

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

If the client fails to send data to its AggregationServer 3 consecutive times, it will shut down automatically.

### ContentServer

Pushes weather data to AggregationServers from a local file. Execute with the command:

```
java .\src\weatheraggregation\contentserver\ContentServer.java <server_hostname> <content_filename>
```

- `<server_hostname>`: The full hostname of the AggregationServer to push data to (in the form `ip:port`).
- `<content_filename>`: The filename that weather data will be read and sent from.

Upon running this command, the ContentServer will start running and send a PUT request to the AggregationServer every 2 seconds.
Returned status codes will be printed to stdout.

If the server fails to send data to its AggregationServer 3 consecutive times, it will shut down automatically.

### ReplicatedContentServer

Manages a number of ContentServers and fails over when the primary fails to send data to its AggregationServer.
```
java .\src\weatheraggregation\contentserver\ContentServer.java <content_filename> <server_hostname1> <server_hostname2> ... <server_hostnameN>  
```

- `<content_filename>`: The filename that weather data will be read and sent from.
- `<server_hostnameX>`: The full hostname of the Xth AggregationServer to fetch data to (in the form `ip:port`).

Upon running this command, the ReplicatedContentServer will elect a ContentServer as its primary and run it. 
If this server shuts down, the next primary in the list is started. 
The selection of primaries cycles back to the front of the list after the Nth one shuts down.

## Test Coverage

A lot of effort has been put into producing tests with significant coverage. 
Each file in `/src/weatheraggregation/test` contains a test suite associated with one class in the system (except `MixedTests`).

### AggregationServerTests

Tests for AggregationServer that do not involve serving GETClients or ContentServers.

- `expungeDataOnStartup`: Check that all outdated data is purged on the server's startup.
- `expungeDataRegularly`: Run the server and check that data older than 30 seconds is purged every 30 seconds.

### ContentServerTests

Tests for lone (non-replicated) ContentServers.

- `sendFirstData`: Send data to an AggregationServer for the first time, and confirm that it is committed.
- `sendRepeatedData`: Send data to an AggregationServer many times, and confirm that it is committed.
- `sendDifferentData`: Send data from different weather stations and confirm that it is committed.
- `sendDataWithoutValidFields`:  Fail to send data that lacks any weather fields and ensure the response is 500.
- `sendDataWithoutID`: Fail to send data that has some weather fields, but lacks an ID. Ensure the response is 500.
- `sendEmptyJSON`: Fail to send data that lacks any data, and ensure the response is 204.
- `regularRequestsSent`: Run the ContentServer and ensure data is pushed every 2 seconds.

### ContentServerTests

Tests for GETClients.

- `fetchSoleData`: Fetch the only entry from an AggregationServer.
- `fetchMostRecentData`: Fetch the most recent entry from an AggregationServer.
- `fetchSpecificData`: Fetch a specific (not most recent) entry from an AggregationServer.
- `fetchMissingData`: Fail to fetch a non-existent entry from an AggregationServer. Ensure the response is 404.
- `fetchNoData`: Fail to fetch a non-existent entry from an AggregationServer that has no data. Ensure the response is 404.
- `regularRequestsSent`: Run the GETClient and ensure data is fetched every 2 seconds.

### JsonParserTests

Tests for my CustomJsonParser.

- `parseOneField`: Parse a JSON object with one string field.
- `parseManyFields`: Parse a JSON object with multiple string and non-string fields.
- `parseNoFields`: Parse a JSON object with no fields.
- `parseFieldWithColon`: Parse a JSON object containing a colon within a field value.
- `parseWithWhitespace`: Parse a JSON object that has leading and trailing whitespace.
- `failParseNoLeadingCurlyBrace`: Fail to parse a JSON object missing its leading curly brace.
- `failParseNoTrailingCurlyBrace`: Fail to parse a JSON object missing its trailing curly brace.
- `failParseCommaItem`: Fail to parse a JSON object with an unexpected comma inside a field value.

### LamportTests

Tests for LamportClockImpls.

- `lamportClockStartsAtZero`: A Lamport clock starts at 0 when initialized.
- `lamportClockInternalIncrement`: Increment a Lamport clock with internal events.
- `lamportClockLoadNewTime`: A Lamport clock will update to an incoming time if that time is larger.
- `lamportClockKeepOldTime`: A Lamport clock will reject an incoming time if that time is smaller.

### ReplicatedContentServerTests

Tests for ReplicatedContentServers.

- `initialConfigurationValid`: Create a ReplicatedContentServer with one ContentServer and confirm that it is the primary.
- `noFailover`: Create a ReplicatedContentServer with two ContentServers. Both ContentServers have AggregationServers to connect to, so no failover should occur.
- `failover`: Create a ReplicatedContentServer with two ContentServers. The first ContentServer is missing its AggregationServer, causing failover.
- `doubleFailover`: Create a ReplicatedContentServer with three ContentServers. The first two ContentServers are missing their AggregationServers, causing two failovers.
- `failBack`: Create a ReplicatedContentServer with two ContentServers. All ContentServers are missing their AggregationServers, causing continued failover between the two.

## Lamport Clocks

All clients and servers are synchronised with Lamport clocks (LamportClockImpl for its implementation).
This keeps clients and servers operating in-order, as specified by the assignment page.

Notably, the Lamport time of a ContentServer's commit is stored with the data.
If weather data with the same station ID but a smaller lamport time is received, it will be rejected with a 500 error.

> This means that if data already exists in the AggregationServer, and a ContentServer is restarted, its first commit will be rejected. Don't worry!
> Future commits will not be rejected as the ContentServer will now have an up-to-date Lamport time.