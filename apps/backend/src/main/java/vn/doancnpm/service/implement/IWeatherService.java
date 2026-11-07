package vn.doancnpm.service.implement;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;
import vn.doancnpm.service.WeatherService;

@Service
public class IWeatherService implements WeatherService {

    private record City(String name, double latitude, double longitude) {}

    private static final Map<String, City> CITIES = Map.of(
            "hcm", new City("TP. Hồ Chí Minh", 10.7769, 106.7009),
            "hanoi", new City("Hà Nội", 21.0285, 105.8542),
            "danang", new City("Đà Nẵng", 16.0544, 108.2022));
    private static final String DEFAULT_USER_AGENT = "BrewLite/1.0 admin@brewlite.vn";
    private static final ZoneId VIETNAM_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter WTTR_OBSERVATION_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm a");

    private final RestClient openMeteoClient;
    private final RestClient metNoClient;
    private final RestClient wttrClient;
    private final String metNoUserAgent;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    public IWeatherService(
            @Value("${weather.user-agent:" + DEFAULT_USER_AGENT + "}") String metNoUserAgent) {
        this(
                createClient("https://api.open-meteo.com", 1500, 2500),
                createClient("https://api.met.no", 2000, 3500),
                createClient("https://wttr.in", 2000, 3500),
                metNoUserAgent);
    }

    IWeatherService(RestClient openMeteoClient, RestClient metNoClient, RestClient wttrClient) {
        this(openMeteoClient, metNoClient, wttrClient, DEFAULT_USER_AGENT);
    }

    IWeatherService(
            RestClient openMeteoClient,
            RestClient metNoClient,
            RestClient wttrClient,
            String metNoUserAgent) {
        this.openMeteoClient = openMeteoClient;
        this.metNoClient = metNoClient;
        this.wttrClient = wttrClient;
        this.metNoUserAgent = metNoUserAgent == null || metNoUserAgent.isBlank()
                ? DEFAULT_USER_AGENT
                : metNoUserAgent;
    }

    private static RestClient createClient(String baseUrl, int connectTimeout, int readTimeout) {
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectTimeout);
        factory.setReadTimeout(readTimeout);
        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(factory)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Override
    public WeatherView current(String cityId) {
        City city = CITIES.get(cityId);
        if (city == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Thành phố không được hỗ trợ");
        }

        WeatherView result = fetchOpenMeteo(city);
        if (result != null) return result;

        result = fetchMetNo(city);
        if (result != null) return result;

        result = fetchWttr(city);
        if (result != null) return result;

        return unavailable(city, "Tạm thời không tải được thời tiết từ các nhà cung cấp. Vui lòng thử lại sau.");
    }

    private JsonNode parseJson(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try {
            return objectMapper.readTree(raw);
        } catch (Exception e) {
            return null;
        }
    }

    private WeatherView fetchOpenMeteo(City city) {
        try {
            String raw = openMeteoClient
                    .get()
                    .uri(uri -> uri.path("/v1/forecast")
                            .queryParam("latitude", city.latitude())
                            .queryParam("longitude", city.longitude())
                            .queryParam(
                                    "current",
                                    "temperature_2m,apparent_temperature,relative_humidity_2m,wind_speed_10m,weather_code")
                            .queryParam("hourly", "temperature_2m,precipitation_probability,weather_code")
                            .queryParam("forecast_hours", 5)
                            .queryParam("timezone", "UTC")
                            .build())
                    .retrieve()
                    .body(String.class);
            JsonNode data = parseJson(raw);
            if (data == null || !data.at("/current/temperature_2m").isNumber()) return null;

            JsonNode current = data.path("current");
            JsonNode hourly = data.path("hourly");
            List<ForecastHour> hours = new ArrayList<>();
            JsonNode times = hourly.path("time");
            for (int i = 0; i < Math.min(5, times.size()); i++) {
                if (!hourly.path("temperature_2m").path(i).isNumber()) continue;
                hours.add(new ForecastHour(
                        openMeteoTime(times.path(i)),
                        number(hourly.path("temperature_2m").path(i)),
                        number(hourly.path("precipitation_probability").path(i)),
                        openMeteoDescription(hourly.path("weather_code").path(i))));
            }
            return new WeatherView(
                    true,
                    city.name(),
                    number(current.path("temperature_2m")),
                    number(current.path("apparent_temperature")),
                    number(current.path("relative_humidity_2m")),
                    number(current.path("wind_speed_10m")),
                    openMeteoDescription(current.path("weather_code")),
                    openMeteoTime(current.path("time")),
                    "",
                    hours);
        } catch (RestClientException ex) {
            return null;
        }
    }

    private WeatherView fetchMetNo(City city) {
        try {
            String raw = metNoClient
                    .get()
                    .uri(uri -> uri.path("/weatherapi/locationforecast/2.0/compact")
                            .queryParam("lat", city.latitude())
                            .queryParam("lon", city.longitude())
                            .build())
                    .header(HttpHeaders.USER_AGENT, metNoUserAgent)
                    .retrieve()
                    .body(String.class);
            JsonNode data = parseJson(raw);
            JsonNode timeseries = data == null ? null : data.at("/properties/timeseries");
            if (timeseries == null || !timeseries.isArray() || timeseries.isEmpty()) return null;

            JsonNode first = timeseries.path(0);
            JsonNode details = first.at("/data/instant/details");
            Double temperature = number(details.path("air_temperature"));
            if (temperature == null) return null;

            List<ForecastHour> hours = new ArrayList<>();
            for (int i = 0; i < Math.min(5, timeseries.size()); i++) {
                JsonNode entry = timeseries.path(i);
                Double hourlyTemperature = number(entry.at("/data/instant/details/air_temperature"));
                if (hourlyTemperature == null) continue;
                hours.add(new ForecastHour(
                        text(entry.path("time")),
                        hourlyTemperature,
                        number(entry.at("/data/next_1_hours/details/probability_of_precipitation")),
                        metNoDescription(metNoSymbol(entry))));
            }

            return new WeatherView(
                    true,
                    city.name(),
                    temperature,
                    null,
                    number(details.path("relative_humidity")),
                    metersPerSecondToKmh(number(details.path("wind_speed"))),
                    metNoDescription(metNoSymbol(first)),
                    text(first.path("time")),
                    "",
                    hours);
        } catch (RestClientException ex) {
            return null;
        }
    }

    private WeatherView fetchWttr(City city) {
        try {
            String location = city.latitude() + "," + city.longitude();
            String raw = wttrClient
                    .get()
                    .uri(uri -> uri.path("/{location}")
                            .queryParam("format", "j1")
                            .build(location))
                    .retrieve()
                    .body(String.class);
            JsonNode data = parseJson(raw);
            if (data == null) return null;

            JsonNode root = data.path("data").isObject() ? data.path("data") : data;
            JsonNode current = root.path("current_condition").path(0);
            Double temperature = numberText(current.path("temp_C"));
            if (temperature == null) return null;

            List<ForecastHour> hours = wttrHours(root.path("weather").path(0));
            return new WeatherView(
                    true,
                    city.name(),
                    temperature,
                    numberText(current.path("FeelsLikeC")),
                    numberText(current.path("humidity")),
                    numberText(current.path("windspeedKmph")),
                    wttrDescription(current),
                    wttrObservationTime(current.path("localObsDateTime")),
                    "",
                    hours);
        } catch (RestClientException ex) {
            return null;
        }
    }

    private static List<ForecastHour> wttrHours(JsonNode day) {
        List<ForecastHour> hours = new ArrayList<>();
        JsonNode hourly = day.path("hourly");
        LocalDate date = parseDate(text(day.path("date")));
        for (int i = 0; i < Math.min(5, hourly.size()); i++) {
            JsonNode entry = hourly.path(i);
            Double temperature = numberText(entry.path("tempC"));
            if (temperature == null) continue;
            hours.add(new ForecastHour(
                    wttrForecastTime(date, text(entry.path("time"))),
                    temperature,
                    numberText(entry.path("chanceofrain")),
                    wttrDescription(entry)));
        }
        return hours;
    }

    private static Double number(JsonNode node) {
        return node.isNumber() ? node.doubleValue() : null;
    }

    private static Double numberText(JsonNode node) {
        String value = text(node);
        if (value.isEmpty()) return null;
        try {
            return Double.valueOf(value);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static String text(JsonNode node) {
        return node == null ? "" : node.asText("");
    }

    private static String openMeteoTime(JsonNode node) {
        String time = text(node);
        return time.isEmpty() || time.endsWith("Z") ? time : time + "Z";
    }

    private static String openMeteoDescription(JsonNode codeNode) {
        if (!codeNode.isInt()) return "Thời tiết hiện tại";
        return switch (codeNode.intValue()) {
            case 0 -> "Trời quang";
            case 1 -> "Ít mây";
            case 2 -> "Có mây";
            case 3 -> "Nhiều mây";
            case 45, 48 -> "Sương mù";
            case 51, 53, 55, 56, 57 -> "Mưa phùn";
            case 61, 63, 65, 66, 67 -> "Mưa";
            case 71, 73, 75, 77, 85, 86 -> "Tuyết";
            case 80, 81, 82 -> "Mưa rào";
            case 95, 96, 99 -> "Dông";
            default -> "Thời tiết hiện tại";
        };
    }

    private static String metNoSymbol(JsonNode entry) {
        String symbol = text(entry.at("/data/next_1_hours/summary/symbol_code"));
        if (symbol.isEmpty()) {
            symbol = text(entry.at("/data/next_6_hours/summary/symbol_code"));
        }
        return symbol;
    }

    private static String metNoDescription(String symbol) {
        String normalized = symbol.replace("_day", "").replace("_night", "").replace("_polartwilight", "");
        if (normalized.contains("thunder")) return "Dông";
        if (normalized.contains("sleet")) return "Mưa tuyết";
        if (normalized.contains("snow")) return "Tuyết";
        if (normalized.contains("rainshowers")) return "Mưa rào";
        if (normalized.contains("rain")) return "Mưa";
        if (normalized.contains("fog")) return "Sương mù";
        if (normalized.contains("partlycloudy")) return "Có mây";
        if (normalized.contains("cloudy")) return "Nhiều mây";
        if (normalized.contains("fair")) return "Ít mây";
        if (normalized.contains("clearsky")) return "Trời quang";
        return "Thời tiết hiện tại";
    }

    private static String wttrDescription(JsonNode entry) {
        Integer code = integerText(entry.path("weatherCode"));
        if (code == null) return weatherDescriptionText(entry);
        return switch (code) {
            case 113 -> "Trời quang";
            case 116 -> "Có mây";
            case 119, 122 -> "Nhiều mây";
            case 143, 248, 260 -> "Sương mù";
            case 176, 263, 266, 281, 284 -> "Mưa phùn";
            case 179, 182, 185, 311, 314, 317, 320, 350, 362, 365, 374, 377 -> "Mưa tuyết";
            case 227, 230, 323, 326, 329, 332, 335, 338, 368, 371 -> "Tuyết";
            case 200, 386, 389, 392, 395 -> "Dông";
            case 293, 296, 299, 302, 305, 308, 353, 356, 359 -> "Mưa";
            default -> weatherDescriptionText(entry);
        };
    }

    private static Integer integerText(JsonNode node) {
        String value = text(node);
        try {
            return value.isEmpty() ? null : Integer.valueOf(value);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static String weatherDescriptionText(JsonNode entry) {
        String description = text(entry.path("weatherDesc").path(0).path("value"));
        return description.isEmpty() ? "Thời tiết hiện tại" : description;
    }

    private static Double metersPerSecondToKmh(Double value) {
        return value == null ? null : Math.round(value * 36.0) / 10.0;
    }

    private static String wttrObservationTime(JsonNode node) {
        String value = text(node);
        if (value.isEmpty()) return "";
        try {
            return LocalDateTime.parse(value, WTTR_OBSERVATION_FORMAT)
                    .atZone(VIETNAM_ZONE)
                    .toInstant()
                    .toString();
        } catch (DateTimeParseException ex) {
            return "";
        }
    }

    private static LocalDate parseDate(String value) {
        try {
            return value.isEmpty() ? null : LocalDate.parse(value);
        } catch (DateTimeParseException ex) {
            return null;
        }
    }

    private static String wttrForecastTime(LocalDate date, String hhmm) {
        if (date == null || hhmm.isEmpty()) return "";
        try {
            int encoded = Integer.parseInt(hhmm);
            int hour = encoded / 100;
            int minute = encoded % 100;
            return date.atTime(hour, minute).atZone(VIETNAM_ZONE).toInstant().toString();
        } catch (DateTimeException | NumberFormatException ex) {
            return "";
        }
    }

    private static WeatherView unavailable(City city, String message) {
        return new WeatherView(false, city.name(), null, null, null, null, "", "", message, List.of());
    }
}
