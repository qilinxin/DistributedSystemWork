import com.google.gson.Gson;
import org.adelaide.dto.WeatherInfoDTO;
import org.adelaide.util.JsonUtil;
import org.adelaide.util.RandomWeatherInfoUtil;

public class JsonTest {

    public static void main(String[] args) throws IllegalAccessException, InstantiationException {
        Gson gson = new Gson();
        // Test example
        WeatherInfoDTO weatherInfoDTO = RandomWeatherInfoUtil.createMockWeatherInfo();
        String a = JsonUtil.toJson(weatherInfoDTO);
        String b = gson.toJson(weatherInfoDTO);
        System.out.println(a);
        System.out.println(b);


        String jsonString = "{\"id\":\"IDS60461\",\"name\":\"Location 26\",\"state\":\"SA\",\"time_zone\":\"CST\",\"lat\":0.7109235518555799,\"lon\":0.9438878370565853,\"local_date_time\":\"15/8:00pm\",\"local_date_time_full\":\"202409301615\",\"air_temp\":4.077108962352228,\"apparent_t\":6.536788431169587,\"cloud\":\"Clear\",\"dewpt\":3.6820549153899473,\"press\":19.596664989348984,\"rel_hum\":45,\"wind_dir\":\"N\",\"wind_spd_kmh\":12,\"wind_spd_kt\":6}";

        WeatherInfoDTO weatherInfoDTO1 = gson.fromJson(jsonString, WeatherInfoDTO.class);
        WeatherInfoDTO weatherInfoDTO2 = JsonUtil.fromJson(jsonString, WeatherInfoDTO.class);
        System.out.println(weatherInfoDTO1.toString().equals(weatherInfoDTO2.toString()));
        System.out.println(weatherInfoDTO1.toString());
        System.out.println(weatherInfoDTO2.toString());
    }
}
