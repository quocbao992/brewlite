package vn.doancnpm.controller;

import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.doancnpm.service.WeatherService;

@RestController
@RequestMapping("/api/weather")
public class WeatherController {

    private final WeatherService weatherService;

    public WeatherController(WeatherService weatherService) {
        this.weatherService = weatherService;
    }

    @GetMapping
    public ResponseEntity<WeatherService.WeatherView> current(@RequestParam(defaultValue = "hcm") String city) {
        var result = weatherService.current(city);
        return ResponseEntity.status(result.available() ? 200 : 503)
                .cacheControl(CacheControl.noStore())
                .body(result);
    }
}
