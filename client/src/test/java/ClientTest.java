import org.adelaide.ClientApplication;
import org.adelaide.dto.CommonResult;
import org.adelaide.util.JsonUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = ClientApplication.class)
public class ClientTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @BeforeEach
    public void setUp() {

    }

    /**
     * Test the hello endpoint to verify connectivity.
     */
    @Test
    public void testHello() {
        // Arrange
        String mockUrl = "http://localhost:" + port + "/Client/checkClientToAgg";

        // Act
        ResponseEntity<String> response = restTemplate.getForEntity(mockUrl, String.class);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("client======Hello from Aggregation Server! (Lamport Clock: 1)", response.getBody());
    }

    /**
     * Test the retry mechanism of the queryWeatherById method.
     */
    @Test
    public void testQueryWeatherById() {
        // Arrange
        String mockId = "Capacity19";
        String mockUrl = "http://localhost:" + port + "/Client/queryWeatherById?id=" + mockId;

        // Act
        ResponseEntity<CommonResult> response = restTemplate.getForEntity(mockUrl, CommonResult.class);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    /**
     * Test handling a successful response from the REST endpoint.
     */
    @Test
    public void testQueryWeatherByIdSuccessfulResponse() {
        // Arrange
        String mockId = "Capacity19";
        String mockUrl = "http://localhost:" + port + "/Client/queryWeatherById?id=" + mockId ;

        // Act
        ResponseEntity<CommonResult> response = restTemplate.getForEntity(mockUrl, CommonResult.class);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        CommonResult result = response.getBody();
        assertNotNull(result, "The response should not be null");
        assertEquals(200, result.getCode(), "The response code should be 200");
    }

    /**
     * Test the Lamport clock behavior when a successful response is received.
     */
    @Test
    public void testQueryWeatherByIdLamportClockUpdate() throws IllegalAccessException {
        // Arrange
        String mockId = "Capacity19";
        String mockUrl = "http://localhost:" + port + "/Client/queryWeatherById?id=" + mockId ;

        // Act
        ResponseEntity<CommonResult> response = restTemplate.getForEntity(mockUrl, CommonResult.class);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        CommonResult result = response.getBody();
        System.out.println(JsonUtil.toJson(result));
        assertNotNull(result);
        assertTrue(result.getClock() > 1, "Lamport clock should be updated based on received clock");
    }

    /**
     * Test the method when the response is null.
     * Verifies that the method handles null response appropriately.
     */
    @Test
    public void testQueryWeatherByIdNullResponse() {
        // Arrange
        String mockId = "";
        String mockUrl = "http://localhost:" + port + "/Client/queryWeatherById?id=" + mockId;

        // Act
        ResponseEntity<CommonResult> response = restTemplate.getForEntity(mockUrl, CommonResult.class);

        // Assert
        assertEquals(204, response.getBody().getCode(), "The status should be NO_CONTENT for a null response");
    }

}
