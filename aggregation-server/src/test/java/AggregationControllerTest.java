import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import org.adelaide.AggregationApplication;
import org.adelaide.controller.AggregationController;
import org.adelaide.dto.CommonResult;
import org.adelaide.dto.WeatherInfoWrapperDTO;
import org.adelaide.service.AggregationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Map;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@SpringBootTest(classes = AggregationApplication.class) // Add this to specify the configuration class
public class AggregationControllerTest {

    private MockMvc mockMvc;

    private Gson gson = new Gson();

    @Mock
    private AggregationService aggregationService;

    @InjectMocks
    private AggregationController aggregationController;

    @BeforeEach
    public void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(aggregationController).build();
    }

    @Test
    public void testSayHello() throws Exception {
        mockMvc.perform(get("/Agg/hello"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", is("Hello from Aggregation Server!")));
    }

    @Test
    public void testQueryCacheInfo() throws Exception {
        // Perform the GET request to the "/queryCacheInfo" endpoint
        MvcResult mvcResult = mockMvc.perform(get("/Agg/queryCacheInfo"))
                .andExpect(status().isOk())
                .andReturn();

        // Get the response content as a String
        String contentAsString = mvcResult.getResponse().getContentAsString();
        System.out.println("contentAsString===" + contentAsString);
        // Use ObjectMapper to deserialize the JSON string into a Map
        ObjectMapper objectMapper = new ObjectMapper();
        TypeReference<Map<String, WeatherInfoWrapperDTO>> typeRef =
                new TypeReference<Map<String, WeatherInfoWrapperDTO>>() {};

        Map<String, WeatherInfoWrapperDTO> resultMap = objectMapper.readValue(contentAsString, typeRef);

        // Now you can perform assertions on resultMap
        System.out.println(resultMap);
        // Add more assertions as needed
    }

    @Test
    public void testQueryWeatherById() throws Exception {
        CommonResult mockResult = new CommonResult(200, "", null, 124);
        when(aggregationService.queryWeatherById(anyInt(), anyString())).thenReturn(mockResult);

        mockMvc.perform(get("/Agg/queryWeatherById")
                        .param("clock", "123")
                        .param("id", "IDS60999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(200)))
                .andExpect(jsonPath("$.data.id", is("IDS60999")))
                .andExpect(jsonPath("$.data.name", is("Adelaide (West Terrace / ngayirdapira)")));
    }


    @Test
    public void testSaveOrUpdateWeatherInfo() throws Exception {
        CommonResult mockResult = new CommonResult(200, "Weather information updated successfully.", null, 124);
        when(aggregationService.saveOrUpdateWeatherInfo(anyString(), anyInt())).thenReturn(mockResult);

    }

}
