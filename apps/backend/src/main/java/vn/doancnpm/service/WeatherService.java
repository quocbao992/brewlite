package vn.doancnpm.service;

import java.util.List;

public interface WeatherService {

    record WeatherView(
            boolean available,
            String city,
            Double temperature,
            Double feelsLike,
            Double humidity,
            Double wind,
            String description,
            String updatedAt,
            String message,
            List<ForecastHour> hours) {}

    record ForecastHour(
            String startTime,
            Double temperature,
            Double precipitationPercent,
            String condition) {}

    WeatherView current(String cityId);
}
