# Assignment2 Weather Aggregation

This project is a Spring Boot-based microservice architecture that provides functionality for managing weather data aggregation and client interactions through a RESTful API. The project contains three main modules: Aggregation Server, Content Server, and Client.

## Project Structure

The project contains several key components:

- **Aggregation Module**: Handles data aggregation and provides endpoints for storing and querying weather data.
- **Content Module**: Manages communication with the aggregation server to update and retrieve weather data.
- **Client Module**: Interacts with the aggregation server for weather data requests.


### Running the Project

1. **Navigate to the project directory**:

    ```sh
    cd /path/to/DistributedSystemWork
    ```

2. **Compile the Project**:

    ```sh
    mvn compile
    ```

3. **Run Unit Tests**:

    ```sh
   cd path/to/DistributedSystemWork/aggregation-server
    mvn test
   //Both client and content-server tests depend on aggregation-server. 
   cd path/to/DistributedSystemWork/content-server
   mvn test
   // TO test ClientTest. 1 executing testCapacity() in AggregationServiceTest,2 start AggregationService otherwise the data will be empty.
   cd path/to/DistributedSystemWork/client
   mvn test
    ```

4. **Start the Aggregation Server**:
   ```sh
   cd path/to/DistributedSystemWork/aggregation-server
   mvn exec:java -Dexec.mainClass="org.adelaide.AggregationApplication"
   ```
5. **Start the Content Server**:

    ```sh
   cd path/to/DistributedSystemWork/content-server
   mvn exec:java -Dexec.mainClass="org.adelaide.ContentApplication"
    ```

6. **Start the Client**:

    ```sh
    cd path/to/DistributedSystemWork/client
   mvn exec:java -Dexec.mainClass="org.adelaide.ClientApplication"
    ```


### Modules Overview

#### 1. Aggregation Module

##### AggregationController.java

- **Base Path**: `/Agg`
- **Endpoints**:
  - `/Agg/hello` (GET): A simple endpoint to test connectivity with the Aggregation Server.
  - `/Agg/saveOrUpdateWeatherInfo` (PUT): Accepts JSON weather data and stores or updates it in the aggregation system.
  - `/Agg/queryWeatherById` (GET): Queries weather data by a specific ID.

##### AggregationService.java

- Handles the core business logic of data aggregation.
- Provides methods for saving, updating, and querying weather data.
- Ensures data consistency and handles edge cases such as missing or duplicate data.

#### 2. Content Module

##### ContentController.java

- **Base Path**: `/Content`
- **Endpoints**:
  - `/Content/checkClientToAgg` (GET): Checks connectivity with the Aggregation Server by sending a request to its `/hello` endpoint.
  - `/Content/saveOrUpdateWeatherInfo` (PUT): Sends weather data to the Aggregation Server for updating or saving, and synchronizes the Lamport clock to ensure data consistency.

- **Lamport Clock**: The ContentController uses a Lamport clock to manage the logical time of distributed events. It increments the clock before each request and updates it based on the responses received.
- **Retry Mechanism**: The `@Retryable` annotation is used in the ContentController to retry operations when certain exceptions occur, providing fault tolerance to network issues.

#### 3. Client Module

##### ClientController.java

- **Base Path**: `/Client`
- **Endpoints**:
  - `/Client/checkConnectionToAgg` (GET): Sends a request to the Aggregation Server to verify connectivity.
  - `/Client/getWeatherById` (GET): Retrieves weather data by ID from the Aggregation Server.
  - Uses `TestRestTemplate` for making REST API calls to communicate with the Aggregation Server.

## Functionalities

### Basic Functionality

- **Text Sending Works**: Verified by tests like `testValidGetRequestWithoutStationId` and `testPutRequest`.
- **Client, Server, and Content Server Communication**: Verified by `testServerStartup` and `testPutRequest`.
- **PUT Operation Works for One Content Server**: Verified by `testPutRequest` and `testSubsequentPutRequests`.
- **GET Operation Works for Many Read Clients**: Verified by `testGetRequestAfterPut` and `testConcurrentRequests`.
- **Data Expiration After 30s**: Verified by `testDataExpirationAfterTimeout`.
- **Retry on Errors (Server Not Available, etc.)**: Verified by `testFailureTolerance` and `testLamportClockIncrementOnRetry`.

### Full Functionality

- **Lamport Clocks Implemented**: Verified by `testLamportClockUpdateOnReceive`, `testLamportClockIncrementOnPut`, and `testLamportClockSynchronization`.
- **All Error Codes Implemented**: Verified by `testNotFoundResponse`, `testErrorResponse`, `testInvalidRequestMethod`, and `testInvalidRequestFormat`.
- **Content Servers are Replicated and Fault Tolerant**: Verified by `testFaultToleranceWithMultipleContentServers`.

### Bonus Functionality (10 points)

- **Custom JSON Parsing**: Verified by `JsonTest`.

### Prerequisites

- **Java Development Kit (JDK) 17+**.
- **Apache Maven**.

### Running Multiple Instances of Content Server

The `ContentApplication` class in the Content module provides an example of running multiple instances on different ports. This helps in simulating distributed environments for testing synchronization.

```java
public static void main(String[] args) {
    ConfigurableApplicationContext instance1 = startInstance(8081, "instance1");
    ConfigurableApplicationContext instance2 = startInstance(8082, "instance2");
    ConfigurableApplicationContext instance3 = startInstance(8083, "instance3");
}
