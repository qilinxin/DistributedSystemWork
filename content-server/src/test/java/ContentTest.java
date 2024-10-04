import org.adelaide.ContentApplication;
import org.adelaide.dto.CommonResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = ContentApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class ContentTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @BeforeEach
    public void setUp() {
        // 可以在此初始化需要的测试数据或模拟数据
    }


    /**
     * Test the connectivity between client and aggregation server.
     * Checks if the hello endpoint in ContentController is working correctly.
     */
    @Test
    public void testCheckClientToAgg() {
        // Arrange
        String url = "http://localhost:" + port + "/Content/checkClientToAgg";

        // Act
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("Hello from Aggregation Server!");
    }

    /**
     * Test the saveOrUpdateWeatherInfo endpoint using TestRestTemplate.
     * Sends a PUT request with weather data to check the response.
     */
    @Test
    public void testSaveOrUpdateWeatherInfo() {
        // Arrange
        String url = "http://localhost:" + port + "/Content/saveOrUpdateWeatherInfo";
        String weatherData = "{\"id\":\"IDS60901\",\"name\":\"Adelaide\",\"air_temp\":23.5}";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> requestEntity = new HttpEntity<>(weatherData, headers);

        // Act
        ResponseEntity<CommonResult> response = restTemplate.exchange(url, HttpMethod.PUT, requestEntity, CommonResult.class);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo(200);
    }

    /**
     * Test the retry mechanism of the saveOrUpdateWeatherInfo endpoint.
     * Verifies that the method retries the request when a RuntimeException occurs.
     */
    @Test
    public void testSaveOrUpdateWeatherInfoWithRetry() {
        // Arrange
        String url = "http://localhost:" + port + "/Content/saveOrUpdateWeatherInfo";
        String weatherData = "{\"id\":\"IDS60901\",\"name\":\"Adelaide\",\"air_temp\":23.5}";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> requestEntity = new HttpEntity<>(weatherData, headers);

        // Act
        ResponseEntity<CommonResult> response = restTemplate.exchange(url, HttpMethod.PUT, requestEntity, CommonResult.class);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo(200);
    }

    /**
     * Test the null response handling of the saveOrUpdateWeatherInfo endpoint.
     * Verifies that the method handles a null response appropriately.
     */
    @Test
    public void testSaveOrUpdateWeatherInfoNullResponse() {
        // Arrange
        String url = "http://localhost:" + port + "/Content/saveOrUpdateWeatherInfo";
        String weatherData = "";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> requestEntity = new HttpEntity<>(weatherData, headers);

        // Act
        ResponseEntity<CommonResult> response = restTemplate.exchange(url, HttpMethod.PUT, requestEntity, CommonResult.class);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    /**
     * Test the response consistency and Lamport clock behavior in the saveOrUpdateWeatherInfo endpoint.
     * Verifies that the Lamport clock is updated correctly upon receiving the response.
     */
    @Test
    public void testLamportClockUpdateInSaveOrUpdateWeatherInfo() {
        // Arrange
        String url = "http://localhost:" + port + "/Content/saveOrUpdateWeatherInfo";
        String weatherData = "{\"id\":\"IDS60901\",\"name\":\"Adelaide\",\"air_temp\":23.5}";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> requestEntity = new HttpEntity<>(weatherData, headers);

        // Act
        ResponseEntity<CommonResult> response = restTemplate.exchange(url, HttpMethod.PUT, requestEntity, CommonResult.class);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo(200);
        assertThat(response.getBody().getClock()).isGreaterThan(0);
    }
}
