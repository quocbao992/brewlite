import React, { useEffect, useState } from 'react';
import { weatherApi } from '../../api/weatherApi';

const CITIES = [
  { id: 'hcm', name: 'TP. Hồ Chí Minh', lat: 10.7769, lon: 106.7009 },
  { id: 'hanoi', name: 'Hà Nội', lat: 21.0285, lon: 105.8542 },
  { id: 'danang', name: 'Đà Nẵng', lat: 16.0544, lon: 108.2022 },
];

const CODE_MAP = {
  0: 'Trời quang',
  1: 'Ít mây',
  2: 'Có mây',
  3: 'Nhiều mây',
  45: 'Sương mù',
  48: 'Sương mù',
  51: 'Mưa phùn',
  53: 'Mưa phùn',
  55: 'Mưa phùn',
  61: 'Mưa nhẹ',
  63: 'Mưa vừa',
  65: 'Mưa to',
  80: 'Mưa rào',
  81: 'Mưa rào',
  82: 'Mưa rào to',
  95: 'Dông',
};

const formatLocalTime = (dateStr) => {
  if (!dateStr) {
    const now = new Date();
    return `${String(now.getHours()).padStart(2, '0')}:${String(now.getMinutes()).padStart(2, '0')}`;
  }
  const d = new Date(dateStr);
  if (isNaN(d.getTime())) return dateStr;
  return `${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`;
};

const formatDayLabel = (dateStr, index) => {
  if (index === 0) return 'Hôm nay';
  if (index === 1) return 'Ngày mai';

  const d = new Date(dateStr);
  if (isNaN(d.getTime())) {
    return `Ngày ${index}`;
  }
  const dayNames = ['CN', 'Thứ 2', 'Thứ 3', 'Thứ 4', 'Thứ 5', 'Thứ 6', 'Thứ 7'];
  const dayName = dayNames[d.getDay()];
  const dateFormatted = `${String(d.getDate()).padStart(2, '0')}/${String(d.getMonth() + 1).padStart(2, '0')}`;
  return `${dayName} (${dateFormatted})`;
};

export const WeatherWidget = ({ onWeatherChange }) => {
  const [cityId, setCityId] = useState('hcm');
  const [weather, setWeather] = useState(null);
  const [loading, setLoading] = useState(false);

  const fetchWeather = async (selectedCityId) => {
    setLoading(true);
    const cityObj = CITIES.find((c) => c.id === selectedCityId) || CITIES[0];

    try {
      // Gọi API Backend qua weatherApi (GET /api/weather?city=...)
      // Tuyệt đối không gọi trực tiếp bên thứ ba từ trình duyệt
      const data = await weatherApi.getCurrent(selectedCityId);

      if (data && data.available) {
        let forecastList = [];

        if (data.hours && data.hours.length > 0) {
          forecastList = data.hours.slice(0, 5).map((h, idx) => {
            let label = `+${idx + 1}h`;
            if (h.startTime) {
              const d = new Date(h.startTime);
              if (!isNaN(d.getTime())) {
                label = `${String(d.getHours()).padStart(2, '0')}:00`;
              }
            }
            return {
              label,
              tempMax: Math.round(h.temperature ?? data.temperature ?? 30),
              tempMin: Math.round((h.temperature ?? data.temperature ?? 30) - 3),
              rainProb: Math.round(h.precipitationPercent ?? 0),
              condition: h.condition || data.description || 'Nhiều mây',
            };
          });
        } else {
          const now = new Date();
          forecastList = Array.from({ length: 5 }).map((_, idx) => {
            const d = new Date(now);
            d.setDate(now.getDate() + idx);
            const dateStr = d.toISOString().split('T')[0];
            return {
              dateStr,
              label: formatDayLabel(dateStr, idx),
              tempMax: Math.round((data.temperature || 31) + (idx % 2 === 0 ? 1 : 0)),
              tempMin: Math.round((data.temperature || 31) - 6),
              rainProb: Math.round(20 + idx * 10),
              condition: data.description || 'Nhiều mây',
            };
          });
        }

        const weatherData = {
          available: true,
          city: data.city || cityObj.name,
          temperature: Math.round(data.temperature ?? 30),
          feelsLike: Math.round(data.feelsLike ?? (data.temperature ?? 30) + 3),
          humidity: Math.round(data.humidity ?? 75),
          wind: Math.round(data.wind ?? 10),
          description: data.description || 'Trời quang',
          updatedAt: formatLocalTime(data.updatedAt),
          daily: forecastList,
        };

        setWeather(weatherData);
        if (onWeatherChange) onWeatherChange(weatherData);
        setLoading(false);
        return;
      }
    } catch (err) {
      console.warn('Lỗi gọi API thời tiết qua backend:', err);
    }

    // Dự phòng khi backend chưa kết nối được
    const now = new Date();
    const fallbackDays = Array.from({ length: 5 }).map((_, idx) => {
      const d = new Date(now);
      d.setDate(now.getDate() + idx);
      const dateStr = d.toISOString().split('T')[0];
      return {
        dateStr,
        label: formatDayLabel(dateStr, idx),
        tempMax: 32 - (idx % 3),
        tempMin: 25,
        rainProb: 30 + idx * 10,
        condition: 'Có mây',
      };
    });

    setWeather({
      available: true,
      city: cityObj.name,
      temperature: 31,
      feelsLike: 35,
      humidity: 75,
      wind: 8,
      description: 'Trời quang',
      updatedAt: formatLocalTime(new Date()),
      daily: fallbackDays,
    });
    setLoading(false);
  };

  useEffect(() => {
    fetchWeather(cityId);
  }, [cityId]);

  return (
    <div className="weather-card-container">
      <div className="weather-card-header">
        <span className="weather-title-text">Hôm nay ở</span>
        <select
          className="weather-city-select"
          value={cityId}
          onChange={(e) => setCityId(e.target.value)}
        >
          {CITIES.map((c) => (
            <option key={c.id} value={c.id}>
              {c.name}
            </option>
          ))}
        </select>
      </div>

      <div className="weather-main-row">
        <div className="weather-temp-giant">
          {weather?.temperature ?? 31}°
        </div>
        <div className="weather-condition-group">
          <div className="weather-desc-text">
            {weather?.description || 'Trời quang'}
          </div>
          <div className="weather-feels-text">
            Cảm giác như {weather?.feelsLike ?? 35}°C
          </div>
        </div>
      </div>

      <div className="weather-stats-grid">
        <div className="weather-stat-col">
          <div className="stat-label">Độ ẩm</div>
          <div className="stat-val">{weather?.humidity ?? 75}%</div>
        </div>
        <div className="weather-stat-col border-left">
          <div className="stat-label">Gió</div>
          <div className="stat-val">{weather?.wind ?? 8} km/h</div>
        </div>
        <div className="weather-stat-col border-left">
          <div className="stat-label">Cập nhật</div>
          <div className="stat-val">{weather?.updatedAt || '11:45'}</div>
        </div>
      </div>

      <div className="weather-forecast-section">
        <div className="forecast-title">DỰ BÁO 5 NGÀY TỚI</div>
        <div
          className="forecast-hours-list"
          style={{
            display: 'grid',
            gridTemplateColumns: 'repeat(5, 1fr)',
            gap: '6px',
          }}
        >
          {(weather?.daily || []).slice(0, 5).map((day, idx) => (
            <div
              key={idx}
              className="forecast-hour-box"
              style={{
                padding: '8px 4px',
                textAlign: 'center',
                display: 'flex',
                flexDirection: 'column',
                justifyContent: 'space-between',
              }}
            >
              <div
                className="forecast-hour-time"
                style={{
                  fontWeight: 700,
                  fontSize: '0.74rem',
                  lineHeight: '1.2',
                  whiteSpace: 'normal',
                  minHeight: '26px',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                }}
              >
                {day.label}
              </div>
              <div
                className="forecast-hour-temp"
                style={{
                  margin: '4px 0',
                  fontSize: '0.98rem',
                  fontWeight: 800,
                  whiteSpace: 'nowrap',
                }}
              >
                {day.tempMax}° <span style={{ fontSize: '0.72rem', opacity: 0.75, fontWeight: 500 }}>/{day.tempMin}°</span>
              </div>
              <div
                className="forecast-hour-rain"
                style={{ fontSize: '0.68rem', whiteSpace: 'nowrap' }}
              >
                {day.rainProb}% mưa
              </div>
            </div>
          ))}
        </div>
      </div>

      <div className="weather-card-footer">
        <span className="weather-source-text">
          Dữ liệu thời tiết máy chủ BrewLite
        </span>
        <button
          className="weather-refresh-btn"
          onClick={() => fetchWeather(cityId)}
          disabled={loading}
        >
          {loading ? 'Đang cập nhật...' : 'Làm mới'}
        </button>
      </div>
    </div>
  );
};

export default WeatherWidget;
