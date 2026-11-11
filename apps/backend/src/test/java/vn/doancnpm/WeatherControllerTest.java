package vn.doancnpm;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import vn.doancnpm.config.GlobalExceptionHandler;
import vn.doancnpm.controller.WeatherController;
import vn.doancnpm.service.WeatherService;

@ExtendWith(MockitoExtension.class)
class WeatherControllerTest {

    private static final String BASE_URL = "/api/weather";

    @Mock
    private WeatherService weatherService;

    @InjectMocks
    private WeatherController weatherController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(weatherController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private WeatherService.WeatherView createAvailableWeather(String city) {
        List<WeatherService.ForecastHour> hours = List.of(
                new WeatherService.ForecastHour("12:00", 31.0, 10.0, "Sunny"),
                new WeatherService.ForecastHour("13:00", 32.5, 15.0, "Partly Cloudy")
        );
        return new WeatherService.WeatherView(
                true,
                city,
                30.5,
                34.0,
                75.0,
                12.5,
                "Trời nắng nhẹ",
                "2026-11-11T11:00:00",
                "Cập nhật thời tiết thành công",
                hours
        );
    }

    private WeatherService.WeatherView createUnavailableWeather(String city) {
        return new WeatherService.WeatherView(
                false,
                city,
                null,
                null,
                null,
                null,
                null,
                null,
                "Dịch vụ thời tiết tạm thời không khả dụng",
                List.of()
        );
    }

    // =========================================================================
    // 1. GET /api/weather
    // =========================================================================
    @Nested
    @DisplayName("GET " + BASE_URL)
    class GetCurrentWeatherTests {

        @Test
        @DisplayName("Không truyền param city -> mặc định lấy city 'hcm' và trả về 200 OK cùng cache-control no-store")
        void current_defaultCity_returns200WithNoStore() throws Exception {
            WeatherService.WeatherView view = createAvailableWeather("hcm");
            when(weatherService.current("hcm")).thenReturn(view);

            mockMvc.perform(get(BASE_URL))
                    .andExpect(status().isOk())
                    .andExpect(header().string("Cache-Control", containsString("no-store")))
                    .andExpect(jsonPath("$.available").value(true))
                    .andExpect(jsonPath("$.city").value("hcm"))
                    .andExpect(jsonPath("$.temperature").value(30.5))
                    .andExpect(jsonPath("$.feelsLike").value(34.0))
                    .andExpect(jsonPath("$.humidity").value(75.0))
                    .andExpect(jsonPath("$.wind").value(12.5))
                    .andExpect(jsonPath("$.description").value("Trời nắng nhẹ"))
                    .andExpect(jsonPath("$.hours.length()").value(2))
                    .andExpect(jsonPath("$.hours[0].startTime").value("12:00"))
                    .andExpect(jsonPath("$.hours[0].temperature").value(31.0))
                    .andExpect(jsonPath("$.hours[0].condition").value("Sunny"));

            verify(weatherService).current("hcm");
        }

        @Test
        @DisplayName("Có truyền param city -> lấy thời tiết theo city đó và trả về 200 OK")
        void current_customCity_returns200() throws Exception {
            WeatherService.WeatherView view = createAvailableWeather("danang");
            when(weatherService.current("danang")).thenReturn(view);

            mockMvc.perform(get(BASE_URL).param("city", "danang"))
                    .andExpect(status().isOk())
                    .andExpect(header().string("Cache-Control", containsString("no-store")))
                    .andExpect(jsonPath("$.available").value(true))
                    .andExpect(jsonPath("$.city").value("danang"))
                    .andExpect(jsonPath("$.temperature").value(30.5));

            verify(weatherService).current("danang");
        }

        @Test
        @DisplayName("Dữ liệu thời tiết không sẵn sàng (available = false) -> trả về 503 Service Unavailable")
        void current_weatherUnavailable_returns503WithNoStore() throws Exception {
            WeatherService.WeatherView view = createUnavailableWeather("hcm");
            when(weatherService.current("hcm")).thenReturn(view);

            mockMvc.perform(get(BASE_URL))
                    .andExpect(status().isServiceUnavailable())
                    .andExpect(header().string("Cache-Control", containsString("no-store")))
                    .andExpect(jsonPath("$.available").value(false))
                    .andExpect(jsonPath("$.message").value("Dịch vụ thời tiết tạm thời không khả dụng"));

            verify(weatherService).current("hcm");
        }

        @Test
        @DisplayName("Service ném IllegalStateException -> xử lý qua GlobalExceptionHandler trả về 400 Bad Request")
        void current_serviceThrowsIllegalStateException_returns400() throws Exception {
            when(weatherService.current("badcity"))
                    .thenThrow(new IllegalStateException("Lỗi kết nối nhà cung cấp thời tiết"));

            mockMvc.perform(get(BASE_URL).param("city", "badcity"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.message").value("Lỗi kết nối nhà cung cấp thời tiết"));

            verify(weatherService).current("badcity");
        }

        @Test
        @DisplayName("POST /api/weather không được hỗ trợ -> trả về 405 Method Not Allowed")
        void current_postNotAllowed_returns405() throws Exception {
            mockMvc.perform(post(BASE_URL))
                    .andExpect(status().isMethodNotAllowed());

            verifyNoInteractions(weatherService);
        }
    }
}

